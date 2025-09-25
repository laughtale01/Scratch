package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.constants.ErrorConstants;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Security tests to validate the implementation
 * These tests help ensure the security measures are working correctly
 */
public final class SecurityTests {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    // Using constants from ErrorConstants class

    // Test constants
    private static final String LOCALHOST_IP = "127.0.0.1";
    private static final String EXTERNAL_IP = "8.8.8.8";

    private SecurityTests() {
        throw new UnsupportedOperationException("Utility class");
    }

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
            LOGGER.info("All security tests {}", ErrorConstants.PASSED);
        } else {
            LOGGER.error("Some security tests {}", ErrorConstants.FAILED);
        }

        return allPassed;
    }

    /**
     * Test input validation
     */
    private static boolean testInputValidation() {
        LOGGER.info("Testing input validation...");

        boolean passed = true;

        passed &= testUsernameValidation();
        passed &= testCoordinateValidation();
        passed &= testBlockTypeValidation();
        passed &= testChatMessageValidation();

        LOGGER.info("Input validation tests: {}", passed ? ErrorConstants.PASSED : ErrorConstants.FAILED);
        return passed;
    }

    /**
     * Test username validation rules
     */
    private static boolean testUsernameValidation() {
        boolean passed = true;

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

        return passed;
    }

    /**
     * Test coordinate validation rules
     */
    private static boolean testCoordinateValidation() {
        boolean passed = true;

        if (!InputValidator.validateCoordinates("100", "64", "-50")) {
            LOGGER.error("Valid coordinates rejected");
            passed = false;
        }

        if (InputValidator.validateCoordinates("50000000", "64", "0")) {
            LOGGER.error("Out of bounds coordinates accepted");
            passed = false;
        }

        return passed;
    }

    /**
     * Test block type validation rules
     */
    private static boolean testBlockTypeValidation() {
        boolean passed = true;

        if (!InputValidator.validateBlockType("minecraft:stone")) {
            LOGGER.error("Valid block type rejected");
            passed = false;
        }

        if (InputValidator.validateBlockType("tnt")) {
            LOGGER.error("Blocked block type accepted");
            passed = false;
        }

        return passed;
    }

    /**
     * Test chat message validation rules
     */
    private static boolean testChatMessageValidation() {
        boolean passed = true;

        final String validMessage = InputValidator.validateChatMessage("Hello world!");
        if (validMessage == null) {
            LOGGER.error("Valid chat message rejected");
            passed = false;
        }

        final String invalidMessage = InputValidator.validateChatMessage("<script>alert('hack')</script>");
        if (invalidMessage != null) {
            LOGGER.error("Dangerous chat message accepted");
            passed = false;
        }

        return passed;
    }

    /**
     * Test authentication system
     */
    private static boolean testAuthentication() {
        LOGGER.info("Testing authentication...");

        boolean passed = true;
        final AuthenticationManager authManager = DependencyInjector.getInstance().getService(AuthenticationManager.class);

        try {
            passed &= testTokenOperations(authManager);

        } catch (SecurityException | IllegalArgumentException e) {
            LOGGER.error("Authentication test failed with expected exception", e);
            passed = false;
        }

        LOGGER.info("Authentication tests: {}", passed ? ErrorConstants.PASSED : ErrorConstants.FAILED);
        return passed;
    }

    /**
     * Test token generation, validation, and cleanup operations
     */
    private static boolean testTokenOperations(final AuthenticationManager authManager) {
        boolean passed = true;

        // Test token generation
        final String token = authManager.generateToken("testUser", AuthenticationManager.UserRole.STUDENT);
        if (token == null || token.isEmpty()) {
            LOGGER.error("Token generation failed");
            return false;
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
        final AuthenticationManager.UserRole role = authManager.getRoleForConnection("testConn");
        if (role != AuthenticationManager.UserRole.STUDENT) {
            LOGGER.error("Incorrect role returned");
            passed = false;
        }

        // Cleanup
        authManager.removeConnection("testConn");
        authManager.revokeToken(token);

        return passed;
    }

    /**
     * Test rate limiting
     */
    private static boolean testRateLimiting() {
        LOGGER.info("Testing rate limiting...");

        boolean passed = true;
        final RateLimiter rateLimiter = DependencyInjector.getInstance().getService(RateLimiter.class);

        try {
            passed &= testRateLimitEnforcement(rateLimiter);

        } catch (SecurityException | IllegalArgumentException e) {
            LOGGER.error("Rate limiting test failed with expected exception", e);
            passed = false;
        }

        LOGGER.info("Rate limiting tests: {}", passed ? ErrorConstants.PASSED : ErrorConstants.FAILED);
        return passed;
    }

    /**
     * Test rate limit enforcement and reset functionality
     */
    private static boolean testRateLimitEnforcement(final RateLimiter rateLimiter) {
        boolean passed = true;
        final String testIdentifier = "testRateLimit";

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

        return passed;
    }

    /**
     * Test security configuration
     */
    private static boolean testSecurityConfig() {
        LOGGER.info("Testing security configuration...");

        boolean passed = true;

        passed &= testAddressValidation();
        passed &= testCommandBlocking();
        passed &= testBlockBlocking();
        passed &= testChatSanitization();

        LOGGER.info("Security configuration tests: {}", passed ? ErrorConstants.PASSED : ErrorConstants.FAILED);
        return passed;
    }

    /**
     * Test IP address validation rules
     */
    private static boolean testAddressValidation() {
        boolean passed = true;

        if (!SecurityConfig.isAddressAllowed(LOCALHOST_IP)) {
            LOGGER.error("Localhost not allowed");
            passed = false;
        }

        if (SecurityConfig.isAddressAllowed(EXTERNAL_IP)) {
            LOGGER.error("External address allowed");
            passed = false;
        }

        return passed;
    }

    /**
     * Test dangerous command blocking
     */
    private static boolean testCommandBlocking() {
        boolean passed = true;
        final List<String> dangerousCommands = Arrays.asList("op", "stop", "execute", "function");

        for (final String command : dangerousCommands) {
            if (SecurityConfig.isCommandAllowed(command)) {
                LOGGER.error("Dangerous command '{}' allowed", command);
                passed = false;
            }
        }

        return passed;
    }

    /**
     * Test dangerous block blocking
     */
    private static boolean testBlockBlocking() {
        boolean passed = true;
        final List<String> dangerousBlocks = Arrays.asList("tnt", "minecraft:tnt", "end_crystal");

        for (final String block : dangerousBlocks) {
            if (SecurityConfig.isBlockAllowed(block)) {
                LOGGER.error("Dangerous block '{}' allowed", block);
                passed = false;
            }
        }

        return passed;
    }

    /**
     * Test chat message sanitization
     */
    private static boolean testChatSanitization() {
        boolean passed = true;
        final String dirtyMessage = "Hello\nworld\r\nwith\tcontrol\bchars";
        final String cleanMessage = SecurityConfig.sanitizeChatMessage(dirtyMessage);

        if (cleanMessage.contains("\n") || cleanMessage.contains("\r") || cleanMessage.contains("\t")) {
            LOGGER.error("Chat sanitization failed");
            passed = false;
        }

        return passed;
    }

    /**
     * Get security statistics for monitoring
     */
    public static Map<String, Object> getSecurityStatistics() {
        final Map<String, Object> stats = new HashMap<>();

        // Authentication stats
        final AuthenticationManager authManager = DependencyInjector.getInstance().getService(AuthenticationManager.class);
        stats.putAll(authManager.getStatistics());

        // Security config info
        stats.put("maxConnections", SecurityConfig.MAX_CONNECTIONS);
        stats.put("commandRateLimit", SecurityConfig.COMMAND_RATE_LIMIT_PER_SECOND);
        stats.put("maxCommandLength", SecurityConfig.MAX_COMMAND_LENGTH);
        stats.put("connectionTimeout", SecurityConfig.CONNECTION_TIMEOUT_MS);

        return stats;
    }
}
