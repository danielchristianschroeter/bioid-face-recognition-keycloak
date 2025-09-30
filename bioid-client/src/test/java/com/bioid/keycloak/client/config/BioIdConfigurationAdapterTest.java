package com.bioid.keycloak.client.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for BioIdConfigurationAdapter. */
class BioIdConfigurationAdapterTest {

  @Mock private BioIdConfiguration mockConfiguration;

  private BioIdConfigurationAdapter adapter;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    adapter = new BioIdConfigurationAdapter(mockConfiguration);
  }

  @Test
  void testGetEndpoint() {
    when(mockConfiguration.getEndpoint()).thenReturn("grpcs://test.example.com:443");

    assertEquals("grpcs://test.example.com:443", adapter.getEndpoint());
    verify(mockConfiguration).getEndpoint();
  }

  @Test
  void testGetClientId() {
    when(mockConfiguration.getClientId()).thenReturn("test-client-id");

    assertEquals("test-client-id", adapter.getClientId());
    verify(mockConfiguration).getClientId();
  }

  @Test
  void testGetSecretKey() {
    when(mockConfiguration.getKey()).thenReturn("test-secret-key");

    assertEquals("test-secret-key", adapter.getSecretKey());
    verify(mockConfiguration).getKey();
  }

  @Test
  void testGetJwtExpireMinutes() {
    when(mockConfiguration.getJwtExpireMinutes()).thenReturn(90);

    assertEquals(90, adapter.getJwtExpireMinutes());
    verify(mockConfiguration).getJwtExpireMinutes();
  }

  @Test
  void testGetVerificationTimeout() {
    when(mockConfiguration.getVerificationTimeout()).thenReturn(Duration.ofSeconds(5));

    assertEquals(Duration.ofSeconds(5), adapter.getVerificationTimeout());
    verify(mockConfiguration).getVerificationTimeout();
  }

  @Test
  void testGetEnrollmentTimeout() {
    when(mockConfiguration.getEnrollmentTimeout()).thenReturn(Duration.ofSeconds(10));

    assertEquals(Duration.ofSeconds(10), adapter.getEnrollmentTimeout());
    verify(mockConfiguration).getEnrollmentTimeout();
  }

  @Test
  void testGetMaxRetryAttempts() {
    when(mockConfiguration.getRetryMaxAttempts()).thenReturn(5);

    assertEquals(5, adapter.getMaxRetryAttempts());
    verify(mockConfiguration).getRetryMaxAttempts();
  }

  @Test
  void testGetRetryBackoffMultiplier() {
    when(mockConfiguration.getRetryBackoffMultiplier()).thenReturn(1.5);

    assertEquals(1.5, adapter.getRetryBackoffMultiplier(), 0.001);
    verify(mockConfiguration).getRetryBackoffMultiplier();
  }

  @Test
  void testCreateClientConfig() {
    // Setup mock configuration
    when(mockConfiguration.getEndpoint()).thenReturn("grpcs://test.example.com:443");
    when(mockConfiguration.getClientId()).thenReturn("test-client");
    when(mockConfiguration.getKey()).thenReturn("test-key");
    when(mockConfiguration.getJwtExpireMinutes()).thenReturn(90);
    when(mockConfiguration.getChannelPoolSize()).thenReturn(10);
    when(mockConfiguration.getKeepAliveTime()).thenReturn(Duration.ofSeconds(45));
    when(mockConfiguration.getVerificationTimeout()).thenReturn(Duration.ofSeconds(5));
    when(mockConfiguration.getEnrollmentTimeout()).thenReturn(Duration.ofSeconds(15));
    when(mockConfiguration.getRetryMaxAttempts()).thenReturn(5);
    when(mockConfiguration.getRetryBackoffMultiplier()).thenReturn(1.5);
    when(mockConfiguration.getHealthCheckInterval()).thenReturn(Duration.ofSeconds(60));

    BioIdClientConfig clientConfig = adapter.createClientConfig();

    assertNotNull(clientConfig);
    assertEquals("grpcs://test.example.com:443", clientConfig.endpoint());
    assertEquals("test-client", clientConfig.clientId());
    assertEquals("test-key", clientConfig.secretKey());
    assertEquals(90, clientConfig.jwtExpireMinutes());
    assertEquals(10, clientConfig.channelPoolSize());
    assertEquals(Duration.ofSeconds(45), clientConfig.keepAliveTime());
    assertEquals(Duration.ofSeconds(5), clientConfig.verificationTimeout());
    assertEquals(Duration.ofSeconds(15), clientConfig.enrollmentTimeout());
    assertEquals(5, clientConfig.maxRetryAttempts());
    assertEquals(1.5, clientConfig.retryBackoffMultiplier(), 0.001);
    assertEquals(Duration.ofSeconds(60), clientConfig.healthCheckInterval());
  }

  @Test
  void testGetConfiguration() {
    assertEquals(mockConfiguration, adapter.getConfiguration());
  }

  @Test
  void testReload() {
    adapter.reload();
    verify(mockConfiguration).reload();
  }

  @Test
  void testCheckAndReload() {
    adapter.checkAndReload();
    verify(mockConfiguration).checkAndReload();
  }

  @Test
  void testDefaultConstructor() {
    // Enable test mode and set required properties to avoid validation errors
    System.setProperty("bioid.test.mode", "true");
    System.setProperty("bioid.clientId", "test-client");
    System.setProperty("bioid.key", "test-key");

    try {
      // Reset singleton to pick up test properties
      BioIdConfiguration.instance = null;

      // Test that default constructor uses singleton instance
      BioIdConfigurationAdapter defaultAdapter = new BioIdConfigurationAdapter();
      assertNotNull(defaultAdapter.getConfiguration());
      assertEquals(BioIdConfiguration.getInstance(), defaultAdapter.getConfiguration());
    } finally {
      // Clean up
      System.clearProperty("bioid.test.mode");
      System.clearProperty("bioid.clientId");
      System.clearProperty("bioid.key");
      BioIdConfiguration.instance = null;
    }
  }
}
