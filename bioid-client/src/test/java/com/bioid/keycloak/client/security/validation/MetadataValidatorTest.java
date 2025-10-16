package com.bioid.keycloak.client.security.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for MetadataValidator.
 */
class MetadataValidatorTest {
    
    private MetadataValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new MetadataValidator();
    }
    
    @Test
    @DisplayName("Should accept valid JSON metadata")
    void testValidJsonMetadata() {
        // Given
        String validJson = "{\"userId\":\"user123\",\"timestamp\":\"2023-01-01T00:00:00Z\"}";
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> validator.validateMetadata(validJson));
    }
    
    @Test
    @DisplayName("Should accept valid XML metadata")
    void testValidXmlMetadata() {
        // Given
        String validXml = "<metadata><userId>user123</userId><timestamp>2023-01-01T00:00:00Z</timestamp></metadata>";
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> validator.validateMetadata(validXml));
    }
    
    @Test
    @DisplayName("Should accept plain text metadata")
    void testPlainTextMetadata() {
        // Given
        String plainText = "Simple metadata content without special formatting";
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> validator.validateMetadata(plainText));
    }
    
    @Test
    @DisplayName("Should reject null metadata")
    void testNullMetadata() {
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateMetadata(null));
        
        assertEquals("Metadata cannot be null or empty", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject empty metadata")
    void testEmptyMetadata() {
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateMetadata(""));
        
        assertEquals("Metadata cannot be null or empty", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject metadata that is too large")
    void testMetadataTooLarge() {
        // Given - Metadata larger than 10KB
        StringBuilder largeMetadata = new StringBuilder();
        for (int i = 0; i < 11000; i++) {
            largeMetadata.append("a");
        }
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateMetadata(largeMetadata.toString()));
        
        assertEquals("Metadata exceeds maximum allowed size", exception.getMessage());
    }   
 
    @Test
    @DisplayName("Should reject metadata with SQL injection patterns")
    void testSqlInjectionPatterns() {
        // Given
        String maliciousMetadata = "'; DROP TABLE users; --";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateMetadata(maliciousMetadata));
        
        assertEquals("Metadata contains SQL injection patterns", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject metadata with XSS patterns")
    void testXssPatterns() {
        // Given
        String xssMetadata = "<script>alert('xss')</script>";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateMetadata(xssMetadata));
        
        assertEquals("Metadata contains XSS patterns", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject metadata with dangerous patterns")
    void testDangerousPatterns() {
        // Given
        String dangerousMetadata = "javascript:alert('dangerous')";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateMetadata(dangerousMetadata));
        
        assertEquals("Metadata contains XSS patterns", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject invalid JSON structure")
    void testInvalidJsonStructure() {
        // Given - JSON with unmatched braces
        String invalidJson = "{\"key\":\"value\"";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateMetadata(invalidJson));
        
        assertEquals("Invalid JSON structure: unmatched braces", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject JSON that is too deep")
    void testJsonTooDeep() {
        // Given - JSON with excessive nesting
        StringBuilder deepJson = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            deepJson.append("{\"level").append(i).append("\":");
        }
        deepJson.append("\"value\"");
        for (int i = 0; i < 15; i++) {
            deepJson.append("}");
        }
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateMetadata(deepJson.toString()));
        
        assertEquals("JSON structure too deep (max depth: 10)", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject invalid XML structure")
    void testInvalidXmlStructure() {
        // Given - XML with unmatched tags
        String invalidXml = "<root><child>content</root>";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateMetadata(invalidXml));
        
        assertEquals("Invalid XML structure: unmatched tags", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should sanitize metadata for logging")
    void testSanitizeForLogging() {
        // Given
        String unsafeMetadata = "<script>alert('test')</script>";
        
        // When
        String sanitized = validator.sanitizeForLogging(unsafeMetadata);
        
        // Then
        assertFalse(sanitized.contains("<"));
        assertFalse(sanitized.contains(">"));
        assertTrue(sanitized.contains("_"));
    }
    
    @Test
    @DisplayName("Should provide correct maximum metadata size")
    void testGetMaxMetadataSize() {
        // When & Then
        assertEquals(10 * 1024, validator.getMaxMetadataSize()); // 10KB
    }
}