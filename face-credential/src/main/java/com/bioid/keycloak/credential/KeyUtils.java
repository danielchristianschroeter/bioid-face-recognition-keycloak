package com.bioid.keycloak.credential;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Utility class for handling Keycloak session keys and user ID sanitization.
 *
 * <p>Addresses the issue where user IDs like "demo-user-1" are not valid keys per Keycloak's
 * specification and may cause future migration failures.
 */
public class KeyUtils {

  private static final Pattern VALID_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
  private static final int MAX_KEY_LENGTH = 255;

  /**
   * Sanitizes a user ID to create a valid Keycloak session key.
   *
   * @param userId the original user ID
   * @return sanitized key that meets Keycloak's requirements
   */
  public static String sanitizeUserIdForKey(String userId) {
    if (userId == null || userId.trim().isEmpty()) {
      throw new IllegalArgumentException("User ID cannot be null or empty");
    }

    String trimmed = userId.trim();

    // If the user ID is already valid and not too long, use it as-is
    if (VALID_KEY_PATTERN.matcher(trimmed).matches() && trimmed.length() <= MAX_KEY_LENGTH) {
      return trimmed;
    }

    // For invalid or too-long user IDs, create a hash-based key
    return createHashBasedKey(trimmed);
  }

  /**
   * Creates a hash-based key from a user ID. This ensures the key is always valid and within length
   * limits.
   *
   * @param userId the original user ID
   * @return hash-based key
   */
  private static String createHashBasedKey(String userId) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(userId.getBytes(StandardCharsets.UTF_8));

      // Use Base64 URL-safe encoding (no padding) to create a valid key
      String hashKey = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

      // Prefix with "user_" to make it more readable and ensure it starts with a letter
      return "user_" + hashKey;

    } catch (NoSuchAlgorithmException e) {
      // This should never happen as SHA-256 is always available
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  /**
   * Validates if a key meets Keycloak's requirements.
   *
   * @param key the key to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidKey(String key) {
    if (key == null || key.trim().isEmpty()) {
      return false;
    }

    String trimmed = key.trim();
    return VALID_KEY_PATTERN.matcher(trimmed).matches() && trimmed.length() <= MAX_KEY_LENGTH;
  }

  /**
   * Creates a credential-specific key for a user. This can be used for storing credential-related
   * data in sessions.
   *
   * @param userId the user ID
   * @param credentialType the credential type (e.g., "face-biometric")
   * @return sanitized key for the credential
   */
  public static String createCredentialKey(String userId, String credentialType) {
    if (credentialType == null || credentialType.trim().isEmpty()) {
      throw new IllegalArgumentException("Credential type cannot be null or empty");
    }

    String sanitizedUserId = sanitizeUserIdForKey(userId);
    String sanitizedType = credentialType.replaceAll("[^a-zA-Z0-9_-]", "_");

    return sanitizedUserId + "_" + sanitizedType;
  }
}
