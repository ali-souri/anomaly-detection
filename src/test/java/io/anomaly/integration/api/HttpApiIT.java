package io.anomaly.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.anomaly.support.HttpTestSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(
    useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "SWAGGER_ENABLED=true")
@Import(HttpApiIT.FailureEndpoint.class)
class HttpApiIT extends HttpTestSupport {
  @Test
  void sampleRunsThroughRealSerializationRegistryAndIqr() throws Exception {
    var response =
        post("{\"algorithm\":\"IQR\",\"threshold\":1.5,\"data\":[10,12,11,13,12,14,11,13,12,100]}");
    assertThat(response.statusCode()).isEqualTo(200);
    var body = json(response);
    assertThat(body.path("algorithm").asText()).isEqualTo("IQR");
    assertThat(body.path("threshold").asDouble()).isEqualTo(1.5);
    assertThat(body.path("status").asText()).isEqualTo("COMPLETED");
    assertThat(body.path("q1").asDouble()).isEqualTo(11);
    assertThat(body.path("q3").asDouble()).isEqualTo(13);
    assertThat(body.path("iqr").asDouble()).isEqualTo(2);
    assertThat(body.path("lowerBound").asDouble()).isEqualTo(8);
    assertThat(body.path("upperBound").asDouble()).isEqualTo(16);
    assertThat(body.path("anomalies")).isEqualTo(mapper.readTree("[100.0]"));
    assertThat(body.path("normalValues"))
        .isEqualTo(mapper.readTree("[10.0,12.0,11.0,13.0,12.0,14.0,11.0,13.0,12.0]"));
    assertThat(body.path("warnings").size()).isZero();
    assertThat(body.has("mean")).isFalse();
  }

  @Test
  void emptyAndSmallAreSuccessfulAndThresholdOmissionIsDistinctFromNull() throws Exception {
    var empty = post("{\"algorithm\":\"IQR\",\"data\":[]}");
    assertThat(empty.statusCode()).isEqualTo(200);
    var body = json(empty);
    assertThat(body.path("status").asText()).isEqualTo("NO_DATA");
    for (String field : List.of("q1", "q3", "iqr", "lowerBound", "upperBound")) {
      assertThat(body.has(field)).as(field).isTrue();
      assertThat(body.path(field).isNull()).as(field).isTrue();
    }
    assertThat(body.path("anomalies").size()).isZero();
    assertThat(body.path("normalValues").size()).isZero();
    var small = post("{\"algorithm\":\"IQR\",\"data\":[5]}");
    assertThat(small.statusCode()).isEqualTo(200);
    assertThat(json(small).path("warnings").get(0).asText()).isEqualTo("SMALL_DATASET");
    assertThat(json(small).path("threshold").asDouble()).isEqualTo(1.5);
    assertThat(json(small).path("q1").asDouble()).isEqualTo(5);
    assertProblem(
        post("{\"algorithm\":\"IQR\",\"threshold\":null,\"data\":[]}"), 400, "INVALID_REQUEST");
    assertThat(post("{\"algorithm\":\"IQR\",\"threshold\":0,\"data\":[1,2,3]}").statusCode())
        .isEqualTo(200);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{}",
        "[]",
        "null",
        "17",
        "\"text\"",
        "{\"data\":[]}",
        "{\"algorithm\":null,\"data\":[]}",
        "{\"algorithm\":5,\"data\":[]}",
        "{\"algorithm\":\"IQR\"}",
        "{\"algorithm\":\"IQR\",\"data\":null}",
        "{\"algorithm\":\"IQR\",\"data\":5}",
        "{\"algorithm\":\"IQR\",\"data\":{}}",
        "{\"algorithm\":\"IQR\",\"data\":[null]}",
        "{\"algorithm\":\"IQR\",\"data\":[true]}",
        "{\"algorithm\":\"IQR\",\"data\":[\"2\"]}",
        "{\"algorithm\":\"IQR\",\"data\":[{}]}",
        "{\"algorithm\":\"IQR\",\"data\":[[]]}",
        "{\"algorithm\":\"IQR\",\"data\":[1e309]}",
        "{\"algorithm\":\"IQR\",\"data\":[],\"threshold\":-1}",
        "{\"algorithm\":\"IQR\",\"data\":[],\"threshold\":\"1.5\"}",
        "{\"algorithm\":\"IQR\",\"data\":[],\"threshold\":true}",
        "{\"algorithm\":\"IQR\",\"data\":[],\"threshold\":1e309}",
        "{\"algorithm\":\"IQR\",\"data\":[],\"threshhold\":1.5}"
      })
  void wrongShapesAndUndocumentedFieldsAreRejected(String request) throws Exception {
    var response = post(request);
    assertThat(response.statusCode()).as(request).isEqualTo(400);
    assertThat(json(response).path("code").asText()).isIn("INVALID_REQUEST", "MALFORMED_JSON");
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .contains("application/problem+json");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{",
        "{\"algorithm\":\"IQR\",\"data\":[NaN]}",
        "{\"algorithm\":\"IQR\",\"data\":[Infinity]}"
      })
  void malformedJsonUsesProblemDetails(String request) throws Exception {
    assertProblem(post(request), 400, "MALFORMED_JSON");
  }

  @Test
  void unknownAlgorithmIsValidatedEvenWithEmptyData() throws Exception {
    assertProblem(post("{\"algorithm\":\"UNKNOWN\",\"data\":[]}"), 400, "UNSUPPORTED_ALGORITHM");
    assertProblem(post("{\"algorithm\":\"iqr\",\"data\":[]}"), 400, "UNSUPPORTED_ALGORITHM");
  }

  @Test
  void preservesFramework415And405AndMapsArithmeticTo422() throws Exception {
    assertProblem(
        send("POST", "/api/v1/anomaly-detection", "text/plain", "hello"),
        415,
        "UNSUPPORTED_MEDIA_TYPE");
    var wrongMethod = get("/api/v1/anomaly-detection");
    assertProblem(wrongMethod, 405, "METHOD_NOT_ALLOWED");
    assertThat(wrongMethod.headers().firstValue("Allow").orElse("")).contains("POST");
    assertProblem(
        post("{\"algorithm\":\"IQR\",\"data\":[-1.7976931348623157e308,1.7976931348623157e308]}"),
        422,
        "NUMERICAL_RANGE");
  }

  @Test
  void unexpectedErrorsAreSafeProblemDetails() throws Exception {
    var response = get("/test/failure");
    assertProblem(response, 500, "INTERNAL_ERROR");
    assertThat(response.body()).doesNotContain("private failure", "stackTrace", "RuntimeException");
  }

  @Test
  void swaggerAndJsonStayDisabledOutsideDevEvenWithTheFlag() throws Exception {
    assertThat(get("/v3/api-docs").statusCode()).isEqualTo(404);
    assertThat(get("/v3/api-docs.yaml").statusCode()).isEqualTo(404);
    assertThat(get("/swagger-ui/index.html").statusCode()).isEqualTo(404);
    assertThat(get("/swagger-ui.html").statusCode()).isEqualTo(404);
    var properties = new java.util.Properties();
    try (var metadata =
        getClass().getResourceAsStream("/META-INF/maven/org.webjars/swagger-ui/pom.properties")) {
      properties.load(metadata);
    }
    assertThat(get("/webjars/swagger-ui/index.html").statusCode()).isEqualTo(404);
    assertThat(
            get("/webjars/swagger-ui/" + properties.getProperty("version") + "/index.html")
                .statusCode())
        .isEqualTo(404);
  }

  @Test
  void concurrentRequestsKeepIndependentState() throws Exception {
    var executor = Executors.newFixedThreadPool(4);
    try {
      var tasks = new ArrayList<Callable<Void>>();
      for (int i = 0; i < 16; i++) {
        final int value = i;
        tasks.add(
            () -> {
              var response =
                  post(
                      "{\"algorithm\":\"IQR\",\"threshold\":"
                          + value
                          + ",\"data\":["
                          + value
                          + "]}");
              assertThat(response.statusCode()).isEqualTo(200);
              assertThat(json(response).path("q1").asDouble()).isEqualTo(value);
              assertThat(json(response).path("threshold").asDouble()).isEqualTo(value);
              return null;
            });
      }
      for (var future : executor.invokeAll(tasks)) {
        future.get();
      }
    } finally {
      executor.shutdownNow();
    }
  }

  void assertProblem(java.net.http.HttpResponse<String> response, int status, String code)
      throws Exception {
    assertThat(response.statusCode()).isEqualTo(status);
    assertThat(response.headers().firstValue("Content-Type").orElse(""))
        .contains("application/problem+json");
    var body = json(response);
    assertThat(body.path("status").asInt()).isEqualTo(status);
    assertThat(body.path("code").asText()).isEqualTo(code);
    assertThat(body.path("type").asText()).startsWith("urn:anomaly-detection:problem:");
    assertThat(body.path("title").asText()).isNotBlank();
    assertThat(body.path("detail").asText()).isNotBlank();
    assertThat(body.path("instance").asText()).startsWith("/");
  }

  @RestController
  static class FailureEndpoint {
    @GetMapping("/test/failure")
    String fail() {
      throw new IllegalStateException("private failure");
    }
  }
}
