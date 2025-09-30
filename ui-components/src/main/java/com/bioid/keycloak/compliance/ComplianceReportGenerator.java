package com.bioid.keycloak.compliance;

import com.bioid.keycloak.events.FaceRecognitionAdminAuditEvent;
import com.bioid.keycloak.events.FaceRecognitionAuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Generates compliance reports based on audit events. */
public class ComplianceReportGenerator {

  private static final Logger logger = LoggerFactory.getLogger(ComplianceReportGenerator.class);
  private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
  private static final DateTimeFormatter FILE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

  private final Path reportDirectory;

  /**
   * Creates a new compliance report generator.
   *
   * @param reportDirectory The directory where reports will be stored
   */
  public ComplianceReportGenerator(Path reportDirectory) {
    this.reportDirectory = reportDirectory;
    ensureDirectoryExists(reportDirectory);
  }

  /**
   * Generates a compliance report for the given audit events.
   *
   * @param auditEvents The audit events to include in the report
   * @param adminAuditEvents The admin audit events to include in the report
   * @param reportType The type of report to generate
   * @return The path to the generated report file
   */
  public Path generateReport(
      List<FaceRecognitionAuditEvent> auditEvents,
      List<FaceRecognitionAdminAuditEvent> adminAuditEvents,
      ComplianceReportType reportType) {

    // Filter only compliance-relevant events
    List<FaceRecognitionAuditEvent> relevantAuditEvents =
        auditEvents.stream()
            .filter(FaceRecognitionAuditEvent::isComplianceRelevant)
            .collect(Collectors.toList());

    List<FaceRecognitionAdminAuditEvent> relevantAdminAuditEvents =
        adminAuditEvents.stream()
            .filter(FaceRecognitionAdminAuditEvent::isComplianceRelevant)
            .collect(Collectors.toList());

    // Generate the report based on the report type
    switch (reportType) {
      case JSON:
        return generateJsonReport(relevantAuditEvents, relevantAdminAuditEvents);
      case CSV:
        return generateCsvReport(relevantAuditEvents, relevantAdminAuditEvents);
      case PDF:
        return generatePdfReport(relevantAuditEvents, relevantAdminAuditEvents);
      default:
        throw new IllegalArgumentException("Unsupported report type: " + reportType);
    }
  }

  /** Generates a JSON compliance report. */
  private Path generateJsonReport(
      List<FaceRecognitionAuditEvent> auditEvents,
      List<FaceRecognitionAdminAuditEvent> adminAuditEvents) {
    try {
      ObjectNode reportNode = objectMapper.createObjectNode();
      reportNode.put("reportGeneratedAt", Instant.now().toString());
      reportNode.put("reportType", "Compliance Report");

      // Add user authentication events
      ArrayNode userEventsNode = reportNode.putArray("userAuthenticationEvents");
      for (FaceRecognitionAuditEvent event : auditEvents) {
        ObjectNode eventNode = objectMapper.createObjectNode();
        eventNode.put("timestamp", formatTimestamp(event.getTimestamp()));
        eventNode.put("eventType", event.getEventType());
        eventNode.put("userId", event.getUserId());
        eventNode.put("realmId", event.getRealmId());
        eventNode.put("clientId", event.getClientId());
        eventNode.put("ipAddress", event.getIpAddress());
        eventNode.put("sessionId", event.getSessionId());
        eventNode.put("auditCategory", event.getAuditCategory());
        userEventsNode.add(eventNode);
      }

      // Add admin events
      ArrayNode adminEventsNode = reportNode.putArray("adminEvents");
      for (FaceRecognitionAdminAuditEvent event : adminAuditEvents) {
        ObjectNode eventNode = objectMapper.createObjectNode();
        eventNode.put("timestamp", formatTimestamp(event.getTimestamp()));
        eventNode.put("operationType", event.getOperationType());
        eventNode.put("resourceType", event.getResourceType());
        eventNode.put("resourcePath", event.getResourcePath());
        eventNode.put("adminUserId", event.getAdminUserId());
        eventNode.put("adminUsername", event.getAdminUsername());
        eventNode.put("realmId", event.getRealmId());
        eventNode.put("ipAddress", event.getIpAddress());
        eventNode.put("auditCategory", event.getAuditCategory());
        adminEventsNode.add(eventNode);
      }

      // Add summary statistics
      ObjectNode statsNode = reportNode.putObject("statistics");
      statsNode.put("totalUserEvents", auditEvents.size());
      statsNode.put("totalAdminEvents", adminAuditEvents.size());

      // Count events by type
      Map<String, Long> eventTypeCounts =
          auditEvents.stream()
              .collect(
                  Collectors.groupingBy(
                      FaceRecognitionAuditEvent::getEventType, Collectors.counting()));

      ObjectNode eventTypeNode = statsNode.putObject("eventTypeCounts");
      for (Map.Entry<String, Long> entry : eventTypeCounts.entrySet()) {
        eventTypeNode.put(entry.getKey(), entry.getValue());
      }

      // Write to file
      String filename =
          "compliance_report_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".json";
      Path reportPath = reportDirectory.resolve(filename);
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), reportNode);

      logger.info("Generated JSON compliance report: " + reportPath);
      return reportPath;

    } catch (IOException e) {
      logger.error("Failed to generate JSON compliance report", e);
      throw new RuntimeException("Failed to generate JSON compliance report", e);
    }
  }

  /** Generates a CSV compliance report. */
  private Path generateCsvReport(
      List<FaceRecognitionAuditEvent> auditEvents,
      List<FaceRecognitionAdminAuditEvent> adminAuditEvents) {
    try {
      StringBuilder csvBuilder = new StringBuilder();

      // Add header
      csvBuilder.append("Report Type,Compliance Report\n");
      csvBuilder.append("Generated At,").append(Instant.now()).append("\n\n");

      // User events section
      csvBuilder.append("USER AUTHENTICATION EVENTS\n");
      csvBuilder.append(
          "Timestamp,Event Type,User ID,Realm ID,Client ID,IP Address,Session ID,Audit Category\n");

      for (FaceRecognitionAuditEvent event : auditEvents) {
        csvBuilder.append(formatTimestamp(event.getTimestamp())).append(",");
        csvBuilder.append(escapeCSV(event.getEventType())).append(",");
        csvBuilder.append(escapeCSV(event.getUserId())).append(",");
        csvBuilder.append(escapeCSV(event.getRealmId())).append(",");
        csvBuilder.append(escapeCSV(event.getClientId())).append(",");
        csvBuilder.append(escapeCSV(event.getIpAddress())).append(",");
        csvBuilder.append(escapeCSV(event.getSessionId())).append(",");
        csvBuilder.append(escapeCSV(event.getAuditCategory())).append("\n");
      }

      csvBuilder.append("\n");

      // Admin events section
      csvBuilder.append("ADMIN EVENTS\n");
      csvBuilder.append(
          "Timestamp,Operation Type,Resource Type,Resource Path,Admin User ID,Admin Username,Realm ID,IP Address,Audit Category\n");

      for (FaceRecognitionAdminAuditEvent event : adminAuditEvents) {
        csvBuilder.append(formatTimestamp(event.getTimestamp())).append(",");
        csvBuilder.append(escapeCSV(event.getOperationType())).append(",");
        csvBuilder.append(escapeCSV(event.getResourceType())).append(",");
        csvBuilder.append(escapeCSV(event.getResourcePath())).append(",");
        csvBuilder.append(escapeCSV(event.getAdminUserId())).append(",");
        csvBuilder.append(escapeCSV(event.getAdminUsername())).append(",");
        csvBuilder.append(escapeCSV(event.getRealmId())).append(",");
        csvBuilder.append(escapeCSV(event.getIpAddress())).append(",");
        csvBuilder.append(escapeCSV(event.getAuditCategory())).append("\n");
      }

      csvBuilder.append("\n");

      // Statistics section
      csvBuilder.append("STATISTICS\n");
      csvBuilder.append("Total User Events,").append(auditEvents.size()).append("\n");
      csvBuilder.append("Total Admin Events,").append(adminAuditEvents.size()).append("\n\n");

      // Event type counts
      csvBuilder.append("EVENT TYPE COUNTS\n");
      csvBuilder.append("Event Type,Count\n");

      Map<String, Long> eventTypeCounts =
          auditEvents.stream()
              .collect(
                  Collectors.groupingBy(
                      FaceRecognitionAuditEvent::getEventType, Collectors.counting()));

      for (Map.Entry<String, Long> entry : eventTypeCounts.entrySet()) {
        csvBuilder
            .append(escapeCSV(entry.getKey()))
            .append(",")
            .append(entry.getValue())
            .append("\n");
      }

      // Write to file
      String filename =
          "compliance_report_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".csv";
      Path reportPath = reportDirectory.resolve(filename);
      Files.write(reportPath, csvBuilder.toString().getBytes());

      logger.info("Generated CSV compliance report: " + reportPath);
      return reportPath;

    } catch (IOException e) {
      logger.error("Failed to generate CSV compliance report", e);
      throw new RuntimeException("Failed to generate CSV compliance report", e);
    }
  }

  /**
   * Generates a PDF compliance report. Note: This is a placeholder implementation. In a real
   * implementation, you would use a PDF library like iText or Apache PDFBox.
   */
  private Path generatePdfReport(
      List<FaceRecognitionAuditEvent> auditEvents,
      List<FaceRecognitionAdminAuditEvent> adminAuditEvents) {
    // For now, we'll just generate a text file with a .pdf extension as a placeholder
    try {
      StringBuilder textBuilder = new StringBuilder();

      textBuilder.append("COMPLIANCE REPORT\n");
      textBuilder.append("Generated At: ").append(Instant.now()).append("\n\n");

      textBuilder
          .append("USER AUTHENTICATION EVENTS (Total: ")
          .append(auditEvents.size())
          .append(")\n");
      for (FaceRecognitionAuditEvent event : auditEvents) {
        textBuilder
            .append("  - ")
            .append(formatTimestamp(event.getTimestamp()))
            .append(" | ")
            .append(event.getEventType())
            .append(" | User: ")
            .append(event.getUserId())
            .append(" | IP: ")
            .append(event.getIpAddress())
            .append("\n");
      }

      textBuilder.append("\nADMIN EVENTS (Total: ").append(adminAuditEvents.size()).append(")\n");
      for (FaceRecognitionAdminAuditEvent event : adminAuditEvents) {
        textBuilder
            .append("  - ")
            .append(formatTimestamp(event.getTimestamp()))
            .append(" | ")
            .append(event.getOperationType())
            .append(" | Admin: ")
            .append(event.getAdminUserId())
            .append(" | IP: ")
            .append(event.getIpAddress())
            .append("\n");
      }

      // Write to file
      String filename =
          "compliance_report_" + LocalDateTime.now().format(FILE_DATE_FORMAT) + ".pdf";
      Path reportPath = reportDirectory.resolve(filename);
      Files.write(reportPath, textBuilder.toString().getBytes());

      logger.info("Generated PDF compliance report: " + reportPath);
      return reportPath;

    } catch (IOException e) {
      logger.error("Failed to generate PDF compliance report", e);
      throw new RuntimeException("Failed to generate PDF compliance report", e);
    }
  }

  /** Formats a timestamp into a human-readable date and time. */
  private String formatTimestamp(long timestamp) {
    LocalDateTime dateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    return dateTime.format(DATE_FORMAT) + " " + dateTime.format(TIME_FORMAT);
  }

  /** Escapes CSV values to handle commas and quotes. */
  private String escapeCSV(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  /** Ensures that the specified directory exists, creating it if necessary. */
  private void ensureDirectoryExists(Path directory) {
    try {
      if (!Files.exists(directory)) {
        Files.createDirectories(directory);
        logger.info("Created report directory: " + directory);
      }
    } catch (IOException e) {
      logger.error("Failed to create report directory: " + directory, e);
      throw new RuntimeException("Failed to create report directory", e);
    }
  }
}
