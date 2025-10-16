package com.bioid.keycloak.action;

import com.bioid.keycloak.client.BioIdClient;
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
import org.keycloak.events.Errors;
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

    // Log what we received
    if (imageData != null) {
      logger.info("Received imageData length: {}, starts with: {}", 
          imageData.length(), 
          imageData.substring(0, Math.min(100, imageData.length())));
    } else {
      logger.warn("Received null imageData");
    }

    // --- SECURITY VALIDATION ---
    if (!isImageDataValid(imageData)) {
      handleEnrollmentFailure(context, "Invalid or missing image data. Please try again.");
      return;
    }

    try {
      // Parse image data - could be single image or JSON array of images
      java.util.List<String> imageList = parseImageData(imageData);
      
      if (imageList.isEmpty()) {
        handleEnrollmentFailure(context, "No valid images provided. Please try again.");
        return;
      }
      
      logger.info("Processing enrollment with {} image(s) for user: {}", 
          imageList.size(), context.getUser().getId());

      // Step 1: Send image(s) to BioID and get a result.
      BioIdClient.EnrollmentResult enrollmentResult = performEnrollment(context, imageList);

      // Step 2: Create the credential in Keycloak based on the result.
      FaceCredentialModel credential = createFaceCredentialFromResponse(context, enrollmentResult);

      // Step 3: Verify the newly created template as a sanity check using the first image.
      boolean verificationSuccess = performVerification(context, credential, imageList.get(0));

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
      String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
      if (errorMessage.contains("BioID") || errorMessage.contains("gRPC") || errorMessage.contains("BWS")) {
        logger.error(
            "BioID service error during face enrollment for user: {}", context.getUser().getId(), e);
        handleEnrollmentFailure(context, "Face enrollment service failed: " + errorMessage);
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
    // For single images: MAX_IMAGE_SIZE_MB * 1.4 (base64 overhead)
    // For JSON arrays: Allow up to 3 images, each MAX_IMAGE_SIZE_MB
    int maxSingleImageBytes = (int) (MAX_IMAGE_SIZE_MB * 1024 * 1024 * 1.4);
    int maxMultiImageBytes = maxSingleImageBytes * 3 + 1000; // +1000 for JSON overhead
    
    boolean isJsonArray = imageData.trim().startsWith("[");
    int maxAllowedSize = isJsonArray ? maxMultiImageBytes : maxSingleImageBytes;
    
    if (imageData.length() > maxAllowedSize) {
      logger.warn(
          "Image data payload is too large: {} bytes. Limit is {} bytes ({} images).",
          imageData.length(),
          maxAllowedSize,
          isJsonArray ? "multiple" : "single");
      return false;
    }
    // Format check: Ensure it's either a data URL for an image or a JSON array
    if (!imageData.startsWith("data:image/") && !imageData.trim().startsWith("[")) {
      logger.warn("Invalid image data format. Must be a data URL or JSON array. Starts with: {}", 
          imageData.substring(0, Math.min(50, imageData.length())));
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

  /**
   * Parses image data which can be either a single image string or a JSON array of images.
   */
  private java.util.List<String> parseImageData(String imageData) {
    java.util.List<String> imageList = new java.util.ArrayList<>();
    
    if (imageData == null || imageData.trim().isEmpty()) {
      logger.warn("Image data is null or empty");
      return imageList;
    }
    
    // Check if it's a JSON array
    String trimmed = imageData.trim();
    logger.info("Parsing image data, starts with '[': {}, length: {}", 
        trimmed.startsWith("["), trimmed.length());
    
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
      try {
        // Use Jackson ObjectMapper which is available in Keycloak
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String[] images = mapper.readValue(trimmed, String[].class);
        
        for (String img : images) {
          if (img != null && !img.trim().isEmpty()) {
            imageList.add(img);
          }
        }
        
        logger.info("Successfully parsed {} images from JSON array using Jackson", imageList.size());
      } catch (Exception e) {
        logger.error("Failed to parse JSON array with Jackson, trying manual parsing", e);
        
        // Fallback to manual parsing
        try {
          String content = trimmed.substring(1, trimmed.length() - 1);
          
          if (content.trim().isEmpty()) {
            logger.warn("Empty JSON array received");
            return imageList;
          }
          
          // Simple split by "," for quoted strings
          int start = 0;
          boolean inQuotes = false;
          
          for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
              inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
              String img = content.substring(start, i).trim();
              if (img.startsWith("\"") && img.endsWith("\"")) {
                img = img.substring(1, img.length() - 1);
              }
              if (!img.isEmpty()) {
                imageList.add(img);
              }
              start = i + 1;
            }
          }
          
          // Add the last image
          String lastImg = content.substring(start).trim();
          if (lastImg.startsWith("\"") && lastImg.endsWith("\"")) {
            lastImg = lastImg.substring(1, lastImg.length() - 1);
          }
          if (!lastImg.isEmpty()) {
            imageList.add(lastImg);
          }
          
          logger.info("Successfully parsed {} images from JSON array using fallback", imageList.size());
        } catch (Exception e2) {
          logger.error("Both Jackson and manual parsing failed, treating as single image", e2);
          imageList.add(imageData);
        }
      }
    } else {
      // Single image
      logger.info("Treating as single image (not a JSON array)");
      imageList.add(imageData);
    }
    
    return imageList;
  }

  private BioIdClient.EnrollmentResult performEnrollment(
      RequiredActionContext context, java.util.List<String> imageDataList) throws Exception {
    try {
      BioIdClient client = getCredentialProvider(context.getSession()).getBioIdClient();

      // Check if client is available (credentials configured)
      if (client == null) {
        logger.error("SECURITY ISSUE: BioID credentials not configured - face enrollment cannot proceed");
        
        // Set error message for user
        context.getEvent().error(Errors.INVALID_REQUEST);
        Response errorResponse = context.form()
            .setError("Face enrollment service is not available. Please contact your administrator.")
            .createForm("face-enroll.ftl");
        context.challenge(errorResponse);
        return null;
      }

      long classId = Math.abs(context.getUser().getId().hashCode() + System.currentTimeMillis());

      // Process all images - remove data URL prefix if present
      java.util.List<String> base64Images = new java.util.ArrayList<>();
      for (String imageData : imageDataList) {
        String base64Image = imageData.contains(",") ? imageData.split(",")[1] : imageData;
        base64Images.add(base64Image);
      }

      // Use reflection to call the multi-image enrollment method
      Object result = client.getClass()
          .getMethod("enrollFaceWithMultipleImages", long.class, java.util.List.class)
          .invoke(client, classId, base64Images);
      return (BioIdClient.EnrollmentResult) result;
    } catch (Exception e) {
      // Handle specific gRPC errors that indicate service issues
      String errorMessage = e.getMessage();
      if (errorMessage == null) {
        errorMessage = e.getClass().getSimpleName();
      }
      
      if (errorMessage.contains("HTTP status code 308")
          || errorMessage.contains("invalid content-type: text/html")
          || errorMessage.contains("Permanent Redirect")) {
        logger.error(
            "PRODUCTION ISSUE: BioID service returned HTTP redirect (HTTP 308). "
                + "This indicates authentication failure. Check BWS_CLIENT_ID and BWS_KEY credentials. "
                + "Verify endpoint configuration and network connectivity.");
      } else if (errorMessage.contains("DEADLINE_EXCEEDED") || errorMessage.contains("timeout")) {
        logger.error(
            "PRODUCTION ISSUE: BioID enrollment timed out after processing {} images: {}",
            imageDataList.size(), errorMessage);
      } else {
        logger.error(
            "PRODUCTION ISSUE: BioID service unavailable: {}. "
                + "Check service status, network connectivity, and credentials.",
            errorMessage);
      }
      
      // Set error message for user
      context.getEvent().error(Errors.INVALID_REQUEST);
      Response errorResponse = context.form()
          .setError("Face enrollment failed. Please try again or contact your administrator.")
          .createForm("face-enroll.ftl");
      context.challenge(errorResponse);
      return null;
    }
  }



  private boolean performVerification(
      RequiredActionContext context, FaceCredentialModel credential, String imageData)
      throws Exception {
    logger.info("Performing verification for newly enrolled classId: {}", credential.getClassId());

    try {
      BioIdClient client = getCredentialProvider(context.getSession()).getBioIdClient();

      // Check if client is available (credentials configured)
      if (client == null) {
        logger.error("SECURITY ISSUE: BioID credentials not configured - face verification cannot proceed");
        return false; // Fail securely when client is not available
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
                + "This indicates authentication failure. Check BWS_CLIENT_ID and BWS_KEY credentials.");
      } else {
        logger.error(
            "PRODUCTION ISSUE: BioID service unavailable during verification: {}. "
                + "Check service status, network connectivity, and credentials.",
            e.getMessage());
      }
      return false; // Fail securely when service is unavailable
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
