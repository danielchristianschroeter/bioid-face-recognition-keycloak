package com.bioid.keycloak.error;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Service for implementing graceful degradation patterns when BioID services
 * are unavailable or experiencing issues.
 */
@ApplicationScoped
public class GracefulDegradationService {

    private static final Logger logger = Logger.getLogger(GracefulDegradationService.class);

    @Inject
    private AdminErrorHandler errorHandler;

    @Inject
    private AdminCircuitBreaker circuitBreaker;

    /**
     * Execute operation with graceful degradation to fallback
     */
    public <T> T executeWithFallback(String operationName, Supplier<T> primaryOperation, 
                                   Supplier<T> fallbackOperation) {
        return executeWithFallback(operationName, primaryOperation, fallbackOperation, null);
    }

    /**
     * Execute operation with graceful degradation and default value
     */
    public <T> T executeWithFallback(String operationName, Supplier<T> primaryOperation, 
                                   Supplier<T> fallbackOperation, T defaultValue) {
        try {
            logger.debugf("Executing primary operation: %s", operationName);
            return circuitBreaker.executeWithCircuitBreaker(operationName, primaryOperation);
            
        } catch (AdminException e) {
            logger.warnf("Primary operation '%s' failed: %s. Attempting fallback.", operationName, e.getMessage());
            
            if (fallbackOperation != null) {
                try {
                    T result = fallbackOperation.get();
                    logger.infof("Fallback operation succeeded for: %s", operationName);
                    return result;
                } catch (Exception fallbackException) {
                    logger.errorf(fallbackException, "Fallback operation also failed for: %s", operationName);
                }
            }
            
            if (defaultValue != null) {
                logger.infof("Returning default value for operation: %s", operationName);
                return defaultValue;
            }
            
            // Re-throw original exception if no fallback succeeded
            throw e;
        }
    }

    /**
     * Execute operation with timeout and fallback
     */
    public <T> T executeWithTimeoutAndFallback(String operationName, Supplier<T> primaryOperation,
                                             Supplier<T> fallbackOperation, long timeoutSeconds) {
        try {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> 
                circuitBreaker.executeWithCircuitBreaker(operationName, primaryOperation));
            
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
            
        } catch (TimeoutException e) {
            logger.warnf("Operation '%s' timed out after %d seconds. Attempting fallback.", 
                operationName, timeoutSeconds);
            
            if (fallbackOperation != null) {
                try {
                    return fallbackOperation.get();
                } catch (Exception fallbackException) {
                    logger.errorf(fallbackException, "Fallback operation failed for: %s", operationName);
                    throw new AdminException(AdminErrorType.SERVICE_UNAVAILABLE, 
                        "Operation timed out and fallback failed", fallbackException);
                }
            }
            
            throw new AdminException(AdminErrorType.CONNECTION_TIMEOUT, 
                String.format("Operation '%s' timed out after %d seconds", operationName, timeoutSeconds), e);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warnf("Operation '%s' was interrupted. Attempting fallback.", operationName);
            
            if (fallbackOperation != null) {
                try {
                    return fallbackOperation.get();
                } catch (Exception fallbackException) {
                    logger.errorf(fallbackException, "Fallback operation failed for: %s", operationName);
                    throw new AdminException(AdminErrorType.BULK_OPERATION_CANCELLED, 
                        "Operation was interrupted and fallback failed", fallbackException);
                }
            }
            
            throw new AdminException(AdminErrorType.BULK_OPERATION_CANCELLED, 
                String.format("Operation '%s' was interrupted", operationName), e);
            
        } catch (java.util.concurrent.ExecutionException e) {
            logger.errorf(e, "Operation '%s' failed during execution. Attempting fallback.", operationName);
            
            if (fallbackOperation != null) {
                try {
                    return fallbackOperation.get();
                } catch (Exception fallbackException) {
                    logger.errorf(fallbackException, "Fallback operation failed for: %s", operationName);
                    throw new AdminException(AdminErrorType.INTERNAL_ERROR, 
                        "Operation failed during execution and fallback failed", fallbackException);
                }
            }
            
            throw new AdminException(AdminErrorType.INTERNAL_ERROR, 
                String.format("Operation '%s' failed during execution", operationName), e);
            
        } catch (Exception e) {
            logger.errorf(e, "Unexpected error during operation: %s", operationName);
            
            if (fallbackOperation != null) {
                try {
                    return fallbackOperation.get();
                } catch (Exception fallbackException) {
                    logger.errorf(fallbackException, "Fallback operation failed for: %s", operationName);
                }
            }
            
            if (e instanceof AdminException) {
                throw e;
            } else {
                throw new AdminException(AdminErrorType.INTERNAL_ERROR, 
                    "Unexpected error during operation", e);
            }
        }
    }

    /**
     * Execute operation with cached fallback data
     */
    public <T> Optional<T> executeWithCachedFallback(String operationName, Supplier<T> primaryOperation,
                                                   Supplier<Optional<T>> cachedDataSupplier) {
        try {
            T result = circuitBreaker.executeWithCircuitBreaker(operationName, primaryOperation);
            return Optional.of(result);
            
        } catch (AdminException e) {
            logger.warnf("Primary operation '%s' failed: %s. Attempting cached fallback.", 
                operationName, e.getMessage());
            
            if (cachedDataSupplier != null) {
                try {
                    Optional<T> cachedResult = cachedDataSupplier.get();
                    if (cachedResult.isPresent()) {
                        logger.infof("Using cached data for operation: %s", operationName);
                        return cachedResult;
                    } else {
                        logger.warnf("No cached data available for operation: %s", operationName);
                    }
                } catch (Exception cacheException) {
                    logger.errorf(cacheException, "Failed to retrieve cached data for: %s", operationName);
                }
            }
            
            logger.warnf("No fallback available for operation: %s", operationName);
            return Optional.empty();
        }
    }

    /**
     * Execute read-only operation with stale data fallback
     */
    public <T> T executeReadOnlyWithStaleFallback(String operationName, Supplier<T> primaryOperation,
                                                Supplier<T> staleDataSupplier, long maxStaleAgeMinutes) {
        try {
            return circuitBreaker.executeWithCircuitBreaker(operationName, primaryOperation);
            
        } catch (AdminException e) {
            logger.warnf("Primary read operation '%s' failed: %s. Checking for stale data.", 
                operationName, e.getMessage());
            
            if (staleDataSupplier != null) {
                try {
                    T staleData = staleDataSupplier.get();
                    logger.infof("Using stale data (max age: %d minutes) for operation: %s", 
                        maxStaleAgeMinutes, operationName);
                    return staleData;
                } catch (Exception staleException) {
                    logger.errorf(staleException, "Failed to retrieve stale data for: %s", operationName);
                }
            }
            
            throw e;
        }
    }

    /**
     * Execute operation with reduced functionality fallback
     */
    public <T> T executeWithReducedFunctionality(String operationName, Supplier<T> fullOperation,
                                                Supplier<T> reducedOperation) {
        try {
            return circuitBreaker.executeWithCircuitBreaker(operationName, fullOperation);
            
        } catch (AdminException e) {
            if (isServiceDegradationError(e)) {
                logger.warnf("Service degraded for operation '%s': %s. Using reduced functionality.", 
                    operationName, e.getMessage());
                
                if (reducedOperation != null) {
                    try {
                        return reducedOperation.get();
                    } catch (Exception reducedException) {
                        logger.errorf(reducedException, "Reduced functionality also failed for: %s", operationName);
                    }
                }
            }
            
            throw e;
        }
    }

    /**
     * Execute bulk operation with partial success tolerance
     */
    public <T> BulkOperationResult<T> executeBulkWithPartialSuccess(String operationName,
                                                                  Supplier<BulkOperationResult<T>> bulkOperation,
                                                                  double minimumSuccessRate) {
        try {
            BulkOperationResult<T> result = bulkOperation.get();
            
            if (result.getSuccessRate() >= minimumSuccessRate) {
                logger.infof("Bulk operation '%s' succeeded with %.2f%% success rate", 
                    operationName, result.getSuccessRate() * 100);
                return result;
            } else {
                logger.warnf("Bulk operation '%s' failed to meet minimum success rate of %.2f%% (actual: %.2f%%)", 
                    operationName, minimumSuccessRate * 100, result.getSuccessRate() * 100);
                
                throw new AdminException(AdminErrorType.PARTIAL_BULK_FAILURE,
                    String.format("Bulk operation failed to meet minimum success rate of %.2f%%", 
                        minimumSuccessRate * 100));
            }
            
        } catch (AdminException e) {
            throw e;
        } catch (Exception e) {
            throw new AdminException(AdminErrorType.BULK_OPERATION_TIMEOUT,
                "Bulk operation failed unexpectedly", e);
        }
    }

    /**
     * Check if error indicates service degradation rather than complete failure
     */
    private boolean isServiceDegradationError(AdminException e) {
        AdminErrorType errorType = e.getErrorType();
        return errorType == AdminErrorType.RATE_LIMIT_EXCEEDED ||
               errorType == AdminErrorType.CONNECTION_TIMEOUT ||
               errorType == AdminErrorType.LOAD_BALANCER_ERROR ||
               errorType == AdminErrorType.PARTIAL_BULK_FAILURE ||
               (errorType == AdminErrorType.SERVICE_UNAVAILABLE && e.isRetryable());
    }

    /**
     * Get service health status with degradation information
     */
    public ServiceHealthStatus getServiceHealthWithDegradation(String serviceName) {
        try {
            // Check circuit breaker status
            var circuitBreakerStats = circuitBreaker.getStats(serviceName);
            
            if (circuitBreakerStats != null) {
                switch (circuitBreakerStats.getState()) {
                    case OPEN:
                        return new ServiceHealthStatus(serviceName, HealthStatus.UNHEALTHY, 
                            "Circuit breaker is OPEN", circuitBreakerStats.getFailureRate());
                    case HALF_OPEN:
                        return new ServiceHealthStatus(serviceName, HealthStatus.DEGRADED, 
                            "Circuit breaker is HALF_OPEN (testing recovery)", circuitBreakerStats.getFailureRate());
                    case CLOSED:
                        if (circuitBreakerStats.getFailureRate() > 0.2) { // 20% failure rate threshold
                            return new ServiceHealthStatus(serviceName, HealthStatus.DEGRADED, 
                                "High failure rate detected", circuitBreakerStats.getFailureRate());
                        } else {
                            return new ServiceHealthStatus(serviceName, HealthStatus.HEALTHY, 
                                "Service operating normally", circuitBreakerStats.getFailureRate());
                        }
                }
            }
            
            return new ServiceHealthStatus(serviceName, HealthStatus.UNKNOWN, 
                "No circuit breaker statistics available", 0.0);
                
        } catch (Exception e) {
            logger.errorf(e, "Failed to get service health for: %s", serviceName);
            return new ServiceHealthStatus(serviceName, HealthStatus.UNKNOWN, 
                "Failed to determine service health", 0.0);
        }
    }

    /**
     * Service health status with degradation information
     */
    public static class ServiceHealthStatus {
        private final String serviceName;
        private final HealthStatus status;
        private final String message;
        private final double failureRate;

        public ServiceHealthStatus(String serviceName, HealthStatus status, String message, double failureRate) {
            this.serviceName = serviceName;
            this.status = status;
            this.message = message;
            this.failureRate = failureRate;
        }

        public String getServiceName() { return serviceName; }
        public HealthStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public double getFailureRate() { return failureRate; }

        @Override
        public String toString() {
            return String.format("ServiceHealthStatus{serviceName='%s', status=%s, message='%s', failureRate=%.2f}", 
                serviceName, status, message, failureRate);
        }
    }

    public enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }
}