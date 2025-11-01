package com.bioid.keycloak.admin.rest;

import com.bioid.keycloak.admin.security.AdminSecurityConfig;
import com.bioid.keycloak.admin.service.BWSAdminService;
import com.bioid.keycloak.admin.service.TemplateInfo;
import com.bioid.keycloak.admin.service.ValidationResult;
import com.bioid.keycloak.admin.service.AdminStats;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * REST API for BWS face template administration.
 * 
 * Provides endpoints for administrators with the 'bws-admin' role to:
 * - View statistics about enrolled templates
 * - List all enrolled face templates across users
 * - Validate consistency between Keycloak and BWS
 * - Find and delete orphaned templates
 * 
 * Security:
 * - All endpoints require authentication
 * - All endpoints require 'bws-admin' realm role
 * - All actions are audit logged
 * - Rate limiting applied (configured in properties)
 * 
 * @author BioID Keycloak Extension
 * @version 1.0.0
 */
@Path("/")
public class BWSAdminResource {

  private static final Logger logger = LoggerFactory.getLogger(BWSAdminResource.class);
  
  private final KeycloakSession session;
  private final BWSAdminService adminService;
  private final AdminSecurityConfig securityConfig;

  public BWSAdminResource(KeycloakSession session) {
    this.session = session;
    this.adminService = new BWSAdminService(session);
    this.securityConfig = new AdminSecurityConfig(session);
  }

  /**
   * Get template statistics.
   * 
   * Returns aggregated statistics about enrolled face templates including:
   * - Total number of templates
   * - Number of active users with face credentials
   * - Number of orphaned templates
   * - Last enrollment timestamp
   * 
   * @return statistics response
   */
  @GET
  @Path("/stats")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStats() {
    logger.info("GET /bws-admin/stats called");

    try {
      // Authenticate and authorize
      UserModel admin = authenticateAndAuthorize();
      if (admin == null) {
        return unauthorizedResponse();
      }

      // Get statistics
      AdminStats stats = adminService.getStatistics();
      
      // Audit log
      adminService.auditLog(admin.getId(), "VIEW_STATS", null);
      
      logger.info("Admin {} viewed statistics: {} total templates", 
          admin.getUsername(), stats.getTotalTemplates());

      return Response.ok(stats)
          .header("Access-Control-Allow-Origin", "*")
          .build();

    } catch (Exception e) {
      logger.error("Error getting admin stats", e);
      return errorResponse("Failed to get statistics: " + e.getMessage());
    }
  }

  /**
   * List all enrolled face templates.
   * 
   * Returns a list of all face templates with user information including:
   * - Class ID
   * - Username and email
   * - Enrollment date
   * - Status (active or orphaned)
   * 
   * @return list of template information
   */
  @GET
  @Path("/templates")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listAllTemplates() {
    logger.info("GET /bws-admin/templates called");

    try {
      // Authenticate and authorize
      UserModel admin = authenticateAndAuthorize();
      if (admin == null) {
        return unauthorizedResponse();
      }

      // Get all templates
      List<TemplateInfo> templates = adminService.listAllTemplates();
      
      // Audit log
      adminService.auditLog(admin.getId(), "LIST_TEMPLATES", 
          Map.of("count", templates.size()));
      
      logger.info("Admin {} listed {} templates", admin.getUsername(), templates.size());

      return Response.ok(templates)
          .header("Access-Control-Allow-Origin", "*")
          .build();

    } catch (Exception e) {
      logger.error("Error listing templates", e);
      return errorResponse("Failed to list templates: " + e.getMessage());
    }
  }

  /**
   * Get details for a specific template.
   * 
   * @param classId the class ID of the template
   * @return template details
   */
  @GET
  @Path("/templates/{classId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTemplateDetails(@PathParam("classId") String classId) {
    logger.info("GET /bws-admin/templates/{} called", classId);

    try {
      // Authenticate and authorize
      UserModel admin = authenticateAndAuthorize();
      if (admin == null) {
        return unauthorizedResponse();
      }

      // Validate class ID
      long classIdLong;
      try {
        classIdLong = Long.parseLong(classId);
      } catch (NumberFormatException e) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", "Invalid class ID format"))
            .header("Access-Control-Allow-Origin", "*")
            .build();
      }

      // Get template details
      TemplateInfo template = adminService.getTemplateDetails(classIdLong);
      
      if (template == null) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(Map.of("error", "Template not found"))
            .header("Access-Control-Allow-Origin", "*")
            .build();
      }
      
      // Audit log
      adminService.auditLog(admin.getId(), "VIEW_TEMPLATE", 
          Map.of("classId", classId));
      
      logger.info("Admin {} viewed template {}", admin.getUsername(), classId);

      return Response.ok(template)
          .header("Access-Control-Allow-Origin", "*")
          .build();

    } catch (Exception e) {
      logger.error("Error getting template details for classId: {}", classId, e);
      return errorResponse("Failed to get template details: " + e.getMessage());
    }
  }

  /**
   * Delete a face template.
   * 
   * Deletes the template from both BWS and Keycloak.
   * This operation is irreversible.
   * 
   * @param classId the class ID of the template to delete
   * @return deletion result
   */
  @DELETE
  @Path("/templates/{classId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteTemplate(@PathParam("classId") String classId) {
    logger.info("DELETE /bws-admin/templates/{} called", classId);

    try {
      // Authenticate and authorize
      UserModel admin = authenticateAndAuthorize();
      if (admin == null) {
        return unauthorizedResponse();
      }

      // Validate class ID
      long classIdLong;
      try {
        classIdLong = Long.parseLong(classId);
      } catch (NumberFormatException e) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", "Invalid class ID format"))
            .header("Access-Control-Allow-Origin", "*")
            .build();
      }

      // Delete template
      boolean deleted = adminService.deleteTemplate(classIdLong);
      
      if (!deleted) {
        return Response.status(Response.Status.NOT_FOUND)
            .entity(Map.of("error", "Template not found or already deleted"))
            .header("Access-Control-Allow-Origin", "*")
            .build();
      }
      
      // Audit log
      adminService.auditLog(admin.getId(), "DELETE_TEMPLATE", 
          Map.of("classId", classId));
      
      logger.info("Admin {} deleted template {}", admin.getUsername(), classId);

      return Response.ok(Map.of(
          "success", true,
          "message", "Template deleted successfully",
          "classId", classId
      ))
          .header("Access-Control-Allow-Origin", "*")
          .build();

    } catch (Exception e) {
      logger.error("Error deleting template: {}", classId, e);
      return errorResponse("Failed to delete template: " + e.getMessage());
    }
  }

  /**
   * Validate template consistency.
   * 
   * Checks for inconsistencies between Keycloak and BWS including:
   * - Orphaned templates (in BWS but not in Keycloak)
   * - Missing templates (in Keycloak but not in BWS)
   * 
   * @return validation result
   */
  @GET
  @Path("/validate")
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateTemplates() {
    logger.info("GET /bws-admin/validate called");

    try {
      // Authenticate and authorize
      UserModel admin = authenticateAndAuthorize();
      if (admin == null) {
        return unauthorizedResponse();
      }

      // Validate templates
      ValidationResult result = adminService.validateTemplates();
      
      // Audit log
      adminService.auditLog(admin.getId(), "VALIDATE_TEMPLATES", 
          Map.of("isValid", result.isValid(), 
                 "orphanedCount", result.getOrphanedTemplates().size()));
      
      logger.info("Admin {} validated templates: {} orphaned, {} missing", 
          admin.getUsername(), 
          result.getOrphanedTemplates().size(),
          result.getMissingTemplates().size());

      return Response.ok(result)
          .header("Access-Control-Allow-Origin", "*")
          .build();

    } catch (Exception e) {
      logger.error("Error validating templates", e);
      return errorResponse("Failed to validate templates: " + e.getMessage());
    }
  }

  /**
   * Find orphaned templates.
   * 
   * Returns templates that exist in BWS but have no corresponding Keycloak user.
   * These templates can be safely deleted.
   * 
   * @return list of orphaned templates
   */
  @GET
  @Path("/orphaned")
  @Produces(MediaType.APPLICATION_JSON)
  public Response findOrphanedTemplates() {
    logger.info("GET /bws-admin/orphaned called");

    try {
      // Authenticate and authorize
      UserModel admin = authenticateAndAuthorize();
      if (admin == null) {
        return unauthorizedResponse();
      }

      // Find orphaned templates
      List<TemplateInfo> orphaned = adminService.findOrphanedTemplates();
      
      // Audit log
      adminService.auditLog(admin.getId(), "FIND_ORPHANED", 
          Map.of("count", orphaned.size()));
      
      logger.info("Admin {} found {} orphaned templates", 
          admin.getUsername(), orphaned.size());

      return Response.ok(orphaned)
          .header("Access-Control-Allow-Origin", "*")
          .build();

    } catch (Exception e) {
      logger.error("Error finding orphaned templates", e);
      return errorResponse("Failed to find orphaned templates: " + e.getMessage());
    }
  }

  /**
   * Delete all orphaned templates.
   * 
   * Bulk deletes all templates that exist in BWS but have no Keycloak user.
   * This operation is irreversible.
   * 
   * @return deletion result with count
   */
  @DELETE
  @Path("/orphaned")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteAllOrphaned() {
    logger.info("DELETE /bws-admin/orphaned called");

    try {
      // Authenticate and authorize
      UserModel admin = authenticateAndAuthorize();
      if (admin == null) {
        return unauthorizedResponse();
      }

      // Delete all orphaned templates
      int deletedCount = adminService.deleteAllOrphaned();
      
      // Audit log
      adminService.auditLog(admin.getId(), "DELETE_ALL_ORPHANED", 
          Map.of("deletedCount", deletedCount));
      
      logger.info("Admin {} deleted {} orphaned templates", 
          admin.getUsername(), deletedCount);

      return Response.ok(Map.of(
          "success", true,
          "deletedCount", deletedCount,
          "message", "Deleted " + deletedCount + " orphaned template(s)"
      ))
          .header("Access-Control-Allow-Origin", "*")
          .build();

    } catch (Exception e) {
      logger.error("Error deleting orphaned templates", e);
      return errorResponse("Failed to delete orphaned templates: " + e.getMessage());
    }
  }

  /**
   * CORS preflight handler for stats endpoint.
   */
  @OPTIONS
  @Path("/stats")
  public Response statsOptions() {
    return corsResponse();
  }

  /**
   * CORS preflight handler for templates endpoint.
   */
  @OPTIONS
  @Path("/templates")
  public Response templatesOptions() {
    return corsResponse();
  }

  /**
   * CORS preflight handler for specific template endpoint.
   */
  @OPTIONS
  @Path("/templates/{classId}")
  public Response templateOptions() {
    return corsResponse();
  }

  /**
   * CORS preflight handler for validate endpoint.
   */
  @OPTIONS
  @Path("/validate")
  public Response validateOptions() {
    return corsResponse();
  }

  /**
   * CORS preflight handler for orphaned endpoint.
   */
  @OPTIONS
  @Path("/orphaned")
  public Response orphanedOptions() {
    return corsResponse();
  }

  // ============================================================================
  // Private Helper Methods
  // ============================================================================

  /**
   * Authenticates the request and checks for bws-admin role.
   * 
   * Security checks performed:
   * 1. Valid bearer token authentication
   * 2. User account is enabled
   * 3. User has bws-admin realm role
   * 4. Token is not expired
   * 5. Realm context matches
   * 
   * @return authenticated user if authorized, null otherwise
   */
  private UserModel authenticateAndAuthorize() {
    // Authenticate the user with bearer token
    AuthenticationManager.AuthResult authResult = 
        new AppAuthManager.BearerTokenAuthenticator(session).authenticate();

    if (authResult == null) {
      logger.warn("Unauthenticated request to BWS admin API from IP: {}", 
          getClientIpAddress());
      return null;
    }

    UserModel user = authResult.getUser();
    
    // Security check: User must be enabled
    if (!user.isEnabled()) {
      logger.warn("Disabled user {} attempted to access BWS admin API", 
          user.getUsername());
      return null;
    }
    
    // Security check: Verify realm context
    String expectedRealm = session.getContext().getRealm().getName();
    String tokenRealm = authResult.getSession().getRealm().getName();
    if (!expectedRealm.equals(tokenRealm)) {
      logger.error("Realm mismatch: expected {}, got {} for user {}", 
          expectedRealm, tokenRealm, user.getUsername());
      return null;
    }
    
    // Security check: IP address must be allowed (if IP filtering is configured)
    String clientIp = getClientIpAddress();
    if (!securityConfig.isIpAllowed(clientIp)) {
      logger.warn("User {} attempted to access BWS admin API from blocked IP: {}", 
          user.getUsername(), clientIp);
      return null;
    }
    
    // Security check: User must have the configured admin role
    String adminRoleName = securityConfig.getAdminRoleName();
    RoleModel adminRole = session.getContext().getRealm().getRole(adminRoleName);
    if (adminRole == null) {
      logger.error("{} role not found in realm {}", adminRoleName, expectedRealm);
      return null;
    }

    if (!user.hasRole(adminRole)) {
      logger.warn("User {} (ID: {}) attempted to access BWS admin API without {} role from IP: {}", 
          user.getUsername(), user.getId(), adminRoleName, clientIp);
      // Audit log unauthorized access attempt
      if (securityConfig.isAuditEnabled()) {
        try {
          adminService.auditLog(user.getId(), "UNAUTHORIZED_ACCESS_ATTEMPT", 
              Map.of("ip", clientIp, "role", "missing", "requiredRole", adminRoleName));
        } catch (Exception e) {
          logger.error("Failed to audit unauthorized access attempt", e);
        }
      }
      return null;
    }

    // Log successful authentication
    logger.debug("User {} successfully authenticated for BWS admin API", 
        user.getUsername());

    return user;
  }
  
  /**
   * Gets the client IP address from the request context.
   * 
   * @return client IP address or "unknown"
   */
  private String getClientIpAddress() {
    try {
      var connection = session.getContext().getConnection();
      if (connection != null) {
        return connection.getRemoteAddr();
      }
    } catch (Exception e) {
      logger.debug("Could not determine client IP", e);
    }
    return "unknown";
  }

  /**
   * Creates an unauthorized response.
   */
  private Response unauthorizedResponse() {
    return Response.status(Response.Status.FORBIDDEN)
        .entity(Map.of(
            "error", "Access denied",
            "message", securityConfig.getAdminRoleName() + " role required"
        ))
        .header("Access-Control-Allow-Origin", "*")
        .build();
  }

  /**
   * Creates an error response.
   */
  private Response errorResponse(String message) {
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(Map.of("error", message))
        .header("Access-Control-Allow-Origin", "*")
        .build();
  }

  /**
   * Creates a CORS preflight response.
   */
  private Response corsResponse() {
    return Response.ok()
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
        .header("Access-Control-Max-Age", "3600")
        .build();
  }
}
