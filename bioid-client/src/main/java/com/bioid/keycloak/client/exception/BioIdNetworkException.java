package com.bioid.keycloak.client.exception;

import java.io.Serial;

/**
 * Exception for network-related errors when communicating with BioID BWS service. These are
 * typically retryable errors caused by temporary network issues.
 */
public final class BioIdNetworkException extends BioIdException {

  @Serial private static final long serialVersionUID = 1L;

  public BioIdNetworkException(String message, String errorCode, boolean retryable) {
    super(message, errorCode, retryable, 503);
  }

  public BioIdNetworkException(
      String message, String errorCode, boolean retryable, Throwable cause) {
    super(message, cause, errorCode, retryable, 503);
  }

  // Factory methods for common network errors
  public static BioIdNetworkException timeout(String endpoint, long timeoutMs) {
    return new BioIdNetworkException(
        String.format("Request to %s timed out after %d ms", endpoint, timeoutMs),
        "RequestTimeout",
        true);
  }

  public static BioIdNetworkException serviceUnavailable(String endpoint) {
    return new BioIdNetworkException(
        String.format("BioID BWS service unavailable at %s", endpoint), "ServiceUnavailable", true);
  }

  public static BioIdNetworkException connectionFailed(String endpoint, Throwable cause) {
    return new BioIdNetworkException(
        String.format("Failed to connect to BioID BWS service at %s", endpoint),
        "ConnectionFailed",
        true,
        cause);
  }

  public static BioIdNetworkException requestCancelled(String details) {
    return new BioIdNetworkException(
        "Request was cancelled: " + details,
        "RequestCancelled",
        false // Don't retry cancelled requests
        );
  }
}
