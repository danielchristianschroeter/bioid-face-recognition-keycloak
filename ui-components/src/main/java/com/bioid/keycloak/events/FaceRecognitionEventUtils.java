package com.bioid.keycloak.events;

import java.util.Map;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * Utility class for creating and sending Face Recognition events.
 *
 * <p>Provides convenient methods for generating structured audit events from various components of
 * the face recognition system.
 */
public class FaceRecognitionEventUtils {

  /** Create an enrollment success event. */
  public static void createEnrollmentSuccessEvent(
      KeycloakSession session,
      RealmModel realm,
      UserModel user,
      String templateType,
      String sessionId) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.CUSTOM_REQUIRED_ACTION)
            .user(user)
            .session(sessionId)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_ENROLLMENT_SUCCESS)
            .detail(FaceRecognitionEventListener.DETAIL_TEMPLATE_TYPE, templateType)
            .detail("face_recognition", "true")
            .detail("biometric_auth", "enrollment");

    eventBuilder.success();
  }

  /** Create an enrollment failure event. */
  public static void createEnrollmentFailureEvent(
      KeycloakSession session,
      RealmModel realm,
      UserModel user,
      String errorCode,
      int retryCount,
      String sessionId) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.CUSTOM_REQUIRED_ACTION_ERROR)
            .user(user)
            .session(sessionId)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_ENROLLMENT_FAILURE)
            .detail(FaceRecognitionEventListener.DETAIL_ERROR_CODE, errorCode)
            .detail(FaceRecognitionEventListener.DETAIL_RETRY_COUNT, String.valueOf(retryCount))
            .detail("face_recognition", "true")
            .detail("biometric_auth", "enrollment");

    eventBuilder.error(errorCode);
  }

  /** Create a verification success event. */
  public static void createVerificationSuccessEvent(
      KeycloakSession session,
      RealmModel realm,
      UserModel user,
      double verificationScore,
      Double livenessScore,
      String sessionId) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.LOGIN)
            .user(user)
            .session(sessionId)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_VERIFICATION_SUCCESS)
            .detail(
                FaceRecognitionEventListener.DETAIL_VERIFICATION_SCORE,
                String.valueOf(verificationScore))
            .detail("face_recognition", "true")
            .detail("biometric_auth", "verification")
            .detail("auth_method", "face_recognition");

    if (livenessScore != null) {
      eventBuilder.detail(
          FaceRecognitionEventListener.DETAIL_LIVENESS_SCORE, String.valueOf(livenessScore));
    }

    eventBuilder.success();
  }

  /** Create a verification failure event. */
  public static void createVerificationFailureEvent(
      KeycloakSession session,
      RealmModel realm,
      UserModel user,
      String errorCode,
      int retryCount,
      String sessionId) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.LOGIN_ERROR)
            .user(user)
            .session(sessionId)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_VERIFICATION_FAILURE)
            .detail(FaceRecognitionEventListener.DETAIL_ERROR_CODE, errorCode)
            .detail(FaceRecognitionEventListener.DETAIL_RETRY_COUNT, String.valueOf(retryCount))
            .detail("face_recognition", "true")
            .detail("biometric_auth", "verification")
            .detail("auth_method", "face_recognition");

    eventBuilder.error(errorCode);
  }

  /** Create a template deletion event. */
  public static void createTemplateDeletedEvent(
      KeycloakSession session,
      RealmModel realm,
      UserModel user,
      String reason,
      String adminUserId) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.DELETE_ACCOUNT)
            .user(user)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_TEMPLATE_DELETED)
            .detail(FaceRecognitionEventListener.DETAIL_DELETION_REASON, reason)
            .detail(FaceRecognitionEventListener.DETAIL_ADMIN_USER, adminUserId)
            .detail("face_recognition", "true")
            .detail("biometric_auth", "deletion");

    eventBuilder.success();
  }

  /** Create a deletion request created event. */
  public static void createDeletionRequestCreatedEvent(
      KeycloakSession session, RealmModel realm, UserModel user, String priority, String reason) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.UPDATE_PROFILE)
            .user(user)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_DELETION_REQUEST_CREATED)
            .detail(FaceRecognitionEventListener.DETAIL_REQUEST_PRIORITY, priority)
            .detail(FaceRecognitionEventListener.DETAIL_DELETION_REASON, reason)
            .detail("face_recognition", "true")
            .detail("biometric_auth", "deletion_request");

    eventBuilder.success();
  }

  /** Create a deletion request approved event. */
  public static void createDeletionRequestApprovedEvent(
      KeycloakSession session, RealmModel realm, UserModel user, String adminUserId) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.UPDATE_PROFILE)
            .user(user)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_DELETION_REQUEST_APPROVED)
            .detail(FaceRecognitionEventListener.DETAIL_ADMIN_USER, adminUserId)
            .detail("face_recognition", "true")
            .detail("biometric_auth", "deletion_request");

    eventBuilder.success();
  }

  /** Create a deletion request declined event. */
  public static void createDeletionRequestDeclinedEvent(
      KeycloakSession session,
      RealmModel realm,
      UserModel user,
      String adminUserId,
      String reason) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.UPDATE_PROFILE)
            .user(user)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_DELETION_REQUEST_DECLINED)
            .detail(FaceRecognitionEventListener.DETAIL_ADMIN_USER, adminUserId)
            .detail(FaceRecognitionEventListener.DETAIL_DELETION_REASON, reason)
            .detail("face_recognition", "true")
            .detail("biometric_auth", "deletion_request");

    eventBuilder.success();
  }

  /** Create a liveness check event. */
  public static void createLivenessCheckEvent(
      KeycloakSession session,
      RealmModel realm,
      UserModel user,
      String livenessType,
      double livenessScore,
      String sessionId) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.LOGIN)
            .user(user)
            .session(sessionId)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_LIVENESS_CHECK)
            .detail(FaceRecognitionEventListener.DETAIL_LIVENESS_TYPE, livenessType)
            .detail(
                FaceRecognitionEventListener.DETAIL_LIVENESS_SCORE, String.valueOf(livenessScore))
            .detail("face_recognition", "true")
            .detail("biometric_auth", "liveness");

    eventBuilder.success();
  }

  /** Create a configuration update event. */
  public static void createConfigurationUpdateEvent(
      KeycloakSession session, RealmModel realm, String adminUserId, String configSection) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(EventType.UPDATE_PROFILE)
            .detail("face_event_type", FaceRecognitionEventListener.FACE_CONFIG_UPDATED)
            .detail(FaceRecognitionEventListener.DETAIL_ADMIN_USER, adminUserId)
            .detail("config_section", configSection)
            .detail("face_recognition", "true")
            .detail("biometric_auth", "configuration");

    eventBuilder.success();
  }

  /** Create a generic face recognition event with custom details. */
  public static void createCustomFaceEvent(
      KeycloakSession session,
      RealmModel realm,
      UserModel user,
      EventType eventType,
      String faceEventType,
      Map<String, String> customDetails,
      String sessionId) {
    EventBuilder eventBuilder =
        new EventBuilder(realm, session, session.getContext().getConnection())
            .event(eventType)
            .detail("face_event_type", faceEventType)
            .detail("face_recognition", "true")
            .detail("biometric_auth", "custom");

    if (user != null) {
      eventBuilder.user(user);
    }

    if (sessionId != null) {
      eventBuilder.session(sessionId);
    }

    if (customDetails != null) {
      for (Map.Entry<String, String> entry : customDetails.entrySet()) {
        eventBuilder.detail(entry.getKey(), entry.getValue());
      }
    }

    eventBuilder.success();
  }
}
