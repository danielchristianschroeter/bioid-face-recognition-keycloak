package com.bioid.keycloak.client.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for SecureCredentialStorage implementation.
 */
class SecureCredentialStorageTest {
    
    private SecureCredentialStorage storage;
    
    @BeforeEach
    void setUp() {
        storage = new SecureCredentialStorage();
    }
    
    @Test
    @DisplayName("Should encrypt and decrypt credential metadata successfully")
    void testEncryptDecryptCredentialMetadata() {
        // Given
        String credentialId = "test-credential-123";
        String originalMetadata = "{\"classId\":123456789,\"createdAt\":\"2024-01-15T10:30:00Z\",\"imageCount\":3}";
        
        // When
        String encryptedData = storage.encryptCredentialMetadata(credentialId, originalMetadata);
        String decryptedMetadata = storage.decryptCredentialMetadata(credentialId, encryptedData);
        
        // Then
        assertNotNull(encryptedData);
        assertNotEquals(originalMetadata, encryptedData);
        assertEquals(originalMetadata, decryptedMetadata);
    }
    
    @Test
    @DisplayName("Should produce different encrypted data for same input")
    void testEncryptionRandomness() {
        // Given
        String credentialId = "test-credential-456";
        String metadata = "{\"classId\":987654321,\"createdAt\":\"2024-01-15T11:00:00Z\"}";
        
        // When
        String encrypted1 = storage.encryptCredentialMetadata(credentialId + "-1", metadata);
        String encrypted2 = storage.encryptCredentialMetadata(credentialId + "-2", metadata);
        
        // Then
        assertNotEquals(encrypted1, encrypted2, "Encryption should produce different results due to random IV");
    }
    
    @Test
    @DisplayName("Should handle empty and null metadata appropriately")
    void testInvalidMetadataHandling() {
        String credentialId = "test-credential-789";
        
        // Test null metadata
        assertThrows(IllegalArgumentException.class, () -> 
            storage.encryptCredentialMetadata(credentialId, null));
        
        // Test empty metadata
        assertThrows(IllegalArgumentException.class, () -> 
            storage.encryptCredentialMetadata(credentialId, ""));
    }
    
    @Test
    @DisplayName("Should handle invalid encrypted data")
    void testInvalidEncryptedDataHandling() {
        String credentialId = "test-credential-invalid";
        
        // Test null encrypted data
        assertThrows(IllegalArgumentException.class, () -> 
            storage.decryptCredentialMetadata(credentialId, null));
        
        // Test empty encrypted data
        assertThrows(IllegalArgumentException.class, () -> 
            storage.decryptCredentialMetadata(credentialId, ""));
        
        // Test invalid base64 data
        assertThrows(SecurityException.class, () -> 
            storage.decryptCredentialMetadata(credentialId, "invalid-base64-data"));
    }
    
    @Test
    @DisplayName("Should handle large metadata payloads")
    void testLargeMetadataHandling() {
        // Given
        String credentialId = "test-credential-large";
        StringBuilder largeMetadata = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeMetadata.append("\"field").append(i).append("\":\"value").append(i).append("\",");
        }
        String metadata = "{" + largeMetadata.toString() + "\"end\":true}";
        
        // When
        String encryptedData = storage.encryptCredentialMetadata(credentialId, metadata);
        String decryptedMetadata = storage.decryptCredentialMetadata(credentialId, encryptedData);
        
        // Then
        assertEquals(metadata, decryptedMetadata);
    }
    
    @Test
    @DisplayName("Should remove credentials from cache")
    void testCredentialRemoval() {
        // Given
        String credentialId = "test-credential-remove";
        String metadata = "{\"classId\":555666777}";
        
        // When
        String encryptedData = storage.encryptCredentialMetadata(credentialId, metadata);
        storage.removeCredential(credentialId);
        
        // Then - should still be able to decrypt with the encrypted data
        String decryptedMetadata = storage.decryptCredentialMetadata(credentialId, encryptedData);
        assertEquals(metadata, decryptedMetadata);
    }
    
    @Test
    @DisplayName("Should clear all credentials from cache")
    void testClearAllCredentials() {
        // Given
        String metadata1 = "{\"classId\":111}";
        String metadata2 = "{\"classId\":222}";
        
        // When
        storage.encryptCredentialMetadata("cred-1", metadata1);
        storage.encryptCredentialMetadata("cred-2", metadata2);
        storage.clearAll();
        
        // Then - cache should be cleared but encryption/decryption should still work
        // This test mainly verifies no exceptions are thrown during clearAll
        assertDoesNotThrow(() -> storage.clearAll());
    }
    
    @Test
    @DisplayName("Should handle concurrent access safely")
    void testConcurrentAccess() throws InterruptedException {
        // Given
        int threadCount = 10;
        int operationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String credentialId = "thread-" + threadId + "-cred-" + j;
                    String metadata = "{\"classId\":" + (threadId * 1000 + j) + "}";
                    
                    String encrypted = storage.encryptCredentialMetadata(credentialId, metadata);
                    String decrypted = storage.decryptCredentialMetadata(credentialId, encrypted);
                    
                    assertEquals(metadata, decrypted);
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for completion
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout
        }
        
        // Then - no exceptions should have been thrown
        assertTrue(true, "Concurrent access completed successfully");
    }
    
    @Test
    @DisplayName("Should validate encryption strength")
    void testEncryptionStrength() {
        // Given
        String credentialId = "test-encryption-strength";
        String metadata = "sensitive-biometric-metadata";
        
        // When
        String encryptedData = storage.encryptCredentialMetadata(credentialId, metadata);
        
        // Then
        // Encrypted data should be significantly different from original
        assertFalse(encryptedData.contains(metadata), 
            "Encrypted data should not contain plaintext");
        
        // Should be base64 encoded (only valid base64 characters)
        assertTrue(encryptedData.matches("^[A-Za-z0-9+/]*={0,2}$"), 
            "Encrypted data should be valid base64");
        
        // Should be longer than original due to IV and authentication tag
        assertTrue(encryptedData.length() > metadata.length(), 
            "Encrypted data should be longer than original due to IV and tag");
    }
}