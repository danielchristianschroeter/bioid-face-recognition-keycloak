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
    Response challenge =
        context
            .form()
            .setAttribute("maxRetries", getMaxRetries(context))
            .setAttribute("retryCount", retryCount)
            .createForm("face-authenticate.ftl");
    context.challenge(challenge);
  }

  /** Called when the user submits the verification form from the UI. */
  @Override
  public void action(AuthenticationFlowContext context) {
    MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
    String imageData = formData.getFirst("imageData");

    if (imageData == null || imageData.isEmpty()) {
      handleFailure(context, "No image data provided. Please try again.");
      return;
    }

    // Use the session from the context
    FaceCredentialModel credential =
        getCredentialProvider(context.getSession())
            .getMostRecentFaceCredential(context.getRealm(), context.getUser());
    if (credential == null) {
      handleFailure(context, "No valid face credential found for user. Please try re-enrolling.");
      return;
    }

    try {
      boolean verificationSuccess = performVerification(context, credential, imageData);
      if (verificationSuccess) {
        logger.info("Face verification successful for user: {}", context.getUser().getId());
        context.success();
      } else {
        handleFailure(context, "Face verification failed. Please try again.");
      }
    } catch (BioIdException e) {
      logger.error(
          "BioID service error during face verification for user: {}",
          context.getUser().getId(),
          e);
      handleFailure(context, "Face verification service failed: " + e.getMessage());
    }
  }

  private boolean performVerification(
      AuthenticationFlowContext context, FaceCredentialModel credential, String imageData)
      throws BioIdException {
    logger.info("Performing verification for classId: {}", credential.getClassId());

    // Use the credential provider's verification method which handles protobuf details internally
    return getCredentialProvider(context.getSession())
        .verifyFace(context.getRealm(), context.getUser(), imageData);
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
      // Present the challenge again with an error message
      String userFriendlyMessage = getUserFriendlyErrorMessage(errorMessage);
      Response challenge =
          context
              .form()
              .setError(userFriendlyMessage)
              .setAttribute("maxRetries", maxRetries)
              .setAttribute("retryCount", retryCount)
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
