package edu.minecraft.collaboration.test.base;

import edu.minecraft.collaboration.test.categories.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for unit tests that don't require Minecraft runtime.
 * Provides common setup and utilities for pure unit tests.
 */
@UnitTest
public abstract class BaseUnitTest {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @BeforeEach
    void setUpBaseUnitTest(TestInfo testInfo) {
        logger.debug("Starting unit test: {}", testInfo.getDisplayName());
    }
    
    @AfterEach
    void tearDownBaseUnitTest(TestInfo testInfo) {
        logger.debug("Completed unit test: {}", testInfo.getDisplayName());
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