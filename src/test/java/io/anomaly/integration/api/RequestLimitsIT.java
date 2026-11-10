package io.anomaly.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.anomaly.support.HttpTestSupport;
import java.io.ByteArrayInputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "detection.limits.max-active=1",
      "detection.limits.max-numbers=3",
      "detection.limits.max-body-bytes=128"
    })
class RequestLimitsIT extends HttpTestSupport {
  private static final String VALID = "{\"algorithm\":\"IQR\",\"data\":[1,2,3]}";

  @Test
  void readinessIsAvailableWithoutTakingDetectionCapacity() throws Exception {
    var response = get("/actuator/health/readiness");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(json(response).path("status").asText()).isEqualTo("UP");
  }

  @Test
  void numberBoundaryIsInclusiveAndOversizeRecovers() throws Exception {
    assertThat(post(VALID).statusCode()).isEqualTo(200);
    assertLimit(post("{\"algorithm\":\"IQR\",\"data\":[1,2,3,4]}"));
    assertThat(post(VALID).statusCode()).isEqualTo(200);
  }

  @Test
  void declaredByteBoundaryIsInclusiveAndCountsWhitespace() throws Exception {
    assertThat(post(VALID + " ".repeat(128 - VALID.length())).statusCode()).isEqualTo(200);
    assertLimit(post(VALID + " ".repeat(129 - VALID.length())));
    assertThat(post(VALID).statusCode()).isEqualTo(200);
  }

  @Test
  void unknownLengthChunkedBodyCannotBypassByteLimit() throws Exception {
    assertThat(chunked(VALID + " ".repeat(128 - VALID.length())).statusCode()).isEqualTo(200);
    assertLimit(chunked(VALID + " ".repeat(129 - VALID.length())));
    assertThat(post(VALID).statusCode()).isEqualTo(200);
  }

  @Test
  void malformedAndNumericalErrorsReleaseCapacity() throws Exception {
    assertThat(post("{").statusCode()).isEqualTo(400);
    assertThat(post(VALID).statusCode()).isEqualTo(200);
    assertThat(
            post("{\"algorithm\":\"IQR\",\"data\":[-1.7976931348623157e308,1.7976931348623157e308]}")
                .statusCode())
        .isEqualTo(422);
    assertThat(post(VALID).statusCode()).isEqualTo(200);
  }

  private HttpResponse<String> chunked(String body) throws Exception {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    var publisher = HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(bytes));
    assertThat(publisher.contentLength()).isEqualTo(-1);
    return client.send(
        HttpRequest.newBuilder(uri("/api/v1/anomaly-detection"))
            .header("Content-Type", "application/json")
            .POST(publisher)
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private void assertLimit(HttpResponse<String> response) throws Exception {
    assertThat(response.statusCode()).isEqualTo(413);
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .contains("application/problem+json");
    assertThat(json(response).path("code").asText()).isEqualTo("INPUT_TOO_LARGE");
    assertThat(json(response).path("instance").asText()).isEqualTo("/api/v1/anomaly-detection");
  }
}
