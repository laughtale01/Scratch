package edu.minecraft.collaboration.integration;

import edu.minecraft.collaboration.config.ConfigurationManager;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.security.RateLimiter;
import edu.minecraft.collaboration.security.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for configuration system with other components
 */
class ConfigurationIntegrationTest {
    
    private DependencyInjector injector;
    
    @BeforeEach 
    void setUp() {
        injector = DependencyInjector.getInstance();
        injector.clearForTesting();
    }
    
    @AfterEach
    void tearDown() {
        injector.clearForTesting();
        System.clearProperty("security.command.rate.limit.per.second");
        System.clearProperty("websocket.port");
    }
    
    @Test
    void testConfigurationManagerRegistration() {
        // ConfigurationManager should be registered in DependencyInjector
        assertTrue(injector.isServiceRegistered(ConfigurationManager.class));
        
        // Should be able to get instance
        ConfigurationManager config = injector.getService(ConfigurationManager.class);
        assertNotNull(config);
    }
    
    @Test
    void testRateLimiterWithConfiguration() {
        // Create RateLimiter with configuration dependency
        RateLimiter rateLimiter = injector.getService(RateLimiter.class);
        assertNotNull(rateLimiter);
        
        // Test that it uses configured values
        String testUser = "testUser";
        
        // Should allow commands up to the configured limit
        for (int i = 0; i < 20; i++) { // Default limit is 20 (from application.properties)
            assertTrue(rateLimiter.allowCommand(testUser), "Command " + i + " should be allowed");
        }

        // 21st command should be blocked
        assertFalse(rateLimiter.allowCommand(testUser), "21st command should be blocked");
    }
    
    @Test
    void testRateLimiterWithSystemPropertyOverride() {
        // Set system property to override rate limit
        System.setProperty("security.command.rate.limit.per.second", "5");
        
        // Clear injector to force recreation
        injector.clearForTesting();
        
        // Create new RateLimiter with overridden configuration
        RateLimiter rateLimiter = injector.getService(RateLimiter.class);
        assertNotNull(rateLimiter);
        
        String testUser = "testUser2";
        
        // Should allow commands up to the overridden limit (5)
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.allowCommand(testUser), "Command " + i + " should be allowed");
        }
        
        // 6th command should be blocked
        assertFalse(rateLimiter.allowCommand(testUser), "6th command should be blocked");
    }
    
    @Test
    void testSecurityConfigWithConfiguration() {
        // Test that SecurityConfig uses configuration
        int port = SecurityConfig.getWebSocketPort();
        assertEquals(14711, port); // Default value
        
        String host = SecurityConfig.getWebSocketHost();
        assertEquals("localhost", host);
        
        int maxConnections = SecurityConfig.getMaxConnections();
        assertEquals(10, maxConnections);
    }
    
    @Test
    void testSecurityConfigWithSystemPropertyOverride() {
        // Override websocket port via system property
        System.setProperty("websocket.port", "15555");
        
        // Clear cached config manager in SecurityConfig by creating new instance
        injector.clearForTesting();
        
        // Should use overridden value
        int port = SecurityConfig.getWebSocketPort();
        assertEquals(15555, port);
    }
    
    @Test
    void testBlockAndCommandFiltering() {
        // Test block filtering with configuration
        assertTrue(SecurityConfig.isBlockAllowed("stone"));
        assertTrue(SecurityConfig.isBlockAllowed("dirt"));
        assertFalse(SecurityConfig.isBlockAllowed("tnt"));
        assertFalse(SecurityConfig.isBlockAllowed("minecraft:tnt"));
        
        // Test command filtering with configuration
        assertTrue(SecurityConfig.isCommandAllowed("help"));
        assertTrue(SecurityConfig.isCommandAllowed("time"));
        assertFalse(SecurityConfig.isCommandAllowed("op"));
        assertFalse(SecurityConfig.isCommandAllowed("stop"));
    }
    
    @Test
    void testIPAddressFiltering() {
        // Test IP address filtering with configuration
        assertTrue(SecurityConfig.isAddressAllowed("localhost"));
        assertTrue(SecurityConfig.isAddressAllowed("127.0.0.1"));
        assertTrue(SecurityConfig.isAddressAllowed("::1"));
        
        // Private networks should be allowed by default
        assertTrue(SecurityConfig.isAddressAllowed("192.168.1.1"));
        assertTrue(SecurityConfig.isAddressAllowed("10.0.0.1"));
        assertTrue(SecurityConfig.isAddressAllowed("172.16.0.1"));
    }
    
    @Test
    void testChatMessageSanitization() {
        // Test chat message sanitization uses configured length limit
        String longMessage = "a".repeat(1000); // Longer than default limit of 256
        String sanitized = SecurityConfig.sanitizeChatMessage(longMessage);
        
        assertTrue(sanitized.length() <= 256, "Sanitized message should not exceed configured limit");
        
        // Test control character removal
        String messageWithControlChars = "Hello\u0000World\u0001Test";
        String cleaned = SecurityConfig.sanitizeChatMessage(messageWithControlChars);
        assertEquals("HelloWorldTest", cleaned);
    }
    
    @Test
    void testMultipleConfigurationInstances() {
        // Test that multiple instances of ConfigurationManager behave consistently
        ConfigurationManager config1 = injector.getService(ConfigurationManager.class);
        ConfigurationManager config2 = injector.getService(ConfigurationManager.class);
        
        // Should be the same instance (singleton behavior through DI)
        assertSame(config1, config2);
        
        // Should return same values
        assertEquals(config1.getProperty("websocket.host"), config2.getProperty("websocket.host"));
        assertEquals(config1.getIntProperty("websocket.port", 0), config2.getIntProperty("websocket.port", 0));
    }
}