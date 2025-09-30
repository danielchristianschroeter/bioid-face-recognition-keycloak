package com.bioid.keycloak.client.liveness;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for BioID liveness detection functionality.
 *
 * <p>Provides passive, active, and challenge-response liveness detection capabilities integrated
 * with face verification and enrollment processes.
 */
public interface LivenessDetectionService {

  /**
   * Performs passive liveness detection on a single image.
   *
   * <p>Passive liveness detection analyzes the image for signs of life without requiring user
   * interaction. This is automatically integrated during verification with minimal performance
   * overhead.
   *
   * @param imageData The image data to analyze
   * @param config Liveness detection configuration
   * @return CompletableFuture containing the liveness detection result
   */
  CompletableFuture<LivenessResult> detectPassiveLiveness(byte[] imageData, LivenessConfig config);

  /**
   * Performs active liveness detection requiring user interaction.
   *
   * <p>Active liveness detection requires the user to perform a specific action (like smiling) to
   * prove they are alive and present.
   *
   * @param imageData The image data to analyze
   * @param challenge The challenge the user was asked to perform
   * @param config Liveness detection configuration
   * @return CompletableFuture containing the liveness detection result
   */
  CompletableFuture<LivenessResult> detectActiveLiveness(
      byte[] imageData, LivenessChallenge challenge, LivenessConfig config);

  /**
   * Performs challenge-response liveness detection with head movement.
   *
   * <p>Challenge-response liveness detection asks the user to move their head in a specific
   * direction to prove they are alive and present.
   *
   * @param imageSequence Sequence of images showing head movement
   * @param challenge The movement challenge that was presented
   * @param config Liveness detection configuration
   * @return CompletableFuture containing the liveness detection result
   */
  CompletableFuture<LivenessResult> detectChallengeResponseLiveness(
      byte[][] imageSequence, MovementChallenge challenge, LivenessConfig config);

  /**
   * Performs integrated liveness detection during face verification.
   *
   * <p>This method combines face verification with liveness detection, providing both verification
   * and liveness results in a single call.
   *
   * @param classId The class ID to verify against
   * @param imageData The image data for verification
   * @param config Liveness detection configuration
   * @return CompletableFuture containing combined verification and liveness results
   */
  CompletableFuture<VerificationWithLivenessResult> verifyWithLiveness(
      long classId, byte[] imageData, LivenessConfig config);

  /**
   * Checks if liveness detection is available and properly configured.
   *
   * @return true if liveness detection is available, false otherwise
   */
  boolean isLivenessDetectionAvailable();

  /**
   * Gets the maximum overhead time for passive liveness detection.
   *
   * @return Duration representing the maximum additional time for liveness detection
   */
  Duration getMaximumLivenessOverhead();

  /**
   * Validates liveness detection configuration.
   *
   * @param config The configuration to validate
   * @throws IllegalArgumentException if configuration is invalid
   */
  void validateConfiguration(LivenessConfig config);
}
