package io.anomaly.api.parser;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import io.anomaly.api.dto.DetectionRequest;
import io.anomaly.api.exception.InputTooLargeException;
import io.anomaly.api.exception.InvalidRequestException;
import io.anomaly.api.exception.MalformedJsonException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/** Direct token validation; no tree, coercion or request-specific shared state. */
public final class RequestParser {
  private final int maxNumbers;
  private final JsonFactory factory;

  /** Creates the default two-million-number reader. */
  public RequestParser() {
    this(2_000_000);
  }

  /**
   * Creates a reader with default Jackson constraints.
   *
   * @param maxNumbers inclusive validated positive count policy
   */
  public RequestParser(int maxNumbers) {
    this(maxNumbers, new JsonFactory());
  }

  /**
   * Uses the application's configured factory/constraints without mutating it.
   *
   * @param maxNumbers inclusive validated positive count policy
   * @param factory configured thread-safe factory
   */
  public RequestParser(int maxNumbers, JsonFactory factory) {
    this.maxNumbers = maxNumbers;
    this.factory = factory;
  }

  /**
   * Reads default JSON encodings from bounded bytes.
   *
   * @param request raw input; null is a Java contract error
   * @return immutable validated request
   */
  public DetectionRequest parse(byte[] request) {
    return parse(request, null);
  }

  /**
   * Reads one complete root value, retaining baseline last-wins and ignored-trailing policies.
   * Semantic checks follow syntax consumption so later malformed input retains its error category.
   *
   * @param request bounded bytes, still O(n) memory together with the numeric list
   * @param charset optional declared charset; Unicode encodings retain Jackson autodetection
   * @return immutable request, with null threshold only for omission
   * @throws InvalidRequestException for semantic/type/required-field failures
   * @throws InputTooLargeException for the final data field's count policy
   * @throws MalformedJsonException for syntax, encoding or configured read constraints
   */
  public DetectionRequest parse(byte[] request, Charset charset) {
    if (request == null) {
      throw new InvalidRequestException("Request must be a JSON object.");
    }
    try (JsonParser parser = open(request, charset)) {
      JsonToken first = parser.nextToken();
      if (first == null) {
        throw new JsonParseException(parser, "Expected a JSON value.");
      }
      if (first != JsonToken.START_OBJECT) {
        skipValue(parser);
        throw new InvalidRequestException("Request must be a JSON object.");
      }
      String algorithm = null;
      String unknown = null;
      Double threshold = null;
      boolean thresholdSeen = false;
      Data data = null;
      while (parser.nextToken() != JsonToken.END_OBJECT) {
        String field = parser.currentName();
        parser.nextToken();
        switch (field) {
          case "algorithm" -> {
            algorithm = parser.currentToken() == JsonToken.VALUE_STRING ? parser.getText() : null;
            skipValue(parser);
          }
          case "threshold" -> {
            thresholdSeen = true;
            threshold = number(parser);
          }
          case "data" -> data = data(parser);
          default -> {
            if (unknown == null) {
              unknown = field;
            }
            skipValue(parser);
          }
        }
      }
      if (unknown != null) {
        throw new InvalidRequestException("Undocumented field: " + unknown + ".");
      }
      if (algorithm == null || algorithm.isBlank()) {
        throw new InvalidRequestException("algorithm is a required canonical string identifier.");
      }
      if (thresholdSeen && (threshold == null || threshold < 0)) {
        throw new InvalidRequestException("threshold must be a finite non-negative JSON number.");
      }
      if (data == null || data.values() == null) {
        throw new InvalidRequestException("data is a required numeric array; empty is valid.");
      }
      if (data.oversized()) {
        throw new InputTooLargeException("data exceeds the configured number limit.");
      }
      if (data.invalid()) {
        throw new InvalidRequestException(
            "data elements must contain finite JSON numbers, without nulls or strings.");
      }
      return new DetectionRequest(algorithm, threshold, data.values());
    } catch (IOException error) {
      throw new MalformedJsonException(error);
    }
  }

  private JsonParser open(byte[] request, Charset charset) throws IOException {
    return charset == null || charset.name().startsWith("UTF-")
        ? factory.createParser(request)
        : factory.createParser(new InputStreamReader(new ByteArrayInputStream(request), charset));
  }

  private Data data(JsonParser parser) throws IOException {
    if (parser.currentToken() != JsonToken.START_ARRAY) {
      skipValue(parser);
      return new Data(null, false, false);
    }
    var values = new ArrayList<Double>();
    int count = 0;
    boolean invalid = false;
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      count++;
      Double value = number(parser);
      if (value == null) {
        invalid = true;
      } else if (count <= maxNumbers) {
        values.add(value);
      }
    }
    return new Data(values, invalid, count > maxNumbers);
  }

  private static Double number(JsonParser parser) throws IOException {
    if (!parser.currentToken().isNumeric()) {
      skipValue(parser);
      return null;
    }
    // IntNode formerly erased integer -0's sign; preserve that distinction from floating -0.0.
    double value =
        parser.currentToken() == JsonToken.VALUE_NUMBER_INT
            ? parser.getNumberValue().doubleValue()
            : parser.getDoubleValue();
    return Double.isFinite(value) ? value : null;
  }

  private static void skipValue(JsonParser parser) throws IOException {
    finishValue(parser);
    if (!parser.currentToken().isStructStart()) {
      return;
    }
    int depth = 1;
    while (depth > 0) {
      JsonToken token = parser.nextToken();
      if (token.isStructStart()) {
        depth++;
      } else if (token.isStructEnd()) {
        depth--;
      }
      finishValue(parser);
    }
  }

  private static void finishValue(JsonParser parser) throws IOException {
    parser.finishToken();
    if (parser.currentToken() == JsonToken.VALUE_STRING) {
      parser.getText();
    }
    if (parser.currentToken().isNumeric()) {
      parser.getNumberValue();
    }
  }

  private record Data(List<Double> values, boolean invalid, boolean oversized) {}
}
