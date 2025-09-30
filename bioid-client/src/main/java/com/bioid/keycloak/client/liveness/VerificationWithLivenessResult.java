package com.bioid.keycloak.client.liveness;

import java.time.Duration;
import java.time.Instant;

/**
 * Combined result of face verification and liveness detection.
 *
 * <p>Contains both verification and liveness detection results, providing a comprehensive
 * authentication decision.
 */
public class VerificationWithLivenessResult {

  private final boolean verified;
  private final double verificationScore;
  private final LivenessResult livenessResult;
  private final boolean overallSuccess;
  private final Duration totalProcessingTime;
  private final Instant timestamp;
  private final String errorMessage;

  public VerificationWithLivenessResult(
      boolean verified,
      double verificationScore,
      LivenessResult livenessResult,
      Duration totalProcessingTime) {
    this(verified, verificationScore, livenessResult, totalProcessingTime, null);
  }

  public VerificationWithLivenessResult(
      boolean verified,
      double verificationScore,
      LivenessResult livenessResult,
      Duration totalProcessingTime,
      String errorMessage) {
    this.verified = verified;
    this.verificationScore = verificationScore;
    this.livenessResult = livenessResult;
    this.totalProcessingTime = totalProcessingTime;
    this.timestamp = Instant.now();
    this.errorMessage = errorMessage;

    // Overall success requires both verification and liveness to pass
    // Check for error directly instead of calling hasError() method to avoid 'this' escape
    boolean hasErrorCondition = errorMessage != null && !errorMessage.trim().isEmpty();
    this.overallSuccess =
        verified && (livenessResult != null && livenessResult.isAlive()) && !hasErrorCondition;
  }

  /** Creates a successful combined result. */
  public static VerificationWithLivenessResult success(
      double verificationScore, LivenessResult livenessResult, Duration totalProcessingTime) {
    return new VerificationWithLivenessResult(
        true, verificationScore, livenessResult, totalProcessingTime);
  }

  /** Creates a failed verification result. */
  public static VerificationWithLivenessResult verificationFailed(
      double verificationScore, LivenessResult livenessResult, Duration totalProcessingTime) {
    return new VerificationWithLivenessResult(
        false, verificationScore, livenessResult, totalProcessingTime);
  }

  /** Creates a failed liveness result. */
  public static VerificationWithLivenessResult livenessFailed(
      double verificationScore, LivenessResult livenessResult, Duration totalProcessingTime) {
    return new VerificationWithLivenessResult(
        true, verificationScore, livenessResult, totalProcessingTime);
  }

  /** Creates an error result. */
  public static VerificationWithLivenessResult error(
      String errorMessage, Duration totalProcessingTime) {
    return new VerificationWithLivenessResult(false, 0.0, null, totalProcessingTime, errorMessage);
  }

  // Getters

  /** Checks if face verification was successful. */
  public boolean isVerified() {
    return verified;
  }

  /** Gets the face verification score. */
  public double getVerificationScore() {
    return verificationScore;
  }

  /** Gets the liveness detection result. */
  public LivenessResult getLivenessResult() {
    return livenessResult;
  }

  /** Checks if both verification and liveness detection were successful. */
  public boolean isOverallSuccess() {
    return overallSuccess;
  }

  /** Gets the total processing time for both verification and liveness detection. */
  public Duration getTotalProcessingTime() {
    return totalProcessingTime;
  }

  /** Gets the timestamp when the combined operation was performed. */
  public Instant getTimestamp() {
    return timestamp;
  }

  /** Gets any error message if the operation failed. */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** Checks if there was an error during the operation. */
  public boolean hasError() {
    return errorMessage != null && !errorMessage.trim().isEmpty();
  }

  /** Checks if liveness detection was performed. */
  public boolean hasLivenessResult() {
    return livenessResult != null;
  }

  /** Gets the liveness detection processing time. */
  public Duration getLivenessProcessingTime() {
    return livenessResult != null ? livenessResult.getProcessingTime() : Duration.ZERO;
  }

  /** Gets the estimated verification processing time (total minus liveness). */
  public Duration getVerificationProcessingTime() {
    if (livenessResult != null) {
      return totalProcessingTime.minus(livenessResult.getProcessingTime());
    }
    return totalProcessingTime;
  }

  /** Checks if the liveness detection overhead was within acceptable limits. */
  public boolean isLivenessOverheadAcceptable(Duration maxOverhead) {
    return livenessResult == null || livenessResult.isWithinOverheadLimit(maxOverhead);
  }

  /** Gets a detailed authentication decision. */
  public AuthenticationDecision getAuthenticationDecision() {
    if (hasError()) {
      return AuthenticationDecision.ERROR;
    }

    if (!verified) {
      return AuthenticationDecision.VERIFICATION_FAILED;
    }

    if (livenessResult == null) {
      return AuthenticationDecision.SUCCESS_NO_LIVENESS;
    }

    if (!livenessResult.isAlive()) {
      return AuthenticationDecision.LIVENESS_FAILED;
    }

    return AuthenticationDecision.SUCCESS_WITH_LIVENESS;
  }

  /** Authentication decision types. */
  public enum AuthenticationDecision {
    SUCCESS_WITH_LIVENESS("Success with liveness", true),
    SUCCESS_NO_LIVENESS("Success without liveness", true),
    VERIFICATION_FAILED("Face verification failed", false),
    LIVENESS_FAILED("Liveness detection failed", false),
    ERROR("Error during authentication", false);

    private final String description;
    private final boolean successful;

    AuthenticationDecision(String description, boolean successful) {
      this.description = description;
      this.successful = successful;
    }

    public String getDescription() {
      return description;
    }

    public boolean isSuccessful() {
      return successful;
    }
  }

  @Override
  public String toString() {
    return String.format(
        "VerificationWithLivenessResult{verified=%s, verificationScore=%.3f, "
            + "livenessAlive=%s, overallSuccess=%s, totalTime=%dms, decision=%s}",
        verified,
        verificationScore,
        livenessResult != null ? livenessResult.isAlive() : "N/A",
        overallSuccess,
        totalProcessingTime.toMillis(),
        getAuthenticationDecision().getDescription());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    VerificationWithLivenessResult that = (VerificationWithLivenessResult) obj;
    return verified == that.verified
        && Double.compare(that.verificationScore, verificationScore) == 0
        && overallSuccess == that.overallSuccess
        && java.util.Objects.equals(livenessResult, that.livenessResult);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(verified, verificationScore, livenessResult, overallSuccess);
  }
}
