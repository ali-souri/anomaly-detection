package io.github.alisouri.anomaly.core;

/** Stable advisory codes; warnings do not turn a valid calculation into an error. */
public enum WarningCode {
  /** One to three values provide limited statistical evidence. */
  SMALL_DATASET,
  /** Population variance is zero; Z-Score classifies all values as normal. */
  ZERO_VARIANCE
}
