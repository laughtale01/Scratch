package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.security.RateLimiter;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.test.categories.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for RateLimiter
 */
@DisplayName("RateLimiter Tests")
@UnitTest
public class RateLimiterTest {
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = DependencyInjector.getInstance().getService(RateLimiter.class);
    }
    
    @Test
    @DisplayName("Should allow commands within rate limit")
    void testAllowCommandsWithinLimit() {
        // Given
        String identifier = "test-player";

        // When & Then - Rate limit is 20 commands/second
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimiter.allowCommand(identifier),
                "Command " + (i + 1) + " should be allowed");
        }
    }
    
    @Test
    @DisplayName("Should block commands exceeding rate limit")
    void testBlockCommandsExceedingLimit() {
        // Given
        String identifier = "spammer";
        
        // When - Send exactly the limit + 1 command
        int allowed = 0;
        int blocked = 0;
        
        // Rate limit is 20 commands/second (from application.properties)
        // First, send exactly the rate limit number of commands
        for (int i = 0; i < 20; i++) {
            if (rateLimiter.allowCommand(identifier)) {
                allowed++;
            }
        }

        // Then send one more command - this should be blocked
        if (!rateLimiter.allowCommand(identifier)) {
            blocked++;
        }

        // Then
        assertEquals(20, allowed, "Should allow exactly the rate limit");
        assertTrue(blocked > 0, "Should not allow more than rate limit");
    }
    
    @Test
    @DisplayName("Should reset rate limit after time window")
    void testRateLimitReset() throws InterruptedException {
        // Given
        String identifier = "reset-test";
        
        // Fill up the rate limit
        for (int i = 0; i < 20; i++) {
            rateLimiter.allowCommand(identifier);
        }
        
        // Then - Should be blocked
        assertFalse(rateLimiter.allowCommand(identifier));
        
        // When - Wait for window to reset
        Thread.sleep(1100); // Wait just over 1 second
        
        // Then - Should be allowed again
        assertTrue(rateLimiter.allowCommand(identifier));
    }
    
    @Test
    @DisplayName("Should track different identifiers separately")
    void testSeparateIdentifierTracking() {
        // Given
        String player1 = "player1";
        String player2 = "player2";
        
        // When - Fill up rate limit for player1
        for (int i = 0; i < 20; i++) {
            rateLimiter.allowCommand(player1);
        }
        
        // Then
        assertFalse(rateLimiter.allowCommand(player1), "Player1 should be blocked");
        assertTrue(rateLimiter.allowCommand(player2), "Player2 should still be allowed");
    }
    
    @Test
    @DisplayName("Should get current command count")
    void testGetCurrentCommandCount() {
        // Given
        String identifier = "count-test";
        
        // When
        assertEquals(0, rateLimiter.getCurrentCommandCount(identifier));
        
        rateLimiter.allowCommand(identifier);
        assertEquals(1, rateLimiter.getCurrentCommandCount(identifier));
        
        rateLimiter.allowCommand(identifier);
        rateLimiter.allowCommand(identifier);
        assertEquals(3, rateLimiter.getCurrentCommandCount(identifier));
    }
    
    @Test
    @DisplayName("Should reset limit manually")
    void testManualReset() {
        // Given
        String identifier = "manual-reset";
        
        // Fill up some commands
        for (int i = 0; i < 5; i++) {
            rateLimiter.allowCommand(identifier);
        }
        assertEquals(5, rateLimiter.getCurrentCommandCount(identifier));
        
        // When
        rateLimiter.resetLimit(identifier);
        
        // Then
        assertEquals(0, rateLimiter.getCurrentCommandCount(identifier));
    }
}