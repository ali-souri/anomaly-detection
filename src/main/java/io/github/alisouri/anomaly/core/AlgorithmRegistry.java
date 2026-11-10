package io.github.alisouri.anomaly.core;

import java.util.List;

/** Immutable strategy lookup; adding a detector never requires a selection switch. */
public final class AlgorithmRegistry {
  /**
   * Registers unique canonical identifiers once at startup.
   *
   * @param detectors configured stateless strategies
   * @throws IllegalArgumentException if identifiers collide
   */
  public AlgorithmRegistry(List<AnomalyDetector> detectors) {}

  /**
   * Resolves an exact canonical identifier in O(1) expected time.
   *
   * @param algorithm required case-sensitive identifier
   * @return registered strategy
   * @throws UnsupportedAlgorithmException if no strategy matches
   */
  public AnomalyDetector require(String algorithm) {
    throw new UnsupportedOperationException("TDD scaffold");
  }
}
