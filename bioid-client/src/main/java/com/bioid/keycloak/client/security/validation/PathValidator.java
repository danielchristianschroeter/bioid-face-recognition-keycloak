package com.bioid.keycloak.client.security.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Specialized validator for file paths to prevent path traversal attacks.
 * 
 * Features:
 * - Path traversal prevention
 * - Malicious path detection
 * - Path normalization
 * - Security validation
 */
public class PathValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(PathValidator.class);
    
    // Path validation patterns
    private static final Pattern DANGEROUS_PATH_PATTERNS = Pattern.compile(
        "(?i).*(\\.\\.|%2e%2e|%252e%252e|\\\\0|%00).*"
    );
    
    private static final Pattern ABSOLUTE_PATH_PATTERNS = Pattern.compile(
        "^([a-zA-Z]:|/|\\\\\\\\|~).*"
    );
    
    // Maximum path length
    private static final int MAX_PATH_LENGTH = 260; // Windows MAX_PATH limit
    
    /**
     * Validates file path to prevent path traversal attacks.
     * 
     * @param filePath file path to validate
     * @throws SecurityException if path is potentially malicious
     */
    public void validateFilePath(String filePath) throws SecurityException {
        validatePathNotNull(filePath);
        validatePathLength(filePath);
        validatePathSecurity(filePath);
        validatePathStructure(filePath);
    }
    
    /**
     * Validates that path is not null or empty.
     */
    private void validatePathNotNull(String filePath) throws SecurityException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new SecurityException("File path cannot be null or empty");
        }
    }   
 
    /**
     * Validates path length constraints.
     */
    private void validatePathLength(String filePath) throws SecurityException {
        if (filePath.length() > MAX_PATH_LENGTH) {
            logger.warn("File path too long: {} characters (maximum: {})", 
                filePath.length(), MAX_PATH_LENGTH);
            throw new SecurityException("File path exceeds maximum allowed length");
        }
    }
    
    /**
     * Validates path for security threats.
     */
    private void validatePathSecurity(String filePath) throws SecurityException {
        if (containsDangerousPathPatterns(filePath)) {
            logger.warn("Potentially malicious file path detected: {}", sanitizeForLogging(filePath));
            throw new SecurityException("File path contains potentially dangerous elements");
        }
        
        if (isAbsolutePath(filePath)) {
            logger.warn("Absolute path detected: {}", sanitizeForLogging(filePath));
            throw new SecurityException("Absolute paths are not allowed");
        }
        
        if (containsNullBytes(filePath)) {
            logger.warn("Null bytes detected in file path");
            throw new SecurityException("File path contains null bytes");
        }
    }
    
    /**
     * Validates path structure and normalization.
     */
    private void validatePathStructure(String filePath) throws SecurityException {
        try {
            // Normalize path to detect traversal attempts
            Path normalizedPath = Paths.get(filePath).normalize();
            String normalizedString = normalizedPath.toString();
            
            // Check if normalization revealed path traversal
            if (normalizedString.contains("..") || 
                normalizedString.startsWith("/") ||
                normalizedString.matches("^[a-zA-Z]:.*")) {
                
                logger.warn("Path traversal detected after normalization: {} -> {}", 
                    sanitizeForLogging(filePath), sanitizeForLogging(normalizedString));
                throw new SecurityException("File path contains path traversal elements");
            }
            
            // Validate individual path components
            validatePathComponents(normalizedPath);
            
        } catch (InvalidPathException e) {
            logger.warn("Invalid path format: {}", sanitizeForLogging(filePath), e);
            throw new SecurityException("Invalid file path format", e);
        }
    } 
   
    /**
     * Validates individual path components.
     */
    private void validatePathComponents(Path path) throws SecurityException {
        for (Path component : path) {
            String componentStr = component.toString();
            
            // Check for reserved names (Windows)
            if (isReservedName(componentStr)) {
                logger.warn("Reserved filename detected: {}", sanitizeForLogging(componentStr));
                throw new SecurityException("File path contains reserved filename");
            }
            
            // Check for invalid characters
            if (containsInvalidCharacters(componentStr)) {
                logger.warn("Invalid characters in path component: {}", sanitizeForLogging(componentStr));
                throw new SecurityException("File path contains invalid characters");
            }
        }
    }
    
    /**
     * Checks if input contains dangerous path patterns.
     */
    private boolean containsDangerousPathPatterns(String filePath) {
        if (filePath == null) {
            return false;
        }
        
        return DANGEROUS_PATH_PATTERNS.matcher(filePath).matches();
    }
    
    /**
     * Checks if path is absolute.
     */
    private boolean isAbsolutePath(String filePath) {
        if (filePath == null) {
            return false;
        }
        
        return ABSOLUTE_PATH_PATTERNS.matcher(filePath).matches();
    }
    
    /**
     * Checks if path contains null bytes.
     */
    private boolean containsNullBytes(String filePath) {
        return filePath != null && filePath.contains("\0");
    }
    
    /**
     * Checks if filename is reserved (Windows reserved names).
     */
    private boolean isReservedName(String filename) {
        if (filename == null) {
            return false;
        }
        
        String upperName = filename.toUpperCase();
        String[] reservedNames = {
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        };
        
        for (String reserved : reservedNames) {
            if (upperName.equals(reserved) || upperName.startsWith(reserved + ".")) {
                return true;
            }
        }
        
        return false;
    }  
  
    /**
     * Checks if filename contains invalid characters.
     */
    private boolean containsInvalidCharacters(String filename) {
        if (filename == null) {
            return false;
        }
        
        // Windows invalid characters: < > : " | ? * and control characters
        char[] invalidChars = {'<', '>', ':', '"', '|', '?', '*'};
        
        for (char c : filename.toCharArray()) {
            // Check for control characters
            if (c < 32) {
                return true;
            }
            
            // Check for invalid characters
            for (char invalid : invalidChars) {
                if (c == invalid) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Sanitizes path for safe logging.
     */
    private String sanitizeForLogging(String filePath) {
        if (filePath == null) {
            return "null";
        }
        
        // Truncate and remove potentially dangerous characters
        String sanitized = filePath.length() > 100 ? filePath.substring(0, 100) + "..." : filePath;
        return sanitized.replaceAll("[<>\"'&\\u0000]", "_");
    }
    
    /**
     * Gets maximum allowed path length.
     */
    public int getMaxPathLength() {
        return MAX_PATH_LENGTH;
    }
    
    /**
     * Normalizes a path safely.
     */
    public String normalizePath(String filePath) throws SecurityException {
        validateFilePath(filePath);
        
        try {
            return Paths.get(filePath).normalize().toString();
        } catch (InvalidPathException e) {
            throw new SecurityException("Cannot normalize invalid path", e);
        }
    }
}