package com.bioid.keycloak.client.endpoint;

import com.bioid.keycloak.client.config.BioIdClientConfig;
import com.bioid.keycloak.client.exception.BioIdException;

import com.bioid.keycloak.client.health.ServiceHealthStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages regional endpoints for BioID services with automatic failover,
 * latency-based selection, and health monitoring.
 *
 * <p>This class provides:
 * - Regional endpoint discovery and management
 * - Latency-based endpoint selection for optimal performance
 * - Automatic failover to healthy regions
 * - Health monitoring and endpoint status tracking
 * - Load balancing across multiple regions
 */
public class RegionalEndpointManager implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(RegionalEndpointManager.class);

  private final BioIdClientConfig config;
  private final Map<String, RegionInfo> regions;
  private final ScheduledExecutorService healthCheckExecutor;
  private volatile String currentRegion;
  private volatile boolean closed = false;

  // Default regional endpoints - in production these would be configured
  private static final Map<String, String> DEFAULT_REGIONS = Map.of(
      "us-east-1", "bioid-us-east-1.example.com:443",
      "us-west-2", "bioid-us-west-2.example.com:443",
      "eu-west-1", "bioid-eu-west-1.example.com:443",
      "eu-central-1", "bioid-eu-central-1.example.com:443",
      "ap-southeast-1", "bioid-ap-southeast-1.example.com:443"
  );

  public RegionalEndpointManager(BioIdClientConfig config) {
    this.config = config;
    this.regions = new ConcurrentHashMap<>();
    this.healthCheckExecutor = Executors.newScheduledThreadPool(3);
    
    // Initialize regions
    initializeRegions();
    
    // Determine initial region based on configuration or auto-selection
    this.currentRegion = determineInitialRegion();
  }
  
  /**
   * Initialize the endpoint manager and start background tasks.
   * This should be called after construction to avoid 'this' escape.
   */
  public void initialize() {
    // Start health monitoring
    startHealthMonitoring();
    
    logger.info("Regional endpoint manager initialized with {} regions, current: {}", 
        regions.size(), currentRegion);
  }

  /**
   * Gets the list of available regions.
   *
   * @return list of region identifiers
   */
  public List<String> getAvailableRegions() {
    return new ArrayList<>(regions.keySet());
  }

  /**
   * Gets the currently active region.
   *
   * @return current region identifier
   */
  public String getCurrentRegion() {
    return currentRegion;
  }

  /**
   * Gets the endpoint URL for the current region.
   *
   * @return current endpoint URL
   */
  public String getCurrentEndpoint() {
    RegionInfo region = regions.get(currentRegion);
    return region != null ? region.getEndpoint() : config.endpoint();
  }

  /**
   * Switches to a specific region.
   *
   * @param regionId the region to switch to
   * @throws BioIdException if the region is not available or unhealthy
   */
  public void switchToRegion(String regionId) throws BioIdException {
    if (!regions.containsKey(regionId)) {
      throw new BioIdException("Region not available: " + regionId);
    }

    RegionInfo region = regions.get(regionId);
    if (region.getHealthStatus().getState() == ServiceHealthStatus.HealthState.UNHEALTHY) {
      throw new BioIdException("Region is unhealthy: " + regionId);
    }

    String previousRegion = this.currentRegion;
    this.currentRegion = regionId;
    
    logger.info("Switched from region {} to {}", previousRegion, regionId);
  }

  /**
   * Automatically selects the optimal region based on latency and health.
   *
   * @return the selected region identifier
   * @throws BioIdException if no suitable region is available
   */
  public String selectOptimalRegion() throws BioIdException {
    List<RegionInfo> healthyRegions = regions.values().stream()
        .filter(region -> region.getHealthStatus().getState() != ServiceHealthStatus.HealthState.UNHEALTHY)
        .collect(Collectors.toList());

    if (healthyRegions.isEmpty()) {
      throw new BioIdException("No healthy regions available");
    }

    // Sort by latency (ascending) and prefer healthy over degraded
    RegionInfo optimal = healthyRegions.stream()
        .min((r1, r2) -> {
          // First compare by health state (healthy < degraded)
          int healthComparison = r1.getHealthStatus().getState().compareTo(r2.getHealthStatus().getState());
          if (healthComparison != 0) {
            return healthComparison;
          }
          // Then compare by latency
          return Long.compare(r1.getLatencyMs(), r2.getLatencyMs());
        })
        .orElseThrow(() -> new BioIdException("Unable to select optimal region"));

    String selectedRegion = optimal.getRegionId();
    if (!selectedRegion.equals(currentRegion)) {
      switchToRegion(selectedRegion);
    }

    return selectedRegion;
  }

  /**
   * Asynchronously selects the optimal region.
   *
   * @return CompletableFuture containing the selected region identifier
   */
  public CompletableFuture<String> selectOptimalRegionAsync() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        return selectOptimalRegion();
      } catch (BioIdException e) {
        throw new RuntimeException(e);
      }
    }, healthCheckExecutor);
  }

  /**
   * Performs latency tests to all regions and updates their metrics.
   *
   * @return CompletableFuture that completes when all tests are done
   */
  public CompletableFuture<Void> performLatencyTests() {
    List<CompletableFuture<Void>> futures = regions.values().stream()
        .map(this::testRegionLatency)
        .collect(Collectors.toList());

    return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
  }

  /**
   * Gets health status for a specific region.
   *
   * @param regionId the region identifier
   * @return health status, or null if region doesn't exist
   */
  public ServiceHealthStatus getRegionHealth(String regionId) {
    RegionInfo region = regions.get(regionId);
    return region != null ? region.getHealthStatus() : null;
  }

  /**
   * Gets latency information for a specific region.
   *
   * @param regionId the region identifier
   * @return latency in milliseconds, or -1 if region doesn't exist
   */
  public long getRegionLatency(String regionId) {
    RegionInfo region = regions.get(regionId);
    return region != null ? region.getLatencyMs() : -1;
  }

  private void initializeRegions() {
    // In production, this would load from configuration or service discovery
    DEFAULT_REGIONS.forEach((regionId, endpoint) -> {
      RegionInfo regionInfo = new RegionInfo(regionId, endpoint);
      regions.put(regionId, regionInfo);
    });

    // Add the configured endpoint as the default region if not already present
    if (!regions.values().stream().anyMatch(r -> r.getEndpoint().equals(config.endpoint()))) {
      regions.put("default", new RegionInfo("default", config.endpoint()));
    }
  }

  private String determineInitialRegion() {
    // Try to find a region matching the configured endpoint
    return regions.entrySet().stream()
        .filter(entry -> entry.getValue().getEndpoint().equals(config.endpoint()))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse("default");
  }

  private void startHealthMonitoring() {
    // Perform initial health checks
    performLatencyTests();

    // Schedule periodic health checks
    healthCheckExecutor.scheduleWithFixedDelay(() -> {
      try {
        performLatencyTests().join();
      } catch (Exception e) {
        logger.warn("Health monitoring failed", e);
      }
    }, 60, 60, TimeUnit.SECONDS);
  }

  private CompletableFuture<Void> testRegionLatency(RegionInfo region) {
    return CompletableFuture.runAsync(() -> {
      try {
        Instant start = Instant.now();
        
        // Perform a simple connectivity test (in production, this would be a lightweight gRPC call)
        boolean isReachable = performConnectivityTest(region.getEndpoint());
        
        Instant end = Instant.now();
        long latencyMs = Duration.between(start, end).toMillis();
        
        // Update region metrics
        region.updateLatency(latencyMs);
        
        ServiceHealthStatus.HealthState healthState;
        String statusMessage;
        
        if (!isReachable) {
          healthState = ServiceHealthStatus.HealthState.UNHEALTHY;
          statusMessage = "Region is not reachable";
          latencyMs = Long.MAX_VALUE; // Set high latency for unreachable regions
        } else if (latencyMs > 2000) {
          healthState = ServiceHealthStatus.HealthState.DEGRADED;
          statusMessage = "High latency detected";
        } else {
          healthState = ServiceHealthStatus.HealthState.HEALTHY;
          statusMessage = "Region is healthy";
        }
        
        ServiceHealthStatus healthStatus = new ServiceHealthStatus.Builder()
            .setState(healthState)
            .setEndpoint(region.getEndpoint())
            .setRegion(region.getRegionId())
            .setResponseTimeMs(latencyMs)
            .setLastHealthCheck(Instant.now())
            .setStatusMessage(statusMessage)
            .build();
        
        region.updateHealthStatus(healthStatus);
        
        logger.debug("Health check completed for region {}: {} ({}ms)", 
            region.getRegionId(), healthState, latencyMs);
            
      } catch (Exception e) {
        logger.warn("Health check failed for region {}: {}", region.getRegionId(), e.getMessage());
        
        ServiceHealthStatus unhealthyStatus = new ServiceHealthStatus.Builder()
            .setState(ServiceHealthStatus.HealthState.UNHEALTHY)
            .setEndpoint(region.getEndpoint())
            .setRegion(region.getRegionId())
            .setResponseTimeMs(Long.MAX_VALUE)
            .setLastHealthCheck(Instant.now())
            .setStatusMessage("Health check failed: " + e.getMessage())
            .build();
        
        region.updateHealthStatus(unhealthyStatus);
      }
    }, healthCheckExecutor);
  }

  private boolean performConnectivityTest(String endpoint) {
    // In production, this would perform an actual connectivity test
    // For now, we'll simulate based on endpoint format
    try {
      // Simulate network delay
      Thread.sleep(50 + (long) (Math.random() * 200));
      
      // Simulate occasional failures
      return Math.random() > 0.05; // 95% success rate
      
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    
    closed = true;
    logger.info("Shutting down regional endpoint manager");
    
    healthCheckExecutor.shutdown();
    try {
      if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        healthCheckExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      healthCheckExecutor.shutdownNow();
    }
    
    logger.info("Regional endpoint manager shutdown complete");
  }

  /**
   * Internal class to track region information and metrics.
   */
  private static class RegionInfo {
    private final String regionId;
    private final String endpoint;
    private volatile long latencyMs = Long.MAX_VALUE;
    private volatile ServiceHealthStatus healthStatus;
    private volatile Instant lastUpdated = Instant.now();

    public RegionInfo(String regionId, String endpoint) {
      this.regionId = regionId;
      this.endpoint = endpoint;
      this.healthStatus = new ServiceHealthStatus.Builder()
          .setState(ServiceHealthStatus.HealthState.UNKNOWN)
          .setEndpoint(endpoint)
          .setRegion(regionId)
          .setResponseTimeMs(Long.MAX_VALUE)
          .setLastHealthCheck(Instant.now())
          .setStatusMessage("Initial state")
          .build();
    }

    public String getRegionId() { return regionId; }
    public String getEndpoint() { return endpoint; }
    public long getLatencyMs() { return latencyMs; }
    public ServiceHealthStatus getHealthStatus() { return healthStatus; }
    public Instant getLastUpdated() { return lastUpdated; }

    public void updateLatency(long latencyMs) {
      this.latencyMs = latencyMs;
      this.lastUpdated = Instant.now();
    }

    public void updateHealthStatus(ServiceHealthStatus healthStatus) {
      this.healthStatus = healthStatus;
      this.lastUpdated = Instant.now();
    }
  }
}