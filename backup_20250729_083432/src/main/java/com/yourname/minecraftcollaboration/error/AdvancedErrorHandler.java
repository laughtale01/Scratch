package com.yourname.minecraftcollaboration.error;

import com.yourname.minecraftcollaboration.monitoring.MetricsCollector;
import com.yourname.minecraftcollaboration.core.ErrorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Advanced error handling with recovery strategies and circuit breaker pattern
 */
public class AdvancedErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvancedErrorHandler.class);
    private static AdvancedErrorHandler instance;
    
    private final ErrorManager errorManager;
    private final MetricsCollector metrics;
    private final Map<String, CircuitBreaker> circuitBreakers;
    private final Map<String, RecoveryStrategy> recoveryStrategies;
    
    private AdvancedErrorHandler() {
        this.errorManager = ErrorManager.getInstance();
        this.metrics = MetricsCollector.getInstance();
        this.circuitBreakers = new ConcurrentHashMap<>();
        this.recoveryStrategies = new ConcurrentHashMap<>();
        
        // Register default recovery strategies
        registerDefaultStrategies();
    }
    
    public static AdvancedErrorHandler getInstance() {
        if (instance == null) {
            instance = new AdvancedErrorHandler();
        }
        return instance;
    }
    
    /**
     * Execute operation with error handling and recovery
     */
    public <T> ErrorResult<T> execute(String operationName, ErrorProneOperation<T> operation) {
        return execute(operationName, operation, null);
    }
    
    public <T> ErrorResult<T> execute(String operationName, 
                                     ErrorProneOperation<T> operation,
                                     RecoveryStrategy customStrategy) {
        CircuitBreaker breaker = circuitBreakers.computeIfAbsent(
            operationName, k -> new CircuitBreaker(operationName)
        );
        
        // Check circuit breaker
        if (!breaker.canExecute()) {
            metrics.incrementCounter("error.circuit_breaker.open." + operationName);
            return ErrorResult.failure(new CircuitBreakerOpenException(
                "Circuit breaker is open for: " + operationName
            ));
        }
        
        try {
            // Execute operation
            T result = operation.execute();
            breaker.recordSuccess();
            return ErrorResult.success(result);
            
        } catch (Exception e) {
            breaker.recordFailure();
            metrics.incrementCounter("error.operation.failed." + operationName);
            
            // Log error
            errorManager.logError(
                ErrorManager.ErrorCategory.OPERATION,
                ErrorManager.ErrorSeverity.HIGH,
                "Operation failed: " + operationName,
                e
            );
            
            // Try recovery
            RecoveryStrategy strategy = customStrategy != null ? 
                customStrategy : recoveryStrategies.get(operationName);
                
            if (strategy != null) {
                try {
                    strategy.recover(e);
                    metrics.incrementCounter("error.recovery.success." + operationName);
                    
                    // Retry operation after recovery
                    T result = operation.execute();
                    breaker.recordSuccess();
                    return ErrorResult.success(result);
                    
                } catch (Exception recoveryError) {
                    metrics.incrementCounter("error.recovery.failed." + operationName);
                    LOGGER.error("Recovery failed for {}", operationName, recoveryError);
                }
            }
            
            return ErrorResult.failure(e);
        }
    }
    
    /**
     * Execute operation with retry logic
     */
    public <T> ErrorResult<T> executeWithRetry(String operationName,
                                              ErrorProneOperation<T> operation,
                                              int maxRetries,
                                              long retryDelayMs) {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                LOGGER.info("Retrying {} (attempt {}/{})", operationName, attempt, maxRetries);
                try {
                    Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return ErrorResult.failure(ie);
                }
            }
            
            ErrorResult<T> result = execute(operationName, operation);
            if (result.isSuccess()) {
                if (attempt > 0) {
                    metrics.incrementCounter("error.retry.success." + operationName);
                }
                return result;
            }
            
            lastException = result.getError();
            metrics.incrementCounter("error.retry.attempt." + operationName);
        }
        
        metrics.incrementCounter("error.retry.exhausted." + operationName);
        return ErrorResult.failure(lastException);
    }
    
    /**
     * Execute async operation with timeout
     */
    public <T> ErrorResult<T> executeWithTimeout(String operationName,
                                                ErrorProneOperation<T> operation,
                                                long timeoutMs) {
        java.util.concurrent.CompletableFuture<T> future = 
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return operation.execute();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        
        try {
            T result = future.get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            return ErrorResult.success(result);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            metrics.incrementCounter("error.timeout." + operationName);
            return ErrorResult.failure(new OperationTimeoutException(
                "Operation timed out: " + operationName
            ));
        } catch (Exception e) {
            return ErrorResult.failure(e);
        }
    }
    
    /**
     * Register recovery strategy for an operation
     */
    public void registerRecoveryStrategy(String operationName, RecoveryStrategy strategy) {
        recoveryStrategies.put(operationName, strategy);
    }
    
    /**
     * Configure circuit breaker for an operation
     */
    public void configureCircuitBreaker(String operationName, 
                                       int failureThreshold, 
                                       long resetTimeoutMs) {
        CircuitBreaker breaker = circuitBreakers.computeIfAbsent(
            operationName, k -> new CircuitBreaker(operationName)
        );
        breaker.configure(failureThreshold, resetTimeoutMs);
    }
    
    /**
     * Register default recovery strategies
     */
    private void registerDefaultStrategies() {
        // WebSocket reconnection strategy
        registerRecoveryStrategy("websocket.send", (Exception e) -> {
            LOGGER.info("Attempting WebSocket reconnection...");
            // Implementation would reconnect WebSocket
        });
        
        // File operation recovery
        registerRecoveryStrategy("file.write", (Exception e) -> {
            if (e instanceof java.io.IOException) {
                LOGGER.info("Creating parent directories and retrying...");
                // Implementation would create directories
            }
        });
        
        // Database connection recovery
        registerRecoveryStrategy("db.query", (Exception e) -> {
            LOGGER.info("Refreshing database connection...");
            // Implementation would refresh connection
        });
    }
    
    /**
     * Get circuit breaker status
     */
    public Map<String, CircuitBreakerStatus> getCircuitBreakerStatuses() {
        Map<String, CircuitBreakerStatus> statuses = new ConcurrentHashMap<>();
        circuitBreakers.forEach((name, breaker) -> {
            statuses.put(name, breaker.getStatus());
        });
        return statuses;
    }
    
    /**
     * Reset all circuit breakers
     */
    public void resetAllCircuitBreakers() {
        circuitBreakers.values().forEach(CircuitBreaker::reset);
        LOGGER.info("All circuit breakers reset");
    }
    
    /**
     * Functional interface for error-prone operations
     */
    @FunctionalInterface
    public interface ErrorProneOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Functional interface for recovery strategies
     */
    @FunctionalInterface
    public interface RecoveryStrategy {
        void recover(Exception e) throws Exception;
    }
    
    /**
     * Result wrapper for error handling
     */
    public static class ErrorResult<T> {
        private final boolean success;
        private final T value;
        private final Exception error;
        
        private ErrorResult(boolean success, T value, Exception error) {
            this.success = success;
            this.value = value;
            this.error = error;
        }
        
        public static <T> ErrorResult<T> success(T value) {
            return new ErrorResult<>(true, value, null);
        }
        
        public static <T> ErrorResult<T> failure(Exception error) {
            return new ErrorResult<>(false, null, error);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public T getValue() {
            if (!success) {
                throw new IllegalStateException("Cannot get value from failed result");
            }
            return value;
        }
        
        public Exception getError() {
            return error;
        }
        
        public T getValueOrDefault(T defaultValue) {
            return success ? value : defaultValue;
        }
        
        public <U> ErrorResult<U> map(java.util.function.Function<T, U> mapper) {
            if (success) {
                try {
                    return success(mapper.apply(value));
                } catch (Exception e) {
                    return failure(e);
                }
            }
            return failure(error);
        }
    }
    
    /**
     * Circuit breaker implementation
     */
    private static class CircuitBreaker {
        private final String name;
        private final AtomicInteger failureCount;
        private volatile State state;
        private volatile long lastFailureTime;
        
        private int failureThreshold = 5;
        private long resetTimeoutMs = 60000; // 1 minute
        
        private enum State {
            CLOSED, OPEN, HALF_OPEN
        }
        
        CircuitBreaker(String name) {
            this.name = name;
            this.failureCount = new AtomicInteger(0);
            this.state = State.CLOSED;
            this.lastFailureTime = 0;
        }
        
        void configure(int failureThreshold, long resetTimeoutMs) {
            this.failureThreshold = failureThreshold;
            this.resetTimeoutMs = resetTimeoutMs;
        }
        
        boolean canExecute() {
            if (state == State.CLOSED) {
                return true;
            }
            
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > resetTimeoutMs) {
                    state = State.HALF_OPEN;
                    LOGGER.info("Circuit breaker {} moved to HALF_OPEN", name);
                    return true;
                }
                return false;
            }
            
            // HALF_OPEN
            return true;
        }
        
        void recordSuccess() {
            if (state == State.HALF_OPEN) {
                reset();
                LOGGER.info("Circuit breaker {} closed after successful operation", name);
            }
        }
        
        void recordFailure() {
            lastFailureTime = System.currentTimeMillis();
            int failures = failureCount.incrementAndGet();
            
            if (failures >= failureThreshold && state != State.OPEN) {
                state = State.OPEN;
                LOGGER.warn("Circuit breaker {} opened after {} failures", name, failures);
            }
        }
        
        void reset() {
            failureCount.set(0);
            state = State.CLOSED;
            lastFailureTime = 0;
        }
        
        CircuitBreakerStatus getStatus() {
            return new CircuitBreakerStatus(
                name, state.toString(), failureCount.get(), lastFailureTime
            );
        }
    }
    
    /**
     * Circuit breaker status
     */
    public static class CircuitBreakerStatus {
        public final String name;
        public final String state;
        public final int failureCount;
        public final long lastFailureTime;
        
        CircuitBreakerStatus(String name, String state, int failureCount, long lastFailureTime) {
            this.name = name;
            this.state = state;
            this.failureCount = failureCount;
            this.lastFailureTime = lastFailureTime;
        }
    }
    
    /**
     * Custom exceptions
     */
    public static class CircuitBreakerOpenException extends Exception {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
    
    public static class OperationTimeoutException extends Exception {
        public OperationTimeoutException(String message) {
            super(message);
        }
    }
}