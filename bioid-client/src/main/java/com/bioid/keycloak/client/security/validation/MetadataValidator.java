package com.bioid.keycloak.client.security.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Specialized validator for metadata content.
 */
public class MetadataValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(MetadataValidator.class);
    
    // Metadata validation constants
    private static final int MAX_METADATA_SIZE_BYTES = 10 * 1024; // 10KB
    private static final int MAX_JSON_DEPTH = 10;
    
    // Security patterns
    private static final Pattern DANGEROUS_PATTERNS = Pattern.compile(
        "(?i).*(script|javascript|vbscript|onload|onerror|eval|expression).*"
    );
    
    private static final Pattern SQL_INJECTION_PATTERNS = Pattern.compile(
        "(?i).*(union|select|insert|update|delete|drop|create|alter|exec|execute).*"
    );
    
    private static final Pattern XSS_PATTERNS = Pattern.compile(
        "(?i).*(<script|javascript:|vbscript:|onload=|onerror=|onclick=|alert\\(|document\\.).*"
    );
    
    /**
     * Validates metadata content for security compliance.
     */
    public void validateMetadata(String metadata) throws SecurityException {
        validateMetadataNotNull(metadata);
        validateMetadataSize(metadata);
        validateMetadataSecurity(metadata);
        validateMetadataStructure(metadata);
    }
    
    /**
     * Validates that metadata is not null or empty.
     */
    private void validateMetadataNotNull(String metadata) throws SecurityException {
        if (metadata == null || metadata.trim().isEmpty()) {
            throw new SecurityException("Metadata cannot be null or empty");
        }
    }
    
    /**
     * Validates metadata size constraints.
     */
    private void validateMetadataSize(String metadata) throws SecurityException {
        if (metadata.length() > MAX_METADATA_SIZE_BYTES) {
            logger.warn("Metadata too large: {} bytes (maximum: {} bytes)", 
                metadata.length(), MAX_METADATA_SIZE_BYTES);
            throw new SecurityException("Metadata exceeds maximum allowed size");
        }
    }    
  
  /**
     * Validates metadata for security threats.
     */
    private void validateMetadataSecurity(String metadata) throws SecurityException {
        // Check for specific threats first (more specific error messages)
        if (containsXssPatterns(metadata)) {
            logger.warn("XSS patterns detected in metadata");
            throw new SecurityException("Metadata contains XSS patterns");
        }
        
        if (containsSqlInjectionPatterns(metadata)) {
            logger.warn("SQL injection patterns detected in metadata");
            throw new SecurityException("Metadata contains SQL injection patterns");
        }
        
        if (containsDangerousPatterns(metadata)) {
            logger.warn("Potentially malicious metadata detected");
            throw new SecurityException("Metadata contains potentially malicious content");
        }
    }
    
    /**
     * Validates metadata structure if it appears to be structured data.
     */
    private void validateMetadataStructure(String metadata) throws SecurityException {
        String trimmed = metadata.trim();
        
        // Validate JSON structure if metadata appears to be JSON
        if (isJsonLike(trimmed)) {
            validateJsonStructure(trimmed);
        }
        
        // Validate XML structure if metadata appears to be XML
        if (isXmlLike(trimmed)) {
            validateXmlStructure(trimmed);
        }
    }
    
    /**
     * Checks if content appears to be JSON.
     */
    private boolean isJsonLike(String content) {
        return content.startsWith("{") || content.startsWith("[");
    }
    
    /**
     * Checks if content appears to be XML.
     */
    private boolean isXmlLike(String content) {
        return content.startsWith("<") && content.endsWith(">");
    }
    
    /**
     * Validates JSON structure without parsing sensitive content.
     */
    private void validateJsonStructure(String json) throws SecurityException {
        JsonStructureValidator validator = new JsonStructureValidator();
        validator.validate(json);
    }
    
    /**
     * Validates XML structure for basic well-formedness.
     */
    private void validateXmlStructure(String xml) throws SecurityException {
        XmlStructureValidator validator = new XmlStructureValidator();
        validator.validate(xml);
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
     * Checks if input contains SQL injection patterns.
     */
    private boolean containsSqlInjectionPatterns(String input) {
        if (input == null) {
            return false;
        }
        
        return SQL_INJECTION_PATTERNS.matcher(input).find();
    }
    
    /**
     * Checks if input contains XSS patterns.
     */
    private boolean containsXssPatterns(String input) {
        if (input == null) {
            return false;
        }
        
        return XSS_PATTERNS.matcher(input).find();
    }
    
    /**
     * Sanitizes metadata for safe logging.
     */
    public String sanitizeForLogging(String metadata) {
        if (metadata == null) {
            return "null";
        }
        
        // Truncate and remove potentially dangerous characters
        String sanitized = metadata.length() > 100 ? metadata.substring(0, 100) + "..." : metadata;
        return sanitized.replaceAll("[<>\"'&]", "_");
    }
    
    /**
     * Gets maximum allowed metadata size.
     */
    public int getMaxMetadataSize() {
        return MAX_METADATA_SIZE_BYTES;
    }
    
    /**
     * JSON structure validator.
     */
    private static class JsonStructureValidator {
        
        public void validate(String json) throws SecurityException {
            int braceCount = 0;
            int bracketCount = 0;
            int depth = 0;
            boolean inString = false;
            boolean escaped = false;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
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
                        case '{':
                            braceCount++;
                            depth++;
                            if (depth > MAX_JSON_DEPTH) {
                                throw new SecurityException("JSON structure too deep (max depth: " + MAX_JSON_DEPTH + ")");
                            }
                            break;
                        case '}':
                            braceCount--;
                            depth--;
                            if (braceCount < 0) {
                                throw new SecurityException("Invalid JSON structure: unmatched closing brace");
                            }
                            break;
                        case '[':
                            bracketCount++;
                            depth++;
                            if (depth > MAX_JSON_DEPTH) {
                                throw new SecurityException("JSON structure too deep (max depth: " + MAX_JSON_DEPTH + ")");
                            }
                            break;
                        case ']':
                            bracketCount--;
                            depth--;
                            if (bracketCount < 0) {
                                throw new SecurityException("Invalid JSON structure: unmatched closing bracket");
                            }
                            break;
                    }
                }
            }
            
            if (braceCount != 0) {
                throw new SecurityException("Invalid JSON structure: unmatched braces");
            }
            
            if (bracketCount != 0) {
                throw new SecurityException("Invalid JSON structure: unmatched brackets");
            }
            
            if (inString) {
                throw new SecurityException("Invalid JSON structure: unterminated string");
            }
        }
    } 
   
    /**
     * XML structure validator.
     */
    private static class XmlStructureValidator {
        
        public void validate(String xml) throws SecurityException {
            // Basic XML validation - check for balanced tags
            int openTags = 0;
            boolean inTag = false;
            boolean inComment = false;
            
            for (int i = 0; i < xml.length() - 3; i++) {
                if (!inComment && i + 4 <= xml.length() && xml.substring(i, i + 4).equals("<!--")) {
                    inComment = true;
                    i += 3;
                    continue;
                }
                
                if (inComment && i + 3 <= xml.length() && xml.substring(i, i + 3).equals("-->")) {
                    inComment = false;
                    i += 2;
                    continue;
                }
                
                if (!inComment) {
                    char c = xml.charAt(i);
                    
                    if (c == '<') {
                        inTag = true;
                        // Check if it's a closing tag
                        if (i + 1 < xml.length() && xml.charAt(i + 1) == '/') {
                            openTags--;
                        } else if (i + 1 < xml.length() && xml.charAt(i + 1) != '!' && xml.charAt(i + 1) != '?') {
                            openTags++;
                        }
                    } else if (c == '>') {
                        inTag = false;
                        // Check if it's a self-closing tag
                        if (i > 0 && xml.charAt(i - 1) == '/') {
                            openTags--;
                        }
                    }
                }
            }
            
            if (openTags != 0) {
                throw new SecurityException("Invalid XML structure: unmatched tags");
            }
        }
    }
}