package com.bioid.keycloak.config;

import com.bioid.keycloak.audit.AdminAuditService;
import com.bioid.keycloak.audit.AdminActionType;
import com.bioid.keycloak.audit.AdminActionResult;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for managing administrative configuration.
 * 
 * Provides thread-safe access to configuration with caching, validation,
 * and audit logging. Supports runtime configuration updates and automatic
 * persistence to Keycloak's database.
 * 
 * Requirements addressed: 3.2, 6.1, 9.1
 */
@ApplicationScoped
public class AdminConfigurationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminConfigurationService.class);
    
    @Inject
    private AdminAuditService auditService;
    
    // Cache for configurations by realm ID
    private final ConcurrentHashMap<String, CachedConfiguration> configCache = new ConcurrentHashMap<>();
    
    // Read-write locks for thread safety
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> realmLocks = new ConcurrentHashMap<>();
    
    /**
     * Cached configuration with metadata.
     */
    private static class CachedConfiguration {
        final AdminConfiguration configuration;
        final Instant loadedAt;
        final long version;
        
        CachedConfiguration(AdminConfiguration configuration, long version) {
            this.configuration = configuration;
            this.loadedAt = Instant.now();
            this.version = version;
        }
        
        boolean isExpired(long cacheTtlMillis) {
            return Instant.now().toEpochMilli() - loadedAt.toEpochMilli() > cacheTtlMillis;
        }
    }
    
    /**
     * Gets the administrative configuration for a realm.
     * 
     * @param session the Keycloak session
     * @param realm the realm
     * @return the configuration
     */
    public AdminConfiguration getConfiguration(KeycloakSession session, RealmModel realm) {
        String realmId = realm.getId();
        ReentrantReadWriteLock lock = realmLocks.computeIfAbsent(realmId, k -> new ReentrantReadWriteLock());
        
        lock.readLock().lock();
        try {
            CachedConfiguration cached = configCache.get(realmId);
            if (cached != null && !cached.isExpired(getCacheTtlMillis())) {
                return cached.configuration;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        // Need to load/reload configuration
        lock.writeLock().lock();
        try {
            // Double-check pattern
            CachedConfiguration cached = configCache.get(realmId);
            if (cached != null && !cached.isExpired(getCacheTtlMillis())) {
                return cached.configuration;
            }
            
            // Load configuration from realm
            AdminConfiguration config = AdminConfiguration.loadFromRealm(realm);
            long version = System.currentTimeMillis();
            configCache.put(realmId, new CachedConfiguration(config, version));
            
            logger.debug("Loaded admin configuration for realm: {}", realm.getName());
            return config;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Updates the administrative configuration for a realm.
     * 
     * @param session the Keycloak session
     * @param realm the realm
     * @param updates the configuration updates
     * @param user the user making the changes
     * @throws IllegalArgumentException if validation fails
     */
    public void updateConfiguration(KeycloakSession session, RealmModel realm, 
                                  AdminConfiguration updates, UserModel user) {
        String realmId = realm.getId();
        String userId = user != null ? user.getId() : "system";
        String username = user != null ? user.getUsername() : "system";
        
        ReentrantReadWriteLock lock = realmLocks.computeIfAbsent(realmId, k -> new ReentrantReadWriteLock());
        
        lock.writeLock().lock();
        try {
            // Get current configuration
            AdminConfiguration current = getConfigurationInternal(realm);
            
            // Create a copy for validation
            AdminConfiguration updated = current.copy();
            updated.updateConfiguration(updates, username);
            
            // Validate the updated configuration
            updated.validate();
            
            // Save to realm
            updated.saveToRealm(realm);
            
            // Update cache
            long version = System.currentTimeMillis();
            configCache.put(realmId, new CachedConfiguration(updated, version));
            
            // Audit the change
            auditConfigurationChange(session, realm, user, current, updated);
            
            logger.info("Admin configuration updated for realm: {} by user: {}", realm.getName(), username);
            
        } catch (Exception e) {
            // Audit the failure
            if (auditService != null) {
                Map<String, Object> details = new HashMap<>();
                details.put("error", e.getMessage());
                auditService.logAdminAction(
                    AdminActionType.CONFIGURATION_UPDATE, user, null, realm, details, AdminActionResult.FAILURE
                );
            }
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Reloads configuration from the database, bypassing cache.
     * 
     * @param session the Keycloak session
     * @param realm the realm
     * @return the reloaded configuration
     */
    public AdminConfiguration reloadConfiguration(KeycloakSession session, RealmModel realm) {
        String realmId = realm.getId();
        ReentrantReadWriteLock lock = realmLocks.computeIfAbsent(realmId, k -> new ReentrantReadWriteLock());
        
        lock.writeLock().lock();
        try {
            // Force reload from realm
            AdminConfiguration config = AdminConfiguration.loadFromRealm(realm);
            long version = System.currentTimeMillis();
            configCache.put(realmId, new CachedConfiguration(config, version));
            
            logger.info("Reloaded admin configuration for realm: {}", realm.getName());
            return config;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Exports configuration for deployment automation.
     * 
     * @param session the Keycloak session
     * @param realm the realm
     * @param user the user requesting export
     * @return the configuration as JSON string
     */
    public String exportConfiguration(KeycloakSession session, RealmModel realm, UserModel user) {
        AdminConfiguration config = getConfiguration(session, realm);
        
        // Audit the export
        if (auditService != null) {
            Map<String, Object> details = new HashMap<>();
            details.put("action", "Configuration exported");
            auditService.logAdminAction(
                AdminActionType.CONFIGURATION_EXPORT, user, null, realm, details, AdminActionResult.SUCCESS
            );
        }
        
        return config.exportConfiguration();
    }
    
    /**
     * Imports configuration from JSON string.
     * 
     * @param session the Keycloak session
     * @param realm the realm
     * @param configJson the configuration JSON
     * @param user the user importing configuration
     * @throws IllegalArgumentException if validation fails
     */
    public void importConfiguration(KeycloakSession session, RealmModel realm, 
                                  String configJson, UserModel user) {
        String username = user != null ? user.getUsername() : "system";
        
        try {
            // Parse and validate imported configuration
            AdminConfiguration imported = AdminConfiguration.importConfiguration(configJson);
            
            // Update with imported configuration
            updateConfiguration(session, realm, imported, user);
            
            // Audit the import
            if (auditService != null) {
                Map<String, Object> details = new HashMap<>();
                details.put("action", "Configuration imported successfully");
                auditService.logAdminAction(
                    AdminActionType.CONFIGURATION_IMPORT, user, null, realm, details, AdminActionResult.SUCCESS
                );
            }
            
            logger.info("Admin configuration imported for realm: {} by user: {}", realm.getName(), username);
            
        } catch (Exception e) {
            // Audit the failure
            if (auditService != null) {
                Map<String, Object> details = new HashMap<>();
                details.put("error", e.getMessage());
                auditService.logAdminAction(
                    AdminActionType.CONFIGURATION_IMPORT, user, null, realm, details, AdminActionResult.FAILURE
                );
            }
            throw new RuntimeException("Failed to import configuration", e);
        }
    }
    
    /**
     * Validates configuration without saving.
     * 
     * @param session the Keycloak session
     * @param realm the realm
     * @param updates the configuration updates to validate
     * @return validation result with any errors
     */
    public ConfigurationValidationResult validateConfiguration(KeycloakSession session, RealmModel realm, 
                                                             AdminConfiguration updates) {
        try {
            AdminConfiguration current = getConfiguration(session, realm);
            AdminConfiguration test = current.copy();
            test.updateConfiguration(updates, "validation");
            test.validate();
            
            return new ConfigurationValidationResult(true, null);
        } catch (Exception e) {
            return new ConfigurationValidationResult(false, e.getMessage());
        }
    }
    
    /**
     * Clears the configuration cache for a realm.
     * 
     * @param realmId the realm ID
     */
    public void clearCache(String realmId) {
        configCache.remove(realmId);
        logger.debug("Cleared configuration cache for realm: {}", realmId);
    }
    
    /**
     * Clears all configuration caches.
     */
    public void clearAllCaches() {
        configCache.clear();
        logger.debug("Cleared all configuration caches");
    }
    
    /**
     * Gets configuration cache statistics.
     * 
     * @return cache statistics
     */
    public ConfigurationCacheStats getCacheStats() {
        int totalEntries = configCache.size();
        long totalMemory = configCache.values().stream()
            .mapToLong(cached -> estimateConfigurationSize(cached.configuration))
            .sum();
        
        return new ConfigurationCacheStats(totalEntries, totalMemory);
    }
    
    /**
     * Internal method to get configuration without caching logic.
     */
    private AdminConfiguration getConfigurationInternal(RealmModel realm) {
        CachedConfiguration cached = configCache.get(realm.getId());
        if (cached != null) {
            return cached.configuration;
        }
        return AdminConfiguration.loadFromRealm(realm);
    }
    
    /**
     * Audits configuration changes.
     */
    private void auditConfigurationChange(KeycloakSession session, RealmModel realm, UserModel user,
                                        AdminConfiguration before, AdminConfiguration after) {
        if (auditService != null) {
            StringBuilder details = new StringBuilder();
            details.append("Configuration updated. Changes: ");
            
            // Compare key settings and log changes
            if (!before.getDefaultLivenessMode().equals(after.getDefaultLivenessMode())) {
                details.append("livenessMode: ").append(before.getDefaultLivenessMode())
                       .append(" -> ").append(after.getDefaultLivenessMode()).append("; ");
            }
            
            if (before.getLivenessThreshold() != after.getLivenessThreshold()) {
                details.append("livenessThreshold: ").append(before.getLivenessThreshold())
                       .append(" -> ").append(after.getLivenessThreshold()).append("; ");
            }
            
            if (before.getMaxBulkOperationSize() != after.getMaxBulkOperationSize()) {
                details.append("maxBulkOperationSize: ").append(before.getMaxBulkOperationSize())
                       .append(" -> ").append(after.getMaxBulkOperationSize()).append("; ");
            }
            
            if (before.isEnableDetailedAuditLogging() != after.isEnableDetailedAuditLogging()) {
                details.append("detailedAuditLogging: ").append(before.isEnableDetailedAuditLogging())
                       .append(" -> ").append(after.isEnableDetailedAuditLogging()).append("; ");
            }
            
            Map<String, Object> auditDetails = new HashMap<>();
            auditDetails.put("changes", details.toString());
            auditService.logAdminAction(
                AdminActionType.CONFIGURATION_UPDATE, user, null, realm, auditDetails, AdminActionResult.SUCCESS
            );
        }
    }
    
    /**
     * Gets cache TTL in milliseconds.
     */
    private long getCacheTtlMillis() {
        // Default to 5 minutes, could be made configurable
        return 5 * 60 * 1000L;
    }
    
    /**
     * Estimates the memory size of a configuration object.
     */
    private long estimateConfigurationSize(AdminConfiguration config) {
        // Rough estimate - could be more precise with object instrumentation
        return 2048; // 2KB estimate per configuration
    }
    
    /**
     * Configuration validation result.
     */
    public static class ConfigurationValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ConfigurationValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * Configuration cache statistics.
     */
    public static class ConfigurationCacheStats {
        private final int totalEntries;
        private final long totalMemoryBytes;
        
        public ConfigurationCacheStats(int totalEntries, long totalMemoryBytes) {
            this.totalEntries = totalEntries;
            this.totalMemoryBytes = totalMemoryBytes;
        }
        
        public int getTotalEntries() { return totalEntries; }
        public long getTotalMemoryBytes() { return totalMemoryBytes; }
    }
}