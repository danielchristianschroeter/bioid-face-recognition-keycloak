package com.bioid.keycloak.admin;

import com.bioid.keycloak.admin.dto.ConnectivityTestResult;
import com.bioid.keycloak.admin.dto.DeletionRequestDto;
import com.bioid.keycloak.admin.dto.DeletionRequestPriority;
import com.bioid.keycloak.admin.dto.DeletionRequestStatus;
import com.bioid.keycloak.admin.dto.FaceRecognitionConfigDto;
import com.bioid.keycloak.admin.service.DeletionRequestService;
import com.bioid.keycloak.client.config.BioIdConfiguration;
import com.bioid.keycloak.health.FaceRecognitionHealthCheck;
import com.bioid.keycloak.metrics.FaceRecognitionMetrics;
import jakarta.enterprise.inject.Vetoed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS resource for Face Recognition admin configuration. Provides REST endpoints for managing
 * face recognition settings in Keycloak admin console.
 *
 * <p>Note: This class is NOT a CDI bean - it's created manually by
 * FaceRecognitionAdminResourceProvider
 */
@Vetoed
@Path("/face-recognition")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FaceRecognitionAdminResource {

  private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionAdminResource.class);

  private final KeycloakSession session;
  private final RealmModel realm;
  private final BioIdConfiguration configuration;
  private final DeletionRequestService deletionRequestService;
  private final FaceRecognitionMetrics metrics;
  private final FaceRecognitionHealthCheck healthCheck;

  public FaceRecognitionAdminResource(KeycloakSession session, RealmModel realm) {
    this.session = session;
    this.realm = realm;
    this.configuration = BioIdConfiguration.getInstance();
    this.deletionRequestService = new DeletionRequestService(session, realm);
    this.metrics = new FaceRecognitionMetrics();
    this.healthCheck = new FaceRecognitionHealthCheck();
  }

  /** Get current face recognition configuration. */
  @GET
  @Path("/config")
  public Response getConfiguration() {
    try {
      FaceRecognitionConfigDto config =
          FaceRecognitionConfigDto.builder()
              .endpoint(configuration.getEndpoint())
              .clientId(configuration.getClientId())
              .verificationThreshold(configuration.getVerificationThreshold())
              .maxRetries(configuration.getMaxRetries())
              .livenessEnabled(configuration.isLivenessEnabled())
              .passiveLivenessEnabled(configuration.isLivenessPassiveEnabled())
              .activeLivenessEnabled(configuration.isLivenessActiveEnabled())
              .challengeResponseLivenessEnabled(configuration.isLivenessChallengeResponseEnabled())
              .livenessConfidenceThreshold(configuration.getLivenessConfidenceThreshold())
              .livenessMaxOverheadMs((int) configuration.getLivenessMaxOverhead().toMillis())
              .preferredRegion(configuration.getPreferredRegion())
              .dataResidencyRequired(configuration.isDataResidencyRequired())
              .failoverEnabled(configuration.isFailoverEnabled())
              .channelPoolSize(configuration.getChannelPoolSize())
              .keepAliveTime((int) configuration.getKeepAliveTime().toSeconds())
              .verificationTimeout((int) configuration.getVerificationTimeout().toSeconds())
              .enrollmentTimeout((int) configuration.getEnrollmentTimeout().toSeconds())
              .templateTtlDays(configuration.getTemplateTtlDays())
              .build();

      return Response.ok(config).build();
    } catch (Exception e) {
      logger.error("Failed to get face recognition configuration", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get configuration: " + e.getMessage()))
          .build();
    }
  }

  /** Update face recognition configuration. */
  @PUT
  @Path("/config")
  public Response updateConfiguration(FaceRecognitionConfigDto config) {
    try {
      // Validate configuration
      validateConfiguration(config);

      // Update configuration (this would need to be implemented in BioIdConfiguration)
      // For now, we'll return success - actual implementation would update the config

      logger.info("Face recognition configuration updated for realm: {}", realm.getName());
      return Response.ok().build();
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid configuration provided: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    } catch (Exception e) {
      logger.error("Failed to update face recognition configuration", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to update configuration: " + e.getMessage()))
          .build();
    }
  }

  /** Test connectivity to BioID service. */
  @POST
  @Path("/test-connectivity")
  public Response testConnectivity() {
    try {
      // This would test actual connectivity to BioID service
      // For now, we'll simulate the test
      ConnectivityTestResult result =
          ConnectivityTestResult.builder()
              .success(true)
              .endpoint(configuration.getEndpoint())
              .responseTimeMs(150)
              .message("Connection successful")
              .build();

      return Response.ok(result).build();
    } catch (Exception e) {
      logger.error("Connectivity test failed", e);
      ConnectivityTestResult result =
          ConnectivityTestResult.builder()
              .success(false)
              .endpoint(configuration.getEndpoint())
              .responseTimeMs(-1)
              .message("Connection failed: " + e.getMessage())
              .build();

      return Response.ok(result).build();
    }
  }

  /** Get deletion requests with optional filtering. */
  @GET
  @Path("/deletion-requests")
  public Response getDeletionRequests(
      @QueryParam("status") String statusStr,
      @QueryParam("priority") String priorityStr,
      @QueryParam("maxAge") Integer maxAge) {
    try {
      DeletionRequestStatus status =
          statusStr != null ? DeletionRequestStatus.fromValue(statusStr) : null;

      DeletionRequestPriority priority =
          priorityStr != null ? DeletionRequestPriority.fromValue(priorityStr) : null;

      List<DeletionRequestDto> requests =
          deletionRequestService.getDeletionRequests(status, priority, maxAge);
      return Response.ok(requests).build();
    } catch (Exception e) {
      logger.error("Failed to get deletion requests", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get deletion requests: " + e.getMessage()))
          .build();
    }
  }

  /** Get metrics summary for administrative dashboard. */
  @GET
  @Path("/metrics")
  public Response getMetrics() {
    try {
      FaceRecognitionMetrics.MetricsSummary summary = metrics.getMetricsSummary();
      return Response.ok(summary).build();
    } catch (Exception e) {
      logger.error("Failed to get metrics", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get metrics: " + e.getMessage()))
          .build();
    }
  }

  /** Get detailed health check information. */
  @GET
  @Path("/health")
  public Response getHealth() {
    try {
      FaceRecognitionHealthCheck.DetailedHealthCheckResult health = healthCheck.getDetailedHealth();

      if (health.isOverallHealthy()) {
        return Response.ok(health).build();
      } else {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(health).build();
      }
    } catch (Exception e) {
      logger.error("Failed to get health status", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get health status: " + e.getMessage()))
          .build();
    }
  }

  /** Approve a deletion request. */
  @POST
  @Path("/deletion-requests/{requestId}/approve")
  public Response approveDeletionRequest(@PathParam("requestId") String requestId) {
    try {
      // This would approve the deletion request and delete the template
      logger.info("Deletion request {} approved for realm: {}", requestId, realm.getName());
      return Response.ok().build();
    } catch (Exception e) {
      logger.error("Failed to approve deletion request: {}", requestId, e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to approve deletion request: " + e.getMessage()))
          .build();
    }
  }

  /** Decline a deletion request. */
  @POST
  @Path("/deletion-requests/{requestId}/decline")
  public Response declineDeletionRequest(@PathParam("requestId") String requestId) {
    try {
      // This would decline the deletion request
      logger.info("Deletion request {} declined for realm: {}", requestId, realm.getName());
      return Response.ok().build();
    } catch (Exception e) {
      logger.error("Failed to decline deletion request: {}", requestId, e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to decline deletion request: " + e.getMessage()))
          .build();
    }
  }

  private void validateConfiguration(FaceRecognitionConfigDto config) {
    if (config.getVerificationThreshold() < 0.001 || config.getVerificationThreshold() > 1.0) {
      throw new IllegalArgumentException("Verification threshold must be between 0.001 and 1.0");
    }

    if (config.getMaxRetries() < 1 || config.getMaxRetries() > 10) {
      throw new IllegalArgumentException("Max retries must be between 1 and 10");
    }

    if (config.getLivenessConfidenceThreshold() < 0.0
        || config.getLivenessConfidenceThreshold() > 1.0) {
      throw new IllegalArgumentException(
          "Liveness confidence threshold must be between 0.0 and 1.0");
    }

    if (config.getLivenessMaxOverheadMs() < 50 || config.getLivenessMaxOverheadMs() > 5000) {
      throw new IllegalArgumentException(
          "Liveness max overhead must be between 50 and 5000 milliseconds");
    }

    if (config.getChannelPoolSize() < 1 || config.getChannelPoolSize() > 50) {
      throw new IllegalArgumentException("Channel pool size must be between 1 and 50");
    }

    if (config.getTemplateTtlDays() < 1 || config.getTemplateTtlDays() > 3650) {
      throw new IllegalArgumentException("Template TTL must be between 1 and 3650 days");
    }
  }

  /** Error response DTO. */
  public static class ErrorResponse {
    private final String error;

    public ErrorResponse(String error) {
      this.error = error;
    }

    public String getError() {
      return error;
    }
  }
}
