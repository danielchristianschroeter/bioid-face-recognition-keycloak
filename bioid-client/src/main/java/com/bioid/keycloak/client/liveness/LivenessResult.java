package com.bioid.keycloak.client.liveness;

import java.time.Duration;
import java.time.Instant;

/**
 * Result of a liveness detection operation.
 *
 * <p>Contains the liveness detection decision, confidence score, and additional metadata about the
 * detection process.
 */
public class LivenessResult {

  private final boolean alive;
  private final double confidence;
  private final LivenessMethod method;
  private final Duration processingTime;
  private final Instant timestamp;
  private final String errorMessage;
  private final LivenessQuality quality;

  public LivenessResult(
      boolean alive,
      double confidence,
      LivenessMethod method,
      Duration processingTime,
      LivenessQuality quality) {
    this(alive, confidence, method, processingTime, Instant.now(), null, quality);
  }

  public LivenessResult(
      boolean alive,
      double confidence,
      LivenessMethod method,
      Duration processingTime,
      Instant timestamp,
      String errorMessage,
      LivenessQuality quality) {
    this.alive = alive;
    this.confidence = confidence;
    this.method = method;
    this.processingTime = processingTime;
    this.timestamp = timestamp;
    this.errorMessage = errorMessage;
    this.quality = quality;
  }

  /** Creates a successful liveness result. */
  public static LivenessResult success(
      double confidence, LivenessMethod method, Duration processingTime, LivenessQuality quality) {
    return new LivenessResult(true, confidence, method, processingTime, quality);
  }

  /** Creates a failed liveness result. */
  public static LivenessResult failure(
      double confidence, LivenessMethod method, Duration processingTime, LivenessQuality quality) {
    return new LivenessResult(false, confidence, method, processingTime, quality);
  }

  /** Creates an error liveness result. */
  public static LivenessResult error(
      String errorMessage, LivenessMethod method, Duration processingTime) {
    return new LivenessResult(
        false, 0.0, method, processingTime, Instant.now(), errorMessage, LivenessQuality.POOR);
  }

  /** Checks if the subject is determined to be alive. */
  public boolean isAlive() {
    return alive;
  }

  /**
   * Gets the confidence score of the liveness detection. Range: 0.0 (no confidence) to 1.0 (high
   * confidence)
   */
  public double getConfidence() {
    return confidence;
  }

  /** Gets the liveness detection method used. */
  public LivenessMethod getMethod() {
    return method;
  }

  /** Gets the processing time for the liveness detection. */
  public Duration getProcessingTime() {
    return processingTime;
  }

  /** Gets the timestamp when the detection was performed. */
  public Instant getTimestamp() {
    return timestamp;
  }

  /** Gets any error message if the detection failed. */
  public String getErrorMessage() {
    return errorMessage;
  }

  /** Gets the quality assessment of the liveness detection. */
  public LivenessQuality getQuality() {
    return quality;
  }

  /** Checks if there was an error during detection. */
  public boolean hasError() {
    return errorMessage != null && !errorMessage.trim().isEmpty();
  }

  /** Checks if the liveness detection meets the minimum confidence threshold. */
  public boolean meetsConfidenceThreshold(double threshold) {
    return confidence >= threshold;
  }

  /** Checks if the processing time is within the acceptable overhead limit. */
  public boolean isWithinOverheadLimit(Duration maxOverhead) {
    return processingTime.compareTo(maxOverhead) <= 0;
  }

  @Override
  public String toString() {
    return String.format(
        "LivenessResult{alive=%s, confidence=%.3f, method=%s, "
            + "processingTime=%dms, quality=%s, hasError=%s}",
        alive, confidence, method, processingTime.toMillis(), quality, hasError());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    LivenessResult that = (LivenessResult) obj;
    return alive == that.alive
        && Double.compare(that.confidence, confidence) == 0
        && method == that.method
        && quality == that.quality;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(alive, confidence, method, quality);
  }
}
