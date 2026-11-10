package io.anomaly.core.exception;

/** The requested canonical identifier is not registered. */
public final class UnsupportedAlgorithmException extends IllegalArgumentException {
  /** Creates a stable error without reflecting untrusted input. */
  public UnsupportedAlgorithmException() {
    super("Unsupported algorithm; use a documented canonical identifier.");
  }
}
