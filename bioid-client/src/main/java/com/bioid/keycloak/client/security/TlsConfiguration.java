package com.bioid.keycloak.client.security;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * TLS configuration for secure gRPC communication with BioID BWS service.
 * 
 * Provides support for:
 * - Standard TLS encryption
 * - Mutual TLS (mTLS) authentication
 * - Certificate validation and pinning
 * - Secure cipher suite selection
 */
public class TlsConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(TlsConfiguration.class);
    
    private final boolean tlsEnabled;
    private final boolean mutualTlsEnabled;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final String trustStorePath;
    private final String trustStorePassword;
    private final boolean certificatePinningEnabled;
    private final String[] pinnedCertificates;
    
    private TlsConfiguration(Builder builder) {
        this.tlsEnabled = builder.tlsEnabled;
        this.mutualTlsEnabled = builder.mutualTlsEnabled;
        this.keyStorePath = builder.keyStorePath;
        this.keyStorePassword = builder.keyStorePassword;
        this.trustStorePath = builder.trustStorePath;
        this.trustStorePassword = builder.trustStorePassword;
        this.certificatePinningEnabled = builder.certificatePinningEnabled;
        this.pinnedCertificates = builder.pinnedCertificates;
    }
    
    /**
     * Creates SSL context for gRPC channel configuration.
     * 
     * @return configured SSL context
     * @throws SecurityException if SSL context creation fails
     */
    public SslContext createSslContext() throws SecurityException {
        if (!tlsEnabled) {
            return null;
        }
        
        try {
            SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient();
            
            // Configure trust manager
            if (trustStorePath != null && !trustStorePath.isEmpty()) {
                TrustManagerFactory trustManagerFactory = createTrustManagerFactory();
                sslContextBuilder.trustManager(trustManagerFactory);
                logger.debug("Configured custom trust store: {}", trustStorePath);
            } else {
                // Use system default trust store
                logger.debug("Using system default trust store");
            }
            
            // Configure mutual TLS if enabled
            if (mutualTlsEnabled) {
                if (keyStorePath == null || keyStorePath.isEmpty()) {
                    throw new SecurityException("Key store path required for mutual TLS");
                }
                
                KeyManagerFactory keyManagerFactory = createKeyManagerFactory();
                sslContextBuilder.keyManager(keyManagerFactory);
                logger.info("Configured mutual TLS with key store: {}", keyStorePath);
            }
            
            // Configure secure cipher suites and protocols
            sslContextBuilder
                .protocols("TLSv1.3", "TLSv1.2")
                .ciphers(getSecureCipherSuites());
            
            SslContext sslContext = sslContextBuilder.build();
            logger.info("Created SSL context - TLS: {}, mTLS: {}", tlsEnabled, mutualTlsEnabled);
            
            return sslContext;
            
        } catch (Exception e) {
            logger.error("Failed to create SSL context", e);
            throw new SecurityException("Failed to create SSL context: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates trust manager factory from configured trust store.
     */
    private TrustManagerFactory createTrustManagerFactory() 
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        
        try (FileInputStream trustStoreStream = new FileInputStream(trustStorePath)) {
            trustStore.load(trustStoreStream, 
                trustStorePassword != null ? trustStorePassword.toCharArray() : null);
        }
        
        TrustManagerFactory trustManagerFactory = 
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        
        return trustManagerFactory;
    }
    
    /**
     * Creates key manager factory from configured key store for mutual TLS.
     */
    private KeyManagerFactory createKeyManagerFactory() 
            throws KeyStoreException, IOException, NoSuchAlgorithmException, 
                   CertificateException, UnrecoverableKeyException {
        
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        
        try (FileInputStream keyStoreStream = new FileInputStream(keyStorePath)) {
            keyStore.load(keyStoreStream, 
                keyStorePassword != null ? keyStorePassword.toCharArray() : null);
        }
        
        KeyManagerFactory keyManagerFactory = 
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, 
            keyStorePassword != null ? keyStorePassword.toCharArray() : null);
        
        return keyManagerFactory;
    }
    
    /**
     * Returns secure cipher suites for TLS communication.
     */
    private Iterable<String> getSecureCipherSuites() {
        return java.util.Arrays.asList(
            // TLS 1.3 cipher suites
            "TLS_AES_256_GCM_SHA384",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_AES_128_GCM_SHA256",
            
            // TLS 1.2 cipher suites (fallback)
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256"
        );
    }
    
    // Getters
    public boolean isTlsEnabled() { return tlsEnabled; }
    public boolean isMutualTlsEnabled() { return mutualTlsEnabled; }
    public boolean isCertificatePinningEnabled() { return certificatePinningEnabled; }
    public String[] getPinnedCertificates() { return pinnedCertificates; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private boolean tlsEnabled = true;
        private boolean mutualTlsEnabled = false;
        private String keyStorePath;
        private String keyStorePassword;
        private String trustStorePath;
        private String trustStorePassword;
        private boolean certificatePinningEnabled = false;
        private String[] pinnedCertificates;
        
        public Builder tlsEnabled(boolean tlsEnabled) {
            this.tlsEnabled = tlsEnabled;
            return this;
        }
        
        public Builder mutualTlsEnabled(boolean mutualTlsEnabled) {
            this.mutualTlsEnabled = mutualTlsEnabled;
            return this;
        }
        
        public Builder keyStorePath(String keyStorePath) {
            this.keyStorePath = keyStorePath;
            return this;
        }
        
        public Builder keyStorePassword(String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
            return this;
        }
        
        public Builder trustStorePath(String trustStorePath) {
            this.trustStorePath = trustStorePath;
            return this;
        }
        
        public Builder trustStorePassword(String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
            return this;
        }
        
        public Builder certificatePinningEnabled(boolean certificatePinningEnabled) {
            this.certificatePinningEnabled = certificatePinningEnabled;
            return this;
        }
        
        public Builder pinnedCertificates(String[] pinnedCertificates) {
            this.pinnedCertificates = pinnedCertificates;
            return this;
        }
        
        public TlsConfiguration build() {
            return new TlsConfiguration(this);
        }
    }
}