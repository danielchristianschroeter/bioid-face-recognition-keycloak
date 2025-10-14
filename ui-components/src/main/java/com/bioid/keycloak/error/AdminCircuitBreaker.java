package com.bioid.keycloak.error;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Circuit breaker implementation for administrative operations to prevent
 * cascading failures and provide graceful degradation.
 */
@ApplicationScoped
public class AdminCircuitBreaker {

    private static final Logger logger = Logger.getLogger(AdminCircuitBreaker.class);

    private final ConcurrentHashMap<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    // Default circuit breaker configuration
    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    private static final double DEFAULT_FAILURE_RATE_THRESHOLD = 0.5; // 50%
    private static final Duration DEFAULT_WAIT_DURATION = Duration.ofSeconds(30);
    private static final int DEFAULT_SLIDING_WINDOW_SIZE = 10;
    private static final int DEFAULT_MINIMUM_NUMBER_OF_CALLS = 5;

    /**
     * Get or create a circuit breaker for the specified operation
     */
    public CircuitBreaker getCircuitBreaker(String operation) {
        return circuitBreakers.computeIfAbsent(operation, key -> 
            new CircuitBreaker(key, DEFAULT_FAILURE_THRESHOLD, DEFAULT_FAILURE_RATE_THRESHOLD,
                DEFAULT_WAIT_DURATION, DEFAULT_SLIDING_WINDOW_SIZE, DEFAULT_MINIMUM_NUMBER_OF_CALLS));
    }

    /**
     * Get or create a circuit breaker with custom configuration
     */
    public CircuitBreaker getCircuitBreaker(String operation, int failureThreshold, 
                                          double failureRateThreshold, Duration waitDuration) {
        return circuitBreakers.computeIfAbsent(operation, key -> 
            new CircuitBreaker(key, failureThreshold, failureRateThreshold, waitDuration, 
                DEFAULT_SLIDING_WINDOW_SIZE, DEFAULT_MINIMUM_NUMBER_OF_CALLS));
    }

    /**
     * Execute operation with circuit breaker protection
     */
    public <T> T executeWithCircuitBreaker(String operation, Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = getCircuitBreaker(operation);
        return circuitBreaker.execute(supplier);
    }

    /**
     * Get circuit breaker statistics for monitoring
     */
    public CircuitBreakerStats getStats(String operation) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(operation);
        return circuitBreaker != null ? circuitBreaker.getStats() : null;
    }

    /**
     * Reset circuit breaker to closed state
     */
    public void reset(String operation) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(operation);
        if (circuitBreaker != null) {
            circuitBreaker.reset();
            logger.infof("Circuit breaker for operation '%s' has been reset", operation);
        }
    }

    /**
     * Force circuit breaker to open state
     */
    public void forceOpen(String operation) {
        CircuitBreaker circuitBreaker = circuitBreakers.get(operation);
        if (circuitBreaker != null) {
            circuitBreaker.forceOpen();
            logger.warnf("Circuit breaker for operation '%s' has been forced open", operation);
        }
    }

    /**
     * Inner class representing a single circuit breaker
     */
    public static class CircuitBreaker {
        private final String name;
        private final int failureThreshold;
        private final double failureRateThreshold;
        private final Duration waitDuration;
        private final int slidingWindowSize;
        private final int minimumNumberOfCalls;

        private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicLong lastFailureTime = new AtomicLong(0);
        private final AtomicLong lastSuccessTime = new AtomicLong(0);
        private final AtomicInteger totalCalls = new AtomicInteger(0);

        // Sliding window for failure rate calculation
        private final long[] callResults; // 1 for success, 0 for failure
        private final AtomicInteger windowIndex = new AtomicInteger(0);

        public CircuitBreaker(String name, int failureThreshold, double failureRateThreshold,
                            Duration waitDuration, int slidingWindowSize, int minimumNumberOfCalls) {
            this.name = name;
            this.failureThreshold = failureThreshold;
            this.failureRateThreshold = failureRateThreshold;
            this.waitDuration = waitDuration;
            this.slidingWindowSize = slidingWindowSize;
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            this.callResults = new long[slidingWindowSize];
        }

        public <T> T execute(Supplier<T> supplier) {
            CircuitBreakerState currentState = state.get();

            // Check if circuit breaker should transition from OPEN to HALF_OPEN
            if (currentState == CircuitBreakerState.OPEN && shouldAttemptReset()) {
                state.compareAndSet(CircuitBreakerState.OPEN, CircuitBreakerState.HALF_OPEN);
                currentState = CircuitBreakerState.HALF_OPEN;
                logger.infof("Circuit breaker '%s' transitioning to HALF_OPEN", name);
            }

            // Reject calls if circuit breaker is OPEN
            if (currentState == CircuitBreakerState.OPEN) {
                throw new AdminException(AdminErrorType.CIRCUIT_BREAKER_OPEN, 
                    String.format("Circuit breaker '%s' is OPEN", name));
            }

            totalCalls.incrementAndGet();

            try {
                T result = supplier.get();
                onSuccess();
                return result;

            } catch (Exception e) {
                onFailure();
                
                // Convert to AdminException if needed
                if (e instanceof AdminException) {
                    throw e;
                } else {
                    throw new AdminException(AdminErrorType.INTERNAL_ERROR, 
                        "Operation failed in circuit breaker", e);
                }
            }
        }

        private void onSuccess() {
            successCount.incrementAndGet();
            lastSuccessTime.set(System.currentTimeMillis());
            recordCall(true);

            CircuitBreakerState currentState = state.get();
            
            if (currentState == CircuitBreakerState.HALF_OPEN) {
                // Transition back to CLOSED after successful call in HALF_OPEN
                state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.CLOSED);
                failureCount.set(0);
                logger.infof("Circuit breaker '%s' transitioning to CLOSED after successful call", name);
            }
        }

        private void onFailure() {
            int failures = failureCount.incrementAndGet();
            lastFailureTime.set(System.currentTimeMillis());
            recordCall(false);

            CircuitBreakerState currentState = state.get();

            if (currentState == CircuitBreakerState.HALF_OPEN) {
                // Transition back to OPEN on failure in HALF_OPEN
                state.compareAndSet(CircuitBreakerState.HALF_OPEN, CircuitBreakerState.OPEN);
                logger.warnf("Circuit breaker '%s' transitioning to OPEN after failure in HALF_OPEN", name);
                
            } else if (currentState == CircuitBreakerState.CLOSED && shouldOpen()) {
                // Transition to OPEN if failure threshold is exceeded
                state.compareAndSet(CircuitBreakerState.CLOSED, CircuitBreakerState.OPEN);
                logger.warnf("Circuit breaker '%s' transitioning to OPEN after %d failures", name, failures);
            }
        }

        private void recordCall(boolean success) {
            int index = windowIndex.getAndIncrement() % slidingWindowSize;
            callResults[index] = success ? 1 : 0;
        }

        private boolean shouldOpen() {
            int totalCallsInWindow = Math.min(totalCalls.get(), slidingWindowSize);
            
            if (totalCallsInWindow < minimumNumberOfCalls) {
                return false;
            }

            // Check consecutive failure threshold
            if (failureCount.get() >= failureThreshold) {
                return true;
            }

            // Check failure rate threshold
            long successfulCalls = 0;
            for (int i = 0; i < totalCallsInWindow; i++) {
                successfulCalls += callResults[i];
            }
            
            double failureRate = 1.0 - ((double) successfulCalls / totalCallsInWindow);
            return failureRate >= failureRateThreshold;
        }

        private boolean shouldAttemptReset() {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
            return timeSinceLastFailure >= waitDuration.toMillis();
        }

        public void reset() {
            state.set(CircuitBreakerState.CLOSED);
            failureCount.set(0);
            successCount.set(0);
            totalCalls.set(0);
            windowIndex.set(0);
            
            // Clear sliding window
            for (int i = 0; i < slidingWindowSize; i++) {
                callResults[i] = 0;
            }
        }

        public void forceOpen() {
            state.set(CircuitBreakerState.OPEN);
            lastFailureTime.set(System.currentTimeMillis());
        }

        public CircuitBreakerStats getStats() {
            int totalCallsInWindow = Math.min(totalCalls.get(), slidingWindowSize);
            long successfulCalls = 0;
            
            for (int i = 0; i < totalCallsInWindow; i++) {
                successfulCalls += callResults[i];
            }
            
            double failureRate = totalCallsInWindow > 0 ? 
                1.0 - ((double) successfulCalls / totalCallsInWindow) : 0.0;

            return new CircuitBreakerStats(
                name,
                state.get(),
                failureCount.get(),
                successCount.get(),
                totalCalls.get(),
                failureRate,
                lastFailureTime.get(),
                lastSuccessTime.get()
            );
        }
    }

    /**
     * Circuit breaker states
     */
    public enum CircuitBreakerState {
        CLOSED,    // Normal operation
        OPEN,      // Failing fast, not allowing calls
        HALF_OPEN  // Testing if service has recovered
    }

    /**
     * Circuit breaker statistics
     */
    public static class CircuitBreakerStats {
        private final String name;
        private final CircuitBreakerState state;
        private final int failureCount;
        private final int successCount;
        private final int totalCalls;
        private final double failureRate;
        private final long lastFailureTime;
        private final long lastSuccessTime;

        public CircuitBreakerStats(String name, CircuitBreakerState state, int failureCount,
                                 int successCount, int totalCalls, double failureRate,
                                 long lastFailureTime, long lastSuccessTime) {
            this.name = name;
            this.state = state;
            this.failureCount = failureCount;
            this.successCount = successCount;
            this.totalCalls = totalCalls;
            this.failureRate = failureRate;
            this.lastFailureTime = lastFailureTime;
            this.lastSuccessTime = lastSuccessTime;
        }

        // Getters
        public String getName() { return name; }
        public CircuitBreakerState getState() { return state; }
        public int getFailureCount() { return failureCount; }
        public int getSuccessCount() { return successCount; }
        public int getTotalCalls() { return totalCalls; }
        public double getFailureRate() { return failureRate; }
        public long getLastFailureTime() { return lastFailureTime; }
        public long getLastSuccessTime() { return lastSuccessTime; }

        @Override
        public String toString() {
            return String.format("CircuitBreakerStats{name='%s', state=%s, failureCount=%d, " +
                    "successCount=%d, totalCalls=%d, failureRate=%.2f}", 
                    name, state, failureCount, successCount, totalCalls, failureRate);
        }
    }
}