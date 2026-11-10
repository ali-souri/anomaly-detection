package io.anomaly.api.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.anomaly.api.dto.DetectionRequest;
import io.anomaly.api.exception.InvalidRequestException;
import java.util.ArrayList;
import java.util.Set;

/** Explicit JSON type checks prevent coercion and preserve omitted-versus-null semantics. */
public final class RequestParser {
  /** Creates a stateless parser with no coercion or request-specific fields. */
  public RequestParser() {}

  private static final Set<String> FIELDS = Set.of("algorithm", "threshold", "data");

  /**
   * Validates and copies a JSON request in O(n) time and memory.
   *
   * @param request required JSON object
   * @return API input with null threshold only for omission
   * @throws InvalidRequestException for undocumented fields, invalid types or non-finite values
   */
  public DetectionRequest parse(JsonNode request) {
    if (request == null || !request.isObject()) {
      throw new InvalidRequestException("Request must be a JSON object.");
    }
    var fields = request.fieldNames();
    while (fields.hasNext()) {
      String field = fields.next();
      if (!FIELDS.contains(field)) {
        throw new InvalidRequestException("Undocumented field: " + field + ".");
      }
    }
    JsonNode algorithm = request.get("algorithm");
    if (algorithm == null || !algorithm.isTextual() || algorithm.textValue().isBlank()) {
      throw new InvalidRequestException("algorithm is a required canonical string identifier.");
    }
    Double threshold = null;
    if (request.has("threshold")) {
      threshold = number(request.get("threshold"), "threshold");
      if (threshold < 0) {
        throw new InvalidRequestException("threshold must be a finite non-negative JSON number.");
      }
    }
    JsonNode data = request.get("data");
    if (data == null || !data.isArray()) {
      throw new InvalidRequestException("data is a required numeric array; empty is valid.");
    }
    var values = new ArrayList<Double>(data.size());
    for (JsonNode value : data) {
      values.add(number(value, "data elements"));
    }
    return new DetectionRequest(algorithm.textValue(), threshold, values);
  }

  private static double number(JsonNode value, String field) {
    if (value == null || !value.isNumber() || !Double.isFinite(value.doubleValue())) {
      throw new InvalidRequestException(
          field + " must contain finite JSON numbers, without nulls or strings.");
    }
    return value.doubleValue();
  }
}
