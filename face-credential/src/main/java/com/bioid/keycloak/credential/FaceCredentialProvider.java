package com.bioid.keycloak.credential;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.BioIdGrpcClientProduction;
import com.bioid.keycloak.client.config.BioIdConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keycloak credential provider for face biometric credentials.
 *
 * <p>
 * This provider manages the storage and lifecycle of face biometric credentials in Keycloak. It
 * stores only metadata about templates stored in BioID BWS, never raw biometric data.
 *
 * <p>
 * Features: - Secure credential storage with encryption - Automatic expiration handling - Template
 * metadata management - Integration with Keycloak's credential framework - Audit trail support
 *
 * <p>
 * Security: - No raw biometric data stored in Keycloak - Encrypted credential data - Secure class
 * ID generation - Automatic cleanup of expired credentials
 */
public class FaceCredentialProvider
    implements CredentialProvider<FaceCredentialModel>, CredentialInputValidator, org.keycloak.credential.CredentialInputUpdater {

  private static final Logger logger = LoggerFactory.getLogger(FaceCredentialProvider.class);

  public static final String TYPE = "face-biometric";
  public static final String DISPLAY_NAME = "Face Recognition";
  public static final String HELP_TEXT = "Face biometric authentication using BioID technology";

  private final KeycloakSession session;
  private final ObjectMapper objectMapper;
  private BioIdClient bioIdClient; // Made non-final

  public FaceCredentialProvider(KeycloakSession session) {
    System.out.println("DEBUG: FaceCredentialProvider constructor called with session: " + session);
    this.session = Objects.requireNonNull(session, "KeycloakSession cannot be null");
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    // Configure ObjectMapper for security
    this.objectMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    this.objectMapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);

    // Lazily initialize the BioID gRPC client
    // This avoids issues with provider instantiation order during Keycloak startup
    this.bioIdClient = null;
    System.out.println("DEBUG: FaceCredentialProvider constructor completed successfully");
  }

  public BioIdClient getBioIdClient() {
    if (this.bioIdClient == null) {
      try {
        BioIdConfiguration configAdapter = BioIdConfiguration.getInstance();

        // Check if credentials are available
        if (configAdapter.getClientId() == null || configAdapter.getClientId().trim().isEmpty()
            || configAdapter.getKey() == null || configAdapter.getKey().trim().isEmpty()) {
          logger.error(
              "PRODUCTION ISSUE: BioID credentials not configured. Set BWS_CLIENT_ID and BWS_KEY environment variables. "
                  + "System will use mock implementations which are NOT suitable for production use.");
          return null; // Return null to trigger mock fallback
        }

        // Validate endpoint configuration
        String endpoint = configAdapter.getEndpoint();
        if (endpoint == null || endpoint.trim().isEmpty()) {
          logger.error(
              "PRODUCTION ISSUE: BioID endpoint not configured. Set BWS_ENDPOINT environment variable.");
          return null;
        }

        // Initialize the BioID gRPC client with proper configuration
        logger.info("Initializing BioID gRPC client with endpoint: {}", endpoint);

        // Create the production BWS gRPC client (uses proper gRPC protocol)
        this.bioIdClient = new com.bioid.keycloak.client.BioIdGrpcClientProduction(configAdapter,
            endpoint, configAdapter.getClientId(), configAdapter.getKey());

        logger.info("BioID gRPC client initialized successfully with endpoint: {}", endpoint);
      } catch (Exception e) {
        logger.error(
            "PRODUCTION ISSUE: Failed to initialize BioID client. Check configuration and network connectivity. "
                + "Error: {} - System will use mock implementations which are NOT suitable for production use.",
            e.getMessage());
        return null; // Return null to trigger mock fallback
      }
    }
    return this.bioIdClient;
  }

  public String getType() {
    return TYPE;
  }

  public CredentialModel createCredential(RealmModel realm, UserModel user,
      FaceCredentialModel credentialModel) {
    if (credentialModel == null) {
      throw new IllegalArgumentException("Face credential model cannot be null");
    }

    logger.debug("Creating face credential for user: {} in realm: {}", user.getId(),
        realm.getName());

    try {
      // Serialize the credential data parts separately for consistency
      String credentialData =
          objectMapper.writeValueAsString(credentialModel.getFaceCredentialData());
      String secretData = objectMapper.writeValueAsString(credentialModel.getFaceSecretData());

      // Create the Keycloak credential model
      CredentialModel credential = new CredentialModel();
      credential.setId(generateCredentialId());
      credential.setType(TYPE);
      credential.setUserLabel(generateUserLabel(credentialModel));
      credential.setCreatedDate(credentialModel.getCreatedAt().toEpochMilli());
      credential.setCredentialData(credentialData);
      credential.setSecretData(secretData);

      // Store the credential
      user.credentialManager().createStoredCredential(credential);

      logger.info("Face credential created successfully for user: {} with class ID: {}",
          user.getId(), credentialModel.getClassId());

      return credential;

    } catch (JsonProcessingException e) {
      logger.error("Failed to serialize face credential model for user: {}", user.getId(), e);
      throw new RuntimeException("Failed to create face credential", e);
    }
  }

  @Override
  public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
    logger.info("DELETE CREDENTIAL CALLED - Deleting face credential: {} for user: {} in realm: {}", 
        credentialId, user.getId(), realm.getName());

    CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialId);
    if (credential == null) {
      logger.warn("Credential not found: {}", credentialId);
      return false;
    }
    
    if (!TYPE.equals(credential.getType())) {
      logger.info("Credential {} is not a face credential (type: {}), skipping BWS deletion", 
          credentialId, credential.getType());
      return false;
    }
    
    logger.info("Found face credential to delete: {} (type: {})", credentialId, credential.getType());

    try {
      // Get the credential model to extract class ID
      FaceCredentialModel faceCredential = getCredentialFromModel(credential);
      if (faceCredential == null) {
        logger.warn("Failed to deserialize face credential: {}", credentialId);
        return false;
      }

      // Delete the template from BioID BWS
      BioIdClient client = getBioIdClient();
      logger.info("BioID client status: {}", client != null ? "AVAILABLE" : "NULL");
      
      if (client != null) {
        try {
          logger.info("Attempting to delete BWS template for class ID: {}", faceCredential.getClassId());
          client.deleteTemplate(faceCredential.getClassId());
          logger.info("✓ Successfully deleted BioID template for class ID: {} (credential: {})",
              faceCredential.getClassId(), credentialId);
        } catch (com.bioid.keycloak.client.exception.BioIdException e) {
          logger.error("✗ Failed to delete BioID template for class ID: {} (credential: {}): {}",
              faceCredential.getClassId(), credentialId, e.getMessage(), e);
          // Continue with Keycloak deletion even if BioID deletion fails
          // This ensures we don't leave orphaned credentials in Keycloak
        } catch (Exception e) {
          logger.error("✗ Unexpected error deleting BioID template for class ID: {} (credential: {}): {}",
              faceCredential.getClassId(), credentialId, e.getMessage(), e);
        }
      } else {
        logger.warn("⚠ BioID client not available, skipping template deletion for credential: {} (class ID: {})",
            credentialId, faceCredential.getClassId());
      }

      // Remove the credential from Keycloak
      boolean removed = user.credentialManager().removeStoredCredentialById(credentialId);

      if (removed) {
        logger.info("Face credential deleted successfully: {} (class ID: {}) for user: {}",
            credentialId, faceCredential.getClassId(), user.getId());
      } else {
        logger.warn("Failed to delete face credential: {} for user: {}", credentialId,
            user.getId());
      }

      return removed;

    } catch (Exception e) {
      logger.error("Error deleting face credential: {} for user: {}", credentialId, user.getId(),
          e);
      return false;
    }
  }

  @Override
  public FaceCredentialModel getCredentialFromModel(CredentialModel model) {
    if (model == null || !TYPE.equals(model.getType())) {
      return null;
    }

    try {
      String credentialData = model.getCredentialData();
      String secretData = model.getSecretData();

      if (credentialData == null || credentialData.trim().isEmpty()) {
        logger.warn("Empty credential data for face credential: {}", model.getId());
        return null;
      }

      FaceCredentialData faceCredentialData =
          objectMapper.readValue(credentialData, FaceCredentialData.class);

      if (secretData != null && !secretData.trim().isEmpty()) {
        objectMapper.readValue(secretData, FaceSecretData.class);
      } else {
        // Fallback for credentials that don't have secret data
        new FaceSecretData(faceCredentialData.getClassId());
      }

      return FaceCredentialModel.createFromCredentialModel(model);

    } catch (IOException e) {
      logger.error("Failed to deserialize face credential model: {}", model.getId(), e);
      return null;
    }
  }

  @Override
  public CredentialTypeMetadata getCredentialTypeMetadata(
      CredentialTypeMetadataContext metadataContext) {
    return CredentialTypeMetadata.builder().type(TYPE)
        .category(CredentialTypeMetadata.Category.TWO_FACTOR).displayName(DISPLAY_NAME)
        .helpText(HELP_TEXT).iconCssClass("kcAuthenticatorFaceIcon").createAction("face-enroll")
        .removeable(true).build(session);
  }

  /**
   * Gets all face credentials for a user.
   *
   * @param realm the realm
   * @param user the user
   * @return stream of face credential models
   */
  public Stream<FaceCredentialModel> getFaceCredentials(RealmModel realm, UserModel user) {
    return user.credentialManager().getStoredCredentialsStream()
        .filter(credential -> TYPE.equals(credential.getType())).map(this::getCredentialFromModel)
        .filter(credential -> credential != null);
  }

  /**
   * Gets the most recent face credential for a user.
   *
   * @param realm the realm
   * @param user the user
   * @return the most recent face credential, or null if none exists
   */
  public FaceCredentialModel getMostRecentFaceCredential(RealmModel realm, UserModel user) {
    return getFaceCredentials(realm, user).filter(credential -> !credential.isExpired())
        .max((c1, c2) -> c1.getCreatedAt().compareTo(c2.getCreatedAt())).orElse(null);
  }

  /**
   * Checks if a user has any valid (non-expired) face credentials.
   *
   * @param realm the realm
   * @param user the user
   * @return true if the user has valid face credentials, false otherwise
   */
  public boolean hasValidFaceCredentials(RealmModel realm, UserModel user) {
    return getFaceCredentials(realm, user).anyMatch(credential -> !credential.isExpired());
  }

  /**
   * Updates the expiration time for a face credential.
   *
   * @param realm the realm
   * @param user the user
   * @param credentialId the credential ID
   * @param newExpiration the new expiration time
   * @return true if updated successfully, false otherwise
   */
  public boolean updateCredentialExpiration(RealmModel realm, UserModel user, String credentialId,
      Instant newExpiration) {
    CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialId);
    if (credential == null || !TYPE.equals(credential.getType())) {
      return false;
    }

    try {
      FaceCredentialModel faceCredential = getCredentialFromModel(credential);
      if (faceCredential == null) {
        return false;
      }

      // Create updated credential model
      FaceCredentialModel updatedCredential = faceCredential.withExpiration(newExpiration);
      String updatedData =
          objectMapper.writeValueAsString(updatedCredential.getFaceCredentialData());

      // Update the credential
      credential.setCredentialData(updatedData);
      user.credentialManager().updateStoredCredential(credential);

      logger.debug("Updated expiration for face credential: {} to: {}", credentialId,
          newExpiration);
      return true;

    } catch (JsonProcessingException e) {
      logger.error("Failed to update credential expiration: {}", credentialId, e);
      return false;
    }
  }

  /**
   * Removes all expired face credentials for a user.
   *
   * @param realm the realm
   * @param user the user
   * @return number of credentials removed
   */
  public int removeExpiredCredentials(RealmModel realm, UserModel user) {
    List<CredentialModel> expiredCredentials = user.credentialManager().getStoredCredentialsStream()
        .filter(credential -> TYPE.equals(credential.getType())).filter(credential -> {
          FaceCredentialModel faceCredential = getCredentialFromModel(credential);
          return faceCredential != null && faceCredential.isExpired();
        }).toList();

    int removedCount = 0;
    for (CredentialModel credential : expiredCredentials) {
      if (user.credentialManager().removeStoredCredentialById(credential.getId())) {
        removedCount++;
      }
    }

    if (removedCount > 0) {
      logger.info("Removed {} expired face credentials for user: {}", removedCount, user.getId());
    }

    return removedCount;
  }

  /**
   * Updates the tags for a face credential.
   *
   * @param realm the realm
   * @param user the user
   * @param credentialId the credential ID
   * @param newTags the new tags list
   * @return true if updated successfully, false otherwise
   */
  public boolean updateCredentialTags(RealmModel realm, UserModel user, String credentialId,
      List<String> newTags) {
    if (credentialId == null || credentialId.trim().isEmpty()) {
      logger.warn("Invalid credential ID provided for tag update");
      return false;
    }

    CredentialModel credential = user.credentialManager().getStoredCredentialById(credentialId);
    if (credential == null || !TYPE.equals(credential.getType())) {
      return false;
    }

    try {
      FaceCredentialModel faceCredential = getCredentialFromModel(credential);
      if (faceCredential == null) {
        return false;
      }

      // Create updated credential model
      FaceCredentialModel updatedCredential = faceCredential.withTags(newTags);
      String updatedData =
          objectMapper.writeValueAsString(updatedCredential.getFaceCredentialData());

      // Update the credential
      credential.setCredentialData(updatedData);
      user.credentialManager().updateStoredCredential(credential);

      logger.debug("Updated tags for face credential: {} to: {}", credentialId, newTags);
      return true;

    } catch (JsonProcessingException e) {
      logger.error("Failed to update credential tags: {}", credentialId, e);
      return false;
    }
  }

  /**
   * Gets credentials by template type.
   *
   * @param realm the realm
   * @param user the user
   * @param templateType the template type to filter by
   * @return stream of face credentials with the specified template type
   */
  public Stream<FaceCredentialModel> getCredentialsByTemplateType(RealmModel realm, UserModel user,
      FaceCredentialModel.TemplateType templateType) {
    return getFaceCredentials(realm, user)
        .filter(credential -> credential.getTemplateType() == templateType);
  }

  /**
   * Gets credentials by tags.
   *
   * @param realm the realm
   * @param user the user
   * @param tag the tag to filter by
   * @return stream of face credentials containing the specified tag
   */
  public Stream<FaceCredentialModel> getCredentialsByTag(RealmModel realm, UserModel user,
      String tag) {
    return getFaceCredentials(realm, user).filter(credential -> credential.getTags().contains(tag));
  }

  /**
   * Gets credential statistics for a user.
   *
   * @param realm the realm
   * @param user the user
   * @return credential statistics
   */
  public CredentialStatistics getCredentialStatistics(RealmModel realm, UserModel user) {
    List<FaceCredentialModel> credentials = getFaceCredentials(realm, user).toList();

    long totalCount = credentials.size();
    long validCount = credentials.stream().filter(c -> !c.isExpired()).count();
    long expiredCount = totalCount - validCount;

    return new CredentialStatistics(totalCount, validCount, expiredCount);
  }

  /**
   * Queries template status from BioID BWS for a credential.
   *
   * @param realm the realm
   * @param user the user
   * @param credentialId the credential ID
   * @param downloadThumbnails whether to download thumbnails
   * @return template status from BioID, or null if not available
   */

  /**
   * Handles template not found scenarios with proper error handling.
   *
   * @param realm the realm
   * @param user the user
   * @param credentialId the credential ID
   * @param operation the operation being performed
   * @return true if handled successfully, false if credential should be removed
   */
  public boolean handleTemplateNotFound(RealmModel realm, UserModel user, String credentialId,
      String operation) {
    logger.warn("Template not found for credential: {} during operation: {}", credentialId,
        operation);

    // For verification operations, we should trigger re-enrollment
    if ("verify".equals(operation) || "authenticate".equals(operation)) {
      // Mark user for re-enrollment by adding required action
      user.addRequiredAction("face-enroll");
      logger.info("Added face enrollment required action for user: {} due to missing template",
          user.getId());
      return true;
    }

    // For deletion operations, treat as already deleted (idempotent)
    if ("delete".equals(operation)) {
      logger.info("Template already deleted for credential: {}", credentialId);
      return true;
    }

    // For status operations, return false to indicate unavailable
    if ("status".equals(operation) || "query".equals(operation)) {
      return false;
    }

    // For other operations, remove the orphaned credential
    logger.info("Removing orphaned credential: {} due to missing template", credentialId);
    return deleteCredential(realm, user, credentialId);
  }

  /** Statistics about face credentials for a user. */
  public static class CredentialStatistics {
    private final long totalCount;
    private final long validCount;
    private final long expiredCount;

    public CredentialStatistics(long totalCount, long validCount, long expiredCount) {
      this.totalCount = totalCount;
      this.validCount = validCount;
      this.expiredCount = expiredCount;
    }

    public long getTotalCount() {
      return totalCount;
    }

    public long getValidCount() {
      return validCount;
    }

    public long getExpiredCount() {
      return expiredCount;
    }

    @Override
    public String toString() {
      return "CredentialStatistics{" + "totalCount=" + totalCount + ", validCount=" + validCount
          + ", expiredCount=" + expiredCount + '}';
    }
  }

  @Override
  public boolean supportsCredentialType(String credentialType) {
    return TYPE.equals(credentialType);
  }

  @Override
  public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
    if (!supportsCredentialType(credentialType)) {
      return false;
    }

    return hasValidFaceCredentials(realm, user);
  }

  @Override
  public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
    if (!supportsCredentialType(credentialInput.getType())) {
      return false;
    }

    // Face credentials are validated through the authenticator, not here
    // This method is primarily for password-like credentials
    return false;
  }

  /**
   * Generates a unique credential ID that is guaranteed to be valid for Keycloak.
   *
   * @return unique credential ID (max 36 characters for database compatibility)
   */
  private String generateCredentialId() {
    // Generate a standard UUID without prefix to stay within 36 character limit
    return java.util.UUID.randomUUID().toString();
  }

  /**
   * Generates a user-friendly label for the credential.
   *
   * @param credentialModel the credential model
   * @return user-friendly label
   */
  private String generateUserLabel(FaceCredentialModel credentialModel) {
    return String.format("Face Recognition (%s)",
        credentialModel.getCreatedAt().toString().substring(0, 10));
  }

  /**
   * Performs face verification for a user using the provided image data.
   *
   * @param realm the realm
   * @param user the user
   * @param imageData base64-encoded image data (with data URL prefix)
   * @return true if verification succeeds, false otherwise
   * @throws RuntimeException if verification fails due to service issues
   */
  public boolean verifyFace(RealmModel realm, UserModel user, String imageData) {
    FaceCredentialModel credential = getMostRecentFaceCredential(realm, user);
    if (credential == null) {
      logger.warn("No face credential found for user: {}", user.getId());
      return false;
    }
    BioIdClient client = getBioIdClient();
    if (client == null) {
      logger.error("BioID credentials not configured - VERIFICATION FAILED for security");
      return false; // SECURITY: Never allow verification without proper BWS client
    }
    try {
      // Extract base64 image data (remove data URL prefix if present)
      String base64Image = imageData.contains(",") ? imageData.split(",")[1] : imageData;
      // Use reflection to avoid compile-time dependency on BioIdException
      Boolean result =
          (Boolean) client.getClass().getMethod("verifyFaceWithImageData", long.class, String.class)
              .invoke(client, credential.getClassId(), base64Image);
      return result;
    } catch (Exception e) {
      // Handle specific gRPC errors that indicate service issues
      String errorMessage = e.getMessage();
      if (errorMessage != null && (errorMessage.contains("HTTP status code 308")
          || errorMessage.contains("invalid content-type: text/html")
          || errorMessage.contains("Permanent Redirect"))) {
        logger.error(
            "PRODUCTION ISSUE: BioID service returned HTTP redirect (HTTP 308) during verification. "
                + "This indicates authentication failure. Check BWS_CLIENT_ID and BWS_KEY credentials.");
        return false; // Fail securely on authentication issues
      } else {
        logger.error(
            "PRODUCTION ISSUE: BioID service unavailable during verification: {}. "
                + "Check service status, network connectivity, and credentials.",
            errorMessage != null ? errorMessage : "Unknown error");
        return false; // Fail securely when service is unavailable
      }
    }
  }

  /**
   * Performs face verification with liveness detection using two images.
   *
   * @param realm the realm
   * @param user the user
   * @param firstImage base64-encoded first image data
   * @param secondImage base64-encoded second image data
   * @param mode liveness mode ("active" or "challenge-response")
   * @param challengeDirection challenge direction for challenge-response mode
   * @return true if verification succeeds, false otherwise
   * @throws RuntimeException if verification fails due to service issues
   */
  public boolean verifyFaceWithLiveness(RealmModel realm, UserModel user, String firstImage,
      String secondImage, String mode, String challengeDirection) {
    FaceCredentialModel credential = getMostRecentFaceCredential(realm, user);
    if (credential == null) {
      logger.warn("No face credential found for user: {}", user.getId());
      return false;
    }

    BioIdClient client = getBioIdClient();
    if (client == null) {
      logger.error(
          "SECURITY ISSUE: BioID client not initialized - liveness verification cannot proceed");
      return false; // Fail securely when client is not available
    }

    try {
      // Extract base64 image data (remove data URL prefix if present)
      String base64Image1 = firstImage.contains(",") ? firstImage.split(",")[1] : firstImage;
      String base64Image2 = secondImage.contains(",") ? secondImage.split(",")[1] : secondImage;

      logger.info("Performing {} liveness verification for user: {} with classId: {}", mode,
          user.getId(), credential.getClassId());

      // Flip LEFT/RIGHT directions because video is mirrored for user display
      // When user sees "turn LEFT" in mirrored video and turns left, camera captures them turning right
      String flippedDirection = flipDirectionForMirroredVideo(challengeDirection);
      logger.info("Challenge direction: {} (user sees) -> {} (camera captures)", 
                  challengeDirection, flippedDirection);

      // Step 1: Use BWS liveness detection with multiple images
      boolean livenessResult =
          client.livenessDetectionWithImages(firstImage, secondImage, mode, flippedDirection);

      logger.info("Liveness verification completed for user: {} - result: {}", user.getId(),
          livenessResult);

      if (!livenessResult) {
        logger.warn("Liveness detection failed for user: {}", user.getId());
        return false;
      }

      // Step 2: If liveness passed, verify the face against the enrolled template
      // Use the second image (the one that passed liveness) for face verification
      logger.info("Liveness passed, now verifying face against enrolled template for user: {}",
          user.getId());

      boolean verificationResult = client.verifyFaceWithImageData(credential.getClassId(), base64Image2);

      logger.info("Face verification result for user {}: {}", user.getId(), verificationResult);

      return verificationResult;

    } catch (Exception e) {
      // Handle specific gRPC errors that indicate service issues
      String errorMessage = e.getMessage();
      if (errorMessage != null && (errorMessage.contains("HTTP status code 308")
          || errorMessage.contains("invalid content-type: text/html")
          || errorMessage.contains("Permanent Redirect"))) {
        logger.error(
            "PRODUCTION ISSUE: BioID service returned HTTP redirect (HTTP 308) during liveness verification. "
                + "This indicates authentication failure. Check BWS_CLIENT_ID and BWS_KEY credentials.");
        return false; // Fail securely on authentication issues
      } else {
        logger.error(
            "PRODUCTION ISSUE: BioID service unavailable during liveness verification: {}. "
                + "Check service status, network connectivity, and credentials.",
            errorMessage != null ? errorMessage : "Unknown error");
        return false; // Fail securely when service is unavailable
      }
    }
  }

  /**
   * Flips LEFT/RIGHT directions to account for mirrored video display.
   * The video is mirrored for user comfort (like a mirror), but images are captured un-mirrored.
   * When user sees "turn LEFT" and turns left in the mirror, camera captures them turning RIGHT.
   * 
   * @param direction The direction shown to the user (LEFT, RIGHT, UP, DOWN)
   * @return The actual direction captured by the camera
   */
  private String flipDirectionForMirroredVideo(String direction) {
    if (direction == null) {
      return null;
    }
    
    switch (direction.toUpperCase()) {
      case "LEFT":
        return "RIGHT";
      case "RIGHT":
        return "LEFT";
      case "UP":
      case "DOWN":
        // UP and DOWN don't need flipping
        return direction;
      default:
        return direction;
    }
  }

  // CredentialInputUpdater methods - required to intercept credential updates/deletions
  
  @Override
  public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
    logger.info("updateCredential called for user: {}, credentialType: {}", 
        user.getId(), input != null ? input.getCredentialId() : "null");
    // Face credentials are not updatable - they must be re-enrolled
    return false;
  }

  @Override
  public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    logger.info("disableCredentialType called for user: {}, type: {}", user.getId(), credentialType);
    
    if (!TYPE.equals(credentialType)) {
      return;
    }

    // When disabling, delete all face credentials for this user
    logger.info("Disabling all face credentials for user: {}", user.getId());
    
    user.credentialManager().getStoredCredentialsStream()
        .filter(cred -> TYPE.equals(cred.getType()))
        .forEach(cred -> {
          logger.info("Disabling face credential: {}", cred.getId());
          deleteCredential(realm, user, cred.getId());
        });
  }

  @Override
  public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
    logger.debug("getDisableableCredentialTypesStream called for user: {}", user.getId());
    
    // Check if user has any face credentials
    boolean hasFaceCredentials = user.credentialManager().getStoredCredentialsStream()
        .anyMatch(cred -> TYPE.equals(cred.getType()));
    
    if (hasFaceCredentials) {
      return Stream.of(TYPE);
    }
    
    return Stream.empty();
  }

  @Override
  public void close() {
    // No resources to close
  }
}
