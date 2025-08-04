package edu.minecraft.collaboration.test.mocks;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * JUnit 5 extension to mock Minecraft Bootstrap for tests
 */
public class MockMinecraftBootstrap implements BeforeAllCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockMinecraftBootstrap.class);
    private static boolean initialized = false;
    
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!initialized) {
            try {
                // Mock the Bootstrap.bootStrap() method to prevent initialization errors
                Class<?> bootstrapClass = Class.forName("net.minecraft.server.Bootstrap");
                Field checkBootstrapField = bootstrapClass.getDeclaredField("CHECK");
                checkBootstrapField.setAccessible(true);
                
                // Remove final modifier
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(checkBootstrapField, checkBootstrapField.getModifiers() & ~Modifier.FINAL);
                
                // Set to true to indicate already bootstrapped
                checkBootstrapField.set(null, true);
                
                initialized = true;
                LOGGER.info("Mocked Minecraft Bootstrap for testing");
            } catch (Exception e) {
                LOGGER.warn("Could not mock Bootstrap, tests may fail: {}", e.getMessage());
            }
        }
    }
}