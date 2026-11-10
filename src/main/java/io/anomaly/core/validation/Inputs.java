package io.anomaly.core.validation;

import io.anomaly.core.exception.NumericalRangeException;
import java.util.ArrayList;
import java.util.List;

/** Shared core validation; request state is always kept in local values. */
public final class Inputs {
  private Inputs() {}

  /**
   * Validates sensitivity and copies finite data in O(n) time and memory.
   *
   * @param data required values in caller order, including duplicates
   * @param threshold finite non-negative algorithm sensitivity
   * @return mutable local copy; caller input is never modified
   * @throws IllegalArgumentException for missing/non-finite data or invalid sensitivity
   */
  public static List<Double> copy(List<Double> data, double threshold) {
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

  /**
   * Guards computed double arithmetic.
   *
   * @param value computed statistic or intermediate
   * @return the unchanged finite value
   * @throws NumericalRangeException when the calculation is non-finite
   */
  public static double finite(double value) {
    if (!Double.isFinite(value)) {
      throw new NumericalRangeException("Computed statistics exceed safe double arithmetic.");
    }
    return value;
  }
}
