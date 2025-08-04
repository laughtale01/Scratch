package edu.minecraft.collaboration.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for integration tests that require a Minecraft server
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
@Tag("integration")
public abstract class MinecraftIntegrationTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftIntegrationTest.class);
    
    // Minecraft server container configuration
    protected static final int MINECRAFT_PORT = 25565;
    protected static final int RCON_PORT = 25575;
    protected static final int WEBSOCKET_PORT = 14711;
    
    /**
     * Minecraft server container
     * Using a generic container for now as specific Minecraft TestContainers module
     * may not support Forge 1.20.1 yet
     */
    @Container
    protected static final GenericContainer<?> minecraftServer = new GenericContainer<>(
            DockerImageName.parse("itzg/minecraft-server:java17")
    )
            .withExposedPorts(MINECRAFT_PORT, RCON_PORT, WEBSOCKET_PORT)
            .withEnv("EULA", "TRUE")
            .withEnv("TYPE", "FORGE")
            .withEnv("VERSION", "1.20.1")
            .withEnv("FORGE_VERSION", "47.2.0")
            .withEnv("MEMORY", "2G")
            .withEnv("ENABLE_RCON", "true")
            .withEnv("RCON_PASSWORD", "test")
            .withEnv("RCON_PORT", String.valueOf(RCON_PORT))
            .withEnv("ONLINE_MODE", "false")
            .withCommand("/start");
    
    @BeforeAll
    static void setupContainer() {
        LOGGER.info("Starting Minecraft server container for integration tests...");
        
        // Log container output for debugging
        minecraftServer.followOutput(outputFrame -> 
            LOGGER.debug("[Minecraft] {}", outputFrame.getUtf8String().trim())
        );
    }
    
    /**
     * Get the host port mapped to the Minecraft server port
     */
    protected int getMinecraftPort() {
        return minecraftServer.getMappedPort(MINECRAFT_PORT);
    }
    
    /**
     * Get the host port mapped to the RCON port
     */
    protected int getRconPort() {
        return minecraftServer.getMappedPort(RCON_PORT);
    }
    
    /**
     * Get the host port mapped to the WebSocket port
     */
    protected int getWebSocketPort() {
        return minecraftServer.getMappedPort(WEBSOCKET_PORT);
    }
    
    /**
     * Get the container host address
     */
    protected String getContainerHost() {
        return minecraftServer.getHost();
    }
    
    /**
     * Get the full Minecraft server address
     */
    protected String getMinecraftAddress() {
        return getContainerHost() + ":" + getMinecraftPort();
    }
    
    /**
     * Get the WebSocket URL for testing
     */
    protected String getWebSocketUrl() {
        return "ws://" + getContainerHost() + ":" + getWebSocketPort();
    }
    
    /**
     * Wait for the Minecraft server to be ready
     * This can be overridden in subclasses for specific readiness checks
     */
    protected void waitForServerReady() {
        try {
            // Basic wait - can be improved with actual server readiness check
            Thread.sleep(30000); // 30 seconds
            LOGGER.info("Minecraft server should be ready");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for server", e);
        }
    }
}