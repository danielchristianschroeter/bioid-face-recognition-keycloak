package com.bioid.keycloak.audit;

/**
 * Enumeration of risk levels for administrative operations.
 */
public enum RiskLevel {
    LOW("Low risk operation"),
    MEDIUM("Medium risk operation"),
    HIGH("High risk operation"),
    CRITICAL("Critical risk operation");

    private final String description;

    RiskLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this risk level requires additional approval.
     */
    public boolean requiresApproval() {
        return this == HIGH || this == CRITICAL;
    }

    /**
     * Checks if this risk level should trigger security alerts.
     */
    public boolean shouldTriggerAlert() {
        return this == HIGH || this == CRITICAL;
    }

    /**
     * Gets the numeric value for comparison purposes.
     */
    public int getNumericValue() {
        switch (this) {
            case LOW: return 1;
            case MEDIUM: return 2;
            case HIGH: return 3;
            case CRITICAL: return 4;
            default: return 0;
        }
    }
}