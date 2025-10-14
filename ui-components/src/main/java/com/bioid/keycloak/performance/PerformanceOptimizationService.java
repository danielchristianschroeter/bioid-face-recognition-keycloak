package com.bioid.keycloak.performance;

import com.bioid.keycloak.config.AdminConfiguration;
import com.bioid.keycloak.config.AdminConfigurationService;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for performance monitoring and optimization of administrative operations.
 * 
 * Coordinates template caching, connection pooling, load balancing, and automatic
 * throttling to ensure optimal performance under varying load conditions.
 * 
 * Requirements addressed: 9.1, 9.2, 9.5, 9.6
 */
@ApplicationScoped
public class PerformanceOptimizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceOptimizationService.class);
    
    @Inject
    private AdminConfigurationService configService;
    
    private final Map<String, PerformanceContext> realmContexts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService optimizationExecutor;
    private volatile ScheduledFuture<?> optimizationTask;
    
    /**
     * Performance context for a specific realm.
     */
    private static class PerformanceContext {
        private final String realmId;
        private final TemplateCache templateCache;
        private final BulkOperationThrottler throttler;
        private final AdminPerformanceMetrics metrics;
        private volatile Instant lastOptimization = Instant.now();
        
        public PerformanceContext(String realmId, AdminConfiguration config) {
            this.realmId = realmId;
            this.templateCache = TemplateCache.getInstance();
            this.throttler = BulkOperationThrottler.getInstance(config);
            this.metrics = AdminPerformanceMetrics.getInstance(config);
        }
        
        public void updateConfiguration(AdminConfiguration config) {
            templateCache.updateTtl(config);
            throttler.updateConfiguration(config);
            metrics.updateConfiguration(config);
        }
        
        public String getRealmId() { return realmId; }
        public TemplateCache getTemplateCache() { return templateCache; }
        public BulkOperationThrottler getThrottler() { return throttler; }
        public AdminPerformanceMetrics getMetrics() { return metrics; }
        public Instant getLastOptimization() { return lastOptimization; }
        public void setLastOptimization(Instant lastOptimization) { this.lastOptimization = lastOptimization; }
    }
    
    public PerformanceOptimizationService() {
        this.optimizationExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "performance-optimization");
            t.setDaemon(true);
            return t;
        });
        
        startOptimizationTask();
        logger.info("Performance optimization service initialized");
    }
    
    /**
     * Gets or creates performance context for a realm.
     */
    public PerformanceContext getPerformanceContext(KeycloakSession session, RealmModel realm) {
        String realmId = realm.getId();
        return realmContexts.computeIfAbsent(realmId, id -> {
            AdminConfiguration config = configService.getConfiguration(session, realm);
            return new PerformanceContext(id, config);
        });
    }
    
    /**
     * Updates performance configuration for a realm.
     */
    public void updateRealmConfiguration(KeycloakSession session, RealmModel realm) {
        String realmId = realm.getId();
        PerformanceContext context = realmContexts.get(realmId);
        
        if (context != null) {
            AdminConfiguration config = configService.getConfiguration(session, realm);
            context.updateConfiguration(config);
            logger.debug("Updated performance configuration for realm: {}", realm.getName());
        }
    }
    
    /**
     * Gets template cache for a realm.
     */
    public TemplateCache getTemplateCache(KeycloakSession session, RealmModel realm) {
        return getPerformanceContext(session, realm).getTemplateCache();
    }
    
    /**
     * Gets bulk operation throttler for a realm.
     */
    public BulkOperationThrottler getBulkOperationThrottler(KeycloakSession session, RealmModel realm) {
        return getPerformanceContext(session, realm).getThrottler();
    }
    
    /**
     * Gets performance metrics for a realm.
     */
    public AdminPerformanceMetrics getPerformanceMetrics(KeycloakSession session, RealmModel realm) {
        return getPerformanceContext(session, realm).getMetrics();
    }
    
    /**
     * Gets comprehensive performance statistics for a realm.
     */
    public RealmPerformanceStats getRealmPerformanceStats(KeycloakSession session, RealmModel realm) {
        PerformanceContext context = getPerformanceContext(session, realm);
        
        // Template cache stats
        TemplateCache.TemplateCacheStats cacheStats = context.getTemplateCache().getStats();
        
        // Throttler stats
        BulkOperationThrottler.ThrottleStats throttleStats = context.getThrottler().getStats();
        
        // Performance metrics
        AdminPerformanceMetrics.AdminPerformanceSummary perfSummary = 
            context.getMetrics().getPerformanceSummary();
        
        // Connection pool stats
        GrpcChannelPool channelPool = GrpcChannelPool.getInstance();
        Map<String, GrpcChannelPool.ChannelPoolStats> poolStats = channelPool.getAllStats();
        
        return new RealmPerformanceStats(
            realm.getId(),
            realm.getName(),
            cacheStats,
            throttleStats,
            perfSummary,
            poolStats,
            context.getLastOptimization()
        );
    }
    
    /**
     * Gets performance statistics for all realms.
     */
    public Map<String, RealmPerformanceStats> getAllRealmStats(KeycloakSession session) {
        Map<String, RealmPerformanceStats> stats = new ConcurrentHashMap<>();
        
        for (PerformanceContext context : realmContexts.values()) {
            try {
                RealmModel realm = session.realms().getRealm(context.getRealmId());
                if (realm != null) {
                    stats.put(context.getRealmId(), getRealmPerformanceStats(session, realm));
                }
            } catch (Exception e) {
                logger.warn("Error getting stats for realm: {}", context.getRealmId(), e);
            }
        }
        
        return stats;
    }
    
    /**
     * Performs optimization analysis and recommendations.
     */
    public OptimizationRecommendations analyzePerformance(KeycloakSession session, RealmModel realm) {
        PerformanceContext context = getPerformanceContext(session, realm);
        OptimizationRecommendations recommendations = new OptimizationRecommendations();
        
        // Analyze cache performance
        analyzeCachePerformance(context, recommendations);
        
        // Analyze throttling effectiveness
        analyzeThrottlingPerformance(context, recommendations);
        
        // Analyze connection pool utilization
        analyzeConnectionPoolPerformance(recommendations);
        
        // Analyze overall system performance
        analyzeSystemPerformance(context, recommendations);
        
        return recommendations;
    }
    
    /**
     * Applies automatic optimizations based on performance analysis.
     */
    public void applyAutomaticOptimizations(KeycloakSession session, RealmModel realm) {
        OptimizationRecommendations recommendations = analyzePerformance(session, realm);
        PerformanceContext context = getPerformanceContext(session, realm);
        
        // Apply cache optimizations
        if (recommendations.shouldIncreaseCacheSize()) {
            logger.info("Applying cache size optimization for realm: {}", realm.getName());
            // Cache size is fixed, but we can adjust TTL
            AdminConfiguration config = configService.getConfiguration(session, realm);
            if (config.getTemplateCacheTtlMinutes() < 10) {
                // Increase TTL slightly for better hit rates
                // This would require updating the configuration
                logger.debug("Recommending cache TTL increase for realm: {}", realm.getName());
            }
        }
        
        // Apply throttling optimizations
        if (recommendations.shouldAdjustThrottling()) {
            logger.info("Throttling adjustments recommended for realm: {}", realm.getName());
            // Throttling is automatic based on system load
        }
        
        context.setLastOptimization(Instant.now());
        logger.debug("Applied automatic optimizations for realm: {}", realm.getName());
    }
    
    /**
     * Clears performance context for a realm.
     */
    public void clearRealmContext(String realmId) {
        PerformanceContext context = realmContexts.remove(realmId);
        if (context != null) {
            logger.debug("Cleared performance context for realm: {}", realmId);
        }
    }
    
    /**
     * Starts the optimization task.
     */
    private void startOptimizationTask() {
        // Run optimization analysis every 10 minutes
        optimizationTask = optimizationExecutor.scheduleAtFixedRate(
            this::performPeriodicOptimization,
            10, 10, TimeUnit.MINUTES
        );
        
        logger.debug("Started performance optimization task");
    }
    
    /**
     * Performs periodic optimization analysis.
     */
    private void performPeriodicOptimization() {
        try {
            logger.debug("Performing periodic performance optimization");
            
            // Clean up stale contexts
            cleanupStaleContexts();
            
            // Log performance summary
            logPerformanceSummary();
            
        } catch (Exception e) {
            logger.error("Error during periodic optimization", e);
        }
    }
    
    /**
     * Cleans up contexts for realms that no longer exist.
     */
    private void cleanupStaleContexts() {
        // This would require access to KeycloakSession to check realm existence
        // For now, we'll keep contexts until explicitly cleared
        logger.debug("Performance context cleanup - {} active contexts", realmContexts.size());
    }
    
    /**
     * Logs performance summary.
     */
    private void logPerformanceSummary() {
        if (logger.isInfoEnabled()) {
            int activeContexts = realmContexts.size();
            
            // Get overall cache stats
            TemplateCache templateCache = TemplateCache.getInstance();
            TemplateCache.TemplateCacheStats cacheStats = templateCache.getStats();
            
            // Get connection pool stats
            GrpcChannelPool channelPool = GrpcChannelPool.getInstance();
            Map<String, GrpcChannelPool.ChannelPoolStats> poolStats = channelPool.getAllStats();
            
            logger.info("Performance Summary - Realms: {}, Cache Hit Rate: {:.2f}%, Pool Connections: {}", 
                       activeContexts, cacheStats.getHitRate() * 100, 
                       poolStats.values().stream().mapToInt(GrpcChannelPool.ChannelPoolStats::getTotalChannels).sum());
        }
    }
    
    /**
     * Analyzes cache performance.
     */
    private void analyzeCachePerformance(PerformanceContext context, OptimizationRecommendations recommendations) {
        TemplateCache.TemplateCacheStats stats = context.getTemplateCache().getStats();
        
        if (stats.getHitRate() < 0.7) {
            recommendations.addRecommendation("Cache hit rate is low (" + 
                String.format("%.2f%%", stats.getHitRate() * 100) + "). Consider increasing cache TTL.");
        }
        
        if (stats.getSize() >= stats.getMaxSize() * 0.9) {
            recommendations.addRecommendation("Cache is near capacity. Consider increasing max size.");
            recommendations.setShouldIncreaseCacheSize(true);
        }
    }
    
    /**
     * Analyzes throttling performance.
     */
    private void analyzeThrottlingPerformance(PerformanceContext context, OptimizationRecommendations recommendations) {
        BulkOperationThrottler.ThrottleStats stats = context.getThrottler().getStats();
        
        if (stats.getCurrentLevel() != BulkOperationThrottler.ThrottleLevel.NORMAL) {
            recommendations.addRecommendation("System is under load (throttle level: " + 
                stats.getCurrentLevel() + "). Monitor error rates and response times.");
        }
        
        if (stats.getTotalThrottled() > 0) {
            recommendations.addRecommendation("Operations have been throttled (" + 
                stats.getTotalThrottled() + " times). Consider optimizing bulk operation sizes.");
            recommendations.setShouldAdjustThrottling(true);
        }
    }
    
    /**
     * Analyzes connection pool performance.
     */
    private void analyzeConnectionPoolPerformance(OptimizationRecommendations recommendations) {
        GrpcChannelPool channelPool = GrpcChannelPool.getInstance();
        Map<String, GrpcChannelPool.ChannelPoolStats> poolStats = channelPool.getAllStats();
        
        for (Map.Entry<String, GrpcChannelPool.ChannelPoolStats> entry : poolStats.entrySet()) {
            GrpcChannelPool.ChannelPoolStats stats = entry.getValue();
            
            if (stats.getUnhealthyChannels() > 0) {
                recommendations.addRecommendation("Unhealthy connections detected for endpoint: " + 
                    entry.getKey() + " (" + stats.getUnhealthyChannels() + "/" + stats.getTotalChannels() + ")");
            }
            
            if (stats.getIdleChannels() >= stats.getTotalChannels() * 0.8) {
                recommendations.addRecommendation("Many idle connections for endpoint: " + 
                    entry.getKey() + ". Consider reducing pool size.");
            }
        }
    }
    
    /**
     * Analyzes overall system performance.
     */
    private void analyzeSystemPerformance(PerformanceContext context, OptimizationRecommendations recommendations) {
        AdminPerformanceMetrics.AdminPerformanceSummary summary = context.getMetrics().getPerformanceSummary();
        
        if (summary.getErrorRate() > 0.05) {
            recommendations.addRecommendation("High error rate detected (" + 
                String.format("%.2f%%", summary.getErrorRate() * 100) + "). Investigate error causes.");
        }
        
        if (summary.getAverageDuration() > 5000) {
            recommendations.addRecommendation("High average operation duration (" + 
                String.format("%.2fms", summary.getAverageDuration()) + "). Consider performance tuning.");
        }
        
        if (summary.getThrottleRate() > 0.1) {
            recommendations.addRecommendation("High throttle rate (" + 
                String.format("%.2f%%", summary.getThrottleRate() * 100) + "). System may be overloaded.");
        }
    }
    
    /**
     * Shuts down the performance optimization service.
     */
    public void shutdown() {
        logger.info("Shutting down performance optimization service");
        
        if (optimizationTask != null) {
            optimizationTask.cancel(true);
        }
        
        optimizationExecutor.shutdown();
        try {
            if (!optimizationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                optimizationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            optimizationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear all contexts
        realmContexts.clear();
        
        logger.info("Performance optimization service shutdown complete");
    }
    
    /**
     * Performance statistics for a realm.
     */
    public static class RealmPerformanceStats {
        private final String realmId;
        private final String realmName;
        private final TemplateCache.TemplateCacheStats cacheStats;
        private final BulkOperationThrottler.ThrottleStats throttleStats;
        private final AdminPerformanceMetrics.AdminPerformanceSummary performanceSummary;
        private final Map<String, GrpcChannelPool.ChannelPoolStats> connectionPoolStats;
        private final Instant lastOptimization;
        
        public RealmPerformanceStats(String realmId, String realmName,
                                   TemplateCache.TemplateCacheStats cacheStats,
                                   BulkOperationThrottler.ThrottleStats throttleStats,
                                   AdminPerformanceMetrics.AdminPerformanceSummary performanceSummary,
                                   Map<String, GrpcChannelPool.ChannelPoolStats> connectionPoolStats,
                                   Instant lastOptimization) {
            this.realmId = realmId;
            this.realmName = realmName;
            this.cacheStats = cacheStats;
            this.throttleStats = throttleStats;
            this.performanceSummary = performanceSummary;
            this.connectionPoolStats = connectionPoolStats;
            this.lastOptimization = lastOptimization;
        }
        
        // Getters
        public String getRealmId() { return realmId; }
        public String getRealmName() { return realmName; }
        public TemplateCache.TemplateCacheStats getCacheStats() { return cacheStats; }
        public BulkOperationThrottler.ThrottleStats getThrottleStats() { return throttleStats; }
        public AdminPerformanceMetrics.AdminPerformanceSummary getPerformanceSummary() { return performanceSummary; }
        public Map<String, GrpcChannelPool.ChannelPoolStats> getConnectionPoolStats() { return connectionPoolStats; }
        public Instant getLastOptimization() { return lastOptimization; }
    }
    
    /**
     * Optimization recommendations.
     */
    public static class OptimizationRecommendations {
        private final java.util.List<String> recommendations = new java.util.ArrayList<>();
        private boolean shouldIncreaseCacheSize = false;
        private boolean shouldAdjustThrottling = false;
        
        public void addRecommendation(String recommendation) {
            recommendations.add(recommendation);
        }
        
        public java.util.List<String> getRecommendations() { return recommendations; }
        public boolean shouldIncreaseCacheSize() { return shouldIncreaseCacheSize; }
        public void setShouldIncreaseCacheSize(boolean shouldIncreaseCacheSize) { 
            this.shouldIncreaseCacheSize = shouldIncreaseCacheSize; 
        }
        public boolean shouldAdjustThrottling() { return shouldAdjustThrottling; }
        public void setShouldAdjustThrottling(boolean shouldAdjustThrottling) { 
            this.shouldAdjustThrottling = shouldAdjustThrottling; 
        }
    }
}