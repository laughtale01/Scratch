package edu.minecraft.collaboration.test.base;

import edu.minecraft.collaboration.test.categories.IntegrationTest;
import edu.minecraft.collaboration.test.MinecraftTestBase;
import edu.minecraft.collaboration.test.util.WebSocketTestServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for integration tests that may require Minecraft runtime
 * and/or WebSocket server setup.
 */
@IntegrationTest
public abstract class BaseIntegrationTest extends MinecraftTestBase {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected WebSocketTestServer testServer;
    
    @BeforeEach
    void setUpBaseIntegrationTest(TestInfo testInfo) {
        logger.info("Starting integration test: {}", testInfo.getDisplayName());
        
        // Start WebSocket test server if needed
        if (requiresWebSocketServer()) {
            startTestServer();
        }
    }
    
    @AfterEach
    void tearDownBaseIntegrationTest(TestInfo testInfo) {
        // Stop WebSocket test server if running
        if (testServer != null) {
            stopTestServer();
        }
        
        logger.info("Completed integration test: {}", testInfo.getDisplayName());
    }
    
    /**
     * Override this method to indicate if the test needs WebSocket server
     */
    protected boolean requiresWebSocketServer() {
        return false;
    }
    
    /**
     * Start the WebSocket test server
     */
    protected void startTestServer() {
        try {
            testServer = WebSocketTestServer.startTestServer(14711);
            if (testServer == null) {
                logger.warn("WebSocket test server could not start - tests may be skipped");
                return;
            }
            logger.debug("WebSocket test server started on port {}", testServer.getActualPort());
        } catch (Exception e) {
            logger.error("Failed to start WebSocket test server", e);
            throw new RuntimeException("Cannot start test server", e);
        }
    }
    
    /**
     * Stop the WebSocket test server
     */
    protected void stopTestServer() {
        if (testServer != null) {
            try {
                testServer.stop();
                logger.debug("WebSocket test server stopped");
            } catch (Exception e) {
                logger.warn("Error stopping WebSocket test server", e);
            } finally {
                testServer = null;
            }
        }
    }
    
    /**
     * Get the test server port (only available after server is started)
     */
    protected int getTestServerPort() {
        return testServer != null ? testServer.getActualPort() : -1;
    }
    
    /**
     * Helper method to create test data with consistent patterns
     */
    protected String createTestId(String prefix) {
        return prefix + "_" + System.nanoTime();
    }
    
    /**
     * Helper method for assertion messages
     */
    protected String formatAssertionMessage(String message, Object... args) {
        return String.format("[%s] %s", getClass().getSimpleName(), String.format(message, args));
    }
}