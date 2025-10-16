package com.bioid.keycloak.client.security.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Input validation for security compliance.
 */
public class InputValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(InputValidator.class);
    
    // Input validation patterns
    private static final Pattern VALID_USER_ID = Pattern.compile("^[a-zA-Z0-9_.-]{1,255}$");
    private static final Pattern VALID_SESSION_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,128}$");
    private static final Pattern VALID_CREDENTIAL_ID = Pattern.compile("^[a-zA-Z0-9_-]{1,128}$");
    private static final Pattern DANGEROUS_PATTERNS = Pattern.compile(
        "(?i).*(script|javascript|vbscript|onload|onerror|eval|expression|import|@import|" +
        "drop\\s+table|delete\\s+from|insert\\s+into|update\\s+set|union\\s+select|" +
        "exec\\s*\\(|execute\\s*\\(|sp_|xp_|\\.\\./|\\.\\.\\\\|%2e%2e|%252e%252e).*"
    );
    
    // Image data validation
    private static final int MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MIN_IMAGE_SIZE_BYTES = 1024; // 1KB
    
    /**
     * Validates user ID for security compliance.
     */
    public void validateUserId(String userId) throws SecurityException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new SecurityException("User ID cannot be null or empty");
        }
        
        if (!VALID_USER_ID.matcher(userId).matches()) {
            logger.warn("Invalid user ID format: {}", sanitizeForLogging(userId));
            throw new SecurityException("User ID contains invalid characters or exceeds length limit");
        }
        
        if (containsDangerousPatterns(userId)) {
            logger.warn("Potentially malicious user ID detected: {}", sanitizeForLogging(userId));
            throw new SecurityException("User ID contains potentially malicious content");
        }
    }
    
    /**
     * Validates session ID for security compliance.
     */
    public void validateSessionId(String sessionId) throws SecurityException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new SecurityException("Session ID cannot be null or empty");
        }
        
        if (!VALID_SESSION_ID.matcher(sessionId).matches()) {
            logger.warn("Invalid session ID format: {}", sanitizeForLogging(sessionId));
            throw new SecurityException("Session ID contains invalid characters or exceeds length limit");
        }
        
        if (containsDangerousPatterns(sessionId)) {
            logger.warn("Potentially malicious session ID detected: {}", sanitizeForLogging(sessionId));
            throw new SecurityException("Session ID contains potentially malicious content");
        }
    }
    
    /**
     * Validates credential ID for security compliance.
     */
    public void validateCredentialId(String credentialId) throws SecurityException {
        if (credentialId == null || credentialId.trim().isEmpty()) {
            throw new SecurityException("Credential ID cannot be null or empty");
        }
        
        if (!VALID_CREDENTIAL_ID.matcher(credentialId).matches()) {
            logger.warn("Invalid credential ID format: {}", sanitizeForLogging(credentialId));
            throw new SecurityException("Credential ID contains invalid characters or exceeds length limit");
        }
        
        if (containsDangerousPatterns(credentialId)) {
            logger.warn("Potentially malicious credential ID detected: {}", sanitizeForLogging(credentialId));
            throw new SecurityException("Credential ID contains potentially malicious content");
        }
    }
    
    /**
     * Validates biometric image data for security compliance.
     */
    public void validateImageData(byte[] imageData) throws SecurityException {
        if (imageData == null) {
            throw new SecurityException("Image data cannot be null");
        }
        
        if (imageData.length == 0) {
            throw new SecurityException("Image data cannot be empty");
        }
        
        if (imageData.length < MIN_IMAGE_SIZE_BYTES) {
            logger.warn("Image data too small: {} bytes", imageData.length);
            throw new SecurityException("Image data too small to be valid");
        }
        
        if (imageData.length > MAX_IMAGE_SIZE_BYTES) {
            logger.warn("Image data too large: {} bytes", imageData.length);
            throw new SecurityException("Image data exceeds maximum allowed size");
        }
        
        // Basic image format validation
        if (!isValidImageFormat(imageData)) {
            logger.warn("Invalid image format detected");
            throw new SecurityException("Image data does not appear to be a valid image format");
        }
    }
    
    /**
     * Validates metadata content for security compliance.
     */
    public void validateMetadata(String metadata) throws SecurityException {
        if (metadata == null || metadata.trim().isEmpty()) {
            throw new SecurityException("Metadata cannot be null or empty");
        }
        
        if (metadata.length() > 10240) { // 10KB limit
            throw new SecurityException("Metadata exceeds maximum allowed size");
        }
        
        if (containsDangerousPatterns(metadata)) {
            logger.warn("Potentially malicious metadata detected");
            throw new SecurityException("Metadata contains potentially malicious content");
        }
        
        // Validate JSON structure if metadata appears to be JSON
        if (metadata.trim().startsWith("{") && metadata.trim().endsWith("}")) {
            validateJsonStructure(metadata);
        }
    }
    
    /**
     * Validates file path to prevent path traversal attacks.
     */
    public void validateFilePath(String filePath) throws SecurityException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SecurityException("File path cannot be null or empty");
        }
        
        // Normalize path
        String normalizedPath = java.nio.file.Paths.get(filePath).normalize().toString();
        
        // Check for path traversal attempts
        if (normalizedPath.contains("..") || 
            normalizedPath.contains("~") ||
            normalizedPath.startsWith("/") ||
            normalizedPath.matches("^[a-zA-Z]:.*")) {
            
            logger.warn("Potentially malicious file path detected: {}", sanitizeForLogging(filePath));
            throw new SecurityException("File path contains potentially dangerous elements");
        }
    }
    
    /**
     * Checks if input contains dangerous patterns.
     */
    private boolean containsDangerousPatterns(String input) {
        if (input == null) {
            return false;
        }
        
        return DANGEROUS_PATTERNS.matcher(input).matches();
    }
    
    /**
     * Validates basic image format by checking headers.
     */
    private boolean isValidImageFormat(byte[] imageData) {
        if (imageData.length < 8) {
            return false;
        }
        
        // Check for common image format headers
        // JPEG: FF D8 FF
        if (imageData[0] == (byte) 0xFF && imageData[1] == (byte) 0xD8 && imageData[2] == (byte) 0xFF) {
            return true;
        }
        
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (imageData[0] == (byte) 0x89 && imageData[1] == 0x50 && 
            imageData[2] == 0x4E && imageData[3] == 0x47) {
            return true;
        }
        
        // WebP: RIFF ... WEBP
        if (imageData.length >= 12 &&
            imageData[0] == 0x52 && imageData[1] == 0x49 && imageData[2] == 0x46 && imageData[3] == 0x46 &&
            imageData[8] == 0x57 && imageData[9] == 0x45 && imageData[10] == 0x42 && imageData[11] == 0x50) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Validates JSON structure without parsing sensitive content.
     */
    private void validateJsonStructure(String json) throws SecurityException {
        // Basic JSON structure validation without full parsing
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        boolean escaped = false;
        
        for (char c : json.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                continue;
            }
            
            if (c == '"' && !escaped) {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                switch (c) {
                    case '{': braceCount++; break;
                    case '}': braceCount--; break;
                    case '[': bracketCount++; break;
                    case ']': bracketCount--; break;
                }
            }
        }
        
        if (braceCount != 0 || bracketCount != 0) {
            throw new SecurityException("Invalid JSON structure in metadata");
        }
    }
    
    /**
     * Sanitizes input for safe logging.
     */
    private String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        
        // Truncate and remove potentially dangerous characters
        String sanitized = input.length() > 50 ? input.substring(0, 50) + "..." : input;
        return sanitized.replaceAll("[<>\"'&]", "_");
    }
}