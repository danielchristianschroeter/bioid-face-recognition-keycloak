package com.bioid.keycloak.client;

import com.bioid.keycloak.client.exception.BioIdException;

public interface BioIdClient extends AutoCloseable {

  // Simplified method signatures for testing - these will be replaced with protobuf types
  // when the protobuf generation is working properly
  
  /**
   * Enrolls a face template.
   */
  void enroll(byte[] imageData, long classId) throws BioIdException;

  /**
   * Verifies a face against enrolled templates.
   */
  boolean verify(byte[] imageData, long classId) throws BioIdException;

  /**
   * Deletes a template by class ID.
   */
  void deleteTemplate(long classId) throws BioIdException;

  /**
   * Gets template status information.
   */
  String getTemplateStatus(long classId) throws BioIdException;

  /**
   * Gets detailed template status with optional thumbnails.
   *
   * @param classId the class ID for the template
   * @param downloadThumbnails whether to download thumbnail images
   * @return detailed template status information
   * @throws BioIdException if the operation fails
   */
  TemplateStatusDetails getTemplateStatusDetails(long classId, boolean downloadThumbnails) throws BioIdException;

  /**
   * Sets template tags.
   */
  void setTemplateTags(long classId, String[] tags) throws BioIdException;

  /**
   * Performs liveness detection.
   */
  boolean livenessDetection(byte[] imageData) throws BioIdException;

  /**
   * Checks if the service is healthy.
   */
  boolean isHealthy();

  /**
   * Gets the current endpoint.
   */
  String getCurrentEndpoint();

  /**
   * Gets connection pool metrics for monitoring.
   *
   * @return connection pool metrics object
   */
  Object getConnectionPoolMetrics();

  /**
   * Convenience method for face verification using simple parameters.
   *
   * @param classId the class ID for the user
   * @param imageData base64-encoded image data (without data URL prefix)
   * @return true if verification succeeds, false otherwise
   * @throws BioIdException if verification fails
   */
  boolean verifyFaceWithImageData(long classId, String imageData) throws BioIdException;

  /**
   * Convenience method for face enrollment using simple parameters.
   *
   * @param classId the class ID for the user
   * @param imageData base64-encoded image data (without data URL prefix)
   * @return enrollment response with template status and metadata
   * @throws BioIdException if enrollment fails
   */
  EnrollmentResult enrollFaceWithImageData(long classId, String imageData) throws BioIdException;

  /**
   * Performs face enrollment with multiple images.
   *
   * @param classId the class ID for the user
   * @param imageDataList list of base64-encoded image data (without data URL prefix)
   * @return enrollment response with template status and metadata
   * @throws BioIdException if enrollment fails
   */
  EnrollmentResult enrollFaceWithMultipleImages(long classId, java.util.List<String> imageDataList) throws BioIdException;

  /**
   * Performs liveness detection with multiple images for active/challenge-response modes.
   *
   * @param firstImage base64-encoded first image data
   * @param secondImage base64-encoded second image data
   * @param mode liveness mode ("active" or "challenge-response")
   * @param challengeDirection challenge direction for challenge-response mode
   * @return true if liveness detection passes, false otherwise
   * @throws BioIdException if liveness detection fails
   */
  default boolean livenessDetectionWithImages(String firstImage, String secondImage, 
                                            String mode, String challengeDirection) throws BioIdException {
    // Default implementation for backward compatibility
    throw new BioIdException("livenessDetectionWithImages not implemented");
  }

  /** Detailed template status information including thumbnails. */
  class TemplateStatusDetails {
    private final long classId;
    private final boolean available;
    private final java.time.Instant enrolled;
    private final java.util.List<String> tags;
    private final int encoderVersion;
    private final int featureVectors;
    private final int thumbnailsStored;
    private final java.util.List<ThumbnailData> thumbnails;

    public TemplateStatusDetails(long classId, boolean available, java.time.Instant enrolled,
        java.util.List<String> tags, int encoderVersion, int featureVectors, int thumbnailsStored,
        java.util.List<ThumbnailData> thumbnails) {
      this.classId = classId;
      this.available = available;
      this.enrolled = enrolled;
      this.tags = tags;
      this.encoderVersion = encoderVersion;
      this.featureVectors = featureVectors;
      this.thumbnailsStored = thumbnailsStored;
      this.thumbnails = thumbnails;
    }

    public long getClassId() {
      return classId;
    }

    public boolean isAvailable() {
      return available;
    }

    public java.time.Instant getEnrolled() {
      return enrolled;
    }

    public java.util.List<String> getTags() {
      return tags;
    }

    public int getEncoderVersion() {
      return encoderVersion;
    }

    public int getFeatureVectors() {
      return featureVectors;
    }

    public int getThumbnailsStored() {
      return thumbnailsStored;
    }

    public java.util.List<ThumbnailData> getThumbnails() {
      return thumbnails;
    }
  }

  /** Thumbnail image data. */
  class ThumbnailData {
    private final java.time.Instant enrolled;
    private final byte[] imageData;

    public ThumbnailData(java.time.Instant enrolled, byte[] imageData) {
      this.enrolled = enrolled;
      this.imageData = imageData;
    }

    public java.time.Instant getEnrolled() {
      return enrolled;
    }

    public byte[] getImageData() {
      return imageData;
    }
  }

  /** Simple enrollment result containing essential information. */
  class EnrollmentResult {
    private final long classId;
    private final boolean available;
    private final int encoderVersion;
    private final int featureVectors;
    private final int thumbnailsStored;
    private final java.util.List<String> tags;
    private final String performedAction;
    private final int enrolledImages;

    public EnrollmentResult(long classId, boolean available, int encoderVersion, int featureVectors,
        int thumbnailsStored, java.util.List<String> tags, String performedAction,
        int enrolledImages) {
      this.classId = classId;
      this.available = available;
      this.encoderVersion = encoderVersion;
      this.featureVectors = featureVectors;
      this.thumbnailsStored = thumbnailsStored;
      this.tags = tags;
      this.performedAction = performedAction;
      this.enrolledImages = enrolledImages;
    }

    public long getClassId() {
      return classId;
    }

    public boolean isAvailable() {
      return available;
    }

    public int getEncoderVersion() {
      return encoderVersion;
    }

    public int getFeatureVectors() {
      return featureVectors;
    }

    public int getThumbnailsStored() {
      return thumbnailsStored;
    }

    public java.util.List<String> getTags() {
      return tags;
    }

    public String getPerformedAction() {
      return performedAction;
    }

    public int getEnrolledImages() {
      return enrolledImages;
    }
  }

  @Override
  void close();
}
