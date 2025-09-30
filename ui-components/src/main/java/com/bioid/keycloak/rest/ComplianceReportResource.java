package com.bioid.keycloak.rest;

import com.bioid.keycloak.compliance.ComplianceReportService;
import com.bioid.keycloak.compliance.ComplianceReportType;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** REST resource for generating compliance reports. */
public class ComplianceReportResource {

  private static final Logger logger = LoggerFactory.getLogger(ComplianceReportResource.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final KeycloakSession session;
  private final AuthenticationManager.AuthResult auth;
  private final ComplianceReportService reportService;

  public ComplianceReportResource(KeycloakSession session) {
    this.session = session;
    this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    this.reportService = new ComplianceReportService(session);
  }

  /**
   * Generates a compliance report for the specified date range.
   *
   * @param startDate The start date in format yyyy-MM-dd
   * @param endDate The end date in format yyyy-MM-dd
   * @param format The report format (json, csv, pdf)
   * @return A response with the generated report
   */
  @GET
  @Path("/generate")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response generateReport(
      @QueryParam("startDate") String startDate,
      @QueryParam("endDate") String endDate,
      @QueryParam("format") @DefaultValue("json") String format) {
    // Check authentication
    if (auth == null) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    // Check admin permissions (simplified check)
    if (auth.getUser() == null
        || !auth.getUser().hasRole(session.getContext().getRealm().getRole("admin"))) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity("Only admin users can generate compliance reports")
          .build();
    }

    try {
      // Parse dates
      LocalDate start = LocalDate.parse(startDate, DATE_FORMATTER);
      LocalDate end = LocalDate.parse(endDate, DATE_FORMATTER);

      // Validate date range
      if (start.isAfter(end)) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("Start date must be before or equal to end date")
            .build();
      }

      // Parse report format
      ComplianceReportType reportType;
      switch (format.toLowerCase()) {
        case "json":
          reportType = ComplianceReportType.JSON;
          break;
        case "csv":
          reportType = ComplianceReportType.CSV;
          break;
        case "pdf":
          reportType = ComplianceReportType.PDF;
          break;
        default:
          return Response.status(Response.Status.BAD_REQUEST)
              .entity("Invalid format. Supported formats: json, csv, pdf")
              .build();
      }

      // Generate the report
      java.nio.file.Path reportPath = reportService.generateReport(start, end, reportType);
      File reportFile = reportPath.toFile();

      // Determine content type based on format
      String contentType;
      switch (reportType) {
        case JSON:
          contentType = "application/json";
          break;
        case CSV:
          contentType = "text/csv";
          break;
        case PDF:
          contentType = "application/pdf";
          break;
        default:
          contentType = "application/octet-stream";
      }

      // Return the report file for download
      return Response.ok(reportFile)
          .header("Content-Disposition", "attachment; filename=" + reportFile.getName())
          .header("Content-Type", contentType)
          .build();

    } catch (DateTimeParseException e) {
      logger.error("Invalid date format", e);
      return Response.status(Response.Status.BAD_REQUEST)
          .entity("Invalid date format. Use yyyy-MM-dd")
          .build();
    } catch (Exception e) {
      logger.error("Error generating compliance report", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error generating compliance report: " + e.getMessage())
          .build();
    }
  }

  /**
   * Generates a monthly compliance report.
   *
   * @param year The year
   * @param month The month (1-12)
   * @param format The report format (json, csv, pdf)
   * @return A response with the generated report
   */
  @GET
  @Path("/monthly/{year}/{month}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response generateMonthlyReport(
      @PathParam("year") int year,
      @PathParam("month") int month,
      @QueryParam("format") @DefaultValue("json") String format) {
    // Check authentication
    if (auth == null) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    // Check admin permissions (simplified check)
    if (auth.getUser() == null
        || !auth.getUser().hasRole(session.getContext().getRealm().getRole("admin"))) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity("Only admin users can generate compliance reports")
          .build();
    }

    try {
      // Validate month
      if (month < 1 || month > 12) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("Month must be between 1 and 12")
            .build();
      }

      // Parse report format
      ComplianceReportType reportType;
      switch (format.toLowerCase()) {
        case "json":
          reportType = ComplianceReportType.JSON;
          break;
        case "csv":
          reportType = ComplianceReportType.CSV;
          break;
        case "pdf":
          reportType = ComplianceReportType.PDF;
          break;
        default:
          return Response.status(Response.Status.BAD_REQUEST)
              .entity("Invalid format. Supported formats: json, csv, pdf")
              .build();
      }

      // Generate the report
      java.nio.file.Path reportPath = reportService.generateMonthlyReport(year, month, reportType);
      File reportFile = reportPath.toFile();

      // Determine content type based on format
      String contentType;
      switch (reportType) {
        case JSON:
          contentType = "application/json";
          break;
        case CSV:
          contentType = "text/csv";
          break;
        case PDF:
          contentType = "application/pdf";
          break;
        default:
          contentType = "application/octet-stream";
      }

      // Return the report file for download
      return Response.ok(reportFile)
          .header("Content-Disposition", "attachment; filename=" + reportFile.getName())
          .header("Content-Type", contentType)
          .build();

    } catch (Exception e) {
      logger.error("Error generating monthly compliance report", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error generating monthly compliance report: " + e.getMessage())
          .build();
    }
  }

  /**
   * Generates a compliance report for the current month.
   *
   * @param format The report format (json, csv, pdf)
   * @return A response with the generated report
   */
  @GET
  @Path("/monthly/current")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  public Response generateCurrentMonthReport(
      @QueryParam("format") @DefaultValue("json") String format) {
    // Check authentication
    if (auth == null) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    // Check admin permissions (simplified check)
    if (auth.getUser() == null
        || !auth.getUser().hasRole(session.getContext().getRealm().getRole("admin"))) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity("Only admin users can generate compliance reports")
          .build();
    }

    try {
      // Parse report format
      ComplianceReportType reportType;
      switch (format.toLowerCase()) {
        case "json":
          reportType = ComplianceReportType.JSON;
          break;
        case "csv":
          reportType = ComplianceReportType.CSV;
          break;
        case "pdf":
          reportType = ComplianceReportType.PDF;
          break;
        default:
          return Response.status(Response.Status.BAD_REQUEST)
              .entity("Invalid format. Supported formats: json, csv, pdf")
              .build();
      }

      // Generate the report
      java.nio.file.Path reportPath = reportService.generateMonthlyReport(reportType);
      File reportFile = reportPath.toFile();

      // Determine content type based on format
      String contentType;
      switch (reportType) {
        case JSON:
          contentType = "application/json";
          break;
        case CSV:
          contentType = "text/csv";
          break;
        case PDF:
          contentType = "application/pdf";
          break;
        default:
          contentType = "application/octet-stream";
      }

      // Return the report file for download
      return Response.ok(reportFile)
          .header("Content-Disposition", "attachment; filename=" + reportFile.getName())
          .header("Content-Type", contentType)
          .build();

    } catch (Exception e) {
      logger.error("Error generating current month compliance report", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Error generating current month compliance report: " + e.getMessage())
          .build();
    }
  }
}
