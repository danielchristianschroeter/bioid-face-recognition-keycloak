package com.bioid.keycloak.admin.service;

import java.time.Instant;

/**
 * Information about a face template.
 */
public class TemplateInfo {
  private String classId;
  private String username;
  private String email;
  private Instant enrolledAt;
  private String encoderVersion;
  private int featureVectors;
  private int thumbnailsStored;
  private boolean keycloakUserExists;

  public String getClassId() {
    return classId;
  }

  public void setClassId(String classId) {
    this.classId = classId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Instant getEnrolledAt() {
    return enrolledAt;
  }

  public void setEnrolledAt(Instant enrolledAt) {
    this.enrolledAt = enrolledAt;
  }

  public String getEncoderVersion() {
    return encoderVersion;
  }

  public void setEncoderVersion(String encoderVersion) {
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

  public boolean isKeycloakUserExists() {
    return keycloakUserExists;
  }

  public void setKeycloakUserExists(boolean keycloakUserExists) {
    this.keycloakUserExists = keycloakUserExists;
  }
}
