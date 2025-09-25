package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.constants.ErrorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates WebSocket messages for authentication and content requirements
 */
public class WebSocketMessageValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketMessageValidator.class);

    private final AuthenticationManager authManager;
    private final int maxCommandLength;
    private final boolean developmentMode;
    private String lastValidationError;

    public WebSocketMessageValidator(final AuthenticationManager authManager,
                                   final int maxCommandLength,
                                   final boolean developmentMode) {
        this.authManager = authManager;
        this.maxCommandLength = maxCommandLength;
        this.developmentMode = developmentMode;
    }

    /**
     * Default constructor for test compatibility
     */
    public WebSocketMessageValidator() {
        this.authManager = null;
        this.maxCommandLength = 1024;
        this.developmentMode = false;
    }

    /**
     * Validation result containing error information if validation fails
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorType;
        private final String errorMessage;

        private ValidationResult(final boolean valid, final String errorType, final String errorMessage) {
            this.valid = valid;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult failure(final String errorType, final String errorMessage) {
            return new ValidationResult(false, errorType, errorMessage);
        }

        public boolean isValid() { return valid; }
        public String getErrorType() { return errorType; }
        public String getErrorMessage() { return errorMessage; }

        public String toJsonResponse() {
            return String.format(ErrorConstants.JSON_ERROR_TEMPLATE, errorType, errorMessage);
        }
    }

    /**
     * Validate message length
     */
    public ValidationResult validateMessageLength(final String message) {
        if (message == null || message.length() > maxCommandLength) {
            return ValidationResult.failure(ErrorConstants.ERROR_INVALID_MESSAGE_LENGTH, ErrorConstants.MSG_MESSAGE_TOO_LONG);
        }
        return ValidationResult.success();
    }

    /**
     * Validate authentication for non-auth commands
     */
    public ValidationResult validateAuthentication(final String message, final String identifier) {
        // Skip auth validation for auth commands
        if (message.contains("\"command\":\"auth\"")) {
            return ValidationResult.success();
        }

        // Skip authentication check if in development mode
        if (developmentMode) {
            LOGGER.info("DEVELOPMENT MODE: Bypassing authentication check for command from {}", identifier);
            return ValidationResult.success();
        }

        boolean isAuthenticated = authManager.isAuthenticated(identifier);

        // Also check username-based connection IDs
        if (!isAuthenticated) {
            isAuthenticated = checkAlternativeAuthentication(message);
        }

        if (!isAuthenticated) {
            LOGGER.warn("Unauthenticated request from {}", identifier);
            return ValidationResult.failure(ErrorConstants.ERROR_UNAUTHENTICATED, ErrorConstants.MSG_AUTHENTICATE_FIRST);
        }

        return ValidationResult.success();
    }

    /**
     * Check alternative authentication methods
     */
    private boolean checkAlternativeAuthentication(final String message) {
        try {
            if (message.contains("\"username\"")) {
                final String username = extractUsernameFromMessage(message);
                if (username != null) {
                    final String altIdentifier = ErrorConstants.CONNECTION_PREFIX + username;
                    final boolean isAuthenticated = authManager.isAuthenticated(altIdentifier);
                    LOGGER.debug("Checking alternative identifier: {} -> {}", altIdentifier, isAuthenticated);
                    return isAuthenticated;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error extracting username for auth check: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract username from JSON message
     */
    private String extractUsernameFromMessage(final String message) {
        // Simple JSON parsing for username field
        final int usernameIndex = message.indexOf("\"username\"");
        if (usernameIndex == -1) {
            return null;
        }

        final int colonIndex = message.indexOf(":", usernameIndex);
        if (colonIndex == -1) {
            return null;
        }

        final int startQuoteIndex = message.indexOf("\"", colonIndex);
        if (startQuoteIndex == -1) {
            return null;
        }

        final int endQuoteIndex = message.indexOf("\"", startQuoteIndex + 1);
        if (endQuoteIndex == -1) {
            return null;
        }

        return message.substring(startQuoteIndex + 1, endQuoteIndex);
    }

    /**
     * Simple validation method for test compatibility
     */
    public boolean isValidMessage(String message) {
        // Reset last error
        lastValidationError = null;

        // Basic validation checks
        if (message == null) {
            lastValidationError = "Message is null";
            return false;
        }

        if (message.isEmpty() || message.trim().isEmpty()) {
            lastValidationError = "Message is empty";
            return false;
        }

        if (message.length() > maxCommandLength) {
            lastValidationError = "Message exceeds maximum length";
            return false;
        }

        // Check for dangerous patterns
        String[] dangerousPatterns = {
            "<script", "javascript:", "onclick", "onerror",
            "DROP TABLE", "DELETE FROM", "; DROP", "--",
            "$(", "${", "rm -rf", "sudo ", "/bin/", "/usr/bin/",
            "chmod ", "wget ", "curl ", "nc ", "netcat",
            "&&", "||", ";", "|", ">"
        };

        String lowerMessage = message.toLowerCase();
        for (String pattern : dangerousPatterns) {
            if (lowerMessage.contains(pattern.toLowerCase())) {
                lastValidationError = "Message contains dangerous pattern: " + pattern;
                return false;
            }
        }

        // Basic JSON validation if it looks like JSON
        if (message.trim().startsWith("{")) {
            try {
                // Simple check for balanced braces
                int openBraces = 0;
                int closeBraces = 0;
                for (char c : message.toCharArray()) {
                    if (c == '{') openBraces++;
                    if (c == '}') closeBraces++;
                }
                if (openBraces != closeBraces || openBraces == 0) {
                    lastValidationError = "Invalid JSON structure";
                    return false;
                }
                return true;
            } catch (Exception e) {
                lastValidationError = "JSON validation error: " + e.getMessage();
                return false;
            }
        }

        return true;
    }

    /**
     * Get the last validation error message
     */
    public String getLastValidationError() {
        return lastValidationError;
    }
}
