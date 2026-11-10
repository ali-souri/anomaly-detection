package io.anomaly.core.exception;

/** Signals finite valid inputs whose calculation exceeds safe double arithmetic. */
public class NumericalRangeException extends RuntimeException {
  /**
   * Describes the failed computation, without exposing request values.
   *
   * @param message safe operation description
   */
  public NumericalRangeException(String message) {
    super(message);
  }
}
