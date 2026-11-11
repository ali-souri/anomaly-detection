package io.anomaly.api.exception;

/** Jackson syntax/encoding/constraint failure at the direct token boundary. */
public final class MalformedJsonException extends RuntimeException {
  /**
   * Retains the internal cause while exposing only a stable public category.
   *
   * @param cause Jackson read failure
   */
  public MalformedJsonException(Throwable cause) {
    super("Malformed JSON request.", cause);
  }
}
