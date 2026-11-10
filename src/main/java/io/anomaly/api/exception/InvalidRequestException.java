package io.anomaly.api.exception;

/** A JSON shape or value violates the documented HTTP contract. */
public final class InvalidRequestException extends IllegalArgumentException {
  /**
   * Creates a validation error safe to include in Problem Details.
   *
   * @param message concise field constraint, without request data
   */
  public InvalidRequestException(String message) {
    super(message);
  }
}
