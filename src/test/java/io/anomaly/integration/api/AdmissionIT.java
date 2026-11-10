package io.anomaly.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.anomaly.support.HttpTestSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AdmissionIT.Coordination.class)
class AdmissionIT extends HttpTestSupport {
  @Autowired Gate gate;

  @ParameterizedTest
  @ValueSource(ints = {0, 10_000_000})
  void occupiedSlotRejectsWithoutWaitingThenRecovers(int padding) throws Exception {
    gate.entered = new CountDownLatch(1);
    gate.release = new CountDownLatch(1);
    var first =
        client.sendAsync(
            HttpRequest.newBuilder(uri("/api/v1/anomaly-detection"))
                .header("Content-Type", "application/json")
                .header("X-Test-Hold", "yes")
                .POST(
                    HttpRequest.BodyPublishers.ofString("{\"algorithm\":\"IQR\",\"data\":[3,1,2]}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    try {
      assertThat(gate.entered.await(5, TimeUnit.SECONDS)).isTrue();
      var second =
          client
              .sendAsync(
                  HttpRequest.newBuilder(uri("/api/v1/anomaly-detection"))
                      .header("Content-Type", "application/json")
                      .POST(
                          HttpRequest.BodyPublishers.ofString(
                              "{\"algorithm\":\"Z_SCORE\",\"data\":[7]}" + " ".repeat(padding)))
                      .build(),
                  HttpResponse.BodyHandlers.ofString())
              .get(2, TimeUnit.SECONDS);
      assertThat(second.statusCode()).isEqualTo(503);
      assertThat(second.headers().firstValue("Retry-After")).contains("1");
      assertThat(json(second).path("code").asText()).isEqualTo("ADMISSION_FULL");
      assertThat(json(second).path("status").asInt()).isEqualTo(503);
    } finally {
      gate.release.countDown();
    }
    assertThat(first.get(5, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
    var recovered = post("{\"algorithm\":\"Z_SCORE\",\"data\":[7]}");
    assertThat(recovered.statusCode()).isEqualTo(200);
    assertThat(json(recovered).path("normalValues").get(0).asDouble()).isEqualTo(7);
  }

  static class Gate extends OncePerRequestFilter {
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
      if (request.getHeader("X-Test-Hold") != null) {
        entered.countDown();
        try {
          if (!release.await(10, TimeUnit.SECONDS)) {
            throw new ServletException("Test coordination timed out");
          }
        } catch (InterruptedException error) {
          Thread.currentThread().interrupt();
          throw new ServletException(error);
        }
      }
      chain.doFilter(request, response);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class Coordination {
    @Bean
    Gate gate() {
      return new Gate();
    }

    @Bean
    FilterRegistrationBean<Gate> coordinatedFilter(Gate gate) {
      var registration = new FilterRegistrationBean<>(gate);
      registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
      return registration;
    }
  }
}
