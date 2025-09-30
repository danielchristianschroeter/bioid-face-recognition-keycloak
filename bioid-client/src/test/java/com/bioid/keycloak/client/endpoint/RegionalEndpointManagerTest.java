package com.bioid.keycloak.client.endpoint;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for RegionalEndpointManager. */
class RegionalEndpointManagerTest {

  private RegionalEndpointManager manager;

  @BeforeEach
  void setUp() {
    manager =
        new RegionalEndpointManager(
            RegionalEndpointManager.Region.EU,
            false, // data residency not required
            Duration.ofSeconds(30),
            Duration.ofSeconds(5),
            3);
  }

  @Test
  @DisplayName("Should initialize with EU as preferred region")
  void shouldInitializeWithEuAsPreferredRegion() {
    assertEquals(RegionalEndpointManager.Region.EU, manager.getPreferredRegion());
    assertEquals(RegionalEndpointManager.EU_ENDPOINT, manager.getPrimaryEndpoint());
    assertFalse(manager.isDataResidencyRequired());
  }

  @Test
  @DisplayName("Should return all regions when data residency not required")
  void shouldReturnAllRegionsWhenDataResidencyNotRequired() {
    List<String> endpoints = manager.getOrderedEndpoints();

    // Should have all 3 regions
    assertEquals(3, endpoints.size());

    // EU should be first (preferred)
    assertEquals(RegionalEndpointManager.EU_ENDPOINT, endpoints.get(0));

    // Should contain all regional endpoints
    assertTrue(endpoints.contains(RegionalEndpointManager.EU_ENDPOINT));
    assertTrue(endpoints.contains(RegionalEndpointManager.US_ENDPOINT));
    assertTrue(endpoints.contains(RegionalEndpointManager.SA_ENDPOINT));
  }

  @Test
  @DisplayName("Should only return preferred region when data residency required")
  void shouldOnlyReturnPreferredRegionWhenDataResidencyRequired() {
    RegionalEndpointManager restrictedManager =
        new RegionalEndpointManager(
            RegionalEndpointManager.Region.US, true // data residency required
            );

    List<String> endpoints = restrictedManager.getOrderedEndpoints();

    // Should only have US region
    assertEquals(1, endpoints.size());
    assertEquals(RegionalEndpointManager.US_ENDPOINT, endpoints.get(0));
    assertEquals(RegionalEndpointManager.US_ENDPOINT, restrictedManager.getPrimaryEndpoint());
  }

  @Test
  @DisplayName("Should report success and update health status")
  void shouldReportSuccessAndUpdateHealthStatus() {
    Duration latency = Duration.ofMillis(150);

    manager.reportSuccess(RegionalEndpointManager.EU_ENDPOINT, latency);

    Map<String, RegionalEndpointManager.EndpointHealth> healthStatus = manager.getHealthStatus();
    RegionalEndpointManager.EndpointHealth euHealth =
        healthStatus.get(RegionalEndpointManager.EU_ENDPOINT);

    assertNotNull(euHealth);
    assertTrue(euHealth.isHealthy());
    assertEquals(latency, euHealth.getLatency());
    assertEquals(0, euHealth.getConsecutiveFailures());
  }

  @Test
  @DisplayName("Should report failure and update health status")
  void shouldReportFailureAndUpdateHealthStatus() {
    String errorMessage = "Connection timeout";

    manager.reportFailure(RegionalEndpointManager.EU_ENDPOINT, errorMessage);

    Map<String, RegionalEndpointManager.EndpointHealth> healthStatus = manager.getHealthStatus();
    RegionalEndpointManager.EndpointHealth euHealth =
        healthStatus.get(RegionalEndpointManager.EU_ENDPOINT);

    assertNotNull(euHealth);
    assertTrue(euHealth.isHealthy()); // Still healthy after 1 failure (threshold is 3)
    assertEquals(1, euHealth.getConsecutiveFailures());
    assertEquals(errorMessage, euHealth.getErrorMessage());
  }

  @Test
  @DisplayName("Should mark endpoint as unhealthy after consecutive failures")
  void shouldMarkEndpointAsUnhealthyAfterConsecutiveFailures() {
    String errorMessage = "Connection failed";

    // Report 3 consecutive failures (threshold)
    for (int i = 0; i < 3; i++) {
      manager.reportFailure(RegionalEndpointManager.EU_ENDPOINT, errorMessage);
    }

    Map<String, RegionalEndpointManager.EndpointHealth> healthStatus = manager.getHealthStatus();
    RegionalEndpointManager.EndpointHealth euHealth =
        healthStatus.get(RegionalEndpointManager.EU_ENDPOINT);

    assertNotNull(euHealth);
    assertFalse(euHealth.isHealthy());
    assertEquals(3, euHealth.getConsecutiveFailures());
  }

  @Test
  @DisplayName("Should failover to next available endpoint when primary fails")
  void shouldFailoverToNextAvailableEndpointWhenPrimaryFails() {
    // Mark EU endpoint as unhealthy
    for (int i = 0; i < 3; i++) {
      manager.reportFailure(RegionalEndpointManager.EU_ENDPOINT, "Connection failed");
    }

    // Primary endpoint should switch to a healthy one
    String newPrimary = manager.getPrimaryEndpoint();
    assertNotEquals(RegionalEndpointManager.EU_ENDPOINT, newPrimary);

    // Should be one of the other regional endpoints
    assertTrue(
        newPrimary.equals(RegionalEndpointManager.US_ENDPOINT)
            || newPrimary.equals(RegionalEndpointManager.SA_ENDPOINT));
  }

  @Test
  @DisplayName("Should reset failure counter on successful operation")
  void shouldResetFailureCounterOnSuccessfulOperation() {
    String endpoint = RegionalEndpointManager.EU_ENDPOINT;

    // Report some failures
    manager.reportFailure(endpoint, "Error 1");
    manager.reportFailure(endpoint, "Error 2");

    Map<String, RegionalEndpointManager.EndpointHealth> healthStatus = manager.getHealthStatus();
    assertEquals(2, healthStatus.get(endpoint).getConsecutiveFailures());

    // Report success
    manager.reportSuccess(endpoint, Duration.ofMillis(100));

    // Failure counter should be reset
    healthStatus = manager.getHealthStatus();
    assertEquals(0, healthStatus.get(endpoint).getConsecutiveFailures());
    assertTrue(healthStatus.get(endpoint).isHealthy());
  }

  @Test
  @DisplayName("Should get region from endpoint URL")
  void shouldGetRegionFromEndpointUrl() {
    assertEquals(
        RegionalEndpointManager.Region.EU,
        RegionalEndpointManager.getRegionFromEndpoint(RegionalEndpointManager.EU_ENDPOINT));
    assertEquals(
        RegionalEndpointManager.Region.US,
        RegionalEndpointManager.getRegionFromEndpoint(RegionalEndpointManager.US_ENDPOINT));
    assertEquals(
        RegionalEndpointManager.Region.SA,
        RegionalEndpointManager.getRegionFromEndpoint(RegionalEndpointManager.SA_ENDPOINT));
    assertNull(RegionalEndpointManager.getRegionFromEndpoint("unknown://endpoint"));
  }

  @Test
  @DisplayName("Should create manager from configuration")
  void shouldCreateManagerFromConfiguration() {
    RegionalEndpointManager configManager =
        RegionalEndpointManager.fromConfiguration(
            RegionalEndpointManager.US_ENDPOINT,
            true,
            Duration.ofSeconds(60),
            Duration.ofSeconds(10));

    assertEquals(RegionalEndpointManager.Region.US, configManager.getPreferredRegion());
    assertTrue(configManager.isDataResidencyRequired());
    assertEquals(Duration.ofSeconds(60), configManager.getHealthCheckInterval());
  }

  @Test
  @DisplayName("Should default to EU region for unknown endpoints")
  void shouldDefaultToEuRegionForUnknownEndpoints() {
    RegionalEndpointManager configManager =
        RegionalEndpointManager.fromConfiguration(
            "grpcs://unknown.endpoint.com:443",
            false,
            Duration.ofSeconds(30),
            Duration.ofSeconds(5));

    assertEquals(RegionalEndpointManager.Region.EU, configManager.getPreferredRegion());
  }

  @Test
  @DisplayName("Should order endpoints by health and latency")
  void shouldOrderEndpointsByHealthAndLatency() {
    // Make US endpoint faster than SA
    manager.reportSuccess(RegionalEndpointManager.US_ENDPOINT, Duration.ofMillis(50));
    manager.reportSuccess(RegionalEndpointManager.SA_ENDPOINT, Duration.ofMillis(200));

    // Mark EU as unhealthy
    for (int i = 0; i < 3; i++) {
      manager.reportFailure(RegionalEndpointManager.EU_ENDPOINT, "Failed");
    }

    List<String> orderedEndpoints = manager.getOrderedEndpoints();

    // US should be first (healthy and faster)
    assertEquals(RegionalEndpointManager.US_ENDPOINT, orderedEndpoints.get(0));
    // SA should be second (healthy but slower)
    assertEquals(RegionalEndpointManager.SA_ENDPOINT, orderedEndpoints.get(1));
    // EU should be last (unhealthy)
    assertEquals(RegionalEndpointManager.EU_ENDPOINT, orderedEndpoints.get(2));
  }

  @Test
  @DisplayName("Should handle health check simulation")
  void shouldHandleHealthCheckSimulation() {
    // This test verifies that performHealthCheck doesn't throw exceptions
    // In real implementation, this would test actual health checks
    assertDoesNotThrow(() -> manager.performHealthCheck());

    // Verify that health status is updated
    Map<String, RegionalEndpointManager.EndpointHealth> healthStatus = manager.getHealthStatus();
    assertFalse(healthStatus.isEmpty());

    // All endpoints should have health information
    assertTrue(healthStatus.containsKey(RegionalEndpointManager.EU_ENDPOINT));
    assertTrue(healthStatus.containsKey(RegionalEndpointManager.US_ENDPOINT));
    assertTrue(healthStatus.containsKey(RegionalEndpointManager.SA_ENDPOINT));
  }
}
