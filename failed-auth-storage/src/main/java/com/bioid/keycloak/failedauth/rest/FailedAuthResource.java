package com.bioid.keycloak.failedauth.rest;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.credential.FaceCredentialProvider;
import com.bioid.keycloak.failedauth.config.FailedAuthConfiguration;
import com.bioid.keycloak.failedauth.entity.FailedAuthAttemptEntity;
import com.bioid.keycloak.failedauth.service.FailedAuthImageStorageService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/")
public class FailedAuthResource {
    
    private static final Logger logger = LoggerFactory.getLogger(FailedAuthResource.class);
    
    private final KeycloakSession session;
    private final FailedAuthImageStorageService storageService;
    
    public FailedAuthResource(KeycloakSession session) {
        this.session = session;
        this.storageService = new FailedAuthImageStorageService(
            FailedAuthConfiguration.getInstance());
    }
    
    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }
    
    @GET
    @Path("/attempts")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFailedAttempts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("10") int pageSize) {
        
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Not authenticated")).build();
            }
            
            List<FailedAuthAttemptEntity> attempts = storageService.getFailedAttempts(
                session, user.getId(), false, null, page, pageSize);
            
            List<Map<String, Object>> result = attempts.stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("attempts", result);
            response.put("page", page);
            response.put("pageSize", pageSize);
            response.put("total", result.size());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Failed to get attempts", e);
            return Response.serverError()
                .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    @GET
    @Path("/attempts/{attemptId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAttempt(@PathParam("attemptId") String attemptId) {
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Not authenticated")).build();
            }
            
            FailedAuthAttemptEntity attempt = storageService.getAttempt(
                session, attemptId, user.getId());
            
            return Response.ok(toDetails(attempt)).build();
            
        } catch (Exception e) {
            logger.error("Failed to get attempt", e);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    @GET
    @Path("/attempts/{attemptId}/image/{index}")
    @Produces("image/jpeg")
    public Response getImage(
            @PathParam("attemptId") String attemptId,
            @PathParam("index") int index,
            @QueryParam("thumbnail") @DefaultValue("false") boolean thumbnail) {
        
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            byte[] imageData = storageService.getImage(
                session, attemptId, index, user.getId(), thumbnail);
            
            return Response.ok(imageData)
                .header("Content-Type", "image/jpeg")
                .build();
            
        } catch (Exception e) {
            logger.error("Failed to get image", e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
    
    @POST
    @Path("/attempts/{attemptId}/enroll")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enrollAttempt(
            @PathParam("attemptId") String attemptId,
            Map<String, Object> request) {
        
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Not authenticated")).build();
            }
            
            @SuppressWarnings("unchecked")
            List<Integer> imageIndices = (List<Integer>) request.get("imageIndices");
            
            if (imageIndices == null || imageIndices.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "No images selected")).build();
            }
            
            // Get BioIdClient from credential provider
            BioIdClient bioIdClient = getBioIdClient();
            if (bioIdClient == null) {
                return Response.serverError()
                    .entity(Map.of("error", "BioID client not available")).build();
            }
            
            FailedAuthImageStorageService.EnrollmentResult result = 
                storageService.enrollFailedAttempt(
                    session, getRealm(), user, attemptId, imageIndices, bioIdClient);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("enrolledImages", result.getEnrolledImages());
            response.put("newFeatureVectors", result.getNewFeatureVectors());
            response.put("timestamp", result.getTimestamp().toString());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Failed to enroll attempt", e);
            return Response.serverError()
                .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    @DELETE
    @Path("/attempts/{attemptId}")
    public Response deleteAttempt(@PathParam("attemptId") String attemptId) {
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
            
            storageService.deleteAttempt(session, attemptId, user.getId());
            
            return Response.noContent().build();
            
        } catch (Exception e) {
            logger.error("Failed to delete attempt", e);
            return Response.serverError()
                .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    @GET
    @Path("/statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics() {
        try {
            UserModel user = getAuthenticatedUser();
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Map.of("error", "Not authenticated")).build();
            }
            
            FailedAuthImageStorageService.FailedAttemptStatistics stats = 
                storageService.getStatistics(session, user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalCount", stats.getTotalCount());
            response.put("enrolledCount", stats.getEnrolledCount());
            response.put("unenrolledCount", stats.getUnenrolledCount());
            response.put("recommendedCount", stats.getRecommendedCount());
            response.put("avgQualityScore", stats.getAvgQualityScore());
            
            return Response.ok(response).build();
            
        } catch (Exception e) {
            logger.error("Failed to get statistics", e);
            return Response.serverError()
                .entity(Map.of("error", e.getMessage())).build();
        }
    }
    
    private UserModel getAuthenticatedUser() {
        AuthenticationManager.AuthResult authResult = 
            new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
        
        if (authResult == null) {
            return null;
        }
        
        return authResult.getUser();
    }
    
    private BioIdClient getBioIdClient() {
        try {
            FaceCredentialProvider provider = (FaceCredentialProvider) session.getProvider(
                CredentialProvider.class, "face-credential");
            if (provider != null) {
                return provider.getBioIdClient();
            }
        } catch (Exception e) {
            logger.error("Failed to get BioIdClient", e);
        }
        return null;
    }
    
    private Map<String, Object> toSummary(FailedAuthAttemptEntity attempt) {
        Map<String, Object> map = new HashMap<>();
        map.put("attemptId", attempt.getAttemptId());
        map.put("timestamp", attempt.getTimestamp().toString());
        map.put("failureReason", attempt.getFailureReason());
        map.put("imageCount", attempt.getImageCount());
        map.put("avgQualityScore", attempt.getAvgQualityScore());
        map.put("enrolled", attempt.getEnrolled());
        map.put("retryAttempt", attempt.getRetryAttempt());
        map.put("livenessMode", attempt.getLivenessMode());
        return map;
    }
    
    private Map<String, Object> toDetails(FailedAuthAttemptEntity attempt) {
        Map<String, Object> map = toSummary(attempt);
        map.put("verificationScore", attempt.getVerificationScore());
        map.put("verificationThreshold", attempt.getVerificationThreshold());
        map.put("scoreDifference", attempt.getScoreDifference());
        map.put("livenessScore", attempt.getLivenessScore());
        map.put("livenessPassed", attempt.getLivenessPassed());
        map.put("ipAddress", attempt.getIpAddress());
        map.put("sessionId", attempt.getSessionId());
        return map;
    }
}
