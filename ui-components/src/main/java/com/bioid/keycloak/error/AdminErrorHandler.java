package com.bioid.keycloak.error;

import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Comprehensive error handler for administrative operations with retry logic,
 * exponential backoff, and graceful degradation capabilities.
 */
@ApplicationScoped
public class AdminErrorHandler {

    private static final Logger logger = Logger.getLogger(AdminErrorHandler.class);

    @Inject
    private AdminCircuitBreaker circuitBreaker;

    // Default retry configuration
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_BASE_DELAY = Duration.ofMillis(1000);
    private static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(30);
    private static final double DEFAULT_JITTER_FACTOR = 0.1;

    // Retryable error types by default
    private static final Set<AdminErrorType> DEFAULT_RETRYABLE_ERRORS = Set.of(
        AdminErrorType.SERVICE_UNAVAILABLE,
        AdminErrorType.CONNECTION_TIMEOUT,
        AdminErrorType.NETWORK_ERROR,
        AdminErrorType.RATE_LIMIT_EXCEEDED,
        AdminErrorType.TEMPLATE_SYNC_MISMATCH,
        AdminErrorType.BULK_OPERATION_TIMEOUT,
        AdminErrorType.DATA_INCONSISTENCY,
        AdminErrorType.CIRCUIT_BREAKER_OPEN,
        AdminErrorType.LOAD_BALANCER_ERROR,
        AdminErrorType.DNS_RESOLUTION_FAILED,
        AdminErrorType.AUDIT_LOG_FAILURE,
        AdminErrorType.UNKNOWN_ERROR,
        AdminErrorType.INTERNAL_ERROR
    );

    /**
     * Execute operation with retry logic and exponential backoff
     */
    public <T> T executeWithRetry(Supplier<T> operation) {
        return executeWithRetry(operation, DEFAULT_RETRYABLE_ERRORS);
    }

    /**
     * Execute operation with retry logic for specific error types
     */
    public <T> T executeWithRetry(Supplier<T> operation, Set<AdminErrorType> retryableErrors) {
        return executeWithRetry(operation, retryableErrors, DEFAULT_MAX_ATTEMPTS);
    }

    /**
     * Execute operation with custom retry configuration
     */
    public <T> T executeWithRetry(Supplier<T> operation, Set<AdminErrorType> retryableErrors, int maxAttempts) {
        return executeWithRetry(operation, retryableErrors, maxAttempts, DEFAULT_BASE_DELAY, DEFAULT_MAX_DELAY);
    }

    /**
     * Execute operation with full retry configuration
     */
    public <T> T executeWithRetry(Supplier<T> operation, Set<AdminErrorType> retryableErrors, 
                                  int maxAttempts, Duration baseDelay, Duration maxDelay) {
        
        AdminException lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.debugf("Executing operation, attempt %d of %d", attempt, maxAttempts);
                T result = operation.get();
                
                if (attempt > 1) {
                    logger.infof("Operation succeeded on attempt %d", attempt);
                }
                
                return result;
                
            } catch (AdminException e) {
                e.incrementAttemptCount();
                lastException = e;
                
                logger.warnf("Operation failed on attempt %d: %s", attempt, e.getMessage());
                
                // Check if error is retryable
                if (!retryableErrors.contains(e.getErrorType()) || !e.isRetryable()) {
                    logger.errorf("Non-retryable error encountered: %s", e.getErrorType());
                    throw e;
                }
                
                // Don't sleep on the last attempt
                if (attempt < maxAttempts) {
                    Duration delay = calculateDelay(attempt, baseDelay, maxDelay);
                    logger.debugf("Waiting %d ms before retry", delay.toMillis());
                    
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AdminException(AdminErrorType.BULK_OPERATION_CANCELLED, 
                            "Operation interrupted during retry", ie);
                    }
                }
                
            } catch (Exception e) {
                // Convert non-AdminException to AdminException
                AdminException adminException = new AdminException(AdminErrorType.INTERNAL_ERROR, 
                    "Unexpected error during operation", e);
                adminException.incrementAttemptCount();
                
                logger.errorf(e, "Unexpected error on attempt %d", attempt);
                
                if (attempt < maxAttempts && DEFAULT_RETRYABLE_ERRORS.contains(AdminErrorType.INTERNAL_ERROR)) {
                    Duration delay = calculateDelay(attempt, baseDelay, maxDelay);
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AdminException(AdminErrorType.BULK_OPERATION_CANCELLED, 
                            "Operation interrupted during retry", ie);
                    }
                } else {
                    throw adminException;
                }
                
                lastException = adminException;
            }
        }
        
        logger.errorf("Operation failed after %d attempts", maxAttempts);
        throw lastException != null ? lastException : 
            new AdminException(AdminErrorType.INTERNAL_ERROR, "Operation failed after all retry attempts");
    }

    /**
     * Execute operation asynchronously with retry logic
     */
    public <T> CompletableFuture<T> executeWithRetryAsync(Supplier<T> operation) {
        return CompletableFuture.supplyAsync(() -> executeWithRetry(operation));
    }

    /**
     * Handle bulk operation errors with individual item retry
     */
    public <T, R> BulkOperationResult<R> handleBulkOperationErrors(
            List<T> items, Function<T, R> operation, String operationName) {
        
        BulkOperationResult<R> result = new BulkOperationResult<>();
        result.setOperationId(generateOperationId());
        result.setTotalItems(items.size());
        result.setStartedAt(Instant.now());
        
        logger.infof("Starting bulk operation '%s' with %d items", operationName, items.size());
        
        List<R> successResults = new ArrayList<>();
        List<BulkOperationError> errors = new ArrayList<>();
        
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            String itemId = getItemId(item, i);
            
            try {
                // Execute with retry for individual items
                R operationResult = executeWithRetry(() -> operation.apply(item), 
                    DEFAULT_RETRYABLE_ERRORS, 2); // Reduced retries for bulk operations
                
                successResults.add(operationResult);
                result.incrementSuccessfulItems();
                
            } catch (AdminException e) {
                BulkOperationError error = new BulkOperationError();
                error.setItemId(itemId);
                error.setErrorCode(e.getErrorCode());
                error.setErrorMessage(e.getMessage());
                error.setRetryable(e.isRetryable());
                error.setAttemptCount(e.getAttemptCount());
                error.setContext(e.getContext());
                
                errors.add(error);
                result.incrementFailedItems();
                
                logger.warnf("Bulk operation item %s failed: %s", itemId, e.getMessage());
                
            } catch (Exception e) {
                BulkOperationError error = new BulkOperationError();
                error.setItemId(itemId);
                error.setErrorCode(AdminErrorType.INTERNAL_ERROR.getCode());
                error.setErrorMessage(e.getMessage());
                error.setRetryable(false);
                error.setAttemptCount(1);
                
                errors.add(error);
                result.incrementFailedItems();
                
                logger.errorf(e, "Unexpected error processing bulk operation item %s", itemId);
            }
            
            result.incrementProcessedItems();
        }
        
        result.setResults(successResults);
        result.setErrors(errors);
        result.setCompletedAt(Instant.now());
        
        // Determine final status
        if (result.getFailedItems() == 0) {
            result.setStatus(BulkOperationStatus.COMPLETED);
        } else if (result.getSuccessfulItems() == 0) {
            result.setStatus(BulkOperationStatus.FAILED);
        } else {
            result.setStatus(BulkOperationStatus.PARTIALLY_COMPLETED);
        }
        
        logger.infof("Bulk operation '%s' completed: %d successful, %d failed out of %d total", 
            operationName, result.getSuccessfulItems(), result.getFailedItems(), result.getTotalItems());
        
        return result;
    }

    /**
     * Execute operation with circuit breaker protection
     */
    public <T> T executeWithCircuitBreaker(String operation, Supplier<T> supplier) {
        return circuitBreaker.executeWithCircuitBreaker(operation, supplier);
    }

    /**
     * Execute operation with both retry and circuit breaker
     */
    public <T> T executeWithRetryAndCircuitBreaker(String operation, Supplier<T> supplier) {
        return circuitBreaker.executeWithCircuitBreaker(operation, () -> executeWithRetry(supplier));
    }

    /**
     * Graceful degradation - execute primary operation with fallback
     */
    public <T> T executeWithFallback(Supplier<T> primaryOperation, Supplier<T> fallbackOperation) {
        try {
            return executeWithRetry(primaryOperation);
        } catch (AdminException e) {
            logger.warnf("Primary operation failed, attempting fallback: %s", e.getMessage());
            
            try {
                T result = fallbackOperation.get();
                logger.info("Fallback operation succeeded");
                return result;
            } catch (Exception fallbackException) {
                logger.errorf(fallbackException, "Fallback operation also failed");
                // Return original exception
                throw e;
            }
        }
    }

    /**
     * Calculate exponential backoff delay with jitter
     */
    private Duration calculateDelay(int attempt, Duration baseDelay, Duration maxDelay) {
        // Exponential backoff: baseDelay * 2^(attempt-1)
        long delayMs = baseDelay.toMillis() * (1L << (attempt - 1));
        
        // Cap at maximum delay
        delayMs = Math.min(delayMs, maxDelay.toMillis());
        
        // Add jitter to prevent thundering herd
        double jitter = ThreadLocalRandom.current().nextDouble(-DEFAULT_JITTER_FACTOR, DEFAULT_JITTER_FACTOR);
        delayMs = (long) (delayMs * (1 + jitter));
        
        return Duration.ofMillis(Math.max(delayMs, 0));
    }

    /**
     * Generate unique operation ID
     */
    private String generateOperationId() {
        return "bulk_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    /**
     * Extract item ID for logging and error reporting
     */
    private String getItemId(Object item, int index) {
        if (item == null) {
            return "item_" + index;
        }
        
        // Try to extract meaningful ID from common object types
        if (item instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) item;
            Object id = map.get("id");
            if (id != null) {
                return id.toString();
            }
            Object userId = map.get("userId");
            if (userId != null) {
                return userId.toString();
            }
        }
        
        // Try reflection for getId() method
        try {
            java.lang.reflect.Method getIdMethod = item.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(item);
            if (id != null) {
                return id.toString();
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        
        // Try reflection for getUserId() method
        try {
            java.lang.reflect.Method getUserIdMethod = item.getClass().getMethod("getUserId");
            Object userId = getUserIdMethod.invoke(item);
            if (userId != null) {
                return userId.toString();
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        
        return "item_" + index;
    }
}