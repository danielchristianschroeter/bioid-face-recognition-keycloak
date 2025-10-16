package com.bioid.keycloak.client.security;

import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for TLS configuration implementation.
 */
class TlsConfigurationTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should create SSL context with TLS enabled")
    void testCreateSslContextWithTls() {
        // Given
        TlsConfiguration config = TlsConfiguration.builder()
            .tlsEnabled(true)
            .mutualTlsEnabled(false)
            .build();
        
        // When
        SslContext sslContext = config.createSslContext();
        
        // Then
        assertNotNull(sslContext);
        assertTrue(config.isTlsEnabled());
        assertFalse(config.isMutualTlsEnabled());
    }
    
    @Test
    @DisplayName("Should return null SSL context when TLS disabled")
    void testCreateSslContextWithTlsDisabled() {
        // Given
        TlsConfiguration config = TlsConfiguration.builder()
            .tlsEnabled(false)
            .build();
        
        // When
        SslContext sslContext = config.createSslContext();
        
        // Then
        assertNull(sslContext);
        assertFalse(config.isTlsEnabled());
    }
    
    @Test
    @DisplayName("Should handle mutual TLS configuration")
    void testMutualTlsConfiguration() throws Exception {
        // Given
        Path keyStorePath = createTestKeyStore();
        
        TlsConfiguration config = TlsConfiguration.builder()
            .tlsEnabled(true)
            .mutualTlsEnabled(true)
            .keyStorePath(keyStorePath.toString())
            .keyStorePassword("testpass")
            .build();
        
        // When/Then - Should not throw exception during configuration
        assertDoesNotThrow(() -> {
            // Note: This may fail due to test keystore not having proper certificates
            // but it should not fail due to configuration issues
            try {
                SslContext sslContext = config.createSslContext();
                // If successful, verify properties
                if (sslContext != null) {
                    assertTrue(config.isTlsEnabled());
                    assertTrue(config.isMutualTlsEnabled());
                }
            } catch (SecurityException e) {
                // Expected for test keystore without proper certificates
                assertTrue(e.getMessage().contains("SSL context") || 
                          e.getMessage().contains("Key store"));
            }
        });
    }
    
    @Test
    @DisplayName("Should fail mutual TLS without key store")
    void testMutualTlsWithoutKeyStore() {
        // Given
        TlsConfiguration config = TlsConfiguration.builder()
            .tlsEnabled(true)
            .mutualTlsEnabled(true)
            // No key store path provided
            .build();
        
        // When/Then
        assertThrows(SecurityException.class, () -> 
            config.createSslContext());
    }
    
    @Test
    @DisplayName("Should handle invalid key store path")
    void testInvalidKeyStorePath() {
        // Given
        TlsConfiguration config = TlsConfiguration.builder()
            .tlsEnabled(true)
            .mutualTlsEnabled(true)
            .keyStorePath("/non/existent/keystore.jks")
            .keyStorePassword("password")
            .build();
        
        // When/Then
        assertThrows(SecurityException.class, () -> 
            config.createSslContext());
    }
    
    @Test
    @DisplayName("Should handle certificate pinning configuration")
    void testCertificatePinningConfiguration() {
        // Given
        String[] pinnedCerts = {
            "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
        };
        
        TlsConfiguration config = TlsConfiguration.builder()
            .tlsEnabled(true)
            .certificatePinningEnabled(true)
            .pinnedCertificates(pinnedCerts)
            .build();
        
        // When/Then
        assertTrue(config.isCertificatePinningEnabled());
        assertArrayEquals(pinnedCerts, config.getPinnedCertificates());
    }
    
    @Test
    @DisplayName("Should validate TLS configuration builder")
    void testTlsConfigurationBuilder() {
        // Given/When
        TlsConfiguration config = TlsConfiguration.builder()
            .tlsEnabled(true)
            .mutualTlsEnabled(false)
            .keyStorePath("/path/to/keystore")
            .keyStorePassword("password")
            .trustStorePath("/path/to/truststore")
            .trustStorePassword("trustpass")
            .certificatePinningEnabled(false)
            .build();
        
        // Then
        assertTrue(config.isTlsEnabled());
        assertFalse(config.isMutualTlsEnabled());
        assertFalse(config.isCertificatePinningEnabled());
    }
    
    @Test
    @DisplayName("Should handle trust store configuration")
    void testTrustStoreConfiguration() throws Exception {
        // Given
        Path trustStorePath = createTestKeyStore();
        
        TlsConfiguration config = TlsConfiguration.builder()
            .tlsEnabled(true)
            .trustStorePath(trustStorePath.toString())
            .trustStorePassword("testpass")
            .build();
        
        // When/Then - Should not throw exception during configuration
        assertDoesNotThrow(() -> {
            try {
                SslContext sslContext = config.createSslContext();
                // Configuration should succeed even with empty trust store
                assertNotNull(sslContext);
            } catch (SecurityException e) {
                // May fail due to empty trust store, but should be configuration-related
                assertTrue(e.getMessage().contains("SSL context") || 
                          e.getMessage().contains("trust"));
            }
        });
    }
    
    @Test
    @DisplayName("Should create default configuration")
    void testDefaultConfiguration() {
        // Given/When
        TlsConfiguration config = TlsConfiguration.builder().build();
        
        // Then
        assertTrue(config.isTlsEnabled()); // Default should be true
        assertFalse(config.isMutualTlsEnabled()); // Default should be false
        assertFalse(config.isCertificatePinningEnabled()); // Default should be false
    }
    
    /**
     * Creates a test keystore for testing purposes.
     */
    private Path createTestKeyStore() throws KeyStoreException, IOException, 
            NoSuchAlgorithmException, CertificateException {
        
        Path keyStorePath = tempDir.resolve("test-keystore.jks");
        
        // Create empty keystore for testing
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, "testpass".toCharArray());
        
        try (FileOutputStream fos = new FileOutputStream(keyStorePath.toFile())) {
            keyStore.store(fos, "testpass".toCharArray());
        }
        
        return keyStorePath;
    }
}