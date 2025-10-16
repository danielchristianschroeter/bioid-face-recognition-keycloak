package com.bioid.keycloak.authenticator;

import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.credential.FaceCredentialModel;
import com.bioid.keycloak.credential.FaceCredentialProvider;
import com.bioid.keycloak.credential.FaceCredentialProviderFactory;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keycloak authenticator for face biometric verification.
 *
 * <p>This authenticator integrates face recognition into Keycloak authentication flows, providing a
 * modern, secure, and user-friendly verification experience.
 */
public class FaceAuthenticator implements Authenticator {

  private static final Logger logger = LoggerFactory.getLogger(FaceAuthenticator.class);
  private static final String ATTR_RETRY_COUNT = "face.auth.retry.count";

  public FaceAuthenticator(KeycloakSession session) {
    // The session passed here by the factory can become stale.
    // It's better not to store it and use the one from the context instead.
    // this.session = session;
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
   * Called when this authenticator is invoked in the flow. It checks if the user is enrolled and
   * presents the verification UI.
   */
  @Override
  public void authenticate(AuthenticationFlowContext context) {
    UserModel user = context.getUser();
    
    // Check if user has disabled face authentication
    boolean faceAuthEnabled = Boolean.parseBoolean(
        user.getFirstAttribute("face.auth.enabled"));
    
    // If face auth is explicitly disabled, skip
    if (user.getFirstAttribute("face.auth.enabled") != null && !faceAuthEnabled) {
      logger.debug("Face authentication is disabled for user: {}", user.getId());
      context.attempted();
      return;
    }

    // Use the session from the context
    if (!getCredentialProvider(context.getSession())
        .hasValidFaceCredentials(context.getRealm(), user)) {
      logger.debug(
          "User {} does not have face credentials. Skipping face authentication.",
          user.getId());
      context.attempted();
      return;
    }

    int retryCount = getRetryCount(context);
    
    // Get liveness configuration from BioIdConfiguration
    com.bioid.keycloak.client.config.BioIdConfiguration bioIdConfig = 
        com.bioid.keycloak.client.config.BioIdConfiguration.getInstance();
    
    logger.info("Liveness config - Active: {}, Challenge-Response: {}, Threshold: {}", 
                bioIdConfig.isLivenessActiveEnabled(), 
                bioIdConfig.isLivenessChallengeResponseEnabled(), 
                bioIdConfig.getLivenessConfidenceThreshold());
    
    // Debug logging for liveness configuration
    logger.info("Liveness configuration - Active: {}, Challenge-Response: {}, Confidence: {}, Timeout: {}s", 
                bioIdConfig.isLivenessActiveEnabled(),
                bioIdConfig.isLivenessChallengeResponseEnabled(),
                bioIdConfig.getLivenessConfidenceThreshold(),
                bioIdConfig.getLivenessChallengeTimeout().toSeconds());

    Response challenge =
        context
            .form()
            .setAttribute("maxRetries", getMaxRetries(context))
            .setAttribute("retryCount", retryCount)
            .setAttribute("livenessActiveEnabled", bioIdConfig.isLivenessActiveEnabled())
            .setAttribute("livenessChallengeResponseEnabled", bioIdConfig.isLivenessChallengeResponseEnabled())
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

    logger.warn(
        "Face verification failed for user: {} (Attempt {}/{}): {}",
        context.getUser().getId(),
        retryCount,
        maxRetries,
        errorMessage);

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
      // Get liveness configuration to maintain settings across retries
      com.bioid.keycloak.client.config.BioIdConfiguration bioIdConfig = 
          com.bioid.keycloak.client.config.BioIdConfiguration.getInstance();
      
      // Present the challenge again with an error message
      String userFriendlyMessage = getUserFriendlyErrorMessage(errorMessage);
      Response challenge =
          context
              .form()
              .setError(userFriendlyMessage)
              .setAttribute("maxRetries", maxRetries)
              .setAttribute("retryCount", retryCount)
              .setAttribute("livenessActiveEnabled", bioIdConfig.isLivenessActiveEnabled())
              .setAttribute("livenessChallengeResponseEnabled", bioIdConfig.isLivenessChallengeResponseEnabled())
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

  @Override
  public void close() {}
}
