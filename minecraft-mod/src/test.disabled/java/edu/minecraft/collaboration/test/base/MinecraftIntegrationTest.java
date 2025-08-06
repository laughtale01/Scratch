package edu.minecraft.collaboration.test.base;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Testcontainers;
import edu.minecraft.collaboration.test.categories.IntegrationTest;

/**
 * Base class for Minecraft integration tests using TestContainers.
 * Provides a containerized Minecraft server environment for testing.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@IntegrationTest
public abstract class MinecraftIntegrationTest {
    
    // Note: Actual Minecraft container implementation will be added
    // when TestContainers Minecraft module is available or custom container is created
    
    @BeforeAll
    static void setupContainer() {
        // TODO: Initialize Minecraft container
        // This will be implemented when we create the custom Minecraft Docker image
        System.out.println("MinecraftIntegrationTest: Container setup placeholder");
    }
    
    /**
     * Get the Minecraft server address for testing
     * @return Server address in format "host:port"
     */
    protected String getServerAddress() {
        // TODO: Return actual container address
        return "localhost:25565";
    }
    
    /**
     * Get the WebSocket server address for testing
     * @return WebSocket address in format "ws://host:port"
     */
    protected String getWebSocketAddress() {
        // TODO: Return actual WebSocket address from container
        return "ws://localhost:14711";
    }
}