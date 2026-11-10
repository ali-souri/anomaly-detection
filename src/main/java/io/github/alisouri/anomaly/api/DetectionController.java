package io.github.alisouri.anomaly.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Thin HTTP entry point for one calculation per request. */
@RestController
public final class DetectionController {
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
  public Object detect(@RequestBody JsonNode request) {
    return null;
  }
}
