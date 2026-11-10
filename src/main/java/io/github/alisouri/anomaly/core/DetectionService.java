package io.github.alisouri.anomaly.core;

import java.util.List;

/** Application use case: chooses a strategy and applies its default threshold. */
public final class DetectionService {
  /**
   * Creates a stateless use case with constructor injection.
   *
   * @param registry immutable strategy lookup
   */
  public DetectionService(AlgorithmRegistry registry) {}

  /**
   * Validates selection even for empty data, then delegates without HTTP dependencies.
   *
   * @param algorithm canonical algorithm identifier
   * @param data required finite input values
   * @param threshold null means omitted; otherwise a finite non-negative override
   * @return the chosen algorithm's typed result
   * @throws UnsupportedAlgorithmException if algorithm is unknown
   * @throws IllegalArgumentException for invalid data or threshold
   * @throws NumericalRangeException for unsafe calculated arithmetic
   */
  public DetectionResult detect(String algorithm, List<Double> data, Double threshold) {
    throw new UnsupportedOperationException("TDD scaffold");
  }
}
