package edu.minecraft.collaboration.core;

import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.security.RateLimiter;
import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.monitoring.MetricsCollector;
import edu.minecraft.collaboration.collaboration.CollaborationManager;
import edu.minecraft.collaboration.localization.LanguageManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DependencyInjector system
 */
class DependencyInjectorTest {
    
    private DependencyInjector injector;
    
    @BeforeEach
    void setUp() {
        injector = DependencyInjector.getInstance();
        injector.clearForTesting();
    }
    
    @Test
    @DisplayName("Should create single instance of DependencyInjector")
    void testSingletonInstance() {
        DependencyInjector injector1 = DependencyInjector.getInstance();
        DependencyInjector injector2 = DependencyInjector.getInstance();
        
        assertSame(injector1, injector2);
    }
    
    @Test
    @DisplayName("Should register and retrieve RateLimiter service")
    void testRateLimiterService() {
        // Service should be registered by default
        assertTrue(injector.isServiceRegistered(RateLimiter.class));
        assertFalse(injector.isServiceInitialized(RateLimiter.class));
        
        // Get service - should create instance
        RateLimiter rateLimiter1 = injector.getService(RateLimiter.class);
        assertNotNull(rateLimiter1);
        assertTrue(injector.isServiceInitialized(RateLimiter.class));
        
        // Get again - should return same instance
        RateLimiter rateLimiter2 = injector.getService(RateLimiter.class);
        assertSame(rateLimiter1, rateLimiter2);
    }
    
    @Test
    @DisplayName("Should register and retrieve AuthenticationManager service")
    void testAuthenticationManagerService() {
        assertTrue(injector.isServiceRegistered(AuthenticationManager.class));
        
        AuthenticationManager authManager = injector.getService(AuthenticationManager.class);
        assertNotNull(authManager);
        
        // Test basic functionality
        String token = authManager.generateToken("testUser", AuthenticationManager.UserRole.STUDENT);
        assertNotNull(token);
        assertTrue(authManager.validateToken(token));
    }
    
    @Test
    @DisplayName("Should register and retrieve MetricsCollector service")
    void testMetricsCollectorService() {
        assertTrue(injector.isServiceRegistered(MetricsCollector.class));
        
        MetricsCollector metrics = injector.getService(MetricsCollector.class);
        assertNotNull(metrics);
        
        // Test basic functionality
        metrics.incrementCounter("test.counter");
        metrics.setGauge("test.gauge", 42);
        
        MetricsCollector.MetricsSnapshot snapshot = metrics.getSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.getCounters().containsKey("test.counter"));
        assertEquals(1L, snapshot.getCounters().get("test.counter"));
        assertTrue(snapshot.getGauges().containsKey("test.gauge"));
        assertEquals(42L, snapshot.getGauges().get("test.gauge"));
    }
    
    @Test
    @DisplayName("Should register and retrieve CollaborationManager service")
    void testCollaborationManagerService() {
        assertTrue(injector.isServiceRegistered(CollaborationManager.class));
        
        CollaborationManager collaborationManager = injector.getService(CollaborationManager.class);
        assertNotNull(collaborationManager);
    }
    
    @Test
    @DisplayName("Should register and retrieve LanguageManager service")
    void testLanguageManagerService() {
        assertTrue(injector.isServiceRegistered(LanguageManager.class));
        
        LanguageManager languageManager = injector.getService(LanguageManager.class);
        assertNotNull(languageManager);
        
        // Test basic functionality
        String message = languageManager.getMessage("test.key", "en_US");
        assertNotNull(message);
    }
    
    @Test
    @DisplayName("Should handle custom service registration")
    void testCustomServiceRegistration() {
        // Register a custom service
        injector.registerService(String.class, () -> "test-service");
        
        assertTrue(injector.isServiceRegistered(String.class));
        assertFalse(injector.isServiceInitialized(String.class));
        
        String service = injector.getService(String.class);
        assertEquals("test-service", service);
        assertTrue(injector.isServiceInitialized(String.class));
    }
    
    @Test
    @DisplayName("Should handle instance registration")
    void testInstanceRegistration() {
        String testInstance = "pre-created-instance";
        injector.registerInstance(String.class, testInstance);
        
        assertTrue(injector.isServiceRegistered(String.class));
        assertTrue(injector.isServiceInitialized(String.class));
        
        String retrieved = injector.getService(String.class);
        assertSame(testInstance, retrieved);
    }
    
    @Test
    @DisplayName("Should provide service statistics")
    void testServiceStatistics() {
        DependencyInjector.ServiceStatistics stats = injector.getStatistics();
        assertNotNull(stats);
        
        // Should have 6 core services registered (including ResourceManager)
        assertEquals(6, stats.getRegisteredFactories());
        
        // Initially no services should be initialized
        assertEquals(0, stats.getInitializedServices());
        assertFalse(stats.isShutdown());
        
        // Get a service to initialize it
        injector.getService(RateLimiter.class);
        
        stats = injector.getStatistics();
        // RateLimiter and its dependency ConfigurationManager both get initialized
        assertEquals(2, stats.getInitializedServices());
    }
    
    @Test
    @DisplayName("Should throw exception for unregistered service")
    void testUnregisteredService() {
        assertThrows(IllegalArgumentException.class, () -> {
            injector.getService(java.util.List.class);
        });
    }
    
    @Test
    @DisplayName("Should throw exception for null service instance registration")
    void testNullInstanceRegistration() {
        assertThrows(IllegalArgumentException.class, () -> {
            injector.registerInstance(String.class, null);
        });
    }
    
    @Test
    @DisplayName("Should handle shutdown properly")
    void testShutdown() {
        // Get some services first
        RateLimiter rateLimiter = injector.getService(RateLimiter.class);
        MetricsCollector metrics = injector.getService(MetricsCollector.class);
        
        assertNotNull(rateLimiter);
        assertNotNull(metrics);
        
        // Shutdown
        injector.shutdown();
        
        DependencyInjector.ServiceStatistics stats = injector.getStatistics();
        assertTrue(stats.isShutdown());
        
        // Should throw exception when trying to use after shutdown
        assertThrows(IllegalStateException.class, () -> {
            injector.getService(AuthenticationManager.class);
        });
        
        assertThrows(IllegalStateException.class, () -> {
            injector.registerService(Integer.class, () -> 42);
        });
    }
    
    @Test
    @DisplayName("Should prevent circular dependencies")
    void testCircularDependencyDetection() {
        // This is a theoretical test since our current services don't have circular dependencies
        // But the mechanism should be in place
        
        // Register services that would create circular dependency
        injector.registerService(Integer.class, () -> {
            // This would try to get String service
            injector.getService(String.class);
            return 42;
        });
        
        injector.registerService(String.class, () -> {
            // This would try to get Integer service, creating a circle
            injector.getService(Integer.class);
            return "test";
        });
        
        // Should throw exception when trying to resolve circular dependency
        assertThrows(IllegalStateException.class, () -> {
            injector.getService(Integer.class);
        });
    }
}