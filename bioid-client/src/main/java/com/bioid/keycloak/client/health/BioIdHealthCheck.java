package com.bioid.keycloak.client.health;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Health check for the BioID service. Provides methods to check if the service is healthy and to
 * update the health status.
 */
public class BioIdHealthCheck {
  private final AtomicBoolean healthy = new AtomicBoolean(true);
  private final AtomicReference<Instant> lastSuccessfulCheck = new AtomicReference<>(Instant.now());
  private final AtomicReference<String> lastErrorMessage = new AtomicReference<>("");
  private final AtomicReference<String> currentEndpoint = new AtomicReference<>("");

  private final Duration healthTimeout;

  /** Creates a new BioIdHealthCheck with the default health timeout of 5 minutes. */
  public BioIdHealthCheck() {
    this(Duration.ofMinutes(5));
  }

  /**
   * Creates a new BioIdHealthCheck with the specified health timeout.
   *
   * @param healthTimeout the duration after which the service is considered unhealthy if no
   *     successful check has occurred
   */
  public BioIdHealthCheck(Duration healthTimeout) {
    this.healthTimeout = healthTimeout;
  }

  /**
   * Checks if the service is healthy. A service is considered healthy if the last successful check
   * was within the health timeout and the healthy flag is set to true.
   *
   * @return true if the service is healthy, false otherwise
   */
  public boolean isHealthy() {
    Instant lastCheck = lastSuccessfulCheck.get();
    Duration sinceLastCheck = Duration.between(lastCheck, Instant.now());

    return healthy.get() && sinceLastCheck.compareTo(healthTimeout) < 0;
  }

  /** Updates the health status to healthy. */
  public void markHealthy() {
    healthy.set(true);
    lastSuccessfulCheck.set(Instant.now());
    lastErrorMessage.set("");
  }

  /**
   * Updates the health status to unhealthy.
   *
   * @param errorMessage the error message describing why the service is unhealthy
   */
  public void markUnhealthy(String errorMessage) {
    healthy.set(false);
    lastErrorMessage.set(errorMessage);
  }

  /**
   * Gets the last error message.
   *
   * @return the last error message, or an empty string if there is no error
   */
  public String getLastErrorMessage() {
    return lastErrorMessage.get();
  }

  /**
   * Gets the time of the last successful health check.
   *
   * @return the time of the last successful health check
   */
  public Instant getLastSuccessfulCheck() {
    return lastSuccessfulCheck.get();
  }

  /**
   * Sets the current endpoint being used.
   *
   * @param endpoint the current endpoint
   */
  public void setCurrentEndpoint(String endpoint) {
    currentEndpoint.set(endpoint);
  }

  /**
   * Gets the current endpoint being used.
   *
   * @return the current endpoint
   */
  public String getCurrentEndpoint() {
    return currentEndpoint.get();
  }
}
