package io.github.alisouri.anomaly.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Median-of-halves IQR strategy. Sorts a copy in O(n log n) time and O(n) memory. For a singleton
 * Q1 = Q3 = its value; for odd n the overall middle is excluded only from the halves, never from
 * classification. Threshold is a dimensionless IQR multiplier.
 */
public final class IqrDetector implements AnomalyDetector {
  /** Creates a stateless median-of-halves IQR strategy. */
  public IqrDetector() {}

  @Override
  public String algorithm() {
    return "IQR";
  }

  @Override
  public double defaultThreshold() {
    return 1.5;
  }

  @Override
  public IqrResult detect(List<Double> data, double threshold) {
    var original = Inputs.copy(data, threshold);
    int size = original.size();
    if (size == 0) {
      return new IqrResult(
          DetectionSummary.forData(algorithm(), threshold, 0, List.of(), List.of(), List.of()),
          null,
          null,
          null,
          null,
          null);
    }
    var sorted = new ArrayList<>(original);
    Collections.sort(sorted);
    double q1 = size == 1 ? sorted.get(0) : median(sorted, 0, size / 2);
    double q3 = size == 1 ? sorted.get(0) : median(sorted, (size + 1) / 2, size);
    double iqr = Inputs.finite(q3 - q1);
    double margin = Inputs.finite(threshold * iqr);
    double lower = Inputs.finite(q1 - margin);
    double upper = Inputs.finite(q3 + margin);
    var anomalies = new ArrayList<Double>();
    var normal = new ArrayList<Double>();
    for (double value : original) {
      if (value < lower || value > upper) {
        anomalies.add(value);
      } else {
        normal.add(value);
      }
    }
    return new IqrResult(
        DetectionSummary.forData(algorithm(), threshold, size, List.of(), anomalies, normal),
        q1,
        q3,
        iqr,
        lower,
        upper);
  }

  private static double median(List<Double> sorted, int from, int to) {
    int middle = from + (to - from) / 2;
    if ((to - from) % 2 == 1) {
      return sorted.get(middle);
    }
    double left = sorted.get(middle - 1);
    double right = sorted.get(middle);
    double sum = left + right;
    // Rescale only when the sum overflows; ordinary addition preserves subnormal values.
    return Double.isFinite(sum) ? sum / 2 : left / 2 + right / 2;
  }
}
