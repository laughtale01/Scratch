package edu.minecraft.collaboration.test.mocks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up mocked environment for command handler tests
 */
public class MockedCommandEnvironment {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockedCommandEnvironment.class);
    private static boolean initialized = false;
    
    public static void setup() {
        if (!initialized) {
            try {
                // Mock Minecraft Bootstrap if needed
                Class<?> bootstrapClass = Class.forName("net.minecraft.server.Bootstrap");
                java.lang.reflect.Field checkField = bootstrapClass.getDeclaredField("CHECK");
                checkField.setAccessible(true);
                
                // Remove final modifier
                java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(checkField, checkField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
                
                // Set to true
                checkField.set(null, true);
                
                initialized = true;
                LOGGER.info("Mocked command environment initialized");
            } catch (Exception e) {
                LOGGER.warn("Could not mock Bootstrap, continuing anyway: {}", e.getMessage());
            }
        }
    }
}