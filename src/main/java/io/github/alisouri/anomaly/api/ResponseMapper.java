package io.github.alisouri.anomaly.api;

import io.github.alisouri.anomaly.core.DetectionResult;
import io.github.alisouri.anomaly.core.IqrResult;

/** Converts core types at the API boundary, keeping serialization out of statistics. */
public final class ResponseMapper {
  /** Creates stateless result-to-wire conversion. */
  public ResponseMapper() {}

  /**
   * Converts a known typed result to its documented flat HTTP shape in O(1) time.
   *
   * @param result immutable statistical result
   * @return API response sharing immutable result lists
   * @throws IllegalStateException for an unconfigured response type
   */
  public DetectionResponse toResponse(DetectionResult result) {
    var summary = result.summary();
    if (result instanceof IqrResult iqr) {
      return new IqrResponse(
          summary.algorithm(),
          summary.threshold(),
          summary.status(),
          summary.warnings(),
          summary.anomalies(),
          summary.normalValues(),
          iqr.q1(),
          iqr.q3(),
          iqr.iqr(),
          iqr.lowerBound(),
          iqr.upperBound());
    }
    throw new IllegalStateException("No API representation configured for this result type.");
  }
}
