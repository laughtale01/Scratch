package edu.minecraft.collaboration.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RateLimiter
 */
public class RateLimiterTest {
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    public void setUp() {
        rateLimiter = RateLimiter.getInstance();
    }
    
    @AfterEach
    public void tearDown() {
        // Reset limits after each test
        rateLimiter.resetLimit("testPlayer");
    }
    
    @Test
    public void testAllowCommandUnderLimit() {
        // Test that commands are allowed under the rate limit
        String identifier = "testPlayer";
        
        // Should allow up to 10 commands per second
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.allowCommand(identifier), 
                "Command " + (i + 1) + " should be allowed");
        }
    }
    
    @Test
    public void testBlockCommandOverLimit() {
        // Test that commands are blocked over the rate limit
        String identifier = "testPlayer";
        
        // Send 10 commands (the limit)
        for (int i = 0; i < 10; i++) {
            rateLimiter.allowCommand(identifier);
        }
        
        // The 11th command should be blocked
        assertFalse(rateLimiter.allowCommand(identifier), 
            "11th command should be blocked");
    }
    
    @Test
    public void testRateLimitResetAfterWindow() throws InterruptedException {
        // Test that rate limit resets after time window
        String identifier = "testPlayer";
        
        // Fill up the rate limit
        for (int i = 0; i < 10; i++) {
            rateLimiter.allowCommand(identifier);
        }
        
        // Should be blocked
        assertFalse(rateLimiter.allowCommand(identifier));
        
        // Wait for window to reset (1.1 seconds to be safe)
        Thread.sleep(1100);
        
        // Should be allowed again
        assertTrue(rateLimiter.allowCommand(identifier), 
            "Command should be allowed after window reset");
    }
    
    @Test
    public void testMultipleIdentifiersSeparateLimits() {
        // Test that different identifiers have separate limits
        String player1 = "player1";
        String player2 = "player2";
        
        // Fill up player1's limit
        for (int i = 0; i < 10; i++) {
            rateLimiter.allowCommand(player1);
        }
        
        // Player1 should be blocked
        assertFalse(rateLimiter.allowCommand(player1));
        
        // Player2 should still be allowed
        assertTrue(rateLimiter.allowCommand(player2), 
            "Different player should have separate limit");
    }
    
    @Test
    public void testGetCurrentCommandCount() {
        String identifier = "testPlayer";
        
        // Initially should be 0
        assertEquals(0, rateLimiter.getCurrentCommandCount(identifier));
        
        // Send 5 commands
        for (int i = 0; i < 5; i++) {
            rateLimiter.allowCommand(identifier);
        }
        
        // Should be 5
        assertEquals(5, rateLimiter.getCurrentCommandCount(identifier));
    }
    
    @Test
    public void testResetLimit() {
        String identifier = "testPlayer";
        
        // Send some commands
        for (int i = 0; i < 5; i++) {
            rateLimiter.allowCommand(identifier);
        }
        
        // Should have count of 5
        assertEquals(5, rateLimiter.getCurrentCommandCount(identifier));
        
        // Reset the limit
        rateLimiter.resetLimit(identifier);
        
        // Should be back to 0
        assertEquals(0, rateLimiter.getCurrentCommandCount(identifier));
    }
}