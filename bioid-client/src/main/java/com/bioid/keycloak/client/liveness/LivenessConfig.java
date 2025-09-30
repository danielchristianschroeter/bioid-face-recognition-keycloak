package com.bioid.keycloak.client.liveness;

import java.time.Duration;
import java.util.Set;

/**
 * Configuration for liveness detection operations.
 *
 * <p>Defines the behavior, thresholds, and preferences for liveness detection across different
 * authentication flows and risk levels.
 */
public class LivenessConfig {

  private final boolean enabled;
  private final Set<LivenessMethod> enabledMethods;
  private final LivenessMethod preferredMethod;
  private final double confidenceThreshold;
  private final Duration maxOverhead;
  private final boolean adaptiveMode;
  private final RiskLevel riskThreshold;
  private final boolean fallbackToPassword;
  private final int maxRetries;
  private final Duration challengeTimeout;

  private LivenessConfig(Builder builder) {
    this.enabled = builder.enabled;
    this.enabledMethods = Set.copyOf(builder.enabledMethods);
    this.preferredMethod = builder.preferredMethod;
    this.confidenceThreshold = builder.confidenceThreshold;
    this.maxOverhead = builder.maxOverhead;
    this.adaptiveMode = builder.adaptiveMode;
    this.riskThreshold = builder.riskThreshold;
    this.fallbackToPassword = builder.fallbackToPassword;
    this.maxRetries = builder.maxRetries;
    this.challengeTimeout = builder.challengeTimeout;
  }

  /** Creates a default liveness configuration. */
  public static LivenessConfig defaultConfig() {
    return builder().build();
  }

  /** Creates a configuration for passive liveness only. */
  public static LivenessConfig passiveOnly() {
    return builder()
        .enabledMethods(Set.of(LivenessMethod.PASSIVE))
        .preferredMethod(LivenessMethod.PASSIVE)
        .build();
  }

  /** Creates a configuration for high security scenarios. */
  public static LivenessConfig highSecurity() {
    return builder()
        .enabledMethods(Set.of(LivenessMethod.CHALLENGE_RESPONSE, LivenessMethod.COMBINED))
        .preferredMethod(LivenessMethod.COMBINED)
        .confidenceThreshold(0.8)
        .riskThreshold(RiskLevel.HIGH)
        .adaptiveMode(true)
        .build();
  }

  /** Creates a builder for custom configuration. */
  public static Builder builder() {
    return new Builder();
  }

  // Getters

  public boolean isEnabled() {
    return enabled;
  }

  public Set<LivenessMethod> getEnabledMethods() {
    return enabledMethods;
  }

  public LivenessMethod getPreferredMethod() {
    return preferredMethod;
  }

  public double getConfidenceThreshold() {
    return confidenceThreshold;
  }

  public Duration getMaxOverhead() {
    return maxOverhead;
  }

  public boolean isAdaptiveMode() {
    return adaptiveMode;
  }

  public RiskLevel getRiskThreshold() {
    return riskThreshold;
  }

  public boolean isFallbackToPassword() {
    return fallbackToPassword;
  }

  public int getMaxRetries() {
    return maxRetries;
  }

  public Duration getChallengeTimeout() {
    return challengeTimeout;
  }

  /** Checks if a specific liveness method is enabled. */
  public boolean isMethodEnabled(LivenessMethod method) {
    return enabled && enabledMethods.contains(method);
  }

  /** Gets the appropriate liveness method for a given risk level. */
  public LivenessMethod getMethodForRiskLevel(RiskLevel riskLevel) {
    if (!enabled) {
      return null;
    }

    if (!adaptiveMode) {
      return preferredMethod;
    }

    // Adaptive mode: choose method based on risk level
    if (riskLevel.isAtLeast(RiskLevel.VERY_HIGH)
        && enabledMethods.contains(LivenessMethod.COMBINED)) {
      return LivenessMethod.COMBINED;
    } else if (riskLevel.isAtLeast(RiskLevel.HIGH)
        && enabledMethods.contains(LivenessMethod.CHALLENGE_RESPONSE)) {
      return LivenessMethod.CHALLENGE_RESPONSE;
    } else if (riskLevel.isAtLeast(RiskLevel.MEDIUM)
        && enabledMethods.contains(LivenessMethod.ACTIVE_SMILE)) {
      return LivenessMethod.ACTIVE_SMILE;
    } else if (enabledMethods.contains(LivenessMethod.PASSIVE)) {
      return LivenessMethod.PASSIVE;
    }

    return preferredMethod;
  }

  /** Validates the configuration. */
  public void validate() {
    if (enabled) {
      if (enabledMethods.isEmpty()) {
        throw new IllegalArgumentException("At least one liveness method must be enabled");
      }

      if (preferredMethod != null && !enabledMethods.contains(preferredMethod)) {
        throw new IllegalArgumentException("Preferred method must be in enabled methods");
      }

      if (confidenceThreshold < 0.0 || confidenceThreshold > 1.0) {
        throw new IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0");
      }

      if (maxOverhead.isNegative()) {
        throw new IllegalArgumentException("Max overhead cannot be negative");
      }

      if (maxRetries < 0) {
        throw new IllegalArgumentException("Max retries cannot be negative");
      }

      if (challengeTimeout.isNegative() || challengeTimeout.isZero()) {
        throw new IllegalArgumentException("Challenge timeout must be positive");
      }
    }
  }

  /** Builder for LivenessConfig. */
  public static class Builder {
    private boolean enabled = true;
    private Set<LivenessMethod> enabledMethods = Set.of(LivenessMethod.PASSIVE);
    private LivenessMethod preferredMethod = LivenessMethod.PASSIVE;
    private double confidenceThreshold = 0.5;
    private Duration maxOverhead = Duration.ofMillis(200);
    private boolean adaptiveMode = false;
    private RiskLevel riskThreshold = RiskLevel.MEDIUM;
    private boolean fallbackToPassword = true;
    private int maxRetries = 3;
    private Duration challengeTimeout = Duration.ofSeconds(30);

    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder enabledMethods(Set<LivenessMethod> enabledMethods) {
      this.enabledMethods = enabledMethods;
      return this;
    }

    public Builder preferredMethod(LivenessMethod preferredMethod) {
      this.preferredMethod = preferredMethod;
      return this;
    }

    public Builder confidenceThreshold(double confidenceThreshold) {
      this.confidenceThreshold = confidenceThreshold;
      return this;
    }

    public Builder maxOverhead(Duration maxOverhead) {
      this.maxOverhead = maxOverhead;
      return this;
    }

    public Builder adaptiveMode(boolean adaptiveMode) {
      this.adaptiveMode = adaptiveMode;
      return this;
    }

    public Builder riskThreshold(RiskLevel riskThreshold) {
      this.riskThreshold = riskThreshold;
      return this;
    }

    public Builder fallbackToPassword(boolean fallbackToPassword) {
      this.fallbackToPassword = fallbackToPassword;
      return this;
    }

    public Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public Builder challengeTimeout(Duration challengeTimeout) {
      this.challengeTimeout = challengeTimeout;
      return this;
    }

    public LivenessConfig build() {
      LivenessConfig config = new LivenessConfig(this);
      config.validate();
      return config;
    }
  }

  /** Risk levels for adaptive liveness detection. */
  public enum RiskLevel {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    VERY_HIGH("Very High", 4);

    private final String displayName;
    private final int level;

    RiskLevel(String displayName, int level) {
      this.displayName = displayName;
      this.level = level;
    }

    public String getDisplayName() {
      return displayName;
    }

    public int getLevel() {
      return level;
    }

    public boolean isAtLeast(RiskLevel other) {
      return this.level >= other.level;
    }
  }
}
