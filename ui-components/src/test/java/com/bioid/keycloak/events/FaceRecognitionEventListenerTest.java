package com.bioid.keycloak.events;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FaceRecognitionEventListenerTest {

  private FaceRecognitionEventListener eventListener;

  @BeforeEach
  void setUp() {
    eventListener = new FaceRecognitionEventListener();
  }

  @Test
  void shouldProcessFaceRecognitionEvent() {
    // Given
    Event event = createTestEvent();
    Map<String, String> details = new HashMap<>();
    details.put("face_recognition", "true");
    details.put("face_event_type", FaceRecognitionEventListener.FACE_ENROLLMENT_SUCCESS);
    details.put(FaceRecognitionEventListener.DETAIL_TEMPLATE_TYPE, "STANDARD");
    event.setDetails(details);

    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> eventListener.onEvent(event));
  }

  @Test
  void shouldProcessAuthenticationEvent() {
    // Given
    Event event = createTestEvent();
    event.setType(EventType.LOGIN_ERROR);
    Map<String, String> details = new HashMap<>();
    details.put("auth_method", "face_recognition");
    event.setDetails(details);

    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> eventListener.onEvent(event));
  }

  @Test
  void shouldProcessAdminEvent() {
    // Given
    AdminEvent adminEvent = createTestAdminEvent();
    adminEvent.setResourcePath("face-recognition/config");
    adminEvent.setOperationType(OperationType.UPDATE);

    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> eventListener.onEvent(adminEvent, true));
  }

  @Test
  void shouldSkipExcludedEventTypes() {
    // Given
    Event event = createTestEvent();
    event.setType(EventType.LOGIN); // This is in excluded types

    // When & Then - Should not throw exception and should skip processing
    assertDoesNotThrow(() -> eventListener.onEvent(event));
  }

  @Test
  void shouldHandleEventWithoutDetails() {
    // Given
    Event event = createTestEvent();
    event.setDetails(null);

    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> eventListener.onEvent(event));
  }

  @Test
  void shouldHandleAdminEventWithoutResourcePath() {
    // Given
    AdminEvent adminEvent = createTestAdminEvent();
    adminEvent.setResourcePath(null);

    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> eventListener.onEvent(adminEvent, false));
  }

  @Test
  void shouldCreateAuditEventFromEvent() {
    // Given
    Event event = createTestEvent();
    Map<String, String> details = new HashMap<>();
    details.put("face_recognition", "true");
    details.put("test_detail", "test_value");
    event.setDetails(details);

    // When
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

    // Then
    assertNotNull(auditEvent);
    assertEquals(event.getType().toString(), auditEvent.getEventType());
    assertEquals(event.getUserId(), auditEvent.getUserId());
    assertEquals(event.getSessionId(), auditEvent.getSessionId());
    assertEquals(event.getIpAddress(), auditEvent.getIpAddress());
    assertEquals(event.getRealmId(), auditEvent.getRealmId());
    assertEquals(event.getClientId(), auditEvent.getClientId());
    assertEquals(event.getTime(), auditEvent.getTimestamp());
    assertEquals(event.getDetails(), auditEvent.getDetails());
    assertTrue(auditEvent.isComplianceRelevant());
  }

  @Test
  void shouldCreateAdminAuditEventFromAdminEvent() {
    // Given
    AdminEvent adminEvent = createTestAdminEvent();

    // When
    FaceRecognitionAdminAuditEvent auditEvent =
        FaceRecognitionAdminAuditEvent.builder()
            .operationType(adminEvent.getOperationType().toString())
            .resourcePath(adminEvent.getResourcePath())
            .adminUserId(adminEvent.getAuthDetails().getUserId())
            .realmId(adminEvent.getRealmId())
            .timestamp(adminEvent.getTime())
            .ipAddress(adminEvent.getAuthDetails().getIpAddress())
            .userAgent("N/A") // getUserAgent() not available in this Keycloak version
            .build();

    // Then
    assertNotNull(auditEvent);
    assertEquals(adminEvent.getOperationType().toString(), auditEvent.getOperationType());
    assertEquals(adminEvent.getResourcePath(), auditEvent.getResourcePath());
    assertEquals(adminEvent.getAuthDetails().getUserId(), auditEvent.getAdminUserId());
    assertEquals(adminEvent.getRealmId(), auditEvent.getRealmId());
    assertEquals(adminEvent.getTime(), auditEvent.getTimestamp());
    assertEquals(adminEvent.getAuthDetails().getIpAddress(), auditEvent.getIpAddress());
    assertEquals(
        "N/A", auditEvent.getUserAgent()); // getUserAgent() not available in this Keycloak version
    assertTrue(auditEvent.isComplianceRelevant());
    assertTrue(auditEvent.isSecurityRelevant());
  }

  @Test
  void shouldSerializeAuditEventToJson() {
    // Given
    FaceRecognitionAuditEvent auditEvent =
        FaceRecognitionAuditEvent.builder()
            .eventType("TEST_EVENT")
            .userId("test-user")
            .sessionId("test-session")
            .ipAddress("192.168.1.1")
            .realmId("test-realm")
            .clientId("test-client")
            .timestamp(System.currentTimeMillis())
            .build();

    // When
    String json = auditEvent.toJson();

    // Then
    assertNotNull(json);
    assertTrue(json.contains("TEST_EVENT"));
    assertTrue(json.contains("test-user"));
    assertTrue(json.contains("test-session"));
    assertTrue(json.contains("192.168.1.1"));
    assertTrue(json.contains("test-realm"));
    assertTrue(json.contains("test-client"));
    assertTrue(json.contains("BIOMETRIC_AUTHENTICATION"));
  }

  @Test
  void shouldSerializeAdminAuditEventToJson() {
    // Given
    FaceRecognitionAdminAuditEvent auditEvent =
        FaceRecognitionAdminAuditEvent.builder()
            .operationType("UPDATE")
            .resourcePath("face-recognition/config")
            .adminUserId("admin-user")
            .realmId("test-realm")
            .timestamp(System.currentTimeMillis())
            .ipAddress("192.168.1.1")
            .build();

    // When
    String json = auditEvent.toJson();

    // Then
    assertNotNull(json);
    assertTrue(json.contains("UPDATE"));
    assertTrue(json.contains("face-recognition/config"));
    assertTrue(json.contains("admin-user"));
    assertTrue(json.contains("test-realm"));
    assertTrue(json.contains("192.168.1.1"));
    assertTrue(json.contains("BIOMETRIC_ADMINISTRATION"));
  }

  @Test
  void shouldIdentifyHighRiskOperations() {
    // Given
    FaceRecognitionAdminAuditEvent deleteEvent =
        FaceRecognitionAdminAuditEvent.builder()
            .operationType("DELETE")
            .resourcePath("face-recognition/templates/123")
            .build();

    FaceRecognitionAdminAuditEvent configEvent =
        FaceRecognitionAdminAuditEvent.builder()
            .operationType("UPDATE")
            .resourcePath("face-recognition/config")
            .build();

    FaceRecognitionAdminAuditEvent normalEvent =
        FaceRecognitionAdminAuditEvent.builder()
            .operationType("GET")
            .resourcePath("face-recognition/metrics")
            .build();

    // When & Then
    assertTrue(deleteEvent.isHighRiskOperation());
    assertTrue(configEvent.isHighRiskOperation());
    assertFalse(normalEvent.isHighRiskOperation());
  }

  @Test
  void shouldCloseWithoutError() {
    // When & Then - Should not throw exception
    assertDoesNotThrow(() -> eventListener.close());
  }

  private Event createTestEvent() {
    Event event = new Event();
    event.setType(EventType.CUSTOM_REQUIRED_ACTION);
    event.setUserId("test-user-id");
    event.setSessionId("test-session-id");
    event.setIpAddress("192.168.1.1");
    event.setRealmId("test-realm");
    event.setClientId("test-client");
    event.setTime(System.currentTimeMillis());
    return event;
  }

  private AdminEvent createTestAdminEvent() {
    AdminEvent adminEvent = new AdminEvent();
    adminEvent.setOperationType(OperationType.UPDATE);
    adminEvent.setResourcePath("face-recognition/config");
    adminEvent.setRealmId("test-realm");
    adminEvent.setTime(System.currentTimeMillis());

    AuthDetails authDetails = new AuthDetails();
    authDetails.setUserId("admin-user-id");
    authDetails.setIpAddress("192.168.1.1");
    // authDetails.setUserAgent("Mozilla/5.0"); // Not available in this Keycloak version
    adminEvent.setAuthDetails(authDetails);

    return adminEvent;
  }
}
