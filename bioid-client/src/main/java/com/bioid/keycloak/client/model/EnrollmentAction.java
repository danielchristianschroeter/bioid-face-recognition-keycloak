package com.bioid.keycloak.client.model;

/**
 * Enumeration of enrollment actions that can be performed by BioID BWS service. Indicates what
 * action was taken during the enrollment process.
 */
public enum EnrollmentAction {
  /** No action was performed - typically indicates an error or no-op. */
  NONE("none", "No action performed"),

  /** A new biometric template was created from the provided images. */
  NEW_TEMPLATE_CREATED("new_template_created", "New biometric template created"),

  /**
   * An existing template was updated with additional feature vectors. Requires STANDARD or FULL
   * template type.
   */
  TEMPLATE_UPDATED("template_updated", "Existing template updated with new feature vectors"),

  /**
   * An existing template was upgraded to a newer encoder version. Requires FULL template type with
   * stored thumbnails.
   */
  TEMPLATE_UPGRADED("template_upgraded", "Template upgraded to newer encoder version"),

  /** Enrollment failed due to various reasons (poor image quality, etc.). */
  ENROLLMENT_FAILED("enrollment_failed", "Enrollment process failed");

  private final String value;
  private final String description;

  EnrollmentAction(String value, String description) {
    this.value = value;
    this.description = description;
  }

  /**
   * Gets the string value for this enrollment action.
   *
   * @return the string value
   */
  public String getValue() {
    return value;
  }

  /**
   * Gets the human-readable description for this enrollment action.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this action indicates a successful enrollment.
   *
   * @return true if enrollment was successful, false otherwise
   */
  public boolean isSuccessful() {
    return this != NONE && this != ENROLLMENT_FAILED;
  }

  /**
   * Checks if this action modified an existing template.
   *
   * @return true if an existing template was modified, false otherwise
   */
  public boolean isTemplateModification() {
    return this == TEMPLATE_UPDATED || this == TEMPLATE_UPGRADED;
  }

  /**
   * Parses a string value to an EnrollmentAction.
   *
   * @param value the string value
   * @return the corresponding EnrollmentAction
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static EnrollmentAction fromValue(String value) {
    if (value == null) {
      return NONE;
    }

    for (EnrollmentAction action : values()) {
      if (action.value.equalsIgnoreCase(value)) {
        return action;
      }
    }

    throw new IllegalArgumentException("Unknown enrollment action: " + value);
  }
}
