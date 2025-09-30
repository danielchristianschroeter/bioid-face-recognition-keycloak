package com.bioid.keycloak.client.exception;

import java.io.Serial;

/**
 * Exception for template-related errors in BioID BWS operations. These occur when templates are not
 * found, corrupted, or have version mismatches.
 */
public final class BioIdTemplateException extends BioIdException {

  @Serial private static final long serialVersionUID = 1L;

  public BioIdTemplateException(String message, String errorCode, boolean retryable) {
    super(message, errorCode, retryable, 404);
  }

  public BioIdTemplateException(
      String message, String errorCode, boolean retryable, Throwable cause) {
    super(message, cause, errorCode, retryable, 404);
  }

  // Factory methods for common template errors
  public static BioIdTemplateException templateNotFound(long classId) {
    return new BioIdTemplateException(
        String.format("Biometric template not found for class ID: %d", classId),
        "TemplateNotFound",
        false // Template doesn't exist, requires enrollment
        );
  }

  public static BioIdTemplateException templateCorrupted(long classId, String details) {
    return new BioIdTemplateException(
        String.format("Biometric template corrupted for class ID %d: %s", classId, details),
        "TemplateCorrupted",
        false // Corrupted template requires re-enrollment
        );
  }

  public static BioIdTemplateException templateExpired(long classId) {
    return new BioIdTemplateException(
        String.format("Biometric template expired for class ID: %d", classId),
        "TemplateExpired",
        false // Expired template requires re-enrollment
        );
  }

  public static BioIdTemplateException templateVersionMismatch(
      long classId, int currentVersion, int requiredVersion) {
    return new BioIdTemplateException(
        String.format(
            "Template version mismatch for class ID %d: current=%d, required=%d",
            classId, currentVersion, requiredVersion),
        "TemplateVersionMismatch",
        false // Version mismatch requires template upgrade
        );
  }

  public static BioIdTemplateException templateInUse(long classId) {
    return new BioIdTemplateException(
        String.format("Template is currently in use and cannot be deleted: %d", classId),
        "TemplateInUse",
        true // Can retry deletion later
        );
  }
}
