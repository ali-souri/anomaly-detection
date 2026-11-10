package io.github.alisouri.anomaly.core;

/** Typed algorithm results expose their shared behavior without assuming IQR statistics. */
public interface DetectionResult {
  /**
   * @return immutable status, sensitivity, warnings and ordered partitions
   */
  DetectionSummary summary();
}
