package com.bioid.keycloak.client.security;

import com.bioid.keycloak.client.security.ratelimit.RateLimiter;
import com.bioid.keycloak.client.security.validation.ImageValidator;
import com.bioid.keycloak.client.security.validation.InputValidator;
import com.bioid.keycloak.client.security.validation.MetadataValidator;
import com.bioid.keycloak.client.security.validation.PathValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Security tests for SecurityValidator facade.
 */
class SecurityValidatorTest {
    
    @Mock
    private InputValidator inputValidator;
    
    @Mock
    private ImageValidator imageValidator;
    
    @Mock
    private MetadataValidator metadataValidator;
    
    @Mock
    private PathValidator pathValidator;
    
    @Mock
    private RateLimiter rateLimiter;
    
    private SecurityValidator securityValidator;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        securityValidator = new SecurityValidator(
            inputValidator, imageValidator, metadataValidator, pathValidator, rateLimiter
        );
    }
    
    @Test
    @DisplayName("Should delegate user ID validation to InputValidator")
    void testValidateUserId() {
        // Given
        String userId = "user123";
        
        // When
        securityValidator.validateUserId(userId);
        
        // Then
        verify(inputValidator).validateUserId(userId);
    }
    
    @Test
    @DisplayName("Should delegate session ID validation to InputValidator")
    void testValidateSessionId() {
        // Given
        String sessionId = "session123";
        
        // When
        securityValidator.validateSessionId(sessionId);
        
        // Then
        verify(inputValidator).validateSessionId(sessionId);
    }
    
    @Test
    @DisplayName("Should delegate credential ID validation to InputValidator")
    void testValidateCredentialId() {
        // Given
        String credentialId = "cred123";
        
        // When
        securityValidator.validateCredentialId(credentialId);
        
        // Then
        verify(inputValidator).validateCredentialId(credentialId);
    }
    
    @Test
    @DisplayName("Should delegate image validation to ImageValidator")
    void testValidateImageData() {
        // Given
        byte[] imageData = new byte[1024];
        
        // When
        securityValidator.validateImageData(imageData);
        
        // Then
        verify(imageValidator).validateImageData(imageData);
    }    

    @Test
    @DisplayName("Should delegate metadata validation to MetadataValidator")
    void testValidateMetadata() {
        // Given
        String metadata = "{\"key\":\"value\"}";
        
        // When
        securityValidator.validateMetadata(metadata);
        
        // Then
        verify(metadataValidator).validateMetadata(metadata);
    }
    
    @Test
    @DisplayName("Should delegate file path validation to PathValidator")
    void testValidateFilePath() {
        // Given
        String filePath = "documents/file.txt";
        
        // When
        securityValidator.validateFilePath(filePath);
        
        // Then
        verify(pathValidator).validateFilePath(filePath);
    }
    
    @Test
    @DisplayName("Should delegate rate limiting to RateLimiter")
    void testCheckRateLimit() {
        // Given
        String identifier = "user123";
        
        // When
        securityValidator.checkRateLimit(identifier);
        
        // Then
        verify(rateLimiter).checkRateLimit(identifier);
    }
    
    @Test
    @DisplayName("Should validate all provided inputs comprehensively")
    void testValidateAll() {
        // Given
        String userId = "user123";
        String sessionId = "session123";
        String credentialId = "cred123";
        byte[] imageData = new byte[1024];
        String metadata = "{\"key\":\"value\"}";
        String filePath = "documents/file.txt";
        
        // When
        securityValidator.validateAll(userId, sessionId, credentialId, imageData, metadata, filePath);
        
        // Then
        verify(inputValidator).validateUserId(userId);
        verify(inputValidator).validateSessionId(sessionId);
        verify(inputValidator).validateCredentialId(credentialId);
        verify(imageValidator).validateImageData(imageData);
        verify(metadataValidator).validateMetadata(metadata);
        verify(pathValidator).validateFilePath(filePath);
        verify(rateLimiter).checkRateLimit(userId);
    }
    
    @Test
    @DisplayName("Should skip null inputs in comprehensive validation")
    void testValidateAllWithNullInputs() {
        // Given - Only userId provided
        String userId = "user123";
        
        // When
        securityValidator.validateAll(userId, null, null, null, null, null);
        
        // Then
        verify(inputValidator).validateUserId(userId);
        verify(rateLimiter).checkRateLimit(userId);
        
        // Verify other validators are not called
        verifyNoInteractions(imageValidator);
        verifyNoInteractions(metadataValidator);
        verifyNoInteractions(pathValidator);
        verify(inputValidator, never()).validateSessionId(any());
        verify(inputValidator, never()).validateCredentialId(any());
    }
    
    @Test
    @DisplayName("Should delegate cleanup to RateLimiter")
    void testCleanupRateLimitEntries() {
        // When
        securityValidator.cleanupRateLimitEntries();
        
        // Then
        verify(rateLimiter).cleanupRateLimitEntries();
    }
    
    @Test
    @DisplayName("Should delegate rate limit stats to RateLimiter")
    void testGetRateLimitStats() {
        // Given
        RateLimiter.RateLimitStats expectedStats = new RateLimiter.RateLimitStats(10, 5, 2);
        when(rateLimiter.getRateLimitStats()).thenReturn(expectedStats);
        
        // When
        RateLimiter.RateLimitStats actualStats = securityValidator.getRateLimitStats();
        
        // Then
        assertEquals(expectedStats, actualStats);
        verify(rateLimiter).getRateLimitStats();
    }
    
    @Test
    @DisplayName("Should provide access to specialized validators")
    void testGetSpecializedValidators() {
        // When & Then
        assertEquals(inputValidator, securityValidator.getInputValidator());
        assertEquals(imageValidator, securityValidator.getImageValidator());
        assertEquals(metadataValidator, securityValidator.getMetadataValidator());
        assertEquals(pathValidator, securityValidator.getPathValidator());
        assertEquals(rateLimiter, securityValidator.getRateLimiter());
    }
    
    @Test
    @DisplayName("Should create SecurityValidator with default validators")
    void testDefaultConstructor() {
        // When
        SecurityValidator defaultValidator = new SecurityValidator();
        
        // Then
        assertNotNull(defaultValidator.getInputValidator());
        assertNotNull(defaultValidator.getImageValidator());
        assertNotNull(defaultValidator.getMetadataValidator());
        assertNotNull(defaultValidator.getPathValidator());
        assertNotNull(defaultValidator.getRateLimiter());
    }
    
    @Test
    @DisplayName("Should propagate exceptions from specialized validators")
    void testExceptionPropagation() {
        // Given
        String userId = "malicious_user";
        SecurityException expectedException = new SecurityException("Invalid user ID");
        doThrow(expectedException).when(inputValidator).validateUserId(userId);
        
        // When & Then
        SecurityException actualException = assertThrows(SecurityException.class, 
            () -> securityValidator.validateUserId(userId));
        
        assertEquals(expectedException, actualException);
        verify(inputValidator).validateUserId(userId);
    }
}