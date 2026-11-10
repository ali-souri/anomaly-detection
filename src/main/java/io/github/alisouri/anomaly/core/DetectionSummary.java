package io.github.alisouri.anomaly.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable behavior shared by all algorithms.
 *
 * @param algorithm canonical identifier
 * @param threshold effective sensitivity in algorithm-specific units
 * @param status successful calculation state
 * @param warnings stable advisory codes
 * @param anomalies original values outside the algorithm's strict boundary, in input order
 * @param normalValues remaining original values in input order, including duplicates
 */
public record DetectionSummary(
    String algorithm,
    double threshold,
    DetectionStatus status,
    List<WarningCode> warnings,
    List<Double> anomalies,
    List<Double> normalValues) {
  /** Defensively copies the lists so callers cannot change a published result. */
  public DetectionSummary {
    warnings = List.copyOf(warnings);
    anomalies = List.copyOf(anomalies);
    normalValues = List.copyOf(normalValues);
  }

  static DetectionSummary forData(
      String algorithm,
      double threshold,
      int size,
      List<WarningCode> extraWarnings,
      List<Double> anomalies,
      List<Double> normalValues) {
    var warnings = new ArrayList<WarningCode>();
    if (size > 0 && size < 4) {
      warnings.add(WarningCode.SMALL_DATASET);
    }
    warnings.addAll(extraWarnings);
    return new DetectionSummary(
        algorithm,
        threshold,
        size == 0 ? DetectionStatus.NO_DATA : DetectionStatus.COMPLETED,
        warnings,
        anomalies,
        normalValues);
  }
}
