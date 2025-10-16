package com.bioid.keycloak.client.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prometheus-compatible metrics exporter for BioID administrative operations.
 * Provides standardized metrics export in Prometheus format for external monitoring systems.
 */
public class PrometheusMetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(PrometheusMetricsExporter.class);
    
    private final MeterRegistry meterRegistry;
    private final AdminMetrics adminMetrics;
    private final BioIdMetrics bioIdMetrics;
    
    public PrometheusMetricsExporter(MeterRegistry meterRegistry, 
                                   AdminMetrics adminMetrics, 
                                   BioIdMetrics bioIdMetrics) {
        this.meterRegistry = meterRegistry;
        this.adminMetrics = adminMetrics;
        this.bioIdMetrics = bioIdMetrics;
    }
    
    /**
     * Exports all metrics in Prometheus format.
     *
     * @return Prometheus-formatted metrics string
     */
    public String exportMetrics() {
        try {
            StringWriter writer = new StringWriter();
            
            // Export standard Micrometer metrics
            exportMicrometerMetrics(writer);
            
            // Export custom administrative metrics
            exportAdministrativeMetrics(writer);
            
            // Export health and performance metrics
            exportHealthMetrics(writer);
            
            // Add metadata
            writer.write(String.format("# Generated at %s\n", Instant.now()));
            writer.write("# BioID Administrative Metrics Export\n");
            
            return writer.toString();
            
        } catch (Exception e) {
            logger.error("Failed to export Prometheus metrics", e);
            return generateErrorMetrics(e);
        }
    }
    
    /**
     * Exports core Micrometer metrics in Prometheus format.
     */
    private void exportMicrometerMetrics(StringWriter writer) {
        // Export counter metrics
        meterRegistry.getMeters().stream()
            .filter(meter -> meter.getId().getName().startsWith("bioid."))
            .forEach(meter -> {
                String metricName = sanitizeMetricName(meter.getId().getName());
                String help = meter.getId().getDescription() != null ? 
                    meter.getId().getDescription() : "BioID metric";
                
                writer.write(String.format("# HELP %s %s\n", metricName, help));
                writer.write(String.format("# TYPE %s %s\n", metricName, getPrometheusType(meter)));
                
                meter.measure().forEach(measurement -> {
                    Map<String, String> labels = new HashMap<>();
                    meter.getId().getTags().forEach(tag -> labels.put(tag.getKey(), tag.getValue()));
                    
                    String labelString = labels.entrySet().stream()
                        .map(entry -> String.format("%s=\"%s\"", entry.getKey(), entry.getValue()))
                        .collect(Collectors.joining(","));
                    
                    if (!labelString.isEmpty()) {
                        labelString = "{" + labelString + "}";
                    }
                    
                    writer.write(String.format("%s%s %f\n", 
                        metricName, labelString, measurement.getValue()));
                });
                
                writer.write("\n");
            });
    }
    
    /**
     * Exports administrative-specific metrics.
     */
    private void exportAdministrativeMetrics(StringWriter writer) {
        // Liveness detection success rate
        double livenessSuccessRate = adminMetrics.getLivenessDetectionSuccessRate();
        writer.write("# HELP bioid_admin_liveness_success_rate Liveness detection success rate\n");
        writer.write("# TYPE bioid_admin_liveness_success_rate gauge\n");
        writer.write(String.format("bioid_admin_liveness_success_rate %f\n", livenessSuccessRate));
        writer.write("\n");
        
        // Template upgrade success rate
        double templateUpgradeSuccessRate = adminMetrics.getTemplateUpgradeSuccessRate();
        writer.write("# HELP bioid_admin_template_upgrade_success_rate Template upgrade success rate\n");
        writer.write("# TYPE bioid_admin_template_upgrade_success_rate gauge\n");
        writer.write(String.format("bioid_admin_template_upgrade_success_rate %f\n", templateUpgradeSuccessRate));
        writer.write("\n");
        
        // System information
        writer.write("# HELP bioid_admin_system_info System information\n");
        writer.write("# TYPE bioid_admin_system_info gauge\n");
        writer.write(String.format("bioid_admin_system_info{version=\"1.0.0\",component=\"admin\"} 1\n"));
        writer.write("\n");
    }
    
    /**
     * Exports health and performance metrics.
     */
    private void exportHealthMetrics(StringWriter writer) {
        // Export uptime metric
        long uptimeSeconds = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        writer.write("# HELP bioid_admin_uptime_seconds System uptime in seconds\n");
        writer.write("# TYPE bioid_admin_uptime_seconds counter\n");
        writer.write(String.format("bioid_admin_uptime_seconds %d\n", uptimeSeconds));
        writer.write("\n");
        
        // Export memory usage
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        writer.write("# HELP bioid_admin_memory_used_bytes Used memory in bytes\n");
        writer.write("# TYPE bioid_admin_memory_used_bytes gauge\n");
        writer.write(String.format("bioid_admin_memory_used_bytes %d\n", usedMemory));
        writer.write("\n");
        
        writer.write("# HELP bioid_admin_memory_total_bytes Total memory in bytes\n");
        writer.write("# TYPE bioid_admin_memory_total_bytes gauge\n");
        writer.write(String.format("bioid_admin_memory_total_bytes %d\n", totalMemory));
        writer.write("\n");
        
        // Export thread count
        int threadCount = Thread.activeCount();
        writer.write("# HELP bioid_admin_threads_active Active thread count\n");
        writer.write("# TYPE bioid_admin_threads_active gauge\n");
        writer.write(String.format("bioid_admin_threads_active %d\n", threadCount));
        writer.write("\n");
    }
    
    /**
     * Sanitizes metric names for Prometheus compatibility.
     */
    private String sanitizeMetricName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_:]", "_");
    }
    
    /**
     * Determines the Prometheus metric type based on Micrometer meter type.
     */
    private String getPrometheusType(io.micrometer.core.instrument.Meter meter) {
        if (meter instanceof io.micrometer.core.instrument.Counter) {
            return "counter";
        } else if (meter instanceof io.micrometer.core.instrument.Timer) {
            return "histogram";
        } else if (meter instanceof io.micrometer.core.instrument.Gauge) {
            return "gauge";
        } else {
            return "untyped";
        }
    }
    
    /**
     * Generates error metrics when export fails.
     */
    private String generateErrorMetrics(Exception e) {
        StringWriter writer = new StringWriter();
        writer.write("# HELP bioid_admin_metrics_export_errors Metrics export error count\n");
        writer.write("# TYPE bioid_admin_metrics_export_errors counter\n");
        writer.write("bioid_admin_metrics_export_errors 1\n");
        writer.write("\n");
        
        writer.write("# HELP bioid_admin_metrics_export_last_error Last metrics export error\n");
        writer.write("# TYPE bioid_admin_metrics_export_last_error gauge\n");
        writer.write(String.format("bioid_admin_metrics_export_last_error{error=\"%s\"} %d\n", 
            e.getMessage().replaceAll("\"", "'"), System.currentTimeMillis() / 1000));
        writer.write("\n");
        
        return writer.toString();
    }
    
    /**
     * Exports metrics with custom labels and filtering.
     *
     * @param includePatterns patterns to include (null for all)
     * @param excludePatterns patterns to exclude
     * @param customLabels additional labels to add
     * @return filtered Prometheus metrics
     */
    public String exportMetrics(String[] includePatterns, 
                              String[] excludePatterns, 
                              Map<String, String> customLabels) {
        try {
            StringWriter writer = new StringWriter();
            
            // Add custom labels to all metrics
            String globalLabels = "";
            if (customLabels != null && !customLabels.isEmpty()) {
                globalLabels = customLabels.entrySet().stream()
                    .map(entry -> String.format("%s=\"%s\"", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining(","));
            }
            
            // Export filtered metrics
            meterRegistry.getMeters().stream()
                .filter(meter -> shouldIncludeMetric(meter.getId().getName(), includePatterns, excludePatterns))
                .forEach(meter -> {
                    String metricName = sanitizeMetricName(meter.getId().getName());
                    String help = meter.getId().getDescription() != null ? 
                        meter.getId().getDescription() : "BioID metric";
                    
                    writer.write(String.format("# HELP %s %s\n", metricName, help));
                    writer.write(String.format("# TYPE %s %s\n", metricName, getPrometheusType(meter)));
                    
                    meter.measure().forEach(measurement -> {
                        Map<String, String> labels = new HashMap<>();
                        meter.getId().getTags().forEach(tag -> labels.put(tag.getKey(), tag.getValue()));
                        
                        // Add custom labels
                        if (customLabels != null) {
                            labels.putAll(customLabels);
                        }
                        
                        String labelString = labels.entrySet().stream()
                            .map(entry -> String.format("%s=\"%s\"", entry.getKey(), entry.getValue()))
                            .collect(Collectors.joining(","));
                        
                        if (!labelString.isEmpty()) {
                            labelString = "{" + labelString + "}";
                        }
                        
                        writer.write(String.format("%s%s %f\n", 
                            metricName, labelString, measurement.getValue()));
                    });
                    
                    writer.write("\n");
                });
            
            return writer.toString();
            
        } catch (Exception e) {
            logger.error("Failed to export filtered Prometheus metrics", e);
            return generateErrorMetrics(e);
        }
    }
    
    /**
     * Determines if a metric should be included based on patterns.
     */
    private boolean shouldIncludeMetric(String metricName, String[] includePatterns, String[] excludePatterns) {
        // Check exclude patterns first
        if (excludePatterns != null) {
            for (String pattern : excludePatterns) {
                if (metricName.matches(pattern)) {
                    return false;
                }
            }
        }
        
        // Check include patterns
        if (includePatterns != null && includePatterns.length > 0) {
            for (String pattern : includePatterns) {
                if (metricName.matches(pattern)) {
                    return true;
                }
            }
            return false; // No include pattern matched
        }
        
        return true; // No include patterns specified, include by default
    }
    
    /**
     * Exports metrics summary for dashboard display.
     *
     * @return summary metrics in key-value format
     */
    public Map<String, Object> exportMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            // Administrative metrics summary
            summary.put("liveness_detection_success_rate", adminMetrics.getLivenessDetectionSuccessRate());
            summary.put("template_upgrade_success_rate", adminMetrics.getTemplateUpgradeSuccessRate());
            
            // System metrics summary
            Runtime runtime = Runtime.getRuntime();
            summary.put("memory_usage_percent", 
                (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.totalMemory() * 100);
            summary.put("uptime_seconds", 
                java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
            summary.put("active_threads", Thread.activeCount());
            
            // Metrics collection timestamp
            summary.put("collected_at", Instant.now().toString());
            
        } catch (Exception e) {
            logger.error("Failed to generate metrics summary", e);
            summary.put("error", e.getMessage());
        }
        
        return summary;
    }
}