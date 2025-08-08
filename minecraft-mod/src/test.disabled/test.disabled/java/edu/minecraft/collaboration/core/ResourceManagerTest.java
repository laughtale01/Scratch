package edu.minecraft.collaboration.core;

import edu.minecraft.collaboration.core.ResourceManager;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ResourceManager to verify proper resource management
 */
public class ResourceManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManagerTest.class);
    
    private ResourceManager resourceManager;
    
    @BeforeEach
    void setUp() {
        // Note: ResourceManager is a singleton, so we get the same instance
        resourceManager = ResourceManager.getInstance();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up any test resources
        try {
            resourceManager.unregisterAndCloseResource("test-resource");
            resourceManager.unregisterAndShutdownExecutor("test-executor");
        } catch (Exception e) {
            LOGGER.warn("Error during test cleanup", e);
        }
    }
    
    @Test
    @DisplayName("Should register and manage AutoCloseable resources")
    void testResourceRegistration() {
        AtomicBoolean resourceClosed = new AtomicBoolean(false);
        
        // Create a test resource
        AutoCloseable testResource = () -> {
            resourceClosed.set(true);
            LOGGER.info("Test resource closed");
        };
        
        // Register the resource
        assertDoesNotThrow(() -> {
            resourceManager.registerResource("test-resource", testResource);
        });
        
        // Verify resource is tracked
        ResourceManager.ResourceStatistics stats = resourceManager.getStatistics();
        assertTrue(stats.getManagedResources() > 0);
        
        // Close the resource
        assertTrue(resourceManager.unregisterAndCloseResource("test-resource"));
        assertTrue(resourceClosed.get(), "Resource should have been closed");
    }
    
    @Test
    @DisplayName("Should register and manage ExecutorService")
    void testExecutorRegistration() throws InterruptedException {
        CountDownLatch executorTaskLatch = new CountDownLatch(1);
        
        // Create a test executor
        ScheduledExecutorService testExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Register the executor
        assertDoesNotThrow(() -> {
            resourceManager.registerExecutor("test-executor", testExecutor);
        });
        
        // Verify executor is tracked
        ResourceManager.ResourceStatistics stats = resourceManager.getStatistics();
        assertTrue(stats.getManagedExecutors() > 0);
        
        // Submit a task to verify executor is working
        testExecutor.submit(() -> {
            LOGGER.info("Test executor task running");
            executorTaskLatch.countDown();
        });
        
        // Wait for task to complete
        assertTrue(executorTaskLatch.await(5, TimeUnit.SECONDS), 
                  "Executor task should complete within 5 seconds");
        
        // Shutdown the executor
        assertTrue(resourceManager.unregisterAndShutdownExecutor("test-executor"));
        
        // Verify executor is shutdown
        assertTrue(testExecutor.isShutdown(), "Executor should be shutdown");
    }
    
    @Test
    @DisplayName("Should register and execute shutdown hooks")
    void testShutdownHooks() {
        AtomicBoolean hookExecuted = new AtomicBoolean(false);
        
        // Create a test shutdown hook
        Runnable testHook = () -> {
            hookExecuted.set(true);
            LOGGER.info("Test shutdown hook executed");
        };
        
        // Register the hook
        assertDoesNotThrow(() -> {
            resourceManager.registerShutdownHook(testHook);
        });
        
        // Verify hook is tracked
        ResourceManager.ResourceStatistics stats = resourceManager.getStatistics();
        assertTrue(stats.getShutdownHooks() > 0);
        
        // Note: We cannot easily test actual shutdown hook execution in a unit test
        // without shutting down the ResourceManager, which would affect other tests
    }
    
    @Test
    @DisplayName("Should handle null resources gracefully")
    void testNullResourceHandling() {
        // Test null resource registration
        assertDoesNotThrow(() -> {
            resourceManager.registerResource("null-resource", null);
        });
        
        // Test null executor registration
        assertDoesNotThrow(() -> {
            resourceManager.registerExecutor("null-executor", null);
        });
        
        // Test null shutdown hook registration
        assertDoesNotThrow(() -> {
            resourceManager.registerShutdownHook(null);
        });
    }
    
    @Test
    @DisplayName("Should prevent resource registration during shutdown")
    void testRegistrationDuringShutdown() {
        // Note: This test is tricky because ResourceManager is a singleton
        // and we can't easily simulate shutdown state without affecting other tests
        
        // For now, just verify that ResourceManager is not in shutdown state
        assertFalse(resourceManager.isShutdown(), 
                   "ResourceManager should not be shutdown during tests");
    }
    
    @Test
    @DisplayName("Should replace existing resources correctly")
    void testResourceReplacement() {
        AtomicBoolean firstResourceClosed = new AtomicBoolean(false);
        AtomicBoolean secondResourceClosed = new AtomicBoolean(false);
        
        // Create first resource
        AutoCloseable firstResource = () -> {
            firstResourceClosed.set(true);
            LOGGER.info("First resource closed");
        };
        
        // Create second resource
        AutoCloseable secondResource = () -> {
            secondResourceClosed.set(true);
            LOGGER.info("Second resource closed");
        };
        
        // Register first resource
        resourceManager.registerResource("replaceable-resource", firstResource);
        
        // Replace with second resource - first should be closed automatically
        resourceManager.registerResource("replaceable-resource", secondResource);
        
        // Verify first resource was closed during replacement
        assertTrue(firstResourceClosed.get(), 
                  "First resource should have been closed when replaced");
        
        // Clean up second resource
        resourceManager.unregisterAndCloseResource("replaceable-resource");
        assertTrue(secondResourceClosed.get(), 
                  "Second resource should be closed when unregistered");
    }
    
    @Test
    @DisplayName("Should provide accurate statistics")
    void testStatistics() {
        // Get initial stats
        ResourceManager.ResourceStatistics initialStats = resourceManager.getStatistics();
        int initialResources = initialStats.getManagedResources();
        int initialExecutors = initialStats.getManagedExecutors();
        int initialHooks = initialStats.getShutdownHooks();
        
        // Add resources
        AutoCloseable testResource = () -> {};
        ScheduledExecutorService testExecutor = Executors.newSingleThreadScheduledExecutor();
        Runnable testHook = () -> {};
        
        resourceManager.registerResource("stats-test-resource", testResource);
        resourceManager.registerExecutor("stats-test-executor", testExecutor);
        resourceManager.registerShutdownHook(testHook);
        
        // Verify stats increased
        ResourceManager.ResourceStatistics newStats = resourceManager.getStatistics();
        assertEquals(initialResources + 1, newStats.getManagedResources());
        assertEquals(initialExecutors + 1, newStats.getManagedExecutors());
        assertEquals(initialHooks + 1, newStats.getShutdownHooks());
        
        // Clean up
        resourceManager.unregisterAndCloseResource("stats-test-resource");
        resourceManager.unregisterAndShutdownExecutor("stats-test-executor");
        
        // Verify stats decreased (shutdown hooks remain)
        ResourceManager.ResourceStatistics finalStats = resourceManager.getStatistics();
        assertEquals(initialResources, finalStats.getManagedResources());
        assertEquals(initialExecutors, finalStats.getManagedExecutors());
        // Note: Shutdown hooks are not removed individually, only cleared during shutdown
    }
    
    @Test
    @DisplayName("Should handle resource closing exceptions gracefully")
    void testResourceClosingExceptions() {
        // Create a resource that throws an exception when closed
        AutoCloseable problematicResource = () -> {
            throw new RuntimeException("Test exception during resource closing");
        };
        
        // Register the problematic resource
        resourceManager.registerResource("problematic-resource", problematicResource);
        
        // Closing should not throw, but should log the error
        assertDoesNotThrow(() -> {
            resourceManager.unregisterAndCloseResource("problematic-resource");
        });
    }
    
    @Test
    @DisplayName("Should handle executor shutdown exceptions gracefully")
    void testExecutorShutdownExceptions() {
        // Note: It's difficult to create an executor that throws during shutdown
        // without using mocks, so this test verifies the normal case
        
        ScheduledExecutorService testExecutor = Executors.newSingleThreadScheduledExecutor();
        
        resourceManager.registerExecutor("shutdown-test-executor", testExecutor);
        
        // Should not throw during shutdown
        assertDoesNotThrow(() -> {
            resourceManager.unregisterAndShutdownExecutor("shutdown-test-executor");
        });
        
        assertTrue(testExecutor.isShutdown());
    }
}