package io.anomaly.core.algorithm;

import io.anomaly.core.exception.NumericalRangeException;
import io.anomaly.core.model.DetectionResult;
import java.util.List;

/** Strategy for a stateless detector; identifiers are case-sensitive canonical names. */
public interface AnomalyDetector {
  /**
   * Identifies this strategy for exact registry lookup.
   *
   * @return the unique canonical algorithm identifier
   */
  String algorithm();

  /**
   * Supplies the sensitivity used when the request omits threshold.
   *
   * @return the finite non-negative default sensitivity, in algorithm-specific units
   */
  double defaultThreshold();

  /**
   * Partitions all input values without mutating them or changing their relative order.
   *
   * @param data required finite values; empty is valid and duplicates are retained
   * @param threshold finite non-negative sensitivity, in algorithm-specific units
   * @return immutable common behavior and algorithm-specific statistics
   * @throws IllegalArgumentException if input or threshold is invalid
   * @throws NumericalRangeException if computed arithmetic cannot be represented safely
   */
  DetectionResult detect(List<Double> data, double threshold);
}
