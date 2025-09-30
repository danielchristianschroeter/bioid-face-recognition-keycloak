package com.bioid.keycloak.compliance;

import static org.junit.jupiter.api.Assertions.*;

import com.bioid.keycloak.events.FaceRecognitionAdminAuditEvent;
import com.bioid.keycloak.events.FaceRecognitionAuditEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ComplianceReportGeneratorTest {

  @TempDir Path tempDir;

  private ComplianceReportGenerator reportGenerator;
  private List<FaceRecognitionAuditEvent> auditEvents;
  private List<FaceRecognitionAdminAuditEvent> adminAuditEvents;

  @BeforeEach
  void setUp() {
    reportGenerator = new ComplianceReportGenerator(tempDir);

    // Create sample audit events
    auditEvents =
        Arrays.asList(
            FaceRecognitionAuditEvent.builder()
                .eventType("BIOMETRIC_LOGIN_SUCCESS")
                .userId("user1")
                .sessionId("session1")
                .ipAddress("192.168.1.1")
                .realmId("master")
                .clientId("account")
                .timestamp(System.currentTimeMillis())
                .build(),
            FaceRecognitionAuditEvent.builder()
                .eventType("BIOMETRIC_LOGIN_FAILED")
                .userId("user2")
                .sessionId("session2")
                .ipAddress("192.168.1.2")
                .realmId("master")
                .clientId("account")
                .timestamp(System.currentTimeMillis())
                .build());

    // Create sample admin audit events
    adminAuditEvents =
        Arrays.asList(
            FaceRecognitionAdminAuditEvent.builder()
                .operationType("UPDATE")
                .resourceType("FACE_RECOGNITION_CONFIG")
                .resourcePath("/admin/realms/master/face-recognition/config")
                .adminUserId("admin")
                .adminUsername("admin")
                .realmId("master")
                .ipAddress("192.168.1.100")
                .timestamp(System.currentTimeMillis())
                .build());
  }

  @Test
  void shouldGenerateJsonReport() throws IOException {
    // When
    Path reportPath =
        reportGenerator.generateReport(auditEvents, adminAuditEvents, ComplianceReportType.JSON);

    // Then
    assertTrue(Files.exists(reportPath), "Report file should exist");
    assertTrue(reportPath.toString().endsWith(".json"), "Report should have JSON extension");

    String content = Files.readString(reportPath);
    assertTrue(content.contains("BIOMETRIC_LOGIN_SUCCESS"), "Report should contain event type");
    assertTrue(content.contains("user1"), "Report should contain user ID");
    assertTrue(content.contains("UPDATE"), "Report should contain admin operation type");
  }

  @Test
  void shouldGenerateCsvReport() throws IOException {
    // When
    Path reportPath =
        reportGenerator.generateReport(auditEvents, adminAuditEvents, ComplianceReportType.CSV);

    // Then
    assertTrue(Files.exists(reportPath), "Report file should exist");
    assertTrue(reportPath.toString().endsWith(".csv"), "Report should have CSV extension");

    String content = Files.readString(reportPath);
    assertTrue(content.contains("BIOMETRIC_LOGIN_SUCCESS"), "Report should contain event type");
    assertTrue(content.contains("user1"), "Report should contain user ID");
    assertTrue(content.contains("UPDATE"), "Report should contain admin operation type");
  }

  @Test
  void shouldGeneratePdfReport() throws IOException {
    // When
    Path reportPath =
        reportGenerator.generateReport(auditEvents, adminAuditEvents, ComplianceReportType.PDF);

    // Then
    assertTrue(Files.exists(reportPath), "Report file should exist");
    assertTrue(reportPath.toString().endsWith(".pdf"), "Report should have PDF extension");

    String content = Files.readString(reportPath);
    assertTrue(content.contains("BIOMETRIC_LOGIN_SUCCESS"), "Report should contain event type");
    assertTrue(content.contains("user1"), "Report should contain user ID");
    assertTrue(content.contains("UPDATE"), "Report should contain admin operation type");
  }

  @Test
  void shouldFilterComplianceRelevantEvents() throws IOException {
    // Given
    FaceRecognitionAuditEvent nonComplianceEvent =
        FaceRecognitionAuditEvent.builder()
            .eventType("NON_COMPLIANCE_EVENT")
            .userId("user3")
            .sessionId("session3")
            .ipAddress("192.168.1.3")
            .realmId("master")
            .clientId("account")
            .timestamp(System.currentTimeMillis())
            .complianceRelevant(false)
            .build();

    List<FaceRecognitionAuditEvent> mixedEvents =
        Arrays.asList(auditEvents.get(0), nonComplianceEvent);

    // When
    Path reportPath =
        reportGenerator.generateReport(mixedEvents, adminAuditEvents, ComplianceReportType.JSON);

    // Then
    assertTrue(Files.exists(reportPath), "Report file should exist");

    String content = Files.readString(reportPath);
    assertTrue(
        content.contains("BIOMETRIC_LOGIN_SUCCESS"),
        "Report should contain compliance relevant event");
    assertFalse(
        content.contains("NON_COMPLIANCE_EVENT"), "Report should not contain non-compliance event");
  }

  @Test
  void shouldHandleEmptyEventLists() throws IOException {
    // When
    Path reportPath =
        reportGenerator.generateReport(Arrays.asList(), Arrays.asList(), ComplianceReportType.JSON);

    // Then
    assertTrue(Files.exists(reportPath), "Report file should exist");

    String content = Files.readString(reportPath);
    assertTrue(content.contains("\"totalUserEvents\" : 0"), "Report should show zero user events");
    assertTrue(
        content.contains("\"totalAdminEvents\" : 0"), "Report should show zero admin events");
  }

  @Test
  void shouldEscapeCsvValues() throws IOException {
    // Given
    FaceRecognitionAuditEvent eventWithComma =
        FaceRecognitionAuditEvent.builder()
            .eventType("TEST,EVENT")
            .userId("user,with,commas")
            .sessionId("session1")
            .ipAddress("192.168.1.1")
            .realmId("master")
            .clientId("account")
            .timestamp(System.currentTimeMillis())
            .build();

    List<FaceRecognitionAuditEvent> eventsWithSpecialChars = Arrays.asList(eventWithComma);

    // When
    Path reportPath =
        reportGenerator.generateReport(
            eventsWithSpecialChars, Arrays.asList(), ComplianceReportType.CSV);

    // Then
    assertTrue(Files.exists(reportPath), "Report file should exist");

    String content = Files.readString(reportPath);
    assertTrue(content.contains("\"TEST,EVENT\""), "CSV should escape values with commas");
    assertTrue(content.contains("\"user,with,commas\""), "CSV should escape user ID with commas");
  }
}
