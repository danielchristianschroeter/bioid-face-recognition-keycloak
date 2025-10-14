package com.bioid.keycloak.security;

/**
 * Result of additional authentication validation.
 */
public class AuthValidationResult {
    
    private final boolean success;
    private final String errorMessage;
    private final String challengeId;

    private AuthValidationResult(boolean success, String errorMessage, String challengeId) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.challengeId = challengeId;
    }

    /**
     * Creates a successful validation result.
     */
    public static AuthValidationResult success() {
        return new AuthValidationResult(true, null, null);
    }

    /**
     * Creates a successful validation result with challenge ID.
     * 
     * @param challengeId The challenge ID that was validated
     */
    public static AuthValidationResult success(String challengeId) {
        return new AuthValidationResult(true, null, challengeId);
    }

    /**
     * Creates a failed validation result.
     * 
     * @param errorMessage The error message
     */
    public static AuthValidationResult failed(String errorMessage) {
        return new AuthValidationResult(false, errorMessage, null);
    }

    /**
     * Creates a failed validation result with challenge ID.
     * 
     * @param errorMessage The error message
     * @param challengeId The challenge ID that failed validation
     */
    public static AuthValidationResult failed(String errorMessage, String challengeId) {
        return new AuthValidationResult(false, errorMessage, challengeId);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailed() {
        return !success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getChallengeId() {
        return challengeId;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("AuthValidationResult{success=true, challengeId='%s'}", challengeId);
        } else {
            return String.format("AuthValidationResult{success=false, errorMessage='%s', challengeId='%s'}", 
                               errorMessage, challengeId);
        }
    }
}