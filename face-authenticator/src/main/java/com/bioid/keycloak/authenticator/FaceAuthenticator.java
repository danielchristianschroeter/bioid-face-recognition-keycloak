package com.bioid.keycloak.authenticator;

import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.credential.FaceCredentialModel;
import com.bioid.keycloak.credential.FaceCredentialProvider;
import com.bioid.keycloak.credential.FaceCredentialProviderFactory;
import com.bioid.keycloak.failedauth.config.FailedAuthConfiguration;
import com.bioid.keycloak.failedauth.service.FailedAuthImageStorageService;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;

/**
 * Keycloak authenticator for face biometric verification.
 *
 * <p>This authenticator integrates face recognition into Keycloak authentication flows, providing a
 * modern, secure, and user-friendly verification experience.
 */
public class FaceAuthenticator implements Authenticator {

  private static final Logger logger = LoggerFactory.getLogger(FaceAuthenticator.class);
  private static final String ATTR_RETRY_COUNT = "face.auth.retry.count";
  
  private final FailedAuthImageStorageService failedAuthStorageService;

  public FaceAuthenticator(KeycloakSession session) {
    // The session passed here by the factory can become stale.
    // It's better not to store it and use the one from the context instead.
    // this.session = session;
    
    // Initialize failed auth storage service (skip in test mode to avoid database connection)
    boolean isTestMode = Boolean.parseBoolean(System.getProperty("bioid.test.mode", "false"));
    if (isTestMode) {
      this.failedAuthStorageService = null;
      logger.debug("Test mode enabled, skipping FailedAuthImageStorageService initialization");
    } else {
      FailedAuthConfiguration config = FailedAuthConfiguration.getInstance();
      this.failedAuthStorageService = new FailedAuthImageStorageService(config);
    }
  }

  /** Helper method to get the FaceCredentialProvider from the correct session. */
  private FaceCredentialProvider getCredentialProvider(KeycloakSession session) {
    FaceCredentialProvider provider =
        (FaceCredentialProvider)
            session.getProvider(
                CredentialProvider.class, FaceCredentialProviderFactory.PROVIDER_ID);
    if (provider == null) {
      logger.error(
          "FaceCredentialProvider not found. Is the extension deployed correctly? Check server logs for deployment errors.");
      throw new IllegalStateException("FaceCredentialProvider not available. Check deployment.");
    }
    return provider;
  }

  /**
   * Check if face authentication is enabled at the realm level.
   * Controlled by realm attribute "faceAuthEnabled" (default: true)
   */
  private boolean isRealmFaceAuthEnabled(RealmModel realm) {
    String realmAttr = realm.getAttribute("faceAuthEnabled");
    // Default to true if not set
    return realmAttr == null || Boolean.parseBoolean(realmAttr);
  }

  /**
   * Check if face authentication is required (not optional) at the realm level.
   * Controlled by realm attribute "faceAuthRequired" (default: true)
   * When false, users can skip enrollment.
   */
  private boolean isRealmFaceAuthRequired(RealmModel realm) {
    String realmAttr = realm.getAttribute("faceAuthRequired");
    // Default to true if not set
    return realmAttr == null || Boolean.parseBoolean(realmAttr);
  }

  /**
   * Check if user has explicitly disabled face authentication.
   * User attribute "face.auth.enabled" = "false" means disabled.
   */
  private boolean isUserFaceAuthEnabled(UserModel user) {
    String userAttr = user.getFirstAttribute("face.auth.enabled");
    // Default to true if not set
    return userAttr == null || Boolean.parseBoolean(userAttr);
  }

  /**
   * Check if user has skipped face enrollment.
   * User attribute "face.auth.skipped" = "true" means skipped.
   */
  private boolean hasUserSkippedEnrollment(UserModel user) {
    return Boolean.parseBoolean(user.getFirstAttribute("face.auth.skipped"));
  }

  /**
   * Called when this authenticator is invoked in the flow. It checks if the user is enrolled and
   * presents the verification UI.
   */
  @Override
  public void authenticate(AuthenticationFlowContext context) {
    UserModel user = context.getUser();
    RealmModel realm = context.getRealm();

    // 1. Check realm-level: Is face auth enabled for this realm?
    if (!isRealmFaceAuthEnabled(realm)) {
      logger.debug("Face authentication is disabled for realm: {}", realm.getName());
      context.attempted();
      return;
    }

    // 2. Check user-level: Has admin disabled face auth for this user?
    if (!isUserFaceAuthEnabled(user)) {
      logger.debug("Face authentication is disabled for user: {}", user.getId());
      context.attempted();
      return;
    }

    // 3. Check if user has previously skipped enrollment (only if not required)
    if (!isRealmFaceAuthRequired(realm) && hasUserSkippedEnrollment(user)) {
      logger.debug("User {} has skipped face enrollment", user.getId());
      context.attempted();
      return;
    }

    // 4. Check if user has face credentials
    if (!getCredentialProvider(context.getSession())
        .hasValidFaceCredentials(realm, user)) {
      
      // If face auth is not required, allow user to skip
      if (!isRealmFaceAuthRequired(realm)) {
        logger.info("User {} does not have face credentials. Showing enrollment with skip option.", user.getId());
        context.forceChallenge(
            context.form()
                .setAttribute("allowSkip", true)
                .setInfo("Face enrollment is optional. You can enroll now or skip.")
                .createForm("face-enroll.ftl"));
        return;
      }
      
      // Face auth is required - force enrollment
      logger.info("User {} does not have face credentials. Redirecting to enrollment.", user.getId());
      user.addRequiredAction("face-enroll");
      context.forceChallenge(
          context.form()
              .setAttribute("allowSkip", false)
              .setInfo("You need to enroll your face before continuing.")
              .createForm("face-enroll.ftl"));
      return;
    }

    int retryCount = getRetryCount(context);
    
    // Get liveness configuration from BioIdConfiguration
    com.bioid.keycloak.client.config.BioIdConfiguration bioIdConfig =
        com.bioid.keycloak.client.config.BioIdConfiguration.getInstance();
    LivenessSettings livenessSettings = resolveLivenessSettings(context, bioIdConfig);

    logger.info("Resolved liveness mode {} (source: {}), active={}, challenge={}, confidence={}, timeout={}s",
        livenessSettings.modeName(),
        livenessSettings.isDefinedInUi() ? "ui" : "environment",
        livenessSettings.isActiveEnabled(),
        livenessSettings.isChallengeEnabled(),
        bioIdConfig.getLivenessConfidenceThreshold(),
        bioIdConfig.getLivenessChallengeTimeout().toSeconds());

    Response challenge =
        context
            .form()
            .setAttribute("maxRetries", getMaxRetries(context))
            .setAttribute("retryCount", retryCount)
            .setAttribute("livenessActiveEnabled", livenessSettings.isActiveEnabled())
            .setAttribute("livenessChallengeResponseEnabled", livenessSettings.isChallengeEnabled())
            .setAttribute("livenessConfidenceThreshold", bioIdConfig.getLivenessConfidenceThreshold())
            .setAttribute("livenessChallengeTimeoutSeconds", (int) bioIdConfig.getLivenessChallengeTimeout().toSeconds())
            .createForm("face-authenticate.ftl");
    context.challenge(challenge);
  }

  /** Called when the user submits the verification form from the UI. */
  @Override
  public void action(AuthenticationFlowContext context) {
    try {
      MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
      String imageData = formData.getFirst("imageData");

      logger.info("Face authentication action called for user: {}", context.getUser().getId());
      logger.info("Image data length: {}, starts with: {}", 
                  imageData != null ? imageData.length() : "null",
                  imageData != null && imageData.length() > 100 ? imageData.substring(0, 100) : imageData);

      if (imageData == null || imageData.isEmpty()) {
        logger.warn("No image data provided for user: {}", context.getUser().getId());
        handleFailure(context, "No image data provided. Please try again.");
        return;
      }

      // Use the session from the context
      FaceCredentialModel credential =
          getCredentialProvider(context.getSession())
              .getMostRecentFaceCredential(context.getRealm(), context.getUser());
      if (credential == null) {
        logger.warn("No face credential found for user: {}", context.getUser().getId());
        handleFailure(context, "No valid face credential found for user. Please try re-enrolling.");
        return;
      }

      logger.info("Found face credential for user: {}, classId: {}", context.getUser().getId(), credential.getClassId());

      boolean verificationSuccess = performVerification(context, credential, imageData);
      if (verificationSuccess) {
        logger.info("Face verification successful for user: {}", context.getUser().getId());
        context.success();
      } else {
        logger.warn("Face verification failed for user: {}", context.getUser().getId());
        handleFailure(context, "Face verification failed. Please try again.");
      }
    } catch (BioIdException e) {
      logger.error(
          "BioID service error during face verification for user: {}",
          context.getUser().getId(),
          e);
      handleFailure(context, "Face verification service failed: " + e.getMessage());
    } catch (Exception e) {
      logger.error(
          "Unexpected error during face verification for user: {}",
          context.getUser().getId(),
          e);
      handleFailure(context, "An unexpected error occurred. Please try again.");
    }
  }

  private boolean performVerification(
      AuthenticationFlowContext context, FaceCredentialModel credential, String imageData)
      throws BioIdException {
    logger.info("Performing verification for classId: {}", credential.getClassId());

    // Check if imageData is JSON (multiple images) or single image
    if (imageData.startsWith("{")) {
      // Handle multiple images for active liveness detection
      return performLivenessVerification(context, credential, imageData);
    } else {
      // Handle single image (passive liveness or fallback)
      return getCredentialProvider(context.getSession())
          .verifyFace(context.getRealm(), context.getUser(), imageData);
    }
  }

  private boolean performLivenessVerification(
      AuthenticationFlowContext context, FaceCredentialModel credential, String jsonData)
      throws BioIdException {
    
    try {
      logger.debug("Parsing liveness verification JSON data for user: {}", context.getUser().getId());
      
      // Parse the JSON data containing multiple images
      com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
      com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(jsonData);
      
      com.fasterxml.jackson.databind.JsonNode imagesNode = jsonNode.get("images");
      String mode = jsonNode.has("mode") ? jsonNode.get("mode").asText() : "active";
      String challengeDirection = jsonNode.has("challengeDirection") ? jsonNode.get("challengeDirection").asText() : null;
      
      logger.info("Liveness verification mode: {}, challengeDirection: {}", mode, challengeDirection);
      
      if (imagesNode == null || !imagesNode.isArray()) {
        logger.error("Invalid liveness data: images node is null or not an array");
        throw new BioIdException("Invalid image data format");
      }
      
      if (imagesNode.size() < 2) {
        logger.warn("Insufficient images for liveness verification: {} images provided", imagesNode.size());
        throw new BioIdException("At least 2 images required for liveness verification");
      }
      
      String firstImage = imagesNode.get(0).asText();
      String secondImage = imagesNode.get(1).asText();
      
      logger.info("Performing {} liveness verification with {} images for user: {}", 
                  mode, imagesNode.size(), context.getUser().getId());
      logger.info("First image length: {}, starts with: {}", 
                  firstImage != null ? firstImage.length() : 0, 
                  firstImage != null && firstImage.length() > 50 ? firstImage.substring(0, 50) : "null or empty");
      logger.info("Second image length: {}, starts with: {}", 
                  secondImage != null ? secondImage.length() : 0, 
                  secondImage != null && secondImage.length() > 50 ? secondImage.substring(0, 50) : "null or empty");
      
      // Use the credential provider's liveness verification method
      boolean result = getCredentialProvider(context.getSession())
          .verifyFaceWithLiveness(context.getRealm(), context.getUser(), firstImage, secondImage, mode, challengeDirection);
          
      logger.info("Liveness verification result for user {}: {}", context.getUser().getId(), result);
      return result;
          
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      logger.error("Failed to parse JSON liveness verification data for user: {}", context.getUser().getId(), e);
      throw new BioIdException("Invalid JSON format in liveness verification data: " + e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected error during liveness verification for user: {}", context.getUser().getId(), e);
      throw new BioIdException("Liveness verification failed: " + e.getMessage());
    }
  }

  /** Handles a failed verification attempt, managing retries and final failure. */
  private void handleFailure(AuthenticationFlowContext context, String errorMessage) {
    int retryCount = incrementRetryCount(context);
    int maxRetries = getMaxRetries(context);
    com.bioid.keycloak.client.config.BioIdConfiguration bioIdConfig =
        com.bioid.keycloak.client.config.BioIdConfiguration.getInstance();
    LivenessSettings livenessSettings = resolveLivenessSettings(context, bioIdConfig);

    logger.warn(
        "Face verification failed for user: {} (Attempt {}/{}): {}",
        context.getUser().getId(),
        retryCount,
        maxRetries,
        errorMessage);
    
    // Capture failed attempt for later review and training
    try {
      storeFailedAttempt(context, errorMessage, retryCount, maxRetries, livenessSettings);
    } catch (Exception e) {
      logger.error("Failed to store failed authentication attempt", e);
      // Don't fail the authentication flow due to storage issues
    }

    if (retryCount >= maxRetries) {
      logger.warn("Max retries ({}) exceeded for user: {}", maxRetries, context.getUser().getId());
      // Create a more user-friendly error response
      Response errorResponse =
          context
              .form()
              .setError(
                  "Maximum face verification attempts reached. Please use an alternative login method or try again later.")
              .setAttribute("maxRetries", maxRetries)
              .setAttribute("retryCount", retryCount)
              .createErrorPage(Response.Status.UNAUTHORIZED);
      context.failure(AuthenticationFlowError.INVALID_CREDENTIALS, errorResponse);
    } else {
      // Present the challenge again with an error message
      String userFriendlyMessage = getUserFriendlyErrorMessage(errorMessage);
      Response challenge =
          context
              .form()
              .setError(userFriendlyMessage)
              .setAttribute("maxRetries", maxRetries)
              .setAttribute("retryCount", retryCount)
              .setAttribute("livenessActiveEnabled", livenessSettings.isActiveEnabled())
              .setAttribute("livenessChallengeResponseEnabled", livenessSettings.isChallengeEnabled())
              .setAttribute("livenessConfidenceThreshold", bioIdConfig.getLivenessConfidenceThreshold())
              .setAttribute("livenessChallengeTimeoutSeconds", (int) bioIdConfig.getLivenessChallengeTimeout().toSeconds())
              .createForm("face-authenticate.ftl");
      context.challenge(challenge);
    }
  }

  /** Converts technical error messages to user-friendly ones. */
  private String getUserFriendlyErrorMessage(String technicalMessage) {
    if (technicalMessage == null) {
      return "Face verification failed. Please try again.";
    }

    String lowerMessage = technicalMessage.toLowerCase();

    if (lowerMessage.contains("no image data") || lowerMessage.contains("invalid image")) {
      return "Please ensure your camera is working and try again.";
    } else if (lowerMessage.contains("service failed")
        || lowerMessage.contains("service unavailable")) {
      return "Face verification service is temporarily unavailable. Please try again.";
    } else if (lowerMessage.contains("no valid face credential")) {
      return "Face enrollment not found. Please complete face enrollment first.";
    } else if (lowerMessage.contains("verification failed")) {
      return "Face not recognized. Please position your face clearly in the camera and try again.";
    } else {
      return "Face verification failed. Please try again.";
    }
  }

  private int getRetryCount(AuthenticationFlowContext context) {
    String countStr = context.getAuthenticationSession().getAuthNote(ATTR_RETRY_COUNT);
    return countStr == null ? 0 : Integer.parseInt(countStr);
  }

  private int incrementRetryCount(AuthenticationFlowContext context) {
    int count = getRetryCount(context) + 1;
    context.getAuthenticationSession().setAuthNote(ATTR_RETRY_COUNT, String.valueOf(count));
    return count;
  }

  private int getMaxRetries(AuthenticationFlowContext context) {
    return FaceAuthenticatorFactory.getMaxRetries(context.getAuthenticatorConfig());
  }

  /**
   * Store failed authentication attempt with images and metadata.
   */
  private void storeFailedAttempt(
          AuthenticationFlowContext context,
          String errorMessage,
          int retryCount,
          int maxRetries,
          LivenessSettings livenessSettings) {
    
    try {
      // Skip if storage service is not initialized (e.g., in test mode)
      if (failedAuthStorageService == null) {
        logger.debug("Failed auth storage service not initialized");
        return;
      }
      
      // Check if storage is enabled
      if (!FailedAuthConfiguration.getInstance().isStorageEnabled()) {
        logger.debug("Failed auth storage is disabled");
        return;
      }
      
      // Extract images from form data
      MultivaluedMap<String, String> formData = 
          context.getHttpRequest().getDecodedFormParameters();
      String imageData = formData.getFirst("imageData");
      
      if (imageData == null || imageData.isEmpty()) {
        logger.debug("No image data to store");
        return;
      }
      
      // Parse images (handle both single and multiple images)
      List<String> images = parseImages(imageData);
      
      if (images.isEmpty()) {
        logger.debug("No valid images to store");
        return;
      }
      
      // Get session information
      String sessionId = context.getAuthenticationSession().getParentSession().getId();
      String ipAddress = context.getConnection().getRemoteAddr();
      String userAgent = context.getHttpRequest().getHttpHeaders()
          .getHeaderString("User-Agent");
      
      // Determine failure reason
      String failureReason = determineFailureReason(errorMessage);
      
      String livenessMode = determineLivenessMode(livenessSettings, images.size());
      String challengeDirection = extractChallengeDirection(imageData);
      
      // Store the failed attempt
      String attemptId = failedAuthStorageService.storeFailedAttempt(
          context.getSession(),
          context.getRealm(),
          context.getUser(),
          images,
          failureReason,
          null, // verificationScore - not available in current flow
          null, // verificationThreshold - not available
          livenessMode,
          null, // livenessScore - not available
          null, // livenessPassed - not available
          challengeDirection,
          retryCount,
          maxRetries,
          sessionId,
          ipAddress,
          userAgent
      );
      
      if (attemptId != null) {
        logger.info("Stored failed authentication attempt: {} for user: {}", 
            attemptId, context.getUser().getUsername());
      }
      
    } catch (Exception e) {
      logger.error("Failed to store failed authentication attempt", e);
      // Don't propagate - storage failure shouldn't affect authentication flow
    }
  }

  /**
   * Parse images from form data (handles both single and multiple images).
   */
  private List<String> parseImages(String imageData) {
    List<String> images = new ArrayList<>();
    
    try {
      if (imageData.startsWith("{")) {
        // JSON format with multiple images
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
            new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(imageData);
        com.fasterxml.jackson.databind.JsonNode imagesNode = jsonNode.get("images");
        
        if (imagesNode != null && imagesNode.isArray()) {
          for (com.fasterxml.jackson.databind.JsonNode imageNode : imagesNode) {
            images.add(imageNode.asText());
          }
        }
      } else {
        // Single image
        images.add(imageData);
      }
    } catch (Exception e) {
      logger.error("Failed to parse image data", e);
    }
    
    return images;
  }

  /**
   * Determine failure reason from error message.
   */
  private String determineFailureReason(String errorMessage) {
    if (errorMessage == null) {
      return "UNKNOWN";
    }
    
    String lowerMessage = errorMessage.toLowerCase();
    
    if (lowerMessage.contains("liveness")) {
      return "LIVENESS_FAILED";
    } else if (lowerMessage.contains("quality")) {
      return "LOW_QUALITY";
    } else if (lowerMessage.contains("face not found") || lowerMessage.contains("no face")) {
      return "NO_FACE_DETECTED";
    } else if (lowerMessage.contains("verification failed") || lowerMessage.contains("not match")) {
      return "VERIFICATION_FAILED";
    } else if (lowerMessage.contains("timeout")) {
      return "TIMEOUT";
    } else {
      return "VERIFICATION_FAILED";
    }
  }

  /**
   * Determine liveness mode that was configured for the attempt.
   */
  private String determineLivenessMode(LivenessSettings settings, int imageCount) {
    if (settings == null) {
      return "PASSIVE";
    }

    if (settings.isChallengeEnabled()) {
      if (imageCount < 2) {
        logger.debug("Challenge-response mode configured but received {} image(s)", imageCount);
      }
      return "CHALLENGE_RESPONSE";
    }

    if (settings.isActiveEnabled()) {
      if (imageCount < 2) {
        logger.debug("Active liveness mode configured but received {} image(s)", imageCount);
      }
      return "ACTIVE";
    }

    if (settings.getMode() == ResolvedLivenessMode.NONE) {
      return "NONE";
    }

    return "PASSIVE";
  }

  /**
   * Extract challenge direction from JSON data.
   */
  private String extractChallengeDirection(String imageData) {
    try {
      if (imageData.startsWith("{")) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
            new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(imageData);
        
        if (jsonNode.has("challengeDirection")) {
          return jsonNode.get("challengeDirection").asText();
        }
      }
    } catch (Exception e) {
      logger.debug("Could not extract challenge direction", e);
    }
    
    return null;
  }

  @Override
  public boolean requiresUser() {
    return true;
  }

  @Override
  public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
    // Use the session passed directly to this method
    return getCredentialProvider(session).hasValidFaceCredentials(realm, user);
  }

  @Override
  public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    // Use the session passed directly to this method
    if (!configuredFor(session, realm, user)) {
      user.addRequiredAction("face-enroll");
    }
  }

  private LivenessSettings resolveLivenessSettings(
      AuthenticationFlowContext context,
      com.bioid.keycloak.client.config.BioIdConfiguration bioIdConfig) {
    AuthenticatorConfigModel authenticatorConfig = context.getAuthenticatorConfig();
    Map<String, String> configMap =
        authenticatorConfig != null ? authenticatorConfig.getConfig() : null;
    String configuredMode = null;
    if (configMap != null) {
      configuredMode = configMap.get(FaceAuthenticatorFactory.CONFIG_LIVENESS_MODE);
      if (configuredMode != null) {
        configuredMode = configuredMode.trim();
      }
    }

    boolean hasUiOverride = configuredMode != null && !configuredMode.isEmpty();
    if (hasUiOverride) {
      return new LivenessSettings(ResolvedLivenessMode.from(configuredMode), true);
    }

    return new LivenessSettings(deriveModeFromEnvironment(bioIdConfig), false);
  }

  private ResolvedLivenessMode deriveModeFromEnvironment(
      com.bioid.keycloak.client.config.BioIdConfiguration bioIdConfig) {
    if (bioIdConfig == null) {
      return ResolvedLivenessMode.PASSIVE;
    }

    if (bioIdConfig.isLivenessChallengeResponseEnabled()) {
      return ResolvedLivenessMode.CHALLENGE_RESPONSE;
    }

    if (bioIdConfig.isLivenessActiveEnabled()) {
      return ResolvedLivenessMode.ACTIVE;
    }

    return ResolvedLivenessMode.PASSIVE;
  }

  private enum ResolvedLivenessMode {
    NONE,
    PASSIVE,
    ACTIVE,
    CHALLENGE_RESPONSE;

    static ResolvedLivenessMode from(String value) {
      if (value == null || value.trim().isEmpty()) {
        return PASSIVE;
      }
      try {
        return ResolvedLivenessMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        logger.warn("Unknown liveness mode '{}' configured in UI; defaulting to PASSIVE", value);
        return PASSIVE;
      }
    }
  }

  private static final class LivenessSettings {
    private final ResolvedLivenessMode mode;
    private final boolean activeEnabled;
    private final boolean challengeEnabled;
    private final boolean definedInUi;

    private LivenessSettings(ResolvedLivenessMode mode, boolean definedInUi) {
      this.mode = mode != null ? mode : ResolvedLivenessMode.PASSIVE;
      this.definedInUi = definedInUi;
      this.challengeEnabled = this.mode == ResolvedLivenessMode.CHALLENGE_RESPONSE;
      this.activeEnabled = this.challengeEnabled || this.mode == ResolvedLivenessMode.ACTIVE;
    }

    boolean isDefinedInUi() {
      return definedInUi;
    }

    boolean isActiveEnabled() {
      return activeEnabled;
    }

    boolean isChallengeEnabled() {
      return challengeEnabled;
    }

    String modeName() {
      return mode.name();
    }

    ResolvedLivenessMode getMode() {
      return mode;
    }
  }

  @Override
  public void close() {}
}
