package com.bioid.keycloak.client.alerting;

/**
 * Severity levels for alerts in the BioID administrative system.
 * Determines the urgency and priority of alert handling and routing.
 */
public enum AlertSeverity {
    /**
     * Critical severity - immediate attention required.
     * System is down or severely impacted.
     */
    CRITICAL(5, "Immediate attention required - system severely impacted"),
    
    /**
     * High severity - urgent attention required.
     * Major functionality is impacted.
     */
    HIGH(4, "Urgent attention required - major functionality impacted"),
    
    /**
     * Warning severity - attention required soon.
     * Some functionality may be impacted.
     */
    WARNING(3, "Attention required - functionality may be impacted"),
    
    /**
     * Medium severity - attention required.
     * Minor issues or potential problems.
     */
    MEDIUM(2, "Attention required - minor issues detected"),
    
    /**
     * Low severity - informational.
     * No immediate impact but worth noting.
     */
    LOW(1, "Informational - no immediate impact");
    
    private final int level;
    private final String description;
    
    AlertSeverity(int level, String description) {
        this.level = level;
        this.description = description;
    }
    
    public int getLevel() {
        return level;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Checks if this severity is higher than another severity.
     *
     * @param other the other severity to compare
     * @return true if this severity is higher
     */
    public boolean isHigherThan(AlertSeverity other) {
        return this.level > other.level;
    }
    
    /**
     * Checks if this severity is lower than another severity.
     *
     * @param other the other severity to compare
     * @return true if this severity is lower
     */
    public boolean isLowerThan(AlertSeverity other) {
        return this.level < other.level;
    }
    
    /**
     * Checks if this severity requires immediate attention.
     *
     * @return true if immediate attention is required
     */
    public boolean requiresImmediateAttention() {
        return this == CRITICAL || this == HIGH;
    }
    
    /**
     * Gets the appropriate notification urgency for this severity.
     *
     * @return notification urgency level
     */
    public NotificationUrgency getNotificationUrgency() {
        return switch (this) {
            case CRITICAL -> NotificationUrgency.IMMEDIATE;
            case HIGH -> NotificationUrgency.URGENT;
            case WARNING -> NotificationUrgency.NORMAL;
            case MEDIUM -> NotificationUrgency.LOW;
            case LOW -> NotificationUrgency.INFORMATIONAL;
        };
    }
    
    /**
     * Notification urgency levels for alert routing.
     */
    public enum NotificationUrgency {
        IMMEDIATE,      // Send immediately via all channels
        URGENT,         // Send within minutes via primary channels
        NORMAL,         // Send within reasonable time via standard channels
        LOW,            // Send when convenient via standard channels
        INFORMATIONAL   // Log only, no active notification
    }
}