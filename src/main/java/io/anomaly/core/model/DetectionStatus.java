package io.anomaly.core.model;

/** Successful calculation state, independent of whether an anomaly exists. */
public enum DetectionStatus {
  /** At least one value was classified. */
  COMPLETED,
  /** Empty input; all algorithm statistics are unavailable. */
  NO_DATA
}
