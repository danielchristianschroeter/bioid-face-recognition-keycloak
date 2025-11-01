package com.bioid.keycloak.admin.client;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * Generates JWT tokens for BWS Management API authentication.
 * 
 * This utility creates JWT tokens from email and API key credentials,
 * eliminating the need for external jwt tools.
 */
public class JwtTokenGenerator {

  private static final Logger logger = LoggerFactory.getLogger(JwtTokenGenerator.class);
  
  private static final long DEFAULT_EXPIRATION_MINUTES = 60;

  /**
   * Generate a JWT token for BWS Management API authentication.
   * 
   * @param email BWS Portal email address (subject)
   * @param apiKey BWS Portal API key (signing key)
   * @return JWT token string
   */
  public static String generateToken(String email, String apiKey) {
    return generateToken(email, apiKey, DEFAULT_EXPIRATION_MINUTES);
  }

  /**
   * Generate a JWT token with custom expiration time.
   * 
   * @param email BWS Portal email address (subject)
   * @param apiKey BWS Portal API key (signing key, base64-encoded)
   * @param expirationMinutes token expiration time in minutes
   * @return JWT token string
   */
  public static String generateToken(String email, String apiKey, long expirationMinutes) {
    try {
      long nowMillis = System.currentTimeMillis();
      Date now = new Date(nowMillis);
      Date expiration = new Date(nowMillis + (expirationMinutes * 60 * 1000));

      // API key from BWS Portal is base64-encoded, decode it first
      byte[] apiKeyBytes;
      try {
        apiKeyBytes = java.util.Base64.getDecoder().decode(apiKey);
        logger.debug("Decoded base64 API key for JWT signing");
      } catch (IllegalArgumentException e) {
        // If decoding fails, use the key as-is (UTF-8 bytes)
        logger.warn("API key is not valid base64, using as UTF-8 string");
        apiKeyBytes = apiKey.getBytes(StandardCharsets.UTF_8);
      }
      
      Key signingKey = new SecretKeySpec(apiKeyBytes, SignatureAlgorithm.HS256.getJcaName());

      // Build JWT token
      String token = Jwts.builder()
          .setSubject(email)
          .setIssuedAt(now)
          .setExpiration(expiration)
          .signWith(signingKey, SignatureAlgorithm.HS256)
          .compact();

      logger.debug("Generated JWT token for email: {}, expires in {} minutes", 
          email, expirationMinutes);
      
      return token;
      
    } catch (Exception e) {
      logger.error("Failed to generate JWT token", e);
      throw new RuntimeException("Failed to generate JWT token: " + e.getMessage(), e);
    }
  }

  /**
   * Validate that email and API key are provided.
   * 
   * @param email email address
   * @param apiKey API key
   * @throws IllegalArgumentException if either parameter is null or empty
   */
  public static void validateCredentials(String email, String apiKey) {
    if (email == null || email.trim().isEmpty()) {
      throw new IllegalArgumentException("Email address is required for JWT token generation");
    }
    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new IllegalArgumentException("API key is required for JWT token generation");
    }
  }
}
