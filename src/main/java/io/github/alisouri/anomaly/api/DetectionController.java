package io.github.alisouri.anomaly.api;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.alisouri.anomaly.core.DetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Thin HTTP entry point for one calculation per request. */
@RestController
public final class DetectionController {
  private final RequestParser parser;
  private final DetectionService service;
  private final ResponseMapper mapper;

  /**
   * Injects stateless API conversion and application behavior.
   *
   * @param parser strict JSON contract validation
   * @param service algorithm selection and execution
   * @param mapper typed result conversion
   */
  public DetectionController(
      RequestParser parser, DetectionService service, ResponseMapper mapper) {
    this.parser = parser;
    this.service = service;
    this.mapper = mapper;
  }

  /**
   * Runs detection on a required JSON object.
   *
   * @param request untrusted JSON, validated before conversion
   * @return a flat algorithm-specific JSON response
   */
  @PostMapping(
      value = "/api/v1/anomaly-detection",
      consumes = "application/json",
      produces = "application/json")
  @Operation(
      summary = "Classify a finite numeric sequence",
      description =
          "IQR uses medians of lower/upper halves, excluding the overall middle for odd n. Z_SCORE uses Welford population variance (divisor n); abs(score) must exceed threshold. Boundaries are strict; equality is normal. Defaults: IQR 1.5, Z_SCORE 3.0. Empty input is NO_DATA, 1..3 values add SMALL_DATASET. Zero Z-Score variance adds ZERO_VARIANCE and all values are normal. Omitted threshold uses the selected default; explicit null is invalid. Unknown fields and numeric strings are rejected.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              content =
                  @Content(
                      schema = @Schema(implementation = DetectionRequest.class),
                      examples = {
                        @ExampleObject(
                            name = "IQR sample",
                            value =
                                "{\"algorithm\":\"IQR\",\"threshold\":1.5,\"data\":[10,12,11,13,12,14,11,13,12,100]}"),
                        @ExampleObject(
                            name = "Population Z-Score",
                            value =
                                "{\"algorithm\":\"Z_SCORE\",\"threshold\":1.5,\"data\":[2,4,4,4,5,5,7,9]}"),
                        @ExampleObject(
                            name = "Empty",
                            value = "{\"algorithm\":\"IQR\",\"data\":[]}"),
                        @ExampleObject(
                            name = "Small",
                            value = "{\"algorithm\":\"IQR\",\"data\":[5]}")
                      })))
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Successful calculation, including small and empty input",
        content =
            @Content(
                schema = @Schema(implementation = DetectionResponse.class),
                examples = {
                  @ExampleObject(
                      name = "Completed",
                      value =
                          "{\"algorithm\":\"IQR\",\"threshold\":1.5,\"status\":\"COMPLETED\",\"warnings\":[],\"anomalies\":[100.0],\"normalValues\":[10.0,12.0,11.0,13.0,12.0,14.0,11.0,13.0,12.0],\"q1\":11.0,\"q3\":13.0,\"iqr\":2.0,\"lowerBound\":8.0,\"upperBound\":16.0}"),
                  @ExampleObject(
                      name = "No data",
                      value =
                          "{\"algorithm\":\"IQR\",\"threshold\":1.5,\"status\":\"NO_DATA\",\"warnings\":[],\"anomalies\":[],\"normalValues\":[],\"q1\":null,\"q3\":null,\"iqr\":null,\"lowerBound\":null,\"upperBound\":null}"),
                  @ExampleObject(
                      name = "Small dataset",
                      value =
                          "{\"algorithm\":\"IQR\",\"threshold\":1.5,\"status\":\"COMPLETED\",\"warnings\":[\"SMALL_DATASET\"],\"anomalies\":[],\"normalValues\":[5.0],\"q1\":5.0,\"q3\":5.0,\"iqr\":0.0,\"lowerBound\":5.0,\"upperBound\":5.0}"),
                  @ExampleObject(
                      name = "Z-Score completed",
                      value =
                          "{\"algorithm\":\"Z_SCORE\",\"threshold\":1.5,\"status\":\"COMPLETED\",\"warnings\":[],\"anomalies\":[9.0],\"normalValues\":[2.0,4.0,4.0,4.0,5.0,5.0,7.0],\"mean\":5.0,\"standardDeviation\":2.0}"),
                  @ExampleObject(
                      name = "Z-Score empty",
                      value =
                          "{\"algorithm\":\"Z_SCORE\",\"threshold\":3.0,\"status\":\"NO_DATA\",\"warnings\":[],\"anomalies\":[],\"normalValues\":[],\"mean\":null,\"standardDeviation\":null}"),
                  @ExampleObject(
                      name = "Z-Score small and zero variance",
                      value =
                          "{\"algorithm\":\"Z_SCORE\",\"threshold\":3.0,\"status\":\"COMPLETED\",\"warnings\":[\"SMALL_DATASET\",\"ZERO_VARIANCE\"],\"anomalies\":[],\"normalValues\":[7.0],\"mean\":7.0,\"standardDeviation\":0.0}")
                })),
    @ApiResponse(
        responseCode = "400",
        description = "INVALID_REQUEST, MALFORMED_JSON or UNSUPPORTED_ALGORITHM",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "405",
        description = "METHOD_NOT_ALLOWED; framework Allow header preserved",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "415",
        description = "UNSUPPORTED_MEDIA_TYPE; use application/json",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "422",
        description = "NUMERICAL_RANGE: unsafe computed double arithmetic",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(
        responseCode = "500",
        description = "INTERNAL_ERROR: unexpected failure without internal details",
        content =
            @Content(
                mediaType = "application/problem+json",
                schema = @Schema(implementation = ProblemDetail.class)))
  })
  public DetectionResponse detect(@RequestBody JsonNode request) {
    DetectionRequest input = parser.parse(request);
    return mapper.toResponse(service.detect(input.algorithm(), input.data(), input.threshold()));
  }
}
