package io.github.alisouri.anomaly.core;

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
    throw new UnsupportedOperationException("TDD scaffold");
  }
}
