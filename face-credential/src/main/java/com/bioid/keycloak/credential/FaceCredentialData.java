package com.bioid.keycloak.credential;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Data class for storing face credential metadata in the credential_data field. This contains
 * non-sensitive information that can be shared via REST API.
 */
public class FaceCredentialData {

  private long classId;
  private int imageCount;
  private int encoderVersion;
  private int featureVectors;
  private int thumbnailsStored;
  private long expiresAtEpochSecond;
  private List<String> tags;
  private FaceCredentialModel.TemplateType templateType;
  private String enrollmentAction;
  private FaceCredentialModel.EnrollmentMetadata enrollmentMetadata;

  // Default constructor for Jackson
  public FaceCredentialData() {
    this.tags = List.of();
  }

  // Public constructor for programmatic use
  public FaceCredentialData(
      long classId,
      int imageCount,
      int encoderVersion,
      int featureVectors,
      int thumbnailsStored,
      long expiresAtEpochSecond,
      List<String> tags,
      FaceCredentialModel.TemplateType templateType,
      String enrollmentAction,
      FaceCredentialModel.EnrollmentMetadata enrollmentMetadata) {
    this.classId = classId;
    this.imageCount = imageCount;
    this.encoderVersion = encoderVersion;
    this.featureVectors = featureVectors;
    this.thumbnailsStored = thumbnailsStored;
    this.expiresAtEpochSecond = expiresAtEpochSecond;
    this.tags = tags != null ? List.copyOf(tags) : List.of();
    this.templateType = templateType;
    this.enrollmentAction = enrollmentAction;
    this.enrollmentMetadata = enrollmentMetadata;
  }

  // Convenience constructor that accepts Instant
  public FaceCredentialData(
      long classId,
      int imageCount,
      int encoderVersion,
      int featureVectors,
      int thumbnailsStored,
      Instant expiresAt,
      List<String> tags,
      FaceCredentialModel.TemplateType templateType,
      String enrollmentAction,
      FaceCredentialModel.EnrollmentMetadata enrollmentMetadata) {
    this(
        classId,
        imageCount,
        encoderVersion,
        featureVectors,
        thumbnailsStored,
        expiresAt != null ? expiresAt.getEpochSecond() : 0,
        tags,
        templateType,
        enrollmentAction,
        enrollmentMetadata);
  }

  public long getClassId() {
    return classId;
  }

  public void setClassId(long classId) {
    this.classId = classId;
  }

  public int getImageCount() {
    return imageCount;
  }

  public void setImageCount(int imageCount) {
    this.imageCount = imageCount;
  }

  public int getEncoderVersion() {
    return encoderVersion;
  }

  public void setEncoderVersion(int encoderVersion) {
    this.encoderVersion = encoderVersion;
  }

  public int getFeatureVectors() {
    return featureVectors;
  }

  public void setFeatureVectors(int featureVectors) {
    this.featureVectors = featureVectors;
  }

  public int getThumbnailsStored() {
    return thumbnailsStored;
  }

  public void setThumbnailsStored(int thumbnailsStored) {
    this.thumbnailsStored = thumbnailsStored;
  }

  public Instant getExpiresAt() {
    return expiresAtEpochSecond > 0 ? Instant.ofEpochSecond(expiresAtEpochSecond) : null;
  }

  @JsonProperty("expiresAtEpochSecond")
  public long getExpiresAtEpochSecond() {
    return expiresAtEpochSecond;
  }

  @JsonProperty("expiresAtEpochSecond")
  public void setExpiresAtEpochSecond(long expiresAtEpochSecond) {
    this.expiresAtEpochSecond = expiresAtEpochSecond;
  }

  // Backward compatibility setter for old field name
  @JsonProperty("expiresAt")
  public void setExpiresAt(long expiresAt) {
    this.expiresAtEpochSecond = expiresAt;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags != null ? List.copyOf(tags) : List.of();
  }

  public FaceCredentialModel.TemplateType getTemplateType() {
    return templateType;
  }

  public void setTemplateType(FaceCredentialModel.TemplateType templateType) {
    this.templateType = templateType;
  }

  public String getEnrollmentAction() {
    return enrollmentAction;
  }

  public void setEnrollmentAction(String enrollmentAction) {
    this.enrollmentAction = enrollmentAction;
  }

  public FaceCredentialModel.EnrollmentMetadata getEnrollmentMetadata() {
    return enrollmentMetadata;
  }

  public void setEnrollmentMetadata(FaceCredentialModel.EnrollmentMetadata enrollmentMetadata) {
    this.enrollmentMetadata = enrollmentMetadata;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FaceCredentialData that = (FaceCredentialData) o;
    return classId == that.classId
        && imageCount == that.imageCount
        && encoderVersion == that.encoderVersion
        && featureVectors == that.featureVectors
        && thumbnailsStored == that.thumbnailsStored
        && expiresAtEpochSecond == that.expiresAtEpochSecond
        && Objects.equals(tags, that.tags)
        && templateType == that.templateType
        && Objects.equals(enrollmentAction, that.enrollmentAction)
        && Objects.equals(enrollmentMetadata, that.enrollmentMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        classId,
        imageCount,
        encoderVersion,
        featureVectors,
        thumbnailsStored,
        expiresAtEpochSecond,
        tags,
        templateType,
        enrollmentAction,
        enrollmentMetadata);
  }

  @Override
  public String toString() {
    return "FaceCredentialData{"
        + "classId="
        + classId
        + ", imageCount="
        + imageCount
        + ", encoderVersion="
        + encoderVersion
        + ", featureVectors="
        + featureVectors
        + ", thumbnailsStored="
        + thumbnailsStored
        + ", expiresAt="
        + getExpiresAt()
        + ", tags="
        + tags
        + ", templateType="
        + templateType
        + ", enrollmentAction='"
        + enrollmentAction
        + '\''
        + ", enrollmentMetadata="
        + enrollmentMetadata
        + '}';
  }
}
