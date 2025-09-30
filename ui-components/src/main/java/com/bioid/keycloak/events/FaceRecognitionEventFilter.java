package com.bioid.keycloak.events;

import java.util.Map;
import java.util.Set;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;

/**
 * Event filter for Face Recognition events.
 *
 * <p>Provides filtering and categorization capabilities for audit events to support compliance
 * reporting and administrative monitoring.
 */
public class FaceRecognitionEventFilter {

  // High-risk operations that require special attention
  private static final Set<String> HIGH_RISK_FACE_EVENTS =
      Set.of(
          FaceRecognitionEventListener.FACE_TEMPLATE_DELETED,
          FaceRecognitionEventListener.FACE_DELETION_REQUEST_APPROVED,
          FaceRecognitionEventListener.FACE_CONFIG_UPDATED);

  // Security-relevant operations
  private static final Set<String> SECURITY_RELEVANT_FACE_EVENTS =
      Set.of(
          FaceRecognitionEventListener.FACE_VERIFICATION_FAILURE,
          FaceRecognitionEventListener.FACE_ENROLLMENT_FAILURE,
          FaceRecognitionEventListener.FACE_TEMPLATE_DELETED,
          FaceRecognitionEventListener.FACE_CONFIG_UPDATED);

  // Compliance-relevant operations (GDPR, etc.)
  private static final Set<String> COMPLIANCE_RELEVANT_FACE_EVENTS =
      Set.of(
          FaceRecognitionEventListener.FACE_ENROLLMENT_SUCCESS,
          FaceRecognitionEventListener.FACE_TEMPLATE_DELETED,
          FaceRecognitionEventListener.FACE_DELETION_REQUEST_CREATED,
          FaceRecognitionEventListener.FACE_DELETION_REQUEST_APPROVED,
          FaceRecognitionEventListener.FACE_DELETION_REQUEST_DECLINED);

  // High-risk admin operations
  private static final Set<OperationType> HIGH_RISK_ADMIN_OPERATIONS =
      Set.of(OperationType.DELETE, OperationType.UPDATE);

  /** Check if an event is face recognition related. */
  public static boolean isFaceRecognitionEvent(Event event) {
    if (event.getDetails() == null) {
      return false;
    }

    return event.getDetails().containsKey("face_recognition")
        || event.getDetails().containsKey("biometric_auth")
        || event.getDetails().containsKey("face_enrollment")
        || event.getDetails().containsKey("face_verification")
        || event.getDetails().containsKey("face_event_type");
  }

  /** Check if an admin event is face recognition related. */
  public static boolean isFaceRecognitionAdminEvent(AdminEvent adminEvent) {
    String resourcePath = adminEvent.getResourcePath();
    return resourcePath != null
        && (resourcePath.contains("face-recognition")
            || resourcePath.contains("deletion-requests")
            || resourcePath.contains("biometric"));
  }

  /** Check if an event is high-risk and requires special attention. */
  public static boolean isHighRiskEvent(Event event) {
    if (!isFaceRecognitionEvent(event)) {
      return false;
    }

    Map<String, String> details = event.getDetails();
    String faceEventType = details.get("face_event_type");

    return faceEventType != null && HIGH_RISK_FACE_EVENTS.contains(faceEventType);
  }

  /** Check if an admin event is high-risk. */
  public static boolean isHighRiskAdminEvent(AdminEvent adminEvent) {
    if (!isFaceRecognitionAdminEvent(adminEvent)) {
      return false;
    }

    return HIGH_RISK_ADMIN_OPERATIONS.contains(adminEvent.getOperationType())
        || adminEvent.getResourcePath().contains("config")
        || adminEvent.getResourcePath().contains("deletion-requests");
  }

  /** Check if an event is security-relevant. */
  public static boolean isSecurityRelevantEvent(Event event) {
    if (!isFaceRecognitionEvent(event)) {
      return false;
    }

    // Authentication failures are always security relevant
    if (event.getType() == EventType.LOGIN_ERROR) {
      return true;
    }

    Map<String, String> details = event.getDetails();
    String faceEventType = details.get("face_event_type");

    return faceEventType != null && SECURITY_RELEVANT_FACE_EVENTS.contains(faceEventType);
  }

  /** Check if an event is compliance-relevant (GDPR, etc.). */
  public static boolean isComplianceRelevantEvent(Event event) {
    if (!isFaceRecognitionEvent(event)) {
      return false;
    }

    Map<String, String> details = event.getDetails();
    String faceEventType = details.get("face_event_type");

    return faceEventType != null && COMPLIANCE_RELEVANT_FACE_EVENTS.contains(faceEventType);
  }

  /** Get event category for filtering and reporting. */
  public static EventCategory getEventCategory(Event event) {
    if (!isFaceRecognitionEvent(event)) {
      return EventCategory.OTHER;
    }

    Map<String, String> details = event.getDetails();
    String biometricAuth = details.get("biometric_auth");

    if (biometricAuth == null) {
      return EventCategory.OTHER;
    }

    switch (biometricAuth) {
      case "enrollment":
        return EventCategory.ENROLLMENT;
      case "verification":
        return EventCategory.VERIFICATION;
      case "deletion":
      case "deletion_request":
        return EventCategory.DELETION;
      case "liveness":
        return EventCategory.LIVENESS;
      case "configuration":
        return EventCategory.CONFIGURATION;
      default:
        return EventCategory.OTHER;
    }
  }

  /** Get admin event category. */
  public static AdminEventCategory getAdminEventCategory(AdminEvent adminEvent) {
    if (!isFaceRecognitionAdminEvent(adminEvent)) {
      return AdminEventCategory.OTHER;
    }

    String resourcePath = adminEvent.getResourcePath();

    if (resourcePath.contains("config")) {
      return AdminEventCategory.CONFIGURATION;
    } else if (resourcePath.contains("deletion-requests")) {
      return AdminEventCategory.DELETION_MANAGEMENT;
    } else if (resourcePath.contains("templates")) {
      return AdminEventCategory.TEMPLATE_MANAGEMENT;
    } else if (resourcePath.contains("metrics")) {
      return AdminEventCategory.MONITORING;
    } else {
      return AdminEventCategory.OTHER;
    }
  }

  /** Check if event should be included in audit logs based on severity. */
  public static boolean shouldAuditEvent(Event event, AuditLevel auditLevel) {
    if (!isFaceRecognitionEvent(event)) {
      return false;
    }

    switch (auditLevel) {
      case MINIMAL:
        return isHighRiskEvent(event) || isSecurityRelevantEvent(event);
      case STANDARD:
        return isComplianceRelevantEvent(event) || isSecurityRelevantEvent(event);
      case COMPREHENSIVE:
        return true;
      default:
        return false;
    }
  }

  /** Check if admin event should be included in audit logs. */
  public static boolean shouldAuditAdminEvent(AdminEvent adminEvent, AuditLevel auditLevel) {
    if (!isFaceRecognitionAdminEvent(adminEvent)) {
      return false;
    }

    switch (auditLevel) {
      case MINIMAL:
        return isHighRiskAdminEvent(adminEvent);
      case STANDARD:
      case COMPREHENSIVE:
        return true;
      default:
        return false;
    }
  }

  /** Event categories for filtering and reporting. */
  public enum EventCategory {
    ENROLLMENT,
    VERIFICATION,
    DELETION,
    LIVENESS,
    CONFIGURATION,
    OTHER
  }

  /** Admin event categories. */
  public enum AdminEventCategory {
    CONFIGURATION,
    DELETION_MANAGEMENT,
    TEMPLATE_MANAGEMENT,
    MONITORING,
    OTHER
  }

  /** Audit levels for event filtering. */
  public enum AuditLevel {
    MINIMAL, // Only high-risk and security events
    STANDARD, // Compliance and security events
    COMPREHENSIVE // All face recognition events
  }
}
