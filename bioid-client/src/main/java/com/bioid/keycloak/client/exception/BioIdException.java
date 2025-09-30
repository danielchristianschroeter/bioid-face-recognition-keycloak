package com.bioid.keycloak.client.exception;

/**
 * Base exception for all BioID BWS service related errors. Provides structured error handling with
 * categorization for retry logic.
 */
public class BioIdException extends Exception {

  private final String errorCode;
  private final boolean retryable;
  private final int httpStatus;

  public BioIdException(String message) {
    this(message, null, false, 500);
  }

  public BioIdException(String message, Throwable cause) {
    this(message, cause, null, false, 500);
  }

  public BioIdException(String message, String errorCode, boolean retryable, int httpStatus) {
    super(message);
    this.errorCode = errorCode;
    this.retryable = retryable;
    this.httpStatus = httpStatus;
  }

  public BioIdException(
      String message, Throwable cause, String errorCode, boolean retryable, int httpStatus) {
    super(message, cause);
    this.errorCode = errorCode;
    this.retryable = retryable;
    this.httpStatus = httpStatus;
  }

  /**
   * Gets the BioID specific error code.
   *
   * @return error code or null if not available
   */
  public String getErrorCode() {
    return errorCode;
  }

  /**
   * Indicates whether this error is retryable.
   *
   * @return true if the operation can be retried, false otherwise
   */
  public boolean isRetryable() {
    return retryable;
  }

  /**
   * Gets the HTTP status code equivalent.
   *
   * @return HTTP status code
   */
  public int getHttpStatus() {
    return httpStatus;
  }

  /**
   * Creates a retryable BioID exception.
   *
   * @param message error message
   * @param errorCode BioID error code
   * @return retryable exception
   */
  public static BioIdException retryable(String message, String errorCode) {
    return new BioIdException(message, errorCode, true, 503);
  }

  /**
   * Creates a non-retryable BioID exception.
   *
   * @param message error message
   * @param errorCode BioID error code
   * @param httpStatus HTTP status code
   * @return non-retryable exception
   */
  public static BioIdException nonRetryable(String message, String errorCode, int httpStatus) {
    return new BioIdException(message, errorCode, false, httpStatus);
  }
}
