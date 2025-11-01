package com.bioid.keycloak.client.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration manager for BioID Face Recognition extension.
 *
 * <p>
 * Loads configuration from multiple sources in order of precedence: 1. System properties (highest
 * precedence) 2. Environment variables 3. ${kc.home}/conf/bioid.properties file 4. Default values
 * (lowest precedence)
 *
 * <p>
 * Features: - Runtime configuration updates without server restart - Environment variable support
 * with standard naming - Configuration validation with detailed error messages - Secure handling of
 * sensitive configuration values - Support for different deployment environments
 */
public class BioIdConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(BioIdConfiguration.class);

  // Configuration file locations
  private static final String DEFAULT_CONFIG_FILE = "bioid.properties";
  private static final String KEYCLOAK_HOME_CONFIG = "conf/bioid.properties";

  // Configuration property keys
  public static final String BIOID_ENDPOINT = "bioid.endpoint";
  public static final String BIOID_CLIENT_ID = "bioid.clientId";
  public static final String BIOID_KEY = "bioid.key";
  public static final String BIOID_JWT_EXPIRE_MINUTES = "bioid.jwtExpireMinutes";

  // BWS Management API configuration
  public static final String BWS_MANAGEMENT_URL = "bws.management.url";
  public static final String BWS_MANAGEMENT_JWT_TOKEN = "bws.management.jwtToken";
  public static final String BWS_MANAGEMENT_EMAIL = "bws.management.email";
  public static final String BWS_MANAGEMENT_API_KEY = "bws.management.apiKey";
  public static final String BWS_MANAGEMENT_JWT_EXPIRE_MINUTES = "bws.management.jwtExpireMinutes";

  public static final String VERIFICATION_THRESHOLD = "verification.threshold";
  public static final String VERIFICATION_MAX_RETRIES = "verification.maxRetries";
  public static final String VERIFICATION_TIMEOUT_SECONDS = "verification.timeoutSeconds";
  public static final String ENROLLMENT_TIMEOUT_SECONDS = "enrollment.timeoutSeconds";

  public static final String TEMPLATE_TTL_DAYS = "template.ttl.days";
  public static final String TEMPLATE_CLEANUP_INTERVAL_HOURS = "template.cleanupInterval.hours";
  public static final String TEMPLATE_TYPE = "template.type";
  public static final String TEMPLATE_ENCRYPTION_ENABLED = "template.encryption.enabled";

  public static final String GRPC_CHANNEL_POOL_SIZE = "grpc.channelPool.size";
  public static final String GRPC_KEEP_ALIVE_TIME_SECONDS = "grpc.keepAlive.timeSeconds";
  public static final String GRPC_RETRY_MAX_ATTEMPTS = "grpc.retry.maxAttempts";
  public static final String GRPC_RETRY_BACKOFF_MULTIPLIER = "grpc.retry.backoffMultiplier";

  public static final String HEALTH_CHECK_INTERVAL_SECONDS = "healthCheck.interval.seconds";
  public static final String HEALTH_CHECK_TIMEOUT_SECONDS = "healthCheck.timeout.seconds";

  public static final String REGIONAL_PREFERRED_REGION = "regional.preferredRegion";
  public static final String REGIONAL_DATA_RESIDENCY_REQUIRED = "regional.dataResidencyRequired";
  public static final String REGIONAL_FAILOVER_ENABLED = "regional.failoverEnabled";
  public static final String REGIONAL_LATENCY_THRESHOLD_MS = "regional.latencyThresholdMs";

  public static final String LIVENESS_ACTIVE_ENABLED = "liveness.active.enabled";
  public static final String LIVENESS_CHALLENGE_RESPONSE_ENABLED =
      "liveness.challengeResponse.enabled";
  public static final String LIVENESS_CONFIDENCE_THRESHOLD = "liveness.confidenceThreshold";
  public static final String LIVENESS_MAX_OVERHEAD_MS = "liveness.maxOverheadMs";
  public static final String LIVENESS_ADAPTIVE_MODE = "liveness.adaptiveMode";
  public static final String LIVENESS_FALLBACK_TO_PASSWORD = "liveness.fallbackToPassword";
  public static final String LIVENESS_CHALLENGE_COUNT = "liveness.challengeCount";
  public static final String LIVENESS_CHALLENGE_TIMEOUT_SECONDS =
      "liveness.challengeTimeoutSeconds";

  public static final String DEBUG_IMAGE_STORAGE_ENABLED = "debug.imageStorage.enabled";
  public static final String DEBUG_IMAGE_STORAGE_PATH = "debug.imageStorage.path";
  public static final String DEBUG_IMAGE_STORAGE_INCLUDE_METADATA =
      "debug.imageStorage.includeMetadata";

  // Default values
  private static final String DEFAULT_ENDPOINT = "face.bws-eu.bioid.com:443";
  private static final int DEFAULT_JWT_EXPIRE_MINUTES = 60;
  private static final double DEFAULT_VERIFICATION_THRESHOLD = 0.015;
  private static final int DEFAULT_MAX_RETRIES = 3;
  private static final int DEFAULT_VERIFICATION_TIMEOUT = 4;
  private static final int DEFAULT_ENROLLMENT_TIMEOUT = 7;
  private static final int DEFAULT_TEMPLATE_TTL_DAYS = 730;
  private static final int DEFAULT_CLEANUP_INTERVAL_HOURS = 24;
  private static final String DEFAULT_TEMPLATE_TYPE = "STANDARD";
  private static final boolean DEFAULT_TEMPLATE_ENCRYPTION = true;
  private static final int DEFAULT_CHANNEL_POOL_SIZE = 5;
  private static final int DEFAULT_KEEP_ALIVE_SECONDS = 30;
  private static final int DEFAULT_RETRY_MAX_ATTEMPTS = 3;
  private static final double DEFAULT_RETRY_BACKOFF_MULTIPLIER = 2.0;
  private static final int DEFAULT_HEALTH_CHECK_INTERVAL = 30;
  private static final int DEFAULT_HEALTH_CHECK_TIMEOUT = 5;
  private static final String DEFAULT_PREFERRED_REGION = "EU";
  private static final boolean DEFAULT_DATA_RESIDENCY_REQUIRED = false;
  private static final boolean DEFAULT_FAILOVER_ENABLED = true;
  private static final int DEFAULT_LATENCY_THRESHOLD_MS = 1000;
  private static final boolean DEFAULT_LIVENESS_ACTIVE_ENABLED = true;
  private static final boolean DEFAULT_LIVENESS_CHALLENGE_RESPONSE_ENABLED = false;
  private static final double DEFAULT_LIVENESS_CONFIDENCE_THRESHOLD = 0.5;
  private static final int DEFAULT_LIVENESS_MAX_OVERHEAD_MS = 200;
  private static final boolean DEFAULT_LIVENESS_ADAPTIVE_MODE = false;
  private static final boolean DEFAULT_LIVENESS_FALLBACK_TO_PASSWORD = false;
  private static final int DEFAULT_LIVENESS_CHALLENGE_COUNT = 1;
  private static final int DEFAULT_LIVENESS_CHALLENGE_TIMEOUT_SECONDS = 30;
  private static final boolean DEFAULT_DEBUG_IMAGE_STORAGE_ENABLED = false;
  private static final String DEFAULT_DEBUG_IMAGE_STORAGE_PATH = "./debug-images";
  private static final boolean DEFAULT_DEBUG_IMAGE_STORAGE_INCLUDE_METADATA = true;

  static volatile BioIdConfiguration instance;
  private final Properties properties;
  private volatile long lastModified;
  private volatile Path configFilePath;

  private BioIdConfiguration() {
    this.properties = new Properties();
    loadConfiguration();
  }

  /** Gets the singleton configuration instance. */
  public static BioIdConfiguration getInstance() {
    if (instance == null) {
      synchronized (BioIdConfiguration.class) {
        if (instance == null) {
          instance = new BioIdConfiguration();
        }
      }
    }
    return instance;
  }

  /** Reloads configuration from all sources. */
  public synchronized void reload() {
    logger.info("Reloading BioID configuration");
    properties.clear();
    loadConfiguration();
  }

  /** Checks if configuration file has been modified and reloads if necessary. */
  public void checkAndReload() {
    if (configFilePath != null && Files.exists(configFilePath)) {
      try {
        long currentModified = Files.getLastModifiedTime(configFilePath).toMillis();
        if (currentModified > lastModified) {
          logger.info("Configuration file modified, reloading");
          reload();
        }
      } catch (IOException e) {
        logger.warn("Failed to check configuration file modification time", e);
      }
    }
  }

  /** Loads configuration from all sources in order of precedence. */
  private void loadConfiguration() {
    // 1. Load from configuration file
    loadFromFile();

    // 2. Override with environment variables
    loadFromEnvironment();

    // 3. Override with system properties
    loadFromSystemProperties();

    // 4. Validate configuration
    validateConfiguration();

    logger.info("BioID configuration loaded successfully");
    if (logger.isDebugEnabled()) {
      logConfiguration();
    }
  }

  /** Loads configuration from file. */
  private void loadFromFile() {
    // Skip file loading in test mode
    boolean isTestMode = Boolean.parseBoolean(System.getProperty("bioid.test.mode", "false"));
    if (isTestMode) {
      logger.info("Test mode enabled, skipping configuration file loading");
      return;
    }

    // Try Keycloak home directory first
    String keycloakHome = System.getProperty("kc.home");
    if (keycloakHome != null) {
      Path kcConfigPath = Paths.get(keycloakHome, KEYCLOAK_HOME_CONFIG);
      if (loadPropertiesFromPath(kcConfigPath)) {
        configFilePath = kcConfigPath;
        return;
      }
    }

    // Try current directory
    Path currentDirPath = Paths.get(DEFAULT_CONFIG_FILE);
    if (loadPropertiesFromPath(currentDirPath)) {
      configFilePath = currentDirPath;
      return;
    }

    // Try classpath
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
      if (is != null) {
        properties.load(is);
        logger.info("Loaded configuration from classpath: {}", DEFAULT_CONFIG_FILE);
      } else {
        logger.info("No configuration file found, using defaults and environment variables");
      }
    } catch (IOException e) {
      logger.warn("Failed to load configuration from classpath", e);
    }
  }

  /** Loads properties from a specific file path. */
  private boolean loadPropertiesFromPath(Path path) {
    if (Files.exists(path) && Files.isReadable(path)) {
      try (InputStream is = new FileInputStream(path.toFile())) {
        properties.load(is);
        lastModified = Files.getLastModifiedTime(path).toMillis();
        logger.info("Loaded configuration from: {}", path.toAbsolutePath());
        return true;
      } catch (IOException e) {
        logger.warn("Failed to load configuration from: {}", path.toAbsolutePath(), e);
      }
    }
    return false;
  }

  /** Loads configuration from environment variables. */
  private void loadFromEnvironment() {
    // Map environment variables to property keys
    setFromEnv("BWS_ENDPOINT", BIOID_ENDPOINT);
    setFromEnv("BWS_CLIENT_ID", BIOID_CLIENT_ID);
    setFromEnv("BWS_KEY", BIOID_KEY);
    setFromEnv("BWS_JWT_EXPIRE_MINUTES", BIOID_JWT_EXPIRE_MINUTES);

    setFromEnv("VERIFICATION_THRESHOLD", VERIFICATION_THRESHOLD);
    setFromEnv("VERIFICATION_MAX_RETRIES", VERIFICATION_MAX_RETRIES);
    setFromEnv("VERIFICATION_TIMEOUT_SECONDS", VERIFICATION_TIMEOUT_SECONDS);
    setFromEnv("ENROLLMENT_TIMEOUT_SECONDS", ENROLLMENT_TIMEOUT_SECONDS);

    setFromEnv("TEMPLATE_TTL_DAYS", TEMPLATE_TTL_DAYS);
    setFromEnv("TEMPLATE_CLEANUP_INTERVAL_HOURS", TEMPLATE_CLEANUP_INTERVAL_HOURS);
    setFromEnv("TEMPLATE_TYPE", TEMPLATE_TYPE);
    setFromEnv("TEMPLATE_ENCRYPTION_ENABLED", TEMPLATE_ENCRYPTION_ENABLED);

    setFromEnv("GRPC_CHANNEL_POOL_SIZE", GRPC_CHANNEL_POOL_SIZE);
    setFromEnv("GRPC_KEEP_ALIVE_TIME_SECONDS", GRPC_KEEP_ALIVE_TIME_SECONDS);
    setFromEnv("GRPC_RETRY_MAX_ATTEMPTS", GRPC_RETRY_MAX_ATTEMPTS);
    setFromEnv("GRPC_RETRY_BACKOFF_MULTIPLIER", GRPC_RETRY_BACKOFF_MULTIPLIER);

    setFromEnv("HEALTH_CHECK_INTERVAL_SECONDS", HEALTH_CHECK_INTERVAL_SECONDS);
    setFromEnv("HEALTH_CHECK_TIMEOUT_SECONDS", HEALTH_CHECK_TIMEOUT_SECONDS);

    setFromEnv("REGIONAL_PREFERRED_REGION", REGIONAL_PREFERRED_REGION);
    setFromEnv("REGIONAL_DATA_RESIDENCY_REQUIRED", REGIONAL_DATA_RESIDENCY_REQUIRED);
    setFromEnv("REGIONAL_FAILOVER_ENABLED", REGIONAL_FAILOVER_ENABLED);
    setFromEnv("REGIONAL_LATENCY_THRESHOLD_MS", REGIONAL_LATENCY_THRESHOLD_MS);

    setFromEnv("LIVENESS_ACTIVE_ENABLED", LIVENESS_ACTIVE_ENABLED);
    setFromEnv("LIVENESS_CHALLENGE_RESPONSE_ENABLED", LIVENESS_CHALLENGE_RESPONSE_ENABLED);
    setFromEnv("LIVENESS_CONFIDENCE_THRESHOLD", LIVENESS_CONFIDENCE_THRESHOLD);
    setFromEnv("LIVENESS_MAX_OVERHEAD_MS", LIVENESS_MAX_OVERHEAD_MS);
    setFromEnv("LIVENESS_ADAPTIVE_MODE", LIVENESS_ADAPTIVE_MODE);
    setFromEnv("LIVENESS_FALLBACK_TO_PASSWORD", LIVENESS_FALLBACK_TO_PASSWORD);
    setFromEnv("LIVENESS_CHALLENGE_COUNT", LIVENESS_CHALLENGE_COUNT);
    setFromEnv("LIVENESS_CHALLENGE_TIMEOUT_SECONDS", LIVENESS_CHALLENGE_TIMEOUT_SECONDS);

    setFromEnv("DEBUG_IMAGE_STORAGE_ENABLED", DEBUG_IMAGE_STORAGE_ENABLED);
    setFromEnv("DEBUG_IMAGE_STORAGE_PATH", DEBUG_IMAGE_STORAGE_PATH);
    setFromEnv("DEBUG_IMAGE_STORAGE_INCLUDE_METADATA", DEBUG_IMAGE_STORAGE_INCLUDE_METADATA);
    
    // BWS Management API configuration
    setFromEnv("BWS_MANAGEMENT_URL", BWS_MANAGEMENT_URL);
    setFromEnv("BWS_MANAGEMENT_JWT_TOKEN", BWS_MANAGEMENT_JWT_TOKEN);
    setFromEnv("BWS_MANAGEMENT_EMAIL", BWS_MANAGEMENT_EMAIL);
    setFromEnv("BWS_MANAGEMENT_API_KEY", BWS_MANAGEMENT_API_KEY);
    setFromEnv("BWS_MANAGEMENT_JWT_EXPIRE_MINUTES", BWS_MANAGEMENT_JWT_EXPIRE_MINUTES);
  }

  /** Sets property from environment variable if present. */
  private void setFromEnv(String envVar, String propertyKey) {
    String value = System.getenv(envVar);
    if (value != null && !value.trim().isEmpty()) {
      properties.setProperty(propertyKey, value.trim());
    }
  }

  /** Loads configuration from system properties. */
  private void loadFromSystemProperties() {
    // System properties have highest precedence
    for (String key : properties.stringPropertyNames()) {
      String systemValue = System.getProperty(key);
      if (systemValue != null) {
        properties.setProperty(key, systemValue);
      }
    }

    // Also check for additional system properties
    String[] systemKeys = {BIOID_ENDPOINT, BIOID_CLIENT_ID, BIOID_KEY, BIOID_JWT_EXPIRE_MINUTES,
        VERIFICATION_THRESHOLD, VERIFICATION_MAX_RETRIES, VERIFICATION_TIMEOUT_SECONDS,
        ENROLLMENT_TIMEOUT_SECONDS, TEMPLATE_TTL_DAYS, TEMPLATE_CLEANUP_INTERVAL_HOURS,
        TEMPLATE_TYPE, TEMPLATE_ENCRYPTION_ENABLED, GRPC_CHANNEL_POOL_SIZE,
        GRPC_KEEP_ALIVE_TIME_SECONDS, GRPC_RETRY_MAX_ATTEMPTS, GRPC_RETRY_BACKOFF_MULTIPLIER,
        HEALTH_CHECK_INTERVAL_SECONDS, HEALTH_CHECK_TIMEOUT_SECONDS};

    for (String key : systemKeys) {
      String value = System.getProperty(key);
      if (value != null) {
        properties.setProperty(key, value);
      }
    }
  }

  /** Validates the loaded configuration. */
  private void validateConfiguration() {
    StringBuilder errors = new StringBuilder();

    // Only validate required properties if they are actually needed (not in test mode)
    boolean isTestMode = Boolean.parseBoolean(System.getProperty("bioid.test.mode", "false"));

    if (!isTestMode) {
      // Validate required properties
      if (getClientId() == null || getClientId().trim().isEmpty()) {
        errors.append("BioID Client ID is required (bioid.clientId or BWS_CLIENT_ID)\n");
      }

      if (getKey() == null || getKey().trim().isEmpty()) {
        errors.append("BioID Key is required (bioid.key or BWS_KEY)\n");
      }
    }

    // Validate numeric ranges
    if (getVerificationThreshold() < 0.0 || getVerificationThreshold() > 1.0) {
      errors.append("Verification threshold must be between 0.0 and 1.0\n");
    }

    if (getMaxRetries() < 1 || getMaxRetries() > 10) {
      errors.append("Max retries must be between 1 and 10\n");
    }

    if (getVerificationTimeout().toSeconds() < 1 || getVerificationTimeout().toSeconds() > 30) {
      errors.append("Verification timeout must be between 1 and 30 seconds\n");
    }

    if (getEnrollmentTimeout().toSeconds() < 1 || getEnrollmentTimeout().toSeconds() > 60) {
      errors.append("Enrollment timeout must be between 1 and 60 seconds\n");
    }

    // Validate template type
    String templateType = getTemplateType();
    if (!templateType.equals("COMPACT") && !templateType.equals("STANDARD")
        && !templateType.equals("FULL")) {
      errors.append("Template type must be COMPACT, STANDARD, or FULL\n");
    }

    if (errors.length() > 0) {
      throw new IllegalStateException("Configuration validation failed:\n" + errors.toString());
    }
  }

  /** Logs the current configuration (excluding sensitive values). */
  private void logConfiguration() {
    logger.debug("BioID Configuration:");
    logger.debug("  Endpoint: {}", getEndpoint());
    logger.debug("  Client ID: {}", maskSensitive(getClientId()));
    logger.debug("  JWT Expire Minutes: {}", getJwtExpireMinutes());
    logger.debug("  Verification Threshold: {}", getVerificationThreshold());
    logger.debug("  Max Retries: {}", getMaxRetries());
    logger.debug("  Verification Timeout: {}s", getVerificationTimeout().toSeconds());
    logger.debug("  Enrollment Timeout: {}s", getEnrollmentTimeout().toSeconds());
    logger.debug("  Template TTL Days: {}", getTemplateTtlDays());
    logger.debug("  Template Type: {}", getTemplateType());
    logger.debug("  Template Encryption: {}", isTemplateEncryptionEnabled());
    logger.debug("  Channel Pool Size: {}", getChannelPoolSize());
    logger.debug("  Keep Alive Time: {}s", getKeepAliveTime().toSeconds());
    logger.debug("  Retry Max Attempts: {}", getRetryMaxAttempts());
    logger.debug("  Retry Backoff Multiplier: {}", getRetryBackoffMultiplier());
    logger.debug("  Health Check Interval: {}s", getHealthCheckInterval().toSeconds());
    logger.debug("  Health Check Timeout: {}s", getHealthCheckTimeout().toSeconds());
  }

  /** Masks sensitive configuration values for logging. */
  private String maskSensitive(String value) {
    if (value == null || value.length() <= 4) {
      return "****";
    }
    return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
  }

  // Configuration getters with defaults

  public String getEndpoint() {
    return properties.getProperty(BIOID_ENDPOINT, DEFAULT_ENDPOINT);
  }

  public String getClientId() {
    return properties.getProperty(BIOID_CLIENT_ID);
  }

  public String getKey() {
    return properties.getProperty(BIOID_KEY);
  }

  public String getManagementUrl() {
    return properties.getProperty(BWS_MANAGEMENT_URL);
  }

  /**
   * Get the Management API JWT token.
   * Returns the token if directly provided, otherwise returns null.
   * JWT generation from email/API key is handled by BWSAdminService.
   * 
   * @return JWT token or null if not configured
   */
  public String getManagementJwtToken() {
    String token = properties.getProperty(BWS_MANAGEMENT_JWT_TOKEN);
    if (token != null && !token.trim().isEmpty()) {
      return token;
    }
    return null;
  }
  
  public String getManagementEmail() {
    return properties.getProperty(BWS_MANAGEMENT_EMAIL);
  }
  
  public String getManagementApiKey() {
    return properties.getProperty(BWS_MANAGEMENT_API_KEY);
  }

  public int getJwtExpireMinutes() {
    return getIntProperty(BIOID_JWT_EXPIRE_MINUTES, DEFAULT_JWT_EXPIRE_MINUTES);
  }

  public double getVerificationThreshold() {
    return getDoubleProperty(VERIFICATION_THRESHOLD, DEFAULT_VERIFICATION_THRESHOLD);
  }

  public int getMaxRetries() {
    return getIntProperty(VERIFICATION_MAX_RETRIES, DEFAULT_MAX_RETRIES);
  }

  public Duration getVerificationTimeout() {
    return Duration
        .ofSeconds(getIntProperty(VERIFICATION_TIMEOUT_SECONDS, DEFAULT_VERIFICATION_TIMEOUT));
  }

  public Duration getEnrollmentTimeout() {
    return Duration
        .ofSeconds(getIntProperty(ENROLLMENT_TIMEOUT_SECONDS, DEFAULT_ENROLLMENT_TIMEOUT));
  }

  public int getTemplateTtlDays() {
    return getIntProperty(TEMPLATE_TTL_DAYS, DEFAULT_TEMPLATE_TTL_DAYS);
  }

  public Duration getCleanupInterval() {
    return Duration
        .ofHours(getIntProperty(TEMPLATE_CLEANUP_INTERVAL_HOURS, DEFAULT_CLEANUP_INTERVAL_HOURS));
  }

  public String getTemplateType() {
    return properties.getProperty(TEMPLATE_TYPE, DEFAULT_TEMPLATE_TYPE);
  }

  public boolean isTemplateEncryptionEnabled() {
    return getBooleanProperty(TEMPLATE_ENCRYPTION_ENABLED, DEFAULT_TEMPLATE_ENCRYPTION);
  }

  public int getChannelPoolSize() {
    return getIntProperty(GRPC_CHANNEL_POOL_SIZE, DEFAULT_CHANNEL_POOL_SIZE);
  }

  public Duration getKeepAliveTime() {
    return Duration
        .ofSeconds(getIntProperty(GRPC_KEEP_ALIVE_TIME_SECONDS, DEFAULT_KEEP_ALIVE_SECONDS));
  }

  public int getRetryMaxAttempts() {
    return getIntProperty(GRPC_RETRY_MAX_ATTEMPTS, DEFAULT_RETRY_MAX_ATTEMPTS);
  }

  public double getRetryBackoffMultiplier() {
    return getDoubleProperty(GRPC_RETRY_BACKOFF_MULTIPLIER, DEFAULT_RETRY_BACKOFF_MULTIPLIER);
  }

  public Duration getHealthCheckInterval() {
    return Duration
        .ofSeconds(getIntProperty(HEALTH_CHECK_INTERVAL_SECONDS, DEFAULT_HEALTH_CHECK_INTERVAL));
  }

  public Duration getHealthCheckTimeout() {
    return Duration
        .ofSeconds(getIntProperty(HEALTH_CHECK_TIMEOUT_SECONDS, DEFAULT_HEALTH_CHECK_TIMEOUT));
  }

  public String getPreferredRegion() {
    return properties.getProperty(REGIONAL_PREFERRED_REGION, DEFAULT_PREFERRED_REGION);
  }

  public boolean isDataResidencyRequired() {
    return getBooleanProperty(REGIONAL_DATA_RESIDENCY_REQUIRED, DEFAULT_DATA_RESIDENCY_REQUIRED);
  }

  public boolean isFailoverEnabled() {
    return getBooleanProperty(REGIONAL_FAILOVER_ENABLED, DEFAULT_FAILOVER_ENABLED);
  }

  public Duration getLatencyThreshold() {
    return Duration
        .ofMillis(getIntProperty(REGIONAL_LATENCY_THRESHOLD_MS, DEFAULT_LATENCY_THRESHOLD_MS));
  }

  public boolean isLivenessActiveEnabled() {
    return getBooleanProperty(LIVENESS_ACTIVE_ENABLED, DEFAULT_LIVENESS_ACTIVE_ENABLED);
  }

  public boolean isLivenessChallengeResponseEnabled() {
    return getBooleanProperty(LIVENESS_CHALLENGE_RESPONSE_ENABLED,
        DEFAULT_LIVENESS_CHALLENGE_RESPONSE_ENABLED);
  }

  public double getLivenessConfidenceThreshold() {
    return getDoubleProperty(LIVENESS_CONFIDENCE_THRESHOLD, DEFAULT_LIVENESS_CONFIDENCE_THRESHOLD);
  }

  public Duration getLivenessMaxOverhead() {
    return Duration
        .ofMillis(getIntProperty(LIVENESS_MAX_OVERHEAD_MS, DEFAULT_LIVENESS_MAX_OVERHEAD_MS));
  }

  public boolean isLivenessAdaptiveMode() {
    return getBooleanProperty(LIVENESS_ADAPTIVE_MODE, DEFAULT_LIVENESS_ADAPTIVE_MODE);
  }

  public boolean isLivenessFallbackToPassword() {
    return getBooleanProperty(LIVENESS_FALLBACK_TO_PASSWORD, DEFAULT_LIVENESS_FALLBACK_TO_PASSWORD);
  }

  public int getLivenessChallengeCount() {
    return getIntProperty(LIVENESS_CHALLENGE_COUNT, DEFAULT_LIVENESS_CHALLENGE_COUNT);
  }

  public Duration getLivenessChallengeTimeout() {
    return Duration.ofSeconds(getIntProperty(LIVENESS_CHALLENGE_TIMEOUT_SECONDS,
        DEFAULT_LIVENESS_CHALLENGE_TIMEOUT_SECONDS));
  }

  // Helper methods for type conversion

  private int getIntProperty(String key, int defaultValue) {
    String value = properties.getProperty(key);
    if (value != null) {
      try {
        return Integer.parseInt(value.trim());
      } catch (NumberFormatException e) {
        logger.warn("Invalid integer value for {}: {}, using default: {}", key, value,
            defaultValue);
      }
    }
    return defaultValue;
  }

  private double getDoubleProperty(String key, double defaultValue) {
    String value = properties.getProperty(key);
    if (value != null) {
      try {
        return Double.parseDouble(value.trim());
      } catch (NumberFormatException e) {
        logger.warn("Invalid double value for {}: {}, using default: {}", key, value, defaultValue);
      }
    }
    return defaultValue;
  }

  private boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = properties.getProperty(key);
    if (value != null) {
      return Boolean.parseBoolean(value.trim());
    }
    return defaultValue;
  }

  private String getStringProperty(String key, String defaultValue) {
    String value = properties.getProperty(key);
    if (value != null && !value.trim().isEmpty()) {
      return value.trim();
    }
    return defaultValue;
  }

  /** Gets a property value for testing purposes. */
  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  /** Sets a property value for testing purposes. */
  public void setProperty(String key, String value) {
    properties.setProperty(key, value);
  }

  // Debug Image Storage Configuration
  public boolean isDebugImageStorageEnabled() {
    return getBooleanProperty(DEBUG_IMAGE_STORAGE_ENABLED, DEFAULT_DEBUG_IMAGE_STORAGE_ENABLED);
  }

  public String getDebugImageStoragePath() {
    return getStringProperty(DEBUG_IMAGE_STORAGE_PATH, DEFAULT_DEBUG_IMAGE_STORAGE_PATH);
  }

  public boolean isDebugImageStorageIncludeMetadata() {
    return getBooleanProperty(DEBUG_IMAGE_STORAGE_INCLUDE_METADATA,
        DEFAULT_DEBUG_IMAGE_STORAGE_INCLUDE_METADATA);
  }
}
