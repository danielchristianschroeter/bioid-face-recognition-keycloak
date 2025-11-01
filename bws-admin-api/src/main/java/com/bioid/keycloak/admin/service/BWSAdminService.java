package com.bioid.keycloak.admin.service;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.config.BioIdConfiguration;
import com.bioid.keycloak.credential.FaceCredentialModel;
import com.bioid.keycloak.credential.FaceCredentialProvider;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for BWS admin operations.
 * 
 * Handles business logic for:
 * - Template statistics and reporting
 * - Template listing and details
 * - Template validation and consistency checks
 * - Template deletion and cleanup
 * - Audit logging
 * 
 * This service coordinates between Keycloak user management and BWS template management.
 * 
 * @author BioID Keycloak Extension
 * @version 1.0.0
 */
public class BWSAdminService {

  private static final Logger logger = LoggerFactory.getLogger(BWSAdminService.class);
  
  private final KeycloakSession session;
  private final RealmModel realm;

  public BWSAdminService(KeycloakSession session) {
    this.session = session;
    this.realm = session.getContext().getRealm();
  }

  /**
   * Get aggregated statistics about face templates.
   * 
   * @return statistics object
   */
  public AdminStats getStatistics() {
    logger.debug("Getting admin statistics");

    try {
      // Get all users with face credentials
      List<UserModel> usersWithFace = getUsersWithFaceCredentials();
      
      // Count total templates in Keycloak
      int totalTemplates = usersWithFace.size();
      
      // Get last enrollment date
      Instant lastEnrollment = usersWithFace.stream()
          .map(this::getFaceCredentialEnrollmentDate)
          .filter(Objects::nonNull)
          .max(Instant::compareTo)
          .orElse(null);
      
      // Count potentially orphaned (users with classId but no credential)
      int orphanedTemplates = findOrphanedTemplates().size();
      
      AdminStats stats = new AdminStats();
      stats.setTotalTemplates(totalTemplates);
      stats.setActiveUsers(totalTemplates);
      stats.setOrphanedTemplates(orphanedTemplates);
      stats.setLastEnrollment(lastEnrollment);
      
      // Try to get BWS class count
      try {
        Integer bwsCount = getBWSClassCount();
        stats.setBwsClassCount(bwsCount);
        logger.debug("BWS reports {} classes", bwsCount);
      } catch (Exception e) {
        logger.warn("Failed to get BWS class count: {}", e.getMessage());
        stats.setBwsError(e.getMessage());
      }
      
      logger.debug("Statistics: {} Keycloak templates, {} active users, {} orphaned", 
          totalTemplates, totalTemplates, orphanedTemplates);
      
      return stats;
      
    } catch (Exception e) {
      logger.error("Error getting statistics", e);
      throw new RuntimeException("Failed to get statistics", e);
    }
  }

  /**
   * Get the number of classes enrolled in BWS using the Management API.
   * 
   * @return number of classes in BWS
   * @throws Exception if BWS API call fails
   */
  private Integer getBWSClassCount() throws Exception {
    try {
      BioIdConfiguration config = BioIdConfiguration.getInstance();
      
      // Get Management API configuration
      String managementUrl = config.getManagementUrl();
      String jwtToken = config.getManagementJwtToken();
      String clientId = config.getClientId();
      
      // If JWT token not directly provided, try to generate from email and API key
      if (jwtToken == null || jwtToken.isEmpty()) {
        String email = config.getManagementEmail();
        String apiKey = config.getManagementApiKey();
        
        if (email != null && !email.trim().isEmpty() && 
            apiKey != null && !apiKey.trim().isEmpty()) {
          try {
            // Generate JWT token
            jwtToken = com.bioid.keycloak.admin.client.JwtTokenGenerator.generateToken(
                email, apiKey);
            logger.info("Auto-generated JWT token from email and API key");
          } catch (Exception e) {
            logger.error("Failed to generate JWT token from email and API key", e);
            throw new Exception("Failed to generate JWT token: " + e.getMessage());
          }
        } else {
          throw new Exception("BWS Management JWT token not configured. " +
              "Set either bws.management.jwtToken OR both bws.management.email " +
              "and bws.management.apiKey in bioid.properties or environment variables");
        }
      }
      
      if (clientId == null || clientId.isEmpty()) {
        throw new Exception("BWS Client ID not configured");
      }
      
      // Call BWS Management API (REST)
      com.bioid.keycloak.admin.client.BWSManagementClient managementClient;
      if (managementUrl != null && !managementUrl.isEmpty()) {
        managementClient = new com.bioid.keycloak.admin.client.BWSManagementClient(
            managementUrl, jwtToken);
      } else {
        managementClient = new com.bioid.keycloak.admin.client.BWSManagementClient(jwtToken);
      }
      
      try {
        int count = managementClient.getClassCount(clientId);
        logger.debug("BWS class count: {}", count);
        return count;
      } finally {
        managementClient.close();
      }
      
    } catch (Exception e) {
      logger.error("Failed to get BWS class count", e);
      throw new Exception("BWS Management API error: " + e.getMessage());
    }
  }

  /**
   * List all face templates with user information.
   * 
   * @return list of template information
   */
  public List<TemplateInfo> listAllTemplates() {
    logger.debug("Listing all templates");

    try {
      List<UserModel> usersWithFace = getUsersWithFaceCredentials();
      
      List<TemplateInfo> templates = new ArrayList<>();
      
      for (UserModel user : usersWithFace) {
        CredentialModel credentialModel = getFaceCredential(user);
        if (credentialModel != null) {
          FaceCredentialModel faceCredential = 
              FaceCredentialModel.createFromCredentialModel(credentialModel);
          
          if (faceCredential != null) {
            TemplateInfo info = new TemplateInfo();
            info.setClassId(String.valueOf(faceCredential.getClassId()));
            info.setUsername(user.getUsername());
            info.setEmail(user.getEmail());
            info.setEnrolledAt(getFaceCredentialEnrollmentDate(user));
            info.setKeycloakUserExists(true);
            
            templates.add(info);
          }
        }
      }
      
      logger.debug("Found {} templates", templates.size());
      
      return templates;
      
    } catch (Exception e) {
      logger.error("Error listing templates", e);
      throw new RuntimeException("Failed to list templates", e);
    }
  }

  /**
   * Get details for a specific template.
   * 
   * @param classId the class ID
   * @return template information or null if not found
   */
  public TemplateInfo getTemplateDetails(long classId) {
    logger.debug("Getting template details for classId: {}", classId);

    try {
      // Find user with this class ID
      UserModel user = findUserByClassId(classId);
      
      if (user == null) {
        logger.debug("No user found with classId: {}", classId);
        return null;
      }
      
      CredentialModel credentialModel = getFaceCredential(user);
      if (credentialModel == null) {
        return null;
      }
      
      FaceCredentialModel faceCredential = 
          FaceCredentialModel.createFromCredentialModel(credentialModel);
      
      if (faceCredential == null) {
        return null;
      }
      
      TemplateInfo info = new TemplateInfo();
      info.setClassId(String.valueOf(faceCredential.getClassId()));
      info.setUsername(user.getUsername());
      info.setEmail(user.getEmail());
      info.setEnrolledAt(getFaceCredentialEnrollmentDate(user));
      info.setKeycloakUserExists(true);
      
      // Try to get additional details from BWS
      try {
        BioIdClient client = createBioIdClient();
        if (client != null) {
          try {
            BioIdClient.TemplateStatusDetails status = 
                client.getTemplateStatusDetails(classId, false);
            
            info.setEncoderVersion(String.valueOf(status.getEncoderVersion()));
            info.setFeatureVectors(status.getFeatureVectors());
            info.setThumbnailsStored(status.getThumbnailsStored());
          } finally {
            client.close();
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to get BWS template details for classId: {}", classId, e);
      }
      
      return info;
      
    } catch (Exception e) {
      logger.error("Error getting template details for classId: {}", classId, e);
      throw new RuntimeException("Failed to get template details", e);
    }
  }

  /**
   * Delete a face template.
   * 
   * @param classId the class ID to delete
   * @return true if deleted, false if not found
   */
  public boolean deleteTemplate(long classId) {
    logger.info("Deleting template with classId: {}", classId);

    try {
      // Find user with this class ID
      UserModel user = findUserByClassId(classId);
      
      if (user == null) {
        logger.debug("No user found with classId: {}", classId);
        return false;
      }
      
      // Delete from BWS first
      try {
        BioIdClient client = createBioIdClient();
        if (client != null) {
          try {
            client.deleteTemplate(classId);
            logger.info("Deleted BWS template for classId: {}", classId);
          } finally {
            client.close();
          }
        }
      } catch (Exception e) {
        logger.warn("Failed to delete BWS template for classId: {}", classId, e);
        // Continue to delete from Keycloak anyway
      }
      
      // Delete from Keycloak
      CredentialModel credentialModel = getFaceCredential(user);
      if (credentialModel != null) {
        boolean deleted = user.credentialManager()
            .removeStoredCredentialById(credentialModel.getId());
        
        if (deleted) {
          logger.info("Deleted Keycloak credential for classId: {}", classId);
          return true;
        }
      }
      
      return false;
      
    } catch (Exception e) {
      logger.error("Error deleting template for classId: {}", classId, e);
      throw new RuntimeException("Failed to delete template", e);
    }
  }

  /**
   * Validate template consistency between Keycloak and BWS.
   * 
   * @return validation result
   */
  public ValidationResult validateTemplates() {
    logger.debug("Validating templates");

    try {
      ValidationResult result = new ValidationResult();
      
      // Get all Keycloak users with face credentials
      List<UserModel> usersWithFace = getUsersWithFaceCredentials();
      Set<String> keycloakClassIds = new HashSet<>();
      
      for (UserModel user : usersWithFace) {
        CredentialModel credentialModel = getFaceCredential(user);
        if (credentialModel != null) {
          FaceCredentialModel faceCredential = 
              FaceCredentialModel.createFromCredentialModel(credentialModel);
          if (faceCredential != null) {
            keycloakClassIds.add(String.valueOf(faceCredential.getClassId()));
          }
        }
      }
      
      // Note: To find orphaned templates, we would need BWS Management API
      // to list all class IDs in BWS and compare with Keycloak
      // For now, we can only validate what's in Keycloak
      
      result.setValid(true);
      result.setMessage("Validation complete. Found " + keycloakClassIds.size() + 
          " templates in Keycloak.");
      
      logger.debug("Validation complete: {} templates in Keycloak", keycloakClassIds.size());
      
      return result;
      
    } catch (Exception e) {
      logger.error("Error validating templates", e);
      throw new RuntimeException("Failed to validate templates", e);
    }
  }

  /**
   * Find potentially orphaned templates (users with classId but no face credential).
   * 
   * Note: BWS Management API doesn't provide a way to list all class IDs,
   * so we can only detect Keycloak users who have a classId attribute
   * but no actual face credential. True orphaned templates (in BWS but not
   * in Keycloak) cannot be detected without additional tracking.
   * 
   * @return list of potentially orphaned templates
   */
  public List<TemplateInfo> findOrphanedTemplates() {
    logger.debug("Finding potentially orphaned templates");

    List<TemplateInfo> orphaned = new ArrayList<>();
    
    try {
      // Find all users with classId attribute
      session.users().searchForUserByUserAttributeStream(realm, "classId", "")
          .forEach(user -> {
            String classId = user.getFirstAttribute("classId");
            if (classId != null && !classId.isEmpty()) {
              // Check if user has actual face credential
              boolean hasCredential = user.credentialManager()
                  .getStoredCredentialsByTypeStream(FaceCredentialProvider.TYPE)
                  .findAny()
                  .isPresent();
              
              if (!hasCredential) {
                // User has classId but no credential - potentially orphaned
                TemplateInfo info = new TemplateInfo();
                info.setClassId(classId);
                info.setUsername(user.getUsername());
                info.setEmail(user.getEmail());
                info.setKeycloakUserExists(true);
                info.setEnrolledAt(null);
                orphaned.add(info);
                
                logger.debug("Found potentially orphaned template: classId={}, user={}", 
                    classId, user.getUsername());
              }
            }
          });
      
      logger.info("Found {} potentially orphaned templates", orphaned.size());
      
    } catch (Exception e) {
      logger.error("Error finding orphaned templates", e);
    }
    
    return orphaned;
  }

  /**
   * Delete all orphaned templates.
   * 
   * @return number of templates deleted
   */
  public int deleteAllOrphaned() {
    logger.info("Deleting all orphaned templates");

    try {
      List<TemplateInfo> orphaned = findOrphanedTemplates();
      int deletedCount = 0;
      
      for (TemplateInfo template : orphaned) {
        try {
          long classId = Long.parseLong(template.getClassId());
          if (deleteTemplate(classId)) {
            deletedCount++;
          }
        } catch (Exception e) {
          logger.error("Failed to delete orphaned template: {}", 
              template.getClassId(), e);
        }
      }
      
      logger.info("Deleted {} orphaned templates", deletedCount);
      return deletedCount;
      
    } catch (Exception e) {
      logger.error("Error deleting orphaned templates", e);
      throw new RuntimeException("Failed to delete orphaned templates", e);
    }
  }

  /**
   * Log an admin action for audit purposes.
   * 
   * @param userId the admin user ID
   * @param action the action performed
   * @param details additional details
   */
  public void auditLog(String userId, String action, Map<String, Object> details) {
    // Log to application logs
    logger.info("AUDIT: user={}, action={}, details={}", userId, action, details);
    
    // TODO: Implement persistent audit logging
    // Could store in database, send to SIEM, etc.
  }

  // ============================================================================
  // Private Helper Methods
  // ============================================================================

  /**
   * Get all users with face credentials.
   */
  private List<UserModel> getUsersWithFaceCredentials() {
    return session.users().searchForUserStream(realm, Map.of())
        .filter(user -> hasFaceCredential(user))
        .collect(Collectors.toList());
  }

  /**
   * Check if user has face credential.
   */
  private boolean hasFaceCredential(UserModel user) {
    return user.credentialManager().getStoredCredentialsStream()
        .anyMatch(c -> FaceCredentialProvider.TYPE.equals(c.getType()));
  }

  /**
   * Get user's face credential.
   */
  private CredentialModel getFaceCredential(UserModel user) {
    return user.credentialManager().getStoredCredentialsStream()
        .filter(c -> FaceCredentialProvider.TYPE.equals(c.getType()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Get face credential enrollment date.
   */
  private Instant getFaceCredentialEnrollmentDate(UserModel user) {
    CredentialModel credential = getFaceCredential(user);
    if (credential != null && credential.getCreatedDate() != null) {
      return Instant.ofEpochMilli(credential.getCreatedDate());
    }
    return null;
  }

  /**
   * Find user by class ID.
   */
  private UserModel findUserByClassId(long classId) {
    return session.users().searchForUserStream(realm, Map.of())
        .filter(user -> {
          CredentialModel credential = getFaceCredential(user);
          if (credential != null) {
            FaceCredentialModel faceCredential = 
                FaceCredentialModel.createFromCredentialModel(credential);
            return faceCredential != null && faceCredential.getClassId() == classId;
          }
          return false;
        })
        .findFirst()
        .orElse(null);
  }

  /**
   * Create BioID client for BWS operations.
   */
  private BioIdClient createBioIdClient() {
    try {
      BioIdConfiguration config = BioIdConfiguration.getInstance();
      
      if (config.getClientId() == null || config.getClientId().trim().isEmpty() ||
          config.getKey() == null || config.getKey().trim().isEmpty()) {
        logger.warn("BioID credentials not configured");
        return null;
      }
      
      return new com.bioid.keycloak.client.BioIdGrpcClientProduction(
          config, config.getEndpoint(), config.getClientId(), config.getKey());
          
    } catch (Exception e) {
      logger.error("Failed to create BioID client", e);
      return null;
    }
  }
}
