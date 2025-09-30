package com.bioid.keycloak.client.config;

import java.time.Duration;

/**
 * Adapter that creates BioIdClientConfig from the new BioIdConfiguration system.
 *
 * <p>This adapter bridges the gap between the existing BioIdClientConfig record and the new
 * externalized configuration system, allowing for seamless integration without breaking existing
 * code.
 */
public class BioIdConfigurationAdapter {

  private final BioIdConfiguration configuration;

  public BioIdConfigurationAdapter() {
    this.configuration = BioIdConfiguration.getInstance();
  }

  public BioIdConfigurationAdapter(BioIdConfiguration configuration) {
    this.configuration = configuration;
  }

  /** Creates a BioIdClientConfig from the current configuration. */
  public BioIdClientConfig createClientConfig() {
    return BioIdClientConfig.builder()
        .endpoint(configuration.getEndpoint())
        .fallbackEndpoints(java.util.Collections.emptyList()) // No fallback endpoints by default
        .clientId(configuration.getClientId())
        .secretKey(configuration.getKey())
        .jwtExpireMinutes(configuration.getJwtExpireMinutes())
        .channelPoolSize(configuration.getChannelPoolSize())
        .keepAliveTime(configuration.getKeepAliveTime())
        .keepAliveTimeout(configuration.getKeepAliveTime()) // Use same as keepAliveTime
        .keepAliveWithoutCalls(true) // Default to true
        .verificationTimeout(configuration.getVerificationTimeout())
        .enrollmentTimeout(configuration.getEnrollmentTimeout())
        .requestTimeout(
            configuration.getVerificationTimeout()) // Use verification timeout as default
        .connectTimeout(java.time.Duration.ofSeconds(10)) // Default connect timeout
        .initialRetryDelay(java.time.Duration.ofMillis(100)) // Default initial retry delay
        .maxRetryDelay(java.time.Duration.ofSeconds(5)) // Default max retry delay
        .maxRetryAttempts(configuration.getRetryMaxAttempts())
        .retryBackoffMultiplier(configuration.getRetryBackoffMultiplier())
        .healthCheckInterval(configuration.getHealthCheckInterval())
        .tlsEnabled(true) // Always use TLS for BioID BWS service
        .mutualTlsEnabled(false) // Default to false
        .keyStorePath(null) // No keystore by default
        .build();
  }

  /** Gets the underlying configuration instance for direct access. */
  public BioIdConfiguration getConfiguration() {
    return configuration;
  }

  /** Reloads the configuration from all sources. */
  public void reload() {
    configuration.reload();
  }

  /** Checks if configuration has been modified and reloads if necessary. */
  public void checkAndReload() {
    configuration.checkAndReload();
  }

  // Convenience methods for direct access to configuration values

  public String getEndpoint() {
    return configuration.getEndpoint();
  }

  public String getClientId() {
    return configuration.getClientId();
  }

  public String getSecretKey() {
    return configuration.getKey();
  }

  public int getJwtExpireMinutes() {
    return configuration.getJwtExpireMinutes();
  }

  public Duration getVerificationTimeout() {
    return configuration.getVerificationTimeout();
  }

  public Duration getEnrollmentTimeout() {
    return configuration.getEnrollmentTimeout();
  }

  public int getMaxRetryAttempts() {
    return configuration.getRetryMaxAttempts();
  }

  public double getRetryBackoffMultiplier() {
    return configuration.getRetryBackoffMultiplier();
  }
}
