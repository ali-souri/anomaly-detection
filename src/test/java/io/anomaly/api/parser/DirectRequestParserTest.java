package io.anomaly.api.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.anomaly.api.exception.InputTooLargeException;
import io.anomaly.api.exception.InvalidRequestException;
import io.anomaly.api.exception.MalformedJsonException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class DirectRequestParserTest {
  @Test
  void countIsBoundedBeforeAppendAndFinalDuplicateCanReplaceAnOversizedArray() {
    var parser = new RequestParser(2);
    assertThatThrownBy(() -> parser.parse(bytes("{\"algorithm\":\"IQR\",\"data\":[1,2,3]}")))
        .isInstanceOf(InputTooLargeException.class);
    var result = parser.parse(bytes("{\"data\":[null,1,2],\"data\":[2,1],\"algorithm\":\"IQR\"}"));
    assertThat(result.data()).containsExactly(2., 1.);
  }

  @Test
  void snapshotsAreIndependentOfTheInputBufferAndImmutable() {
    byte[] body = bytes("{\"algorithm\":\"IQR\",\"data\":[2,1,2]}");
    var result = new RequestParser().parse(body);
    Arrays.fill(body, (byte) 0);
    assertThat(result.data()).containsExactly(2., 1., 2.);
    assertThatThrownBy(() -> result.data().add(3.))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void configuredFactoryConstraintsApplyEvenInsideIgnoredUnknownValues() {
    var factory =
        JsonFactory.builder()
            .streamReadConstraints(
                StreamReadConstraints.builder()
                    .maxStringLength(5)
                    .maxNestingDepth(2)
                    .maxNumberLength(5)
                    .build())
            .build();
    var parser = new RequestParser(10, factory);
    for (String text :
        new String[] {
          "{\"algorithm\":\"IQR\",\"data\":[123456]}",
          "{\"algorithm\":\"IQR\",\"data\":[],\"x\":\"123456\"}",
          "{\"algorithm\":\"IQR\",\"data\":[],\"x\":[[[0]]]}"
        }) {
      assertThatThrownBy(() -> parser.parse(bytes(text)))
          .as(text)
          .isInstanceOf(MalformedJsonException.class);
      assertThatThrownBy(() -> new ObjectMapper(factory).readTree(bytes(text)))
          .as("baseline configured tree constraint: " + text)
          .isInstanceOf(IOException.class);
    }
  }

  @Test
  void theFirstUnknownNameAndSyntaxPrecedenceAreRetained() {
    var parser = new RequestParser();
    assertThatThrownBy(() -> parser.parse(bytes("{\"x\":1,\"y\":true}")))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Undocumented field: x.");
    assertThatThrownBy(() -> parser.parse(bytes("{\"algorithm\":null,\"x\":[1,}")))
        .isInstanceOf(MalformedJsonException.class);
  }

  @Test
  void malformedEncodingAndEmptyBytesAreSyntaxErrors() {
    assertThatThrownBy(() -> new RequestParser().parse(new byte[0]))
        .isInstanceOf(MalformedJsonException.class);
    byte[] text = bytes("{\"algorithm\":\"IQR\",\"data\":[],\"x\":\"é\"}");
    for (int i = 0; i < text.length; i++) {
      if (text[i] == (byte) 0xa9) {
        text[i] = (byte) '(';
      }
    }
    assertThatThrownBy(() -> new RequestParser().parse(text))
        .isInstanceOf(MalformedJsonException.class);
  }

  private static byte[] bytes(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }
}
