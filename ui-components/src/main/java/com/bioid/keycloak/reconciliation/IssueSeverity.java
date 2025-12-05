package com.bioid.keycloak.reconciliation;

/**
 * Severity levels for consistency issues.
 */
public enum IssueSeverity {
    CRITICAL("Critical", "Immediate attention required - affects system functionality"),
    HIGH("High", "High priority - affects user experience"),
    MEDIUM("Medium", "Medium priority - should be addressed soon"),
    LOW("Low", "Low priority - cosmetic or minor issues");

    private final String displayName;
    private final String description;

    IssueSeverity(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}

