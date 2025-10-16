package com.bioid.keycloak.client.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Secure storage for sensitive credential metadata with AES-GCM encryption.
 * 
 * Features:
 * - AES-256-GCM encryption for credential data
 * - Secure key derivation and management
 * - Memory-safe operations with automatic cleanup
 * - Thread-safe concurrent access
 * - Zero persistence of raw biometric data
 */
public class SecureCredentialStorage {
    
    private static final Logger logger = LoggerFactory.getLogger(SecureCredentialStorage.class);
    
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_LENGTH = 256; // 256 bits
    
    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom;
    private final ConcurrentMap<String, EncryptedCredential> credentialCache;
    
    public SecureCredentialStorage() {
        this.encryptionKey = generateEncryptionKey();
        this.secureRandom = new SecureRandom();
        this.credentialCache = new ConcurrentHashMap<>();
        
        logger.info("Initialized secure credential storage with AES-256-GCM encryption");
    }
    
    /**
     * Encrypts and stores credential metadata securely.
     * 
     * @param credentialId unique identifier for the credential
     * @param metadata credential metadata to encrypt
     * @return encrypted credential data for database storage
     * @throws SecurityException if encryption fails
     */
    public String encryptCredentialMetadata(String credentialId, String metadata) 
            throws SecurityException {
        
        if (metadata == null || metadata.isEmpty()) {
            throw new IllegalArgumentException("Credential metadata cannot be null or empty");
        }
        
        try {
            // Generate random IV for each encryption
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec);
            
            // Encrypt the metadata
            byte[] plaintextBytes = metadata.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertextBytes = cipher.doFinal(plaintextBytes);
            
            // Create encrypted credential object
            EncryptedCredential encryptedCredential = new EncryptedCredential(iv, ciphertextBytes);
            
            // Cache for potential reuse (with automatic cleanup)
            credentialCache.put(credentialId, encryptedCredential);
            
            // Return base64-encoded encrypted data for database storage
            String encryptedData = encryptedCredential.toBase64();
            
            // Clear sensitive data from memory
            clearArray(plaintextBytes);
            
            logger.debug("Encrypted credential metadata for ID: {}", credentialId);
            return encryptedData;
            
        } catch (Exception e) {
            logger.error("Failed to encrypt credential metadata for ID: {}", credentialId, e);
            throw new SecurityException("Credential encryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Decrypts stored credential metadata.
     * 
     * @param credentialId unique identifier for the credential
     * @param encryptedData base64-encoded encrypted credential data
     * @return decrypted credential metadata
     * @throws SecurityException if decryption fails
     */
    public String decryptCredentialMetadata(String credentialId, String encryptedData) 
            throws SecurityException {
        
        if (encryptedData == null || encryptedData.isEmpty()) {
            throw new IllegalArgumentException("Encrypted data cannot be null or empty");
        }
        
        try {
            // Parse encrypted credential from base64
            EncryptedCredential encryptedCredential = EncryptedCredential.fromBase64(encryptedData);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedCredential.iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec);
            
            // Decrypt the data
            byte[] plaintextBytes = cipher.doFinal(encryptedCredential.ciphertext);
            String metadata = new String(plaintextBytes, StandardCharsets.UTF_8);
            
            // Clear sensitive data from memory
            clearArray(plaintextBytes);
            
            logger.debug("Decrypted credential metadata for ID: {}", credentialId);
            return metadata;
            
        } catch (Exception e) {
            logger.error("Failed to decrypt credential metadata for ID: {}", credentialId, e);
            throw new SecurityException("Credential decryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Securely removes credential from cache and clears memory.
     * 
     * @param credentialId credential to remove
     */
    public void removeCredential(String credentialId) {
        EncryptedCredential removed = credentialCache.remove(credentialId);
        if (removed != null) {
            removed.clear();
            logger.debug("Removed and cleared credential from cache: {}", credentialId);
        }
    }
    
    /**
     * Clears all cached credentials and sensitive data from memory.
     */
    public void clearAll() {
        credentialCache.values().forEach(EncryptedCredential::clear);
        credentialCache.clear();
        logger.info("Cleared all cached credentials from memory");
    }
    
    /**
     * Generates a secure AES-256 encryption key using SecureRandom.
     */
    private SecretKey generateEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
            // Use SecureRandom for cryptographically strong key generation
            keyGenerator.init(KEY_LENGTH, new SecureRandom());
            SecretKey key = keyGenerator.generateKey();
            
            logger.debug("Generated AES-256 encryption key with SecureRandom");
            return key;
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to generate encryption key", e);
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
    
    /**
     * Securely clears sensitive data from byte array.
     */
    private void clearArray(byte[] array) {
        if (array != null) {
            java.util.Arrays.fill(array, (byte) 0);
        }
    }
    
    /**
     * Encrypted credential container with secure memory handling.
     */
    private static class EncryptedCredential {
        private final byte[] iv;
        private byte[] ciphertext;
        
        public EncryptedCredential(byte[] iv, byte[] ciphertext) {
            this.iv = iv.clone();
            this.ciphertext = ciphertext.clone();
        }
        
        public String toBase64() {
            // Combine IV and ciphertext for storage
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            
            String encoded = Base64.getEncoder().encodeToString(combined);
            
            // Clear temporary array
            java.util.Arrays.fill(combined, (byte) 0);
            
            return encoded;
        }
        
        public static EncryptedCredential fromBase64(String base64Data) {
            byte[] combined = Base64.getDecoder().decode(base64Data);
            
            if (combined.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted credential data");
            }
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            // Clear temporary array
            java.util.Arrays.fill(combined, (byte) 0);
            
            return new EncryptedCredential(iv, ciphertext);
        }
        
        public void clear() {
            if (ciphertext != null) {
                java.util.Arrays.fill(ciphertext, (byte) 0);
                ciphertext = null;
            }
            // IV is not sensitive, but clear for completeness
            java.util.Arrays.fill(iv, (byte) 0);
        }
    }
}