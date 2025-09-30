package com.bioid.keycloak.client.exception;

/**
 * Exception for BioID validation errors (invalid input, template not found, poor image quality).
 * These errors are typically non-retryable without correcting the input.
 */
public class BioIdValidationException extends BioIdException {

  public BioIdValidationException(String message) {
    super(message, "INVALID_ARGUMENT", false, 400);
  }

  public BioIdValidationException(String message, String errorCode) {
    super(message, errorCode, false, 400);
  }

  public BioIdValidationException(String message, Throwable cause, String errorCode) {
    super(message, cause, errorCode, false, 400);
  }

  /**
   * Creates a validation exception for template not found scenarios.
   *
   * @param classId the class ID that was not found
   * @return validation exception
   */
  public static BioIdValidationException templateNotFound(long classId) {
    return new BioIdValidationException(
        "Biometric template not found for class ID: " + classId, "TEMPLATE_NOT_FOUND");
  }

  /**
   * Creates a validation exception for poor image quality scenarios.
   *
   * @param reason the quality issue reason
   * @return validation exception
   */
  public static BioIdValidationException poorImageQuality(String reason) {
    return new BioIdValidationException(
        "Image quality insufficient for processing: " + reason, "POOR_IMAGE_QUALITY");
  }
}
