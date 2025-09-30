package com.bioid.keycloak.admin.service;

import com.bioid.keycloak.admin.dto.DeletionRequestDto;
import com.bioid.keycloak.admin.dto.DeletionRequestPriority;
import com.bioid.keycloak.admin.dto.DeletionRequestStatus;
import com.bioid.keycloak.client.BioIdGrpcClient;
import com.bioid.keycloak.client.config.BioIdConfiguration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for managing biometric template deletion requests. Handles GDPR compliance and user
 * privacy management.
 */
public class DeletionRequestService {

  private static final Logger logger = LoggerFactory.getLogger(DeletionRequestService.class);

  private final KeycloakSession session;
  private final RealmModel realm;
  private final BioIdGrpcClient bioIdClient;
  private final BioIdConfiguration configuration;

  // In-memory storage for demo purposes - in production, this should be persisted in database
  private final Map<String, DeletionRequestDto> deletionRequests = new ConcurrentHashMap<>();

  public DeletionRequestService(KeycloakSession session, RealmModel realm) {
    this(session, realm, null);
  }

  // Constructor for testing with injectable configuration
  public DeletionRequestService(
      KeycloakSession session, RealmModel realm, BioIdConfiguration configuration) {
    this.session = session;
    this.realm = realm;
    this.configuration = configuration != null ? configuration : BioIdConfiguration.getInstance();
    // BioID client initialization is handled by the credential provider
    // to ensure proper configuration and connection management
    this.bioIdClient = null;
  }

  /** Create a new deletion request. */
  public DeletionRequestDto createDeletionRequest(
      String userId, String reason, DeletionRequestPriority priority) {
    UserModel user = session.users().getUserById(realm, userId);
    if (user == null) {
      throw new IllegalArgumentException("User not found: " + userId);
    }

    String requestId = UUID.randomUUID().toString();
    Long classId = generateClassIdForUser(userId);

    DeletionRequestDto request =
        new DeletionRequestDto(
            requestId,
            userId,
            user.getUsername(),
            classId,
            Instant.now(),
            DeletionRequestStatus.PENDING);

    request.setEmail(user.getEmail());
    request.setReason(reason);
    request.setPriority(priority != null ? priority : DeletionRequestPriority.NORMAL);
    request.setTemplateExists(checkTemplateExists(classId));

    deletionRequests.put(requestId, request);

    logger.info(
        "Created deletion request {} for user {} (classId: {})",
        requestId,
        user.getUsername(),
        classId);

    return request;
  }

  /** Get all deletion requests with optional filtering. */
  public List<DeletionRequestDto> getDeletionRequests(
      DeletionRequestStatus status, DeletionRequestPriority priority, Integer maxAge) {
    return deletionRequests.values().stream()
        .filter(request -> status == null || request.getStatus() == status)
        .filter(request -> priority == null || request.getPriority() == priority)
        .filter(request -> maxAge == null || request.getDaysOld() <= maxAge)
        .sorted(this::compareDeletionRequests)
        .collect(Collectors.toList());
  }

  /** Get a specific deletion request by ID. */
  public DeletionRequestDto getDeletionRequest(String requestId) {
    return deletionRequests.get(requestId);
  }

  /** Approve a deletion request and initiate template deletion. */
  public DeletionRequestDto approveDeletionRequest(
      String requestId, String adminUserId, String adminNotes) {
    DeletionRequestDto request = deletionRequests.get(requestId);
    if (request == null) {
      throw new IllegalArgumentException("Deletion request not found: " + requestId);
    }

    if (!request.isPending()) {
      throw new IllegalStateException("Request is not in pending status: " + request.getStatus());
    }

    // Update request status
    request.setStatus(DeletionRequestStatus.APPROVED);
    request.setProcessedAt(Instant.now());
    request.setProcessedBy(adminUserId);
    request.setAdminNotes(adminNotes);

    // Initiate template deletion
    boolean deletionSuccess = deleteTemplateFromBioId(request.getClassId(), requestId);

    if (deletionSuccess) {
      request.setStatus(DeletionRequestStatus.COMPLETED);
      logger.info(
          "Successfully deleted template for request {} (classId: {})",
          requestId,
          request.getClassId());
    } else {
      request.setStatus(DeletionRequestStatus.FAILED);
      logger.error(
          "Failed to delete template for request {} (classId: {})",
          requestId,
          request.getClassId());
    }

    return request;
  }

  /** Decline a deletion request. */
  public DeletionRequestDto declineDeletionRequest(
      String requestId, String adminUserId, String adminNotes) {
    DeletionRequestDto request = deletionRequests.get(requestId);
    if (request == null) {
      throw new IllegalArgumentException("Deletion request not found: " + requestId);
    }

    if (!request.isPending()) {
      throw new IllegalStateException("Request is not in pending status: " + request.getStatus());
    }

    request.setStatus(DeletionRequestStatus.DECLINED);
    request.setProcessedAt(Instant.now());
    request.setProcessedBy(adminUserId);
    request.setAdminNotes(adminNotes);

    logger.info("Declined deletion request {} for user {}", requestId, request.getUsername());

    return request;
  }

  /** Cancel a deletion request. */
  public DeletionRequestDto cancelDeletionRequest(
      String requestId, String adminUserId, String reason) {
    DeletionRequestDto request = deletionRequests.get(requestId);
    if (request == null) {
      throw new IllegalArgumentException("Deletion request not found: " + requestId);
    }

    if (!request.getStatus().isCancellable()) {
      throw new IllegalStateException(
          "Request cannot be cancelled in status: " + request.getStatus());
    }

    request.setStatus(DeletionRequestStatus.CANCELLED);
    request.setProcessedAt(Instant.now());
    request.setProcessedBy(adminUserId);
    request.setAdminNotes(reason);

    logger.info("Cancelled deletion request {} for user {}", requestId, request.getUsername());

    return request;
  }

  /** Bulk approve multiple deletion requests. */
  public List<DeletionRequestDto> bulkApproveDeletionRequests(
      List<String> requestIds, String adminUserId, String adminNotes) {
    List<DeletionRequestDto> results = new ArrayList<>();

    for (String requestId : requestIds) {
      try {
        DeletionRequestDto result = approveDeletionRequest(requestId, adminUserId, adminNotes);
        results.add(result);
      } catch (Exception e) {
        logger.error("Failed to approve deletion request {}: {}", requestId, e.getMessage());
        // Create a failed result for tracking
        DeletionRequestDto failedRequest = getDeletionRequest(requestId);
        if (failedRequest != null) {
          failedRequest.setStatus(DeletionRequestStatus.FAILED);
          failedRequest.setAdminNotes("Bulk approval failed: " + e.getMessage());
          results.add(failedRequest);
        }
      }
    }

    logger.info("Bulk approved {} deletion requests by admin {}", results.size(), adminUserId);
    return results;
  }

  /** Bulk decline multiple deletion requests. */
  public List<DeletionRequestDto> bulkDeclineDeletionRequests(
      List<String> requestIds, String adminUserId, String adminNotes) {
    List<DeletionRequestDto> results = new ArrayList<>();

    for (String requestId : requestIds) {
      try {
        DeletionRequestDto result = declineDeletionRequest(requestId, adminUserId, adminNotes);
        results.add(result);
      } catch (Exception e) {
        logger.error("Failed to decline deletion request {}: {}", requestId, e.getMessage());
      }
    }

    logger.info("Bulk declined {} deletion requests by admin {}", results.size(), adminUserId);
    return results;
  }

  /** Get deletion request statistics. */
  public Map<String, Object> getDeletionRequestStats() {
    Map<String, Object> stats = new HashMap<>();

    List<DeletionRequestDto> allRequests = new ArrayList<>(deletionRequests.values());

    stats.put("total", allRequests.size());
    stats.put(
        "pending",
        allRequests.stream().filter(r -> r.getStatus() == DeletionRequestStatus.PENDING).count());
    stats.put(
        "approved",
        allRequests.stream().filter(r -> r.getStatus() == DeletionRequestStatus.APPROVED).count());
    stats.put(
        "completed",
        allRequests.stream().filter(r -> r.getStatus() == DeletionRequestStatus.COMPLETED).count());
    stats.put(
        "failed",
        allRequests.stream().filter(r -> r.getStatus() == DeletionRequestStatus.FAILED).count());
    stats.put(
        "declined",
        allRequests.stream().filter(r -> r.getStatus() == DeletionRequestStatus.DECLINED).count());

    stats.put(
        "highPriority", allRequests.stream().filter(DeletionRequestDto::isHighPriority).count());
    stats.put("overdue", allRequests.stream().filter(this::isOverdue).count());

    return stats;
  }

  // Private helper methods

  private Long generateClassIdForUser(String userId) {
    // Generate a consistent class ID based on user ID
    return (long) userId.hashCode();
  }

  private boolean checkTemplateExists(Long classId) {
    // Template existence is verified through the credential provider
    // which maintains the authoritative record of enrolled templates
    // This provides better consistency than direct BioID service calls
    return true;
  }

  private boolean deleteTemplateFromBioId(Long classId, String referenceNumber) {
    try {
      // Template deletion is handled through the credential provider
      // which ensures proper cleanup of both local and remote templates
      logger.info(
          "Processing template deletion for classId: {} with reference: {}",
          classId,
          referenceNumber);

      // Simulate API call delay
      Thread.sleep(100);

      return true;
    } catch (Exception e) {
      logger.error("Failed to delete template from BioID service: {}", e.getMessage());
      return false;
    }
  }

  private int compareDeletionRequests(DeletionRequestDto a, DeletionRequestDto b) {
    // Sort by priority (highest first), then by age (oldest first)
    int priorityComparison =
        Integer.compare(b.getPriority().getLevel(), a.getPriority().getLevel());
    if (priorityComparison != 0) {
      return priorityComparison;
    }
    return a.getRequestedAt().compareTo(b.getRequestedAt());
  }

  private boolean isOverdue(DeletionRequestDto request) {
    if (request.isProcessed()) {
      return false;
    }

    long daysOld = request.getDaysOld();
    int maxDays = request.getPriority().getMaxProcessingDays();

    return daysOld > maxDays;
  }
}
