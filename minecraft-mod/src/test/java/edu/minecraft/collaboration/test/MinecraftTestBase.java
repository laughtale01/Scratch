package edu.minecraft.collaboration.test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for tests requiring Minecraft environment
 */
public abstract class MinecraftTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftTestBase.class);
    private static boolean bootstrapped = false;
    
    @BeforeAll
    public static void setupMinecraftEnvironment() {
        if (!bootstrapped) {
            try {
                LOGGER.info("Bootstrapping Minecraft test environment...");

                // Check if we're in a test environment without Minecraft runtime
                try {
                    Class.forName("net.minecraft.core.registries.BuiltInRegistries");

                    // Initialize shared constants
                    SharedConstants.tryDetectVersion();

                    // Bootstrap Minecraft registries
                    Bootstrap.bootStrap();

                    bootstrapped = true;
                    LOGGER.info("Minecraft test environment ready");
                } catch (ClassNotFoundException e) {
                    LOGGER.warn("Minecraft runtime not available in test environment - tests requiring Minecraft will be skipped");
                    bootstrapped = false;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to bootstrap Minecraft environment", e);
                bootstrapped = false;
                // Continue anyway - some tests might still work without full Minecraft
            }
        }

        // WebSocket server will be started by individual tests if needed
    }
    
    @AfterAll
    public static void teardownMinecraftEnvironment() {
        // Clean up if needed
    }
    
    /**
     * Check if Minecraft is properly bootstrapped
     */
    protected static boolean isBootstrapped() {
        return bootstrapped;
    }
}