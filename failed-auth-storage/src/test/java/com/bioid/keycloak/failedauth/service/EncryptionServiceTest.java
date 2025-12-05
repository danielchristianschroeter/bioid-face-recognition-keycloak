package com.bioid.keycloak.failedauth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for EncryptionService.
 */
@DisplayName("EncryptionService Tests")
class EncryptionServiceTest {
    
    private EncryptionService encryptionService;
    
    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
    }
    
    @Test
    @DisplayName("Should encrypt and decrypt data successfully")
    void testEncryptDecrypt() throws Exception {
        // Given
        String originalText = "This is a test message for encryption";
        byte[] plaintext = originalText.getBytes(StandardCharsets.UTF_8);
        
        // When
        byte[] encrypted = encryptionService.encrypt(plaintext);
        byte[] decrypted = encryptionService.decrypt(encrypted);
        
        // Then
        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(new String(decrypted, StandardCharsets.UTF_8)).isEqualTo(originalText);
    }
    
    @Test
    @DisplayName("Should produce different ciphertext for same plaintext (due to random IV)")
    void testRandomIV() throws Exception {
        // Given
        byte[] plaintext = "Test message".getBytes(StandardCharsets.UTF_8);
        
        // When
        byte[] encrypted1 = encryptionService.encrypt(plaintext);
        byte[] encrypted2 = encryptionService.encrypt(plaintext);
        
        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        
        // But both should decrypt to same plaintext
        byte[] decrypted1 = encryptionService.decrypt(encrypted1);
        byte[] decrypted2 = encryptionService.decrypt(encrypted2);
        assertThat(decrypted1).isEqualTo(plaintext);
        assertThat(decrypted2).isEqualTo(plaintext);
    }
    
    @Test
    @DisplayName("Should handle empty data")
    void testEmptyData() throws Exception {
        // Given
        byte[] emptyData = new byte[0];
        
        // When
        byte[] encrypted = encryptionService.encrypt(emptyData);
        byte[] decrypted = encryptionService.decrypt(encrypted);
        
        // Then
        assertThat(decrypted).isEmpty();
    }
    
    @Test
    @DisplayName("Should handle large data")
    void testLargeData() throws Exception {
        // Given
        byte[] largeData = new byte[1024 * 1024]; // 1MB
        new SecureRandom().nextBytes(largeData);
        
        // When
        byte[] encrypted = encryptionService.encrypt(largeData);
        byte[] decrypted = encryptionService.decrypt(encrypted);
        
        // Then
        assertThat(decrypted).isEqualTo(largeData);
    }
    
    @Test
    @DisplayName("Should fail decryption with tampered data")
    void testTamperedData() throws Exception {
        // Given
        byte[] plaintext = "Secret message".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = encryptionService.encrypt(plaintext);
        
        // When - tamper with encrypted data
        encrypted[encrypted.length - 1] ^= 1; // Flip one bit
        
        // Then
        assertThatThrownBy(() -> encryptionService.decrypt(encrypted))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("decrypt");
    }
    
    @Test
    @DisplayName("Should fail decryption with truncated data")
    void testTruncatedData() {
        // Given
        byte[] truncatedData = new byte[5]; // Too short
        
        // Then
        assertThatThrownBy(() -> encryptionService.decrypt(truncatedData))
            .isInstanceOf(Exception.class);
    }
    
    @Test
    @DisplayName("Should calculate checksum correctly")
    void testChecksum() {
        // Given
        byte[] data = "Test data for checksum".getBytes(StandardCharsets.UTF_8);
        
        // When
        String checksum1 = encryptionService.calculateChecksum(data);
        String checksum2 = encryptionService.calculateChecksum(data);
        
        // Then
        assertThat(checksum1).isNotNull();
        assertThat(checksum1).hasSize(64); // SHA-256 produces 64 hex characters
        assertThat(checksum1).isEqualTo(checksum2); // Same data = same checksum
    }
    
    @Test
    @DisplayName("Should produce different checksums for different data")
    void testDifferentChecksums() {
        // Given
        byte[] data1 = "Data 1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "Data 2".getBytes(StandardCharsets.UTF_8);
        
        // When
        String checksum1 = encryptionService.calculateChecksum(data1);
        String checksum2 = encryptionService.calculateChecksum(data2);
        
        // Then
        assertThat(checksum1).isNotEqualTo(checksum2);
    }
    
    @Test
    @DisplayName("Should verify checksum correctly")
    void testVerifyChecksum() {
        // Given
        byte[] data = "Test data".getBytes(StandardCharsets.UTF_8);
        String checksum = encryptionService.calculateChecksum(data);
        
        // When/Then
        assertThat(encryptionService.verifyChecksum(data, checksum)).isTrue();
        
        // Modify data
        data[0] ^= 1;
        assertThat(encryptionService.verifyChecksum(data, checksum)).isFalse();
    }
    
    @Test
    @DisplayName("Should create service with custom key")
    void testCustomKey() throws Exception {
        // Given
        byte[] customKey = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(customKey);
        EncryptionService customService = new EncryptionService(customKey);
        
        byte[] plaintext = "Test with custom key".getBytes(StandardCharsets.UTF_8);
        
        // When
        byte[] encrypted = customService.encrypt(plaintext);
        byte[] decrypted = customService.decrypt(encrypted);
        
        // Then
        assertThat(decrypted).isEqualTo(plaintext);
    }
    
    @Test
    @DisplayName("Should reject invalid key size")
    void testInvalidKeySize() {
        // Given
        byte[] invalidKey = new byte[16]; // Only 128 bits
        
        // Then
        assertThatThrownBy(() -> new EncryptionService(invalidKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("256 bits");
    }
    
    @Test
    @DisplayName("Should get key as base64")
    void testGetKeyAsBase64() {
        // When
        String keyBase64 = encryptionService.getKeyAsBase64();
        
        // Then
        assertThat(keyBase64).isNotNull();
        assertThat(keyBase64).isNotEmpty();
        
        // Should be valid base64
        assertThatCode(() -> java.util.Base64.getDecoder().decode(keyBase64))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("Should securely wipe data")
    void testSecureWipe() {
        // Given
        byte[] sensitiveData = "Sensitive information".getBytes(StandardCharsets.UTF_8);
        byte[] originalCopy = sensitiveData.clone();
        
        // When
        EncryptionService.secureWipe(sensitiveData);
        
        // Then
        assertThat(sensitiveData).isNotEqualTo(originalCopy);
        assertThat(sensitiveData).containsOnly((byte) 0);
    }
    
    @Test
    @DisplayName("Should handle null in secure wipe")
    void testSecureWipeNull() {
        // When/Then
        assertThatCode(() -> EncryptionService.secureWipe(null))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("Should encrypt binary data correctly")
    void testBinaryData() throws Exception {
        // Given
        byte[] binaryData = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryData[i] = (byte) i;
        }
        
        // When
        byte[] encrypted = encryptionService.encrypt(binaryData);
        byte[] decrypted = encryptionService.decrypt(encrypted);
        
        // Then
        assertThat(decrypted).isEqualTo(binaryData);
    }
    
    @Test
    @DisplayName("Should maintain data integrity through multiple encrypt/decrypt cycles")
    void testMultipleCycles() throws Exception {
        // Given
        byte[] originalData = "Test data for multiple cycles".getBytes(StandardCharsets.UTF_8);
        byte[] currentData = originalData;
        
        // When - encrypt and decrypt 10 times
        for (int i = 0; i < 10; i++) {
            byte[] encrypted = encryptionService.encrypt(currentData);
            currentData = encryptionService.decrypt(encrypted);
        }
        
        // Then
        assertThat(currentData).isEqualTo(originalData);
    }
}
