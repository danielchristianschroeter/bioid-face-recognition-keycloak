package com.bioid.keycloak.client.model;

/**
 * Enumeration of BioID template types with different capabilities and sizes. Each type offers
 * different trade-offs between size, functionality, and upgrade capabilities.
 */
public enum TemplateType {
  /**
   * Compact template (~8.2KB) - Contains only biometric template for recognition operations.
   * Minimal size but limited functionality - verification only, no updates or upgrades.
   */
  COMPACT("compact", 8192, false, false),

  /**
   * Standard template (~24KB+) - Includes feature vectors for template updates. Balanced size and
   * functionality - supports verification and template updates.
   */
  STANDARD("standard", 24576, true, false),

  /**
   * Full template (variable size) - Includes thumbnails for encoder version upgrades. Largest size
   * but full functionality - supports verification, updates, and upgrades.
   */
  FULL("full", 32768, true, true);

  private final String value;
  private final int approximateSize;
  private final boolean supportsUpdates;
  private final boolean supportsUpgrades;

  TemplateType(
      String value, int approximateSize, boolean supportsUpdates, boolean supportsUpgrades) {
    this.value = value;
    this.approximateSize = approximateSize;
    this.supportsUpdates = supportsUpdates;
    this.supportsUpgrades = supportsUpgrades;
  }

  /**
   * Gets the string value for this template type.
   *
   * @return the string value
   */
  public String getValue() {
    return value;
  }

  /**
   * Gets the approximate size in bytes for this template type.
   *
   * @return approximate size in bytes
   */
  public int getApproximateSize() {
    return approximateSize;
  }

  /**
   * Checks if this template type supports template updates.
   *
   * @return true if updates are supported, false otherwise
   */
  public boolean supportsUpdates() {
    return supportsUpdates;
  }

  /**
   * Checks if this template type supports encoder version upgrades.
   *
   * @return true if upgrades are supported, false otherwise
   */
  public boolean supportsUpgrades() {
    return supportsUpgrades;
  }

  /**
   * Parses a string value to a TemplateType.
   *
   * @param value the string value
   * @return the corresponding TemplateType
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static TemplateType fromValue(String value) {
    if (value == null) {
      return STANDARD; // Default
    }

    for (TemplateType type : values()) {
      if (type.value.equalsIgnoreCase(value)) {
        return type;
      }
    }

    throw new IllegalArgumentException("Unknown template type: " + value);
  }

  /**
   * Gets the recommended template type based on use case requirements.
   *
   * @param needsUpdates whether template updates are needed
   * @param needsUpgrades whether encoder upgrades are needed
   * @param sizeSensitive whether size optimization is important
   * @return recommended template type
   */
  public static TemplateType getRecommended(
      boolean needsUpdates, boolean needsUpgrades, boolean sizeSensitive) {
    if (needsUpgrades) {
      return FULL;
    } else if (needsUpdates) {
      return STANDARD;
    } else if (sizeSensitive) {
      return COMPACT;
    } else {
      return STANDARD; // Balanced default
    }
  }
}
