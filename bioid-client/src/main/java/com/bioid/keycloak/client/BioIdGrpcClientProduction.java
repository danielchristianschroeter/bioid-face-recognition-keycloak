package com.bioid.keycloak.client;

import com.bioid.keycloak.client.config.BioIdConfiguration;
import com.bioid.keycloak.client.debug.ImageDebugStorage;
import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.services.*;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Production-ready gRPC client for BioID BWS 3 Face Recognition service.
 * 
 * This implementation uses proper gRPC protocol with protobuf messages and JWT authentication as
 * required by BWS 3.
 */
public class BioIdGrpcClientProduction implements BioIdClient {

  private static final Logger logger = LoggerFactory.getLogger(BioIdGrpcClientProduction.class);

  private final BioIdConfiguration config;
  private final String baseEndpoint;
  private final String clientId;
  private final String secretKey;
  private final ManagedChannel faceChannel;
  private final ManagedChannel bwsChannel;
  private final FaceRecognitionGrpc.FaceRecognitionBlockingStub faceRecognitionStub;
  private final BioIDWebServiceGrpc.BioIDWebServiceBlockingStub bwsStub;
  private final com.bioid.keycloak.client.debug.ImageDebugStorage debugStorage;

  public BioIdGrpcClientProduction(BioIdConfiguration config, String baseEndpoint, String clientId,
      String secretKey) {
    this.config = config;
    this.baseEndpoint = baseEndpoint;
    this.clientId = clientId;
    this.secretKey = secretKey;

    // Create gRPC channel for face recognition service
    String faceEndpoint = "face" + baseEndpoint;
    logger.info("Initializing gRPC channel to: {}", faceEndpoint);

    this.faceChannel = NettyChannelBuilder.forTarget(faceEndpoint).useTransportSecurity().build();

    // Create gRPC channel for BWS service (liveness detection)
    String bwsEndpoint = "grpc" + baseEndpoint;
    logger.info("Initializing BWS gRPC channel to: {}", bwsEndpoint);

    this.bwsChannel = NettyChannelBuilder.forTarget(bwsEndpoint).useTransportSecurity().build();

    // Create stubs with JWT authentication interceptor
    this.faceRecognitionStub =
        FaceRecognitionGrpc.newBlockingStub(faceChannel).withInterceptors(new JwtAuthInterceptor());

    this.bwsStub =
        BioIDWebServiceGrpc.newBlockingStub(bwsChannel).withInterceptors(new JwtAuthInterceptor());

    this.debugStorage = new com.bioid.keycloak.client.debug.ImageDebugStorage(config);

    logger.info("BioID gRPC client initialized successfully");
  }

  @Override
  public void enroll(byte[] imageData, long classId) throws BioIdException {
    logger.info("BWS gRPC enroll called for classId: {}", classId);

    try {
      // Create image data message
      Bwsmessages.ImageData image = Bwsmessages.ImageData.newBuilder()
          .setImage(com.google.protobuf.ByteString.copyFrom(imageData)).build();

      // Create enrollment request
      Facerecognition.FaceEnrollmentRequest request = Facerecognition.FaceEnrollmentRequest
          .newBuilder().setClassId(classId).addImages(image).build();

      // Call gRPC service
      Facerecognition.FaceEnrollmentResponse response = faceRecognitionStub
          .withDeadlineAfter(config.getEnrollmentTimeout().toMillis(), TimeUnit.MILLISECONDS)
          .enroll(request);

      // Check response status
      if (response.getStatus() != Bwsmessages.JobStatus.SUCCEEDED) {
        String errors = response.getErrorsList().stream().map(Bwsmessages.JobError::getMessage)
            .collect(Collectors.joining(", "));
        logger.error("BWS enrollment failed with status: {} - Errors: {}", response.getStatus(),
            errors);
        throw new BioIdException("BWS enrollment failed: " + errors);
      }

      // Check performed action
      if (response
          .getPerformedAction() == Facerecognition.FaceEnrollmentResponse.EnrollmentAction.ENROLLMENT_FAILED) {
        String errors = response.getErrorsList().stream().map(Bwsmessages.JobError::getMessage)
            .collect(Collectors.joining(", "));
        logger.error("BWS enrollment action failed - Errors: {}", errors);
        throw new BioIdException("BWS enrollment failed: " + errors);
      }

      logger.info("BWS enrollment successful for classId: {} - Action: {}, Enrolled images: {}",
          classId, response.getPerformedAction(), response.getEnrolledImages());

    } catch (StatusRuntimeException e) {
      logger.error("gRPC error during BWS enrollment for classId: {}", classId, e);
      throw new BioIdException("BWS enrollment gRPC error: " + e.getStatus().getDescription(), e);
    } catch (Exception e) {
      logger.error("Error during BWS enrollment for classId: {}", classId, e);
      throw new BioIdException("BWS enrollment error: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean verify(byte[] imageData, long classId) throws BioIdException {
    logger.info("BWS gRPC verify called for classId: {}", classId);

    try {
      // Create image data message
      Bwsmessages.ImageData image = Bwsmessages.ImageData.newBuilder()
          .setImage(com.google.protobuf.ByteString.copyFrom(imageData)).build();

      // Create verification request
      Facerecognition.FaceVerificationRequest request = Facerecognition.FaceVerificationRequest
          .newBuilder().setClassId(classId).setImage(image).build();

      // Call gRPC service
      Facerecognition.FaceVerificationResponse response = faceRecognitionStub
          .withDeadlineAfter(config.getVerificationTimeout().toMillis(), TimeUnit.MILLISECONDS)
          .verify(request);

      // Check response status
      if (response.getStatus() != Bwsmessages.JobStatus.SUCCEEDED) {
        String errors = response.getErrorsList().stream().map(Bwsmessages.JobError::getMessage)
            .collect(Collectors.joining(", "));
        logger.error("BWS verification failed with status: {} - Errors: {}", response.getStatus(),
            errors);
        throw new BioIdException("BWS verification failed: " + errors);
      }

      logger.info("BWS verification result for classId {}: verified={}, score={}", classId,
          response.getVerified(), response.getScore());

      return response.getVerified();

    } catch (StatusRuntimeException e) {
      logger.error("gRPC error during BWS verification for classId: {}", classId, e);

      // Handle specific gRPC errors
      if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
        throw new BioIdException("Face template not found - user needs to re-enroll", e);
      }

      throw new BioIdException("BWS verification gRPC error: " + e.getStatus().getDescription(), e);
    } catch (Exception e) {
      logger.error("Error during BWS verification for classId: {}", classId, e);
      throw new BioIdException("BWS verification error: " + e.getMessage(), e);
    }
  }

  @Override
  public void deleteTemplate(long classId) throws BioIdException {
    logger.info("BWS gRPC deleteTemplate called for classId: {}", classId);

    try {
      // Create delete request
      Facerecognition.DeleteTemplateRequest request =
          Facerecognition.DeleteTemplateRequest.newBuilder().setClassId(classId).build();

      // Call gRPC service
      faceRecognitionStub.withDeadlineAfter(5, TimeUnit.SECONDS).deleteTemplate(request);

      logger.info("BWS template deletion successful for classId: {}", classId);

    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
        logger.warn(
            "BWS template deletion - template for classId {} does not exist (already deleted?)",
            classId);
        // Don't throw exception for NOT_FOUND on delete
        return;
      }

      logger.error("gRPC error during BWS template deletion for classId: {}", classId, e);
      throw new BioIdException(
          "BWS template deletion gRPC error: " + e.getStatus().getDescription(), e);
    } catch (Exception e) {
      logger.error("Error during BWS template deletion for classId: {}", classId, e);
      throw new BioIdException("BWS template deletion error: " + e.getMessage(), e);
    }
  }

  @Override
  public int getClassCount() throws BioIdException {
    // Note: ClassCount is a REST Management API, not available via gRPC
    logger.warn("getClassCount called but not implemented in gRPC client");
    throw new BioIdException("ClassCount API not available via gRPC - use REST Management API");
  }

  @Override
  public String getTemplateStatus(long classId) throws BioIdException {
    logger.info("BWS gRPC getTemplateStatus called for classId: {}", classId);

    try {
      // Create status request
      Facerecognition.FaceTemplateStatusRequest request = Facerecognition.FaceTemplateStatusRequest
          .newBuilder().setClassId(classId).setDownloadThumbnails(false).build();

      // Call gRPC service
      Facerecognition.FaceTemplateStatus response =
          faceRecognitionStub.withDeadlineAfter(5, TimeUnit.SECONDS).getTemplateStatus(request);

      boolean available = response.getAvailable();
      logger.info("BWS template status for classId {}: available={}", classId, available);

      return available ? "AVAILABLE" : "NOT_AVAILABLE";

    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
        logger.info("BWS template status - template for classId {} does not exist", classId);
        return "NOT_FOUND";
      }

      logger.error("gRPC error during BWS template status for classId: {}", classId, e);
      return "ERROR";
    } catch (Exception e) {
      logger.error("Error during BWS template status for classId: {}", classId, e);
      return "ERROR";
    }
  }

  @Override
  public TemplateStatusDetails getTemplateStatusDetails(long classId, boolean downloadThumbnails)
      throws BioIdException {
    logger.info("BWS gRPC getTemplateStatusDetails called for classId: {}, downloadThumbnails: {}",
        classId, downloadThumbnails);

    try {
      // Create status request
      Facerecognition.FaceTemplateStatusRequest request = Facerecognition.FaceTemplateStatusRequest
          .newBuilder().setClassId(classId).setDownloadThumbnails(downloadThumbnails).build();

      // Call gRPC service
      Facerecognition.FaceTemplateStatus response =
          faceRecognitionStub.withDeadlineAfter(10, TimeUnit.SECONDS).getTemplateStatus(request);

      if (!response.getAvailable()) {
        logger.info("BWS template not available for classId: {}", classId);
        return new TemplateStatusDetails(classId, false, null, java.util.Collections.emptyList(), 0,
            0, 0, java.util.Collections.emptyList());
      }

      // Convert enrolled timestamp
      java.time.Instant enrolled = null;
      if (response.hasEnrolled()) {
        enrolled = java.time.Instant.ofEpochSecond(response.getEnrolled().getSeconds(),
            response.getEnrolled().getNanos());
      }

      // Convert thumbnails if available
      java.util.List<ThumbnailData> thumbnails = new java.util.ArrayList<>();
      if (downloadThumbnails && response.getThumbnailsCount() > 0) {
        for (Facerecognition.FaceTemplateStatus.Thumbnail thumbnail : response
            .getThumbnailsList()) {
          java.time.Instant thumbEnrolled = null;
          if (thumbnail.hasEnrolled()) {
            thumbEnrolled = java.time.Instant.ofEpochSecond(thumbnail.getEnrolled().getSeconds(),
                thumbnail.getEnrolled().getNanos());
          }
          thumbnails.add(new ThumbnailData(thumbEnrolled, thumbnail.getImage().toByteArray()));
        }
        logger.info("Retrieved {} thumbnails for classId: {}", thumbnails.size(), classId);
      }

      TemplateStatusDetails details = new TemplateStatusDetails(classId, true, enrolled,
          new java.util.ArrayList<>(response.getTagsList()), response.getEncoderVersion(),
          response.getFeatureVectors(), response.getThumbnailsStored(), thumbnails);

      logger.info(
          "BWS template status details for classId {}: encoderVersion={}, featureVectors={}, thumbnailsStored={}",
          classId, details.getEncoderVersion(), details.getFeatureVectors(),
          details.getThumbnailsStored());

      return details;

    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
        logger.info("BWS template not found for classId: {}", classId);
        return new TemplateStatusDetails(classId, false, null, java.util.Collections.emptyList(), 0,
            0, 0, java.util.Collections.emptyList());
      }

      logger.error("gRPC error during BWS template status details for classId: {}", classId, e);
      throw new BioIdException(
          "BWS template status details gRPC error: " + e.getStatus().getDescription(), e);
    } catch (Exception e) {
      logger.error("Error during BWS template status details for classId: {}", classId, e);
      throw new BioIdException("BWS template status details error: " + e.getMessage(), e);
    }
  }

  @Override
  public void setTemplateTags(long classId, String[] tags) throws BioIdException {
    logger.info("BWS gRPC setTemplateTags called for classId: {} with tags: {}", classId,
        Arrays.toString(tags));

    try {
      // Create tags request
      Facerecognition.SetTemplateTagsRequest request = Facerecognition.SetTemplateTagsRequest
          .newBuilder().setClassId(classId).addAllTags(Arrays.asList(tags)).build();

      // Call gRPC service
      faceRecognitionStub.withDeadlineAfter(5, TimeUnit.SECONDS).setTemplateTags(request);

      logger.info("BWS template tags set successfully for classId: {}", classId);

    } catch (StatusRuntimeException e) {
      logger.error("gRPC error during BWS template tags for classId: {}", classId, e);
      throw new BioIdException("BWS template tags gRPC error: " + e.getStatus().getDescription(),
          e);
    } catch (Exception e) {
      logger.error("Error during BWS template tags for classId: {}", classId, e);
      throw new BioIdException("BWS template tags error: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean livenessDetection(byte[] imageData) throws BioIdException {
    // Liveness detection is part of BWS basic service, not face recognition
    // For now, return true as we focus on enrollment/verification
    logger.warn("Liveness detection not implemented in production gRPC client yet");
    return true;
  }

  @Override
  public boolean livenessDetectionWithImages(String firstImage, String secondImage, String mode,
      String challengeDirection) throws BioIdException {
    logger.info("BWS gRPC livenessDetectionWithImages called with mode: {}, direction: {}", mode,
        challengeDirection);

    try {
      // Debug: Save images before sending to BWS
      Map<String, Object> metadata = com.bioid.keycloak.client.debug.ImageDebugStorage.createMetadata("liveness-check", "N/A", "liveness-" + mode);
      metadata.put("mode", mode);
      metadata.put("challengeDirection", challengeDirection != null ? challengeDirection : "none");
      debugStorage.saveImages("liveness-check", "liveness-" + mode, 
          new String[]{firstImage, secondImage}, metadata);

      // Remove data URL prefix if present and decode base64
      String base64Image1 = firstImage.contains(",") ? firstImage.split(",")[1] : firstImage;
      String base64Image2 = secondImage.contains(",") ? secondImage.split(",")[1] : secondImage;

      // Validate base64 data
      if (base64Image1 == null || base64Image1.trim().isEmpty()) {
        logger.error("First image is null or empty");
        throw new BioIdException("First image data is invalid");
      }
      if (base64Image2 == null || base64Image2.trim().isEmpty()) {
        logger.error("Second image is null or empty");
        throw new BioIdException("Second image data is invalid");
      }

      logger.debug("First image base64 length: {}", base64Image1.length());
      logger.debug("Second image base64 length: {}", base64Image2.length());

      byte[] imageBytes1 = java.util.Base64.getDecoder().decode(base64Image1);
      byte[] imageBytes2 = java.util.Base64.getDecoder().decode(base64Image2);

      logger.debug("First image bytes length: {}", imageBytes1.length);
      logger.debug("Second image bytes length: {}", imageBytes2.length);

      // Create image data messages
      Bwsmessages.ImageData.Builder image1Builder = Bwsmessages.ImageData.newBuilder()
          .setImage(com.google.protobuf.ByteString.copyFrom(imageBytes1));

      Bwsmessages.ImageData.Builder image2Builder = Bwsmessages.ImageData.newBuilder()
          .setImage(com.google.protobuf.ByteString.copyFrom(imageBytes2));

      // Add challenge direction tag to second image if challenge-response mode
      if ("challenge-response".equalsIgnoreCase(mode) && challengeDirection != null) {
        logger.info("Adding challenge direction tag: {}", challengeDirection);
        image2Builder.addTags(challengeDirection.toUpperCase());
      }

      // Create liveness detection request
      Bws.LivenessDetectionRequest request = Bws.LivenessDetectionRequest.newBuilder()
          .addLiveImages(image1Builder.build()).addLiveImages(image2Builder.build()).build();

      // Call BWS gRPC service
      Bws.LivenessDetectionResponse response = bwsStub
          .withDeadlineAfter(config.getVerificationTimeout().toMillis(), TimeUnit.MILLISECONDS)
          .livenessDetection(request);

      // Check response status
      if (response.getStatus() != Bwsmessages.JobStatus.SUCCEEDED) {
        String errors = response.getErrorsList().stream().map(Bwsmessages.JobError::getMessage)
            .collect(Collectors.joining(", "));
        logger.error("BWS liveness detection failed with status: {} - Errors: {}",
            response.getStatus(), errors);
        return false;
      }

      boolean isLive = response.getLive();
      double score = response.getLivenessScore();
      
      logger.info("BWS liveness detection result: live={}, score={}", isLive, score);

      // Debug: Add result to metadata
      com.bioid.keycloak.client.debug.ImageDebugStorage.addResult(metadata, isLive, 
          String.format("Liveness: %s, Score: %.4f", isLive ? "PASS" : "FAIL", score));

      return isLive;

    } catch (StatusRuntimeException e) {
      logger.error("gRPC error during BWS liveness detection", e);
      throw new BioIdException(
          "BWS liveness detection gRPC error: " + e.getStatus().getDescription(), e);
    } catch (Exception e) {
      logger.error("Error processing liveness detection images", e);
      throw new BioIdException("Liveness detection image processing error: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isHealthy() {
    try {
      ConnectivityState faceState = faceChannel.getState(false);
      ConnectivityState bwsState = bwsChannel.getState(false);
      return (faceState == ConnectivityState.READY || faceState == ConnectivityState.IDLE)
          && (bwsState == ConnectivityState.READY || bwsState == ConnectivityState.IDLE);
    } catch (Exception e) {
      logger.error("Error checking gRPC channel health", e);
      return false;
    }
  }

  @Override
  public String getCurrentEndpoint() {
    return "face" + baseEndpoint;
  }

  @Override
  public Object getConnectionPoolMetrics() {
    // This client doesn't use connection pooling directly
    // Return null to indicate no pool metrics available
    // The connection manager handles pooling at a higher level
    return null;
  }

  @Override
  public boolean verifyFaceWithImageData(long classId, String imageData) throws BioIdException {
    logger.info("BWS gRPC verifyFaceWithImageData called for classId: {}", classId);

    try {
      // Debug: Save image before sending to BWS
      Map<String, Object> metadata = com.bioid.keycloak.client.debug.ImageDebugStorage.createMetadata("classId-" + classId, String.valueOf(classId), "verification");
      debugStorage.saveImage("classId-" + classId, "verification", imageData, metadata);

      // Remove data URL prefix if present
      String base64Image = imageData.contains(",") ? imageData.split(",")[1] : imageData;

      // Decode base64 to bytes
      byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

      // Use the existing verify method
      boolean result = verify(imageBytes, classId);
      
      // Debug: Add result to metadata
      com.bioid.keycloak.client.debug.ImageDebugStorage.addResult(metadata, result, result ? "Verification successful" : "Verification failed");
      
      return result;

    } catch (Exception e) {
      logger.error("Error processing image data for classId: {}", classId, e);
      throw new BioIdException("Image processing error: " + e.getMessage(), e);
    }
  }

  @Override
  public EnrollmentResult enrollFaceWithImageData(long classId, String imageData)
      throws BioIdException {
    // Delegate to multi-image method with single image
    return enrollFaceWithMultipleImages(classId, java.util.Collections.singletonList(imageData));
  }

  @Override
  public EnrollmentResult enrollFaceWithMultipleImages(long classId, java.util.List<String> imageDataList)
      throws BioIdException {
    logger.info("BWS gRPC enrollFaceWithMultipleImages called for classId: {} with {} images", 
        classId, imageDataList.size());

    if (imageDataList == null || imageDataList.isEmpty()) {
      throw new BioIdException("At least one image is required for enrollment");
    }

    try {
      // Build enrollment request with multiple images
      Facerecognition.FaceEnrollmentRequest.Builder requestBuilder = 
          Facerecognition.FaceEnrollmentRequest.newBuilder().setClassId(classId);

      // Process each image
      for (int i = 0; i < imageDataList.size(); i++) {
        String imageData = imageDataList.get(i);
        
        // Debug: Save image before sending to BWS
        Map<String, Object> metadata = com.bioid.keycloak.client.debug.ImageDebugStorage.createMetadata(
            "classId-" + classId, String.valueOf(classId), "enrollment-image-" + (i + 1));
        debugStorage.saveImage("classId-" + classId, "enrollment-image-" + (i + 1), imageData, metadata);

        // Remove data URL prefix if present
        String base64Image = imageData.contains(",") ? imageData.split(",")[1] : imageData;

        // Decode base64 to bytes
        byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Image);

        // Add image to request
        Bwsmessages.ImageData image = Bwsmessages.ImageData.newBuilder()
            .setImage(com.google.protobuf.ByteString.copyFrom(imageBytes)).build();
        requestBuilder.addImages(image);
      }

      // Build final request
      Facerecognition.FaceEnrollmentRequest request = requestBuilder.build();

      // Call gRPC service with extended timeout for multiple images
      long timeoutMillis = config.getEnrollmentTimeout().toMillis();
      // Increase timeout proportionally for multiple images
      if (imageDataList.size() > 1) {
        timeoutMillis = timeoutMillis * imageDataList.size();
      }
      
      logger.info("Using enrollment timeout: {}ms for {} images", timeoutMillis, imageDataList.size());
      
      Facerecognition.FaceEnrollmentResponse response = faceRecognitionStub
          .withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS)
          .enroll(request);

      // Check response status
      if (response.getStatus() != Bwsmessages.JobStatus.SUCCEEDED) {
        String errors = response.getErrorsList().stream().map(Bwsmessages.JobError::getMessage)
            .collect(Collectors.joining(", "));
        throw new BioIdException("BWS enrollment failed: " + errors);
      }

      // Check performed action
      if (response
          .getPerformedAction() == Facerecognition.FaceEnrollmentResponse.EnrollmentAction.ENROLLMENT_FAILED) {
        String errors = response.getErrorsList().stream().map(Bwsmessages.JobError::getMessage)
            .collect(Collectors.joining(", "));
        throw new BioIdException("BWS enrollment failed: " + errors);
      }

      // Get template status
      Facerecognition.FaceTemplateStatus templateStatus = response.getTemplateStatus();

      // Create enrollment result
      List<String> tags = templateStatus.getTagsList();
      String performedAction = response.getPerformedAction().name();

      logger.info("BWS enrollment successful for classId: {} - Action: {}, Enrolled images: {}/{}", 
          classId, performedAction, response.getEnrolledImages(), imageDataList.size());

      return new EnrollmentResult(classId, true, templateStatus.getEncoderVersion(),
          templateStatus.getFeatureVectors(), templateStatus.getThumbnailsStored(), tags,
          performedAction, response.getEnrolledImages());

    } catch (StatusRuntimeException e) {
      logger.error("gRPC error during enrollment for classId: {}", classId, e);
      throw new BioIdException("BWS enrollment gRPC error: " + e.getStatus().getDescription(), e);
    } catch (Exception e) {
      logger.error("Error processing enrollment image data for classId: {}", classId, e);
      throw new BioIdException("Enrollment image processing error: " + e.getMessage(), e);
    }
  }

  @Override
  public void close() {
    logger.info("Shutting down BioID gRPC client");
    try {
      faceChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      bwsChannel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.warn("Interrupted while shutting down gRPC channels", e);
      faceChannel.shutdownNow();
      bwsChannel.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /**
   * gRPC interceptor that adds JWT authentication to all requests.
   */
  private class JwtAuthInterceptor implements ClientInterceptor {
    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
        CallOptions callOptions, Channel next) {

      return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
          next.newCall(method, callOptions)) {
        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
          // Generate JWT token
          String token = generateJwtToken();

          // Add authorization header
          Metadata.Key<String> authKey =
              Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);
          headers.put(authKey, "Bearer " + token);

          super.start(responseListener, headers);
        }
      };
    }
  }

  /**
   * Generates a JWT token for BWS authentication.
   */
  private String generateJwtToken() {
    try {
      long expireMinutes = config.getJwtExpireMinutes();
      long expirationTime = System.currentTimeMillis() + (expireMinutes * 60 * 1000);

      // Create JWT claims
      java.util.Map<String, Object> claims = new java.util.HashMap<>();
      claims.put("iss", clientId);
      claims.put("sub", clientId);
      claims.put("aud", "https://bws.bioid.com");
      claims.put("iat", System.currentTimeMillis() / 1000);
      claims.put("exp", expirationTime / 1000);
      claims.put("jti", java.util.UUID.randomUUID().toString());

      // Decode the Base64-encoded secret key
      byte[] keyBytes = java.util.Base64.getDecoder().decode(secretKey);

      // Build JWT token using JJWT library
      return io.jsonwebtoken.Jwts.builder().claims(claims)
          .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes)).compact();

    } catch (Exception e) {
      logger.error("Error generating JWT token for BWS authentication", e);
      throw new RuntimeException("JWT token generation failed: " + e.getMessage(), e);
    }
  }
}
