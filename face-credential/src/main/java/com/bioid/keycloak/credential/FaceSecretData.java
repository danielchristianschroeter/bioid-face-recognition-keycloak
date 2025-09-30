package com.bioid.keycloak.credential;

import java.util.Objects;

/**
 * Data class for storing face credential secret data in the secret_data field. This contains
 * sensitive information that should not be exposed via REST API.
 */
public class FaceSecretData {

  private long classId;

  // Default constructor for Jackson
  public FaceSecretData() {}

  public FaceSecretData(long classId) {
    this.classId = classId;
  }

  public long getClassId() {
    return classId;
  }

  public void setClassId(long classId) {
    this.classId = classId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    FaceSecretData that = (FaceSecretData) o;
    return classId == that.classId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(classId);
  }

  @Override
  public String toString() {
    return "FaceSecretData{" + "classId=" + classId + '}';
  }
}
