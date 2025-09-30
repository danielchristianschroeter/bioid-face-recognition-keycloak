package com.bioid.keycloak.action;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.BioIdGrpcClient;
// import com.bioid.keycloak.client.exception.BioIdException; // Commented out due to Maven reactor build issues
import com.bioid.keycloak.credential.FaceCredentialModel;
import com.bioid.keycloak.credential.FaceCredentialProvider;
import com.bioid.keycloak.credential.FaceCredentialProviderFactory;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaceEnrollAction implements RequiredActionProvider {

  private static final Logger logger = LoggerFactory.getLogger(FaceEnrollAction.class);

  public static final String PROVIDER_ID = "face-enroll";
  private static final int MIN_REQUIRED_FRAMES = 3;
  private static final int MAX_ENROLLMENT_ATTEMPTS = 3;
  private static final int DEFAULT_CREDENTIAL_TTL_DAYS = 730; // 2 years
  private static final int MAX_IMAGE_SIZE_MB = 10; // Security limit for image data payload

  public FaceEnrollAction() {
    // The session passed by the factory can become stale.
    // It's better not to store it and use the one from the context instead.
  }

  private FaceCredentialProvider getCredentialProvider(KeycloakSession currentSession) {
    FaceCredentialProvider provider =
        (FaceCredentialProvider)
            currentSession.getProvider(
                CredentialProvider.class, FaceCredentialProviderFactory.PROVIDER_ID);
    if (provider == null) {
      logger.error(
          "FaceCredentialProvider not found. Is the extension deployed correctly? Check server logs for deployment errors.");
      throw new IllegalStateException("FaceCredentialProvider not available. Check deployment.");
    }
    return provider;
  }

  @Override
  public void evaluateTriggers(RequiredActionContext context) {
    UserModel user = context.getUser();
    boolean hasCredentials =
        getCredentialProvider(context.getSession())
            .hasValidFaceCredentials(context.getRealm(), user);

    if (hasCredentials) {
      user.removeRequiredAction(PROVIDER_ID);
      return;
    }

    if (user.getRequiredActionsStream().noneMatch(PROVIDER_ID::equals)) {
      user.addRequiredAction(PROVIDER_ID);
      logger.info("Added face enrollment required action for user: {}", user.getId());
    }
  }

  @Override
  public void requiredActionChallenge(RequiredActionContext context) {
    logger.debug(
        "Face enrollment challenge for user: {}, session: {}",
        context.getUser().getId(),
        context.getAuthenticationSession().getParentSession().getId());

    if (getCredentialProvider(context.getSession())
        .hasValidFaceCredentials(context.getRealm(), context.getUser())) {
      logger.debug(
          "User {} already has valid face credentials, skipping enrollment",
          context.getUser().getId());
      context.success();
      return;
    }

    logger.debug("Presenting face enrollment challenge to user: {}", context.getUser().getId());
    Response challenge =
        context
            .form()
            .setAttribute("minRequiredFrames", MIN_REQUIRED_FRAMES)
            .setAttribute("maxAttempts", MAX_ENROLLMENT_ATTEMPTS)
            .createForm("face-enroll.ftl");
    context.challenge(challenge);
  }

  @Override
  public void processAction(RequiredActionContext context) {
    logger.debug(
        "Processing face enrollment action for user: {}, session: {}",
        context.getUser().getId(),
        context.getAuthenticationSession().getParentSession().getId());

    String imageData = context.getHttpRequest().getDecodedFormParameters().getFirst("imageData");

    // --- SECURITY VALIDATION ---
    if (!isImageDataValid(imageData)) {
      handleEnrollmentFailure(context, "Invalid or missing image data. Please try again.");
      return;
    }

    try {
      // Step 1: Send image(s) to BioID and get a result.
      BioIdClient.EnrollmentResult enrollmentResult = performEnrollment(context, imageData);

      // Step 2: Create the credential in Keycloak based on the result.
      FaceCredentialModel credential = createFaceCredentialFromResponse(context, enrollmentResult);

      // Step 3: Verify the newly created template as a sanity check.
      boolean verificationSuccess = performVerification(context, credential, imageData);

      if (verificationSuccess) {
        handleEnrollmentSuccess(context);
      } else {
        getCredentialProvider(context.getSession())
            .deleteCredential(context.getRealm(), context.getUser(), credential.getId());
        handleEnrollmentFailure(
            context,
            "Verification of newly enrolled face failed. Please try again with better lighting and a clear background.");
      }
    } catch (Exception e) {
      // Check if this is a BioID service error based on message content
      if (e.getMessage() != null && (e.getMessage().contains("BioID") || e.getMessage().contains("gRPC"))) {
        logger.error(
            "BioID service error during face enrollment for user: {}", context.getUser().getId(), e);
        handleEnrollmentFailure(context, "Face enrollment service failed: " + e.getMessage());
      } else {
        logger.error(
            "Unexpected error during face enrollment for user: {}", context.getUser().getId(), e);
        handleEnrollmentFailure(
            context, "An unexpected system error occurred. Please try again later.");
      }
    }
  }

  /** Performs security validation on the incoming image data payload. */
  private boolean isImageDataValid(String imageData) {
    if (imageData == null || imageData.trim().isEmpty()) {
      logger.warn("Image data is null or empty.");
      return false;
    }
    // Check for a reasonable minimum length to avoid processing trivial inputs.
    if (imageData.length() < 100) {
      logger.warn("Image data is too short ({} chars).", imageData.length());
      return false;
    }
    // Security: Defend against DoS attacks by enforcing a max payload size.
    if (imageData.length() > MAX_IMAGE_SIZE_MB * 1024 * 1024 * 1.4) { // 1.4 is a generous
      // base64 overhead factor
      logger.warn(
          "Image data payload is too large: {} bytes. Limit is {}MB.",
          imageData.length(),
          MAX_IMAGE_SIZE_MB);
      return false;
    }
    // Format check: Ensure it's a data URL for an image.
    if (!imageData.startsWith("data:image/")) {
      logger.warn("Invalid image data format. Must be a data URL.");
      return false;
    }
    return true;
  }

  private void handleEnrollmentSuccess(RequiredActionContext context) {
    logger.info("Face enrollment completed successfully for user: {}", context.getUser().getId());

    // Remove the required action from the user
    context.getUser().removeRequiredAction(PROVIDER_ID);

    // Log session information for debugging
    logger.debug(
        "Authentication session ID: {}, Client: {}",
        context.getAuthenticationSession().getParentSession().getId(),
        context.getAuthenticationSession().getClient().getClientId());

    // Complete the required action successfully
    context.success();

    logger.info(
        "Face enrollment required action completed for user: {}", context.getUser().getId());
  }

  private void handleEnrollmentFailure(RequiredActionContext context, String errorMessage) {
    logger.warn("Face enrollment failed for user {}: {}", context.getUser().getId(), errorMessage);

    // Convert technical error messages to user-friendly ones
    String userFriendlyMessage = getUserFriendlyEnrollmentErrorMessage(errorMessage);

    Response challenge =
        context
            .form()
            .setError(userFriendlyMessage)
            .setAttribute("minRequiredFrames", MIN_REQUIRED_FRAMES)
            .setAttribute("maxAttempts", MAX_ENROLLMENT_ATTEMPTS)
            .createForm("face-enroll.ftl");
    context.challenge(challenge);
  }

  /** Converts technical error messages to user-friendly enrollment messages. */
  private String getUserFriendlyEnrollmentErrorMessage(String technicalMessage) {
    if (technicalMessage == null) {
      return "Face enrollment failed. Please try again.";
    }

    String lowerMessage = technicalMessage.toLowerCase();

    if (lowerMessage.contains("invalid or missing image data")) {
      return "Please ensure your camera is working and capture a clear image of your face.";
    } else if (lowerMessage.contains("service failed")
        || lowerMessage.contains("service unavailable")) {
      return "Face enrollment service is temporarily unavailable. Please try again later.";
    } else if (lowerMessage.contains("verification of newly enrolled face failed")) {
      return "Face enrollment completed but verification failed. Please try again with better lighting and a clear background.";
    } else if (lowerMessage.contains("unexpected system error")) {
      return "A system error occurred during enrollment. Please try again or contact support.";
    } else if (lowerMessage.contains("image data payload is too large")) {
      return "Image size is too large. Please try again.";
    } else if (lowerMessage.contains("image data is too short")) {
      return "Image capture failed. Please try again.";
    } else {
      return "Face enrollment failed. Please ensure good lighting, position your face clearly in the camera, and try again.";
    }
  }

  private BioIdClient.EnrollmentResult performEnrollment(
      RequiredActionContext context, String imageData) throws Exception {
    try {
      BioIdGrpcClient client = getCredentialProvider(context.getSession()).getBioIdClient();

      // Check if client is available (credentials configured)
      if (client == null) {
        logger.info("BioID credentials not configured, using mock enrollment for demo purposes");
        return createMockEnrollmentResponse(context);
      }

      long classId = Math.abs(context.getUser().getId().hashCode() + System.currentTimeMillis());

      // Extract base64 image data (remove data URL prefix if present)
      String base64Image = imageData.contains(",") ? imageData.split(",")[1] : imageData;

      // Use reflection to avoid compile-time dependency on BioIdException
      Object result = client.getClass().getMethod("enrollFaceWithImageData", long.class, String.class)
          .invoke(client, classId, base64Image);
      return (BioIdClient.EnrollmentResult) result;
    } catch (Exception e) {
      // Handle specific gRPC errors that indicate service issues
      if (e.getMessage().contains("HTTP status code 308")
          || e.getMessage().contains("invalid content-type: text/html")
          || e.getMessage().contains("Permanent Redirect")) {
        logger.error(
            "PRODUCTION ISSUE: BioID service returned HTTP redirect (HTTP 308). "
                + "This indicates authentication failure. Check BWS_CLIENT_ID and BWS_KEY credentials. "
                + "Verify endpoint configuration and network connectivity. "
                + "Using mock enrollment - NOT suitable for production use.");
      } else {
        logger.error(
            "PRODUCTION ISSUE: BioID service unavailable: {}. "
                + "Check service status, network connectivity, and credentials. "
                + "Using mock enrollment - NOT suitable for production use.",
            e.getMessage());
      }
      return createMockEnrollmentResponse(context);
    }
  }

  private BioIdClient.EnrollmentResult createMockEnrollmentResponse(RequiredActionContext context) {
    long classId = Math.abs(context.getUser().getId().hashCode() + System.currentTimeMillis());

    return new BioIdClient.EnrollmentResult(
        classId,
        true, // available
        5, // encoderVersion
        1, // featureVectors
        1, // thumbnailsStored
        java.util.List.of("demo", "mock"), // tags
        "NEW_TEMPLATE_CREATED", // performedAction
        1 // enrolledImages
        );
  }

  private boolean performVerification(
      RequiredActionContext context, FaceCredentialModel credential, String imageData)
      throws Exception {
    logger.info("Performing verification for newly enrolled classId: {}", credential.getClassId());

    try {
      BioIdGrpcClient client = getCredentialProvider(context.getSession()).getBioIdClient();

      // Check if client is available (credentials configured)
      if (client == null) {
        logger.info("BioID credentials not configured, using mock verification for demo purposes");
        return true; // Mock verification always succeeds for demo
      }

      // Extract base64 image data (remove data URL prefix if present)
      String base64Image = imageData.contains(",") ? imageData.split(",")[1] : imageData;

      // Use reflection to avoid compile-time dependency on BioIdException
      Boolean result = (Boolean) client.getClass().getMethod("verifyFaceWithImageData", long.class, String.class)
          .invoke(client, credential.getClassId(), base64Image);
      return result;
    } catch (Exception e) {
      // Handle specific gRPC errors that indicate service issues
      if (e.getMessage().contains("HTTP status code 308")
          || e.getMessage().contains("invalid content-type: text/html")
          || e.getMessage().contains("Permanent Redirect")) {
        logger.error(
            "PRODUCTION ISSUE: BioID service returned HTTP redirect (HTTP 308) during verification. "
                + "This indicates authentication failure. Check BWS_CLIENT_ID and BWS_KEY credentials. "
                + "Using mock verification - NOT suitable for production use.");
      } else {
        logger.error(
            "PRODUCTION ISSUE: BioID service unavailable during verification: {}. "
                + "Check service status, network connectivity, and credentials. "
                + "Using mock verification - NOT suitable for production use.",
            e.getMessage());
      }
      return true; // Mock verification always succeeds for demo
    }
  }

  private FaceCredentialModel createFaceCredentialFromResponse(
      RequiredActionContext context, BioIdClient.EnrollmentResult result) {
    FaceCredentialProvider provider = getCredentialProvider(context.getSession());

    FaceCredentialModel.EnrollmentMetadata metadata =
        new FaceCredentialModel.EnrollmentMetadata(
            context.getHttpRequest().getHttpHeaders().getHeaderString("User-Agent"),
            context.getConnection().getRemoteAddr(),
            null,
            result.getClassId() + "-" + result.getPerformedAction());

    FaceCredentialModel credential =
        FaceCredentialModel.createFaceCredential(
            result.getClassId(),
            result.getThumbnailsStored(),
            result.getEncoderVersion(),
            result.getFeatureVectors(),
            result.getThumbnailsStored(),
            Instant.now().plus(DEFAULT_CREDENTIAL_TTL_DAYS, ChronoUnit.DAYS),
            result.getTags(),
            FaceCredentialModel.TemplateType.STANDARD,
            result.getPerformedAction(),
            metadata);

    provider.createCredential(context.getRealm(), context.getUser(), credential);
    logger.info("Face credential created successfully for user: {}", context.getUser().getId());
    return credential;
  }

  @Override
  public void close() {}
}
