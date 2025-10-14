package com.bioid.keycloak.client.liveness;

import java.util.List;
import java.util.Objects;

/**
 * Detailed liveness analysis results.
 *
 * <p>This class provides comprehensive information about the liveness detection process,
 * including individual scores for different detection methods and challenge results.
 */
public class LivenessDetails {

  private final boolean live;
  private final double livenessScore;
  private final String rejectionReason;
  private final LivenessScoreDetails scoreDetails;

  public LivenessDetails(
      boolean live,
      double livenessScore,
      String rejectionReason,
      LivenessScoreDetails scoreDetails) {
    this.live = live;
    this.livenessScore = livenessScore;
    this.rejectionReason = rejectionReason;
    this.scoreDetails = scoreDetails;
  }

  /**
   * Creates LivenessDetails from a protobuf response.
   *
   * @param protobufResponse the protobuf response
   * @return liveness details
   */
  public static LivenessDetails fromProtobuf(com.bioid.services.Bws.LivenessDetectionResponse protobufResponse) {
    String rejectionReason = determineRejectionReason(protobufResponse);
    LivenessScoreDetails scoreDetails = extractLivenessScoreDetails(protobufResponse);
    
    return new LivenessDetails(
        protobufResponse.getLive(),
        protobufResponse.getLivenessScore(),
        rejectionReason,
        scoreDetails
    );
  }

  private static String determineRejectionReason(com.bioid.services.Bws.LivenessDetectionResponse response) {
    if (response.getLive()) {
      return null; // No rejection
    }
    
    // Check for specific error codes that indicate rejection reasons
    for (com.bioid.services.Bwsmessages.JobError error : response.getErrorsList()) {
      String errorCode = error.getErrorCode();
      switch (errorCode) {
        case "FaceNotFound":
          return "No face detected in the image";
        case "MultipleFacesFound":
          return "Multiple faces detected in the image";
        case "RejectedByPassiveLiveDetection":
          return "Rejected by passive liveness detection";
        case "RejectedByActiveLiveDetection":
          return "Rejected by active liveness detection";
        case "RejectedByChallengeResponse":
          return "Challenge response validation failed";
        case "ThumbnailExtractionFailed":
          return "Failed to extract face thumbnail";
        default:
          return "Liveness detection failed: " + error.getMessage();
      }
    }
    
    return "Liveness detection failed (score: " + response.getLivenessScore() + ")";
  }

  private static LivenessScoreDetails extractLivenessScoreDetails(com.bioid.services.Bws.LivenessDetectionResponse response) {
    // For now, we only have the overall liveness score from the protobuf response
    // In a real implementation, this might extract more detailed scores from image properties
    return new LivenessScoreDetails(
        response.getLivenessScore(), // passive score
        response.getLivenessScore(), // active score (same for now)
        response.getLive()           // challenge response passed
    );
  }

  /**
   * Gets the overall liveness decision.
   *
   * @return true if live, false otherwise
   */
  public boolean isLive() {
    return live;
  }

  /**
   * Gets the overall liveness confidence score.
   *
   * @return score between 0.0 and 1.0
   */
  public double getLivenessScore() {
    return livenessScore;
  }

  /**
   * Gets the reason for rejection if liveness detection failed.
   *
   * @return rejection reason or null if not rejected
   */
  public String getRejectionReason() {
    return rejectionReason;
  }

  /**
   * Gets detailed score breakdown.
   *
   * @return score details
   */
  public LivenessScoreDetails getScoreDetails() {
    return scoreDetails;
  }

  /**
   * Checks if the liveness detection was rejected.
   *
   * @return true if rejected, false otherwise
   */
  public boolean isRejected() {
    return !live;
  }

  /**
   * Gets the passive liveness score.
   *
   * @return passive liveness score
   */
  public double getPassiveLivenessScore() {
    return scoreDetails.getPassiveLivenessScore();
  }

  /**
   * Gets the active liveness score.
   *
   * @return active liveness score
   */
  public double getActiveLivenessScore() {
    return scoreDetails.getActiveLivenessScore();
  }

  /**
   * Checks if challenge response validation passed.
   *
   * @return true if challenge response passed, false otherwise
   */
  public boolean isChallengeResponsePassed() {
    return scoreDetails.isChallengeResponsePassed();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LivenessDetails that = (LivenessDetails) o;
    return live == that.live &&
        Double.compare(that.livenessScore, livenessScore) == 0 &&
        Objects.equals(rejectionReason, that.rejectionReason) &&
        Objects.equals(scoreDetails, that.scoreDetails);
  }

  @Override
  public int hashCode() {
    return Objects.hash(live, livenessScore, rejectionReason, scoreDetails);
  }

  @Override
  public String toString() {
    return "LivenessDetails{" +
        "live=" + live +
        ", livenessScore=" + livenessScore +
        ", rejectionReason='" + rejectionReason + '\'' +
        ", scoreDetails=" + scoreDetails +
        '}';
  }
}

/**
 * Detailed breakdown of liveness scores.
 */
class LivenessScoreDetails {
  private final double passiveLivenessScore;
  private final double activeLivenessScore;
  private final boolean challengeResponsePassed;
  private final List<ChallengeResult> challengeResults;

  public LivenessScoreDetails(
      double passiveLivenessScore,
      double activeLivenessScore,
      boolean challengeResponsePassed) {
    this(passiveLivenessScore, activeLivenessScore, challengeResponsePassed, List.of());
  }

  public LivenessScoreDetails(
      double passiveLivenessScore,
      double activeLivenessScore,
      boolean challengeResponsePassed,
      List<ChallengeResult> challengeResults) {
    this.passiveLivenessScore = passiveLivenessScore;
    this.activeLivenessScore = activeLivenessScore;
    this.challengeResponsePassed = challengeResponsePassed;
    this.challengeResults = challengeResults != null ? List.copyOf(challengeResults) : List.of();
  }

  public double getPassiveLivenessScore() {
    return passiveLivenessScore;
  }

  public double getActiveLivenessScore() {
    return activeLivenessScore;
  }

  public boolean isChallengeResponsePassed() {
    return challengeResponsePassed;
  }

  public List<ChallengeResult> getChallengeResults() {
    return challengeResults;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LivenessScoreDetails that = (LivenessScoreDetails) o;
    return Double.compare(that.passiveLivenessScore, passiveLivenessScore) == 0 &&
        Double.compare(that.activeLivenessScore, activeLivenessScore) == 0 &&
        challengeResponsePassed == that.challengeResponsePassed &&
        Objects.equals(challengeResults, that.challengeResults);
  }

  @Override
  public int hashCode() {
    return Objects.hash(passiveLivenessScore, activeLivenessScore, challengeResponsePassed, challengeResults);
  }

  @Override
  public String toString() {
    return "LivenessScoreDetails{" +
        "passiveLivenessScore=" + passiveLivenessScore +
        ", activeLivenessScore=" + activeLivenessScore +
        ", challengeResponsePassed=" + challengeResponsePassed +
        ", challengeResults=" + challengeResults +
        '}';
  }
}

/**
 * Result of a specific challenge direction validation.
 */
class ChallengeResult {
  private final LivenessDetectionClient.ChallengeDirection direction;
  private final boolean passed;
  private final double confidence;

  public ChallengeResult(
      LivenessDetectionClient.ChallengeDirection direction,
      boolean passed,
      double confidence) {
    this.direction = direction;
    this.passed = passed;
    this.confidence = confidence;
  }

  public LivenessDetectionClient.ChallengeDirection getDirection() {
    return direction;
  }

  public boolean isPassed() {
    return passed;
  }

  public double getConfidence() {
    return confidence;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChallengeResult that = (ChallengeResult) o;
    return passed == that.passed &&
        Double.compare(that.confidence, confidence) == 0 &&
        direction == that.direction;
  }

  @Override
  public int hashCode() {
    return Objects.hash(direction, passed, confidence);
  }

  @Override
  public String toString() {
    return "ChallengeResult{" +
        "direction=" + direction +
        ", passed=" + passed +
        ", confidence=" + confidence +
        '}';
  }
}