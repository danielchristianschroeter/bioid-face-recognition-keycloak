package com.bioid.keycloak.authenticator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for FaceAuthenticatorFactory. */
class FaceAuthenticatorFactoryTest {

  @Mock private KeycloakSession mockSession;

  @Mock private AuthenticatorConfigModel mockConfig;

  private FaceAuthenticatorFactory factory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    // Enable test mode to skip configuration validation
    System.setProperty("bioid.test.mode", "true");

    factory = new FaceAuthenticatorFactory();
  }

  @AfterEach
  void tearDown() {
    // Clean up test mode system property
    System.clearProperty("bioid.test.mode");
  }

  @Test
  void testGetDisplayType() {
    assertEquals("Face Recognition", factory.getDisplayType());
  }

  @Test
  void testGetReferenceCategory() {
    assertEquals("biometric", factory.getReferenceCategory());
  }

  @Test
  void testIsConfigurable() {
    assertTrue(factory.isConfigurable());
  }

  @Test
  void testGetRequirementChoices() {
    AuthenticationExecutionModel.Requirement[] requirements = factory.getRequirementChoices();

    assertEquals(3, requirements.length);
    assertEquals(AuthenticationExecutionModel.Requirement.REQUIRED, requirements[0]);
    assertEquals(AuthenticationExecutionModel.Requirement.ALTERNATIVE, requirements[1]);
    assertEquals(AuthenticationExecutionModel.Requirement.DISABLED, requirements[2]);
  }

  @Test
  void testIsUserSetupAllowed() {
    assertTrue(factory.isUserSetupAllowed());
  }

  @Test
  void testGetHelpText() {
    assertEquals(
        "Validates user identity using face biometric verification", factory.getHelpText());
  }

  @Test
  void testGetConfigProperties() {
    List<ProviderConfigProperty> properties = factory.getConfigProperties();

    assertNotNull(properties);
    assertEquals(5, properties.size());

    // Check that all expected properties are present
    assertTrue(properties.stream().anyMatch(p -> "maxRetries".equals(p.getName())));
    assertTrue(properties.stream().anyMatch(p -> "verificationThreshold".equals(p.getName())));
    assertTrue(properties.stream().anyMatch(p -> "timeoutSeconds".equals(p.getName())));
    assertTrue(properties.stream().anyMatch(p -> "livenessMode".equals(p.getName())));
    assertTrue(properties.stream().anyMatch(p -> "fallbackEnabled".equals(p.getName())));
  }

  @Test
  void testCreate() {
    FaceAuthenticator authenticator = (FaceAuthenticator) factory.create(mockSession);

    assertNotNull(authenticator);
    assertTrue(authenticator instanceof FaceAuthenticator);
  }

  @Test
  void testGetId() {
    assertEquals("face-authenticator", factory.getId());
  }

  @Test
  void testGetMaxRetriesWithConfig() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("maxRetries", "5");
    when(mockConfig.getConfig()).thenReturn(configMap);

    assertEquals(5, FaceAuthenticatorFactory.getMaxRetries(mockConfig));
  }

  @Test
  void testGetMaxRetriesWithNullConfig() {
    assertEquals(3, FaceAuthenticatorFactory.getMaxRetries(null));
  }

  @Test
  void testGetMaxRetriesWithInvalidValue() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("maxRetries", "invalid");
    when(mockConfig.getConfig()).thenReturn(configMap);

    assertEquals(3, FaceAuthenticatorFactory.getMaxRetries(mockConfig));
  }

  @Test
  void testGetVerificationThresholdWithConfig() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("verificationThreshold", "0.025");
    when(mockConfig.getConfig()).thenReturn(configMap);

    assertEquals(0.025, FaceAuthenticatorFactory.getVerificationThreshold(mockConfig), 0.001);
  }

  @Test
  void testGetVerificationThresholdWithNullConfig() {
    assertEquals(0.015, FaceAuthenticatorFactory.getVerificationThreshold(null), 0.001);
  }

  @Test
  void testGetVerificationThresholdWithInvalidValue() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("verificationThreshold", "not-a-number");
    when(mockConfig.getConfig()).thenReturn(configMap);

    assertEquals(0.015, FaceAuthenticatorFactory.getVerificationThreshold(mockConfig), 0.001);
  }

  @Test
  void testGetTimeoutSecondsWithConfig() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("timeoutSeconds", "10");
    when(mockConfig.getConfig()).thenReturn(configMap);

    assertEquals(10, FaceAuthenticatorFactory.getTimeoutSeconds(mockConfig));
  }

  @Test
  void testGetTimeoutSecondsWithNullConfig() {
    assertEquals(4, FaceAuthenticatorFactory.getTimeoutSeconds(null));
  }

  @Test
  void testGetLivenessModeWithConfig() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("livenessMode", "ACTIVE");
    when(mockConfig.getConfig()).thenReturn(configMap);

    assertEquals("ACTIVE", FaceAuthenticatorFactory.getLivenessMode(mockConfig));
  }

  @Test
  void testGetLivenessModeWithNullConfig() {
    assertEquals("PASSIVE", FaceAuthenticatorFactory.getLivenessMode(null));
  }

  @Test
  void testIsFallbackEnabledWithConfig() {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("fallbackEnabled", "false");
    when(mockConfig.getConfig()).thenReturn(configMap);

    assertFalse(FaceAuthenticatorFactory.isFallbackEnabled(mockConfig));
  }

  @Test
  void testIsFallbackEnabledWithNullConfig() {
    assertTrue(FaceAuthenticatorFactory.isFallbackEnabled(null));
  }

  @Test
  void testIsFallbackEnabledWithNullValue() {
    Map<String, String> configMap = new HashMap<>();
    // Don't put fallbackEnabled in the map
    when(mockConfig.getConfig()).thenReturn(configMap);

    assertTrue(FaceAuthenticatorFactory.isFallbackEnabled(mockConfig));
  }
}
