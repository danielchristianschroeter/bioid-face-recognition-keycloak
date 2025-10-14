package com.bioid.keycloak.config;

import com.bioid.keycloak.admin.dto.LivenessMode;
import com.bioid.keycloak.admin.dto.ChallengeDirection;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.models.RealmModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Administrative configuration for BioID Face Recognition extension.
 * 
 * This class manages configuration settings specific to administrative features
 * including liveness detection, template management, performance settings, and
 * audit configuration. Configuration is persisted in Keycloak's database and
 * can be updated at runtime.
 * 
 * Requirements addressed: 3.2, 6.1, 9.1
 */
@ApplicationScoped
public class AdminConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminConfiguration.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Configuration keys for Keycloak attributes
    private static final String ADMIN_CONFIG_KEY = "bioid.admin.config";
    private static final String CONFIG_VERSION_KEY = "bioid.admin.config.version";
    
    // Current configuration version for migration support
    private static final int CURRENT_CONFIG_VERSION = 1;
    
    // Liveness Detection Settings
    @JsonProperty("defaultLivenessMode")
    private LivenessMode defaultLivenessMode = LivenessMode.PASSIVE;
    
    @JsonProperty("livenessThreshold")
    private double livenessThreshold = 0.7;
    
    @JsonProperty("enableChallengeResponse")
    private boolean enableChallengeResponse = false;
    
    @JsonProperty("allowedChallengeDirections")
    private List<ChallengeDirection> allowedChallengeDirections = Arrays.asList(
        ChallengeDirection.UP, ChallengeDirection.DOWN, 
        ChallengeDirection.LEFT, ChallengeDirection.RIGHT
    );
    
    @JsonProperty("livenessTimeoutSeconds")
    private int livenessTimeoutSeconds = 30;
    
    @JsonProperty("adaptiveLivenessEnabled")
    private boolean adaptiveLivenessEnabled = false;
    
    // Template Management Settings
    @JsonProperty("templateCleanupIntervalHours")
    private int templateCleanupIntervalHours = 24;
    
    @JsonProperty("templateExpirationDays")
    private int templateExpirationDays = 730;
    
    @JsonProperty("autoUpgradeTemplates")
    private boolean autoUpgradeTemplates = false;
    
    @JsonProperty("maxBulkOperationSize")
    private int maxBulkOperationSize = 100;
    
    @JsonProperty("templateHealthCheckEnabled")
    private boolean templateHealthCheckEnabled = true;
    
    @JsonProperty("templateHealthCheckIntervalHours")
    private int templateHealthCheckIntervalHours = 6;
    
    // Performance Settings
    @JsonProperty("templateCacheTtlMinutes")
    private int templateCacheTtlMinutes = 5;
    
    @JsonProperty("bulkOperationTimeoutMinutes")
    private int bulkOperationTimeoutMinutes = 30;
    
    @JsonProperty("maxConcurrentOperations")
    private int maxConcurrentOperations = 5;
    
    @JsonProperty("connectionPoolSize")
    private int connectionPoolSize = 10;
    
    @JsonProperty("requestTimeoutSeconds")
    private int requestTimeoutSeconds = 30;
    
    @JsonProperty("enablePerformanceMetrics")
    private boolean enablePerformanceMetrics = true;
    
    @JsonProperty("metricsCollectionIntervalSeconds")
    private int metricsCollectionIntervalSeconds = 60;
    
    // Audit Settings
    @JsonProperty("enableDetailedAuditLogging")
    private boolean enableDetailedAuditLogging = true;
    
    @JsonProperty("auditRetentionDays")
    private int auditRetentionDays = 365;
    
    @JsonProperty("exportAuditToSiem")
    private boolean exportAuditToSiem = false;
    
    @JsonProperty("siemEndpoint")
    private String siemEndpoint;
    
    @JsonProperty("auditCompressionEnabled")
    private boolean auditCompressionEnabled = true;
    
    @JsonProperty("auditEncryptionEnabled")
    private boolean auditEncryptionEnabled = true;
    
    // Security Settings
    @JsonProperty("requireAdditionalAuthForSensitiveOps")
    private boolean requireAdditionalAuthForSensitiveOps = true;
    
    @JsonProperty("sessionTimeoutMinutes")
    private int sessionTimeoutMinutes = 60;
    
    @JsonProperty("maxFailedAttempts")
    private int maxFailedAttempts = 5;
    
    @JsonProperty("lockoutDurationMinutes")
    private int lockoutDurationMinutes = 30;
    
    // Monitoring and Alerting Settings
    @JsonProperty("enableHealthChecks")
    private boolean enableHealthChecks = true;
    
    @JsonProperty("healthCheckIntervalSeconds")
    private int healthCheckIntervalSeconds = 60;
    
    @JsonProperty("alertingEnabled")
    private boolean alertingEnabled = false;
    
    @JsonProperty("alertThresholds")
    private Map<String, Double> alertThresholds = new HashMap<>();
    
    // Metadata
    @JsonProperty("configVersion")
    private int configVersion = CURRENT_CONFIG_VERSION;
    
    @JsonProperty("lastModified")
    private Instant lastModified = Instant.now();
    
    @JsonProperty("modifiedBy")
    private String modifiedBy;
    
    @JsonIgnore
    private boolean dirty = false;
    
    /**
     * Default constructor with sensible defaults.
     */
    public AdminConfiguration() {
        initializeDefaults();
    }
    
    /**
     * Initialize default alert thresholds.
     */
    private void initializeDefaults() {
        alertThresholds.put("errorRate", 0.05); // 5% error rate
        alertThresholds.put("responseTime", 5000.0); // 5 seconds
        alertThresholds.put("templateFailureRate", 0.02); // 2% template failure rate
        alertThresholds.put("livenessFailureRate", 0.10); // 10% liveness failure rate
    }
    
    /**
     * Validates the configuration settings.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        List<String> errors = new ArrayList<>();
        
        // Validate liveness settings
        if (livenessThreshold < 0.0 || livenessThreshold > 1.0) {
            errors.add("Liveness threshold must be between 0.0 and 1.0");
        }
        
        if (allowedChallengeDirections.isEmpty()) {
            errors.add("At least one challenge direction must be allowed");
        }
        
        // Validate template settings
        if (templateExpirationDays < templateCleanupIntervalHours / 24) {
            errors.add("Template expiration must be longer than cleanup interval");
        }
        
        // Validate performance settings
        if (maxConcurrentOperations > connectionPoolSize) {
            errors.add("Max concurrent operations cannot exceed connection pool size");
        }
        
        // Validate SIEM settings
        if (exportAuditToSiem && (siemEndpoint == null || siemEndpoint.trim().isEmpty())) {
            errors.add("SIEM endpoint is required when audit export is enabled");
        }
        
        // Validate alert thresholds
        for (Map.Entry<String, Double> entry : alertThresholds.entrySet()) {
            if (entry.getValue() < 0.0 || entry.getValue() > 1.0) {
                errors.add("Alert threshold for " + entry.getKey() + " must be between 0.0 and 1.0");
            }
        }
        
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Configuration validation failed: " + String.join(", ", errors));
        }
    }
    
    /**
     * Loads configuration from Keycloak realm attributes.
     * 
     * @param realm the Keycloak realm
     * @return the loaded configuration or default if not found
     */
    public static AdminConfiguration loadFromRealm(RealmModel realm) {
        try {
            String configJson = realm.getAttribute(ADMIN_CONFIG_KEY);
            if (configJson != null && !configJson.trim().isEmpty()) {
                AdminConfiguration config = objectMapper.readValue(configJson, AdminConfiguration.class);
                config.dirty = false;
                
                // Check for version migration
                String versionStr = realm.getAttribute(CONFIG_VERSION_KEY);
                int storedVersion = versionStr != null ? Integer.parseInt(versionStr) : 0;
                if (storedVersion < CURRENT_CONFIG_VERSION) {
                    logger.info("Migrating admin configuration from version {} to {}", storedVersion, CURRENT_CONFIG_VERSION);
                    config = migrateConfiguration(config, storedVersion);
                    config.saveToRealm(realm);
                }
                
                return config;
            }
        } catch (Exception e) {
            logger.warn("Failed to load admin configuration from realm, using defaults", e);
        }
        
        // Return default configuration
        AdminConfiguration defaultConfig = new AdminConfiguration();
        defaultConfig.saveToRealm(realm);
        return defaultConfig;
    }
    
    /**
     * Saves configuration to Keycloak realm attributes.
     * 
     * @param realm the Keycloak realm
     */
    public void saveToRealm(RealmModel realm) {
        try {
            this.lastModified = Instant.now();
            this.configVersion = CURRENT_CONFIG_VERSION;
            
            String configJson = objectMapper.writeValueAsString(this);
            realm.setAttribute(ADMIN_CONFIG_KEY, configJson);
            realm.setAttribute(CONFIG_VERSION_KEY, String.valueOf(CURRENT_CONFIG_VERSION));
            
            this.dirty = false;
            logger.debug("Admin configuration saved to realm: {}", realm.getName());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize admin configuration", e);
        }
    }
    
    /**
     * Updates configuration at runtime with validation.
     * 
     * @param updates the configuration updates
     * @param modifiedBy the user making the changes
     * @throws IllegalArgumentException if validation fails
     */
    public void updateConfiguration(AdminConfiguration updates, String modifiedBy) {
        // Create a copy for validation
        AdminConfiguration testConfig = this.copy();
        testConfig.applyUpdates(updates);
        testConfig.validate();
        
        // If validation passes, apply updates
        this.applyUpdates(updates);
        this.modifiedBy = modifiedBy;
        this.dirty = true;
        
        logger.info("Admin configuration updated by: {}", modifiedBy);
    }
    
    /**
     * Applies updates from another configuration instance.
     */
    private void applyUpdates(AdminConfiguration updates) {
        if (updates.defaultLivenessMode != null) {
            this.defaultLivenessMode = updates.defaultLivenessMode;
        }
        if (updates.livenessThreshold != 0.0) {
            this.livenessThreshold = updates.livenessThreshold;
        }
        this.enableChallengeResponse = updates.enableChallengeResponse;
        if (updates.allowedChallengeDirections != null && !updates.allowedChallengeDirections.isEmpty()) {
            this.allowedChallengeDirections = new ArrayList<>(updates.allowedChallengeDirections);
        }
        if (updates.livenessTimeoutSeconds != 0) {
            this.livenessTimeoutSeconds = updates.livenessTimeoutSeconds;
        }
        this.adaptiveLivenessEnabled = updates.adaptiveLivenessEnabled;
        
        // Template management updates
        if (updates.templateCleanupIntervalHours != 0) {
            this.templateCleanupIntervalHours = updates.templateCleanupIntervalHours;
        }
        if (updates.templateExpirationDays != 0) {
            this.templateExpirationDays = updates.templateExpirationDays;
        }
        this.autoUpgradeTemplates = updates.autoUpgradeTemplates;
        if (updates.maxBulkOperationSize != 0) {
            this.maxBulkOperationSize = updates.maxBulkOperationSize;
        }
        this.templateHealthCheckEnabled = updates.templateHealthCheckEnabled;
        if (updates.templateHealthCheckIntervalHours != 0) {
            this.templateHealthCheckIntervalHours = updates.templateHealthCheckIntervalHours;
        }
        
        // Performance updates
        if (updates.templateCacheTtlMinutes != 0) {
            this.templateCacheTtlMinutes = updates.templateCacheTtlMinutes;
        }
        if (updates.bulkOperationTimeoutMinutes != 0) {
            this.bulkOperationTimeoutMinutes = updates.bulkOperationTimeoutMinutes;
        }
        if (updates.maxConcurrentOperations != 0) {
            this.maxConcurrentOperations = updates.maxConcurrentOperations;
        }
        if (updates.connectionPoolSize != 0) {
            this.connectionPoolSize = updates.connectionPoolSize;
        }
        if (updates.requestTimeoutSeconds != 0) {
            this.requestTimeoutSeconds = updates.requestTimeoutSeconds;
        }
        this.enablePerformanceMetrics = updates.enablePerformanceMetrics;
        if (updates.metricsCollectionIntervalSeconds != 0) {
            this.metricsCollectionIntervalSeconds = updates.metricsCollectionIntervalSeconds;
        }
        
        // Audit updates
        this.enableDetailedAuditLogging = updates.enableDetailedAuditLogging;
        if (updates.auditRetentionDays != 0) {
            this.auditRetentionDays = updates.auditRetentionDays;
        }
        this.exportAuditToSiem = updates.exportAuditToSiem;
        if (updates.siemEndpoint != null) {
            this.siemEndpoint = updates.siemEndpoint;
        }
        this.auditCompressionEnabled = updates.auditCompressionEnabled;
        this.auditEncryptionEnabled = updates.auditEncryptionEnabled;
        
        // Security updates
        this.requireAdditionalAuthForSensitiveOps = updates.requireAdditionalAuthForSensitiveOps;
        if (updates.sessionTimeoutMinutes != 0) {
            this.sessionTimeoutMinutes = updates.sessionTimeoutMinutes;
        }
        if (updates.maxFailedAttempts != 0) {
            this.maxFailedAttempts = updates.maxFailedAttempts;
        }
        if (updates.lockoutDurationMinutes != 0) {
            this.lockoutDurationMinutes = updates.lockoutDurationMinutes;
        }
        
        // Monitoring updates
        this.enableHealthChecks = updates.enableHealthChecks;
        if (updates.healthCheckIntervalSeconds != 0) {
            this.healthCheckIntervalSeconds = updates.healthCheckIntervalSeconds;
        }
        this.alertingEnabled = updates.alertingEnabled;
        if (updates.alertThresholds != null && !updates.alertThresholds.isEmpty()) {
            this.alertThresholds.putAll(updates.alertThresholds);
        }
    }
    
    /**
     * Creates a deep copy of this configuration.
     */
    public AdminConfiguration copy() {
        try {
            String json = objectMapper.writeValueAsString(this);
            return objectMapper.readValue(json, AdminConfiguration.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to copy configuration", e);
        }
    }
    
    /**
     * Migrates configuration from older versions.
     */
    private static AdminConfiguration migrateConfiguration(AdminConfiguration config, int fromVersion) {
        // Future migration logic would go here
        // For now, just update the version
        config.configVersion = CURRENT_CONFIG_VERSION;
        return config;
    }
    
    /**
     * Exports configuration for deployment automation.
     */
    public String exportConfiguration() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to export configuration", e);
        }
    }
    
    /**
     * Imports configuration from JSON string.
     */
    public static AdminConfiguration importConfiguration(String configJson) {
        try {
            AdminConfiguration config = objectMapper.readValue(configJson, AdminConfiguration.class);
            config.validate();
            return config;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to import configuration", e);
        }
    }
    
    // Convenience methods for Duration objects
    
    public Duration getLivenessTimeout() {
        return Duration.ofSeconds(livenessTimeoutSeconds);
    }
    
    public Duration getTemplateCleanupInterval() {
        return Duration.ofHours(templateCleanupIntervalHours);
    }
    
    public Duration getTemplateExpiration() {
        return Duration.ofDays(templateExpirationDays);
    }
    
    public Duration getTemplateCacheTtl() {
        return Duration.ofMinutes(templateCacheTtlMinutes);
    }
    
    public Duration getBulkOperationTimeout() {
        return Duration.ofMinutes(bulkOperationTimeoutMinutes);
    }
    
    public Duration getRequestTimeout() {
        return Duration.ofSeconds(requestTimeoutSeconds);
    }
    
    public Duration getSessionTimeout() {
        return Duration.ofMinutes(sessionTimeoutMinutes);
    }
    
    public Duration getLockoutDuration() {
        return Duration.ofMinutes(lockoutDurationMinutes);
    }
    
    public Duration getHealthCheckInterval() {
        return Duration.ofSeconds(healthCheckIntervalSeconds);
    }
    
    public Duration getMetricsCollectionInterval() {
        return Duration.ofSeconds(metricsCollectionIntervalSeconds);
    }
    
    public Duration getTemplateHealthCheckInterval() {
        return Duration.ofHours(templateHealthCheckIntervalHours);
    }
    
    // Getters and setters
    
    public LivenessMode getDefaultLivenessMode() { return defaultLivenessMode; }
    public void setDefaultLivenessMode(LivenessMode defaultLivenessMode) { 
        this.defaultLivenessMode = defaultLivenessMode; 
        this.dirty = true;
    }
    
    public double getLivenessThreshold() { return livenessThreshold; }
    public void setLivenessThreshold(double livenessThreshold) { 
        this.livenessThreshold = livenessThreshold; 
        this.dirty = true;
    }
    
    public boolean isEnableChallengeResponse() { return enableChallengeResponse; }
    public void setEnableChallengeResponse(boolean enableChallengeResponse) { 
        this.enableChallengeResponse = enableChallengeResponse; 
        this.dirty = true;
    }
    
    public List<ChallengeDirection> getAllowedChallengeDirections() { return allowedChallengeDirections; }
    public void setAllowedChallengeDirections(List<ChallengeDirection> allowedChallengeDirections) { 
        this.allowedChallengeDirections = allowedChallengeDirections; 
        this.dirty = true;
    }
    
    public int getLivenessTimeoutSeconds() { return livenessTimeoutSeconds; }
    public void setLivenessTimeoutSeconds(int livenessTimeoutSeconds) { 
        this.livenessTimeoutSeconds = livenessTimeoutSeconds; 
        this.dirty = true;
    }
    
    public boolean isAdaptiveLivenessEnabled() { return adaptiveLivenessEnabled; }
    public void setAdaptiveLivenessEnabled(boolean adaptiveLivenessEnabled) { 
        this.adaptiveLivenessEnabled = adaptiveLivenessEnabled; 
        this.dirty = true;
    }
    
    public int getTemplateCleanupIntervalHours() { return templateCleanupIntervalHours; }
    public void setTemplateCleanupIntervalHours(int templateCleanupIntervalHours) { 
        this.templateCleanupIntervalHours = templateCleanupIntervalHours; 
        this.dirty = true;
    }
    
    public int getTemplateExpirationDays() { return templateExpirationDays; }
    public void setTemplateExpirationDays(int templateExpirationDays) { 
        this.templateExpirationDays = templateExpirationDays; 
        this.dirty = true;
    }
    
    public boolean isAutoUpgradeTemplates() { return autoUpgradeTemplates; }
    public void setAutoUpgradeTemplates(boolean autoUpgradeTemplates) { 
        this.autoUpgradeTemplates = autoUpgradeTemplates; 
        this.dirty = true;
    }
    
    public int getMaxBulkOperationSize() { return maxBulkOperationSize; }
    public void setMaxBulkOperationSize(int maxBulkOperationSize) { 
        this.maxBulkOperationSize = maxBulkOperationSize; 
        this.dirty = true;
    }
    
    public boolean isTemplateHealthCheckEnabled() { return templateHealthCheckEnabled; }
    public void setTemplateHealthCheckEnabled(boolean templateHealthCheckEnabled) { 
        this.templateHealthCheckEnabled = templateHealthCheckEnabled; 
        this.dirty = true;
    }
    
    public int getTemplateHealthCheckIntervalHours() { return templateHealthCheckIntervalHours; }
    public void setTemplateHealthCheckIntervalHours(int templateHealthCheckIntervalHours) { 
        this.templateHealthCheckIntervalHours = templateHealthCheckIntervalHours; 
        this.dirty = true;
    }
    
    public int getTemplateCacheTtlMinutes() { return templateCacheTtlMinutes; }
    public void setTemplateCacheTtlMinutes(int templateCacheTtlMinutes) { 
        this.templateCacheTtlMinutes = templateCacheTtlMinutes; 
        this.dirty = true;
    }
    
    public int getBulkOperationTimeoutMinutes() { return bulkOperationTimeoutMinutes; }
    public void setBulkOperationTimeoutMinutes(int bulkOperationTimeoutMinutes) { 
        this.bulkOperationTimeoutMinutes = bulkOperationTimeoutMinutes; 
        this.dirty = true;
    }
    
    public int getMaxConcurrentOperations() { return maxConcurrentOperations; }
    public void setMaxConcurrentOperations(int maxConcurrentOperations) { 
        this.maxConcurrentOperations = maxConcurrentOperations; 
        this.dirty = true;
    }
    
    public int getConnectionPoolSize() { return connectionPoolSize; }
    public void setConnectionPoolSize(int connectionPoolSize) { 
        this.connectionPoolSize = connectionPoolSize; 
        this.dirty = true;
    }
    
    public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) { 
        this.requestTimeoutSeconds = requestTimeoutSeconds; 
        this.dirty = true;
    }
    
    public boolean isEnablePerformanceMetrics() { return enablePerformanceMetrics; }
    public void setEnablePerformanceMetrics(boolean enablePerformanceMetrics) { 
        this.enablePerformanceMetrics = enablePerformanceMetrics; 
        this.dirty = true;
    }
    
    public int getMetricsCollectionIntervalSeconds() { return metricsCollectionIntervalSeconds; }
    public void setMetricsCollectionIntervalSeconds(int metricsCollectionIntervalSeconds) { 
        this.metricsCollectionIntervalSeconds = metricsCollectionIntervalSeconds; 
        this.dirty = true;
    }
    
    public boolean isEnableDetailedAuditLogging() { return enableDetailedAuditLogging; }
    public void setEnableDetailedAuditLogging(boolean enableDetailedAuditLogging) { 
        this.enableDetailedAuditLogging = enableDetailedAuditLogging; 
        this.dirty = true;
    }
    
    public int getAuditRetentionDays() { return auditRetentionDays; }
    public void setAuditRetentionDays(int auditRetentionDays) { 
        this.auditRetentionDays = auditRetentionDays; 
        this.dirty = true;
    }
    
    public boolean isExportAuditToSiem() { return exportAuditToSiem; }
    public void setExportAuditToSiem(boolean exportAuditToSiem) { 
        this.exportAuditToSiem = exportAuditToSiem; 
        this.dirty = true;
    }
    
    public String getSiemEndpoint() { return siemEndpoint; }
    public void setSiemEndpoint(String siemEndpoint) { 
        this.siemEndpoint = siemEndpoint; 
        this.dirty = true;
    }
    
    public boolean isAuditCompressionEnabled() { return auditCompressionEnabled; }
    public void setAuditCompressionEnabled(boolean auditCompressionEnabled) { 
        this.auditCompressionEnabled = auditCompressionEnabled; 
        this.dirty = true;
    }
    
    public boolean isAuditEncryptionEnabled() { return auditEncryptionEnabled; }
    public void setAuditEncryptionEnabled(boolean auditEncryptionEnabled) { 
        this.auditEncryptionEnabled = auditEncryptionEnabled; 
        this.dirty = true;
    }
    
    public boolean isRequireAdditionalAuthForSensitiveOps() { return requireAdditionalAuthForSensitiveOps; }
    public void setRequireAdditionalAuthForSensitiveOps(boolean requireAdditionalAuthForSensitiveOps) { 
        this.requireAdditionalAuthForSensitiveOps = requireAdditionalAuthForSensitiveOps; 
        this.dirty = true;
    }
    
    public int getSessionTimeoutMinutes() { return sessionTimeoutMinutes; }
    public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) { 
        this.sessionTimeoutMinutes = sessionTimeoutMinutes; 
        this.dirty = true;
    }
    
    public int getMaxFailedAttempts() { return maxFailedAttempts; }
    public void setMaxFailedAttempts(int maxFailedAttempts) { 
        this.maxFailedAttempts = maxFailedAttempts; 
        this.dirty = true;
    }
    
    public int getLockoutDurationMinutes() { return lockoutDurationMinutes; }
    public void setLockoutDurationMinutes(int lockoutDurationMinutes) { 
        this.lockoutDurationMinutes = lockoutDurationMinutes; 
        this.dirty = true;
    }
    
    public boolean isEnableHealthChecks() { return enableHealthChecks; }
    public void setEnableHealthChecks(boolean enableHealthChecks) { 
        this.enableHealthChecks = enableHealthChecks; 
        this.dirty = true;
    }
    
    public int getHealthCheckIntervalSeconds() { return healthCheckIntervalSeconds; }
    public void setHealthCheckIntervalSeconds(int healthCheckIntervalSeconds) { 
        this.healthCheckIntervalSeconds = healthCheckIntervalSeconds; 
        this.dirty = true;
    }
    
    public boolean isAlertingEnabled() { return alertingEnabled; }
    public void setAlertingEnabled(boolean alertingEnabled) { 
        this.alertingEnabled = alertingEnabled; 
        this.dirty = true;
    }
    
    public Map<String, Double> getAlertThresholds() { return alertThresholds; }
    public void setAlertThresholds(Map<String, Double> alertThresholds) { 
        this.alertThresholds = alertThresholds; 
        this.dirty = true;
    }
    
    public int getConfigVersion() { return configVersion; }
    
    public Instant getLastModified() { return lastModified; }
    
    public String getModifiedBy() { return modifiedBy; }
    
    public boolean isDirty() { return dirty; }
}