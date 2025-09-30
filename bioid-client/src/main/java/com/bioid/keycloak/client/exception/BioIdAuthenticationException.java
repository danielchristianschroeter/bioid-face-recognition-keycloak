package com.bioid.keycloak.client.exception;

/**
 * Exception for BioID authentication errors (invalid credentials, expired tokens). These errors are
 * typically non-retryable without credential refresh.
 */
public class BioIdAuthenticationException extends BioIdException {

  public BioIdAuthenticationException(String message) {
    super(message, "UNAUTHENTICATED", false, 401);
  }

  public BioIdAuthenticationException(String message, Throwable cause) {
    super(message, cause, "UNAUTHENTICATED", false, 401);
  }

  public BioIdAuthenticationException(String message, String errorCode) {
    super(message, errorCode, false, 401);
  }

  public BioIdAuthenticationException(String message, Throwable cause, String errorCode) {
    super(message, cause, errorCode, false, 401);
  }
}
