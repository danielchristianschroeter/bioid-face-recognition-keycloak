package com.bioid.keycloak.authenticator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.bioid.keycloak.credential.FaceCredentialProvider;
import com.bioid.keycloak.credential.FaceCredentialProviderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for FaceAuthenticator. */
class FaceAuthenticatorTest {

  @Mock private KeycloakSession mockSession;

  @Mock private AuthenticationFlowContext mockContext;

  @Mock private RealmModel mockRealm;

  @Mock private UserModel mockUser;

  @Mock private FaceCredentialProvider mockCredentialProvider;

  @Mock private AuthenticationSessionModel mockAuthSession;

  @Mock private LoginFormsProvider mockLoginFormsProvider;

  private FaceAuthenticator authenticator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Enable test mode to skip configuration validation
    System.setProperty("bioid.test.mode", "true");

    // Setup mock session to return providers - use the correct provider class and ID
    lenient()
        .when(
            mockSession.getProvider(
                eq(CredentialProvider.class), eq(FaceCredentialProviderFactory.PROVIDER_ID)))
        .thenReturn(mockCredentialProvider);

    // Setup mock context
    when(mockContext.getRealm()).thenReturn(mockRealm);
    when(mockContext.getUser()).thenReturn(mockUser);
    when(mockContext.getAuthenticationSession()).thenReturn(mockAuthSession);
    when(mockContext.form()).thenReturn(mockLoginFormsProvider);

    when(mockContext.getSession()).thenReturn(mockSession);

    when(mockLoginFormsProvider.setAttribute(anyString(), any()))
        .thenReturn(mockLoginFormsProvider);
    when(mockUser.getId()).thenReturn("test-user-id");

    authenticator = new FaceAuthenticator(mockSession);
  }

  @AfterEach
  void tearDown() {
    // Clean up test mode system property
    System.clearProperty("bioid.test.mode");
  }

  @Test
  void testRequiresUser() {
    assertTrue(authenticator.requiresUser());
  }

  @Test
  void testConfiguredForWithValidCredentials() {
    when(mockCredentialProvider.hasValidFaceCredentials(mockRealm, mockUser)).thenReturn(true);

    assertTrue(authenticator.configuredFor(mockSession, mockRealm, mockUser));
    verify(mockCredentialProvider, atLeastOnce()).hasValidFaceCredentials(mockRealm, mockUser);
  }

  @Test
  void testConfiguredForWithoutValidCredentials() {
    when(mockCredentialProvider.hasValidFaceCredentials(mockRealm, mockUser)).thenReturn(false);

    assertFalse(authenticator.configuredFor(mockSession, mockRealm, mockUser));
    verify(mockCredentialProvider, atLeastOnce()).hasValidFaceCredentials(mockRealm, mockUser);
  }

  @Test
  void testConfiguredForWithNullCredentialProvider() {
    // Create authenticator with session that returns null credential provider
    KeycloakSession sessionWithoutProvider = mock(KeycloakSession.class);
    when(sessionWithoutProvider.getProvider(
            eq(CredentialProvider.class), eq(FaceCredentialProviderFactory.PROVIDER_ID)))
        .thenReturn(null);

    FaceAuthenticator authenticatorWithoutProvider = new FaceAuthenticator(sessionWithoutProvider);

    // This should throw an exception since the provider is not available
    assertThrows(
        IllegalStateException.class,
        () ->
            authenticatorWithoutProvider.configuredFor(
                sessionWithoutProvider, mockRealm, mockUser));
  }

  @Test
  void testSetRequiredActionsWhenNotConfigured() {
    when(mockCredentialProvider.hasValidFaceCredentials(mockRealm, mockUser)).thenReturn(false);

    authenticator.setRequiredActions(mockSession, mockRealm, mockUser);

    verify(mockUser).addRequiredAction("face-enroll");
  }

  @Test
  void testSetRequiredActionsWhenConfigured() {
    when(mockCredentialProvider.hasValidFaceCredentials(mockRealm, mockUser)).thenReturn(true);

    authenticator.setRequiredActions(mockSession, mockRealm, mockUser);

    verify(mockUser, never()).addRequiredAction(anyString());
  }

  @Test
  void testAuthenticateWithoutValidCredentials() {
    when(mockCredentialProvider.hasValidFaceCredentials(mockRealm, mockUser)).thenReturn(false);

    authenticator.authenticate(mockContext);

    verify(mockContext).attempted();
    verify(mockContext, never()).challenge(any());
  }

  @Test
  void testAuthenticateWithValidCredentials() {
    when(mockCredentialProvider.hasValidFaceCredentials(mockRealm, mockUser)).thenReturn(true);

    authenticator.authenticate(mockContext);

    verify(mockContext).challenge(any());
    verify(mockContext, never()).attempted();
  }

  @Test
  void testClose() {
    // Should not throw any exceptions
    assertDoesNotThrow(() -> authenticator.close());
  }
}
