package io.anomaly.integration.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.anomaly.support.HttpTestSupport;
import java.io.ByteArrayInputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"detection.limits.max-numbers=4", "detection.limits.max-body-bytes=512"})
class JsonCompatibilityIT extends HttpTestSupport {
  private static final String EMPTY = "{\"algorithm\":\"IQR\",\"data\":[]}";

  @ParameterizedTest
  @MethodSource("propertyOrders")
  void allPropertyOrdersHaveTheSameContract(String body) throws Exception {
    var result = post(body);
    assertThat(result.statusCode()).isEqualTo(200);
    assertThat(json(result).path("threshold").asDouble()).isEqualTo(0);
    assertThat(json(result).path("normalValues")).isEqualTo(mapper.readTree("[2.0]"));
  }

  static Stream<String> propertyOrders() {
    String a = "\"algorithm\":\"IQR\"", d = "\"data\":[2]", t = "\"threshold\":0";
    return Stream.of(
            a + "," + d + "," + t,
            a + "," + t + "," + d,
            d + "," + a + "," + t,
            d + "," + t + "," + a,
            t + "," + a + "," + d,
            t + "," + d + "," + a)
        .map(value -> "{" + value + "}");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"algorithm\":null,\"algorithm\":\"IQR\",\"threshold\":null,\"threshold\":0,\"data\":[null],\"data\":[2]}",
        "{\"algorithm\":false,\"algorithm\":\"IQR\",\"threshold\":{\"a\":[1,null]},\"threshold\":0,\"data\":[1,2,3,4,5],\"data\":[2]}",
        "{\"algorithm\":\"IQR\",\"threshold\":-1,\"threshold\":0,\"data\":false,\"data\":[2]}"
      })
  void duplicateKnownMembersUseTheirFinalValue(String body) throws Exception {
    var result = post(body);
    assertThat(result.statusCode()).isEqualTo(200);
    assertThat(json(result).path("normalValues")).isEqualTo(mapper.readTree("[2.0]"));
  }

  @ParameterizedTest
  @ValueSource(strings = {" garbage", " {", " [1]", " true"})
  void firstCompleteRootValueWinsAndTrailingContentIsIgnored(String suffix) throws Exception {
    assertThat(post(EMPTY + suffix).statusCode()).isEqualTo(200);
  }

  @ParameterizedTest
  @MethodSource("errors")
  void errorCategoriesAndRecoveryAreStable(String body, int status, String code) throws Exception {
    var result = post(body);
    assertThat(result.statusCode()).as(body).isEqualTo(status);
    assertThat(json(result).path("code").asText()).as(body).isEqualTo(code);
    assertThat(post(EMPTY).statusCode()).isEqualTo(200);
  }

  static Stream<Arguments> errors() {
    return Stream.of(
        Arguments.of("{}", 400, "INVALID_REQUEST"),
        Arguments.of("null", 400, "INVALID_REQUEST"),
        Arguments.of("[1,", 400, "MALFORMED_JSON"),
        Arguments.of("  ", 400, "MALFORMED_JSON"),
        Arguments.of("{\"algorithm\":null,\"data\":[1,}", 400, "MALFORMED_JSON"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[null,1,2,3,4]}", 413, "INPUT_TOO_LARGE"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[1],\"data\":null}", 400, "INVALID_REQUEST"),
        Arguments.of(
            "{\"algorithm\":\"IQR\",\"data\":[],\"threshold\":0,\"threshold\":null}",
            400,
            "INVALID_REQUEST"),
        Arguments.of(
            "{\"algorithm\":\"IQR\",\"data\":[],\"x\":{\"nested\":[true,null,1,\"text\"]}}",
            400,
            "INVALID_REQUEST"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[],\"x\":[1,}", 400, "MALFORMED_JSON"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[01]}", 400, "MALFORMED_JSON"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[1e]}", 400, "MALFORMED_JSON"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[1e309]}", 400, "INVALID_REQUEST"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[null]}", 400, "INVALID_REQUEST"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[true]}", 400, "INVALID_REQUEST"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[\"1\"]}", 400, "INVALID_REQUEST"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[{}]}", 400, "INVALID_REQUEST"),
        Arguments.of("{\"algorithm\":\"IQR\",\"data\":[[]]}", 400, "INVALID_REQUEST"));
  }

  @Test
  void integerFloatExponentSignsAndLargeIntegerInterpretationArePreserved() throws Exception {
    var result = post("{\"algorithm\":\"IQR\",\"data\":[-0,-0.0,1e-9999,9007199254740993]}");
    assertThat(result.statusCode()).isEqualTo(200);
    var values = json(result).path("normalValues");
    assertThat(Double.doubleToRawLongBits(values.get(0).asDouble())).isZero();
    assertThat(Double.doubleToRawLongBits(values.get(1).asDouble())).isEqualTo(Long.MIN_VALUE);
    assertThat(Double.doubleToRawLongBits(values.get(2).asDouble())).isZero();
    assertThat(values.get(3).asDouble()).isEqualTo(9007199254740992.0);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"1.7976931348623157e308", "-1.7976931348623157e308", "5e-324", "42", "4.2e1"})
  void finiteNumericTokenBoundariesAreValid(String number) throws Exception {
    assertThat(post("{\"algorithm\":\"IQR\",\"data\":[" + number + "]}").statusCode())
        .isEqualTo(200);
  }

  @ParameterizedTest
  @ValueSource(strings = {"UTF-8", "UTF-16", "UTF-16LE", "UTF-32", "ISO-8859-1"})
  void declaredCharsetsRetainJsonSemantics(String encoding) throws Exception {
    byte[] bytes =
        ("{\"algorithm\":\"IQR\",\"data\":[],\"x\":\"é\"}").getBytes(Charset.forName(encoding));
    var result = bytes(bytes, "application/json;charset=" + encoding, false);
    assertThat(result.statusCode()).isEqualTo(400);
    assertThat(json(result).path("code").asText()).isEqualTo("INVALID_REQUEST");
  }

  @Test
  void asciiHeaderRetainsTheBaselineUnicodeByteReader() throws Exception {
    var result =
        bytes(
            "{\"algorithm\":\"IQR\",\"data\":[],\"é\":0}".getBytes(StandardCharsets.UTF_8),
            "application/json;charset=US-ASCII",
            false);
    assertThat(result.statusCode()).isEqualTo(400);
    assertThat(json(result).path("detail").asText()).isEqualTo("Undocumented field: é.");
  }

  @Test
  void genericUtf16HeaderRetainsByteAutodetection() throws Exception {
    var result =
        bytes(EMPTY.getBytes(StandardCharsets.UTF_16LE), "application/json;charset=UTF-16", false);
    assertThat(result.statusCode()).isEqualTo(200);
  }

  @Test
  void utf8BomAndExactDeclaredAndChunkedByteLimitsStayEffective() throws Exception {
    byte[] body = EMPTY.getBytes(StandardCharsets.UTF_8);
    byte[] bom = new byte[body.length + 3];
    bom[0] = (byte) 0xef;
    bom[1] = (byte) 0xbb;
    bom[2] = (byte) 0xbf;
    System.arraycopy(body, 0, bom, 3, body.length);
    assertThat(bytes(bom, "application/json", false).statusCode()).isEqualTo(200);
    for (boolean chunked : new boolean[] {false, true}) {
      assertThat(
              bytes(
                      (EMPTY + " ".repeat(512 - EMPTY.length())).getBytes(StandardCharsets.UTF_8),
                      "application/json",
                      chunked)
                  .statusCode())
          .isEqualTo(200);
      var oversized =
          bytes(
              (EMPTY + " ".repeat(513 - EMPTY.length())).getBytes(StandardCharsets.UTF_8),
              "application/json",
              chunked);
      assertThat(oversized.statusCode()).isEqualTo(413);
      assertThat(json(oversized).path("code").asText()).isEqualTo("INPUT_TOO_LARGE");
    }
    assertThat(post(EMPTY).statusCode()).isEqualTo(200);
  }

  private HttpResponse<String> bytes(byte[] body, String contentType, boolean chunked)
      throws Exception {
    var publisher =
        chunked
            ? HttpRequest.BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(body))
            : HttpRequest.BodyPublishers.ofByteArray(body);
    return client.send(
        HttpRequest.newBuilder(uri("/api/v1/anomaly-detection"))
            .header("Content-Type", contentType)
            .POST(publisher)
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }
}
