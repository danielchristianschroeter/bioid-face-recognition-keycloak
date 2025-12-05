package com.bioid.keycloak.failedauth.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * Service for encrypting and decrypting failed authentication images.
 * 
 * Uses AES-256-GCM for authenticated encryption with associated data (AEAD).
 * This provides both confidentiality and integrity protection.
 */
public class EncryptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes (96 bits recommended for GCM)
    private static final int GCM_TAG_LENGTH = 128; // bits (16 bytes)
    
    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom;
    
    static {
        // Add Bouncy Castle provider if not already added
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    /**
     * Create encryption service with a generated key.
     * In production, the key should be loaded from secure key management.
     */
    public EncryptionService() {
        try {
            // In production, load key from secure key management (e.g., AWS KMS, HashiCorp Vault)
            // For now, generate a key (this should be persisted securely)
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE, new SecureRandom());
            this.encryptionKey = keyGen.generateKey();
            this.secureRandom = new SecureRandom();
            
            logger.info("Encryption service initialized with AES-256-GCM");
        } catch (Exception e) {
            logger.error("Failed to initialize encryption service", e);
            throw new RuntimeException("Failed to initialize encryption service", e);
        }
    }
    
    /**
     * Create encryption service with a specific key.
     * 
     * @param keyBytes The 256-bit (32-byte) encryption key
     */
    public EncryptionService(byte[] keyBytes) {
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Key must be 256 bits (32 bytes)");
        }
        this.encryptionKey = new SecretKeySpec(keyBytes, ALGORITHM);
        this.secureRandom = new SecureRandom();
        logger.info("Encryption service initialized with provided key");
    }
    
    /**
     * Encrypt data using AES-256-GCM.
     * 
     * @param plaintext The data to encrypt
     * @return Encrypted data with IV prepended (IV + ciphertext + tag)
     * @throws Exception if encryption fails
     */
    public byte[] encrypt(byte[] plaintext) throws Exception {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec);
            
            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // Prepend IV to ciphertext (IV + ciphertext + tag)
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);
            
            return encrypted;
            
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new Exception("Failed to encrypt data", e);
        }
    }
    
    /**
     * Decrypt data using AES-256-GCM.
     * 
     * @param encrypted The encrypted data (IV + ciphertext + tag)
     * @return Decrypted plaintext
     * @throws Exception if decryption fails or authentication tag is invalid
     */
    public byte[] decrypt(byte[] encrypted) throws Exception {
        try {
            if (encrypted.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted data is too short");
            }
            
            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, GCM_IV_LENGTH);
            
            // Extract ciphertext + tag
            byte[] ciphertext = new byte[encrypted.length - GCM_IV_LENGTH];
            System.arraycopy(encrypted, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            // Initialize cipher
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec);
            
            // Decrypt and verify authentication tag
            return cipher.doFinal(ciphertext);
            
        } catch (Exception e) {
            logger.error("Decryption failed", e);
            throw new Exception("Failed to decrypt data (possible tampering)", e);
        }
    }
    
    /**
     * Calculate SHA-256 checksum of data.
     * 
     * @param data The data to checksum
     * @return Hex-encoded SHA-256 checksum
     */
    public String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (Exception e) {
            logger.error("Failed to calculate checksum", e);
            return null;
        }
    }
    
    /**
     * Verify checksum of data.
     * 
     * @param data The data to verify
     * @param expectedChecksum The expected checksum (hex-encoded)
     * @return true if checksum matches
     */
    public boolean verifyChecksum(byte[] data, String expectedChecksum) {
        String actualChecksum = calculateChecksum(data);
        return actualChecksum != null && actualChecksum.equals(expectedChecksum);
    }
    
    /**
     * Get the encryption key as base64 (for secure storage/retrieval).
     * WARNING: Handle with extreme care!
     * 
     * @return Base64-encoded encryption key
     */
    public String getKeyAsBase64() {
        return Base64.getEncoder().encodeToString(encryptionKey.getEncoded());
    }
    
    /**
     * Convert byte array to hex string.
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Securely wipe sensitive data from memory.
     * 
     * @param data The data to wipe
     */
    public static void secureWipe(byte[] data) {
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                data[i] = 0;
            }
        }
    }
}
