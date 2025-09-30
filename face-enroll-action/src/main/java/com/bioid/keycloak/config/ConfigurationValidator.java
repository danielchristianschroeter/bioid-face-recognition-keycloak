package com.bioid.keycloak.config;

import com.bioid.keycloak.exception.BioidException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for validating BioID extension configuration.
 *
 * <p>This class provides comprehensive validation of configuration parameters to ensure the
 * extension is properly configured before use.
 *
 * @since 1.0.0
 */
public final class ConfigurationValidator {

  private static final Logger logger = LoggerFactory.getLogger(ConfigurationValidator.class);

  // Configuration keys
  private static final String BIOID_ENROLLMENT_MIN_FRAMES = "bioid.enrollment.min.frames";
  private static final String BIOID_ENROLLMENT_MAX_ATTEMPTS = "bioid.enrollment.max.attempts";
  private static final String BIOID_IMAGE_MAX_SIZE_MB = "bioid.image.max.size.mb";
  private static final String BIOID_SESSION_STATE_MAX_SIZE = "bioid.session.state.max.size";
  private static final String BIOID_CREDENTIAL_TTL_DAYS = "bioid.credential.ttl.days";

  // Default values
  private static final int DEFAULT_MIN_FRAMES = 3;
  private static final int DEFAULT_MAX_ATTEMPTS = 3;
  private static final int DEFAULT_MAX_IMAGE_SIZE_MB = 10;
  private static final int DEFAULT_SESSION_STATE_MAX_SIZE = 10000;
  private static final int DEFAULT_CREDENTIAL_TTL_DAYS = 730;

  // Validation ranges
  private static final int MIN_FRAMES_RANGE_MIN = 1;
  private static final int MIN_FRAMES_RANGE_MAX = 10;
  private static final int MAX_ATTEMPTS_RANGE_MIN = 1;
  private static final int MAX_ATTEMPTS_RANGE_MAX = 10;
  private static final int MAX_IMAGE_SIZE_RANGE_MIN = 1;
  private static final int MAX_IMAGE_SIZE_RANGE_MAX = 50;
  private static final int SESSION_STATE_SIZE_RANGE_MIN = 1000;
  private static final int SESSION_STATE_SIZE_RANGE_MAX = 100000;
  private static final int CREDENTIAL_TTL_RANGE_MIN = 1;
  private static final int CREDENTIAL_TTL_RANGE_MAX = 3650; // 10 years

  private ConfigurationValidator() {
    // Utility class
  }

  /**
   * Validates the current system configuration.
   *
   * @return validation result containing any errors or warnings
   */
  public static ValidationResult validateConfiguration() {
    ValidationResult result = new ValidationResult();

    try {
      validateMinFrames(result);
      validateMaxAttempts(result);
      validateMaxImageSize(result);
      validateSessionStateSize(result);
      validateCredentialTtl(result);

      if (result.hasErrors()) {
        logger.error("Configuration validation failed with {} errors", result.getErrors().size());
      } else if (result.hasWarnings()) {
        logger.warn(
            "Configuration validation completed with {} warnings", result.getWarnings().size());
      } else {
        logger.info("Configuration validation passed successfully");
      }

    } catch (Exception e) {
      logger.error("Unexpected error during configuration validation", e);
      result.addError("Unexpected error during validation: " + e.getMessage());
    }

    return result;
  }

  /**
   * Gets a validated integer configuration value.
   *
   * @param key the configuration key
   * @param defaultValue the default value
   * @param minValue minimum allowed value
   * @param maxValue maximum allowed value
   * @return the validated configuration value
   * @throws BioidException if validation fails
   */
  public static int getValidatedIntConfig(String key, int defaultValue, int minValue, int maxValue)
      throws BioidException {
    String value = System.getProperty(key);

    if (value == null || value.trim().isEmpty()) {
      logger.debug("Using default value {} for configuration key: {}", defaultValue, key);
      return defaultValue;
    }

    try {
      int intValue = Integer.parseInt(value.trim());

      if (intValue < minValue || intValue > maxValue) {
        throw new BioidException(
            BioidException.ErrorCode.CONFIGURATION_ERROR,
            String.format(
                "Configuration value for %s (%d) is outside valid range [%d, %d]",
                key, intValue, minValue, maxValue));
      }

      logger.debug("Using configured value {} for key: {}", intValue, key);
      return intValue;

    } catch (NumberFormatException e) {
      throw new BioidException(
          BioidException.ErrorCode.CONFIGURATION_ERROR,
          String.format("Invalid integer value '%s' for configuration key: %s", value, key),
          e);
    }
  }

  private static void validateMinFrames(ValidationResult result) {
    try {
      int value =
          getValidatedIntConfig(
              BIOID_ENROLLMENT_MIN_FRAMES,
              DEFAULT_MIN_FRAMES,
              MIN_FRAMES_RANGE_MIN,
              MIN_FRAMES_RANGE_MAX);
      if (value < 3) {
        result.addWarning("Minimum frames setting (" + value + ") is below recommended value of 3");
      }
    } catch (BioidException e) {
      result.addError("Min frames validation failed: " + e.getUserMessage());
    }
  }

  private static void validateMaxAttempts(ValidationResult result) {
    try {
      getValidatedIntConfig(
          BIOID_ENROLLMENT_MAX_ATTEMPTS,
          DEFAULT_MAX_ATTEMPTS,
          MAX_ATTEMPTS_RANGE_MIN,
          MAX_ATTEMPTS_RANGE_MAX);
    } catch (BioidException e) {
      result.addError("Max attempts validation failed: " + e.getUserMessage());
    }
  }

  private static void validateMaxImageSize(ValidationResult result) {
    try {
      int value =
          getValidatedIntConfig(
              BIOID_IMAGE_MAX_SIZE_MB,
              DEFAULT_MAX_IMAGE_SIZE_MB,
              MAX_IMAGE_SIZE_RANGE_MIN,
              MAX_IMAGE_SIZE_RANGE_MAX);
      if (value > 20) {
        result.addWarning(
            "Maximum image size (" + value + "MB) is quite large and may impact performance");
      }
    } catch (BioidException e) {
      result.addError("Max image size validation failed: " + e.getUserMessage());
    }
  }

  private static void validateSessionStateSize(ValidationResult result) {
    try {
      getValidatedIntConfig(
          BIOID_SESSION_STATE_MAX_SIZE,
          DEFAULT_SESSION_STATE_MAX_SIZE,
          SESSION_STATE_SIZE_RANGE_MIN,
          SESSION_STATE_SIZE_RANGE_MAX);
    } catch (BioidException e) {
      result.addError("Session state size validation failed: " + e.getUserMessage());
    }
  }

  private static void validateCredentialTtl(ValidationResult result) {
    try {
      int value =
          getValidatedIntConfig(
              BIOID_CREDENTIAL_TTL_DAYS,
              DEFAULT_CREDENTIAL_TTL_DAYS,
              CREDENTIAL_TTL_RANGE_MIN,
              CREDENTIAL_TTL_RANGE_MAX);
      if (value < 30) {
        result.addWarning(
            "Credential TTL ("
                + value
                + " days) is quite short and may require frequent re-enrollment");
      }
    } catch (BioidException e) {
      result.addError("Credential TTL validation failed: " + e.getUserMessage());
    }
  }

  /** Result of configuration validation. */
  public static class ValidationResult {
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addError(String error) {
      errors.add(error);
    }

    public void addWarning(String warning) {
      warnings.add(warning);
    }

    public List<String> getErrors() {
      return new ArrayList<>(errors);
    }

    public List<String> getWarnings() {
      return new ArrayList<>(warnings);
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    public boolean isValid() {
      return !hasErrors();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("ValidationResult[");
      sb.append("errors=").append(errors.size());
      sb.append(", warnings=").append(warnings.size());
      sb.append("]");
      return sb.toString();
    }
  }
}
