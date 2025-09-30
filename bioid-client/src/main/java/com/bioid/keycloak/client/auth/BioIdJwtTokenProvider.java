package com.bioid.keycloak.client.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe JWT token provider for BioID BWS authentication. Handles token generation,
 * validation, and automatic renewal.
 *
 * <p>Security features: - HMAC-SHA256 signing - Automatic token renewal before expiration -
 * Thread-safe token caching - Secure key handling
 */
public class BioIdJwtTokenProvider {

  private static final Logger logger = LoggerFactory.getLogger(BioIdJwtTokenProvider.class);

  private final String clientId;
  private final SecretKey secretKey;
  private final int expireMinutes;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  // Cached token and expiration
  private volatile String cachedToken;
  private volatile Instant tokenExpiration;

  // Renewal buffer - renew token 5 minutes before expiration
  private static final int RENEWAL_BUFFER_MINUTES = 5;

  public BioIdJwtTokenProvider(String clientId, String secretKey, int expireMinutes) {
    if (clientId == null || clientId.trim().isEmpty()) {
      throw new IllegalArgumentException("Client ID cannot be null or empty");
    }
    if (secretKey == null || secretKey.trim().isEmpty()) {
      throw new IllegalArgumentException("Secret key cannot be null or empty");
    }
    if (expireMinutes <= 0) {
      throw new IllegalArgumentException("Expire minutes must be positive: " + expireMinutes);
    }

    this.clientId = clientId.trim();

    /* ------------------------------------------------------------------
     * JJWT (Keys.hmacShaKeyFor) requires HS256/384/512 keys to be at least
     * the corresponding hash length (RFC 7518 §3.2). Keys issued by the
     * BioID portal may be shorter (e.g. 24 bytes).  To stay compatible
     * without asking for an extra variable we transparently stretch any
     * key <32 bytes by hashing it with SHA-256 before deriving the SecretKey.
     * BioID never sees this raw key; it is only used locally to sign the
     * JWT sent to BWS. This follows JJWT’s recommended workaround.
     * ------------------------------------------------------------------ */
    byte[] keyBytes;
    try {
      // BioID keys are base64 encoded - decode them first (trim whitespace)
      String trimmedKey = secretKey.trim();
      keyBytes = java.util.Base64.getDecoder().decode(trimmedKey);
      logger.info("Successfully decoded base64 BioID key, length: {} bytes", keyBytes.length);
    } catch (IllegalArgumentException e) {
      // If base64 decoding fails, the key format is incorrect
      logger.error(
          "Failed to decode BioID key as base64. Key must be base64 encoded as provided by BWS Portal.");
      throw new IllegalArgumentException(
          "BioID key must be base64 encoded as provided by BWS Portal", e);
    }

    // BioID BWS keys vary in length - use appropriate algorithm
    logger.info("Using BioID key directly, length: {} bytes", keyBytes.length);

    // For shorter keys, extend to meet HMAC-SHA512 minimum requirements (64 bytes)
    if (keyBytes.length < 64) {
      logger.info(
          "BioID key is {} bytes, extending to 64 bytes for HMAC-SHA512 compatibility",
          keyBytes.length);
      try {
        java.security.MessageDigest sha512 = java.security.MessageDigest.getInstance("SHA-512");
        keyBytes = sha512.digest(keyBytes); // always 64 bytes
      } catch (java.security.NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-512 algorithm not available", e);
      }
    }

    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    this.expireMinutes = expireMinutes;

    logger.debug("Initialized JWT token provider for client: {}", this.clientId);
  }

  /**
   * Gets a valid JWT token, generating or renewing as needed. Thread-safe with automatic renewal.
   *
   * @return valid JWT token
   */
  public String getToken() {
    lock.readLock().lock();
    try {
      if (isTokenValid()) {
        return cachedToken;
      }
    } finally {
      lock.readLock().unlock();
    }

    // Token needs renewal - acquire write lock
    lock.writeLock().lock();
    try {
      // Double-check pattern - another thread might have renewed
      if (isTokenValid()) {
        return cachedToken;
      }

      // Generate new token
      Instant now = Instant.now();
      Instant expiration = now.plusSeconds(expireMinutes * 60L);

      // ===================================================================
      // FIX: Use modern, non-deprecated signWith(key) method.
      // The algorithm is inferred from the SecretKey type.
      // ===================================================================
      String token =
          Jwts.builder()
              .subject(clientId)
              .issuer(clientId)
              .audience()
              .add("BWS")
              .and()
              .issuedAt(Date.from(now))
              .expiration(Date.from(expiration))
              .signWith(secretKey, Jwts.SIG.HS512)
              .compact();

      // Cache the new token
      this.cachedToken = token;
      this.tokenExpiration = expiration;

      logger.info("Generated JWT token for BioID BWS authentication:");
      logger.info("  Client ID: {}", clientId);
      logger.info("  Token length: {} characters", token.length());
      logger.info("  Expires at: {}", expiration);
      logger.debug("  Token preview: {}...", token.substring(0, Math.min(50, token.length())));
      return token;

    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Checks if the current cached token is valid and not near expiration.
   *
   * @return true if token is valid and has sufficient time remaining
   */
  private boolean isTokenValid() {
    if (cachedToken == null || tokenExpiration == null) {
      return false;
    }

    // Check if token expires within the renewal buffer
    Instant renewalThreshold = Instant.now().plusSeconds(RENEWAL_BUFFER_MINUTES * 60L);
    return tokenExpiration.isAfter(renewalThreshold);
  }

  /** Forces token renewal on next access. Useful when authentication errors occur. */
  public void invalidateToken() {
    lock.writeLock().lock();
    try {
      this.cachedToken = null;
      this.tokenExpiration = null;
      logger.debug("Invalidated cached JWT token for client: {}", clientId);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Gets the client ID associated with this token provider.
   *
   * @return client ID
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Gets the token expiration time in minutes.
   *
   * @return expiration time in minutes
   */
  public int getExpireMinutes() {
    return expireMinutes;
  }

  /**
   * Checks if a token is expired (for testing purposes).
   *
   * @param token JWT token to check
   * @return true if expired, false otherwise
   */
  public boolean isTokenExpired(String token) {
    if (token == null || token.trim().isEmpty()) {
      return true;
    }

    try {
      Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
      return false;
    } catch (Exception e) {
      logger.debug("Token validation failed: {}", e.getMessage());
      return true;
    }
  }
}
