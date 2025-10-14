package com.bioid.keycloak.security;

/**
 * Result of a permission check operation.
 */
public class PermissionCheckResult {
    
    private final boolean allowed;
    private final String reason;
    private final String recommendedAction;

    private PermissionCheckResult(boolean allowed, String reason, String recommendedAction) {
        this.allowed = allowed;
        this.reason = reason;
        this.recommendedAction = recommendedAction;
    }

    /**
     * Creates a permission check result indicating access is allowed.
     */
    public static PermissionCheckResult allowed() {
        return new PermissionCheckResult(true, null, null);
    }

    /**
     * Creates a permission check result indicating access is denied.
     * 
     * @param reason The reason for denial
     */
    public static PermissionCheckResult denied(String reason) {
        return new PermissionCheckResult(false, reason, null);
    }

    /**
     * Creates a permission check result indicating access is denied with a recommended action.
     * 
     * @param reason The reason for denial
     * @param recommendedAction The recommended action to resolve the issue
     */
    public static PermissionCheckResult denied(String reason, String recommendedAction) {
        return new PermissionCheckResult(false, reason, recommendedAction);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public boolean isDenied() {
        return !allowed;
    }

    public String getReason() {
        return reason;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    @Override
    public String toString() {
        if (allowed) {
            return "PermissionCheckResult{allowed=true}";
        } else {
            return String.format("PermissionCheckResult{allowed=false, reason='%s', recommendedAction='%s'}", 
                               reason, recommendedAction);
        }
    }
}