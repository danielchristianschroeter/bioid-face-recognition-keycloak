package com.bioid.keycloak.client.endpoint;

import static org.junit.jupiter.api.Assertions.*;

import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.health.ServiceHealthStatus;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for RegionalEndpointManager. */
class RegionalEndpointManagerTest {

  private RegionalEndpointManager manager;
  private BioIdClientConfig config;
  
  private static final String EU_ENDPOINT = "bioid-eu-west-1.example.com:443";
  private static final String US_ENDPOINT = "bioid-us-east-1.example.com:443";

  @BeforeEach
  void setUp() {
    config = BioIdClientConfig.builder()
        .endpoint(EU_ENDPOINT)
        .clientId("test-client")
        .secretKey("test-key")
        .build();
        
    manager = new RegionalEndpointManager(config);
    manager.initialize();
  }
  
  @AfterEach
  void tearDown() {
    if (manager != null) {
      manager.close();
    }
  }

  @Test
  @DisplayName("Should initialize with configured endpoint")
  void shouldInitializeWithConfiguredEndpoint() {
    assertEquals(EU_ENDPOINT, manager.getCurrentEndpoint());
    // The region is determined based on the endpoint, so it should be eu-west-1 for the EU endpoint
    assertEquals("eu-west-1", manager.getCurrentRegion());
  }

  @Test
  @DisplayName("Should return available regions")
  void shouldReturnAvailableRegions() {
    List<String> regions = manager.getAvailableRegions();

    assertNotNull(regions);
    assertFalse(regions.isEmpty());
    assertTrue(regions.size() >= 5); // Should have at least the default regions
  }

  @Test
  @DisplayName("Should switch to different region")
  void shouldSwitchToDifferentRegion() throws Exception {
    List<String> regions = manager.getAvailableRegions();
    
    // Find a region that's not the current one
    String targetRegion = regions.stream()
        .filter(region -> !region.equals(manager.getCurrentRegion()))
        .findFirst()
        .orElse(null);
    
    if (targetRegion != null) {
      String originalRegion = manager.getCurrentRegion();
      
      // Switch to the target region
      assertDoesNotThrow(() -> manager.switchToRegion(targetRegion));
      
      // Verify the switch
      assertEquals(targetRegion, manager.getCurrentRegion());
      assertNotEquals(originalRegion, manager.getCurrentRegion());
    }
  }

  @Test
  @DisplayName("Should select optimal region")
  void shouldSelectOptimalRegion() throws Exception {
    String optimalRegion = manager.selectOptimalRegion();
    
    assertNotNull(optimalRegion);
    assertTrue(manager.getAvailableRegions().contains(optimalRegion));
  }

  @Test
  @DisplayName("Should perform latency tests")
  void shouldPerformLatencyTests() {
    // This should not throw an exception
    assertDoesNotThrow(() -> {
      var future = manager.performLatencyTests();
      future.join(); // Wait for completion
    });
  }

  @Test
  @DisplayName("Should get region health status")
  void shouldGetRegionHealthStatus() {
    List<String> regions = manager.getAvailableRegions();
    
    for (String region : regions) {
      ServiceHealthStatus health = manager.getRegionHealth(region);
      // Health might be null initially, but should not throw exception
      if (health != null) {
        assertNotNull(health.getState());
        assertNotNull(health.getLastHealthCheck());
      }
    }
  }

  @Test
  @DisplayName("Should get region latency")
  void shouldGetRegionLatency() {
    List<String> regions = manager.getAvailableRegions();
    
    for (String region : regions) {
      long latency = manager.getRegionLatency(region);
      // Latency might be -1 for unknown regions or Long.MAX_VALUE for initial state
      assertTrue(latency >= -1);
    }
  }

  @Test
  @DisplayName("Should handle async optimal region selection")
  void shouldHandleAsyncOptimalRegionSelection() {
    assertDoesNotThrow(() -> {
      var future = manager.selectOptimalRegionAsync();
      String region = future.join();
      assertNotNull(region);
      assertTrue(manager.getAvailableRegions().contains(region));
    });
  }

  @Test
  @DisplayName("Should handle invalid region switch")
  void shouldHandleInvalidRegionSwitch() {
    assertThrows(Exception.class, () -> {
      manager.switchToRegion("invalid-region-that-does-not-exist");
    });
  }

  @Test
  @DisplayName("Should close gracefully")
  void shouldCloseGracefully() {
    // Close should not throw an exception
    assertDoesNotThrow(() -> manager.close());
  }
}