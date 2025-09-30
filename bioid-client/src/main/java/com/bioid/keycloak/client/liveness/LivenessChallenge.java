package com.bioid.keycloak.client.liveness;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents a liveness detection challenge for active liveness detection.
 *
 * <p>Defines the type of challenge, instructions for the user, and validation criteria for the
 * response.
 */
public class LivenessChallenge {

  /** Types of liveness challenges. */
  public enum ChallengeType {
    SMILE("Smile", "Please smile for the camera"),
    BLINK("Blink", "Please blink your eyes"),
    NOD("Nod", "Please nod your head"),
    TURN_LEFT("Turn Left", "Please turn your head slightly to the left"),
    TURN_RIGHT("Turn Right", "Please turn your head slightly to the right"),
    OPEN_MOUTH("Open Mouth", "Please open your mouth"),
    RAISE_EYEBROWS("Raise Eyebrows", "Please raise your eyebrows");

    private final String displayName;
    private final String instruction;

    ChallengeType(String displayName, String instruction) {
      this.displayName = displayName;
      this.instruction = instruction;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getInstruction() {
      return instruction;
    }
  }

  private final String challengeId;
  private final ChallengeType type;
  private final String instruction;
  private final Duration timeout;
  private final Instant createdAt;
  private final double requiredConfidence;
  private final int maxAttempts;

  public LivenessChallenge(
      String challengeId,
      ChallengeType type,
      String instruction,
      Duration timeout,
      double requiredConfidence,
      int maxAttempts) {
    this.challengeId = challengeId;
    this.type = type;
    this.instruction = instruction;
    this.timeout = timeout;
    this.createdAt = Instant.now();
    this.requiredConfidence = requiredConfidence;
    this.maxAttempts = maxAttempts;
  }

  /** Creates a default smile challenge. */
  public static LivenessChallenge createSmileChallenge() {
    return new LivenessChallenge(
        java.util.UUID.randomUUID().toString(),
        ChallengeType.SMILE,
        ChallengeType.SMILE.getInstruction(),
        Duration.ofSeconds(10),
        0.7,
        3);
  }

  /** Creates a random challenge from available types. */
  public static LivenessChallenge createRandomChallenge() {
    ChallengeType[] types = ChallengeType.values();
    ChallengeType randomType = types[new java.util.Random().nextInt(types.length)];

    return new LivenessChallenge(
        java.util.UUID.randomUUID().toString(),
        randomType,
        randomType.getInstruction(),
        Duration.ofSeconds(15),
        0.6,
        3);
  }

  /** Creates a challenge for a specific type. */
  public static LivenessChallenge createChallenge(ChallengeType type) {
    return new LivenessChallenge(
        java.util.UUID.randomUUID().toString(),
        type,
        type.getInstruction(),
        Duration.ofSeconds(12),
        0.65,
        3);
  }

  // Getters

  public String getChallengeId() {
    return challengeId;
  }

  public ChallengeType getType() {
    return type;
  }

  public String getInstruction() {
    return instruction;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public double getRequiredConfidence() {
    return requiredConfidence;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  /** Checks if the challenge has expired. */
  public boolean isExpired() {
    return Instant.now().isAfter(createdAt.plus(timeout));
  }

  /** Gets the remaining time for this challenge. */
  public Duration getRemainingTime() {
    Instant expiry = createdAt.plus(timeout);
    Instant now = Instant.now();

    if (now.isAfter(expiry)) {
      return Duration.ZERO;
    }

    return Duration.between(now, expiry);
  }

  /** Validates a challenge response. */
  public boolean validateResponse(double confidence) {
    return confidence >= requiredConfidence && !isExpired();
  }

  @Override
  public String toString() {
    return String.format(
        "LivenessChallenge{id='%s', type=%s, instruction='%s', "
            + "timeout=%s, requiredConfidence=%.2f}",
        challengeId, type, instruction, timeout, requiredConfidence);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    LivenessChallenge that = (LivenessChallenge) obj;
    return challengeId.equals(that.challengeId);
  }

  @Override
  public int hashCode() {
    return challengeId.hashCode();
  }
}
