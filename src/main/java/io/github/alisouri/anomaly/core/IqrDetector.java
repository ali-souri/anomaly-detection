package io.github.alisouri.anomaly.core;

import java.util.List;

/**
 * Median-of-halves IQR strategy. Sorts a copy in O(n log n) time and O(n) memory. For a singleton
 * Q1 = Q3 = its value; for odd n the overall middle is excluded only from the halves, never from
 * classification. Threshold is a dimensionless IQR multiplier.
 */
public final class IqrDetector implements AnomalyDetector {
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
    throw new UnsupportedOperationException("TDD scaffold");
  }
}
