package com.yourname.minecraftcollaboration.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedErrorHandlerTest {
    
    private AdvancedErrorHandler errorHandler;
    
    @BeforeEach
    void setUp() {
        errorHandler = AdvancedErrorHandler.getInstance();
        errorHandler.resetAllCircuitBreakers();
    }
    
    @Test
    @DisplayName("Should execute successful operation")
    void testSuccessfulOperation() {
        // Given
        String operationName = "test.success";
        String expectedResult = "Success!";
        
        // When
        AdvancedErrorHandler.ErrorResult<String> result = errorHandler.execute(
            operationName,
            () -> expectedResult
        );
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals(expectedResult, result.getValue());
    }
    
    @Test
    @DisplayName("Should handle operation failure")
    void testFailedOperation() {
        // Given
        String operationName = "test.failure";
        Exception expectedException = new RuntimeException("Test error");
        
        // When
        AdvancedErrorHandler.ErrorResult<String> result = errorHandler.execute(
            operationName,
            () -> { throw expectedException; }
        );
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals(expectedException.getMessage(), result.getError().getMessage());
    }
    
    @Test
    @DisplayName("Should retry failed operation")
    void testRetryOperation() {
        // Given
        String operationName = "test.retry";
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        // When
        AdvancedErrorHandler.ErrorResult<String> result = errorHandler.executeWithRetry(
            operationName,
            () -> {
                int attempt = attemptCount.incrementAndGet();
                if (attempt < 3) {
                    throw new RuntimeException("Attempt " + attempt);
                }
                return "Success after retries";
            },
            3,  // max retries
            10  // retry delay
        );
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals("Success after retries", result.getValue());
        assertEquals(3, attemptCount.get());
    }
    
    @Test
    @DisplayName("Should timeout long operations")
    void testTimeoutOperation() {
        // Given
        String operationName = "test.timeout";
        
        // When
        AdvancedErrorHandler.ErrorResult<String> result = errorHandler.executeWithTimeout(
            operationName,
            () -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "Should not reach here";
            },
            100  // timeout ms
        );
        
        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getError() instanceof AdvancedErrorHandler.OperationTimeoutException);
    }
    
    @Test
    @DisplayName("Should open circuit breaker after failures")
    void testCircuitBreaker() {
        // Given
        String operationName = "test.circuit";
        errorHandler.configureCircuitBreaker(operationName, 3, 1000);
        
        // When - fail 3 times
        for (int i = 0; i < 3; i++) {
            errorHandler.execute(operationName, () -> {
                throw new RuntimeException("Failure " + i);
            });
        }
        
        // Then - circuit should be open
        AdvancedErrorHandler.ErrorResult<String> result = errorHandler.execute(
            operationName,
            () -> "Should not execute"
        );
        
        assertFalse(result.isSuccess());
        assertTrue(result.getError() instanceof AdvancedErrorHandler.CircuitBreakerOpenException);
    }
    
    @Test
    @DisplayName("Should recover using recovery strategy")
    void testRecoveryStrategy() {
        // Given
        String operationName = "test.recovery";
        AtomicInteger recoveryAttempts = new AtomicInteger(0);
        boolean[] shouldFail = {true};
        
        errorHandler.registerRecoveryStrategy(operationName, (Exception e) -> {
            recoveryAttempts.incrementAndGet();
            shouldFail[0] = false; // Recovery fixes the issue
        });
        
        // When
        AdvancedErrorHandler.ErrorResult<String> result = errorHandler.execute(
            operationName,
            () -> {
                if (shouldFail[0]) {
                    throw new RuntimeException("Initial failure");
                }
                return "Success after recovery";
            }
        );
        
        // Then
        assertTrue(result.isSuccess());
        assertEquals("Success after recovery", result.getValue());
        assertEquals(1, recoveryAttempts.get());
    }
    
    @Test
    @DisplayName("Should map successful result")
    void testResultMapping() {
        // Given
        AdvancedErrorHandler.ErrorResult<Integer> result = 
            AdvancedErrorHandler.ErrorResult.success(42);
        
        // When
        AdvancedErrorHandler.ErrorResult<String> mapped = 
            result.map(n -> "Number: " + n);
        
        // Then
        assertTrue(mapped.isSuccess());
        assertEquals("Number: 42", mapped.getValue());
    }
    
    @Test
    @DisplayName("Should propagate error in mapping")
    void testResultMappingError() {
        // Given
        Exception error = new RuntimeException("Test error");
        AdvancedErrorHandler.ErrorResult<Integer> result = 
            AdvancedErrorHandler.ErrorResult.failure(error);
        
        // When
        AdvancedErrorHandler.ErrorResult<String> mapped = 
            result.map(n -> "Number: " + n);
        
        // Then
        assertFalse(mapped.isSuccess());
        assertEquals(error, mapped.getError());
    }
    
    @Test
    @DisplayName("Should get default value for failed result")
    void testGetValueOrDefault() {
        // Given
        AdvancedErrorHandler.ErrorResult<String> successResult = 
            AdvancedErrorHandler.ErrorResult.success("Success");
        AdvancedErrorHandler.ErrorResult<String> failureResult = 
            AdvancedErrorHandler.ErrorResult.failure(new RuntimeException());
        
        // When/Then
        assertEquals("Success", successResult.getValueOrDefault("Default"));
        assertEquals("Default", failureResult.getValueOrDefault("Default"));
    }
    
    @Test
    @DisplayName("Should get circuit breaker status")
    void testCircuitBreakerStatus() {
        // Given
        String operationName = "test.status";
        errorHandler.configureCircuitBreaker(operationName, 2, 1000);
        
        // When - fail once
        errorHandler.execute(operationName, () -> {
            throw new RuntimeException("Failure");
        });
        
        // Then
        var statuses = errorHandler.getCircuitBreakerStatuses();
        var status = statuses.get(operationName);
        
        assertNotNull(status);
        assertEquals("CLOSED", status.state);
        assertEquals(1, status.failureCount);
    }
}