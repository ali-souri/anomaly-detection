package io.github.alisouri.anomaly.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable strategy lookup; adding a detector never requires a selection switch. */
public final class AlgorithmRegistry {
  private final Map<String, AnomalyDetector> detectors;

  /**
   * Registers unique canonical identifiers once at startup.
   *
   * @param detectors configured stateless strategies
   * @throws IllegalArgumentException if identifiers collide
   */
  public AlgorithmRegistry(List<AnomalyDetector> detectors) {
    var registered = new HashMap<String, AnomalyDetector>();
    for (AnomalyDetector detector : detectors) {
      if (registered.putIfAbsent(detector.algorithm(), detector) != null) {
        throw new IllegalArgumentException("Duplicate algorithm identifier.");
      }
    }
    this.detectors = Map.copyOf(registered);
  }

  /**
   * Resolves an exact canonical identifier in O(1) expected time.
   *
   * @param algorithm required case-sensitive identifier
   * @return registered strategy
   * @throws UnsupportedAlgorithmException if no strategy matches
   */
  public AnomalyDetector require(String algorithm) {
    AnomalyDetector detector = algorithm == null ? null : detectors.get(algorithm);
    if (detector == null) {
      throw new UnsupportedAlgorithmException();
    }
    return detector;
  }
}
