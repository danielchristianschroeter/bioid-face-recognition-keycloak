package com.bioid.keycloak.credential;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;
import org.keycloak.util.JsonSerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data model for face biometric credentials stored in Keycloak.
 *
 * <p>This model contains only metadata about the biometric template stored in BioID BWS. No raw
 * biometric data is stored in Keycloak for privacy and security reasons.
 *
 * <p>Features: - Follows Keycloak credential model pattern - JSON serialization support for
 * database storage - Template metadata tracking for audit and management - Expiration support for
 * automatic cleanup
 */
public final class FaceCredentialModel extends CredentialModel {

  private static final Logger logger = LoggerFactory.getLogger(FaceCredentialModel.class);
  public static final String TYPE = "face-biometric";

  private final FaceCredentialData credentialData;
  private final FaceSecretData secretData;

  @JsonCreator
  private FaceCredentialModel(
      @JsonProperty("credentialData") FaceCredentialData credentialData,
      @JsonProperty("secretData") FaceSecretData secretData) {
    this.credentialData = credentialData;
    this.secretData = secretData;
  }

  public static FaceCredentialModel createFaceCredential(
      long classId,
      int imageCount,
      int encoderVersion,
      int featureVectors,
      int thumbnailsStored,
      Instant expiresAt,
      List<String> tags,
      TemplateType templateType,
      String enrollmentAction,
      EnrollmentMetadata enrollmentMetadata) {
    FaceCredentialData credentialData =
        new FaceCredentialData(
            classId,
            imageCount,
            encoderVersion,
            featureVectors,
            thumbnailsStored,
            expiresAt,
            tags,
            templateType,
            enrollmentAction,
            enrollmentMetadata);
    FaceSecretData secretData = new FaceSecretData(classId);

    FaceCredentialModel credentialModel = new FaceCredentialModel(credentialData, secretData);
    credentialModel.fillCredentialModelFields();
    return credentialModel;
  }

  public static FaceCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
    try {
      logger.debug("Deserializing credential data: {}", credentialModel.getCredentialData());
      FaceCredentialData credentialData =
          JsonSerialization.readValue(
              credentialModel.getCredentialData(), FaceCredentialData.class);

      logger.debug("Deserializing secret data: {}", credentialModel.getSecretData());
      FaceSecretData secretData =
          JsonSerialization.readValue(credentialModel.getSecretData(), FaceSecretData.class);

      FaceCredentialModel faceCredentialModel = new FaceCredentialModel(credentialData, secretData);
      faceCredentialModel.setUserLabel(credentialModel.getUserLabel());
      faceCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
      faceCredentialModel.setType(TYPE);
      faceCredentialModel.setId(credentialModel.getId());
      faceCredentialModel.setSecretData(credentialModel.getSecretData());
      faceCredentialModel.setCredentialData(credentialModel.getCredentialData());
      return faceCredentialModel;
    } catch (IOException e) {
      logger.error(
          "Failed to deserialize face credential model. Credential data: {}, Secret data: {}",
          credentialModel.getCredentialData(),
          credentialModel.getSecretData(),
          e);
      throw new RuntimeException("Failed to deserialize face credential model", e);
    }
  }

  private void fillCredentialModelFields() {
    try {
      setCredentialData(JsonSerialization.writeValueAsString(credentialData));
      setSecretData(JsonSerialization.writeValueAsString(secretData));
      setType(TYPE);
      setCreatedDate(Time.currentTimeMillis());
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize face credential model", e);
    }
  }

  /**
   * Gets the BioID BWS class ID for this template. This is the unique identifier used to reference
   * the template in BioID BWS.
   *
   * @return the class ID (64-bit positive integer)
   */
  public long getClassId() {
    return credentialData.getClassId();
  }

  /**
   * Gets the timestamp when this credential was created.
   *
   * @return creation timestamp
   */
  public Instant getCreatedAt() {
    return Instant.ofEpochMilli(getCreatedDate());
  }

  /**
   * Gets the number of images used to create this template.
   *
   * @return number of enrollment images
   */
  public int getImageCount() {
    return credentialData.getImageCount();
  }

  /**
   * Gets the BioID encoder version used to create this template.
   *
   * @return encoder version number
   */
  public int getEncoderVersion() {
    return credentialData.getEncoderVersion();
  }

  /**
   * Gets the number of feature vectors in this template.
   *
   * @return number of feature vectors
   */
  public int getFeatureVectors() {
    return credentialData.getFeatureVectors();
  }

  /**
   * Gets the number of thumbnails stored with this template.
   *
   * @return number of thumbnails
   */
  public int getThumbnailsStored() {
    return credentialData.getThumbnailsStored();
  }

  /**
   * Gets the expiration timestamp for this credential. After this time, the credential should be
   * considered expired.
   *
   * @return expiration timestamp
   */
  public Instant getExpiresAt() {
    return credentialData.getExpiresAt();
  }

  /**
   * Gets the tags associated with this template. Tags can be used for categorization and
   * management.
   *
   * @return immutable list of tags
   */
  public List<String> getTags() {
    return credentialData.getTags();
  }

  /**
   * Gets the template type (COMPACT, STANDARD, FULL).
   *
   * @return template type
   */
  public TemplateType getTemplateType() {
    return credentialData.getTemplateType();
  }

  /**
   * Gets the enrollment action that was performed.
   *
   * @return enrollment action description
   */
  public String getEnrollmentAction() {
    return credentialData.getEnrollmentAction();
  }

  /**
   * Gets the enrollment metadata containing additional context.
   *
   * @return enrollment metadata
   */
  public EnrollmentMetadata getEnrollmentMetadata() {
    return credentialData.getEnrollmentMetadata();
  }

  /**
   * Checks if this credential is expired.
   *
   * @return true if the credential is expired, false otherwise
   */
  public boolean isExpired() {
    Instant expiresAt = getExpiresAt();
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  /**
   * Creates a new FaceCredentialModel with updated expiration.
   *
   * @param newExpiresAt new expiration timestamp
   * @return new FaceCredentialModel with updated expiration
   */
  public FaceCredentialModel withExpiration(Instant newExpiresAt) {
    return createFaceCredential(
        getClassId(),
        getImageCount(),
        getEncoderVersion(),
        getFeatureVectors(),
        getThumbnailsStored(),
        newExpiresAt,
        getTags(),
        getTemplateType(),
        getEnrollmentAction(),
        getEnrollmentMetadata());
  }

  /**
   * Creates a new FaceCredentialModel with updated tags.
   *
   * @param newTags new tags list
   * @return new FaceCredentialModel with updated tags
   */
  public FaceCredentialModel withTags(List<String> newTags) {
    return createFaceCredential(
        getClassId(),
        getImageCount(),
        getEncoderVersion(),
        getFeatureVectors(),
        getThumbnailsStored(),
        getExpiresAt(),
        newTags,
        getTemplateType(),
        getEnrollmentAction(),
        getEnrollmentMetadata());
  }

  @JsonProperty("credentialData")
  public FaceCredentialData getFaceCredentialData() {
    return credentialData;
  }

  @JsonProperty("secretData")
  public FaceSecretData getFaceSecretData() {
    return secretData;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    FaceCredentialModel that = (FaceCredentialModel) o;
    return Objects.equals(credentialData, that.credentialData)
        && Objects.equals(secretData, that.secretData);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), credentialData, secretData);
  }

  @Override
  public String toString() {
    return "FaceCredentialModel{"
        + "classId="
        + getClassId()
        + ", imageCount="
        + getImageCount()
        + ", encoderVersion="
        + getEncoderVersion()
        + ", featureVectors="
        + getFeatureVectors()
        + ", thumbnailsStored="
        + getThumbnailsStored()
        + ", expiresAt="
        + getExpiresAt()
        + ", tags="
        + getTags()
        + ", templateType="
        + getTemplateType()
        + ", enrollmentAction='"
        + getEnrollmentAction()
        + '\''
        + ", enrollmentMetadata="
        + getEnrollmentMetadata()
        + ", createdAt="
        + getCreatedAt()
        + '}';
  }

  /** Template types supported by BioID BWS. */
  public enum TemplateType {
    /** Compact template (~8.2KB) - contains only biometric template for recognition. */
    COMPACT,

    /** Standard template (~24KB+) - includes feature vectors for template updates. */
    STANDARD,

    /** Full template (variable size) - includes thumbnails for encoder upgrades. */
    FULL
  }

  /** Metadata about the enrollment process. */
  public static final class EnrollmentMetadata {
    private String userAgent;
    private String ipAddress;
    private String deviceFingerprint;
    private String bwsJobId;

    // Default constructor for Jackson
    public EnrollmentMetadata() {}

    public EnrollmentMetadata(
        String userAgent, String ipAddress, String deviceFingerprint, String bwsJobId) {
      this.userAgent = userAgent;
      this.ipAddress = ipAddress;
      this.deviceFingerprint = deviceFingerprint;
      this.bwsJobId = bwsJobId;
    }

    public String getUserAgent() {
      return userAgent;
    }

    public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
    }

    public String getIpAddress() {
      return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
      this.ipAddress = ipAddress;
    }

    public String getDeviceFingerprint() {
      return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
      this.deviceFingerprint = deviceFingerprint;
    }

    public String getBwsJobId() {
      return bwsJobId;
    }

    public void setBwsJobId(String bwsJobId) {
      this.bwsJobId = bwsJobId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      EnrollmentMetadata that = (EnrollmentMetadata) o;
      return Objects.equals(userAgent, that.userAgent)
          && Objects.equals(ipAddress, that.ipAddress)
          && Objects.equals(deviceFingerprint, that.deviceFingerprint)
          && Objects.equals(bwsJobId, that.bwsJobId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userAgent, ipAddress, deviceFingerprint, bwsJobId);
    }

    @Override
    public String toString() {
      return "EnrollmentMetadata{"
          + "userAgent='"
          + userAgent
          + '\''
          + ", ipAddress='"
          + ipAddress
          + '\''
          + ", deviceFingerprint='"
          + deviceFingerprint
          + '\''
          + ", bwsJobId='"
          + bwsJobId
          + '\''
          + '}';
    }
  }
}
