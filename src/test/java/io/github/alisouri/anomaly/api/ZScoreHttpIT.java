package io.github.alisouri.anomaly.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ZScoreHttpIT extends HttpTestSupport {
  @Test
  void knownPopulationStatisticsHaveOnlyZScoreFields() throws Exception {
    var response = post("{\"algorithm\":\"Z_SCORE\",\"threshold\":1.5,\"data\":[2,4,4,4,5,5,7,9]}");
    assertThat(response.statusCode()).isEqualTo(200);
    var body = json(response);
    assertThat(body.path("algorithm").asText()).isEqualTo("Z_SCORE");
    assertThat(body.path("threshold").asDouble()).isEqualTo(1.5);
    assertThat(body.path("mean").asDouble()).isCloseTo(5, within(1e-14));
    assertThat(body.path("standardDeviation").asDouble()).isCloseTo(2, within(1e-14));
    assertThat(body.path("anomalies")).isEqualTo(mapper.readTree("[9.0]"));
    assertThat(body.path("normalValues"))
        .isEqualTo(mapper.readTree("[2.0,4.0,4.0,4.0,5.0,5.0,7.0]"));
    for (String field : List.of("q1", "q3", "iqr", "lowerBound", "upperBound")) {
      assertThat(body.has(field)).as(field).isFalse();
    }
  }

  @Test
  void defaultsUseThreeAndAlgorithmsCanDisagree() throws Exception {
    var response = post("{\"algorithm\":\"Z_SCORE\",\"data\":[0,0,0,0,0,0,0,0,0,0,10]}");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(json(response).path("threshold").asDouble()).isEqualTo(3);
    assertThat(json(response).path("anomalies")).isEqualTo(mapper.readTree("[10.0]"));
    var sample = post("{\"algorithm\":\"Z_SCORE\",\"data\":[10,12,11,13,12,14,11,13,12,100]}");
    assertThat(sample.statusCode()).isEqualTo(200);
    assertThat(json(sample).path("anomalies").size()).isZero();
    assertThat(json(sample).path("normalValues").size()).isEqualTo(10);
  }

  @Test
  void emptyHasExplicitlyNullStatisticsAndStillValidatesParameters() throws Exception {
    var response = post("{\"algorithm\":\"Z_SCORE\",\"data\":[]}");
    assertThat(response.statusCode()).isEqualTo(200);
    var body = json(response);
    assertThat(body.path("status").asText()).isEqualTo("NO_DATA");
    assertThat(body.path("threshold").asDouble()).isEqualTo(3);
    assertThat(body.has("mean")).isTrue();
    assertThat(body.path("mean").isNull()).isTrue();
    assertThat(body.has("standardDeviation")).isTrue();
    assertThat(body.path("standardDeviation").isNull()).isTrue();
    assertThat(body.path("anomalies").size()).isZero();
    assertThat(body.path("normalValues").size()).isZero();
    assertThat(body.path("warnings").size()).isZero();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"algorithm\":\"Z_SCORE\",\"data\":[],\"threshold\":null}",
        "{\"algorithm\":\"Z_SCORE\",\"data\":[],\"threshold\":-1}",
        "{\"algorithm\":\"Z_SCORE\",\"data\":[],\"threshold\":\"3\"}",
        "{\"algorithm\":\"Z_SCORE\",\"data\":[\"1\"]}"
      })
  void strictValidationAlsoAppliesToZScore(String request) throws Exception {
    var response = post(request);
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(json(response).path("code").asText()).isEqualTo("INVALID_REQUEST");
  }

  @Test
  void singletonAndZeroVarianceWarningsAndBoundaries() throws Exception {
    var singleton = post("{\"algorithm\":\"Z_SCORE\",\"data\":[7]}");
    assertThat(singleton.statusCode()).isEqualTo(200);
    assertThat(json(singleton).path("warnings"))
        .isEqualTo(mapper.readTree("[\"SMALL_DATASET\",\"ZERO_VARIANCE\"]"));
    var zero = post("{\"algorithm\":\"Z_SCORE\",\"threshold\":0,\"data\":[4,4,4,4]}");
    assertThat(zero.statusCode()).isEqualTo(200);
    assertThat(json(zero).path("standardDeviation").asDouble()).isZero();
    assertThat(json(zero).path("anomalies").size()).isZero();
    assertThat(json(zero).path("warnings")).isEqualTo(mapper.readTree("[\"ZERO_VARIANCE\"]"));
    var boundary = post("{\"algorithm\":\"Z_SCORE\",\"threshold\":1,\"data\":[-1,1]}");
    assertThat(boundary.statusCode()).isEqualTo(200);
    assertThat(json(boundary).path("anomalies").size()).isZero();
    var exactMean = post("{\"algorithm\":\"Z_SCORE\",\"threshold\":0,\"data\":[-1,0,1]}");
    assertThat(exactMean.statusCode()).isEqualTo(200);
    assertThat(json(exactMean).path("anomalies")).isEqualTo(mapper.readTree("[-1.0,1.0]"));
  }

  @Test
  void unsafeFiniteComputationsReturn422() throws Exception {
    for (String data : new String[] {"[-1e200,1e200]", "[0,1e-200]"}) {
      var response = post("{\"algorithm\":\"Z_SCORE\",\"data\":" + data + "}");
      assertThat(response.statusCode()).isEqualTo(422);
      assertThat(json(response).path("code").asText()).isEqualTo("NUMERICAL_RANGE");
      assertThat(response.headers().firstValue("Content-Type").orElse(""))
          .contains("application/problem+json");
    }
  }

  @Test
  void concurrentMixedAlgorithmsKeepIndependentStatisticsAndThresholds() throws Exception {
    var executor = Executors.newFixedThreadPool(4);
    try {
      var tasks = new ArrayList<Callable<Void>>();
      for (int i = 0; i < 16; i++) {
        final int value = i;
        String algorithm = i % 2 == 0 ? "IQR" : "Z_SCORE";
        tasks.add(
            () -> {
              var response =
                  post(
                      "{\"algorithm\":\""
                          + algorithm
                          + "\",\"threshold\":"
                          + value
                          + ",\"data\":["
                          + value
                          + "]}");
              assertThat(response.statusCode()).isEqualTo(200);
              var body = json(response);
              assertThat(body.path("algorithm").asText()).isEqualTo(algorithm);
              assertThat(body.path("threshold").asDouble()).isEqualTo(value);
              assertThat(body.path(algorithm.equals("IQR") ? "q1" : "mean").asDouble())
                  .isEqualTo(value);
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
}
