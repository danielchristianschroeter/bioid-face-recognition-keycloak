package com.bioid.keycloak.events;

import java.util.Map;
import java.util.Set;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Event listener for Face Recognition operations.
 *
 * <p>Provides comprehensive audit logging for all biometric operations including enrollment,
 * verification, deletion, and administrative actions.
 */
@ApplicationScoped
public class FaceRecognitionEventListener implements EventListenerProvider {

  private static final Logger logger = LoggerFactory.getLogger(FaceRecognitionEventListener.class);
  private static final Logger auditLogger = LoggerFactory.getLogger("FACE_RECOGNITION_AUDIT");

  // Custom event types for face recognition
  public static final String FACE_ENROLLMENT_SUCCESS = "FACE_ENROLLMENT_SUCCESS";
  public static final String FACE_ENROLLMENT_FAILURE = "FACE_ENROLLMENT_FAILURE";
  public static final String FACE_VERIFICATION_SUCCESS = "FACE_VERIFICATION_SUCCESS";
  public static final String FACE_VERIFICATION_FAILURE = "FACE_VERIFICATION_FAILURE";
  public static final String FACE_TEMPLATE_DELETED = "FACE_TEMPLATE_DELETED";
  public static final String FACE_DELETION_REQUEST_CREATED = "FACE_DELETION_REQUEST_CREATED";
  public static final String FACE_DELETION_REQUEST_APPROVED = "FACE_DELETION_REQUEST_APPROVED";
  public static final String FACE_DELETION_REQUEST_DECLINED = "FACE_DELETION_REQUEST_DECLINED";
  public static final String FACE_LIVENESS_CHECK = "FACE_LIVENESS_CHECK";
  public static final String FACE_CONFIG_UPDATED = "FACE_CONFIG_UPDATED";

  // Event details keys
  public static final String DETAIL_VERIFICATION_SCORE = "verification_score";
  public static final String DETAIL_LIVENESS_SCORE = "liveness_score";
  public static final String DETAIL_LIVENESS_TYPE = "liveness_type";
  public static final String DETAIL_TEMPLATE_TYPE = "template_type";
  public static final String DETAIL_DELETION_REASON = "deletion_reason";
  public static final String DETAIL_ADMIN_USER = "admin_user";
  public static final String DETAIL_REQUEST_PRIORITY = "request_priority";
  public static final String DETAIL_BIOID_ENDPOINT = "bioid_endpoint";
  public static final String DETAIL_ERROR_CODE = "error_code";
  public static final String DETAIL_RETRY_COUNT = "retry_count";

  private final Set<EventType> excludedEventTypes =
      Set.of(EventType.LOGIN, EventType.LOGOUT, EventType.REFRESH_TOKEN);

  @Override
  public void onEvent(Event event) {
    try {
      // Skip excluded event types to reduce noise
      if (excludedEventTypes.contains(event.getType())) {
        return;
      }

      // Log face recognition related events
      if (isFaceRecognitionEvent(event)) {
        logFaceRecognitionEvent(event);
      }

      // Log authentication events that might involve face recognition
      if (isAuthenticationEvent(event)) {
        logAuthenticationEvent(event);
      }

    } catch (Exception e) {
      logger.error("Error processing event: {}", event.getType(), e);
    }
  }

  @Override
  public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    try {
      // Log face recognition admin events
      if (isFaceRecognitionAdminEvent(adminEvent)) {
        logFaceRecognitionAdminEvent(adminEvent, includeRepresentation);
      }

    } catch (Exception e) {
      logger.error("Error processing admin event: {}", adminEvent.getOperationType(), e);
    }
  }

  private boolean isFaceRecognitionEvent(Event event) {
    // Check if event is related to face recognition
    return event.getDetails() != null
        && (event.getDetails().containsKey("face_recognition")
            || event.getDetails().containsKey("biometric_auth")
            || event.getDetails().containsKey("face_enrollment")
            || event.getDetails().containsKey("face_verification"));
  }

  private boolean isAuthenticationEvent(Event event) {
    return event.getType() == EventType.LOGIN_ERROR
        || event.getType() == EventType.CUSTOM_REQUIRED_ACTION
        || event.getType() == EventType.CUSTOM_REQUIRED_ACTION_ERROR;
  }

  private boolean isFaceRecognitionAdminEvent(AdminEvent adminEvent) {
    // Check if admin event is related to face recognition
    return adminEvent.getResourcePath() != null
        && (adminEvent.getResourcePath().contains("face-recognition")
            || adminEvent.getResourcePath().contains("deletion-requests")
            || adminEvent.getResourcePath().contains("biometric"));
  }

  private void logFaceRecognitionEvent(Event event) {
    FaceRecognitionAuditEvent auditEvent =
        FaceRecognitionAuditEvent.builder()
            .eventType(event.getType().toString())
            .userId(event.getUserId())
            .sessionId(event.getSessionId())
            .ipAddress(event.getIpAddress())
            .realmId(event.getRealmId())
            .clientId(event.getClientId())
            .timestamp(event.getTime())
            .details(event.getDetails())
            .build();

    // Log to audit logger
    auditLogger.info("Face Recognition Event: {}", auditEvent.toJson());

    // Log specific face recognition events with enhanced details
    logSpecificFaceEvent(event);
  }

  private void logSpecificFaceEvent(Event event) {
    Map<String, String> details = event.getDetails();
    if (details == null) return;

    String eventSubType = details.get("face_event_type");
    if (eventSubType == null) return;

    switch (eventSubType) {
      case FACE_ENROLLMENT_SUCCESS:
        auditLogger.info(
            "ENROLLMENT_SUCCESS: User {} successfully enrolled face template. Template type: {}, Session: {}",
            event.getUserId(),
            details.get(DETAIL_TEMPLATE_TYPE),
            event.getSessionId());
        break;

      case FACE_ENROLLMENT_FAILURE:
        auditLogger.warn(
            "ENROLLMENT_FAILURE: User {} failed to enroll face template. Error: {}, Retry count: {}, Session: {}",
            event.getUserId(),
            details.get(DETAIL_ERROR_CODE),
            details.get(DETAIL_RETRY_COUNT),
            event.getSessionId());
        break;

      case FACE_VERIFICATION_SUCCESS:
        auditLogger.info(
            "VERIFICATION_SUCCESS: User {} successfully verified with face recognition. Score: {}, Liveness: {}, Session: {}",
            event.getUserId(),
            details.get(DETAIL_VERIFICATION_SCORE),
            details.get(DETAIL_LIVENESS_SCORE),
            event.getSessionId());
        break;

      case FACE_VERIFICATION_FAILURE:
        auditLogger.warn(
            "VERIFICATION_FAILURE: User {} failed face verification. Error: {}, Retry count: {}, Session: {}",
            event.getUserId(),
            details.get(DETAIL_ERROR_CODE),
            details.get(DETAIL_RETRY_COUNT),
            event.getSessionId());
        break;

      case FACE_TEMPLATE_DELETED:
        auditLogger.info(
            "TEMPLATE_DELETED: Face template deleted for user {}. Reason: {}, Admin: {}",
            event.getUserId(),
            details.get(DETAIL_DELETION_REASON),
            details.get(DETAIL_ADMIN_USER));
        break;

      case FACE_DELETION_REQUEST_CREATED:
        auditLogger.info(
            "DELETION_REQUEST_CREATED: User {} created deletion request. Priority: {}, Reason: {}",
            event.getUserId(),
            details.get(DETAIL_REQUEST_PRIORITY),
            details.get(DETAIL_DELETION_REASON));
        break;

      case FACE_LIVENESS_CHECK:
        auditLogger.info(
            "LIVENESS_CHECK: User {} liveness check. Type: {}, Score: {}, Session: {}",
            event.getUserId(),
            details.get(DETAIL_LIVENESS_TYPE),
            details.get(DETAIL_LIVENESS_SCORE),
            event.getSessionId());
        break;

      default:
        auditLogger.info(
            "FACE_EVENT: User {} - {} with details: {}", event.getUserId(), eventSubType, details);
        break;
    }
  }

  private void logAuthenticationEvent(Event event) {
    // Log authentication events that might involve face recognition
    Map<String, String> details = event.getDetails();
    if (details != null
        && details.containsKey("auth_method")
        && "face_recognition".equals(details.get("auth_method"))) {

      auditLogger.info(
          "AUTH_EVENT: User {} authentication event {} with face recognition. IP: {}, Session: {}",
          event.getUserId(),
          event.getType(),
          event.getIpAddress(),
          event.getSessionId());
    }
  }

  private void logFaceRecognitionAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
    FaceRecognitionAdminAuditEvent auditEvent =
        FaceRecognitionAdminAuditEvent.builder()
            .operationType(adminEvent.getOperationType().toString())
            .resourceType(
                adminEvent.getResourceType() != null
                    ? adminEvent.getResourceType().toString()
                    : null)
            .resourcePath(adminEvent.getResourcePath())
            .adminUserId(adminEvent.getAuthDetails().getUserId())
            .adminUsername(
                adminEvent.getAuthDetails().getUserId()) // Would need to resolve username
            .realmId(adminEvent.getRealmId())
            .timestamp(adminEvent.getTime())
            .ipAddress(adminEvent.getAuthDetails().getIpAddress())
            .userAgent("N/A") // getUserAgent() not available in this Keycloak version
            .representation(includeRepresentation ? adminEvent.getRepresentation() : null)
            .build();

    auditLogger.info("Face Recognition Admin Event: {}", auditEvent.toJson());

    // Log specific admin operations
    logSpecificAdminEvent(adminEvent);
  }

  private void logSpecificAdminEvent(AdminEvent adminEvent) {
    String resourcePath = adminEvent.getResourcePath();
    OperationType operation = adminEvent.getOperationType();

    if (resourcePath.contains("face-recognition/config")) {
      auditLogger.info(
          "CONFIG_UPDATE: Admin {} updated face recognition configuration. Operation: {}, IP: {}",
          adminEvent.getAuthDetails().getUserId(),
          operation,
          adminEvent.getAuthDetails().getIpAddress());
    } else if (resourcePath.contains("deletion-requests")) {
      if (resourcePath.contains("/approve")) {
        auditLogger.info(
            "DELETION_APPROVED: Admin {} approved deletion request. Path: {}, IP: {}",
            adminEvent.getAuthDetails().getUserId(),
            resourcePath,
            adminEvent.getAuthDetails().getIpAddress());
      } else if (resourcePath.contains("/decline")) {
        auditLogger.info(
            "DELETION_DECLINED: Admin {} declined deletion request. Path: {}, IP: {}",
            adminEvent.getAuthDetails().getUserId(),
            resourcePath,
            adminEvent.getAuthDetails().getIpAddress());
      } else if (operation == OperationType.CREATE) {
        auditLogger.info(
            "DELETION_REQUEST_ADMIN_CREATED: Admin {} created deletion request. Path: {}, IP: {}",
            adminEvent.getAuthDetails().getUserId(),
            resourcePath,
            adminEvent.getAuthDetails().getIpAddress());
      }
    } else if (resourcePath.contains("face-recognition") && operation == OperationType.DELETE) {
      auditLogger.warn(
          "TEMPLATE_ADMIN_DELETED: Admin {} deleted face template. Path: {}, IP: {}",
          adminEvent.getAuthDetails().getUserId(),
          resourcePath,
          adminEvent.getAuthDetails().getIpAddress());
    }
  }

  @Override
  public void close() {
    // Cleanup resources if needed
    logger.debug("Face Recognition Event Listener closed");
  }
}
