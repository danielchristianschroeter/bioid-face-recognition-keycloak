package com.bioid.keycloak.performance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Performance monitoring utility for tracking latency, throughput, and resource utilization. */
public class PerformanceMonitor {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitor.class);

  private final Map<String, OperationMetrics> operationMetrics;
  private final AtomicLong startTime;

  private static volatile PerformanceMonitor instance;

  /** Metrics for a specific operation. */
  private static class OperationMetrics {
    private final LongAdder totalCalls;
    private final LongAdder totalDuration;
    private final AtomicLong minDuration;
    private final AtomicLong maxDuration;
    private final LongAdder errorCount;
    private final AtomicLong lastCallTime;

    public OperationMetrics() {
      this.totalCalls = new LongAdder();
      this.totalDuration = new LongAdder();
      this.minDuration = new AtomicLong(Long.MAX_VALUE);
      this.maxDuration = new AtomicLong(0);
      this.errorCount = new LongAdder();
      this.lastCallTime = new AtomicLong(0);
    }

    public void recordCall(long durationMs, boolean isError) {
      totalCalls.increment();
      totalDuration.add(durationMs);
      lastCallTime.set(System.currentTimeMillis());

      if (isError) {
        errorCount.increment();
      }

      // Update min/max duration
      minDuration.updateAndGet(current -> Math.min(current, durationMs));
      maxDuration.updateAndGet(current -> Math.max(current, durationMs));
    }

    public OperationStats getStats() {
      long calls = totalCalls.sum();
      long duration = totalDuration.sum();
      long errors = errorCount.sum();

      double avgDuration = calls > 0 ? (double) duration / calls : 0.0;
      double errorRate = calls > 0 ? (double) errors / calls : 0.0;
      long min = minDuration.get() == Long.MAX_VALUE ? 0 : minDuration.get();
      long max = maxDuration.get();

      return new OperationStats(
          calls, avgDuration, min, max, errors, errorRate, lastCallTime.get());
    }
  }

  private PerformanceMonitor() {
    this.operationMetrics = new ConcurrentHashMap<>();
    this.startTime = new AtomicLong(System.currentTimeMillis());
    logger.info("Performance monitor initialized");
  }

  /** Get singleton instance. */
  public static PerformanceMonitor getInstance() {
    if (instance == null) {
      synchronized (PerformanceMonitor.class) {
        if (instance == null) {
          instance = new PerformanceMonitor();
        }
      }
    }
    return instance;
  }

  /** Start timing an operation. */
  public TimingContext startTiming(String operationName) {
    return new TimingContext(operationName);
  }

  /** Record a completed operation. */
  public void recordOperation(String operationName, long durationMs, boolean isError) {
    OperationMetrics metrics =
        operationMetrics.computeIfAbsent(operationName, k -> new OperationMetrics());
    metrics.recordCall(durationMs, isError);

    if (logger.isDebugEnabled()) {
      logger.debug(
          String.format(
              "Operation %s completed in %dms (error: %s)", operationName, durationMs, isError));
    }
  }

  /** Get statistics for a specific operation. */
  public OperationStats getOperationStats(String operationName) {
    OperationMetrics metrics = operationMetrics.get(operationName);
    return metrics != null ? metrics.getStats() : new OperationStats(0, 0, 0, 0, 0, 0, 0);
  }

  /** Get statistics for all operations. */
  public Map<String, OperationStats> getAllOperationStats() {
    Map<String, OperationStats> stats = new ConcurrentHashMap<>();
    for (Map.Entry<String, OperationMetrics> entry : operationMetrics.entrySet()) {
      stats.put(entry.getKey(), entry.getValue().getStats());
    }
    return stats;
  }

  /** Get overall system performance summary. */
  public SystemPerformanceStats getSystemStats() {
    long totalCalls = 0;
    long totalErrors = 0;
    double totalDuration = 0;
    long maxDuration = 0;
    long minDuration = Long.MAX_VALUE;

    for (OperationMetrics metrics : operationMetrics.values()) {
      OperationStats stats = metrics.getStats();
      totalCalls += stats.getTotalCalls();
      totalErrors += stats.getErrorCount();
      totalDuration += stats.getAverageDuration() * stats.getTotalCalls();
      maxDuration = Math.max(maxDuration, stats.getMaxDuration());
      minDuration = Math.min(minDuration, stats.getMinDuration());
    }

    double avgDuration = totalCalls > 0 ? totalDuration / totalCalls : 0.0;
    double errorRate = totalCalls > 0 ? (double) totalErrors / totalCalls : 0.0;
    long uptime = System.currentTimeMillis() - startTime.get();
    double throughput = uptime > 0 ? (double) totalCalls / (uptime / 1000.0) : 0.0;

    if (minDuration == Long.MAX_VALUE) {
      minDuration = 0;
    }

    return new SystemPerformanceStats(
        totalCalls,
        avgDuration,
        minDuration,
        maxDuration,
        totalErrors,
        errorRate,
        throughput,
        uptime);
  }

  /** Reset all metrics. */
  public void reset() {
    operationMetrics.clear();
    startTime.set(System.currentTimeMillis());
    logger.info("Performance metrics reset");
  }

  /** Get performance report as formatted string. */
  public String getPerformanceReport() {
    StringBuilder report = new StringBuilder();
    report.append("=== Performance Report ===\n");

    SystemPerformanceStats systemStats = getSystemStats();
    report.append(String.format("System Uptime: %s\n", formatDuration(systemStats.getUptime())));
    report.append(String.format("Total Calls: %d\n", systemStats.getTotalCalls()));
    report.append(String.format("Average Duration: %.2fms\n", systemStats.getAverageDuration()));
    report.append(String.format("Throughput: %.2f calls/sec\n", systemStats.getThroughput()));
    report.append(String.format("Error Rate: %.2f%%\n", systemStats.getErrorRate() * 100));
    report.append("\n=== Operation Details ===\n");

    for (Map.Entry<String, OperationStats> entry : getAllOperationStats().entrySet()) {
      OperationStats stats = entry.getValue();
      report.append(String.format("%s:\n", entry.getKey()));
      report.append(String.format("  Calls: %d\n", stats.getTotalCalls()));
      report.append(String.format("  Avg Duration: %.2fms\n", stats.getAverageDuration()));
      report.append(
          String.format("  Min/Max: %dms/%dms\n", stats.getMinDuration(), stats.getMaxDuration()));
      report.append(
          String.format(
              "  Errors: %d (%.2f%%)\n", stats.getErrorCount(), stats.getErrorRate() * 100));
      report.append(String.format("  Last Call: %s\n", formatTimestamp(stats.getLastCallTime())));
      report.append("\n");
    }

    return report.toString();
  }

  private String formatDuration(long millis) {
    long seconds = millis / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;

    if (hours > 0) {
      return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
    } else if (minutes > 0) {
      return String.format("%dm %ds", minutes, seconds % 60);
    } else {
      return String.format("%ds", seconds);
    }
  }

  private String formatTimestamp(long timestamp) {
    if (timestamp == 0) {
      return "Never";
    }
    long ago = System.currentTimeMillis() - timestamp;
    return formatDuration(ago) + " ago";
  }

  /** Timing context for measuring operation duration. */
  public class TimingContext implements AutoCloseable {
    private final String operationName;
    private final long startTime;
    private boolean isError = false;

    public TimingContext(String operationName) {
      this.operationName = operationName;
      this.startTime = System.currentTimeMillis();
    }

    public void markError() {
      this.isError = true;
    }

    @Override
    public void close() {
      long duration = System.currentTimeMillis() - startTime;
      recordOperation(operationName, duration, isError);
    }
  }

  /** Statistics for a specific operation. */
  public static class OperationStats {
    private final long totalCalls;
    private final double averageDuration;
    private final long minDuration;
    private final long maxDuration;
    private final long errorCount;
    private final double errorRate;
    private final long lastCallTime;

    public OperationStats(
        long totalCalls,
        double averageDuration,
        long minDuration,
        long maxDuration,
        long errorCount,
        double errorRate,
        long lastCallTime) {
      this.totalCalls = totalCalls;
      this.averageDuration = averageDuration;
      this.minDuration = minDuration;
      this.maxDuration = maxDuration;
      this.errorCount = errorCount;
      this.errorRate = errorRate;
      this.lastCallTime = lastCallTime;
    }

    public long getTotalCalls() {
      return totalCalls;
    }

    public double getAverageDuration() {
      return averageDuration;
    }

    public long getMinDuration() {
      return minDuration;
    }

    public long getMaxDuration() {
      return maxDuration;
    }

    public long getErrorCount() {
      return errorCount;
    }

    public double getErrorRate() {
      return errorRate;
    }

    public long getLastCallTime() {
      return lastCallTime;
    }

    @Override
    public String toString() {
      return String.format(
          "OperationStats{calls=%d, avgDuration=%.2fms, errors=%d, errorRate=%.2f%%}",
          totalCalls, averageDuration, errorCount, errorRate * 100);
    }
  }

  /** Overall system performance statistics. */
  public static class SystemPerformanceStats {
    private final long totalCalls;
    private final double averageDuration;
    private final long minDuration;
    private final long maxDuration;
    private final long errorCount;
    private final double errorRate;
    private final double throughput;
    private final long uptime;

    public SystemPerformanceStats(
        long totalCalls,
        double averageDuration,
        long minDuration,
        long maxDuration,
        long errorCount,
        double errorRate,
        double throughput,
        long uptime) {
      this.totalCalls = totalCalls;
      this.averageDuration = averageDuration;
      this.minDuration = minDuration;
      this.maxDuration = maxDuration;
      this.errorCount = errorCount;
      this.errorRate = errorRate;
      this.throughput = throughput;
      this.uptime = uptime;
    }

    public long getTotalCalls() {
      return totalCalls;
    }

    public double getAverageDuration() {
      return averageDuration;
    }

    public long getMinDuration() {
      return minDuration;
    }

    public long getMaxDuration() {
      return maxDuration;
    }

    public long getErrorCount() {
      return errorCount;
    }

    public double getErrorRate() {
      return errorRate;
    }

    public double getThroughput() {
      return throughput;
    }

    public long getUptime() {
      return uptime;
    }

    @Override
    public String toString() {
      return String.format(
          "SystemStats{calls=%d, avgDuration=%.2fms, throughput=%.2f/sec, errorRate=%.2f%%}",
          totalCalls, averageDuration, throughput, errorRate * 100);
    }
  }
}
