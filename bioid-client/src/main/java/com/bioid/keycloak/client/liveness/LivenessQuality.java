package com.bioid.keycloak.client.liveness;

/**
 * Quality assessment for liveness detection results.
 *
 * <p>Indicates the quality and reliability of the liveness detection based on image quality,
 * lighting conditions, and other factors.
 */
public enum LivenessQuality {

  /**
   * Excellent quality liveness detection.
   *
   * <p>- High image quality - Optimal lighting conditions - Clear facial features - High confidence
   * in result
   */
  EXCELLENT("Excellent", 5, "High quality image with optimal conditions"),

  /**
   * Good quality liveness detection.
   *
   * <p>- Good image quality - Adequate lighting - Clear facial features - Good confidence in result
   */
  GOOD("Good", 4, "Good quality image with adequate conditions"),

  /**
   * Fair quality liveness detection.
   *
   * <p>- Acceptable image quality - Some lighting issues - Facial features mostly clear - Moderate
   * confidence in result
   */
  FAIR("Fair", 3, "Acceptable quality with some limitations"),

  /**
   * Poor quality liveness detection.
   *
   * <p>- Low image quality - Poor lighting conditions - Unclear facial features - Low confidence in
   * result
   */
  POOR("Poor", 2, "Poor quality image with significant limitations"),

  /**
   * Very poor quality liveness detection.
   *
   * <p>- Very low image quality - Very poor lighting - Facial features unclear - Very low
   * confidence in result
   */
  VERY_POOR("Very Poor", 1, "Very poor quality image, results unreliable");

  private final String displayName;
  private final int qualityScore;
  private final String description;

  LivenessQuality(String displayName, int qualityScore, String description) {
    this.displayName = displayName;
    this.qualityScore = qualityScore;
    this.description = description;
  }

  /** Gets the display name of the quality level. */
  public String getDisplayName() {
    return displayName;
  }

  /** Gets the numeric quality score (1-5, higher is better). */
  public int getQualityScore() {
    return qualityScore;
  }

  /** Gets the description of this quality level. */
  public String getDescription() {
    return description;
  }

  /** Checks if this quality level meets the minimum threshold. */
  public boolean meetsThreshold(LivenessQuality threshold) {
    return this.qualityScore >= threshold.qualityScore;
  }

  /** Checks if this quality level is acceptable for reliable results. */
  public boolean isAcceptable() {
    return qualityScore >= FAIR.qualityScore;
  }

  /** Checks if this quality level is good enough for high-security scenarios. */
  public boolean isHighQuality() {
    return qualityScore >= GOOD.qualityScore;
  }

  /** Gets quality level from a numeric score. */
  public static LivenessQuality fromScore(double score) {
    if (score >= 0.9) return EXCELLENT;
    if (score >= 0.7) return GOOD;
    if (score >= 0.5) return FAIR;
    if (score >= 0.3) return POOR;
    return VERY_POOR;
  }

  /** Gets quality level from image analysis factors. */
  public static LivenessQuality fromFactors(
      double imageSharpness, double lighting, double faceVisibility, double overallQuality) {
    double averageScore = (imageSharpness + lighting + faceVisibility + overallQuality) / 4.0;
    return fromScore(averageScore);
  }
}
