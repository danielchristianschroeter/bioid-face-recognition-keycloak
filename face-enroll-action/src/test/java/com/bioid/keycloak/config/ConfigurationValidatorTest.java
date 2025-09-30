package com.bioid.keycloak.config;

import static org.junit.jupiter.api.Assertions.*;

import com.bioid.keycloak.exception.BioidException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ConfigurationValidator.
 *
 * <p>These tests verify configuration validation logic and error handling.
 *
 * @since 1.0.0
 */
@DisplayName("Configuration Validator Tests")
class ConfigurationValidatorTest {

  private static final String TEST_KEY = "test.config.key";

  @BeforeEach
  void setUp() {
    // Clear any existing system properties
    System.clearProperty(TEST_KEY);
  }

  @AfterEach
  void tearDown() {
    // Clean up system properties
    System.clearProperty(TEST_KEY);
  }

  @Test
  @DisplayName("Should return default value when property not set")
  void shouldReturnDefaultValueWhenPropertyNotSet() throws BioidException {
    // When
    int result = ConfigurationValidator.getValidatedIntConfig(TEST_KEY, 42, 1, 100);

    // Then
    assertEquals(42, result, "Should return default value when property not set");
  }

  @Test
  @DisplayName("Should return configured value when valid")
  void shouldReturnConfiguredValueWhenValid() throws BioidException {
    // Given
    System.setProperty(TEST_KEY, "75");

    // When
    int result = ConfigurationValidator.getValidatedIntConfig(TEST_KEY, 42, 1, 100);

    // Then
    assertEquals(75, result, "Should return configured value when valid");
  }

  @Test
  @DisplayName("Should throw exception when value below minimum")
  void shouldThrowExceptionWhenValueBelowMinimum() {
    // Given
    System.setProperty(TEST_KEY, "0");

    // When & Then
    BioidException exception =
        assertThrows(
            BioidException.class,
            () -> {
              ConfigurationValidator.getValidatedIntConfig(TEST_KEY, 42, 1, 100);
            });

    assertEquals(BioidException.ErrorCode.CONFIGURATION_ERROR, exception.getErrorCode());
    assertTrue(exception.getUserMessage().contains("outside valid range"));
  }

  @Test
  @DisplayName("Should throw exception when value above maximum")
  void shouldThrowExceptionWhenValueAboveMaximum() {
    // Given
    System.setProperty(TEST_KEY, "150");

    // When & Then
    BioidException exception =
        assertThrows(
            BioidException.class,
            () -> {
              ConfigurationValidator.getValidatedIntConfig(TEST_KEY, 42, 1, 100);
            });

    assertEquals(BioidException.ErrorCode.CONFIGURATION_ERROR, exception.getErrorCode());
    assertTrue(exception.getUserMessage().contains("outside valid range"));
  }

  @Test
  @DisplayName("Should throw exception when value is not a number")
  void shouldThrowExceptionWhenValueIsNotANumber() {
    // Given
    System.setProperty(TEST_KEY, "not-a-number");

    // When & Then
    BioidException exception =
        assertThrows(
            BioidException.class,
            () -> {
              ConfigurationValidator.getValidatedIntConfig(TEST_KEY, 42, 1, 100);
            });

    assertEquals(BioidException.ErrorCode.CONFIGURATION_ERROR, exception.getErrorCode());
    assertTrue(exception.getUserMessage().contains("Invalid integer value"));
  }

  @Test
  @DisplayName("Should handle empty string value")
  void shouldHandleEmptyStringValue() throws BioidException {
    // Given
    System.setProperty(TEST_KEY, "");

    // When
    int result = ConfigurationValidator.getValidatedIntConfig(TEST_KEY, 42, 1, 100);

    // Then
    assertEquals(42, result, "Should return default value for empty string");
  }

  @Test
  @DisplayName("Should handle whitespace-only value")
  void shouldHandleWhitespaceOnlyValue() throws BioidException {
    // Given
    System.setProperty(TEST_KEY, "   ");

    // When
    int result = ConfigurationValidator.getValidatedIntConfig(TEST_KEY, 42, 1, 100);

    // Then
    assertEquals(42, result, "Should return default value for whitespace-only string");
  }

  @Test
  @DisplayName("Should trim whitespace from valid value")
  void shouldTrimWhitespaceFromValidValue() throws BioidException {
    // Given
    System.setProperty(TEST_KEY, "  75  ");

    // When
    int result = ConfigurationValidator.getValidatedIntConfig(TEST_KEY, 42, 1, 100);

    // Then
    assertEquals(75, result, "Should trim whitespace and return valid value");
  }

  @Test
  @DisplayName("Should validate configuration successfully with defaults")
  void shouldValidateConfigurationSuccessfullyWithDefaults() {
    // When
    ConfigurationValidator.ValidationResult result = ConfigurationValidator.validateConfiguration();

    // Then
    assertNotNull(result, "Validation result should not be null");
    assertTrue(result.isValid(), "Configuration should be valid with defaults");
    assertFalse(result.hasErrors(), "Should not have errors with defaults");
  }

  @Test
  @DisplayName("Should detect configuration errors")
  void shouldDetectConfigurationErrors() {
    // Given - Set invalid configuration values
    System.setProperty("bioid.enrollment.min.frames", "0"); // Below minimum
    System.setProperty("bioid.enrollment.max.attempts", "20"); // Above maximum

    try {
      // When
      ConfigurationValidator.ValidationResult result =
          ConfigurationValidator.validateConfiguration();

      // Then
      assertNotNull(result, "Validation result should not be null");
      assertFalse(result.isValid(), "Configuration should be invalid");
      assertTrue(result.hasErrors(), "Should have errors");
      assertFalse(result.getErrors().isEmpty(), "Error list should not be empty");

    } finally {
      // Clean up
      System.clearProperty("bioid.enrollment.min.frames");
      System.clearProperty("bioid.enrollment.max.attempts");
    }
  }

  @Test
  @DisplayName("Should detect configuration warnings")
  void shouldDetectConfigurationWarnings() {
    // Given - Set values that trigger warnings
    System.setProperty("bioid.enrollment.min.frames", "1"); // Below recommended
    System.setProperty("bioid.image.max.size.mb", "25"); // Large size

    try {
      // When
      ConfigurationValidator.ValidationResult result =
          ConfigurationValidator.validateConfiguration();

      // Then
      assertNotNull(result, "Validation result should not be null");
      assertTrue(result.isValid(), "Configuration should still be valid");
      assertTrue(result.hasWarnings(), "Should have warnings");
      assertFalse(result.getWarnings().isEmpty(), "Warning list should not be empty");

    } finally {
      // Clean up
      System.clearProperty("bioid.enrollment.min.frames");
      System.clearProperty("bioid.image.max.size.mb");
    }
  }

  @Test
  @DisplayName("Should handle validation result toString")
  void shouldHandleValidationResultToString() {
    // When
    ConfigurationValidator.ValidationResult result = ConfigurationValidator.validateConfiguration();

    // Then
    String toString = result.toString();
    assertNotNull(toString, "toString should not be null");
    assertTrue(toString.contains("ValidationResult"), "toString should contain class name");
    assertTrue(toString.contains("errors="), "toString should contain error count");
    assertTrue(toString.contains("warnings="), "toString should contain warning count");
  }

  @Test
  @DisplayName("Should handle validation result collections")
  void shouldHandleValidationResultCollections() {
    // Given
    ConfigurationValidator.ValidationResult result = new ConfigurationValidator.ValidationResult();

    // When
    result.addError("Test error");
    result.addWarning("Test warning");

    // Then
    assertEquals(1, result.getErrors().size(), "Should have one error");
    assertEquals(1, result.getWarnings().size(), "Should have one warning");
    assertTrue(result.hasErrors(), "Should have errors");
    assertTrue(result.hasWarnings(), "Should have warnings");
    assertFalse(result.isValid(), "Should not be valid with errors");

    // Verify collections are defensive copies
    result.getErrors().clear();
    result.getWarnings().clear();
    assertEquals(1, result.getErrors().size(), "Original error list should be unchanged");
    assertEquals(1, result.getWarnings().size(), "Original warning list should be unchanged");
  }
}
