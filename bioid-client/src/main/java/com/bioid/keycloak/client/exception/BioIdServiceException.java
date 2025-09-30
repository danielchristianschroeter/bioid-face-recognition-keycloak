package com.bioid.keycloak.client.exception;

/**
 * Exception for BioID service-related errors (network, timeout, service unavailable). These errors
 * are typically retryable.
 */
public class BioIdServiceException extends BioIdException {

  public BioIdServiceException(String message) {
    super(message, null, true, 503);
  }

  public BioIdServiceException(String message, Throwable cause) {
    super(message, cause, null, true, 503);
  }

  public BioIdServiceException(String message, String errorCode) {
    super(message, errorCode, true, 503);
  }

  public BioIdServiceException(String message, Throwable cause, String errorCode) {
    super(message, cause, errorCode, true, 503);
  }

  public BioIdServiceException(String message, Throwable cause, String errorCode, int httpStatus) {
    super(message, cause, errorCode, true, httpStatus);
  }
}
