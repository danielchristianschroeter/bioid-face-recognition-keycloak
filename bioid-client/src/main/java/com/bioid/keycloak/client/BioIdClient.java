package com.bioid.keycloak.client;

import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.services.Facerecognition;

public interface BioIdClient extends AutoCloseable {

  Facerecognition.FaceEnrollmentResponse enroll(Facerecognition.FaceEnrollmentRequest request)
      throws BioIdException;

  Facerecognition.FaceVerificationResponse verify(Facerecognition.FaceVerificationRequest request)
      throws BioIdException;

  Facerecognition.DeleteTemplateResponse deleteTemplate(
      Facerecognition.DeleteTemplateRequest request) throws BioIdException;

  Facerecognition.FaceTemplateStatus getTemplateStatus(
      Facerecognition.FaceTemplateStatusRequest request) throws BioIdException;

  Facerecognition.SetTemplateTagsResponse setTemplateTags(
      Facerecognition.SetTemplateTagsRequest request) throws BioIdException;

  boolean isHealthy();

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
   * Convenience method for deleting a face template.
   *
   * @param classId the class ID for the user
   * @throws BioIdException if deletion fails
   */
  void deleteTemplate(long classId) throws BioIdException;

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
