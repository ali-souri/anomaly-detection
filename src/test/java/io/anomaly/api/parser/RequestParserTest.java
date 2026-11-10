package io.anomaly.api.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.anomaly.api.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RequestParserTest {
  private final RequestParser parser = new RequestParser();
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void missingJavaRequestIsAContractError() {
    assertThatThrownBy(() -> parser.parse(null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Request must be a JSON object.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  void blankAlgorithmIsInvalid(String algorithm) {
    var json = mapper.createObjectNode().put("algorithm", algorithm);
    json.putArray("data");
    assertThatThrownBy(() -> parser.parse(json))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("required canonical string");
  }

  @Test
  void convertedDataIsAnImmutableSnapshotWithOmissionPreserved() throws Exception {
    var source = mapper.readTree("{\"algorithm\":\"IQR\",\"data\":[2,1,2]}");
    var request = parser.parse(source);
    ((com.fasterxml.jackson.databind.node.ArrayNode) source.get("data")).add(99);
    assertThat(request.data()).containsExactly(2., 1., 2.);
    assertThat(request.threshold()).isNull();
    assertThatThrownBy(() -> request.data().add(3.))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
