package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Security tests to validate the implementation
 * These tests help ensure the security measures are working correctly
 */
public class SecurityTests {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    /**
     * Run all security tests
     * @return true if all tests pass
     */
    public static boolean runSecurityTests() {
        LOGGER.info("Starting security validation tests...");
        
        boolean allPassed = true;
        
        allPassed &= testInputValidation();
        allPassed &= testAuthentication();
        allPassed &= testRateLimiting();
        allPassed &= testSecurityConfig();
        
        if (allPassed) {
            LOGGER.info("All security tests PASSED");
        } else {
            LOGGER.error("Some security tests FAILED");
        }
        
        return allPassed;
    }
    
    /**
     * Test input validation
     */
    private static boolean testInputValidation() {
        LOGGER.info("Testing input validation...");
        
        boolean passed = true;
        
        // Test username validation
        if (!InputValidator.validateUsername("validUser123")) {
            LOGGER.error("Valid username rejected");
            passed = false;
        }
        
        if (InputValidator.validateUsername("invalid<script>")) {
            LOGGER.error("Invalid username accepted");
            passed = false;
        }
        
        if (InputValidator.validateUsername("verylongusernamethatexceedsthelimit")) {
            LOGGER.error("Too long username accepted");
            passed = false;
        }
        
        // Test coordinate validation
        if (!InputValidator.validateCoordinates("100", "64", "-50")) {
            LOGGER.error("Valid coordinates rejected");
            passed = false;
        }
        
        if (InputValidator.validateCoordinates("50000000", "64", "0")) {
            LOGGER.error("Out of bounds coordinates accepted");
            passed = false;
        }
        
        // Test block type validation
        if (!InputValidator.validateBlockType("minecraft:stone")) {
            LOGGER.error("Valid block type rejected");
            passed = false;
        }
        
        if (InputValidator.validateBlockType("tnt")) {
            LOGGER.error("Blocked block type accepted");
            passed = false;
        }
        
        // Test chat message validation
        String validMessage = InputValidator.validateChatMessage("Hello world!");
        if (validMessage == null) {
            LOGGER.error("Valid chat message rejected");
            passed = false;
        }
        
        String invalidMessage = InputValidator.validateChatMessage("<script>alert('hack')</script>");
        if (invalidMessage != null) {
            LOGGER.error("Dangerous chat message accepted");
            passed = false;
        }
        
        LOGGER.info("Input validation tests: {}", passed ? "PASSED" : "FAILED");
        return passed;
    }
    
    /**
     * Test authentication system
     */
    private static boolean testAuthentication() {
        LOGGER.info("Testing authentication...");
        
        boolean passed = true;
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        
        try {
            // Test token generation
            String token = authManager.generateToken("testUser", AuthenticationManager.UserRole.STUDENT);
            if (token == null || token.isEmpty()) {
                LOGGER.error("Token generation failed");
                passed = false;
            }
            
            // Test token validation
            if (!authManager.validateToken(token)) {
                LOGGER.error("Valid token rejected");
                passed = false;
            }
            
            if (authManager.validateToken("invalidToken123")) {
                LOGGER.error("Invalid token accepted");
                passed = false;
            }
            
            // Test connection authentication
            if (!authManager.authenticateConnection("testConn", token)) {
                LOGGER.error("Connection authentication failed");
                passed = false;
            }
            
            // Test role checking
            AuthenticationManager.UserRole role = authManager.getRoleForConnection("testConn");
            if (role != AuthenticationManager.UserRole.STUDENT) {
                LOGGER.error("Incorrect role returned");
                passed = false;
            }
            
            // Cleanup
            authManager.removeConnection("testConn");
            authManager.revokeToken(token);
            
        } catch (Exception e) {
            LOGGER.error("Authentication test failed with exception", e);
            passed = false;
        }
        
        LOGGER.info("Authentication tests: {}", passed ? "PASSED" : "FAILED");
        return passed;
    }
    
    /**
     * Test rate limiting
     */
    private static boolean testRateLimiting() {
        LOGGER.info("Testing rate limiting...");
        
        boolean passed = true;
        RateLimiter rateLimiter = RateLimiter.getInstance();
        
        try {
            String testIdentifier = "testRateLimit";
            
            // Test normal usage - should be allowed
            for (int i = 0; i < SecurityConfig.COMMAND_RATE_LIMIT_PER_SECOND - 1; i++) {
                if (!rateLimiter.allowCommand(testIdentifier)) {
                    LOGGER.error("Rate limit triggered too early at command {}", i);
                    passed = false;
                    break;
                }
            }
            
            // Test rate limit exceeded
            if (rateLimiter.allowCommand(testIdentifier)) {
                LOGGER.error("Rate limit not enforced");
                passed = false;
            }
            
            // Reset and test again
            rateLimiter.resetLimit(testIdentifier);
            if (!rateLimiter.allowCommand(testIdentifier)) {
                LOGGER.error("Rate limit reset failed");
                passed = false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Rate limiting test failed with exception", e);
            passed = false;
        }
        
        LOGGER.info("Rate limiting tests: {}", passed ? "PASSED" : "FAILED");
        return passed;
    }
    
    /**
     * Test security configuration
     */
    private static boolean testSecurityConfig() {
        LOGGER.info("Testing security configuration...");
        
        boolean passed = true;
        
        // Test allowed addresses
        if (!SecurityConfig.isAddressAllowed("127.0.0.1")) {
            LOGGER.error("Localhost not allowed");
            passed = false;
        }
        
        if (SecurityConfig.isAddressAllowed("8.8.8.8")) {
            LOGGER.error("External address allowed");
            passed = false;
        }
        
        // Test blocked commands
        List<String> dangerousCommands = Arrays.asList("op", "stop", "execute", "function");
        for (String command : dangerousCommands) {
            if (SecurityConfig.isCommandAllowed(command)) {
                LOGGER.error("Dangerous command '{}' allowed", command);
                passed = false;
            }
        }
        
        // Test blocked blocks
        List<String> dangerousBlocks = Arrays.asList("tnt", "minecraft:tnt", "end_crystal");
        for (String block : dangerousBlocks) {
            if (SecurityConfig.isBlockAllowed(block)) {
                LOGGER.error("Dangerous block '{}' allowed", block);
                passed = false;
            }
        }
        
        // Test chat sanitization
        String dirtyMessage = "Hello\nworld\r\nwith\tcontrol\bchars";
        String cleanMessage = SecurityConfig.sanitizeChatMessage(dirtyMessage);
        if (cleanMessage.contains("\n") || cleanMessage.contains("\r") || cleanMessage.contains("\t")) {
            LOGGER.error("Chat sanitization failed");
            passed = false;
        }
        
        LOGGER.info("Security configuration tests: {}", passed ? "PASSED" : "FAILED");
        return passed;
    }
    
    /**
     * Get security statistics for monitoring
     */
    public static Map<String, Object> getSecurityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Authentication stats
        AuthenticationManager authManager = AuthenticationManager.getInstance();
        stats.putAll(authManager.getStatistics());
        
        // Security config info
        stats.put("maxConnections", SecurityConfig.MAX_CONNECTIONS);
        stats.put("commandRateLimit", SecurityConfig.COMMAND_RATE_LIMIT_PER_SECOND);
        stats.put("maxCommandLength", SecurityConfig.MAX_COMMAND_LENGTH);
        stats.put("connectionTimeout", SecurityConfig.CONNECTION_TIMEOUT_MS);
        
        return stats;
    }
}