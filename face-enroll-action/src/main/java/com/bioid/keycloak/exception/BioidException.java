package com.bioid.keycloak.exception;

/**
 * Base exception class for all BioID-related exceptions in the Keycloak extension.
 *
 * <p>This class provides a foundation for structured error handling throughout the extension,
 * allowing for consistent error reporting and user-friendly error messages.
 *
 * @since 1.0.0
 */
public class BioidException extends Exception {

  private final ErrorCode errorCode;
  private final String userMessage;
  private final String technicalDetails;

  /** Error codes for different types of BioID exceptions. */
  public enum ErrorCode {
    // Enrollment errors
    ENROLLMENT_FAILED("ENROLL_001", "Face enrollment failed"),
    INVALID_IMAGE_DATA("ENROLL_002", "Invalid image data provided"),
    MAX_ATTEMPTS_REACHED("ENROLL_003", "Maximum enrollment attempts reached"),
    INSUFFICIENT_IMAGES("ENROLL_004", "Insufficient images for enrollment"),

    // Authentication errors
    AUTHENTICATION_FAILED("AUTH_001", "Face authentication failed"),
    NO_CREDENTIALS_FOUND("AUTH_002", "No face credentials found for user"),
    CREDENTIAL_EXPIRED("AUTH_003", "Face credentials have expired"),

    // System errors
    BIOID_SERVICE_UNAVAILABLE("SYS_001", "BioID service is currently unavailable"),
    CONFIGURATION_ERROR("SYS_002", "System configuration error"),
    DATABASE_ERROR("SYS_003", "Database operation failed"),
    NETWORK_ERROR("SYS_004", "Network communication error"),

    // Validation errors
    INVALID_INPUT("VAL_001", "Invalid input provided"),
    SECURITY_VIOLATION("VAL_002", "Security validation failed"),
    SIZE_LIMIT_EXCEEDED("VAL_003", "Data size limit exceeded");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
      this.code = code;
      this.defaultMessage = defaultMessage;
    }

    public String getCode() {
      return code;
    }

    public String getDefaultMessage() {
      return defaultMessage;
    }
  }

  /**
   * Creates a new BioidException with the specified error code.
   *
   * @param errorCode the error code
   */
  public BioidException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
    this.userMessage = errorCode.getDefaultMessage();
    this.technicalDetails = null;
  }

  /**
   * Creates a new BioidException with the specified error code and user message.
   *
   * @param errorCode the error code
   * @param userMessage user-friendly error message
   */
  public BioidException(ErrorCode errorCode, String userMessage) {
    super(userMessage);
    this.errorCode = errorCode;
    this.userMessage = userMessage;
    this.technicalDetails = null;
  }

  /**
   * Creates a new BioidException with the specified error code, user message, and cause.
   *
   * @param errorCode the error code
   * @param userMessage user-friendly error message
   * @param cause the underlying cause
   */
  public BioidException(ErrorCode errorCode, String userMessage, Throwable cause) {
    super(userMessage, cause);
    this.errorCode = errorCode;
    this.userMessage = userMessage;
    this.technicalDetails = cause != null ? cause.getMessage() : null;
  }

  /**
   * Creates a new BioidException with the specified error code, user message, technical details,
   * and cause.
   *
   * @param errorCode the error code
   * @param userMessage user-friendly error message
   * @param technicalDetails technical details for logging/debugging
   * @param cause the underlying cause
   */
  public BioidException(
      ErrorCode errorCode, String userMessage, String technicalDetails, Throwable cause) {
    super(userMessage, cause);
    this.errorCode = errorCode;
    this.userMessage = userMessage;
    this.technicalDetails = technicalDetails;
  }

  /**
   * Gets the error code associated with this exception.
   *
   * @return the error code
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }

  /**
   * Gets the user-friendly error message.
   *
   * @return the user message
   */
  public String getUserMessage() {
    return userMessage;
  }

  /**
   * Gets the technical details for logging/debugging purposes.
   *
   * @return the technical details, or null if not available
   */
  public String getTechnicalDetails() {
    return technicalDetails;
  }

  /**
   * Returns a formatted string representation of this exception for logging.
   *
   * @return formatted exception details
   */
  public String toLogString() {
    StringBuilder sb = new StringBuilder();
    sb.append("BioidException[")
        .append("code=")
        .append(errorCode.getCode())
        .append(", userMessage='")
        .append(userMessage)
        .append("'");

    if (technicalDetails != null) {
      sb.append(", technicalDetails='").append(technicalDetails).append("'");
    }

    if (getCause() != null) {
      sb.append(", cause=")
          .append(getCause().getClass().getSimpleName())
          .append(": ")
          .append(getCause().getMessage());
    }

    sb.append("]");
    return sb.toString();
  }
}
