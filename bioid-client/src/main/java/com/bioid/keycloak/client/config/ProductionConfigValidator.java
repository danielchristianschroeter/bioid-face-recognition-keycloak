package com.bioid.keycloak.client.config;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates BioID configuration for production readiness.
 *
 * <p>This validator checks for common configuration issues that could cause authentication failures
 * or security problems in production environments.
 */
public class ProductionConfigValidator {

  private static final Logger logger = LoggerFactory.getLogger(ProductionConfigValidator.class);

  /**
   * Validates the BioID configuration for production use.
   *
   * @param config the configuration to validate
   * @return validation result with issues found
   */
  public static ValidationResult validate(BioIdClientConfig config) {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    // Validate required credentials
    validateCredentials(config, errors);

    // Validate endpoint configuration
    validateEndpoint(config, errors, warnings);

    // Validate security settings
    validateSecurity(config, warnings);

    // Validate timeouts and performance settings
    validatePerformance(config, warnings);

    return new ValidationResult(errors, warnings);
  }

  private static void validateCredentials(BioIdClientConfig config, List<String> errors) {
    if (config.clientId() == null || config.clientId().trim().isEmpty()) {
      errors.add("BWS_CLIENT_ID is not configured. This is required for BioID authentication.");
    } else if (config.clientId().length() < 10) {
      errors.add(
          "BWS_CLIENT_ID appears to be too short. Verify it's the correct client ID from BioID portal.");
    }

    if (config.secretKey() == null || config.secretKey().trim().isEmpty()) {
      errors.add("BWS_KEY is not configured. This is required for BioID authentication.");
    } else if (config.secretKey().length() < 16) {
      errors.add(
          "BWS_KEY appears to be too short. Verify it's the correct secret key from BioID portal.");
    }
  }

  private static void validateEndpoint(
      BioIdClientConfig config, List<String> errors, List<String> warnings) {
    String endpoint = config.endpoint();

    if (endpoint == null || endpoint.trim().isEmpty()) {
      errors.add(
          "BWS_ENDPOINT is not configured. Set to appropriate BioID service endpoint (e.g., face.bws-eu.bioid.com).");
      return;
    }

    // Check for common endpoint issues
    if (endpoint.startsWith("http://")) {
      errors.add("BWS_ENDPOINT uses HTTP instead of HTTPS. BioID requires secure connections.");
    } else if (!endpoint.startsWith("grpcs://") && !endpoint.contains(".bioid.com")) {
      warnings.add(
          "BWS_ENDPOINT doesn't appear to be a standard BioID endpoint. Verify the endpoint is correct.");
    }

    // Check for regional endpoints
    if (endpoint.contains("bws-eu.bioid.com")) {
      logger.info("Using EU region endpoint: {}", endpoint);
    } else if (endpoint.contains("bws-us.bioid.com")) {
      logger.info("Using US region endpoint: {}", endpoint);
    } else if (endpoint.contains("bws-sa.bioid.com")) {
      logger.info("Using South America region endpoint: {}", endpoint);
    } else {
      warnings.add(
          "Endpoint region not recognized. Ensure you're using the correct regional endpoint for optimal performance.");
    }
  }

  private static void validateSecurity(BioIdClientConfig config, List<String> warnings) {
    // Check JWT expiration time
    if (config.jwtExpireMinutes() > 120) {
      warnings.add(
          "JWT token expiration is set to "
              + config.jwtExpireMinutes()
              + " minutes. "
              + "Consider using shorter expiration times (60-120 minutes) for better security.");
    }

    if (config.jwtExpireMinutes() < 30) {
      warnings.add(
          "JWT token expiration is set to "
              + config.jwtExpireMinutes()
              + " minutes. "
              + "Very short expiration times may cause frequent re-authentication and performance issues.");
    }
  }

  private static void validatePerformance(BioIdClientConfig config, List<String> warnings) {
    // Check timeout settings
    if (config.verificationTimeout().toSeconds() > 10) {
      warnings.add(
          "Verification timeout is set to "
              + config.verificationTimeout().toSeconds()
              + " seconds. "
              + "Long timeouts may impact user experience.");
    }

    if (config.enrollmentTimeout().toSeconds() > 15) {
      warnings.add(
          "Enrollment timeout is set to "
              + config.enrollmentTimeout().toSeconds()
              + " seconds. "
              + "Long timeouts may impact user experience.");
    }

    // Check retry settings
    if (config.maxRetryAttempts() > 5) {
      warnings.add(
          "Max retry attempts is set to "
              + config.maxRetryAttempts()
              + ". "
              + "Too many retries may cause delays during service issues.");
    }

    if (config.maxRetryAttempts() < 2) {
      warnings.add(
          "Max retry attempts is set to "
              + config.maxRetryAttempts()
              + ". "
              + "Consider allowing at least 2-3 retries for better reliability.");
    }
  }

  /** Result of configuration validation. */
  public static class ValidationResult {
    private final List<String> errors;
    private final List<String> warnings;

    public ValidationResult(List<String> errors, List<String> warnings) {
      this.errors = List.copyOf(errors);
      this.warnings = List.copyOf(warnings);
    }

    public boolean isValid() {
      return errors.isEmpty();
    }

    public List<String> getErrors() {
      return errors;
    }

    public List<String> getWarnings() {
      return warnings;
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    public void logResults() {
      if (!errors.isEmpty()) {
        logger.error("PRODUCTION CONFIGURATION ERRORS found:");
        errors.forEach(error -> logger.error("  - {}", error));
      }

      if (!warnings.isEmpty()) {
        logger.warn("PRODUCTION CONFIGURATION WARNINGS:");
        warnings.forEach(warning -> logger.warn("  - {}", warning));
      }

      if (isValid() && !hasWarnings()) {
        logger.info("BioID configuration validation passed - ready for production use.");
      }
    }
  }
}
