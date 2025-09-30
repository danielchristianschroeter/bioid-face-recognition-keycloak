package com.bioid.keycloak.compliance;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.keycloak.models.KeycloakSession;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ComplianceReportServiceTest {

  @TempDir Path tempDir;

  @Mock private KeycloakSession session;

  private ComplianceReportService reportService;

  @BeforeEach
  void setUp() {
    // Set the report directory system property for testing
    System.setProperty("bioid.reports.directory", tempDir.toString());

    reportService = new ComplianceReportService(session);
  }

  @Test
  void shouldGenerateReportForDateRange() {
    // Given
    LocalDate startDate = LocalDate.now().minusDays(7);
    LocalDate endDate = LocalDate.now();

    // When
    Path reportPath = reportService.generateReport(startDate, endDate, ComplianceReportType.JSON);

    // Then
    assertTrue(Files.exists(reportPath), "Report file should exist");
    assertTrue(reportPath.toString().endsWith(".json"), "Report should have JSON extension");
  }

  @Test
  void shouldGenerateMonthlyReport() {
    // When
    Path reportPath = reportService.generateMonthlyReport(ComplianceReportType.JSON);

    // Then
    assertTrue(Files.exists(reportPath), "Report file should exist");
    assertTrue(reportPath.toString().endsWith(".json"), "Report should have JSON extension");
  }

  @Test
  void shouldGenerateMonthlyReportForSpecificMonth() {
    // Given
    int year = 2025;
    int month = 7; // July

    // When
    Path reportPath = reportService.generateMonthlyReport(year, month, ComplianceReportType.JSON);

    // Then
    assertTrue(Files.exists(reportPath), "Report file should exist");
    assertTrue(reportPath.toString().endsWith(".json"), "Report should have JSON extension");
  }

  @Test
  void shouldReturnCorrectReportDirectory() {
    // When
    Path reportDir = reportService.getReportDirectory();

    // Then
    assertEquals(
        tempDir.toString(),
        reportDir.toString(),
        "Report directory should match the configured directory");
  }

  @Test
  void shouldGenerateReportsInDifferentFormats() {
    // Given
    LocalDate startDate = LocalDate.now().minusDays(1);
    LocalDate endDate = LocalDate.now();

    // When
    Path jsonReport = reportService.generateReport(startDate, endDate, ComplianceReportType.JSON);
    Path csvReport = reportService.generateReport(startDate, endDate, ComplianceReportType.CSV);
    Path pdfReport = reportService.generateReport(startDate, endDate, ComplianceReportType.PDF);

    // Then
    assertTrue(Files.exists(jsonReport), "JSON report should exist");
    assertTrue(Files.exists(csvReport), "CSV report should exist");
    assertTrue(Files.exists(pdfReport), "PDF report should exist");

    assertTrue(
        jsonReport.toString().endsWith(".json"), "JSON report should have correct extension");
    assertTrue(csvReport.toString().endsWith(".csv"), "CSV report should have correct extension");
    assertTrue(pdfReport.toString().endsWith(".pdf"), "PDF report should have correct extension");
  }
}
