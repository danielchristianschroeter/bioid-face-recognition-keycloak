package com.bioid.keycloak.client.config;

import java.time.Duration;

/**
 * Configuration record for BioID gRPC client.
 *
 * <p>This record encapsulates all configuration parameters needed for the BioID client, providing a
 * type-safe and immutable configuration object.
 */
public record BioIdClientConfig(
    String endpoint,
    java.util.List<String> fallbackEndpoints,
    String clientId,
    String secretKey,
    int jwtExpireMinutes,
    int channelPoolSize,
    Duration keepAliveTime,
    Duration keepAliveTimeout,
    boolean keepAliveWithoutCalls,
    Duration verificationTimeout,
    Duration enrollmentTimeout,
    Duration requestTimeout,
    Duration connectTimeout,
    Duration initialRetryDelay,
    Duration maxRetryDelay,
    int maxRetryAttempts,
    double retryBackoffMultiplier,
    Duration healthCheckInterval,
    boolean tlsEnabled,
    boolean mutualTlsEnabled,
    String keyStorePath) {

  /** Creates a builder for BioIdClientConfig. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for BioIdClientConfig. */
  public static class Builder {
    private String endpoint;
    private java.util.List<String> fallbackEndpoints = java.util.Collections.emptyList();
    private String clientId;
    private String secretKey;
    private int jwtExpireMinutes = 60;
    private int channelPoolSize = 5;
    private Duration keepAliveTime = Duration.ofSeconds(30);
    private Duration keepAliveTimeout = Duration.ofSeconds(30);
    private boolean keepAliveWithoutCalls = true;
    private Duration verificationTimeout = Duration.ofSeconds(4);
    private Duration enrollmentTimeout = Duration.ofSeconds(7);
    private Duration requestTimeout = Duration.ofSeconds(4);
    private Duration connectTimeout = Duration.ofSeconds(10);
    private Duration initialRetryDelay = Duration.ofMillis(100);
    private Duration maxRetryDelay = Duration.ofSeconds(5);
    private int maxRetryAttempts = 3;
    private double retryBackoffMultiplier = 2.0;
    private Duration healthCheckInterval = Duration.ofSeconds(30);
    private boolean tlsEnabled = true;
    private boolean mutualTlsEnabled = false;
    private String keyStorePath;

    public Builder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder fallbackEndpoints(java.util.List<String> fallbackEndpoints) {
      this.fallbackEndpoints =
          fallbackEndpoints != null ? fallbackEndpoints : java.util.Collections.emptyList();
      return this;
    }

    public Builder clientId(String clientId) {
      this.clientId = clientId;
      return this;
    }

    public Builder secretKey(String secretKey) {
      this.secretKey = secretKey;
      return this;
    }

    public Builder jwtExpireMinutes(int jwtExpireMinutes) {
      this.jwtExpireMinutes = jwtExpireMinutes;
      return this;
    }

    public Builder channelPoolSize(int channelPoolSize) {
      this.channelPoolSize = channelPoolSize;
      return this;
    }

    public Builder keepAliveTime(Duration keepAliveTime) {
      this.keepAliveTime = keepAliveTime;
      return this;
    }

    public Builder keepAliveTimeout(Duration keepAliveTimeout) {
      this.keepAliveTimeout = keepAliveTimeout;
      return this;
    }

    public Builder keepAliveWithoutCalls(boolean keepAliveWithoutCalls) {
      this.keepAliveWithoutCalls = keepAliveWithoutCalls;
      return this;
    }

    public Builder verificationTimeout(Duration verificationTimeout) {
      this.verificationTimeout = verificationTimeout;
      return this;
    }

    public Builder enrollmentTimeout(Duration enrollmentTimeout) {
      this.enrollmentTimeout = enrollmentTimeout;
      return this;
    }

    public Builder requestTimeout(Duration requestTimeout) {
      this.requestTimeout = requestTimeout;
      return this;
    }

    public Builder maxRetryAttempts(int maxRetryAttempts) {
      this.maxRetryAttempts = maxRetryAttempts;
      return this;
    }

    public Builder retryBackoffMultiplier(double retryBackoffMultiplier) {
      this.retryBackoffMultiplier = retryBackoffMultiplier;
      return this;
    }

    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout = connectTimeout;
      return this;
    }

    public Builder initialRetryDelay(Duration initialRetryDelay) {
      this.initialRetryDelay = initialRetryDelay;
      return this;
    }

    public Builder maxRetryDelay(Duration maxRetryDelay) {
      this.maxRetryDelay = maxRetryDelay;
      return this;
    }

    public Builder healthCheckInterval(Duration healthCheckInterval) {
      this.healthCheckInterval = healthCheckInterval;
      return this;
    }

    public Builder tlsEnabled(boolean tlsEnabled) {
      this.tlsEnabled = tlsEnabled;
      return this;
    }

    public Builder mutualTlsEnabled(boolean mutualTlsEnabled) {
      this.mutualTlsEnabled = mutualTlsEnabled;
      return this;
    }

    public Builder keyStorePath(String keyStorePath) {
      this.keyStorePath = keyStorePath;
      return this;
    }

    public BioIdClientConfig build() {
      // Validate required fields
      if (endpoint == null || endpoint.trim().isEmpty()) {
        throw new IllegalArgumentException("Endpoint cannot be empty");
      }
      if (clientId == null || clientId.trim().isEmpty()) {
        throw new IllegalArgumentException("Client ID cannot be empty");
      }
      if (secretKey == null || secretKey.trim().isEmpty()) {
        throw new IllegalArgumentException("Secret key cannot be empty");
      }

      return new BioIdClientConfig(
          endpoint,
          fallbackEndpoints,
          clientId,
          secretKey,
          jwtExpireMinutes,
          channelPoolSize,
          keepAliveTime,
          keepAliveTimeout,
          keepAliveWithoutCalls,
          verificationTimeout,
          enrollmentTimeout,
          requestTimeout,
          connectTimeout,
          initialRetryDelay,
          maxRetryDelay,
          maxRetryAttempts,
          retryBackoffMultiplier,
          healthCheckInterval,
          tlsEnabled,
          mutualTlsEnabled,
          keyStorePath);
    }
  }

  /** Validates the configuration. */
  public void validate() {
    if (endpoint == null || endpoint.trim().isEmpty()) {
      throw new IllegalArgumentException("BioID endpoint is required");
    }

    if (clientId == null || clientId.trim().isEmpty()) {
      throw new IllegalArgumentException("BioID client ID is required");
    }

    if (secretKey == null || secretKey.trim().isEmpty()) {
      throw new IllegalArgumentException("BioID secret key is required");
    }

    if (jwtExpireMinutes <= 0) {
      throw new IllegalArgumentException("JWT expire minutes must be positive");
    }

    if (channelPoolSize <= 0) {
      throw new IllegalArgumentException("Channel pool size must be positive");
    }

    if (keepAliveTime.isNegative() || keepAliveTime.isZero()) {
      throw new IllegalArgumentException("Keep alive time must be positive");
    }

    if (verificationTimeout.isNegative() || verificationTimeout.isZero()) {
      throw new IllegalArgumentException("Verification timeout must be positive");
    }

    if (enrollmentTimeout.isNegative() || enrollmentTimeout.isZero()) {
      throw new IllegalArgumentException("Enrollment timeout must be positive");
    }

    if (requestTimeout.isNegative() || requestTimeout.isZero()) {
      throw new IllegalArgumentException("Request timeout must be positive");
    }

    if (maxRetryAttempts < 0) {
      throw new IllegalArgumentException("Max retry attempts cannot be negative");
    }

    if (retryBackoffMultiplier <= 1.0) {
      throw new IllegalArgumentException("Retry backoff multiplier must be greater than 1.0");
    }

    if (healthCheckInterval.isNegative() || healthCheckInterval.isZero()) {
      throw new IllegalArgumentException("Health check interval must be positive");
    }
  }
}
