package com.bioid.keycloak.client.liveness;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Represents a movement-based challenge for challenge-response liveness detection.
 *
 * <p>Defines a sequence of head movements that the user must perform to prove they are alive and
 * present.
 */
public class MovementChallenge {

  /** Types of head movements for challenges. */
  public enum MovementType {
    TURN_LEFT("Turn Left", "Turn your head to the left"),
    TURN_RIGHT("Turn Right", "Turn your head to the right"),
    TURN_UP("Look Up", "Look up slightly"),
    TURN_DOWN("Look Down", "Look down slightly"),
    NOD_UP("Nod Up", "Nod your head up"),
    NOD_DOWN("Nod Down", "Nod your head down"),
    TILT_LEFT("Tilt Left", "Tilt your head to the left"),
    TILT_RIGHT("Tilt Right", "Tilt your head to the right");

    private final String displayName;
    private final String instruction;

    MovementType(String displayName, String instruction) {
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

  /** A single movement step in the challenge sequence. */
  public static class MovementStep {
    private final MovementType type;
    private final Duration duration;
    private final double requiredAngle;
    private final String instruction;

    public MovementStep(MovementType type, Duration duration, double requiredAngle) {
      this.type = type;
      this.duration = duration;
      this.requiredAngle = requiredAngle;
      this.instruction = type.getInstruction();
    }

    public MovementType getType() {
      return type;
    }

    public Duration getDuration() {
      return duration;
    }

    public double getRequiredAngle() {
      return requiredAngle;
    }

    public String getInstruction() {
      return instruction;
    }

    @Override
    public String toString() {
      return String.format(
          "MovementStep{type=%s, duration=%s, angle=%.1f°}", type, duration, requiredAngle);
    }
  }

  private final String challengeId;
  private final List<MovementStep> steps;
  private final Duration totalTimeout;
  private final Instant createdAt;
  private final double requiredPrecision;
  private final int maxAttempts;

  public MovementChallenge(
      String challengeId,
      List<MovementStep> steps,
      Duration totalTimeout,
      double requiredPrecision,
      int maxAttempts) {
    this.challengeId = challengeId;
    this.steps = List.copyOf(steps);
    this.totalTimeout = totalTimeout;
    this.createdAt = Instant.now();
    this.requiredPrecision = requiredPrecision;
    this.maxAttempts = maxAttempts;
  }

  /** Creates a simple single-movement challenge. */
  public static MovementChallenge createSimpleChallenge() {
    MovementType[] types = {
      MovementType.TURN_LEFT, MovementType.TURN_RIGHT, MovementType.TURN_UP, MovementType.TURN_DOWN
    };
    MovementType randomType = types[new java.util.Random().nextInt(types.length)];

    MovementStep step = new MovementStep(randomType, Duration.ofSeconds(2), 15.0);

    return new MovementChallenge(
        java.util.UUID.randomUUID().toString(), List.of(step), Duration.ofSeconds(15), 0.7, 3);
  }

  /** Creates a complex multi-step challenge. */
  public static MovementChallenge createComplexChallenge() {
    List<MovementStep> steps =
        List.of(
            new MovementStep(MovementType.TURN_LEFT, Duration.ofSeconds(2), 20.0),
            new MovementStep(MovementType.TURN_RIGHT, Duration.ofSeconds(2), 20.0),
            new MovementStep(MovementType.NOD_UP, Duration.ofSeconds(1), 10.0));

    return new MovementChallenge(
        java.util.UUID.randomUUID().toString(), steps, Duration.ofSeconds(25), 0.8, 2);
  }

  /** Creates a random movement challenge. */
  public static MovementChallenge createRandomChallenge() {
    java.util.Random random = new java.util.Random();
    MovementType[] types = MovementType.values();

    int stepCount = 1 + random.nextInt(3); // 1-3 steps
    List<MovementStep> steps = new java.util.ArrayList<>();

    for (int i = 0; i < stepCount; i++) {
      MovementType type = types[random.nextInt(types.length)];
      Duration duration = Duration.ofSeconds(1 + random.nextInt(3)); // 1-3 seconds
      double angle = 10.0 + random.nextDouble() * 20.0; // 10-30 degrees

      steps.add(new MovementStep(type, duration, angle));
    }

    Duration totalTimeout = Duration.ofSeconds(10 + stepCount * 5);

    return new MovementChallenge(
        java.util.UUID.randomUUID().toString(),
        steps,
        totalTimeout,
        0.6 + random.nextDouble() * 0.3, // 0.6-0.9
        3);
  }

  // Getters

  public String getChallengeId() {
    return challengeId;
  }

  public List<MovementStep> getSteps() {
    return steps;
  }

  public Duration getTotalTimeout() {
    return totalTimeout;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public double getRequiredPrecision() {
    return requiredPrecision;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  /** Gets the total expected duration for all steps. */
  public Duration getExpectedDuration() {
    return steps.stream().map(MovementStep::getDuration).reduce(Duration.ZERO, Duration::plus);
  }

  /** Checks if the challenge has expired. */
  public boolean isExpired() {
    return Instant.now().isAfter(createdAt.plus(totalTimeout));
  }

  /** Gets the remaining time for this challenge. */
  public Duration getRemainingTime() {
    Instant expiry = createdAt.plus(totalTimeout);
    Instant now = Instant.now();

    if (now.isAfter(expiry)) {
      return Duration.ZERO;
    }

    return Duration.between(now, expiry);
  }

  /** Gets formatted instructions for all steps. */
  public String getFormattedInstructions() {
    if (steps.size() == 1) {
      return steps.get(0).getInstruction();
    }

    StringBuilder sb = new StringBuilder("Follow these steps:\n");
    for (int i = 0; i < steps.size(); i++) {
      sb.append(String.format("%d. %s\n", i + 1, steps.get(i).getInstruction()));
    }
    return sb.toString().trim();
  }

  /** Gets the complexity level of this challenge. */
  public ComplexityLevel getComplexityLevel() {
    if (steps.size() == 1) {
      return ComplexityLevel.SIMPLE;
    } else if (steps.size() <= 2) {
      return ComplexityLevel.MODERATE;
    } else {
      return ComplexityLevel.COMPLEX;
    }
  }

  /** Complexity levels for movement challenges. */
  public enum ComplexityLevel {
    SIMPLE("Simple", 1),
    MODERATE("Moderate", 2),
    COMPLEX("Complex", 3);

    private final String displayName;
    private final int level;

    ComplexityLevel(String displayName, int level) {
      this.displayName = displayName;
      this.level = level;
    }

    public String getDisplayName() {
      return displayName;
    }

    public int getLevel() {
      return level;
    }
  }

  @Override
  public String toString() {
    return String.format(
        "MovementChallenge{id='%s', steps=%d, complexity=%s, timeout=%s}",
        challengeId, steps.size(), getComplexityLevel(), totalTimeout);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    MovementChallenge that = (MovementChallenge) obj;
    return challengeId.equals(that.challengeId);
  }

  @Override
  public int hashCode() {
    return challengeId.hashCode();
  }
}
