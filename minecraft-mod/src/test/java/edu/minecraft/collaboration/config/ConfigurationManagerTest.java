package edu.minecraft.collaboration.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ConfigurationManager
 */
class ConfigurationManagerTest {
    
    private ConfigurationManager configManager;
    
    @BeforeEach
    void setUp() {
        configManager = new ConfigurationManager();
    }
    
    @AfterEach
    void tearDown() {
        // Clear system properties that might affect tests
        System.clearProperty("websocket.port");
        System.clearProperty("minecraft.collaboration.profile");
    }
    
    @Test
    void testDefaultConfigurationLoading() {
        assertNotNull(configManager);
        assertEquals("default", configManager.getActiveProfile());
    }
    
    @Test
    void testWebSocketPortConfiguration() {
        // Should load default value from application.properties
        int port = configManager.getIntProperty("websocket.port", 0);
        assertEquals(14711, port);
    }
    
    @Test
    void testBooleanPropertyLoading() {
        // Test development mode property
        boolean devMode = configManager.getBooleanProperty("development.mode", false);
        assertTrue(devMode); // Should be true in default config
    }
    
    @Test
    void testStringPropertyLoading() {
        // Test websocket host property
        String host = configManager.getProperty("websocket.host", "unknown");
        assertEquals("localhost", host);
    }
    
    @Test
    void testPropertyWithDefault() {
        // Test a property that doesn't exist
        String nonExistent = configManager.getProperty("non.existent.property", "default-value");
        assertEquals("default-value", nonExistent);
    }
    
    @Test
    void testIntPropertyWithDefault() {
        // Test an int property that doesn't exist
        int nonExistent = configManager.getIntProperty("non.existent.int", 999);
        assertEquals(999, nonExistent);
    }
    
    @Test
    void testSystemPropertyOverride() {
        // Set a system property
        System.setProperty("websocket.port", "15000");
        
        // Should get the system property value
        int port = configManager.getIntProperty("websocket.port", 14711);
        assertEquals(15000, port);
    }
    
    @Test
    void testInvalidIntProperty() {
        // Create a temporary system property with invalid int value
        System.setProperty("test.invalid.int", "not-a-number");
        
        // Should return default value when parsing fails
        int value = configManager.getIntProperty("test.invalid.int", 42);
        assertEquals(42, value);
        
        // Clean up
        System.clearProperty("test.invalid.int");
    }
    
    @Test
    void testLongProperty() {
        long value = configManager.getLongProperty("rate.limiter.cleanup.interval.minutes", 0L);
        assertEquals(1L, value);
    }
    
    @Test
    void testDoubleProperty() {
        double value = configManager.getDoubleProperty("agent.follow.distance", 0.0);
        assertEquals(3.0, value, 0.001);
    }
    
    @Test
    void testHasProperty() {
        assertTrue(configManager.hasProperty("websocket.port"));
        assertFalse(configManager.hasProperty("non.existent.property"));
    }
    
    @Test
    void testPropertyKeys() {
        var keys = configManager.getPropertyKeys();
        assertNotNull(keys);
        assertTrue(keys.size() > 0);
        assertTrue(keys.contains("websocket.port"));
    }
    
    @Test
    void testConfigurationStatistics() {
        var stats = configManager.getStatistics();
        assertNotNull(stats);
        assertEquals("default", stats.getActiveProfile());
        assertTrue(stats.getCachedProperties() > 0);
        assertTrue(stats.getTotalProperties() > 0);
    }
    
    @Test
    void testReload() {
        // Get initial value
        String initialHost = configManager.getProperty("websocket.host", "unknown");
        assertEquals("localhost", initialHost);
        
        // Reload should not fail
        assertDoesNotThrow(() -> configManager.reload());
        
        // Value should still be the same after reload
        String reloadedHost = configManager.getProperty("websocket.host", "unknown");
        assertEquals("localhost", reloadedHost);
    }
    
    @Test
    void testEnvironmentProfileDetection() {
        // Test with system property
        System.setProperty("minecraft.collaboration.profile", "test");
        
        ConfigurationManager testConfigManager = new ConfigurationManager();
        assertEquals("test", testConfigManager.getActiveProfile());
        
        // Clean up
        System.clearProperty("minecraft.collaboration.profile");
    }
}