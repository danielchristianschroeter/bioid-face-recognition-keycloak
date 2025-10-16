package com.bioid.keycloak.client.security;

import com.bioid.keycloak.client.security.ratelimit.RateLimiter;
import com.bioid.keycloak.client.security.validation.ImageValidator;
import com.bioid.keycloak.client.security.validation.InputValidator;
import com.bioid.keycloak.client.security.validation.MetadataValidator;
import com.bioid.keycloak.client.security.validation.PathValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Comprehensive security validator that coordinates specialized validation components.
 * 
 * This class serves as a facade for various security validation concerns:
 * - Input validation (user IDs, session IDs, credential IDs)
 * - Image validation (biometric data)
 * - Metadata validation (JSON/XML content)
 * - Path validation (file paths)
 * - Rate limiting (abuse prevention)
 */
public class SecurityValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityValidator.class);
    
    // Specialized validators
    private final InputValidator inputValidator;
    private final ImageValidator imageValidator;
    private final MetadataValidator metadataValidator;
    private final PathValidator pathValidator;
    private final RateLimiter rateLimiter;
    
    /**
     * Creates a new SecurityValidator with default specialized validators.
     */
    public SecurityValidator() {
        this.inputValidator = new InputValidator();
        this.imageValidator = new ImageValidator();
        this.metadataValidator = new MetadataValidator();
        this.pathValidator = new PathValidator();
        this.rateLimiter = new RateLimiter();
    }
    
    /**
     * Creates a SecurityValidator with custom specialized validators.
     */
    public SecurityValidator(InputValidator inputValidator, ImageValidator imageValidator,
                           MetadataValidator metadataValidator, PathValidator pathValidator,
                           RateLimiter rateLimiter) {
        this.inputValidator = inputValidator;
        this.imageValidator = imageValidator;
        this.metadataValidator = metadataValidator;
        this.pathValidator = pathValidator;
        this.rateLimiter = rateLimiter;
    }
    
    /**
     * Validates user ID for security compliance.
     * 
     * @param userId user identifier to validate
     * @throws SecurityException if validation fails
     */
    public void validateUserId(String userId) throws SecurityException {
        inputValidator.validateUserId(userId);
    }
    
    /**
     * Validates session ID for security compliance.
     * 
     * @param sessionId session identifier to validate
     * @throws SecurityException if validation fails
     */
    public void validateSessionId(String sessionId) throws SecurityException {
        inputValidator.validateSessionId(sessionId);
    }
    
    /**
     * Validates credential ID for security compliance.
     * 
     * @param credentialId credential identifier to validate
     * @throws SecurityException if validation fails
     */
    public void validateCredentialId(String credentialId) throws SecurityException {
        inputValidator.validateCredentialId(credentialId);
    }
    
    /**
     * Validates biometric image data for security compliance.
     * 
     * @param imageData image data to validate
     * @throws SecurityException if validation fails
     */
    public void validateImageData(byte[] imageData) throws SecurityException {
        imageValidator.validateImageData(imageData);
    }
    
    /**
     * Validates metadata content for security compliance.
     * 
     * @param metadata metadata to validate
     * @throws SecurityException if validation fails
     */
    public void validateMetadata(String metadata) throws SecurityException {
        metadataValidator.validateMetadata(metadata);
    }
    
    /**
     * Checks rate limiting for a given identifier.
     * 
     * @param identifier identifier to check (e.g., user ID, IP address)
     * @throws SecurityException if rate limit exceeded
     */
    public void checkRateLimit(String identifier) throws SecurityException {
        rateLimiter.checkRateLimit(identifier);
    }
    
    /**
     * Validates file path to prevent path traversal attacks.
     * 
     * @param filePath file path to validate
     * @throws SecurityException if path is potentially malicious
     */
    public void validateFilePath(String filePath) throws SecurityException {
        pathValidator.validateFilePath(filePath);
    }
    
    /**
     * Performs comprehensive validation of all provided inputs.
     * 
     * @param userId user identifier
     * @param sessionId session identifier
     * @param credentialId credential identifier
     * @param imageData biometric image data
     * @param metadata associated metadata
     * @param filePath file path (optional)
     * @throws SecurityException if any validation fails
     */
    public void validateAll(String userId, String sessionId, String credentialId, 
                           byte[] imageData, String metadata, String filePath) throws SecurityException {
        
        // Validate identifiers
        if (userId != null) {
            validateUserId(userId);
        }
        
        if (sessionId != null) {
            validateSessionId(sessionId);
        }
        
        if (credentialId != null) {
            validateCredentialId(credentialId);
        }
        
        // Validate data
        if (imageData != null) {
            validateImageData(imageData);
        }
        
        if (metadata != null) {
            validateMetadata(metadata);
        }
        
        if (filePath != null) {
            validateFilePath(filePath);
        }
        
        // Check rate limiting for user
        if (userId != null) {
            checkRateLimit(userId);
        }
    }
    
    /**
     * Cleans up expired rate limit entries.
     */
    public void cleanupRateLimitEntries() {
        rateLimiter.cleanupRateLimitEntries();
    }
    
    /**
     * Gets rate limit statistics.
     */
    public RateLimiter.RateLimitStats getRateLimitStats() {
        return rateLimiter.getRateLimitStats();
    }
    
    /**
     * Gets the input validator instance.
     */
    public InputValidator getInputValidator() {
        return inputValidator;
    }
    
    /**
     * Gets the image validator instance.
     */
    public ImageValidator getImageValidator() {
        return imageValidator;
    }
    
    /**
     * Gets the metadata validator instance.
     */
    public MetadataValidator getMetadataValidator() {
        return metadataValidator;
    }
    
    /**
     * Gets the path validator instance.
     */
    public PathValidator getPathValidator() {
        return pathValidator;
    }
    
    /**
     * Gets the rate limiter instance.
     */
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }
}