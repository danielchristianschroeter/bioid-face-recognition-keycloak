package com.bioid.keycloak.client;

import com.bioid.keycloak.client.auth.BioIdJwtTokenProvider;
import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.connection.BioIdConnectionManager;
import com.bioid.keycloak.client.error.BioIdErrorMapper;
import com.bioid.keycloak.client.exception.*;
import com.bioid.services.*;
import io.grpc.*;
import io.grpc.stub.MetadataUtils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-performance, thread-safe gRPC client for BioID BWS 3 face recognition service.
 *
 * <p>Features: - Automatic JWT authentication with token renewal - Connection pooling and circuit
 * breaker pattern - Comprehensive error handling and retry logic - Metrics collection for
 * observability - Type-safe API with modern Java patterns
 *
 * <p>Security: - TLS encryption for all communications - Secure JWT token handling - No sensitive
 * data logging
 *
 * <p>Performance: - Connection pooling for optimal throughput - Configurable timeouts per operation
 * type - Efficient protobuf serialization
 */
public class BioIdGrpcClient implements BioIdClient {

  private static final Logger logger = LoggerFactory.getLogger(BioIdGrpcClient.class);

  private final BioIdClientConfig config;
  private final BioIdJwtTokenProvider tokenProvider;
  private final BioIdConnectionManager connectionManager;

  private final MeterRegistry meterRegistry;

  // Metrics
  private final Counter enrollmentAttempts;
  private final Counter enrollmentSuccesses;
  private final Counter enrollmentFailures;
  private final Counter verificationAttempts;
  private final Counter verificationSuccesses;
  private final Counter verificationFailures;
  private final Timer enrollmentLatency;
  private final Timer verificationLatency;

  /**
   * Creates a new BioID gRPC client with the specified configuration.
   *
   * @param config client configuration
   * @param tokenProvider JWT token provider for authentication
   * @param connectionManager connection manager for gRPC channels
   * @param meterRegistry metrics registry for observability
   */
  public BioIdGrpcClient(
      BioIdClientConfig config,
      BioIdJwtTokenProvider tokenProvider,
      BioIdConnectionManager connectionManager,
      MeterRegistry meterRegistry) {
    this.config = config;
    this.tokenProvider = tokenProvider;
    this.connectionManager = connectionManager;
    this.meterRegistry = meterRegistry;

    // Initialize metrics
    this.enrollmentAttempts =
        Counter.builder("bioid.enrollment.attempts")
            .description("Total enrollment attempts")
            .register(meterRegistry);
    this.enrollmentSuccesses =
        Counter.builder("bioid.enrollment.successes")
            .description("Successful enrollments")
            .register(meterRegistry);
    this.enrollmentFailures =
        Counter.builder("bioid.enrollment.failures")
            .description("Failed enrollments")
            .register(meterRegistry);
    this.verificationAttempts =
        Counter.builder("bioid.verification.attempts")
            .description("Total verification attempts")
            .register(meterRegistry);
    this.verificationSuccesses =
        Counter.builder("bioid.verification.successes")
            .description("Successful verifications")
            .register(meterRegistry);
    this.verificationFailures =
        Counter.builder("bioid.verification.failures")
            .description("Failed verifications")
            .register(meterRegistry);
    this.enrollmentLatency =
        Timer.builder("bioid.enrollment.latency")
            .description("Enrollment operation latency")
            .register(meterRegistry);
    this.verificationLatency =
        Timer.builder("bioid.verification.latency")
            .description("Verification operation latency")
            .register(meterRegistry);
  }

  /**
   * Enrolls face biometric template for a user.
   *
   * @param request enrollment request with class ID and image data
   * @return enrollment response with result and metadata
   * @throws BioIdException if enrollment fails
   */
  @Override
  public Facerecognition.FaceEnrollmentResponse enroll(
      Facerecognition.FaceEnrollmentRequest request) throws BioIdException {
    enrollmentAttempts.increment();
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      logger.debug("Starting enrollment for class ID: {}", request.getClassId());

      Facerecognition.FaceEnrollmentResponse response =
          executeWithRetry(
              stub ->
                  stub.withDeadlineAfter(
                          config.enrollmentTimeout().toMillis(), TimeUnit.MILLISECONDS)
                      .enroll(request),
              "enroll");

      sample.stop(enrollmentLatency);

      if (response.getStatus() == Bwsmessages.JobStatus.SUCCEEDED) {
        enrollmentSuccesses.increment();
        connectionManager.recordSuccess();
        logger.debug(
            "Enrollment successful for class ID: {}, action: {}",
            request.getClassId(),
            response.getPerformedAction());
      } else {
        enrollmentFailures.increment();
        logger.warn(
            "Enrollment failed for class ID: {}, errors: {}",
            request.getClassId(),
            response.getErrorsList());
        // Create exception from BWS JobError list
        String errorMessage =
            response.getErrorsList().isEmpty()
                ? "Enrollment failed"
                : response.getErrorsList().get(0).getMessage();
        throw new BioIdServiceException(errorMessage, "ENROLLMENT_FAILED");
      }

      return response;

    } catch (Exception e) {
      sample.stop(enrollmentLatency);
      enrollmentFailures.increment();
      connectionManager.recordFailure();
      throw handleException(e, "enrollment");
    }
  }

  /**
   * Verifies face against enrolled biometric template.
   *
   * @param request verification request with class ID and image data
   * @return verification response with result
   * @throws BioIdException if verification fails
   */
  @Override
  public Facerecognition.FaceVerificationResponse verify(
      Facerecognition.FaceVerificationRequest request) throws BioIdException {
    verificationAttempts.increment();
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      logger.debug("Starting verification for class ID: {}", request.getClassId());

      Facerecognition.FaceVerificationResponse response =
          executeWithRetry(
              stub ->
                  stub.withDeadlineAfter(
                          config.verificationTimeout().toMillis(), TimeUnit.MILLISECONDS)
                      .verify(request),
              "verify");

      sample.stop(verificationLatency);

      if (response.getVerified()) {
        verificationSuccesses.increment();
        logger.debug(
            "Verification successful for class ID: {}, score: {}",
            request.getClassId(),
            response.getScore());
      } else {
        verificationFailures.increment();
        logger.debug(
            "Verification failed for class ID: {}, score: {}",
            request.getClassId(),
            response.getScore());
      }

      connectionManager.recordSuccess();
      return response;

    } catch (Exception e) {
      sample.stop(verificationLatency);
      verificationFailures.increment();
      connectionManager.recordFailure();
      throw handleException(e, "verification");
    }
  }

  /**
   * Deletes a biometric template.
   *
   * @param request delete request with class ID
   * @return delete response
   * @throws BioIdException if deletion fails
   */
  @Override
  public Facerecognition.DeleteTemplateResponse deleteTemplate(
      Facerecognition.DeleteTemplateRequest request) throws BioIdException {
    try {
      logger.debug("Deleting template for class ID: {}", request.getClassId());

      Facerecognition.DeleteTemplateResponse response =
          executeWithRetry(
              stub ->
                  stub.withDeadlineAfter(config.requestTimeout().toMillis(), TimeUnit.MILLISECONDS)
                      .deleteTemplate(request),
              "deleteTemplate");

      connectionManager.recordSuccess();
      logger.debug("Template deleted successfully for class ID: {}", request.getClassId());
      return response;

    } catch (Exception e) {
      connectionManager.recordFailure();
      throw handleException(e, "deleteTemplate");
    }
  }

  /**
   * Gets template status information.
   *
   * @param request status request with class ID
   * @return template status
   * @throws BioIdException if status query fails
   */
  @Override
  public Facerecognition.FaceTemplateStatus getTemplateStatus(
      Facerecognition.FaceTemplateStatusRequest request) throws BioIdException {
    try {
      logger.debug("Getting template status for class ID: {}", request.getClassId());

      Facerecognition.FaceTemplateStatus response =
          executeWithRetry(
              stub ->
                  stub.withDeadlineAfter(config.requestTimeout().toMillis(), TimeUnit.MILLISECONDS)
                      .getTemplateStatus(request),
              "getTemplateStatus");

      connectionManager.recordSuccess();
      logger.debug(
          "Template status retrieved for class ID: {}, available: {}",
          request.getClassId(),
          response.getAvailable());
      return response;

    } catch (Exception e) {
      connectionManager.recordFailure();
      throw handleException(e, "getTemplateStatus");
    }
  }

  /**
   * Sets template tags.
   *
   * @param request set tags request with class ID and tags
   * @return set tags response
   * @throws BioIdException if setting tags fails
   */
  @Override
  public Facerecognition.SetTemplateTagsResponse setTemplateTags(
      Facerecognition.SetTemplateTagsRequest request) throws BioIdException {
    try {
      logger.debug(
          "Setting template tags for class ID: {}, tags: {}",
          request.getClassId(),
          request.getTagsList());

      Facerecognition.SetTemplateTagsResponse response =
          executeWithRetry(
              stub ->
                  stub.withDeadlineAfter(config.requestTimeout().toMillis(), TimeUnit.MILLISECONDS)
                      .setTemplateTags(request),
              "setTemplateTags");

      // SetTemplateTagsResponse is empty in BWS - success is indicated by no
      // exception
      connectionManager.recordSuccess();
      logger.debug("Template tags set successfully for class ID: {}", request.getClassId());
      return response;

    } catch (Exception e) {
      connectionManager.recordFailure();
      throw handleException(e, "setTemplateTags");
    }
  }

  /**
   * Checks if the client is healthy and can communicate with the service.
   *
   * @return true if healthy, false otherwise
   */
  @Override
  public boolean isHealthy() {
    return connectionManager.isHealthy();
  }

  /**
   * Gets the current active endpoint.
   *
   * @return current endpoint URL
   */
  @Override
  public String getCurrentEndpoint() {
    return connectionManager.getCurrentEndpoint();
  }

  /**
   * Convenience method for face verification using simple parameters.
   *
   * @param classId the class ID for the user
   * @param imageData base64-encoded image data (without data URL prefix)
   * @return true if verification succeeds, false otherwise
   * @throws BioIdException if verification fails
   */
  @Override
  public boolean verifyFaceWithImageData(long classId, String imageData) throws BioIdException {
    try {
      // Decode base64 image data
      byte[] imageBytes = java.util.Base64.getDecoder().decode(imageData);
      // Create protobuf objects
      Bwsmessages.ImageData grpcImageData =
          Bwsmessages.ImageData.newBuilder()
              .setImage(com.google.protobuf.ByteString.copyFrom(imageBytes))
              .build();
      Facerecognition.FaceVerificationRequest request =
          Facerecognition.FaceVerificationRequest.newBuilder()
              .setClassId(classId)
              .setImage(grpcImageData)
              .build();
      Facerecognition.FaceVerificationResponse response = verify(request);
      return response.getVerified();
    } catch (Exception e) {
      logger.error("Error in face verification with image data for class ID: {}", classId, e);
      throw handleException(e, "verifyFaceWithImageData");
    }
  }

  /**
   * Convenience method for deleting a face template.
   *
   * @param classId the class ID for the user
   * @throws BioIdException if deletion fails
   */
  @Override
  public void deleteTemplate(long classId) throws BioIdException {
    try {
      Facerecognition.DeleteTemplateRequest deleteRequest =
          Facerecognition.DeleteTemplateRequest.newBuilder().setClassId(classId).build();
      deleteTemplate(deleteRequest);
    } catch (Exception e) {
      logger.error("Error deleting template for class ID: {}", classId, e);
      throw handleException(e, "deleteTemplate");
    }
  }

  /**
   * Convenience method for face enrollment using simple parameters.
   *
   * @param classId the class ID for the user
   * @param imageData base64-encoded image data (without data URL prefix)
   * @return enrollment result with template status and metadata
   * @throws BioIdException if enrollment fails
   */
  @Override
  public BioIdClient.EnrollmentResult enrollFaceWithImageData(long classId, String imageData)
      throws BioIdException {
    try {
      // Decode base64 image data
      byte[] imageBytes = java.util.Base64.getDecoder().decode(imageData);
      // Create protobuf objects
      Bwsmessages.ImageData grpcImageData =
          Bwsmessages.ImageData.newBuilder()
              .setImage(com.google.protobuf.ByteString.copyFrom(imageBytes))
              .build();
      Facerecognition.FaceEnrollmentRequest request =
          Facerecognition.FaceEnrollmentRequest.newBuilder()
              .setClassId(classId)
              .addImages(grpcImageData)
              .build();
      Facerecognition.FaceEnrollmentResponse response = enroll(request);

      // Convert protobuf response to simple result
      Facerecognition.FaceTemplateStatus status = response.getTemplateStatus();
      return new BioIdClient.EnrollmentResult(
          status.getClassId(),
          status.getAvailable(),
          status.getEncoderVersion(),
          status.getFeatureVectors(),
          status.getThumbnailsStored(),
          java.util.List.copyOf(status.getTagsList()),
          response.getPerformedAction().name(),
          response.getEnrolledImages());
    } catch (Exception e) {
      logger.error("Error in face enrollment with image data for class ID: {}", classId, e);
      throw handleException(e, "enrollFaceWithImageData");
    }
  }

  @Override
  public void close() {
    if (connectionManager != null) {
      connectionManager.close();
    }
  }

  private <T> T executeWithRetry(GrpcOperation<T> operation, String operationName)
      throws BioIdException {
    int attempts = 0;
    Duration delay = config.initialRetryDelay();

    while (attempts <= config.maxRetryAttempts()) {
      try {
        ManagedChannel channel = connectionManager.getChannel();
        FaceRecognitionGrpc.FaceRecognitionBlockingStub stub = createAuthenticatedStub(channel);

        return operation.execute(stub);

      } catch (StatusRuntimeException e) {
        attempts++;
        if (attempts > config.maxRetryAttempts() || !isRetryableError(e)) {
          throw handleException(e, operationName);
        }

        logger.warn(
            "Retryable error in {} (attempt {}/{}): {}. Retrying in {}ms",
            operationName,
            attempts,
            config.maxRetryAttempts(),
            e.getMessage(),
            delay.toMillis());

        try {
          Thread.sleep(delay.toMillis());
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new BioIdServiceException("Operation interrupted", ie);
        }

        delay = Duration.ofMillis((long) (delay.toMillis() * config.retryBackoffMultiplier()));
      }
    }

    throw new BioIdServiceException("Max retry attempts exceeded for " + operationName);
  }

  private FaceRecognitionGrpc.FaceRecognitionBlockingStub createAuthenticatedStub(
      ManagedChannel channel) {
    String token = tokenProvider.getToken();
    Metadata metadata = new Metadata();
    metadata.put(
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);

    return FaceRecognitionGrpc.newBlockingStub(channel)
        .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
  }

  private boolean isRetryableError(StatusRuntimeException e) {
    Status.Code code = e.getStatus().getCode();
    return code == Status.Code.UNAVAILABLE
        || code == Status.Code.DEADLINE_EXCEEDED
        || code == Status.Code.RESOURCE_EXHAUSTED
        || code == Status.Code.INTERNAL;
  }

  private BioIdException handleException(Exception e, String operation) {
    if (e instanceof StatusRuntimeException) {
      return BioIdErrorMapper.mapGrpcException((StatusRuntimeException) e, operation);
    } else {
      return BioIdErrorMapper.mapGeneralException(e, operation);
    }
  }

  /** Functional interface for gRPC operations with retry support. */
  @FunctionalInterface
  private interface GrpcOperation<T> {
    T execute(FaceRecognitionGrpc.FaceRecognitionBlockingStub stub) throws StatusRuntimeException;
  }
}
