package com.bioid.keycloak.exception;

/**
 * Specialized exception for face enrollment operations.
 *
 * <p>This exception is thrown when face enrollment operations fail, providing specific context
 * about enrollment-related errors.
 *
 * @since 1.0.0
 */
public class EnrollmentException extends BioidException {

  private final int attemptNumber;
  private final int maxAttempts;
  private final String userId;

  /**
   * Creates a new EnrollmentException with the specified error code.
   *
   * @param errorCode the error code
   */
  public EnrollmentException(ErrorCode errorCode) {
    super(errorCode);
    this.attemptNumber = 0;
    this.maxAttempts = 0;
    this.userId = null;
  }

  /**
   * Creates a new EnrollmentException with context information.
   *
   * @param errorCode the error code
   * @param userMessage user-friendly error message
   * @param userId the user ID
   * @param attemptNumber current attempt number
   * @param maxAttempts maximum allowed attempts
   */
  public EnrollmentException(
      ErrorCode errorCode, String userMessage, String userId, int attemptNumber, int maxAttempts) {
    super(errorCode, userMessage);
    this.userId = userId;
    this.attemptNumber = attemptNumber;
    this.maxAttempts = maxAttempts;
  }

  /**
   * Creates a new EnrollmentException with context information and cause.
   *
   * @param errorCode the error code
   * @param userMessage user-friendly error message
   * @param userId the user ID
   * @param attemptNumber current attempt number
   * @param maxAttempts maximum allowed attempts
   * @param cause the underlying cause
   */
  public EnrollmentException(
      ErrorCode errorCode,
      String userMessage,
      String userId,
      int attemptNumber,
      int maxAttempts,
      Throwable cause) {
    super(errorCode, userMessage, cause);
    this.userId = userId;
    this.attemptNumber = attemptNumber;
    this.maxAttempts = maxAttempts;
  }

  /**
   * Gets the current attempt number.
   *
   * @return the attempt number
   */
  public int getAttemptNumber() {
    return attemptNumber;
  }

  /**
   * Gets the maximum allowed attempts.
   *
   * @return the maximum attempts
   */
  public int getMaxAttempts() {
    return maxAttempts;
  }

  /**
   * Gets the user ID associated with this enrollment.
   *
   * @return the user ID
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Checks if the maximum attempts have been reached.
   *
   * @return true if max attempts reached
   */
  public boolean isMaxAttemptsReached() {
    return maxAttempts > 0 && attemptNumber >= maxAttempts;
  }

  /**
   * Gets the remaining attempts.
   *
   * @return the number of remaining attempts, or -1 if unlimited
   */
  public int getRemainingAttempts() {
    if (maxAttempts <= 0) {
      return -1; // Unlimited attempts
    }
    return Math.max(0, maxAttempts - attemptNumber);
  }

  @Override
  public String toLogString() {
    StringBuilder sb = new StringBuilder();
    sb.append("EnrollmentException[")
        .append("code=")
        .append(getErrorCode().getCode())
        .append(", userMessage='")
        .append(getUserMessage())
        .append("'");

    if (userId != null) {
      sb.append(", userId='").append(userId).append("'");
    }

    if (maxAttempts > 0) {
      sb.append(", attempt=").append(attemptNumber).append("/").append(maxAttempts);
    }

    if (getTechnicalDetails() != null) {
      sb.append(", technicalDetails='").append(getTechnicalDetails()).append("'");
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
