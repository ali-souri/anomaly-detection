package io.anomaly.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class ProblemHandlerTest {
  @Test
  void frameworkBodyReadFailuresRetainThePublicMalformedContract() {
    var request = new MockHttpServletRequest("POST", "/api/v1/anomaly-detection");
    var failure =
        new HttpMessageNotReadableException(
            "private converter detail", new MockHttpInputMessage(new byte[0]));
    var result =
        new ProblemHandler()
            .handleExceptionInternal(
                failure,
                null,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                new ServletWebRequest(request));
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    var problem = (ProblemDetail) result.getBody();
    assertThat(problem.getProperties()).containsEntry("code", "MALFORMED_JSON");
    assertThat(problem.getDetail()).isEqualTo("Malformed JSON request.");
    assertThat(problem.getInstance().toString()).isEqualTo("/api/v1/anomaly-detection");
  }
}
