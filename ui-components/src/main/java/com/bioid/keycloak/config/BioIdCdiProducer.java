package com.bioid.keycloak.config;

import com.bioid.keycloak.client.BioIdClient;
import com.bioid.keycloak.client.auth.BioIdJwtTokenProvider;
// import com.bioid.keycloak.client.config.BioIdClientConfig; // Commented out due to Maven reactor build issues
import com.bioid.keycloak.client.config.BioIdConfiguration;
import com.bioid.keycloak.client.connection.BioIdConnectionManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

/**
 * CDI producer for BioID dependencies. This class provides CDI beans for the BioID extension
 * components.
 */
@ApplicationScoped
public class BioIdCdiProducer {

  /** Produces BioIdConfiguration as a CDI bean. */
  @Produces
  @ApplicationScoped
  public BioIdConfiguration produceBioIdConfiguration() {
    return BioIdConfiguration.getInstance();
  }

  /** Produces BioIdClientConfig from BioIdConfiguration using reflection. */
  @Produces
  @ApplicationScoped
  public Object produceBioIdClientConfig(BioIdConfiguration configuration) {
    try {
      // Use reflection to avoid compile-time dependency on BioIdClientConfig
      Class<?> configClass = Class.forName("com.bioid.keycloak.client.config.BioIdClientConfig");
      Object builder = configClass.getMethod("builder").invoke(null);
      
      builder = builder.getClass().getMethod("endpoint", String.class).invoke(builder, configuration.getEndpoint());
      builder = builder.getClass().getMethod("clientId", String.class).invoke(builder, configuration.getClientId());
      builder = builder.getClass().getMethod("secretKey", String.class).invoke(builder, configuration.getKey());
      builder = builder.getClass().getMethod("jwtExpireMinutes", int.class).invoke(builder, configuration.getJwtExpireMinutes());
      builder = builder.getClass().getMethod("channelPoolSize", int.class).invoke(builder, configuration.getChannelPoolSize());
      builder = builder.getClass().getMethod("keepAliveTime", long.class).invoke(builder, configuration.getKeepAliveTime());
      builder = builder.getClass().getMethod("verificationTimeout", long.class).invoke(builder, configuration.getVerificationTimeout());
      builder = builder.getClass().getMethod("enrollmentTimeout", long.class).invoke(builder, configuration.getEnrollmentTimeout());
      builder = builder.getClass().getMethod("requestTimeout", long.class).invoke(builder, configuration.getVerificationTimeout());
      builder = builder.getClass().getMethod("maxRetryAttempts", int.class).invoke(builder, configuration.getRetryMaxAttempts());
      builder = builder.getClass().getMethod("retryBackoffMultiplier", double.class).invoke(builder, configuration.getRetryBackoffMultiplier());
      builder = builder.getClass().getMethod("healthCheckInterval", long.class).invoke(builder, configuration.getHealthCheckInterval());
      
      return builder.getClass().getMethod("build").invoke(builder);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create BioIdClientConfig using reflection", e);
    }
  }

  /** Produces MeterRegistry for metrics collection. */
  @Produces
  @Singleton
  public MeterRegistry produceMeterRegistry() {
    // For now, use a simple meter registry
    // In production, this should integrate with Keycloak's metrics
    return new SimpleMeterRegistry();
  }

  /** Produces BioIdClient as a CDI bean using reflection. */
  @Produces
  @ApplicationScoped
  public BioIdClient produceBioIdClient(
      Object config, MeterRegistry meterRegistry) {
    try {
      // Use reflection to avoid compile-time dependency on BioIdClientConfig
      String clientId = (String) config.getClass().getMethod("clientId").invoke(config);
      String secretKey = (String) config.getClass().getMethod("secretKey").invoke(config);
      Integer jwtExpireMinutes = (Integer) config.getClass().getMethod("jwtExpireMinutes").invoke(config);
      
      // Create required components for BioIdGrpcClient
      BioIdJwtTokenProvider tokenProvider = new BioIdJwtTokenProvider(clientId, secretKey, jwtExpireMinutes);
      
      Class<?> connectionManagerClass = Class.forName("com.bioid.keycloak.client.connection.BioIdConnectionManager");
      BioIdConnectionManager connectionManager = (BioIdConnectionManager) connectionManagerClass
          .getDeclaredConstructor(Object.class, MeterRegistry.class)
          .newInstance(config, meterRegistry);

      // Use reflection for BioIdGrpcClient constructor as well
      Class<?> grpcClientClass = Class.forName("com.bioid.keycloak.client.BioIdGrpcClient");
      return (BioIdClient) grpcClientClass
          .getDeclaredConstructor(Object.class, BioIdJwtTokenProvider.class, BioIdConnectionManager.class, MeterRegistry.class)
          .newInstance(config, tokenProvider, connectionManager, meterRegistry);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create BioIdClient using reflection", e);
    }
  }
}
