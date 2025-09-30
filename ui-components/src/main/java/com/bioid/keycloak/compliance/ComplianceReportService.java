package com.bioid.keycloak.compliance;

import com.bioid.keycloak.events.FaceRecognitionAdminAuditEvent;
import com.bioid.keycloak.events.FaceRecognitionAuditEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.keycloak.models.KeycloakSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service for generating and managing compliance reports. */
public class ComplianceReportService {

  private static final Logger logger = LoggerFactory.getLogger(ComplianceReportService.class);

  private final KeycloakSession session;
  private final ComplianceReportGenerator reportGenerator;
  private final Path reportDirectory;

  /**
   * Creates a new compliance report service.
   *
   * @param session The Keycloak session
   */
  public ComplianceReportService(KeycloakSession session) {
    this.session = session;

    // Get the report directory from system property or use default
    String reportDirPath =
        System.getProperty(
            "bioid.reports.directory",
            System.getProperty("jboss.server.data.dir", System.getProperty("java.io.tmpdir"))
                + "/bioid-reports");
    this.reportDirectory = Paths.get(reportDirPath);
    this.reportGenerator = new ComplianceReportGenerator(reportDirectory);

    logger.info("Compliance report directory: " + reportDirectory);
  }

  /**
   * Generates a compliance report for the specified date range.
   *
   * @param startDate The start date (inclusive)
   * @param endDate The end date (inclusive)
   * @param reportType The type of report to generate
   * @return The path to the generated report
   */
  public Path generateReport(
      LocalDate startDate, LocalDate endDate, ComplianceReportType reportType) {
    logger.info(
        "Generating compliance report from "
            + startDate
            + " to "
            + endDate
            + " in format "
            + reportType);

    // Convert dates to timestamps
    long startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    long endTimestamp =
        endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;

    // Retrieve audit events for the date range
    List<FaceRecognitionAuditEvent> auditEvents = retrieveAuditEvents(startTimestamp, endTimestamp);
    List<FaceRecognitionAdminAuditEvent> adminAuditEvents =
        retrieveAdminAuditEvents(startTimestamp, endTimestamp);

    // Generate the report
    return reportGenerator.generateReport(auditEvents, adminAuditEvents, reportType);
  }

  /**
   * Generates a compliance report for the current month.
   *
   * @param reportType The type of report to generate
   * @return The path to the generated report
   */
  public Path generateMonthlyReport(ComplianceReportType reportType) {
    LocalDate now = LocalDate.now();
    LocalDate startOfMonth = now.withDayOfMonth(1);

    return generateReport(startOfMonth, now, reportType);
  }

  /**
   * Generates a compliance report for the specified month.
   *
   * @param year The year
   * @param month The month (1-12)
   * @param reportType The type of report to generate
   * @return The path to the generated report
   */
  public Path generateMonthlyReport(int year, int month, ComplianceReportType reportType) {
    LocalDate startOfMonth = LocalDate.of(year, month, 1);
    LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

    return generateReport(startOfMonth, endOfMonth, reportType);
  }

  /**
   * Retrieves audit events for the specified date range. In a real implementation, this would query
   * a database or event store. For now, we'll return a sample list of events.
   */
  private List<FaceRecognitionAuditEvent> retrieveAuditEvents(
      long startTimestamp, long endTimestamp) {
    // In a real implementation, this would query a database or event store
    // For now, we'll return a sample list of events
    List<FaceRecognitionAuditEvent> events = new ArrayList<>();

    // Add some sample events
    events.add(
        FaceRecognitionAuditEvent.builder()
            .eventType("BIOMETRIC_LOGIN_SUCCESS")
            .userId("user1")
            .sessionId("session1")
            .ipAddress("192.168.1.1")
            .realmId("master")
            .clientId("account")
            .timestamp(System.currentTimeMillis() - 3600000) // 1 hour ago
            .build());

    events.add(
        FaceRecognitionAuditEvent.builder()
            .eventType("BIOMETRIC_LOGIN_FAILED")
            .userId("user2")
            .sessionId("session2")
            .ipAddress("192.168.1.2")
            .realmId("master")
            .clientId("account")
            .timestamp(System.currentTimeMillis() - 7200000) // 2 hours ago
            .build());

    events.add(
        FaceRecognitionAuditEvent.builder()
            .eventType("BIOMETRIC_ENROLLMENT")
            .userId("user3")
            .sessionId("session3")
            .ipAddress("192.168.1.3")
            .realmId("master")
            .clientId("account")
            .timestamp(System.currentTimeMillis() - 86400000) // 1 day ago
            .build());

    // Filter events by timestamp
    return events.stream()
        .filter(
            event -> event.getTimestamp() >= startTimestamp && event.getTimestamp() <= endTimestamp)
        .collect(Collectors.toList());
  }

  /**
   * Retrieves admin audit events for the specified date range. In a real implementation, this would
   * query a database or event store. For now, we'll return a sample list of events.
   */
  private List<FaceRecognitionAdminAuditEvent> retrieveAdminAuditEvents(
      long startTimestamp, long endTimestamp) {
    // In a real implementation, this would query a database or event store
    // For now, we'll return a sample list of events
    List<FaceRecognitionAdminAuditEvent> events = new ArrayList<>();

    // Add some sample events
    events.add(
        FaceRecognitionAdminAuditEvent.builder()
            .operationType("UPDATE")
            .resourceType("FACE_RECOGNITION_CONFIG")
            .resourcePath("/admin/realms/master/face-recognition/config")
            .adminUserId("admin")
            .adminUsername("admin")
            .realmId("master")
            .ipAddress("192.168.1.100")
            .timestamp(System.currentTimeMillis() - 3600000) // 1 hour ago
            .build());

    events.add(
        FaceRecognitionAdminAuditEvent.builder()
            .operationType("DELETE")
            .resourceType("BIOMETRIC_TEMPLATE")
            .resourcePath("/admin/realms/master/face-recognition/deletion-requests/123")
            .adminUserId("admin")
            .adminUsername("admin")
            .realmId("master")
            .ipAddress("192.168.1.100")
            .timestamp(System.currentTimeMillis() - 7200000) // 2 hours ago
            .build());

    // Filter events by timestamp
    return events.stream()
        .filter(
            event -> event.getTimestamp() >= startTimestamp && event.getTimestamp() <= endTimestamp)
        .collect(Collectors.toList());
  }

  /** Gets the path to the report directory. */
  public Path getReportDirectory() {
    return reportDirectory;
  }
}
