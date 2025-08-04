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
                
                // Initialize shared constants
                SharedConstants.tryDetectVersion();
                
                // Bootstrap Minecraft registries
                Bootstrap.bootStrap();
                
                bootstrapped = true;
                LOGGER.info("Minecraft test environment ready");
            } catch (Exception e) {
                LOGGER.error("Failed to bootstrap Minecraft environment", e);
                // Continue anyway - some tests might still work
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