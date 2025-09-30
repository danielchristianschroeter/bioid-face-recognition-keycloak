package com.bioid.keycloak.admin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bioid.keycloak.admin.dto.DeletionRequestDto;
import com.bioid.keycloak.admin.dto.DeletionRequestPriority;
import com.bioid.keycloak.admin.dto.DeletionRequestStatus;
import com.bioid.keycloak.client.config.BioIdConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeletionRequestServiceTest {

  @Mock private KeycloakSession session;

  @Mock private RealmModel realm;

  @Mock private UserProvider userProvider;

  @Mock private UserModel user;

  @Mock private BioIdConfiguration configuration;

  private DeletionRequestService service;

  private static final String USER_ID = "test-user-id";
  private static final String USERNAME = "testuser";
  private static final String EMAIL = "test@example.com";
  private static final String ADMIN_ID = "admin-user-id";

  @BeforeEach
  void setUp() {
    when(session.users()).thenReturn(userProvider);
    when(userProvider.getUserById(realm, USER_ID)).thenReturn(user);
    when(user.getUsername()).thenReturn(USERNAME);
    when(user.getEmail()).thenReturn(EMAIL);

    service = new DeletionRequestService(session, realm, configuration);
  }

  @Test
  void shouldCreateDeletionRequest() {
    // Given
    String reason = "GDPR request";
    DeletionRequestPriority priority = DeletionRequestPriority.HIGH;

    // When
    DeletionRequestDto request = service.createDeletionRequest(USER_ID, reason, priority);

    // Then
    assertNotNull(request);
    assertNotNull(request.getId());
    assertEquals(USER_ID, request.getUserId());
    assertEquals(USERNAME, request.getUsername());
    assertEquals(EMAIL, request.getEmail());
    assertEquals(reason, request.getReason());
    assertEquals(priority, request.getPriority());
    assertEquals(DeletionRequestStatus.PENDING, request.getStatus());
    assertTrue(request.getTemplateExists()); // Default is true in our implementation
  }

  @Test
  void shouldGetDeletionRequests() {
    // Given
    service.createDeletionRequest(USER_ID, "Reason 1", DeletionRequestPriority.HIGH);
    service.createDeletionRequest(USER_ID, "Reason 2", DeletionRequestPriority.NORMAL);

    // When
    List<DeletionRequestDto> allRequests = service.getDeletionRequests(null, null, null);
    List<DeletionRequestDto> highPriorityRequests =
        service.getDeletionRequests(null, DeletionRequestPriority.HIGH, null);
    List<DeletionRequestDto> pendingRequests =
        service.getDeletionRequests(DeletionRequestStatus.PENDING, null, null);

    // Then
    assertEquals(2, allRequests.size());
    assertEquals(1, highPriorityRequests.size());
    assertEquals(2, pendingRequests.size());
  }

  @Test
  void shouldApproveDeletionRequest() {
    // Given
    DeletionRequestDto request =
        service.createDeletionRequest(USER_ID, "GDPR request", DeletionRequestPriority.HIGH);
    String requestId = request.getId();
    String adminNotes = "Approved as requested";

    // When
    DeletionRequestDto approvedRequest =
        service.approveDeletionRequest(requestId, ADMIN_ID, adminNotes);

    // Then
    assertNotNull(approvedRequest);
    assertEquals(requestId, approvedRequest.getId());
    assertEquals(
        DeletionRequestStatus.COMPLETED,
        approvedRequest.getStatus()); // Simulated deletion succeeds
    assertEquals(ADMIN_ID, approvedRequest.getProcessedBy());
    assertEquals(adminNotes, approvedRequest.getAdminNotes());
    assertNotNull(approvedRequest.getProcessedAt());
  }

  @Test
  void shouldDeclineDeletionRequest() {
    // Given
    DeletionRequestDto request =
        service.createDeletionRequest(USER_ID, "GDPR request", DeletionRequestPriority.HIGH);
    String requestId = request.getId();
    String adminNotes = "Declined due to verification issues";

    // When
    DeletionRequestDto declinedRequest =
        service.declineDeletionRequest(requestId, ADMIN_ID, adminNotes);

    // Then
    assertNotNull(declinedRequest);
    assertEquals(requestId, declinedRequest.getId());
    assertEquals(DeletionRequestStatus.DECLINED, declinedRequest.getStatus());
    assertEquals(ADMIN_ID, declinedRequest.getProcessedBy());
    assertEquals(adminNotes, declinedRequest.getAdminNotes());
    assertNotNull(declinedRequest.getProcessedAt());
  }

  @Test
  void shouldCancelDeletionRequest() {
    // Given
    DeletionRequestDto request =
        service.createDeletionRequest(USER_ID, "GDPR request", DeletionRequestPriority.HIGH);
    String requestId = request.getId();
    String reason = "User withdrew request";

    // When
    DeletionRequestDto cancelledRequest =
        service.cancelDeletionRequest(requestId, ADMIN_ID, reason);

    // Then
    assertNotNull(cancelledRequest);
    assertEquals(requestId, cancelledRequest.getId());
    assertEquals(DeletionRequestStatus.CANCELLED, cancelledRequest.getStatus());
    assertEquals(ADMIN_ID, cancelledRequest.getProcessedBy());
    assertEquals(reason, cancelledRequest.getAdminNotes());
    assertNotNull(cancelledRequest.getProcessedAt());
  }

  @Test
  void shouldPerformBulkApproval() {
    // Given
    DeletionRequestDto request1 =
        service.createDeletionRequest(USER_ID, "Request 1", DeletionRequestPriority.HIGH);
    DeletionRequestDto request2 =
        service.createDeletionRequest(USER_ID, "Request 2", DeletionRequestPriority.NORMAL);
    List<String> requestIds = List.of(request1.getId(), request2.getId());
    String adminNotes = "Bulk approval";

    // When
    List<DeletionRequestDto> results =
        service.bulkApproveDeletionRequests(requestIds, ADMIN_ID, adminNotes);

    // Then
    assertEquals(2, results.size());
    results.forEach(
        result -> {
          assertEquals(DeletionRequestStatus.COMPLETED, result.getStatus());
          assertEquals(ADMIN_ID, result.getProcessedBy());
          assertEquals(adminNotes, result.getAdminNotes());
        });
  }

  @Test
  void shouldGetDeletionRequestStats() {
    // Given
    service.createDeletionRequest(USER_ID, "Request 1", DeletionRequestPriority.HIGH);
    service.createDeletionRequest(USER_ID, "Request 2", DeletionRequestPriority.NORMAL);

    DeletionRequestDto request3 =
        service.createDeletionRequest(USER_ID, "Request 3", DeletionRequestPriority.URGENT);
    service.approveDeletionRequest(request3.getId(), ADMIN_ID, "Approved");

    DeletionRequestDto request4 =
        service.createDeletionRequest(USER_ID, "Request 4", DeletionRequestPriority.LOW);
    service.declineDeletionRequest(request4.getId(), ADMIN_ID, "Declined");

    // When
    Map<String, Object> stats = service.getDeletionRequestStats();

    // Then
    assertEquals(4, stats.get("total"));
    assertEquals(2L, stats.get("pending"));
    assertEquals(1L, stats.get("completed"));
    assertEquals(1L, stats.get("declined"));
    assertEquals(2L, stats.get("highPriority")); // HIGH + URGENT priorities
    assertTrue((Long) stats.get("overdue") >= 0); // Should be non-negative
  }

  @Test
  void shouldThrowExceptionForNonExistentRequest() {
    // Given
    String nonExistentId = "non-existent-id";

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> service.approveDeletionRequest(nonExistentId, ADMIN_ID, "Notes"));

    assertThrows(
        IllegalArgumentException.class,
        () -> service.declineDeletionRequest(nonExistentId, ADMIN_ID, "Notes"));

    assertThrows(
        IllegalArgumentException.class,
        () -> service.cancelDeletionRequest(nonExistentId, ADMIN_ID, "Reason"));
  }

  @Test
  void shouldThrowExceptionForAlreadyProcessedRequest() {
    // Given
    DeletionRequestDto request =
        service.createDeletionRequest(USER_ID, "GDPR request", DeletionRequestPriority.HIGH);
    String requestId = request.getId();

    // Process the request first
    service.approveDeletionRequest(requestId, ADMIN_ID, "First approval");

    // When & Then - trying to process again should fail
    assertThrows(
        IllegalStateException.class,
        () -> service.approveDeletionRequest(requestId, ADMIN_ID, "Second approval"));

    assertThrows(
        IllegalStateException.class,
        () -> service.declineDeletionRequest(requestId, ADMIN_ID, "Decline after approval"));
  }

  @Test
  void shouldFilterRequestsByAge() {
    // Given
    service.createDeletionRequest(USER_ID, "Old request", DeletionRequestPriority.NORMAL);

    // When
    List<DeletionRequestDto> recentRequests =
        service.getDeletionRequests(null, null, 1); // Last 1 day
    List<DeletionRequestDto> allRequests = service.getDeletionRequests(null, null, null);

    // Then
    assertEquals(1, recentRequests.size()); // Should include recent request
    assertEquals(1, allRequests.size());
  }

  @Test
  void shouldHandleEmptyBulkOperations() {
    // Given
    List<String> emptyList = List.of();

    // When
    List<DeletionRequestDto> results =
        service.bulkApproveDeletionRequests(emptyList, ADMIN_ID, "Notes");

    // Then
    assertTrue(results.isEmpty());
  }

  @Test
  void shouldValidateUserExists() {
    // Given
    String nonExistentUserId = "non-existent-user";
    when(userProvider.getUserById(realm, nonExistentUserId)).thenReturn(null);

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.createDeletionRequest(
                nonExistentUserId, "Reason", DeletionRequestPriority.NORMAL));
  }
}
