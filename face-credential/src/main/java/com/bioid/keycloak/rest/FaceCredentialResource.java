package com.bioid.keycloak.rest;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.exception.BioIdException;
import com.bioid.keycloak.credential.FaceCredentialModel;
import com.bioid.keycloak.credential.FaceCredentialProvider;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for face credential management.
 * 
 * Provides endpoints to:
 * - Get template status from BWS
 * - View enrolled thumbnail images
 * - Delete templates
 */
@Path("/")
public class FaceCredentialResource {

  private static final Logger logger = LoggerFactory.getLogger(FaceCredentialResource.class);
  private final KeycloakSession session;

  public FaceCredentialResource(KeycloakSession session) {
    this.session = session;
  }

  @GET
  @Path("/status")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTemplateStatus() {
    logger.info("GET /face-api/status called");

    try {
      // Authenticate the user
      AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
          .authenticate();

      if (authResult == null) {
        logger.warn("Unauthenticated request to /face-api/status");
        return Response.status(Response.Status.UNAUTHORIZED)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, OPTIONS")
            .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
            .entity(Map.of("error", "Authentication required")).build();
      }

      UserModel user = authResult.getUser();
      RealmModel realm = session.getContext().getRealm();

      logger.info("Getting template status for user: {}", user.getId());

      // Get user's face credential directly from credential manager
      CredentialModel credentialModel = user.credentialManager()
          .getStoredCredentialsStream()
          .filter(c -> FaceCredentialProvider.TYPE.equals(c.getType()))
          .findFirst()
          .orElse(null);

      if (credentialModel == null) {
        logger.info("No face credential found for user: {}", user.getId());
        return Response.ok(Map.of("enrolled", false, "message", "No face credential found"))
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, OPTIONS")
            .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
            .build();
      }

      // Parse the credential model to get class ID
      FaceCredentialModel credential = FaceCredentialModel.createFromCredentialModel(credentialModel);

      if (credential == null) {
        logger.info("No face credential found for user: {}", user.getId());
        return Response.ok(Map.of("enrolled", false, "message", "No face credential found"))
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, OPTIONS")
            .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
            .build();
      }

      logger.info("Found face credential for user: {}, classId: {}", user.getId(),
          credential.getClassId());

      // Get BioID client - create directly from configuration
      com.bioid.keycloak.client.config.BioIdConfiguration config = 
          com.bioid.keycloak.client.config.BioIdConfiguration.getInstance();
      
      if (config.getClientId() == null || config.getClientId().trim().isEmpty() ||
          config.getKey() == null || config.getKey().trim().isEmpty()) {
        logger.error("BioID credentials not configured");
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, OPTIONS")
            .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
            .entity(Map.of("error", "BioID service not configured")).build();
      }

      BioIdClient bioIdClient = new com.bioid.keycloak.client.BioIdGrpcClientProduction(
          config, config.getEndpoint(), config.getClientId(), config.getKey());

      // Get template status with thumbnails
      BioIdClient.TemplateStatusDetails status =
          bioIdClient.getTemplateStatusDetails(credential.getClassId(), true);

      logger.info("Retrieved template status for classId: {}, available: {}, thumbnails: {}",
          credential.getClassId(), status.isAvailable(), status.getThumbnails().size());

      // Build response
      Map<String, Object> response = new HashMap<>();
      response.put("enrolled", status.isAvailable());
      response.put("classId", status.getClassId());
      response.put("enrolledAt",
          status.getEnrolled() != null ? status.getEnrolled().toString() : null);
      response.put("encoderVersion", status.getEncoderVersion());
      response.put("featureVectors", status.getFeatureVectors());
      response.put("thumbnailsStored", status.getThumbnailsStored());
      response.put("tags", status.getTags());

      // Convert thumbnails to base64
      List<Map<String, String>> thumbnails = new ArrayList<>();
      for (BioIdClient.ThumbnailData thumb : status.getThumbnails()) {
        Map<String, String> thumbData = new HashMap<>();
        thumbData.put("enrolledAt",
            thumb.getEnrolled() != null ? thumb.getEnrolled().toString() : null);
        thumbData.put("image",
            "data:image/png;base64," + Base64.getEncoder().encodeToString(thumb.getImageData()));
        thumbnails.add(thumbData);
      }
      response.put("thumbnails", thumbnails);

      logger.info("Returning template status with {} thumbnails", thumbnails.size());

      return Response.ok(response)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "GET, OPTIONS")
          .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
          .build();

    } catch (BioIdException e) {
      logger.error("BioID error getting template status", e);
      return Response.status(Response.Status.BAD_REQUEST)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "GET, OPTIONS")
          .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
          .entity(Map.of("error", "BioID error: " + e.getMessage())).build();
    } catch (Exception e) {
      logger.error("Error getting template status", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "GET, OPTIONS")
          .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
          .entity(Map.of("error", "Internal error: " + e.getMessage())).build();
    }
  }

  @OPTIONS
  @Path("/status")
  public Response statusOptions() {
    return Response.ok()
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Methods", "GET, OPTIONS")
        .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
        .build();
  }

  @DELETE
  @Path("/template")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteTemplate() {
    logger.info("DELETE /face-api/template called");

    try {
      // Authenticate the user
      AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
          .authenticate();

      if (authResult == null) {
        logger.warn("Unauthenticated request to /face-api/template");
        return Response.status(Response.Status.UNAUTHORIZED)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "DELETE, OPTIONS")
            .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
            .entity(Map.of("error", "Authentication required")).build();
      }

      UserModel user = authResult.getUser();
      RealmModel realm = session.getContext().getRealm();

      logger.info("Deleting template for user: {}", user.getId());

      // Get all user's face credentials directly from credential manager
      List<CredentialModel> credentials =
          user.credentialManager().getStoredCredentialsStream()
              .filter(c -> FaceCredentialProvider.TYPE.equals(c.getType()))
              .collect(Collectors.toList());

      if (credentials.isEmpty()) {
        logger.info("No face credentials found for user: {}", user.getId());
        return Response.ok(Map.of("deleted", false, "message", "No face credentials found"))
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "DELETE, OPTIONS")
            .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
            .build();
      }

      // Get BioID client for template deletion
      com.bioid.keycloak.client.config.BioIdConfiguration config = 
          com.bioid.keycloak.client.config.BioIdConfiguration.getInstance();
      BioIdClient bioIdClient = null;
      
      if (config.getClientId() != null && !config.getClientId().trim().isEmpty() &&
          config.getKey() != null && !config.getKey().trim().isEmpty()) {
        bioIdClient = new com.bioid.keycloak.client.BioIdGrpcClientProduction(
            config, config.getEndpoint(), config.getClientId(), config.getKey());
      }

      // Delete all face credentials (which will also delete BWS templates)
      int deletedCount = 0;
      for (CredentialModel credentialModel : credentials) {
        try {
          // Parse credential to get class ID for BWS deletion
          FaceCredentialModel faceCredential = FaceCredentialModel.createFromCredentialModel(credentialModel);
          
          // Delete from BWS if client is available
          if (bioIdClient != null && faceCredential != null) {
            try {
              bioIdClient.deleteTemplate(faceCredential.getClassId());
              logger.info("Deleted BWS template for class ID: {}", faceCredential.getClassId());
            } catch (Exception e) {
              logger.warn("Failed to delete BWS template for class ID: {}", 
                  faceCredential.getClassId(), e);
            }
          }
          
          // Delete from Keycloak
          boolean deleted = user.credentialManager().removeStoredCredentialById(credentialModel.getId());
          if (deleted) {
            deletedCount++;
            logger.info("Deleted face credential: {} for user: {}", credentialModel.getId(),
                user.getId());
          }
        } catch (Exception e) {
          logger.error("Failed to delete credential: {}", credentialModel.getId(), e);
        }
      }

      logger.info("Deleted {} face credentials for user: {}", deletedCount, user.getId());

      return Response.ok(Map.of("deleted", true, "count", deletedCount,
          "message", "Successfully deleted " + deletedCount + " face credential(s)"))
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "DELETE, OPTIONS")
          .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
          .build();

    } catch (Exception e) {
      logger.error("Error deleting template", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Methods", "DELETE, OPTIONS")
          .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
          .entity(Map.of("error", "Internal error: " + e.getMessage())).build();
    }
  }

  @OPTIONS
  @Path("/template")
  public Response templateOptions() {
    return Response.ok()
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Methods", "DELETE, OPTIONS")
        .header("Access-Control-Allow-Headers", "Authorization, Content-Type")
        .build();
  }
}
