package com.bioid.keycloak.client.liveness;

/** Enumeration of liveness detection methods supported by BioID. */
public enum LivenessMethod {

  /**
   * Passive liveness detection.
   *
   * <p>Analyzes a single image for signs of life without requiring user interaction. Uses advanced
   * algorithms to detect spoofing attempts and ensure the subject is alive and present.
   *
   * <p>Characteristics: - No user interaction required - Fast processing (typically < 200ms
   * overhead) - Good for seamless user experience - Moderate security level
   */
  PASSIVE("Passive", "Automatic liveness detection without user interaction", false, 200),

  /**
   * Active liveness detection with smile challenge.
   *
   * <p>Requires the user to perform a specific action (smiling) to prove they are alive and
   * present. Provides higher security than passive detection but requires user cooperation.
   *
   * <p>Characteristics: - Requires user interaction (smile) - Medium processing time (typically <
   * 500ms) - Better security than passive - May impact user experience
   */
  ACTIVE_SMILE("Active Smile", "Liveness detection requiring user to smile", true, 500),

  /**
   * Challenge-response liveness detection with head movement.
   *
   * <p>Presents a random challenge requiring the user to move their head in a specific direction.
   * Provides the highest level of security but has the most impact on user experience.
   *
   * <p>Characteristics: - Requires user interaction (head movement) - Longer processing time
   * (typically < 1000ms) - Highest security level - Most impact on user experience
   */
  CHALLENGE_RESPONSE(
      "Challenge Response", "Liveness detection with head movement challenges", true, 1000),

  /**
   * Combined liveness detection.
   *
   * <p>Uses multiple liveness detection methods in combination for maximum security. May use
   * passive detection first, followed by active detection if needed.
   *
   * <p>Characteristics: - May require user interaction based on risk assessment - Variable
   * processing time - Highest security level - Adaptive user experience
   */
  COMBINED("Combined", "Multiple liveness detection methods combined", true, 1500);

  private final String displayName;
  private final String description;
  private final boolean requiresUserInteraction;
  private final int typicalOverheadMs;

  LivenessMethod(
      String displayName,
      String description,
      boolean requiresUserInteraction,
      int typicalOverheadMs) {
    this.displayName = displayName;
    this.description = description;
    this.requiresUserInteraction = requiresUserInteraction;
    this.typicalOverheadMs = typicalOverheadMs;
  }

  /** Gets the display name of the liveness method. */
  public String getDisplayName() {
    return displayName;
  }

  /** Gets the description of the liveness method. */
  public String getDescription() {
    return description;
  }

  /** Checks if this method requires user interaction. */
  public boolean requiresUserInteraction() {
    return requiresUserInteraction;
  }

  /** Gets the typical processing overhead in milliseconds. */
  public int getTypicalOverheadMs() {
    return typicalOverheadMs;
  }

  /** Gets the typical processing overhead as a Duration. */
  public java.time.Duration getTypicalOverhead() {
    return java.time.Duration.ofMillis(typicalOverheadMs);
  }

  /** Checks if this method is suitable for seamless user experience. */
  public boolean isSeamless() {
    return !requiresUserInteraction && typicalOverheadMs <= 200;
  }

  /** Gets the security level of this liveness method. */
  public SecurityLevel getSecurityLevel() {
    switch (this) {
      case PASSIVE:
        return SecurityLevel.MODERATE;
      case ACTIVE_SMILE:
        return SecurityLevel.HIGH;
      case CHALLENGE_RESPONSE:
      case COMBINED:
        return SecurityLevel.VERY_HIGH;
      default:
        return SecurityLevel.LOW;
    }
  }

  /** Security levels for liveness detection methods. */
  public enum SecurityLevel {
    LOW("Low", 1),
    MODERATE("Moderate", 2),
    HIGH("High", 3),
    VERY_HIGH("Very High", 4);

    private final String displayName;
    private final int level;

    SecurityLevel(String displayName, int level) {
      this.displayName = displayName;
      this.level = level;
    }

    public String getDisplayName() {
      return displayName;
    }

    public int getLevel() {
      return level;
    }

    public boolean isAtLeast(SecurityLevel other) {
      return this.level >= other.level;
    }
  }
}
