package io.github.alisouri.anomaly.core;

import java.util.ArrayList;
import java.util.List;

/** Shared core validation; request state is always kept in local values. */
final class Inputs {
  private Inputs() {}

  static List<Double> copy(List<Double> data, double threshold) {
    if (!Double.isFinite(threshold) || threshold < 0) {
      throw new IllegalArgumentException("threshold must be finite and non-negative.");
    }
    if (data == null) {
      throw new IllegalArgumentException("data is required.");
    }
    var copy = new ArrayList<Double>(data.size());
    for (Double value : data) {
      if (value == null || !Double.isFinite(value)) {
        throw new IllegalArgumentException("data must contain finite numbers only.");
      }
      copy.add(value);
    }
    return copy;
  }

  static double finite(double value) {
    if (!Double.isFinite(value)) {
      throw new NumericalRangeException("Computed statistics exceed safe double arithmetic.");
    }
    return value;
  }
}
