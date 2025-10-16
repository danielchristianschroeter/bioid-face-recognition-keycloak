package com.bioid.keycloak.client.security.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for PathValidator.
 */
class PathValidatorTest {
    
    private PathValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new PathValidator();
    }
    
    @Test
    @DisplayName("Should accept valid relative path")
    void testValidRelativePath() {
        // Given
        String validPath = "documents/file.txt";
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> validator.validateFilePath(validPath));
    }
    
    @Test
    @DisplayName("Should accept valid nested relative path")
    void testValidNestedRelativePath() {
        // Given
        String validPath = "folder/subfolder/file.pdf";
        
        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> validator.validateFilePath(validPath));
    }
    
    @Test
    @DisplayName("Should reject null path")
    void testNullPath() {
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(null));
        
        assertEquals("File path cannot be null or empty", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject empty path")
    void testEmptyPath() {
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(""));
        
        assertEquals("File path cannot be null or empty", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject path traversal with double dots")
    void testPathTraversalDoubleDots() {
        // Given
        String maliciousPath = "../../../etc/passwd";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(maliciousPath));
        
        assertEquals("File path contains potentially dangerous elements", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject absolute Unix path")
    void testAbsoluteUnixPath() {
        // Given
        String absolutePath = "/etc/passwd";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(absolutePath));
        
        assertEquals("Absolute paths are not allowed", exception.getMessage());
    }  
  
    @Test
    @DisplayName("Should reject absolute Windows path")
    void testAbsoluteWindowsPath() {
        // Given
        String absolutePath = "C:\\Windows\\System32\\config";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(absolutePath));
        
        assertEquals("Absolute paths are not allowed", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject UNC path")
    void testUncPath() {
        // Given
        String uncPath = "\\\\server\\share\\file.txt";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(uncPath));
        
        assertEquals("Absolute paths are not allowed", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject path with null bytes")
    void testPathWithNullBytes() {
        // Given
        String pathWithNull = "file\0.txt";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(pathWithNull));
        
        assertEquals("File path contains null bytes", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject reserved Windows filename")
    void testReservedWindowsFilename() {
        // Given
        String reservedPath = "documents/CON.txt";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(reservedPath));
        
        assertEquals("File path contains reserved filename", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject path with invalid characters")
    void testPathWithInvalidCharacters() {
        // Given
        String invalidPath = "file<name>.txt";
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(invalidPath));
        
        assertEquals("Invalid file path format", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should reject path that is too long")
    void testPathTooLong() {
        // Given - Path longer than 260 characters
        StringBuilder longPath = new StringBuilder();
        for (int i = 0; i < 270; i++) {
            longPath.append("a");
        }
        
        // When & Then
        SecurityException exception = assertThrows(SecurityException.class, 
            () -> validator.validateFilePath(longPath.toString()));
        
        assertEquals("File path exceeds maximum allowed length", exception.getMessage());
    }
    
    @Test
    @DisplayName("Should normalize valid path")
    void testNormalizePath() {
        // Given
        String path = "folder/subfolder/file.txt";
        
        // When
        String normalized = assertDoesNotThrow(() -> validator.normalizePath(path));
        
        // Then
        assertEquals("folder\\subfolder\\file.txt", normalized); // Windows normalization
    }
    
    @Test
    @DisplayName("Should provide correct maximum path length")
    void testGetMaxPathLength() {
        // When & Then
        assertEquals(260, validator.getMaxPathLength()); // Windows MAX_PATH
    }
}