package com.bioid.keycloak.security;

/**
 * Enumeration of authentication methods for additional security verification.
 */
public enum AuthMethod {
    PASSWORD_CONFIRMATION("Password confirmation required"),
    EMAIL_VERIFICATION("Email verification code required"),
    SMS_VERIFICATION("SMS verification code required"),
    TOTP_VERIFICATION("Time-based OTP verification required"),
    ADMIN_APPROVAL("Administrator approval required"),
    BIOMETRIC_VERIFICATION("Biometric verification required");

    private final String description;

    AuthMethod(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Checks if this authentication method is considered high-security.
     */
    public boolean isHighSecurity() {
        return this == BIOMETRIC_VERIFICATION || 
               this == ADMIN_APPROVAL ||
               this == TOTP_VERIFICATION;
    }

    /**
     * Checks if this authentication method requires external communication.
     */
    public boolean requiresExternalCommunication() {
        return this == EMAIL_VERIFICATION || 
               this == SMS_VERIFICATION ||
               this == ADMIN_APPROVAL;
    }

    /**
     * Gets the typical timeout for this authentication method in minutes.
     */
    public int getTypicalTimeoutMinutes() {
        switch (this) {
            case PASSWORD_CONFIRMATION:
                return 5;
            case EMAIL_VERIFICATION:
            case SMS_VERIFICATION:
                return 10;
            case TOTP_VERIFICATION:
                return 2;
            case ADMIN_APPROVAL:
                return 60; // 1 hour
            case BIOMETRIC_VERIFICATION:
                return 5;
            default:
                return 5;
        }
    }
}