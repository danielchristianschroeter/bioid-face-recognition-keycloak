package com.bioid.keycloak.client.endpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages regional BioID endpoints with health monitoring and automatic failover.
 *
 * <p>Features: - Regional endpoint selection (EU, US, SA) - Health monitoring with automatic
 * failover - Latency-based endpoint selection - Data residency compliance - Connection optimization
 */
public class RegionalEndpointManager {

  private static final Logger logger = LoggerFactory.getLogger(RegionalEndpointManager.class);

  // Regional endpoints
  public static final String EU_ENDPOINT = "grpcs://grpc.bws-eu.bioid.com:443";
  public static final String US_ENDPOINT = "grpcs://grpc.bws-us.bioid.com:443";
  public static final String SA_ENDPOINT = "grpcs://grpc.bws-sa.bioid.com:443";

  // Endpoint regions
  public enum Region {
    EU("Europe", EU_ENDPOINT),
    US("United States", US_ENDPOINT),
    SA("South America", SA_ENDPOINT);

    private final String displayName;
    private final String endpoint;

    Region(String displayName, String endpoint) {
      this.displayName = displayName;
      this.endpoint = endpoint;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getEndpoint() {
      return endpoint;
    }
  }

  // Endpoint health status
  public static class EndpointHealth {
    private final String endpoint;
    private final boolean healthy;
    private final Duration latency;
    private final Instant lastCheck;
    private final String errorMessage;
    private final int consecutiveFailures;

    public EndpointHealth(
        String endpoint,
        boolean healthy,
        Duration latency,
        Instant lastCheck,
        String errorMessage,
        int consecutiveFailures) {
      this.endpoint = endpoint;
      this.healthy = healthy;
      this.latency = latency;
      this.lastCheck = lastCheck;
      this.errorMessage = errorMessage;
      this.consecutiveFailures = consecutiveFailures;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public boolean isHealthy() {
      return healthy;
    }

    public Duration getLatency() {
      return latency;
    }

    public Instant getLastCheck() {
      return lastCheck;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public int getConsecutiveFailures() {
      return consecutiveFailures;
    }

    @Override
    public String toString() {
      return String.format(
          "EndpointHealth{endpoint='%s', healthy=%s, latency=%s, consecutiveFailures=%d}",
          endpoint, healthy, latency, consecutiveFailures);
    }
  }

  private final Map<String, EndpointHealth> endpointHealth = new ConcurrentHashMap<>();
  private final Map<String, AtomicInteger> failureCounters = new ConcurrentHashMap<>();
  private final AtomicReference<String> primaryEndpoint = new AtomicReference<>();
  private final List<String> availableEndpoints;
  private final Duration healthCheckInterval;

  @SuppressWarnings("unused") // Reserved for future health check implementation
  private final Duration healthCheckTimeout;

  private final int maxConsecutiveFailures;
  private final Region preferredRegion;
  private final boolean dataResidencyRequired;

  // Health check configuration
  private static final Duration DEFAULT_HEALTH_CHECK_INTERVAL = Duration.ofSeconds(30);
  private static final Duration DEFAULT_HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);
  private static final int DEFAULT_MAX_CONSECUTIVE_FAILURES = 3;

  public RegionalEndpointManager(Region preferredRegion, boolean dataResidencyRequired) {
    this(
        preferredRegion,
        dataResidencyRequired,
        DEFAULT_HEALTH_CHECK_INTERVAL,
        DEFAULT_HEALTH_CHECK_TIMEOUT,
        DEFAULT_MAX_CONSECUTIVE_FAILURES);
  }

  public RegionalEndpointManager(
      Region preferredRegion,
      boolean dataResidencyRequired,
      Duration healthCheckInterval,
      Duration healthCheckTimeout,
      int maxConsecutiveFailures) {
    this.preferredRegion = preferredRegion;
    this.dataResidencyRequired = dataResidencyRequired;
    this.healthCheckInterval = healthCheckInterval;
    this.healthCheckTimeout = healthCheckTimeout;
    this.maxConsecutiveFailures = maxConsecutiveFailures;

    // Initialize available endpoints based on data residency requirements
    if (dataResidencyRequired) {
      // Only use preferred region for data residency compliance
      this.availableEndpoints = Collections.singletonList(preferredRegion.getEndpoint());
      logger.info(
          "Data residency required, using only {} region: {}",
          preferredRegion.getDisplayName(),
          preferredRegion.getEndpoint());
    } else {
      // Use all regions with preferred region first
      this.availableEndpoints = new ArrayList<>();
      this.availableEndpoints.add(preferredRegion.getEndpoint());
      for (Region region : Region.values()) {
        if (region != preferredRegion) {
          this.availableEndpoints.add(region.getEndpoint());
        }
      }
      logger.info(
          "Using all regions with {} as preferred: {}",
          preferredRegion.getDisplayName(),
          availableEndpoints);
    }

    // Initialize health status and failure counters
    for (String endpoint : availableEndpoints) {
      endpointHealth.put(
          endpoint, new EndpointHealth(endpoint, true, Duration.ZERO, Instant.now(), null, 0));
      failureCounters.put(endpoint, new AtomicInteger(0));
    }

    // Set initial primary endpoint
    primaryEndpoint.set(preferredRegion.getEndpoint());

    logger.info(
        "Regional endpoint manager initialized with preferred region: {}, "
            + "data residency required: {}, available endpoints: {}",
        preferredRegion.getDisplayName(),
        dataResidencyRequired,
        availableEndpoints.size());
  }

  /** Gets the current primary endpoint for new connections. */
  public String getPrimaryEndpoint() {
    String current = primaryEndpoint.get();

    // Check if current primary is still healthy
    EndpointHealth health = endpointHealth.get(current);
    if (health != null && health.isHealthy()) {
      return current;
    }

    // Find the best available endpoint
    String bestEndpoint = selectBestEndpoint();
    if (bestEndpoint != null && !bestEndpoint.equals(current)) {
      logger.info("Switching primary endpoint from {} to {}", current, bestEndpoint);
      primaryEndpoint.set(bestEndpoint);
      return bestEndpoint;
    }

    // Return current even if unhealthy (last resort)
    return current;
  }

  /** Gets all available endpoints ordered by preference and health. */
  public List<String> getOrderedEndpoints() {
    List<String> ordered = new ArrayList<>();

    // Add primary endpoint first if healthy
    String primary = getPrimaryEndpoint();
    EndpointHealth primaryHealth = endpointHealth.get(primary);
    if (primaryHealth != null && primaryHealth.isHealthy()) {
      ordered.add(primary);
    }

    // Add other healthy endpoints sorted by latency
    availableEndpoints.stream()
        .filter(endpoint -> !endpoint.equals(primary))
        .filter(
            endpoint -> {
              EndpointHealth health = endpointHealth.get(endpoint);
              return health != null && health.isHealthy();
            })
        .sorted(
            (e1, e2) -> {
              EndpointHealth h1 = endpointHealth.get(e1);
              EndpointHealth h2 = endpointHealth.get(e2);
              return h1.getLatency().compareTo(h2.getLatency());
            })
        .forEach(ordered::add);

    // Add unhealthy endpoints as last resort
    availableEndpoints.stream()
        .filter(
            endpoint -> {
              EndpointHealth health = endpointHealth.get(endpoint);
              return health == null || !health.isHealthy();
            })
        .forEach(
            endpoint -> {
              if (!ordered.contains(endpoint)) {
                ordered.add(endpoint);
              }
            });

    return ordered;
  }

  /** Reports a successful operation for an endpoint. */
  public void reportSuccess(String endpoint, Duration latency) {
    if (!availableEndpoints.contains(endpoint)) {
      return;
    }

    // Reset failure counter
    AtomicInteger counter = failureCounters.get(endpoint);
    if (counter != null) {
      int previousFailures = counter.getAndSet(0);
      if (previousFailures > 0) {
        logger.info(
            "Endpoint {} recovered after {} consecutive failures", endpoint, previousFailures);
      }
    }

    // Update health status
    endpointHealth.put(
        endpoint, new EndpointHealth(endpoint, true, latency, Instant.now(), null, 0));

    logger.debug(
        "Reported success for endpoint {} with latency {}ms", endpoint, latency.toMillis());
  }

  /** Reports a failure for an endpoint. */
  public void reportFailure(String endpoint, String errorMessage) {
    if (!availableEndpoints.contains(endpoint)) {
      return;
    }

    // Increment failure counter
    AtomicInteger counter = failureCounters.get(endpoint);
    int failures = counter != null ? counter.incrementAndGet() : 1;

    // Update health status
    boolean healthy = failures < maxConsecutiveFailures;
    endpointHealth.put(
        endpoint,
        new EndpointHealth(
            endpoint, healthy, Duration.ZERO, Instant.now(), errorMessage, failures));

    if (!healthy) {
      logger.warn(
          "Endpoint {} marked as unhealthy after {} consecutive failures. Last error: {}",
          endpoint,
          failures,
          errorMessage);

      // Trigger failover if this was the primary endpoint
      if (endpoint.equals(primaryEndpoint.get())) {
        String newPrimary = selectBestEndpoint();
        if (newPrimary != null && !newPrimary.equals(endpoint)) {
          logger.info("Failing over from {} to {}", endpoint, newPrimary);
          primaryEndpoint.set(newPrimary);
        }
      }
    } else {
      logger.debug(
          "Reported failure for endpoint {} ({}/{}): {}",
          endpoint,
          failures,
          maxConsecutiveFailures,
          errorMessage);
    }
  }

  /** Performs health check on all endpoints. */
  public void performHealthCheck() {
    logger.debug("Performing health check on {} endpoints", availableEndpoints.size());

    for (String endpoint : availableEndpoints) {
      try {
        // Simulate health check (in real implementation, this would ping the endpoint)
        Duration latency = simulateHealthCheck(endpoint);
        reportSuccess(endpoint, latency);
      } catch (Exception e) {
        reportFailure(endpoint, e.getMessage());
      }
    }
  }

  /** Gets current health status for all endpoints. */
  public Map<String, EndpointHealth> getHealthStatus() {
    return new HashMap<>(endpointHealth);
  }

  /** Gets the preferred region. */
  public Region getPreferredRegion() {
    return preferredRegion;
  }

  /** Checks if data residency is required. */
  public boolean isDataResidencyRequired() {
    return dataResidencyRequired;
  }

  /** Gets health check interval. */
  public Duration getHealthCheckInterval() {
    return healthCheckInterval;
  }

  /** Selects the best available endpoint based on health and latency. */
  private String selectBestEndpoint() {
    // First, try to find a healthy endpoint
    Optional<String> healthyEndpoint =
        availableEndpoints.stream()
            .filter(
                endpoint -> {
                  EndpointHealth health = endpointHealth.get(endpoint);
                  return health != null && health.isHealthy();
                })
            .min(
                (e1, e2) -> {
                  EndpointHealth h1 = endpointHealth.get(e1);
                  EndpointHealth h2 = endpointHealth.get(e2);

                  // Prefer the preferred region if both are healthy
                  if (e1.equals(preferredRegion.getEndpoint())) return -1;
                  if (e2.equals(preferredRegion.getEndpoint())) return 1;

                  // Otherwise, prefer lower latency
                  return h1.getLatency().compareTo(h2.getLatency());
                });

    if (healthyEndpoint.isPresent()) {
      return healthyEndpoint.get();
    }

    // If no healthy endpoints, return the one with fewest failures
    return availableEndpoints.stream()
        .min(
            (e1, e2) -> {
              EndpointHealth h1 = endpointHealth.get(e1);
              EndpointHealth h2 = endpointHealth.get(e2);
              return Integer.compare(h1.getConsecutiveFailures(), h2.getConsecutiveFailures());
            })
        .orElse(preferredRegion.getEndpoint());
  }

  /** Simulates a health check (placeholder for actual implementation). */
  private Duration simulateHealthCheck(String endpoint) throws Exception {
    // In real implementation, this would:
    // 1. Create a gRPC channel to the endpoint
    // 2. Send a health check request
    // 3. Measure the response time
    // 4. Return the latency or throw an exception

    // For now, simulate random latency and occasional failures
    Random random = new Random();
    if (random.nextDouble() < 0.1) { // 10% chance of failure
      throw new Exception("Simulated health check failure");
    }

    // Simulate latency between 50-200ms
    long latencyMs = 50 + random.nextInt(150);
    return Duration.ofMillis(latencyMs);
  }

  /** Gets region from endpoint URL. */
  public static Region getRegionFromEndpoint(String endpoint) {
    for (Region region : Region.values()) {
      if (region.getEndpoint().equals(endpoint)) {
        return region;
      }
    }
    return null;
  }

  /** Creates a regional endpoint manager from configuration. */
  public static RegionalEndpointManager fromConfiguration(
      String primaryEndpoint,
      boolean dataResidencyRequired,
      Duration healthCheckInterval,
      Duration healthCheckTimeout) {
    Region preferredRegion = getRegionFromEndpoint(primaryEndpoint);
    if (preferredRegion == null) {
      // Default to EU if endpoint doesn't match known regions
      logger.warn("Unknown endpoint {}, defaulting to EU region", primaryEndpoint);
      preferredRegion = Region.EU;
    }

    return new RegionalEndpointManager(
        preferredRegion,
        dataResidencyRequired,
        healthCheckInterval,
        healthCheckTimeout,
        DEFAULT_MAX_CONSECUTIVE_FAILURES);
  }
}
