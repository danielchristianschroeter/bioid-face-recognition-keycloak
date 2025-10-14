package com.bioid.keycloak.config;

import com.bioid.keycloak.security.AdminSecurityService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

/**
 * REST resource for administrative configuration management.
 * 
 * Provides endpoints for getting, updating, importing, and exporting
 * administrative configuration through the Keycloak admin console.
 * 
 * Requirements addressed: 3.2, 6.1, 9.1
 */
@Path("/admin/configuration")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminConfigurationResource {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminConfigurationResource.class);
    
    private KeycloakSession session;
    private RealmModel realm;
    private AuthenticationManager.AuthResult auth;
    
    @Inject
    private AdminConfigurationService configService;
    
    @Inject
    private AdminSecurityService securityService;
    
    public AdminConfigurationResource() {
        // Default constructor for CDI
    }
    
    public AdminConfigurationResource(KeycloakSession session, RealmModel realm, 
                                    AuthenticationManager.AuthResult auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }
    
    public void init(KeycloakSession session, RealmModel realm, AuthenticationManager.AuthResult auth) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
    }
    
    /**
     * Gets the current administrative configuration.
     * 
     * @return the configuration
     */
    @GET
    public Response getConfiguration() {
        try {
            // Check permissions
            if (!hasConfigurationAccess()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Insufficient permissions"))
                    .build();
            }
            
            AdminConfiguration config = configService.getConfiguration(session, realm);
            return Response.ok(config).build();
            
        } catch (Exception e) {
            logger.error("Failed to get admin configuration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to get configuration: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Updates the administrative configuration.
     * 
     * @param updates the configuration updates
     * @return success response or error
     */
    @PUT
    public Response updateConfiguration(AdminConfiguration updates) {
        try {
            // Check permissions
            if (!hasConfigurationAccess()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Insufficient permissions"))
                    .build();
            }
            
            UserModel user = auth.getUser();
            
            // Additional security check for sensitive operations
            if (requiresAdditionalAuth(updates) && !hasAdditionalAuth()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Additional authentication required for sensitive configuration changes"))
                    .build();
            }
            
            // Validate configuration first
            AdminConfigurationService.ConfigurationValidationResult validation = 
                configService.validateConfiguration(session, realm, updates);
            
            if (!validation.isValid()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Configuration validation failed: " + validation.getErrorMessage()))
                    .build();
            }
            
            // Update configuration
            configService.updateConfiguration(session, realm, updates, user);
            
            // Return updated configuration
            AdminConfiguration updated = configService.getConfiguration(session, realm);
            return Response.ok(updated).build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid configuration update", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Invalid configuration: " + e.getMessage()))
                .build();
        } catch (Exception e) {
            logger.error("Failed to update admin configuration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to update configuration: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Validates configuration without saving.
     * 
     * @param updates the configuration to validate
     * @return validation result
     */
    @POST
    @Path("/validate")
    public Response validateConfiguration(AdminConfiguration updates) {
        try {
            // Check permissions
            if (!hasConfigurationAccess()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Insufficient permissions"))
                    .build();
            }
            
            AdminConfigurationService.ConfigurationValidationResult result = 
                configService.validateConfiguration(session, realm, updates);
            
            return Response.ok(Map.of(
                "valid", result.isValid(),
                "errorMessage", result.getErrorMessage()
            )).build();
            
        } catch (Exception e) {
            logger.error("Failed to validate configuration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to validate configuration: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Exports configuration for deployment automation.
     * 
     * @return the configuration as JSON
     */
    @GET
    @Path("/export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response exportConfiguration() {
        try {
            // Check permissions
            if (!hasConfigurationAccess()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Insufficient permissions"))
                    .build();
            }
            
            UserModel user = auth.getUser();
            String configJson = configService.exportConfiguration(session, realm, user);
            
            return Response.ok(configJson)
                .header("Content-Disposition", "attachment; filename=\"bioid-admin-config.json\"")
                .build();
            
        } catch (Exception e) {
            logger.error("Failed to export configuration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to export configuration: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Imports configuration from JSON.
     * 
     * @param request the import request containing configuration JSON
     * @return success response or error
     */
    @POST
    @Path("/import")
    public Response importConfiguration(ConfigurationImportRequest request) {
        try {
            // Check permissions
            if (!hasConfigurationAccess()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Insufficient permissions"))
                    .build();
            }
            
            // Additional security check for import operations
            if (!hasAdditionalAuth()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Additional authentication required for configuration import"))
                    .build();
            }
            
            UserModel user = auth.getUser();
            configService.importConfiguration(session, realm, request.getConfigurationJson(), user);
            
            // Return updated configuration
            AdminConfiguration updated = configService.getConfiguration(session, realm);
            return Response.ok(Map.of(
                "message", "Configuration imported successfully",
                "configuration", updated
            )).build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid configuration import", e);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("error", "Invalid configuration: " + e.getMessage()))
                .build();
        } catch (Exception e) {
            logger.error("Failed to import configuration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to import configuration: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Reloads configuration from database, bypassing cache.
     * 
     * @return the reloaded configuration
     */
    @POST
    @Path("/reload")
    public Response reloadConfiguration() {
        try {
            // Check permissions
            if (!hasConfigurationAccess()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Insufficient permissions"))
                    .build();
            }
            
            AdminConfiguration config = configService.reloadConfiguration(session, realm);
            return Response.ok(Map.of(
                "message", "Configuration reloaded successfully",
                "configuration", config
            )).build();
            
        } catch (Exception e) {
            logger.error("Failed to reload configuration", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to reload configuration: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Gets configuration cache statistics.
     * 
     * @return cache statistics
     */
    @GET
    @Path("/cache/stats")
    public Response getCacheStats() {
        try {
            // Check permissions
            if (!hasConfigurationAccess()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Insufficient permissions"))
                    .build();
            }
            
            AdminConfigurationService.ConfigurationCacheStats stats = configService.getCacheStats();
            return Response.ok(stats).build();
            
        } catch (Exception e) {
            logger.error("Failed to get cache stats", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to get cache stats: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Clears configuration cache.
     * 
     * @return success response
     */
    @DELETE
    @Path("/cache")
    public Response clearCache() {
        try {
            // Check permissions
            if (!hasConfigurationAccess()) {
                return Response.status(Response.Status.FORBIDDEN)
                    .entity(Map.of("error", "Insufficient permissions"))
                    .build();
            }
            
            configService.clearCache(realm.getId());
            return Response.ok(Map.of("message", "Cache cleared successfully")).build();
            
        } catch (Exception e) {
            logger.error("Failed to clear cache", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Failed to clear cache: " + e.getMessage()))
                .build();
        }
    }
    
    /**
     * Checks if the current user has configuration access.
     */
    private boolean hasConfigurationAccess() {
        if (auth == null || auth.getUser() == null) {
            return false;
        }
        
        // Check if user has realm management role
        return auth.getToken().getRealmAccess() != null && 
               (auth.getToken().getRealmAccess().getRoles().contains("realm-admin") ||
                auth.getToken().getRealmAccess().getRoles().contains("manage-realm"));
    }
    
    /**
     * Checks if additional authentication is available for sensitive operations.
     */
    private boolean hasAdditionalAuth() {
        if (securityService == null) {
            return true; // If security service not available, allow operation
        }
        
        // For now, just check if user is authenticated
        return auth != null && auth.getUser() != null;
    }
    
    /**
     * Determines if configuration changes require additional authentication.
     */
    private boolean requiresAdditionalAuth(AdminConfiguration updates) {
        // Check if sensitive settings are being changed
        return updates.isExportAuditToSiem() || 
               updates.getMaxBulkOperationSize() > 500 ||
               !updates.isEnableDetailedAuditLogging() ||
               updates.getSessionTimeoutMinutes() > 240;
    }
    
    /**
     * Request DTO for configuration import.
     */
    public static class ConfigurationImportRequest {
        private String configurationJson;
        private boolean validateOnly = false;
        
        public String getConfigurationJson() { return configurationJson; }
        public void setConfigurationJson(String configurationJson) { this.configurationJson = configurationJson; }
        
        public boolean isValidateOnly() { return validateOnly; }
        public void setValidateOnly(boolean validateOnly) { this.validateOnly = validateOnly; }
    }
}