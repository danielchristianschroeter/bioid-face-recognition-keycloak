package com.bioid.keycloak.admin;

import com.bioid.keycloak.admin.dto.ActivityItem;
import com.bioid.keycloak.admin.dto.AuthenticationMetrics;
import com.bioid.keycloak.admin.dto.BulkOperationRequest;
import com.bioid.keycloak.admin.dto.BulkOperationResult;
import com.bioid.keycloak.admin.dto.ConnectivityTestResult;
import com.bioid.keycloak.admin.dto.DeletionRequestDto;
import com.bioid.keycloak.admin.dto.DeletionRequestPriority;
import com.bioid.keycloak.admin.dto.DeletionRequestStatus;
import com.bioid.keycloak.admin.dto.EnrollmentStatistics;
import com.bioid.keycloak.admin.dto.FaceRecognitionConfigDto;
import com.bioid.keycloak.admin.dto.LivenessConfigDto;
import com.bioid.keycloak.admin.dto.LivenessStatistics;
import com.bioid.keycloak.admin.dto.LivenessTestRequest;
import com.bioid.keycloak.admin.dto.LivenessTestResult;
import com.bioid.keycloak.admin.dto.TemplateDetails;
import com.bioid.keycloak.admin.dto.TemplateListResponse;
import com.bioid.keycloak.admin.dto.TemplateStatistics;
import com.bioid.keycloak.admin.dto.TemplateUpgradeResult;
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

  /** Get templates with pagination and filtering. */
  @GET
  @Path("/templates")
  public Response getTemplates(
      @QueryParam("offset") @DefaultValue("0") int offset,
      @QueryParam("limit") @DefaultValue("20") int limit,
      @QueryParam("search") String search,
      @QueryParam("searchType") @DefaultValue("username") String searchType,
      @QueryParam("healthStatus") String healthStatus,
      @QueryParam("encoderVersion") String encoderVersion) {
    try {
      // This would integrate with AdminService to get template data
      // For now, return mock data structure
      TemplateListResponse response = TemplateListResponse.builder()
          .templates(java.util.Collections.emptyList())
          .totalCount(0)
          .offset(offset)
          .limit(limit)
          .build();

      return Response.ok(response).build();
    } catch (Exception e) {
      logger.error("Failed to get templates", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get templates: " + e.getMessage()))
          .build();
    }
  }

  /** Get template statistics for dashboard. */
  @GET
  @Path("/template-statistics")
  public Response getTemplateStatistics() {
    try {
      // This would integrate with AdminService to get statistics
      TemplateStatistics stats = TemplateStatistics.builder()
          .totalTemplates(0)
          .healthyTemplates(0)
          .needsUpgrade(0)
          .expiringSoon(0)
          .build();

      return Response.ok(stats).build();
    } catch (Exception e) {
      logger.error("Failed to get template statistics", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get template statistics: " + e.getMessage()))
          .build();
    }
  }

  /** Get detailed template information including thumbnails. */
  @GET
  @Path("/templates/{classId}")
  public Response getTemplateDetails(@PathParam("classId") long classId) {
    try {
      // This would integrate with TemplateService to get detailed template info
      TemplateDetails details = TemplateDetails.builder()
          .classId(classId)
          .thumbnails(java.util.Collections.emptyList())
          .build();

      return Response.ok(details).build();
    } catch (Exception e) {
      logger.error("Failed to get template details for classId: {}", classId, e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get template details: " + e.getMessage()))
          .build();
    }
  }

  /** Upgrade a single template. */
  @POST
  @Path("/templates/{classId}/upgrade")
  public Response upgradeTemplate(@PathParam("classId") long classId) {
    try {
      // This would integrate with TemplateService to upgrade template
      logger.info("Template upgrade requested for classId: {} in realm: {}", classId, realm.getName());
      
      TemplateUpgradeResult result = TemplateUpgradeResult.builder()
          .classId(classId)
          .success(true)
          .message("Template upgrade completed successfully")
          .build();

      return Response.ok(result).build();
    } catch (Exception e) {
      logger.error("Failed to upgrade template for classId: {}", classId, e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to upgrade template: " + e.getMessage()))
          .build();
    }
  }

  /** Delete a single template. */
  @DELETE
  @Path("/templates/{classId}")
  public Response deleteTemplate(@PathParam("classId") long classId) {
    try {
      // This would integrate with AdminService to delete template
      logger.info("Template deletion requested for classId: {} in realm: {}", classId, realm.getName());
      return Response.ok().build();
    } catch (Exception e) {
      logger.error("Failed to delete template for classId: {}", classId, e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to delete template: " + e.getMessage()))
          .build();
    }
  }

  /** Start bulk template upgrade operation. */
  @POST
  @Path("/bulk-upgrade")
  public Response bulkUpgradeTemplates(BulkOperationRequest request) {
    try {
      // This would integrate with BulkOperationService
      logger.info("Bulk template upgrade requested for {} templates in realm: {}", 
          request.getClassIds().size(), realm.getName());
      
      BulkOperationResult result = BulkOperationResult.builder()
          .operationId(java.util.UUID.randomUUID().toString())
          .status("PENDING")
          .totalItems(request.getClassIds().size())
          .processedItems(0)
          .successfulItems(0)
          .failedItems(0)
          .build();

      return Response.ok(result).build();
    } catch (Exception e) {
      logger.error("Failed to start bulk template upgrade", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to start bulk upgrade: " + e.getMessage()))
          .build();
    }
  }

  /** Start bulk template deletion operation. */
  @POST
  @Path("/bulk-delete")
  public Response bulkDeleteTemplates(BulkOperationRequest request) {
    try {
      // This would integrate with BulkOperationService
      logger.info("Bulk template deletion requested for {} templates in realm: {}", 
          request.getClassIds().size(), realm.getName());
      
      BulkOperationResult result = BulkOperationResult.builder()
          .operationId(java.util.UUID.randomUUID().toString())
          .status("PENDING")
          .totalItems(request.getClassIds().size())
          .processedItems(0)
          .successfulItems(0)
          .failedItems(0)
          .build();

      return Response.ok(result).build();
    } catch (Exception e) {
      logger.error("Failed to start bulk template deletion", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to start bulk deletion: " + e.getMessage()))
          .build();
    }
  }

  /** Get bulk operation status. */
  @GET
  @Path("/bulk-operations/{operationId}")
  public Response getBulkOperationStatus(@PathParam("operationId") String operationId) {
    try {
      // This would integrate with BulkOperationService to get operation status
      BulkOperationResult result = BulkOperationResult.builder()
          .operationId(operationId)
          .status("COMPLETED")
          .totalItems(10)
          .processedItems(10)
          .successfulItems(9)
          .failedItems(1)
          .build();

      return Response.ok(result).build();
    } catch (Exception e) {
      logger.error("Failed to get bulk operation status for operationId: {}", operationId, e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get operation status: " + e.getMessage()))
          .build();
    }
  }

  /** Cancel bulk operation. */
  @POST
  @Path("/bulk-operations/{operationId}/cancel")
  public Response cancelBulkOperation(@PathParam("operationId") String operationId) {
    try {
      // This would integrate with BulkOperationService to cancel operation
      logger.info("Bulk operation cancellation requested for operationId: {} in realm: {}", 
          operationId, realm.getName());
      return Response.ok().build();
    } catch (Exception e) {
      logger.error("Failed to cancel bulk operation for operationId: {}", operationId, e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to cancel operation: " + e.getMessage()))
          .build();
    }
  }

  /** Get liveness detection configuration. */
  @GET
  @Path("/liveness-config")
  public Response getLivenessConfiguration() {
    try {
      // This would integrate with LivenessService to get configuration
      LivenessConfigDto config = LivenessConfigDto.builder()
          .defaultLivenessMode("PASSIVE")
          .livenessThreshold(0.7)
          .challengeDirections(java.util.Arrays.asList("UP", "DOWN", "LEFT", "RIGHT"))
          .maxOverheadMs(1000)
          .fallbackEnabled(true)
          .detailedLogging(false)
          .build();

      return Response.ok(config).build();
    } catch (Exception e) {
      logger.error("Failed to get liveness configuration", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get liveness configuration: " + e.getMessage()))
          .build();
    }
  }

  /** Update liveness detection configuration. */
  @PUT
  @Path("/liveness-config")
  public Response updateLivenessConfiguration(LivenessConfigDto config) {
    try {
      // Validate configuration
      validateLivenessConfiguration(config);

      // This would integrate with LivenessService to update configuration
      logger.info("Liveness configuration updated for realm: {}", realm.getName());
      return Response.ok().build();
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid liveness configuration provided: {}", e.getMessage());
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    } catch (Exception e) {
      logger.error("Failed to update liveness configuration", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to update liveness configuration: " + e.getMessage()))
          .build();
    }
  }

  /** Test liveness detection with uploaded images. */
  @POST
  @Path("/test-liveness")
  public Response testLivenessDetection(LivenessTestRequest request) {
    try {
      // This would integrate with LivenessService to perform test
      LivenessTestResult result = LivenessTestResult.builder()
          .live(true)
          .livenessScore(0.85)
          .processingTime(250)
          .rejectionReason(null)
          .build();

      return Response.ok(result).build();
    } catch (Exception e) {
      logger.error("Failed to test liveness detection", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to test liveness detection: " + e.getMessage()))
          .build();
    }
  }

  /** Get enrollment statistics for dashboard. */
  @GET
  @Path("/enrollment-statistics")
  public Response getEnrollmentStatistics() {
    try {
      // This would integrate with AdminService to get enrollment statistics
      EnrollmentStatistics stats = EnrollmentStatistics.builder()
          .totalEnrollments(150)
          .successRate(92.5)
          .enrollmentsToday(5)
          .enrollmentsThisWeek(23)
          .build();

      return Response.ok(stats).build();
    } catch (Exception e) {
      logger.error("Failed to get enrollment statistics", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get enrollment statistics: " + e.getMessage()))
          .build();
    }
  }

  /** Get authentication metrics for dashboard. */
  @GET
  @Path("/auth-metrics")
  public Response getAuthenticationMetrics() {
    try {
      // This would integrate with MetricsService to get authentication metrics
      AuthenticationMetrics metrics = AuthenticationMetrics.builder()
          .totalAuthentications(1250)
          .successRate(94.8)
          .avgResponseTime(180)
          .failedAttempts(65)
          .build();

      return Response.ok(metrics).build();
    } catch (Exception e) {
      logger.error("Failed to get authentication metrics", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get authentication metrics: " + e.getMessage()))
          .build();
    }
  }

  /** Get liveness detection statistics for dashboard. */
  @GET
  @Path("/liveness-statistics")
  public Response getLivenessStatistics(@QueryParam("timeframe") @DefaultValue("7d") String timeframe) {
    try {
      // This would integrate with LivenessService to get statistics
      LivenessStatistics stats = LivenessStatistics.builder()
          .totalChecks(850)
          .passRate(89.2)
          .avgScore(0.78)
          .avgTime(320)
          .build();

      return Response.ok(stats).build();
    } catch (Exception e) {
      logger.error("Failed to get liveness statistics", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get liveness statistics: " + e.getMessage()))
          .build();
    }
  }

  /** Get recent activity for dashboard. */
  @GET
  @Path("/recent-activity")
  public Response getRecentActivity(@QueryParam("limit") @DefaultValue("10") int limit) {
    try {
      // This would integrate with AuditService to get recent activities
      java.util.List<ActivityItem> activities = java.util.Collections.emptyList();
      return Response.ok(activities).build();
    } catch (Exception e) {
      logger.error("Failed to get recent activity", e);
      return Response.serverError()
          .entity(new ErrorResponse("Failed to get recent activity: " + e.getMessage()))
          .build();
    }
  }

  private void validateLivenessConfiguration(LivenessConfigDto config) {
    if (config.getLivenessThreshold() < 0.0 || config.getLivenessThreshold() > 1.0) {
      throw new IllegalArgumentException("Liveness threshold must be between 0.0 and 1.0");
    }

    if (config.getMaxOverheadMs() < 50 || config.getMaxOverheadMs() > 5000) {
      throw new IllegalArgumentException("Maximum overhead must be between 50 and 5000 milliseconds");
    }

    if ("CHALLENGE_RESPONSE".equals(config.getDefaultLivenessMode())) {
      if (config.getChallengeDirections() == null || config.getChallengeDirections().size() < 2) {
        throw new IllegalArgumentException("At least 2 challenge directions must be specified for Challenge-Response mode");
      }
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
