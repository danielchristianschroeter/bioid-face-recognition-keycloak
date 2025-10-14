package com.bioid.keycloak.admin.dto;

/**
 * Enumeration of liveness detection modes supported by BioID BWS.
 * 
 * Requirements addressed: 3.1, 4.1, 4.2
 */
public enum LivenessMode {
    /**
     * Passive liveness detection using a single image.
     * Analyzes texture and other static features to detect spoofing attempts.
     */
    PASSIVE("passive"),
    
    /**
     * Active liveness detection using two consecutive images.
     * Detects motion and 3D characteristics between images.
     */
    ACTIVE("active"),
    
    /**
     * Challenge-response liveness detection using two images with specific head movements.
     * Validates that the user performed requested head movements (up, down, left, right).
     */
    CHALLENGE_RESPONSE("challenge_response");
    
    private final String value;
    
    LivenessMode(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Converts string value to LivenessMode enum.
     * 
     * @param value the string value
     * @return the corresponding LivenessMode
     * @throws IllegalArgumentException if value is not recognized
     */
    public static LivenessMode fromValue(String value) {
        if (value == null) {
            return PASSIVE; // Default
        }
        
        for (LivenessMode mode : values()) {
            if (mode.value.equalsIgnoreCase(value) || mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        
        throw new IllegalArgumentException("Unknown liveness mode: " + value);
    }
    
    /**
     * Returns whether this mode requires multiple images.
     */
    public boolean requiresMultipleImages() {
        return this == ACTIVE || this == CHALLENGE_RESPONSE;
    }
    
    /**
     * Returns whether this mode supports challenge directions.
     */
    public boolean supportsChallengeDirections() {
        return this == CHALLENGE_RESPONSE;
    }
    
    @Override
    public String toString() {
        return value;
    }
}