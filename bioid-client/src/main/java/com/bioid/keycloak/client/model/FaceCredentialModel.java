package com.bioid.keycloak.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Immutable data model for face biometric credentials stored in Keycloak. Contains only metadata -
 * no raw biometric data is stored locally.
 *
 * <p>This record represents the encrypted credential data stored in Keycloak's credential_data
 * column for face recognition authentication.
 */
public record FaceCredentialModel(
    @JsonProperty("classId") long classId,
    @JsonProperty("createdAt") Instant createdAt,
    @JsonProperty("updatedAt") Instant updatedAt,
    @JsonProperty("expiresAt") Instant expiresAt,
    @JsonProperty("imageCount") int imageCount,
    @JsonProperty("encoderVersion") int encoderVersion,
    @JsonProperty("featureVectors") int featureVectors,
    @JsonProperty("thumbnailsStored") int thumbnailsStored,
    @JsonProperty("templateType") TemplateType templateType,
    @JsonProperty("tags") List<String> tags,
    @JsonProperty("enrollmentAction") EnrollmentAction enrollmentAction,
    @JsonProperty("enrollmentMetadata") Map<String, String> enrollmentMetadata) {

  /**
   * Creates a new FaceCredentialModel with validation.
   *
   * @param classId BioID template identifier (must be positive)
   * @param createdAt creation timestamp
   * @param updatedAt last update timestamp
   * @param expiresAt expiration timestamp
   * @param imageCount number of enrollment images
   * @param encoderVersion BioID encoder version
   * @param featureVectors number of feature vectors
   * @param thumbnailsStored number of thumbnails stored
   * @param templateType template type
   * @param tags template tags (immutable copy)
   * @param enrollmentAction enrollment action performed
   * @param enrollmentMetadata enrollment metadata (immutable copy)
   */
  public FaceCredentialModel {
    if (classId <= 0) {
      throw new IllegalArgumentException("Class ID must be positive: " + classId);
    }
    if (createdAt == null) {
      throw new IllegalArgumentException("Created timestamp cannot be null");
    }
    if (updatedAt == null) {
      updatedAt = createdAt;
    }
    if (expiresAt == null) {
      // Default to 2 years from creation
      expiresAt = createdAt.plusSeconds(2L * 365 * 24 * 60 * 60);
    }
    if (imageCount < 0) {
      throw new IllegalArgumentException("Image count cannot be negative: " + imageCount);
    }
    if (encoderVersion < 0) {
      throw new IllegalArgumentException("Encoder version cannot be negative: " + encoderVersion);
    }
    if (templateType == null) {
      templateType = TemplateType.STANDARD;
    }
    if (enrollmentAction == null) {
      enrollmentAction = EnrollmentAction.NEW_TEMPLATE_CREATED;
    }

    // Create immutable copies of mutable collections
    tags = tags != null ? List.copyOf(tags) : List.of();
    enrollmentMetadata = enrollmentMetadata != null ? Map.copyOf(enrollmentMetadata) : Map.of();
  }

  /**
   * Checks if the credential has expired.
   *
   * @return true if expired, false otherwise
   */
  public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
  }

  /**
   * Checks if the credential expires within the specified duration.
   *
   * @param seconds seconds from now
   * @return true if expires within the duration, false otherwise
   */
  public boolean expiresWithin(long seconds) {
    return Instant.now().plusSeconds(seconds).isAfter(expiresAt);
  }

  /**
   * Creates a builder for constructing FaceCredentialModel instances.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a builder initialized with values from this instance.
   *
   * @return builder with current values
   */
  public Builder toBuilder() {
    return new Builder()
        .classId(classId)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .expiresAt(expiresAt)
        .imageCount(imageCount)
        .encoderVersion(encoderVersion)
        .featureVectors(featureVectors)
        .thumbnailsStored(thumbnailsStored)
        .templateType(templateType)
        .tags(tags)
        .enrollmentAction(enrollmentAction)
        .enrollmentMetadata(enrollmentMetadata);
  }

  /** Builder for FaceCredentialModel with fluent API. */
  public static class Builder {
    private long classId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
    private int imageCount;
    private int encoderVersion;
    private int featureVectors;
    private int thumbnailsStored;
    private TemplateType templateType = TemplateType.STANDARD;
    private List<String> tags = List.of();
    private EnrollmentAction enrollmentAction = EnrollmentAction.NEW_TEMPLATE_CREATED;
    private Map<String, String> enrollmentMetadata = Map.of();

    public Builder classId(long classId) {
      this.classId = classId;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(Instant updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public Builder expiresAt(Instant expiresAt) {
      this.expiresAt = expiresAt;
      return this;
    }

    public Builder imageCount(int imageCount) {
      this.imageCount = imageCount;
      return this;
    }

    public Builder encoderVersion(int encoderVersion) {
      this.encoderVersion = encoderVersion;
      return this;
    }

    public Builder featureVectors(int featureVectors) {
      this.featureVectors = featureVectors;
      return this;
    }

    public Builder thumbnailsStored(int thumbnailsStored) {
      this.thumbnailsStored = thumbnailsStored;
      return this;
    }

    public Builder templateType(TemplateType templateType) {
      this.templateType = templateType;
      return this;
    }

    public Builder tags(List<String> tags) {
      this.tags = tags;
      return this;
    }

    public Builder enrollmentAction(EnrollmentAction enrollmentAction) {
      this.enrollmentAction = enrollmentAction;
      return this;
    }

    public Builder enrollmentMetadata(Map<String, String> enrollmentMetadata) {
      this.enrollmentMetadata = enrollmentMetadata;
      return this;
    }

    public FaceCredentialModel build() {
      return new FaceCredentialModel(
          classId,
          createdAt,
          updatedAt,
          expiresAt,
          imageCount,
          encoderVersion,
          featureVectors,
          thumbnailsStored,
          templateType,
          tags,
          enrollmentAction,
          enrollmentMetadata);
    }
  }
}
