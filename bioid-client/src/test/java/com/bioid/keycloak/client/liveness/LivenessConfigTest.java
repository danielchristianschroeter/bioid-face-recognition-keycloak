package com.bioid.keycloak.client.liveness;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for LivenessConfig. */
class LivenessConfigTest {

  @Test
  @DisplayName("Should create default configuration")
  void shouldCreateDefaultConfiguration() {
    LivenessConfig config = LivenessConfig.defaultConfig();

    assertTrue(config.isEnabled());
    assertTrue(config.getEnabledMethods().contains(LivenessMethod.PASSIVE));
    assertEquals(LivenessMethod.PASSIVE, config.getPreferredMethod());
    assertEquals(0.5, config.getConfidenceThreshold());
    assertEquals(Duration.ofMillis(200), config.getMaxOverhead());
    assertFalse(config.isAdaptiveMode());
    assertTrue(config.isFallbackToPassword());
    assertEquals(3, config.getMaxRetries());
  }

  @Test
  @DisplayName("Should create passive-only configuration")
  void shouldCreatePassiveOnlyConfiguration() {
    LivenessConfig config = LivenessConfig.passiveOnly();

    assertTrue(config.isEnabled());
    assertEquals(1, config.getEnabledMethods().size());
    assertTrue(config.isMethodEnabled(LivenessMethod.PASSIVE));
    assertFalse(config.isMethodEnabled(LivenessMethod.ACTIVE_SMILE));
    assertEquals(LivenessMethod.PASSIVE, config.getPreferredMethod());
  }

  @Test
  @DisplayName("Should create high security configuration")
  void shouldCreateHighSecurityConfiguration() {
    LivenessConfig config = LivenessConfig.highSecurity();

    assertTrue(config.isEnabled());
    assertTrue(config.isMethodEnabled(LivenessMethod.CHALLENGE_RESPONSE));
    assertTrue(config.isMethodEnabled(LivenessMethod.COMBINED));
    assertEquals(LivenessMethod.COMBINED, config.getPreferredMethod());
    assertEquals(0.8, config.getConfidenceThreshold());
    assertEquals(LivenessConfig.RiskLevel.HIGH, config.getRiskThreshold());
    assertTrue(config.isAdaptiveMode());
  }

  @Test
  @DisplayName("Should build custom configuration")
  void shouldBuildCustomConfiguration() {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.ACTIVE_SMILE, LivenessMethod.PASSIVE))
            .preferredMethod(LivenessMethod.ACTIVE_SMILE)
            .confidenceThreshold(0.7)
            .maxOverhead(Duration.ofMillis(500))
            .adaptiveMode(true)
            .riskThreshold(LivenessConfig.RiskLevel.HIGH)
            .fallbackToPassword(false)
            .maxRetries(5)
            .challengeTimeout(Duration.ofSeconds(45))
            .build();

    assertTrue(config.isEnabled());
    assertEquals(2, config.getEnabledMethods().size());
    assertTrue(config.isMethodEnabled(LivenessMethod.ACTIVE_SMILE));
    assertTrue(config.isMethodEnabled(LivenessMethod.PASSIVE));
    assertEquals(LivenessMethod.ACTIVE_SMILE, config.getPreferredMethod());
    assertEquals(0.7, config.getConfidenceThreshold());
    assertEquals(Duration.ofMillis(500), config.getMaxOverhead());
    assertTrue(config.isAdaptiveMode());
    assertEquals(LivenessConfig.RiskLevel.HIGH, config.getRiskThreshold());
    assertFalse(config.isFallbackToPassword());
    assertEquals(5, config.getMaxRetries());
    assertEquals(Duration.ofSeconds(45), config.getChallengeTimeout());
  }

  @Test
  @DisplayName("Should validate configuration correctly")
  void shouldValidateConfigurationCorrectly() {
    // Valid configuration should not throw
    assertDoesNotThrow(
        () -> {
          LivenessConfig.builder()
              .enabled(true)
              .enabledMethods(Set.of(LivenessMethod.PASSIVE))
              .preferredMethod(LivenessMethod.PASSIVE)
              .confidenceThreshold(0.5)
              .maxOverhead(Duration.ofMillis(200))
              .maxRetries(3)
              .challengeTimeout(Duration.ofSeconds(30))
              .build();
        });

    // Empty enabled methods should throw
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LivenessConfig.builder().enabled(true).enabledMethods(Set.of()).build();
        });

    // Preferred method not in enabled methods should throw
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LivenessConfig.builder()
              .enabled(true)
              .enabledMethods(Set.of(LivenessMethod.PASSIVE))
              .preferredMethod(LivenessMethod.ACTIVE_SMILE)
              .build();
        });

    // Invalid confidence threshold should throw
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LivenessConfig.builder()
              .enabled(true)
              .enabledMethods(Set.of(LivenessMethod.PASSIVE))
              .confidenceThreshold(-0.1)
              .build();
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LivenessConfig.builder()
              .enabled(true)
              .enabledMethods(Set.of(LivenessMethod.PASSIVE))
              .confidenceThreshold(1.1)
              .build();
        });

    // Negative max overhead should throw
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LivenessConfig.builder()
              .enabled(true)
              .enabledMethods(Set.of(LivenessMethod.PASSIVE))
              .maxOverhead(Duration.ofMillis(-100))
              .build();
        });

    // Negative max retries should throw
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LivenessConfig.builder()
              .enabled(true)
              .enabledMethods(Set.of(LivenessMethod.PASSIVE))
              .maxRetries(-1)
              .build();
        });

    // Zero or negative challenge timeout should throw
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          LivenessConfig.builder()
              .enabled(true)
              .enabledMethods(Set.of(LivenessMethod.PASSIVE))
              .challengeTimeout(Duration.ZERO)
              .build();
        });
  }

  @Test
  @DisplayName("Should get appropriate method for risk level in adaptive mode")
  void shouldGetAppropriateMethodForRiskLevelInAdaptiveMode() {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(
                Set.of(
                    LivenessMethod.PASSIVE,
                    LivenessMethod.ACTIVE_SMILE,
                    LivenessMethod.CHALLENGE_RESPONSE,
                    LivenessMethod.COMBINED))
            .preferredMethod(LivenessMethod.PASSIVE)
            .adaptiveMode(true)
            .build();

    // Very high risk should use combined method
    assertEquals(
        LivenessMethod.COMBINED, config.getMethodForRiskLevel(LivenessConfig.RiskLevel.VERY_HIGH));

    // High risk should use challenge-response
    assertEquals(
        LivenessMethod.CHALLENGE_RESPONSE,
        config.getMethodForRiskLevel(LivenessConfig.RiskLevel.HIGH));

    // Medium risk should use active smile
    assertEquals(
        LivenessMethod.ACTIVE_SMILE, config.getMethodForRiskLevel(LivenessConfig.RiskLevel.MEDIUM));

    // Low risk should use passive
    assertEquals(
        LivenessMethod.PASSIVE, config.getMethodForRiskLevel(LivenessConfig.RiskLevel.LOW));
  }

  @Test
  @DisplayName("Should return preferred method in non-adaptive mode")
  void shouldReturnPreferredMethodInNonAdaptiveMode() {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.PASSIVE, LivenessMethod.ACTIVE_SMILE))
            .preferredMethod(LivenessMethod.ACTIVE_SMILE)
            .adaptiveMode(false)
            .build();

    // Should always return preferred method regardless of risk level
    assertEquals(
        LivenessMethod.ACTIVE_SMILE, config.getMethodForRiskLevel(LivenessConfig.RiskLevel.LOW));
    assertEquals(
        LivenessMethod.ACTIVE_SMILE,
        config.getMethodForRiskLevel(LivenessConfig.RiskLevel.VERY_HIGH));
  }

  @Test
  @DisplayName("Should return null method when disabled")
  void shouldReturnNullMethodWhenDisabled() {
    LivenessConfig config = LivenessConfig.builder().enabled(false).build();

    assertNull(config.getMethodForRiskLevel(LivenessConfig.RiskLevel.HIGH));
  }

  @Test
  @DisplayName("Should check method enabled correctly")
  void shouldCheckMethodEnabledCorrectly() {
    LivenessConfig config =
        LivenessConfig.builder()
            .enabled(true)
            .enabledMethods(Set.of(LivenessMethod.PASSIVE, LivenessMethod.ACTIVE_SMILE))
            .build();

    assertTrue(config.isMethodEnabled(LivenessMethod.PASSIVE));
    assertTrue(config.isMethodEnabled(LivenessMethod.ACTIVE_SMILE));
    assertFalse(config.isMethodEnabled(LivenessMethod.CHALLENGE_RESPONSE));
    assertFalse(config.isMethodEnabled(LivenessMethod.COMBINED));

    // When disabled, no methods should be enabled
    LivenessConfig disabledConfig =
        LivenessConfig.builder()
            .enabled(false)
            .enabledMethods(Set.of(LivenessMethod.PASSIVE))
            .build();

    assertFalse(disabledConfig.isMethodEnabled(LivenessMethod.PASSIVE));
  }

  @Test
  @DisplayName("Should handle risk level comparisons")
  void shouldHandleRiskLevelComparisons() {
    assertTrue(LivenessConfig.RiskLevel.HIGH.isAtLeast(LivenessConfig.RiskLevel.MEDIUM));
    assertTrue(LivenessConfig.RiskLevel.VERY_HIGH.isAtLeast(LivenessConfig.RiskLevel.HIGH));
    assertFalse(LivenessConfig.RiskLevel.LOW.isAtLeast(LivenessConfig.RiskLevel.MEDIUM));
    assertTrue(LivenessConfig.RiskLevel.MEDIUM.isAtLeast(LivenessConfig.RiskLevel.MEDIUM));

    assertEquals("Low", LivenessConfig.RiskLevel.LOW.getDisplayName());
    assertEquals(1, LivenessConfig.RiskLevel.LOW.getLevel());
    assertEquals(4, LivenessConfig.RiskLevel.VERY_HIGH.getLevel());
  }
}
