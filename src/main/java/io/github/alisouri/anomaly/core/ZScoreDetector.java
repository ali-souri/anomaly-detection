package io.github.alisouri.anomaly.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Population Z-Score strategy: abs((x - mean) / standardDeviation) strictly exceeds threshold. Uses
 * Welford statistics and a second classification pass in O(n) time and O(n) memory. Threshold is
 * dimensionless, measured in population standard deviations. Zero variance yields all normal values
 * and a warning; unsafe intermediate arithmetic is rejected.
 */
public final class ZScoreDetector implements AnomalyDetector {
  /** Creates a stateless population Z-Score strategy. */
  public ZScoreDetector() {}

  @Override
  public String algorithm() {
    return "Z_SCORE";
  }

  @Override
  public double defaultThreshold() {
    return 3.0;
  }

  @Override
  public ZScoreResult detect(List<Double> data, double threshold) {
    var original = Inputs.copy(data, threshold);
    if (original.isEmpty()) {
      return new ZScoreResult(
          DetectionSummary.forData(algorithm(), threshold, 0, List.of(), List.of(), List.of()),
          null,
          null);
    }
    double mean = 0;
    double sumSquaredDeviations = 0;
    int count = 0;
    boolean allEqual = true;
    double first = original.get(0);
    for (double value : original) {
      count++;
      allEqual = allEqual && value == first;
      double delta = Inputs.finite(value - mean);
      mean = Inputs.finite(mean + delta / count);
      double deltaAfter = Inputs.finite(value - mean);
      double contribution = Inputs.finite(delta * deltaAfter);
      if (contribution < 0 || (delta != 0 && deltaAfter != 0 && contribution == 0)) {
        throw new NumericalRangeException("Variance cannot be represented safely.");
      }
      sumSquaredDeviations = Inputs.finite(sumSquaredDeviations + contribution);
    }
    double variance = Inputs.finite(sumSquaredDeviations / count);
    // Distinct input must never silently become a zero-variance success through underflow.
    if (variance == 0 && !allEqual) {
      throw new NumericalRangeException("Variance cannot be represented safely.");
    }
    double standardDeviation = Inputs.finite(Math.sqrt(variance));
    var anomalies = new ArrayList<Double>();
    var normal = new ArrayList<Double>();
    for (double value : original) {
      if (standardDeviation == 0) {
        normal.add(value);
      } else {
        double score = Inputs.finite(Inputs.finite(value - mean) / standardDeviation);
        if (Math.abs(score) > threshold) {
          anomalies.add(value);
        } else {
          normal.add(value);
        }
      }
    }
    var warnings =
        standardDeviation == 0 ? List.of(WarningCode.ZERO_VARIANCE) : List.<WarningCode>of();
    return new ZScoreResult(
        DetectionSummary.forData(algorithm(), threshold, count, warnings, anomalies, normal),
        mean,
        standardDeviation);
  }
}
