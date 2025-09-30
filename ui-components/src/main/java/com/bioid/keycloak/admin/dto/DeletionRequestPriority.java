package com.bioid.keycloak.admin.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Priority enumeration for biometric template deletion requests. Used to prioritize GDPR deletion
 * requests based on urgency and legal requirements.
 */
public enum DeletionRequestPriority {

  /** Low priority - routine cleanup requests. */
  LOW("low", 1),

  /** Normal priority - standard user requests. */
  NORMAL("normal", 2),

  /** High priority - requests with legal or compliance implications. */
  HIGH("high", 3),

  /** Urgent priority - immediate action required (e.g., legal orders, security breaches). */
  URGENT("urgent", 4);

  private final String value;
  private final int level;

  DeletionRequestPriority(String value, int level) {
    this.value = value;
    this.level = level;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  public int getLevel() {
    return level;
  }

  /** Get the maximum processing time in days based on priority. */
  public int getMaxProcessingDays() {
    return switch (this) {
      case URGENT -> 1; // 24 hours
      case HIGH -> 3; // 3 days
      case NORMAL -> 30; // 30 days (GDPR standard)
      case LOW -> 90; // 90 days
    };
  }

  /** Check if this priority is higher than another. */
  public boolean isHigherThan(DeletionRequestPriority other) {
    return this.level > other.level;
  }

  /** Check if this priority requires immediate attention. */
  public boolean requiresImmediateAttention() {
    return this == URGENT || this == HIGH;
  }

  /** Get the CSS class for UI styling based on priority. */
  public String getCssClass() {
    return switch (this) {
      case URGENT -> "pf-m-danger";
      case HIGH -> "pf-m-warning";
      case NORMAL -> "pf-m-info";
      case LOW -> "pf-m-default";
    };
  }

  /** Get the icon class for UI display based on priority. */
  public String getIconClass() {
    return switch (this) {
      case URGENT -> "fas fa-exclamation-triangle";
      case HIGH -> "fas fa-exclamation-circle";
      case NORMAL -> "fas fa-info-circle";
      case LOW -> "fas fa-minus-circle";
    };
  }

  public static DeletionRequestPriority fromValue(String value) {
    for (DeletionRequestPriority priority : values()) {
      if (priority.value.equals(value)) {
        return priority;
      }
    }
    throw new IllegalArgumentException("Unknown deletion request priority: " + value);
  }

  @Override
  public String toString() {
    return value;
  }
}
