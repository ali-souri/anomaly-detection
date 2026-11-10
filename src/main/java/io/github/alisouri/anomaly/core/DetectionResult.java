package io.github.alisouri.anomaly.core;

/** Typed algorithm results expose their shared behavior without assuming IQR statistics. */
public interface DetectionResult {
  /**
   * Exposes behavior shared by all typed algorithm results.
   *
   * @return immutable status, sensitivity, warnings and ordered partitions
   */
  DetectionSummary summary();
}
