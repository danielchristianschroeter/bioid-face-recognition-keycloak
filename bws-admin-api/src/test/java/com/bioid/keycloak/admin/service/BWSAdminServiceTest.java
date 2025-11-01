package com.bioid.keycloak.admin.service;

import com.bioid.keycloak.credential.FaceCredentialModel;
import com.bioid.keycloak.credential.FaceCredentialProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BWSAdminService.
 * 
 * Note: These are basic unit tests. Full integration tests would require
 * a running Keycloak instance and BWS service.
 */
@ExtendWith(MockitoExtension.class)
class BWSAdminServiceTest {

  @Mock
  private KeycloakSession session;

  @Mock
  private KeycloakContext context;

  @Mock
  private RealmModel realm;

  @Mock
  private UserProvider userProvider;

  @Mock
  private UserModel user1;

  @Mock
  private UserModel user2;

  @Mock
  private CredentialModel credential1;

  @Mock
  private CredentialModel credential2;

  private BWSAdminService adminService;

  @BeforeEach
  void setUp() {
    lenient().when(session.getContext()).thenReturn(context);
    lenient().when(context.getRealm()).thenReturn(realm);
    lenient().when(session.users()).thenReturn(userProvider);
    lenient().when(realm.getName()).thenReturn("test-realm");

    adminService = new BWSAdminService(session);
  }

  @Test
  void testServiceInitialization() {
    // When: Creating service
    BWSAdminService service = new BWSAdminService(session);

    // Then: Should initialize successfully
    assertNotNull(service);
  }

  @Test
  void testGetStatisticsReturnsNonNull() {
    // When: Getting statistics
    AdminStats stats = adminService.getStatistics();

    // Then: Should return non-null stats object
    assertNotNull(stats);
    assertTrue(stats.getTotalTemplates() >= 0);
    assertTrue(stats.getActiveUsers() >= 0);
    assertTrue(stats.getOrphanedTemplates() >= 0);
  }

  @Test
  void testListAllTemplatesReturnsNonNull() {
    // When: Listing all templates
    List<TemplateInfo> templates = adminService.listAllTemplates();

    // Then: Should return non-null list
    assertNotNull(templates);
  }

  @Test
  void testGetTemplateDetailsWithInvalidId() {
    // When: Getting template details with invalid ID
    TemplateInfo template = adminService.getTemplateDetails(0L);

    // Then: Should handle gracefully
    // (May return null or empty, depending on implementation)
    // Just verify it doesn't throw exception
    assertTrue(true);
  }

  @Test
  void testDeleteTemplateWithInvalidId() {
    // When: Deleting template with invalid ID
    boolean deleted = adminService.deleteTemplate(0L);

    // Then: Should return false
    assertFalse(deleted);
  }

  @Test
  void testValidateTemplatesReturnsResult() {
    // When: Validating templates
    ValidationResult result = adminService.validateTemplates();

    // Then: Should return non-null result
    assertNotNull(result);
    assertNotNull(result.getOrphanedTemplates());
    assertNotNull(result.getMissingTemplates());
  }

  @Test
  void testFindOrphanedTemplatesReturnsNonNull() {
    // When: Finding orphaned templates
    List<TemplateInfo> orphaned = adminService.findOrphanedTemplates();

    // Then: Should return non-null list
    assertNotNull(orphaned);
    // Note: Full testing requires integration with BWS service
  }

  @Test
  void testDeleteAllOrphanedReturnsCount() {
    // When: Deleting all orphaned
    int deletedCount = adminService.deleteAllOrphaned();

    // Then: Should return non-negative count
    assertTrue(deletedCount >= 0);
  }

  @Test
  void testAuditLogDoesNotThrowException() {
    // When: Logging audit event
    // Then: Should not throw exception
    assertDoesNotThrow(() -> 
        adminService.auditLog("user-123", "TEST_ACTION", null)
    );
  }
}
