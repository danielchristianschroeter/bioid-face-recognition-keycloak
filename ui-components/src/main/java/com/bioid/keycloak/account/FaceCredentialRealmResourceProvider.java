package com.bioid.keycloak.account;

import com.bioid.keycloak.credential.FaceCredentialModel;
import com.bioid.keycloak.credential.FaceCredentialProvider;
import com.bioid.keycloak.credential.FaceCredentialProviderFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.keycloak.credential.CredentialProvider;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Realm resource provider for face credential management.
 * Provides REST endpoints for users to manage their face authentication settings.
 */
public class FaceCredentialRealmResourceProvider implements RealmResourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(FaceCredentialRealmResourceProvider.class);

    private final KeycloakSession session;

    public FaceCredentialRealmResourceProvider(KeycloakSession session) {
        this.session = session;
    }



    @Override
    public Object getResource() {
        return new FaceCredentialResource(session);
    }

    @Override
    public void close() {
        // No resources to close
    }

    @Path("/face-credentials")
    public static class FaceCredentialResource {
        
        private final KeycloakSession session;
        
        public FaceCredentialResource(KeycloakSession session) {
            this.session = session;
        }
        
        private FaceCredentialProvider getCredentialProvider() {
            FaceCredentialProvider provider = (FaceCredentialProvider) 
                session.getProvider(CredentialProvider.class, FaceCredentialProviderFactory.PROVIDER_ID);
            if (provider == null) {
                throw new IllegalStateException("FaceCredentialProvider not available");
            }
            return provider;
        }

        /**
         * Get user's face credentials and settings with template metadata
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Response getFaceCredentials() {
            AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
            if (auth == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            try {
                UserModel user = auth.getUser();
                RealmModel realm = session.getContext().getRealm();
                FaceCredentialProvider provider = getCredentialProvider();

                List<FaceCredentialModel> credentials = provider.getFaceCredentials(realm, user)
                    .collect(java.util.stream.Collectors.toList());
                boolean hasCredentials = !credentials.isEmpty();
                
                // Get user preferences
                boolean faceAuthEnabled = Boolean.parseBoolean(
                    user.getFirstAttribute("face.auth.enabled"));
                boolean requireFaceAuth = Boolean.parseBoolean(
                    user.getFirstAttribute("face.auth.required"));
                boolean fallbackEnabled = Boolean.parseBoolean(
                    user.getFirstAttribute("face.auth.fallback.enabled"));

                // Get template status information if credentials exist
                TemplateStatusInfo templateStatus = null;
                if (hasCredentials && !credentials.isEmpty()) {
                    templateStatus = getTemplateStatusInfo(credentials.get(0));
                }

                // Get last authentication timestamp
                Instant lastAuthentication = getLastAuthenticationTime(user);

                FaceCredentialStatus status = new FaceCredentialStatus(
                    hasCredentials,
                    faceAuthEnabled,
                    requireFaceAuth,
                    fallbackEnabled,
                    credentials.stream()
                        .map(this::toCredentialInfo)
                        .collect(Collectors.toList()),
                    templateStatus,
                    lastAuthentication
                );

                return Response.ok(status).build();
            } catch (Exception e) {
                logger.error("Error getting face credentials for user: " + auth.getUser().getId(), e);
                return Response.serverError().build();
            }
        }

        /**
         * Update face authentication settings
         */
        @POST
        @Path("/settings")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        public Response updateSettings(
                @FormParam("faceAuthEnabled") boolean faceAuthEnabled,
                @FormParam("requireFaceAuth") boolean requireFaceAuth,
                @FormParam("fallbackEnabled") boolean fallbackEnabled) {
            
            AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
            if (auth == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            try {
                UserModel user = auth.getUser();
                
                // Update user preferences
                user.setSingleAttribute("face.auth.enabled", String.valueOf(faceAuthEnabled));
                user.setSingleAttribute("face.auth.required", String.valueOf(requireFaceAuth));
                user.setSingleAttribute("face.auth.fallback.enabled", String.valueOf(fallbackEnabled));

                // If face auth is disabled, remove required action
                if (!faceAuthEnabled) {
                    user.removeRequiredAction("face-enroll");
                }

                logger.info("Updated face auth settings for user: {} - enabled: {}, required: {}, fallback: {}", 
                    user.getId(), faceAuthEnabled, requireFaceAuth, fallbackEnabled);

                return Response.ok().build();
            } catch (Exception e) {
                logger.error("Error updating face auth settings for user: " + auth.getUser().getId(), e);
                return Response.serverError().build();
            }
        }

        /**
         * Initiate face enrollment
         */
        @POST
        @Path("/enroll")
        public Response enrollFace() {
            AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
            if (auth == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            try {
                UserModel user = auth.getUser();
                
                // Add face enrollment as required action
                user.addRequiredAction("face-enroll");
                
                // Enable face auth by default when enrolling
                user.setSingleAttribute("face.auth.enabled", "true");

                logger.info("Initiated face enrollment for user: {}", user.getId());

                return Response.ok().build();
            } catch (Exception e) {
                logger.error("Error initiating face enrollment for user: " + auth.getUser().getId(), e);
                return Response.serverError().build();
            }
        }

        /**
         * Disable face authentication (keep credentials but disable usage)
         */
        @POST
        @Path("/disable")
        public Response disableFaceAuth() {
            AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
            if (auth == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            try {
                UserModel user = auth.getUser();
                
                // Disable face auth
                user.setSingleAttribute("face.auth.enabled", "false");
                user.setSingleAttribute("face.auth.required", "false");
                user.removeRequiredAction("face-enroll");

                logger.info("Disabled face authentication for user: {}", user.getId());

                return Response.ok().build();
            } catch (Exception e) {
                logger.error("Error disabling face auth for user: " + auth.getUser().getId(), e);
                return Response.serverError().build();
            }
        }

        /**
         * Delete all face credentials and disable face authentication
         */
        @DELETE
        @Path("/delete")
        public Response deleteFaceCredentials() {
            AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
            if (auth == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            try {
                UserModel user = auth.getUser();
                RealmModel realm = session.getContext().getRealm();
                FaceCredentialProvider provider = getCredentialProvider();

                // Delete all face credentials
                List<FaceCredentialModel> credentials = provider.getFaceCredentials(realm, user)
                    .collect(java.util.stream.Collectors.toList());
                for (FaceCredentialModel credential : credentials) {
                    provider.deleteCredential(realm, user, credential.getId());
                }

                // Disable face auth
                user.setSingleAttribute("face.auth.enabled", "false");
                user.setSingleAttribute("face.auth.required", "false");
                user.removeRequiredAction("face-enroll");

                // Log audit event for GDPR compliance
                logAuditEvent(user, "FACE_CREDENTIALS_DELETED", "User deleted all face credentials");

                logger.info("Deleted all face credentials for user: {}", user.getId());

                return Response.ok().build();
            } catch (Exception e) {
                logger.error("Error deleting face credentials for user: " + auth.getUser().getId(), e);
                return Response.serverError().build();
            }
        }

        /**
         * Export user's face authentication data (GDPR compliant)
         */
        @GET
        @Path("/export")
        @Produces("application/json")
        public Response exportFaceData() {
            AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
            if (auth == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            try {
                UserModel user = auth.getUser();
                RealmModel realm = session.getContext().getRealm();
                FaceCredentialProvider provider = getCredentialProvider();

                // Collect all face authentication data (excluding raw biometric templates)
                Map<String, Object> exportData = new HashMap<>();
                exportData.put("userId", user.getId());
                exportData.put("username", user.getUsername());
                exportData.put("exportDate", Instant.now().toString());
                exportData.put("realm", realm.getName());

                // Face authentication settings
                Map<String, Object> settings = new HashMap<>();
                settings.put("faceAuthEnabled", user.getFirstAttribute("face.auth.enabled"));
                settings.put("requireFaceAuth", user.getFirstAttribute("face.auth.required"));
                settings.put("fallbackEnabled", user.getFirstAttribute("face.auth.fallback.enabled"));
                exportData.put("settings", settings);

                // Credential metadata (no biometric data)
                List<FaceCredentialModel> credentials = provider.getFaceCredentials(realm, user)
                    .collect(java.util.stream.Collectors.toList());
                
                List<Map<String, Object>> credentialData = new ArrayList<>();
                for (FaceCredentialModel credential : credentials) {
                    Map<String, Object> credInfo = new HashMap<>();
                    credInfo.put("credentialId", credential.getId());
                    credInfo.put("enrollmentDate", credential.getCreatedAt().toString());
                    credInfo.put("expiryDate", credential.getExpiresAt().toString());
                    
                    // Get template metadata if available
                    TemplateStatusInfo templateStatus = getTemplateStatusInfo(credential);
                    if (templateStatus != null) {
                        credInfo.put("encoderVersion", templateStatus.encoderVersion);
                        credInfo.put("featureVectors", templateStatus.featureVectors);
                        credInfo.put("healthStatus", templateStatus.healthStatus);
                    }
                    
                    credentialData.add(credInfo);
                }
                exportData.put("credentials", credentialData);

                // Authentication history (last authentication only for privacy)
                Instant lastAuth = getLastAuthenticationTime(user);
                if (lastAuth != null) {
                    exportData.put("lastAuthentication", lastAuth.toString());
                }

                // Log audit event
                logAuditEvent(user, "FACE_DATA_EXPORTED", "User exported face authentication data");

                StreamingOutput stream = output -> {
                    try {
                        // Simple JSON serialization
                        String json = mapToJson(exportData);
                        output.write(json.getBytes());
                    } catch (IOException e) {
                        throw new WebApplicationException(e);
                    }
                };

                return Response.ok(stream)
                    .header("Content-Disposition", "attachment; filename=\"face-auth-data-" + 
                           user.getUsername() + "-" + 
                           DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.now().atZone(java.time.ZoneOffset.UTC)) + 
                           ".json\"")
                    .build();

            } catch (Exception e) {
                logger.error("Error exporting face data for user: " + auth.getUser().getId(), e);
                return Response.serverError().build();
            }
        }

        /**
         * Re-enroll face authentication after deletion
         */
        @POST
        @Path("/re-enroll")
        public Response reEnrollFace() {
            AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
            if (auth == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            try {
                UserModel user = auth.getUser();
                
                // Add face enrollment as required action
                user.addRequiredAction("face-enroll");
                
                // Enable face auth by default when re-enrolling
                user.setSingleAttribute("face.auth.enabled", "true");
                user.setSingleAttribute("face.auth.fallback.enabled", "true"); // Enable fallback for safety

                // Log audit event
                logAuditEvent(user, "FACE_RE_ENROLLMENT_INITIATED", "User initiated face re-enrollment");

                logger.info("Initiated face re-enrollment for user: {}", user.getId());

                return Response.ok().build();
            } catch (Exception e) {
                logger.error("Error initiating face re-enrollment for user: " + auth.getUser().getId(), e);
                return Response.serverError().build();
            }
        }

        private CredentialInfo toCredentialInfo(FaceCredentialModel credential) {
            return new CredentialInfo(
                credential.getId(),
                credential.getCreatedAt(),
                credential.getExpiresAt(),
                null // lastUsed is not tracked in current model
            );
        }

        /**
         * Get thumbnails for a credential
         */
        @GET
        @Path("/thumbnails/{credentialId}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getThumbnails(@PathParam("credentialId") String credentialId) {
            AuthenticationManager.AuthResult auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
            if (auth == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            try {
                UserModel user = auth.getUser();
                RealmModel realm = session.getContext().getRealm();
                FaceCredentialProvider provider = getCredentialProvider();

                // Find the credential
                FaceCredentialModel credential = provider.getFaceCredentials(realm, user)
                    .filter(c -> c.getId().equals(credentialId))
                    .findFirst()
                    .orElse(null);

                if (credential == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }

                // Get template status with thumbnails
                com.bioid.keycloak.client.BioIdClient bioIdClient = provider.getBioIdClient();
                if (bioIdClient == null) {
                    return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                        .entity("{\"error\":\"BioID service not available\"}").build();
                }

                com.bioid.keycloak.client.BioIdClient.TemplateStatusDetails details = 
                    bioIdClient.getTemplateStatusDetails(credential.getClassId(), true);

                if (!details.isAvailable() || details.getThumbnails().isEmpty()) {
                    return Response.ok("{\"thumbnails\":[]}").build();
                }

                // Convert thumbnails to base64 for JSON response
                List<Map<String, String>> thumbnailList = new ArrayList<>();
                for (com.bioid.keycloak.client.BioIdClient.ThumbnailData thumb : details.getThumbnails()) {
                    Map<String, String> thumbData = new HashMap<>();
                    thumbData.put("enrolled", thumb.getEnrolled() != null ? thumb.getEnrolled().toString() : "");
                    thumbData.put("image", "data:image/png;base64," + 
                        java.util.Base64.getEncoder().encodeToString(thumb.getImageData()));
                    thumbnailList.add(thumbData);
                }

                Map<String, Object> response = new HashMap<>();
                response.put("thumbnails", thumbnailList);
                response.put("count", thumbnailList.size());

                logger.info("Retrieved {} thumbnails for credential: {} (user: {})", 
                    thumbnailList.size(), credentialId, user.getId());

                return Response.ok(mapToJson(response)).build();

            } catch (Exception e) {
                logger.error("Error getting thumbnails for credential: " + credentialId, e);
                return Response.serverError().build();
            }
        }

        private TemplateStatusInfo getTemplateStatusInfo(FaceCredentialModel credential) {
            try {
                FaceCredentialProvider provider = getCredentialProvider();
                com.bioid.keycloak.client.BioIdClient bioIdClient = provider.getBioIdClient();
                
                if (bioIdClient == null) {
                    logger.warn("BioID client not available, returning null template status");
                    return null;
                }

                // Get template status from BWS (without thumbnails for performance)
                com.bioid.keycloak.client.BioIdClient.TemplateStatusDetails details = 
                    bioIdClient.getTemplateStatusDetails(credential.getClassId(), false);

                if (!details.isAvailable()) {
                    return null;
                }

                // Determine health status based on encoder version
                String healthStatus = "HEALTHY";
                boolean needsUpgrade = false;
                
                // Encoder version 5 is current, 4 is acceptable, 3 or lower needs upgrade
                if (details.getEncoderVersion() < 4) {
                    healthStatus = "NEEDS_UPGRADE";
                    needsUpgrade = true;
                } else if (details.getEncoderVersion() == 4) {
                    healthStatus = "ACCEPTABLE";
                    needsUpgrade = false;
                }

                return new TemplateStatusInfo(
                    details.getEncoderVersion(),
                    details.getFeatureVectors(),
                    healthStatus,
                    needsUpgrade,
                    details.getThumbnailsStored()
                );

            } catch (Exception e) {
                logger.warn("Failed to get template status for credential: " + credential.getId(), e);
                return null;
            }
        }



        private Instant getLastAuthenticationTime(UserModel user) {
            String lastAuthStr = user.getFirstAttribute("face.auth.last.used");
            if (lastAuthStr != null) {
                try {
                    return Instant.parse(lastAuthStr);
                } catch (Exception e) {
                    logger.warn("Failed to parse last authentication time: " + lastAuthStr, e);
                }
            }
            return null;
        }

        private void logAuditEvent(UserModel user, String eventType, String description) {
            try {
                // Simple audit logging - in production this would integrate with 
                // the proper audit event system
                logger.info("Audit Event - User: {}, Type: {}, Description: {}", 
                    user.getId(), eventType, description);
                
                // Could also fire Keycloak admin events here
                // session.getContext().getEvent().event(EventType.CUSTOM_REQUIRED_ACTION)
                //     .user(user)
                //     .detail("action", eventType)
                //     .detail("description", description)
                //     .success();
                    
            } catch (Exception e) {
                logger.error("Failed to log audit event", e);
            }
        }

        private String mapToJson(Map<String, Object> map) {
            // Simple JSON serialization - in production, use Jackson or similar
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":");
                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(value).append("\"");
                } else if (value instanceof Map || value instanceof List) {
                    json.append("\"").append(value.toString()).append("\"");
                } else {
                    json.append(value);
                }
                first = false;
            }
            json.append("}");
            return json.toString();
        }
    }

    // DTOs for JSON responses
    public static class FaceCredentialStatus {
        public boolean hasCredentials;
        public boolean faceAuthEnabled;
        public boolean requireFaceAuth;
        public boolean fallbackEnabled;
        public List<CredentialInfo> credentials;
        public TemplateStatusInfo templateStatus;
        public Instant lastAuthentication;

        public FaceCredentialStatus(boolean hasCredentials, boolean faceAuthEnabled, 
                                  boolean requireFaceAuth, boolean fallbackEnabled,
                                  List<CredentialInfo> credentials, TemplateStatusInfo templateStatus,
                                  Instant lastAuthentication) {
            this.hasCredentials = hasCredentials;
            this.faceAuthEnabled = faceAuthEnabled;
            this.requireFaceAuth = requireFaceAuth;
            this.fallbackEnabled = fallbackEnabled;
            this.credentials = credentials;
            this.templateStatus = templateStatus;
            this.lastAuthentication = lastAuthentication;
        }
    }

    public static class CredentialInfo {
        public String id;
        public java.time.Instant createdDate;
        public java.time.Instant expiryDate;
        public java.time.Instant lastUsed;

        public CredentialInfo(String id, java.time.Instant createdDate, 
                            java.time.Instant expiryDate, java.time.Instant lastUsed) {
            this.id = id;
            this.createdDate = createdDate;
            this.expiryDate = expiryDate;
            this.lastUsed = lastUsed;
        }
    }

    public static class TemplateStatusInfo {
        public int encoderVersion;
        public int featureVectors;
        public String healthStatus;
        public boolean needsUpgrade;
        public int thumbnailsStored;

        public TemplateStatusInfo(int encoderVersion, int featureVectors, 
                                String healthStatus, boolean needsUpgrade, int thumbnailsStored) {
            this.encoderVersion = encoderVersion;
            this.featureVectors = featureVectors;
            this.healthStatus = healthStatus;
            this.needsUpgrade = needsUpgrade;
            this.thumbnailsStored = thumbnailsStored;
        }
    }
}