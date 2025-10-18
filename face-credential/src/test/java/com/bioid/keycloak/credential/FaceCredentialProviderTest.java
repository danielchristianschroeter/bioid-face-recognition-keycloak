package com.bioid.keycloak.credential;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bioid.keycloak.client.BioIdClient;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SubjectCredentialManager;
import org.keycloak.models.UserModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for FaceCredentialProvider. Tests credential storage, retrieval, validation, and
 * lifecycle management.
 */
@ExtendWith(MockitoExtension.class)
class FaceCredentialProviderTest {

  @Mock
  private KeycloakSession session;

  @Mock
  private RealmModel realm;

  @Mock
  private UserModel user;

  @Mock
  private SubjectCredentialManager credentialManager;

  @Mock
  private BioIdClient bioIdClient;

  private FaceCredentialProvider provider;
  private FaceCredentialModel testCredential;

  @BeforeEach
  void setUp() {
    provider = new FaceCredentialProvider(session, bioIdClient);

    lenient().when(user.credentialManager()).thenReturn(credentialManager);
    lenient().when(user.getId()).thenReturn("test-user-id");
    lenient().when(realm.getName()).thenReturn("test-realm");

    // Create test credential model
    testCredential = FaceCredentialModel.createFaceCredential(123456789L, 3, 5, 3, 3,
        Instant.now().plus(730, ChronoUnit.DAYS), List.of("employee", "high-security"),
        FaceCredentialModel.TemplateType.STANDARD, "NEW_TEMPLATE_CREATED",
        new FaceCredentialModel.EnrollmentMetadata("Chrome/120.0", "192.168.1.100",
            "device-fingerprint-hash", "bws-job-id-123"));
  }

  @Test
  @DisplayName("Should return correct credential type")
  void shouldReturnCorrectCredentialType() {
    assertThat(provider.getType()).isEqualTo("face-biometric");
  }

  @Test
  @DisplayName("Should create credential successfully")
  void shouldCreateCredentialSuccessfully() {
    // Given
    CredentialModel expectedCredential = new CredentialModel();
    when(credentialManager.createStoredCredential(any(CredentialModel.class)))
        .thenReturn(expectedCredential);

    // When
    CredentialModel result = provider.createCredential(realm, user, testCredential);

    // Then
    assertThat(result).isNotNull();
    verify(credentialManager).createStoredCredential(argThat(credential -> {
      assertThat(credential.getType()).isEqualTo("face-biometric");
      assertThat(credential.getUserLabel()).contains("Face Recognition");
      assertThat(credential.getCredentialData()).isNotEmpty();
      assertThat(credential.getSecretData()).isNotEmpty();
      return true;
    }));
  }

  @Test
  @DisplayName("Should throw exception when creating credential with null model")
  void shouldThrowExceptionWhenCreatingCredentialWithNullModel() {
    assertThatThrownBy(() -> provider.createCredential(realm, user, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Face credential model cannot be null");
  }

  @Test
  @DisplayName("Should delete credential successfully")
  void shouldDeleteCredentialSuccessfully() {
    // Given
    String credentialId = "test-credential-id";
    CredentialModel credential = createMockCredentialModel(credentialId, testCredential);

    when(credentialManager.getStoredCredentialById(credentialId)).thenReturn(credential);
    when(credentialManager.removeStoredCredentialById(credentialId)).thenReturn(true);

    // Unit test: Pass null BioIdClient to test credential deletion logic in isolation
    // In production, the factory injects the real gRPC client
    FaceCredentialProvider testProvider = new FaceCredentialProvider(session, null);

    // When
    boolean result = testProvider.deleteCredential(realm, user, credentialId);

    // Then
    assertThat(result).isTrue();
    verify(credentialManager).removeStoredCredentialById(credentialId);
  }

  @Test
  @DisplayName("Should return false when deleting non-existent credential")
  void shouldReturnFalseWhenDeletingNonExistentCredential() {
    // Given
    String credentialId = "non-existent-id";
    when(credentialManager.getStoredCredentialById(credentialId)).thenReturn(null);

    // When
    boolean result = provider.deleteCredential(realm, user, credentialId);

    // Then
    assertThat(result).isFalse();
    verify(credentialManager, never()).removeStoredCredentialById(any());
  }

  @Test
  @DisplayName("Should return false when deleting credential of wrong type")
  void shouldReturnFalseWhenDeletingCredentialOfWrongType() {
    // Given
    String credentialId = "wrong-type-credential";
    CredentialModel credential = new CredentialModel();
    credential.setId(credentialId);
    credential.setType("password"); // Wrong type

    when(credentialManager.getStoredCredentialById(credentialId)).thenReturn(credential);

    // When
    boolean result = provider.deleteCredential(realm, user, credentialId);

    // Then
    assertThat(result).isFalse();
    verify(credentialManager, never()).removeStoredCredentialById(any());
  }

  @Test
  @DisplayName("Should deserialize credential from model successfully")
  void shouldDeserializeCredentialFromModelSuccessfully() {
    // Given
    CredentialModel credentialModel = createMockCredentialModel("test-id", testCredential);

    // When
    FaceCredentialModel result = provider.getCredentialFromModel(credentialModel);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getClassId()).isEqualTo(testCredential.getClassId());
    assertThat(result.getImageCount()).isEqualTo(testCredential.getImageCount());
    assertThat(result.getEncoderVersion()).isEqualTo(testCredential.getEncoderVersion());
    assertThat(result.getTemplateType()).isEqualTo(testCredential.getTemplateType());
    assertThat(result.getTags()).containsExactlyElementsOf(testCredential.getTags());
  }

  @Test
  @DisplayName("Should return null when deserializing null model")
  void shouldReturnNullWhenDeserializingNullModel() {
    assertThat(provider.getCredentialFromModel(null)).isNull();
  }

  @Test
  @DisplayName("Should return null when deserializing wrong credential type")
  void shouldReturnNullWhenDeserializingWrongCredentialType() {
    // Given
    CredentialModel credential = new CredentialModel();
    credential.setType("password");

    // When
    FaceCredentialModel result = provider.getCredentialFromModel(credential);

    // Then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should get face credentials for user")
  void shouldGetFaceCredentialsForUser() {
    // Given
    CredentialModel credential1 = createMockCredentialModel("cred-1", testCredential);
    CredentialModel credential2 = createMockCredentialModel("cred-2", testCredential);
    CredentialModel passwordCredential = new CredentialModel();
    passwordCredential.setType("password");

    when(credentialManager.getStoredCredentialsStream())
        .thenReturn(Stream.of(credential1, credential2, passwordCredential));

    // When
    List<FaceCredentialModel> result = provider.getFaceCredentials(realm, user).toList();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(cred -> cred.getClassId() == testCredential.getClassId());
  }

  @Test
  @DisplayName("Should get most recent face credential")
  void shouldGetMostRecentFaceCredential() {
    // Given
    Instant now = Instant.now();
    FaceCredentialModel olderCredential =
        FaceCredentialModel.createFaceCredential(111L, 3, 5, 3, 3, now.plus(730, ChronoUnit.DAYS),
            List.of(), FaceCredentialModel.TemplateType.STANDARD, "NEW_TEMPLATE_CREATED", null);
    FaceCredentialModel newerCredential =
        FaceCredentialModel.createFaceCredential(222L, 3, 5, 3, 3, now.plus(730, ChronoUnit.DAYS),
            List.of(), FaceCredentialModel.TemplateType.STANDARD, "NEW_TEMPLATE_CREATED", null);

    CredentialModel cred1 = createMockCredentialModel("cred-1", olderCredential);
    cred1.setCreatedDate(now.toEpochMilli()); // Older
    CredentialModel cred2 = createMockCredentialModel("cred-2", newerCredential);
    cred2.setCreatedDate(now.plus(1, ChronoUnit.HOURS).toEpochMilli()); // Newer

    when(credentialManager.getStoredCredentialsStream()).thenReturn(Stream.of(cred1, cred2));

    // When
    FaceCredentialModel result = provider.getMostRecentFaceCredential(realm, user);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getClassId()).isEqualTo(222L);
  }

  @Test
  @DisplayName("Should return null when no face credentials exist")
  void shouldReturnNullWhenNoFaceCredentialsExist() {
    // Given
    when(credentialManager.getStoredCredentialsStream()).thenReturn(Stream.empty());

    // When
    FaceCredentialModel result = provider.getMostRecentFaceCredential(realm, user);

    // Then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should check if user has valid face credentials")
  void shouldCheckIfUserHasValidFaceCredentials() {
    // Given
    CredentialModel credential = createMockCredentialModel("cred-1", testCredential);
    when(credentialManager.getStoredCredentialsStream()).thenReturn(Stream.of(credential));

    // When
    boolean result = provider.hasValidFaceCredentials(realm, user);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should return false when user has no valid face credentials")
  void shouldReturnFalseWhenUserHasNoValidFaceCredentials() {
    // Given - expired credential
    FaceCredentialModel expiredCredential = FaceCredentialModel.createFaceCredential(123L, 3, 5, 3,
        3, Instant.now().minus(1, ChronoUnit.HOURS), // Expired
        List.of(), FaceCredentialModel.TemplateType.STANDARD, "NEW_TEMPLATE_CREATED", null);

    CredentialModel credential = createMockCredentialModel("cred-1", expiredCredential);
    when(credentialManager.getStoredCredentialsStream()).thenReturn(Stream.of(credential));

    // When
    boolean result = provider.hasValidFaceCredentials(realm, user);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should update credential expiration successfully")
  void shouldUpdateCredentialExpirationSuccessfully() {
    // Given
    String credentialId = "test-credential-id";
    Instant newExpiration = Instant.now().plus(365, ChronoUnit.DAYS);
    CredentialModel credential = createMockCredentialModel(credentialId, testCredential);

    when(credentialManager.getStoredCredentialById(credentialId)).thenReturn(credential);

    // When
    boolean result = provider.updateCredentialExpiration(realm, user, credentialId, newExpiration);

    // Then
    assertThat(result).isTrue();
    verify(credentialManager).updateStoredCredential(argThat(cred -> {
      FaceCredentialModel updated = provider.getCredentialFromModel(cred);
      return updated != null && updated.getExpiresAt() != null && Math
          .abs(updated.getExpiresAt().getEpochSecond() - newExpiration.getEpochSecond()) <= 1;
    }));
  }

  @Test
  @DisplayName("Should remove expired credentials")
  void shouldRemoveExpiredCredentials() {
    // Given
    FaceCredentialModel expiredCredential = FaceCredentialModel.createFaceCredential(123L, 3, 5, 3,
        3, Instant.now().minus(1, ChronoUnit.HOURS), // Expired
        List.of(), FaceCredentialModel.TemplateType.STANDARD, "NEW_TEMPLATE_CREATED", null);

    CredentialModel validCredential = createMockCredentialModel("valid-cred", testCredential);
    CredentialModel expiredCredentialModel =
        createMockCredentialModel("expired-cred", expiredCredential);

    when(credentialManager.getStoredCredentialsStream())
        .thenReturn(Stream.of(validCredential, expiredCredentialModel));
    when(credentialManager.removeStoredCredentialById("expired-cred")).thenReturn(true);

    // When
    int result = provider.removeExpiredCredentials(realm, user);

    // Then
    assertThat(result).isEqualTo(1);
    verify(credentialManager).removeStoredCredentialById("expired-cred");
    verify(credentialManager, never()).removeStoredCredentialById("valid-cred");
  }

  @Test
  @DisplayName("Should support face credential type")
  void shouldSupportFaceCredentialType() {
    assertThat(provider.supportsCredentialType("face-biometric")).isTrue();
    assertThat(provider.supportsCredentialType("password")).isFalse();
  }

  @Test
  @DisplayName("Should check if configured for user")
  void shouldCheckIfConfiguredForUser() {
    // Given
    CredentialModel credential = createMockCredentialModel("cred-1", testCredential);
    when(credentialManager.getStoredCredentialsStream()).thenReturn(Stream.of(credential));

    // When
    boolean result = provider.isConfiguredFor(realm, user, "face-biometric");

    // Then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("Should return false for unsupported credential type in isConfiguredFor")
  void shouldReturnFalseForUnsupportedCredentialTypeInIsConfiguredFor() {
    boolean result = provider.isConfiguredFor(realm, user, "password");
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("Should return false for isValid (face credentials validated elsewhere)")
  void shouldReturnFalseForIsValid() {
    // Face credentials are validated through the authenticator, not the provider
    boolean result =
        provider.isValid(realm, user, mock(org.keycloak.credential.CredentialInput.class));
    assertThat(result).isFalse();
  }

  private CredentialModel createMockCredentialModel(String id, FaceCredentialModel faceCredential) {
    try {
      CredentialModel credential = new CredentialModel();
      credential.setId(id);
      credential.setType("face-biometric");
      credential.setCredentialData(org.keycloak.util.JsonSerialization
          .writeValueAsString(faceCredential.getFaceCredentialData()));
      credential.setSecretData(org.keycloak.util.JsonSerialization
          .writeValueAsString(faceCredential.getFaceSecretData()));
      credential.setCreatedDate(faceCredential.getCreatedAt().toEpochMilli());

      return credential;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create mock credential", e);
    }
  }
}
