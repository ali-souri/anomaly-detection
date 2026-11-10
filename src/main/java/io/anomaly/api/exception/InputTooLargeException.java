package io.anomaly.api.exception;

/** A configured body-byte or number-count policy was exceeded. */
public final class InputTooLargeException extends RuntimeException {
  /**
   * Creates an operational input limit failure.
   *
   * @param message public explanation without request contents
   */
  public InputTooLargeException(String message) {
    super(message);
  }
}
