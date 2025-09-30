package com.bioid.keycloak.account;

import com.bioid.keycloak.credential.FaceCredentialModel;
import com.bioid.keycloak.credential.FaceCredentialProvider;
import com.bioid.keycloak.credential.FaceCredentialProviderFactory;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
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

    private FaceCredentialProvider getCredentialProvider() {
        FaceCredentialProvider provider = (FaceCredentialProvider) 
            session.getProvider(CredentialProvider.class, FaceCredentialProviderFactory.PROVIDER_ID);
        if (provider == null) {
            throw new IllegalStateException("FaceCredentialProvider not available");
        }
        return provider;
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
         * Get user's face credentials and settings
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

                FaceCredentialStatus status = new FaceCredentialStatus(
                    hasCredentials,
                    faceAuthEnabled,
                    requireFaceAuth,
                    fallbackEnabled,
                    credentials.stream()
                        .map(this::toCredentialInfo)
                        .collect(Collectors.toList())
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

                logger.info("Deleted all face credentials for user: {}", user.getId());

                return Response.ok().build();
            } catch (Exception e) {
                logger.error("Error deleting face credentials for user: " + auth.getUser().getId(), e);
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
    }

    // DTOs for JSON responses
    public static class FaceCredentialStatus {
        public boolean hasCredentials;
        public boolean faceAuthEnabled;
        public boolean requireFaceAuth;
        public boolean fallbackEnabled;
        public List<CredentialInfo> credentials;

        public FaceCredentialStatus(boolean hasCredentials, boolean faceAuthEnabled, 
                                  boolean requireFaceAuth, boolean fallbackEnabled,
                                  List<CredentialInfo> credentials) {
            this.hasCredentials = hasCredentials;
            this.faceAuthEnabled = faceAuthEnabled;
            this.requireFaceAuth = requireFaceAuth;
            this.fallbackEnabled = fallbackEnabled;
            this.credentials = credentials;
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
}