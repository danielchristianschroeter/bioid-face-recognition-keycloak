package com.bioid.keycloak.client;

import com.bioid.keycloak.client.auth.BioIdJwtTokenProvider;
import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.connection.BioIdConnectionManager;
import com.bioid.keycloak.client.exception.BioIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * High-performance, thread-safe gRPC client for BioID BWS 3 face recognition service.
 * 
 * NOTE: This implementation is currently a stub due to protobuf dependency issues. The actual gRPC
 * implementation will be completed when protobuf classes are available.
 */
public class BioIdGrpcClient implements BioIdClient {

  private static final Logger logger = LoggerFactory.getLogger(BioIdGrpcClient.class);

  private final BioIdClientConfig config;
  private final BioIdJwtTokenProvider tokenProvider;
  private final BioIdConnectionManager connectionManager;

  public BioIdGrpcClient(BioIdClientConfig config, BioIdJwtTokenProvider tokenProvider,
      BioIdConnectionManager connectionManager) {
    this.config = config;
    this.tokenProvider = tokenProvider;
    this.connectionManager = connectionManager;
  }

  @Override
  public void enroll(byte[] imageData, long classId) throws BioIdException {
    logger.info("Stub: enroll called for classId: {}", classId);
    // TODO: Implement actual gRPC call when protobuf classes are available
  }

  @Override
  public boolean verify(byte[] imageData, long classId) throws BioIdException {
    logger.info("Stub: verify called for classId: {}", classId);
    // TODO: Implement actual gRPC call when protobuf classes are available
    return true;
  }

  @Override
  public void deleteTemplate(long classId) throws BioIdException {
    logger.info("Stub: deleteTemplate called for classId: {}", classId);
    // TODO: Implement actual gRPC call when protobuf classes are available
  }

  @Override
  public String getTemplateStatus(long classId) throws BioIdException {
    logger.info("Stub: getTemplateStatus called for classId: {}", classId);
    // TODO: Implement actual gRPC call when protobuf classes are available
    return "AVAILABLE";
  }

  @Override
  public void setTemplateTags(long classId, String[] tags) throws BioIdException {
    logger.info("Stub: setTemplateTags called for classId: {} with tags: {}", classId,
        Arrays.toString(tags));
    // TODO: Implement actual gRPC call when protobuf classes are available
  }

  @Override
  public boolean livenessDetection(byte[] imageData) throws BioIdException {
    logger.info("Stub: livenessDetection called");
    // TODO: Implement actual gRPC call when protobuf classes are available
    return true;
  }

  @Override
  public boolean isHealthy() {
    logger.debug("Stub: isHealthy called");
    // TODO: Implement actual health check when protobuf classes are available
    return true;
  }

  @Override
  public String getCurrentEndpoint() {
    return config.endpoint();
  }

  @Override
  public boolean verifyFaceWithImageData(long classId, String imageData) throws BioIdException {
    logger.info("Stub: verifyFaceWithImageData called for classId: {}", classId);
    // TODO: Implement actual gRPC call when protobuf classes are available
    return true;
  }

  @Override
  public EnrollmentResult enrollFaceWithImageData(long classId, String imageData)
      throws BioIdException {
    logger.info("Stub: enrollFaceWithImageData called for classId: {}", classId);
    // TODO: Implement actual gRPC call when protobuf classes are available
    return new EnrollmentResult(classId, true, 3, 1, 1, Arrays.asList("enrolled"), "ENROLLED", 1);
  }

  @Override
  public void close() {
    logger.info("Stub: close called");
    // TODO: Implement actual cleanup when protobuf classes are available
  }
}
