package com.bioid.keycloak.client.error;

import com.bioid.keycloak.client.exception.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for mapping gRPC errors and BioID BWS error codes to application-specific
 * exceptions.
 *
 * <p>Features: - Maps gRPC status codes to appropriate exception types - Handles BioID-specific
 * error codes from BWS service - Determines retry eligibility based on error type - Provides
 * structured error information for logging and monitoring
 */
public final class BioIdErrorMapper {

  private static final Logger logger = LoggerFactory.getLogger(BioIdErrorMapper.class);

  // BioID BWS specific error codes that indicate retryable conditions
  private static final Set<String> RETRYABLE_BWS_ERRORS =
      Set.of(
          "ServiceUnavailable",
          "RequestTimeout",
          "ConnectionFailed",
          "RateLimitExceeded",
          "InternalError");

  // BioID BWS specific error codes that indicate non-retryable conditions
  private static final Set<String> NON_RETRYABLE_BWS_ERRORS =
      Set.of(
          "TemplateNotFound",
          "TemplateCorrupted",
          "TemplateExpired",
          "NoSuitableFaceImage",
          "MultipleFacesFound",
          "NoFeatureVectors",
          "DifferentFeatureVersions",
          "InvalidImageFormat",
          "ImageTooSmall",
          "ImageTooBig",
          "PoorImageQuality");

  // gRPC status codes that are typically retryable
  private static final Set<Status.Code> RETRYABLE_GRPC_CODES =
      Set.of(
          Status.Code.UNAVAILABLE,
          Status.Code.DEADLINE_EXCEEDED,
          Status.Code.RESOURCE_EXHAUSTED,
          Status.Code.INTERNAL,
          Status.Code.ABORTED);

  private BioIdErrorMapper() {
    // Utility class - prevent instantiation
  }

  /**
   * Maps a gRPC StatusRuntimeException to an appropriate BioID exception.
   *
   * @param exception the gRPC exception
   * @param operation the operation that failed (for context)
   * @return mapped BioID exception
   */
  public static BioIdException mapGrpcException(
      StatusRuntimeException exception, String operation) {
    Status status = exception.getStatus();
    Status.Code code = status.getCode();
    String description = status.getDescription();
    String errorMessage = buildErrorMessage(operation, description);

    logger.debug(
        "Mapping gRPC error: code={}, description={}, operation={}", code, description, operation);

    return switch (code) {
      case UNAUTHENTICATED ->
          new BioIdAuthenticationException(errorMessage, exception, "UNAUTHENTICATED");

      case PERMISSION_DENIED ->
          new BioIdAuthenticationException(errorMessage, exception, "PERMISSION_DENIED");

      case INVALID_ARGUMENT -> {
        String bwsErrorCode = extractBwsErrorCode(description);
        if (bwsErrorCode != null && NON_RETRYABLE_BWS_ERRORS.contains(bwsErrorCode)) {
          yield new BioIdValidationException(errorMessage, bwsErrorCode);
        }
        yield new BioIdValidationException(errorMessage, "INVALID_ARGUMENT");
      }

      case NOT_FOUND -> new BioIdValidationException(errorMessage, "TEMPLATE_NOT_FOUND");

      case UNAVAILABLE -> new BioIdServiceException(errorMessage, exception, "SERVICE_UNAVAILABLE");

      case DEADLINE_EXCEEDED ->
          new BioIdServiceException(errorMessage, exception, "REQUEST_TIMEOUT");

      case RESOURCE_EXHAUSTED ->
          new BioIdServiceException(errorMessage, exception, "RATE_LIMIT_EXCEEDED");

      case CANCELLED -> new BioIdServiceException(errorMessage, exception, "REQUEST_CANCELLED");

      case INTERNAL -> {
        String bwsErrorCode = extractBwsErrorCode(description);
        boolean retryable = bwsErrorCode == null || RETRYABLE_BWS_ERRORS.contains(bwsErrorCode);

        if (retryable) {
          yield new BioIdServiceException(errorMessage, exception, "INTERNAL_ERROR", 500);
        } else {
          yield new BioIdValidationException(
              errorMessage, bwsErrorCode != null ? bwsErrorCode : "INTERNAL_ERROR");
        }
      }

      case UNKNOWN -> {
        // Check for HTTP redirect errors (308, 301, 302, etc.)
        if (description != null && description.contains("HTTP status code 308")) {
          yield new BioIdAuthenticationException(
              "BioID service returned HTTP redirect (HTTP 308). This indicates authentication failure. Check BWS_CLIENT_ID and BWS_KEY credentials. Verify endpoint configuration and network connectivity.",
              exception,
              "HTTP_REDIRECT_308");
        }
        yield new BioIdException(errorMessage, exception, "UNKNOWN_ERROR", false, 500);
      }

      default ->
          new BioIdException(
              errorMessage,
              exception,
              code.name(),
              RETRYABLE_GRPC_CODES.contains(code),
              mapStatusCodeToHttp(code));
    };
  }

  /**
   * Maps a general exception to a BioID exception.
   *
   * @param exception the exception to map
   * @param operation the operation that failed
   * @return mapped BioID exception
   */
  public static BioIdException mapGeneralException(Exception exception, String operation) {
    if (exception instanceof BioIdException) {
      return (BioIdException) exception;
    }

    if (exception instanceof StatusRuntimeException) {
      return mapGrpcException((StatusRuntimeException) exception, operation);
    }

    String errorMessage = buildErrorMessage(operation, exception.getMessage());
    logger.debug(
        "Mapping general exception: type={}, message={}, operation={}",
        exception.getClass().getSimpleName(),
        exception.getMessage(),
        operation);

    return new BioIdException(errorMessage, exception, "UNEXPECTED_ERROR", false, 500);
  }

  /**
   * Determines if an exception represents a retryable error condition.
   *
   * @param exception the exception to check
   * @return true if the error is retryable, false otherwise
   */
  public static boolean isRetryable(Exception exception) {
    if (exception instanceof BioIdException bioIdException) {
      return bioIdException.isRetryable();
    }

    if (exception instanceof StatusRuntimeException statusException) {
      Status.Code code = statusException.getStatus().getCode();
      String description = statusException.getStatus().getDescription();

      // Check for specific BWS error codes first (they take precedence)
      String bwsErrorCode = extractBwsErrorCode(description);
      if (bwsErrorCode != null) {
        return RETRYABLE_BWS_ERRORS.contains(bwsErrorCode);
      }

      // Check if it's a retryable gRPC code
      return RETRYABLE_GRPC_CODES.contains(code);
    }

    return false;
  }

  /**
   * Extracts BioID BWS error code from error description if present.
   *
   * @param description the error description
   * @return BWS error code or null if not found
   */
  private static String extractBwsErrorCode(String description) {
    if (description == null || description.trim().isEmpty()) {
      return null;
    }

    // Look for common BWS error patterns
    String lowerDescription = description.toLowerCase();

    // Check for direct BWS error code patterns (e.g., "BWS error: ErrorCode")
    if (lowerDescription.contains("templatenotfound")) {
      return "TemplateNotFound";
    }
    if (lowerDescription.contains("templatecorrupted")) {
      return "TemplateCorrupted";
    }
    if (lowerDescription.contains("nosuitablefaceimage")) {
      return "NoSuitableFaceImage";
    }
    if (lowerDescription.contains("multiplefacesfound")) {
      return "MultipleFacesFound";
    }
    if (lowerDescription.contains("nofeaturevectors")) {
      return "NoFeatureVectors";
    }
    if (lowerDescription.contains("differentfeatureversions")) {
      return "DifferentFeatureVersions";
    }
    if (lowerDescription.contains("poorimagequality")) {
      return "PoorImageQuality";
    }
    if (lowerDescription.contains("serviceunavailable")) {
      return "ServiceUnavailable";
    }
    if (lowerDescription.contains("ratelimitexceeded")) {
      return "RateLimitExceeded";
    }

    // Check for descriptive error patterns
    if (lowerDescription.contains("template not found")) {
      return "TemplateNotFound";
    }
    if (lowerDescription.contains("no suitable face")) {
      return "NoSuitableFaceImage";
    }
    if (lowerDescription.contains("multiple faces")) {
      return "MultipleFacesFound";
    }
    if (lowerDescription.contains("no feature vectors")) {
      return "NoFeatureVectors";
    }
    if (lowerDescription.contains("different feature versions")) {
      return "DifferentFeatureVersions";
    }
    if (lowerDescription.contains("poor image quality")) {
      return "PoorImageQuality";
    }
    if (lowerDescription.contains("service unavailable")) {
      return "ServiceUnavailable";
    }
    if (lowerDescription.contains("rate limit")) {
      return "RateLimitExceeded";
    }

    return null;
  }

  /**
   * Builds a descriptive error message for the exception.
   *
   * @param operation the operation that failed
   * @param description the error description
   * @return formatted error message
   */
  private static String buildErrorMessage(String operation, String description) {
    if (description == null || description.trim().isEmpty()) {
      return String.format("BioID %s operation failed", operation);
    }
    return String.format("BioID %s operation failed: %s", operation, description);
  }

  /**
   * Maps gRPC status codes to HTTP status codes.
   *
   * @param code the gRPC status code
   * @return equivalent HTTP status code
   */
  private static int mapStatusCodeToHttp(Status.Code code) {
    return switch (code) {
      case OK -> 200;
      case CANCELLED -> 499; // Client Closed Request
      case UNKNOWN -> 500;
      case INVALID_ARGUMENT -> 400;
      case DEADLINE_EXCEEDED -> 504; // Gateway Timeout
      case NOT_FOUND -> 404;
      case ALREADY_EXISTS -> 409; // Conflict
      case PERMISSION_DENIED -> 403;
      case RESOURCE_EXHAUSTED -> 429; // Too Many Requests
      case FAILED_PRECONDITION -> 400;
      case ABORTED -> 409; // Conflict
      case OUT_OF_RANGE -> 400;
      case UNIMPLEMENTED -> 501; // Not Implemented
      case INTERNAL -> 500;
      case UNAVAILABLE -> 503; // Service Unavailable
      case DATA_LOSS -> 500;
      case UNAUTHENTICATED -> 401;
      default -> 500;
    };
  }

  /**
   * Gets error classification information for monitoring and logging.
   *
   * @param exception the exception to classify
   * @return error classification details
   */
  public static ErrorClassification classifyError(Exception exception) {
    if (exception instanceof BioIdException bioIdException) {
      return new ErrorClassification(
          bioIdException.getErrorCode(),
          bioIdException.isRetryable(),
          bioIdException.getHttpStatus(),
          determineErrorCategory(bioIdException));
    }

    if (exception instanceof StatusRuntimeException statusException) {
      Status.Code code = statusException.getStatus().getCode();
      String bwsErrorCode = extractBwsErrorCode(statusException.getStatus().getDescription());

      return new ErrorClassification(
          bwsErrorCode != null ? bwsErrorCode : code.name(),
          isRetryable(exception),
          mapStatusCodeToHttp(code),
          determineErrorCategoryFromGrpc(code, bwsErrorCode));
    }

    return new ErrorClassification("UNEXPECTED_ERROR", false, 500, ErrorCategory.SYSTEM);
  }

  private static ErrorCategory determineErrorCategory(BioIdException exception) {
    if (exception instanceof BioIdAuthenticationException) {
      return ErrorCategory.AUTHENTICATION;
    }
    if (exception instanceof BioIdValidationException) {
      return ErrorCategory.VALIDATION;
    }
    if (exception instanceof BioIdServiceException) {
      return ErrorCategory.SERVICE;
    }
    return ErrorCategory.SYSTEM;
  }

  private static ErrorCategory determineErrorCategoryFromGrpc(
      Status.Code code, String bwsErrorCode) {
    return switch (code) {
      case UNAUTHENTICATED, PERMISSION_DENIED -> ErrorCategory.AUTHENTICATION;
      case INVALID_ARGUMENT, NOT_FOUND, FAILED_PRECONDITION, OUT_OF_RANGE ->
          ErrorCategory.VALIDATION;
      case UNAVAILABLE, DEADLINE_EXCEEDED, RESOURCE_EXHAUSTED, ABORTED -> ErrorCategory.SERVICE;
      default -> ErrorCategory.SYSTEM;
    };
  }

  /** Error classification information for monitoring and logging. */
  public record ErrorClassification(
      String errorCode, boolean retryable, int httpStatus, ErrorCategory category) {}

  /** Categories of errors for classification and monitoring. */
  public enum ErrorCategory {
    AUTHENTICATION, // Authentication and authorization errors
    VALIDATION, // Input validation and business rule errors
    SERVICE, // Service availability and network errors
    SYSTEM // Unexpected system errors
  }
}
