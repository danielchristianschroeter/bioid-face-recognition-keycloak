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
