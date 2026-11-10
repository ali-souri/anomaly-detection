package io.anomaly.api.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.anomaly.config.RequestLimits;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DetectionAdmissionFilterTest {
  private final DetectionAdmissionFilter filter =
      new DetectionAdmissionFilter(new RequestLimits(1, 3, 128), new ObjectMapper());

  @Test
  void synchronousResponseAndExceptionalChainRetainThenReleasePermit() throws Exception {
    var response = new MockHttpServletResponse();
    filter.doFilter(
        request(),
        response,
        (input, output) -> {
          var concurrent = new MockHttpServletResponse();
          filter.doFilter(
              request(),
              concurrent,
              (unusedInput, unusedOutput) -> {
                throw new AssertionError("No queued execution");
              });
          assertThat(concurrent.getStatus()).isEqualTo(503);
          output.getWriter().write("complete synchronous response");
        });
    assertThat(response.getContentAsString()).isEqualTo("complete synchronous response");
    assertThatThrownBy(
            () ->
                filter.doFilter(
                    request(),
                    new MockHttpServletResponse(),
                    (input, output) -> {
                      throw new ServletException("Application failure");
                    }))
        .isInstanceOf(ServletException.class);
    assertThatThrownBy(
            () ->
                filter.doFilter(
                    request(),
                    new MockHttpServletResponse(),
                    (input, output) -> {
                      throw new IOException("Client disconnected");
                    }))
        .isInstanceOf(IOException.class);
    filter.doFilter(request(), response, (input, output) -> output.getWriter().write("recovered"));
    assertThat(response.getContentAsString()).endsWith("recovered");
  }

  @Test
  void unrelatedPostIsNotAdmittedOrParsed() throws Exception {
    var request = request();
    request.setServletPath("/other");
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, (input, output) -> output.getWriter().write("unrelated"));
    assertThat(response.getContentAsString()).isEqualTo("unrelated");
  }

  private MockHttpServletRequest request() {
    var request = new MockHttpServletRequest("POST", "/api/v1/anomaly-detection");
    request.setServletPath("/api/v1/anomaly-detection");
    return request;
  }
}
