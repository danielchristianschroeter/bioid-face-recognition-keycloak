package com.bioid.keycloak.debug;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for debugging and development support.
 *
 * <p>This class provides various debugging utilities including performance timing, memory
 * monitoring, and diagnostic information collection.
 *
 * @since 1.0.0
 */
public final class DebugUtils {

  private static final Logger logger = LoggerFactory.getLogger(DebugUtils.class);

  private static final boolean DEBUG_ENABLED =
      Boolean.parseBoolean(System.getProperty("bioid.debug.enabled", "false"));

  private static final Map<String, Long> timers = new ConcurrentHashMap<>();
  private static final Map<String, Integer> counters = new ConcurrentHashMap<>();

  private DebugUtils() {
    // Utility class
  }

  /**
   * Starts a performance timer with the given name.
   *
   * @param timerName the name of the timer
   */
  public static void startTimer(String timerName) {
    if (DEBUG_ENABLED) {
      timers.put(timerName, System.currentTimeMillis());
      logger.debug("Timer started: {}", timerName);
    }
  }

  /**
   * Stops a performance timer and logs the elapsed time.
   *
   * @param timerName the name of the timer
   * @return the elapsed time in milliseconds, or -1 if timer not found
   */
  public static long stopTimer(String timerName) {
    if (!DEBUG_ENABLED) {
      return -1;
    }

    Long startTime = timers.remove(timerName);
    if (startTime == null) {
      logger.warn("Timer not found: {}", timerName);
      return -1;
    }

    long elapsed = System.currentTimeMillis() - startTime;
    logger.debug("Timer stopped: {} - {}ms", timerName, elapsed);
    return elapsed;
  }

  /**
   * Logs the elapsed time for a timer without stopping it.
   *
   * @param timerName the name of the timer
   * @return the elapsed time in milliseconds, or -1 if timer not found
   */
  public static long checkTimer(String timerName) {
    if (!DEBUG_ENABLED) {
      return -1;
    }

    Long startTime = timers.get(timerName);
    if (startTime == null) {
      logger.warn("Timer not found: {}", timerName);
      return -1;
    }

    long elapsed = System.currentTimeMillis() - startTime;
    logger.debug("Timer check: {} - {}ms", timerName, elapsed);
    return elapsed;
  }

  /**
   * Increments a debug counter.
   *
   * @param counterName the name of the counter
   * @return the new counter value
   */
  public static int incrementCounter(String counterName) {
    if (!DEBUG_ENABLED) {
      return 0;
    }

    int newValue = counters.merge(counterName, 1, Integer::sum);
    logger.debug("Counter incremented: {} = {}", counterName, newValue);
    return newValue;
  }

  /**
   * Gets the current value of a debug counter.
   *
   * @param counterName the name of the counter
   * @return the counter value, or 0 if not found
   */
  public static int getCounter(String counterName) {
    return counters.getOrDefault(counterName, 0);
  }

  /**
   * Resets a debug counter to zero.
   *
   * @param counterName the name of the counter
   */
  public static void resetCounter(String counterName) {
    if (DEBUG_ENABLED) {
      counters.remove(counterName);
      logger.debug("Counter reset: {}", counterName);
    }
  }

  /** Logs current memory usage information. */
  public static void logMemoryUsage() {
    if (!DEBUG_ENABLED) {
      return;
    }

    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();

    logger.debug(
        "Memory Usage - Heap: used={}MB, max={}MB, Non-Heap: used={}MB, max={}MB",
        heapUsage.getUsed() / (1024 * 1024),
        heapUsage.getMax() / (1024 * 1024),
        nonHeapUsage.getUsed() / (1024 * 1024),
        nonHeapUsage.getMax() / (1024 * 1024));
  }

  /** Logs thread information. */
  public static void logThreadInfo() {
    if (!DEBUG_ENABLED) {
      return;
    }

    Thread currentThread = Thread.currentThread();
    ThreadGroup threadGroup = currentThread.getThreadGroup();

    logger.debug(
        "Thread Info - Name: {}, ID: {}, State: {}, Group: {}, Active Threads: {}",
        currentThread.getName(),
        currentThread.threadId(),
        currentThread.getState(),
        threadGroup.getName(),
        threadGroup.activeCount());
  }

  /**
   * Creates a debug snapshot with current system information.
   *
   * @return map containing debug information
   */
  public static Map<String, Object> createDebugSnapshot() {
    Map<String, Object> snapshot = new HashMap<>();

    if (!DEBUG_ENABLED) {
      snapshot.put("debug_enabled", false);
      return snapshot;
    }

    snapshot.put("debug_enabled", true);
    snapshot.put("timestamp", Instant.now().toString());

    // Memory information
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
    Map<String, Object> memoryInfo = new HashMap<>();
    memoryInfo.put("heap_used_mb", heapUsage.getUsed() / (1024 * 1024));
    memoryInfo.put("heap_max_mb", heapUsage.getMax() / (1024 * 1024));
    memoryInfo.put("heap_usage_percent", (double) heapUsage.getUsed() / heapUsage.getMax() * 100);
    snapshot.put("memory", memoryInfo);

    // Thread information
    Thread currentThread = Thread.currentThread();
    Map<String, Object> threadInfo = new HashMap<>();
    threadInfo.put("name", currentThread.getName());
    threadInfo.put("id", currentThread.threadId());
    threadInfo.put("state", currentThread.getState().toString());
    threadInfo.put("priority", currentThread.getPriority());
    snapshot.put("thread", threadInfo);

    // Active timers
    if (!timers.isEmpty()) {
      Map<String, Long> activeTimers = new HashMap<>();
      long currentTime = System.currentTimeMillis();
      timers.forEach((name, startTime) -> activeTimers.put(name, currentTime - startTime));
      snapshot.put("active_timers", activeTimers);
    }

    // Counters
    if (!counters.isEmpty()) {
      snapshot.put("counters", new HashMap<>(counters));
    }

    // System properties
    Map<String, String> bioidProperties = new HashMap<>();
    System.getProperties()
        .forEach(
            (key, value) -> {
              String keyStr = key.toString();
              if (keyStr.startsWith("bioid.")) {
                bioidProperties.put(keyStr, value.toString());
              }
            });
    if (!bioidProperties.isEmpty()) {
      snapshot.put("bioid_properties", bioidProperties);
    }

    return snapshot;
  }

  /** Logs a debug snapshot. */
  public static void logDebugSnapshot() {
    if (DEBUG_ENABLED) {
      Map<String, Object> snapshot = createDebugSnapshot();
      logger.debug("Debug Snapshot: {}", snapshot);
    }
  }

  /**
   * Executes a runnable with timing and logging.
   *
   * @param name the operation name
   * @param operation the operation to execute
   */
  public static void timeOperation(String name, Runnable operation) {
    if (!DEBUG_ENABLED) {
      operation.run();
      return;
    }

    logger.debug("Starting operation: {}", name);
    startTimer(name);

    try {
      operation.run();
      logger.debug("Operation completed successfully: {}", name);
    } catch (Exception e) {
      logger.debug("Operation failed: {} - {}", name, e.getMessage());
      throw e;
    } finally {
      long elapsed = stopTimer(name);
      logger.debug("Operation timing: {} - {}ms", name, elapsed);
    }
  }

  /**
   * Sanitizes sensitive data for logging.
   *
   * @param data the data to sanitize
   * @return sanitized data safe for logging
   */
  public static String sanitizeForLogging(String data) {
    if (data == null) {
      return null;
    }

    // Remove or mask sensitive patterns
    String sanitized = data;

    // Mask base64 image data
    if (data.startsWith("data:image/")) {
      int commaIndex = data.indexOf(',');
      if (commaIndex > 0 && data.length() > commaIndex + 20) {
        sanitized =
            data.substring(0, commaIndex + 1)
                + data.substring(commaIndex + 1, commaIndex + 21)
                + "...["
                + (data.length() - commaIndex - 21)
                + " more chars]";
      }
    }

    // Mask potential tokens or IDs longer than 20 characters
    if (data.length() > 50 && data.matches("[A-Za-z0-9+/=]+")) {
      sanitized = data.substring(0, 10) + "...[masked]..." + data.substring(data.length() - 10);
    }

    return sanitized;
  }

  /**
   * Checks if debug mode is enabled.
   *
   * @return true if debug mode is enabled
   */
  public static boolean isDebugEnabled() {
    return DEBUG_ENABLED;
  }

  /** Clears all debug data (timers and counters). */
  public static void clearDebugData() {
    if (DEBUG_ENABLED) {
      timers.clear();
      counters.clear();
      logger.debug("Debug data cleared");
    }
  }
}
