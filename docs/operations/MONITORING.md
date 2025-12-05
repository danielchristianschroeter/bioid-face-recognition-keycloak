# Monitoring and Observability Guide

This guide covers the comprehensive monitoring and observability features of the Keycloak BioID Face Recognition Extension.

## Overview

The extension provides enterprise-grade monitoring capabilities including:

- **Comprehensive Metrics Collection** - MicroProfile Metrics integration with custom administrative metrics
- **Health Check Endpoints** - Service availability monitoring with detailed diagnostics
- **Structured Logging** - JSON-formatted logs with correlation IDs for audit and debugging
- **Distributed Tracing** - Complete trace management for complex administrative workflows
- **Real-time Alerting** - Configurable alerts for critical errors and performance degradation
- **Log Aggregation** - Automated log analysis with anomaly detection
- **Prometheus Integration** - Standard metrics export for external monitoring systems

## Metrics Collection

### Administrative Metrics

The extension collects comprehensive metrics for all administrative operations:

#### Enrollment Management Metrics
- `bioid.admin.enrollment.link.generations` - Number of enrollment links generated
- `bioid.admin.enrollment.link.generation.failures` - Failed enrollment link generations
- `bioid.admin.enrollment.deletions` - User enrollments deleted by administrators
- `bioid.admin.enrollment.deletion.failures` - Failed enrollment deletions

#### Template Management Metrics
- `bioid.admin.template.status.queries` - Template status queries performed
- `bioid.admin.template.status.batch.queries` - Batch template status queries
- `bioid.admin.template.upgrades` - Template upgrades performed
- `bioid.admin.template.upgrade.failures` - Failed template upgrades
- `bioid.admin.template.health.analyses` - Template health analyses performed
- `bioid.admin.template.cleanup.operations` - Template cleanup operations

#### Liveness Detection Metrics
- `bioid.admin.liveness.detection.attempts` - Total liveness detection attempts
- `bioid.admin.liveness.detection.successes` - Successful liveness detections
- `bioid.admin.liveness.detection.failures` - Failed liveness detections
- `bioid.admin.liveness.passive.attempts` - Passive liveness detection attempts
- `bioid.admin.liveness.active.attempts` - Active liveness detection attempts
- `bioid.admin.liveness.challenge.attempts` - Challenge-response liveness attempts

#### Bulk Operation Metrics
- `bioid.admin.bulk.enrollment.link.generations` - Bulk enrollment link generations
- `bioid.admin.bulk.template.deletions` - Bulk template deletions
- `bioid.admin.bulk.template.upgrades` - Bulk template upgrades
- `bioid.admin.bulk.operation.cancellations` - Bulk operation cancellations
- `bioid.admin.bulk.operation.timeouts` - Bulk operation timeouts

#### System Health Gauges
- `bioid.admin.sessions.active` - Number of active administrative sessions
- `bioid.admin.bulk.operations.active` - Number of active bulk operations
- `bioid.admin.users.enrolled.total` - Total number of enrolled users
- `bioid.admin.templates.upgrade.needed` - Number of templates needing upgrade
- `bioid.admin.liveness.score.average` - Average liveness detection score

### Core BioID Metrics

Standard BioID operation metrics are also collected:

- `bioid.enrollment.attempts/successes/failures` - Enrollment operation metrics
- `bioid.verification.attempts/successes/failures` - Verification operation metrics
- `bioid.template.deletions/status.requests/tag.updates` - Template operation metrics
- `bioid.errors.validation/authentication/service/network/template` - Error counters

### Performance Timers

All operations include timing metrics:

- `bioid.admin.enrollment.link.generation.duration` - Time to generate enrollment links
- `bioid.admin.template.upgrade.duration` - Time for template upgrade operations
- `bioid.admin.liveness.detection.duration` - Time for liveness detection operations
- `bioid.admin.bulk.operation.duration` - Time for bulk operations
- `bioid.admin.compliance.report.duration` - Time to generate compliance reports

## Health Check Endpoints

### Administrative Health Check

The extension provides comprehensive health monitoring through the `AdminHealthCheck` service:

**Endpoint:** `/admin/realms/{realm}/face-recognition/health`

**Components Monitored:**
- **Admin Service** - Administrative functionality availability
- **Template Service** - Template management service health
- **Liveness Service** - Liveness detection service status
- **Connection Pool** - Connection pool utilization and health
- **BioID Service** - External BioID service connectivity

**Health Status Levels:**
- `HEALTHY` - All components operational
- `DEGRADED` - Some components experiencing issues but core functionality available
- `UNHEALTHY` - Critical components failed, functionality impacted

**Example Health Response:**
```json
{
  "overallStatus": "HEALTHY",
  "checkDuration": "PT0.245S",
  "checkedAt": "2024-01-15T10:30:00Z",
  "summary": "Health Summary: 5 healthy, 0 degraded, 0 unhealthy components",
  "componentHealth": {
    "adminService": {
      "state": "HEALTHY",
      "statusMessage": "Admin service operational",
      "responseTimeMs": 45,
      "lastHealthCheck": "2024-01-15T10:30:00Z"
    },
    "templateService": {
      "state": "HEALTHY",
      "statusMessage": "Template service operational",
      "responseTimeMs": 32,
      "lastHealthCheck": "2024-01-15T10:30:00Z"
    }
  }
}
```

### Health Check Configuration

Health checks run automatically with configurable intervals:

```properties
# Health check intervals
health.check.interval=5m
health.check.timeout=30s

# Health thresholds
health.response.time.threshold=5s
health.error.rate.threshold=0.05
health.connection.utilization.threshold=0.8
```

## Structured Logging

### Log Event Structure

All administrative operations are logged using structured JSON format:

```json
{
  "timestamp": "2024-01-15T10:30:00.123Z",
  "level": "INFO",
  "component": "bioid-admin",
  "version": "1.0.0",
  "operation": "TEMPLATE_UPGRADE",
  "operationType": "TEMPLATE_MANAGEMENT",
  "userId": "user-123",
  "adminUserId": "admin-456",
  "correlationId": "trace-abc-123",
  "duration": 1250,
  "success": true,
  "details": {
    "templateCount": 5,
    "upgradeVersion": "2.1.0"
  },
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0..."
}
```

### Operation Types

Logs are categorized by operation type:

- `ENROLLMENT_MANAGEMENT` - User enrollment operations
- `TEMPLATE_MANAGEMENT` - Template lifecycle operations
- `LIVENESS_DETECTION` - Liveness detection operations
- `BULK_OPERATION` - Bulk administrative operations
- `SECURITY` - Security-related events
- `COMPLIANCE` - Compliance and audit events
- `PERFORMANCE` - Performance monitoring events
- `ERROR` - Error and exception events

### Correlation IDs

All related operations share correlation IDs for tracing:

```java
// Generate correlation ID
String correlationId = StructuredLogger.createCorrelationId();

// Set in MDC for thread-local access
StructuredLogger.setCorrelationId(correlationId);

// All subsequent logs will include this correlation ID
structuredLogger.logTemplateOperation("TEMPLATE_UPGRADE", userId, adminUserId, 
    true, duration, correlationId, details);
```

## Distributed Tracing

### Trace Management

The extension provides comprehensive distributed tracing for complex workflows:

```java
// Start a trace for a bulk operation
TraceContext context = DistributedTracing.startTrace(
    "BULK_TEMPLATE_UPGRADE", "BULK_OPERATION", null, adminUserId);

try {
    // Create spans for sub-operations
    String spanId = DistributedTracing.startSpan("VALIDATE_TEMPLATES", 
        Map.of("templateCount", "100"));
    
    // Perform validation
    validateTemplates();
    
    DistributedTracing.finishSpan(true, null, 
        Map.of("validatedCount", "95"));
    
    // Continue with upgrade process
    String upgradeSpanId = DistributedTracing.startSpan("UPGRADE_TEMPLATES", null);
    
    upgradeTemplates();
    
    DistributedTracing.finishSpan(true, null, null);
    
    DistributedTracing.finishTrace(true, "Bulk upgrade completed successfully");
    
} catch (Exception e) {
    DistributedTracing.finishTrace(false, "Bulk upgrade failed: " + e.getMessage());
    throw e;
}
```

### Trace Context Information

Each trace includes comprehensive context:

- **Trace ID** - Unique identifier for the entire operation
- **Span Hierarchy** - Parent-child relationships between operations
- **Timing Information** - Start/end times and durations
- **Success/Failure Status** - Operation outcomes
- **Custom Tags** - Operation-specific metadata
- **Events** - Point-in-time occurrences during execution

### Trace Analysis

Traces provide detailed analysis capabilities:

```java
// Get trace statistics
Map<String, Object> stats = DistributedTracing.getTraceStatistics();

// Analyze trace performance
TraceContext context = DistributedTracing.getCurrentTraceContext();
Duration totalDuration = context.getDuration();
int spanCount = context.getSpans().size();
long failedSpans = context.getFailedSpanCount();
int maxDepth = context.getMaxSpanDepth();
```

## Alerting System

### Alert Types and Severities

The alerting system monitors for various conditions:

**Alert Types:**
- `SYSTEM_UNHEALTHY` - System health check failures
- `COMPONENT_FAILURE` - Individual component failures
- `LOW_SUCCESS_RATE` - Operation success rates below thresholds
- `HIGH_ERROR_RATE` - Error rates above acceptable levels
- `BULK_OPERATION_FAILURE` - Bulk operation failures
- `SECURITY_POLICY_VIOLATION` - Security policy violations

**Severity Levels:**
- `CRITICAL` - Immediate attention required, system severely impacted
- `HIGH` - Urgent attention required, major functionality impacted
- `WARNING` - Attention required, functionality may be impacted
- `MEDIUM` - Minor issues detected
- `LOW` - Informational, no immediate impact

### Alert Configuration

Configure alert thresholds and behavior:

```properties
# Alert thresholds
alert.error.rate.threshold=0.05
alert.liveness.success.rate.threshold=0.95
alert.response.time.threshold=5s
alert.failed.operations.threshold=10

# Alert suppression (prevent spam)
alert.suppression.duration=15m

# Monitoring intervals
alert.health.check.interval=2m
alert.metrics.check.interval=1m
```

### Alert Processing

Alerts are processed through multiple channels:

1. **Structured Logging** - All alerts logged with full context
2. **Application Logs** - Severity-appropriate log levels
3. **Metrics Export** - Alert counts exported to monitoring systems
4. **External Integration** - Webhook support for external systems

Example alert processing:

```java
// Configure alerting service
AlertingService alertingService = new AlertingService(
    adminMetrics, healthCheck, structuredLogger);

// Start monitoring
alertingService.start();

// Manual alert triggering
alertingService.triggerAlert(
    AlertType.HIGH_ERROR_RATE, 
    AlertSeverity.WARNING,
    "Liveness detection error rate exceeded threshold",
    Map.of("currentRate", "7.2%", "threshold", "5.0%")
);
```

## Log Aggregation and Analysis

### Automated Log Analysis

The log aggregation service provides automated analysis:

```java
// Configure log aggregation
LogAggregationService logService = new LogAggregationService();
logService.start();

// Add log events
logService.addLogEvent(adminLogEvent);

// Get aggregation statistics
Map<String, Object> stats = logService.getLogStatistics();

// Search logs
LogSearchCriteria criteria = LogSearchCriteria.builder()
    .startTime(Instant.now().minus(Duration.ofHours(1)))
    .operation("TEMPLATE_UPGRADE")
    .successOnly(false)
    .build();

List<AdminLogEvent> failedUpgrades = logService.searchLogs(criteria);
```

### Anomaly Detection

The system automatically detects anomalies:

- **High Error Rates** - Error rates above 10% trigger warnings
- **Operation-Specific Issues** - Per-operation error rate monitoring
- **Unusual User Activity** - High activity patterns detection
- **Performance Degradation** - Response time anomalies

### Aggregation Reports

Regular aggregation reports provide insights:

```java
// Generate aggregation report
LogAggregationReport report = logService.createAggregationReport();

// Report contents
int totalEvents = report.getTotalEvents();
double successRate = report.getSuccessRate();
Map<String, OperationSummary> operationSummaries = report.getOperationSummaries();
Map<String, Long> userActivity = report.getUserActivity();
```

## Prometheus Integration

### Metrics Export

The extension provides Prometheus-compatible metrics export:

**Endpoint:** `/admin/realms/{realm}/face-recognition/metrics`

**Export Formats:**
- **Standard Format** - All metrics in Prometheus format
- **Filtered Export** - Include/exclude patterns supported
- **Custom Labels** - Additional labels for metric categorization

### Prometheus Configuration

Configure Prometheus to scrape metrics:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'keycloak-bioid-admin'
    static_configs:
      - targets: ['keycloak:8080']
    metrics_path: '/admin/realms/master/face-recognition/metrics'
    scrape_interval: 30s
    basic_auth:
      username: 'monitoring-user'
      password: 'monitoring-password'
```

### Key Metrics for Dashboards

Essential metrics for monitoring dashboards:

```promql
# Success rates
rate(bioid_admin_liveness_detection_successes_total[5m]) / 
rate(bioid_admin_liveness_detection_attempts_total[5m])

# Error rates
rate(bioid_admin_template_upgrade_failures_total[5m]) / 
rate(bioid_admin_template_upgrades_total[5m])

# Response times
histogram_quantile(0.95, bioid_admin_template_upgrade_duration_seconds_bucket)

# System health
bioid_admin_health_overall

# Active operations
bioid_admin_bulk_operations_active
```

## Monitoring Best Practices

### Dashboard Design

Create comprehensive monitoring dashboards:

1. **Overview Dashboard**
   - System health status
   - Key performance indicators
   - Error rate trends
   - Active operations count

2. **Operations Dashboard**
   - Operation success rates
   - Response time percentiles
   - Throughput metrics
   - Error breakdowns

3. **Administrative Dashboard**
   - Admin session activity
   - Bulk operation status
   - Template management metrics
   - Compliance report generation

### Alert Configuration

Configure meaningful alerts:

```yaml
# Grafana alert rules
groups:
  - name: bioid-admin-alerts
    rules:
      - alert: HighErrorRate
        expr: rate(bioid_admin_liveness_detection_failures_total[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High liveness detection error rate"
          description: "Error rate is {{ $value | humanizePercentage }}"

      - alert: SystemUnhealthy
        expr: bioid_admin_health_overall < 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "BioID admin system unhealthy"
          description: "System health check failed"
```

### Log Management

Implement effective log management:

1. **Log Retention** - Configure appropriate retention periods
2. **Log Rotation** - Prevent disk space issues
3. **Log Shipping** - Send logs to centralized systems
4. **Log Analysis** - Regular review of error patterns

### Performance Monitoring

Monitor key performance indicators:

- **Response Times** - Track P50, P95, P99 percentiles
- **Throughput** - Operations per second/minute
- **Resource Usage** - Memory, CPU, connection pool utilization
- **Error Rates** - Overall and per-operation error rates

## Troubleshooting with Monitoring Data

### Using Correlation IDs

Track issues across components:

```bash
# Find all logs for a specific operation
grep "correlation-id-123" /var/log/keycloak/keycloak.log

# Search in log aggregation system
curl -X POST "http://elasticsearch:9200/keycloak-logs/_search" \
  -H "Content-Type: application/json" \
  -d '{"query": {"term": {"correlationId": "correlation-id-123"}}}'
```

### Trace Analysis

Analyze distributed traces:

```java
// Get trace by ID
TraceContext trace = DistributedTracing.getTraceById("trace-id-456");

// Analyze performance
Duration totalDuration = trace.getDuration();
List<TraceSpan> slowSpans = trace.getSpans().stream()
    .filter(span -> span.getDuration().toMillis() > 1000)
    .collect(Collectors.toList());

// Find failed operations
List<TraceSpan> failedSpans = trace.getSpans().stream()
    .filter(span -> !span.isSuccess())
    .collect(Collectors.toList());
```

### Metrics-Based Debugging

Use metrics to identify issues:

```promql
# Find operations with high failure rates
topk(5, rate(bioid_admin_operation_failures_total[5m]) by (operation))

# Identify slow operations
topk(5, histogram_quantile(0.95, bioid_admin_operation_duration_seconds_bucket) by (operation))

# Monitor resource usage
bioid_admin_memory_used_bytes / bioid_admin_memory_total_bytes
```

## Integration with External Systems

### ELK Stack Integration

Configure log shipping to Elasticsearch:

```yaml
# filebeat.yml
filebeat.inputs:
- type: log
  paths:
    - /var/log/keycloak/keycloak.log
  fields:
    service: keycloak-bioid-admin
    environment: production
  json.keys_under_root: true
  json.add_error_key: true

output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "keycloak-bioid-admin-%{+yyyy.MM.dd}"
```

### Grafana Dashboard

Import pre-built dashboards:

```json
{
  "dashboard": {
    "title": "BioID Admin Monitoring",
    "panels": [
      {
        "title": "System Health",
        "type": "stat",
        "targets": [
          {
            "expr": "bioid_admin_health_overall",
            "legendFormat": "Health Status"
          }
        ]
      },
      {
        "title": "Operation Success Rates",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(bioid_admin_liveness_detection_successes_total[5m]) / rate(bioid_admin_liveness_detection_attempts_total[5m])",
            "legendFormat": "Liveness Detection"
          }
        ]
      }
    ]
  }
}
```

### SIEM Integration

Forward security events to SIEM systems:

```java
// Configure security event forwarding
structuredLogger.logSecurityEvent(
    "UNAUTHORIZED_ACCESS_ATTEMPT",
    userId, adminUserId, ipAddress, userAgent,
    correlationId,
    Map.of(
        "attemptedOperation", "BULK_DELETE",
        "deniedReason", "INSUFFICIENT_PRIVILEGES"
    )
);
```

This comprehensive monitoring and observability system provides complete visibility into the BioID Face Recognition Extension's operation, enabling proactive issue detection, performance optimization, and compliance reporting.