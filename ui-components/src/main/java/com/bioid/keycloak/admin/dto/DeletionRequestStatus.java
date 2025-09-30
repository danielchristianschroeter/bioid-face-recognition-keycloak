package com.bioid.keycloak.admin.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Status enumeration for biometric template deletion requests. Used to track the lifecycle of GDPR
 * deletion requests.
 */
public enum DeletionRequestStatus {

  /** Request has been submitted and is awaiting admin review. */
  PENDING("pending"),

  /** Request is being processed by an administrator. */
  IN_PROGRESS("in_progress"),

  /** Request has been approved and template deletion is in progress. */
  APPROVED("approved"),

  /** Request has been declined by an administrator. */
  DECLINED("declined"),

  /** Template has been successfully deleted from BioID service. */
  COMPLETED("completed"),

  /** Template deletion failed due to technical issues. */
  FAILED("failed"),

  /** Request has been cancelled by the user or admin. */
  CANCELLED("cancelled");

  private final String value;

  DeletionRequestStatus(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  /** Check if the status represents a final state. */
  public boolean isFinal() {
    return this == COMPLETED || this == FAILED || this == CANCELLED;
  }

  /** Check if the status represents an active processing state. */
  public boolean isActive() {
    return this == PENDING || this == IN_PROGRESS || this == APPROVED;
  }

  /** Check if the request can be cancelled. */
  public boolean isCancellable() {
    return this == PENDING || this == IN_PROGRESS;
  }

  /** Get the next logical status for workflow progression. */
  public DeletionRequestStatus getNextStatus() {
    return switch (this) {
      case PENDING -> IN_PROGRESS;
      case IN_PROGRESS -> APPROVED;
      case APPROVED -> COMPLETED;
      default -> this;
    };
  }

  public static DeletionRequestStatus fromValue(String value) {
    for (DeletionRequestStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown deletion request status: " + value);
  }

  @Override
  public String toString() {
    return value;
  }
}
