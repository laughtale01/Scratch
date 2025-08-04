package edu.minecraft.collaboration.constants;

/**
 * Constants for error messages and common strings to avoid duplication
 */
public final class ErrorConstants {
    
    private ErrorConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Test status constants
    public static final String PASSED = "PASSED";
    public static final String FAILED = "FAILED";
    
    // Error types
    public static final String ERROR_UNKNOWN_COMMAND = "unknownCommand";
    public static final String ERROR_INVALID_COORDINATES = "invalidCoordinates";
    public static final String ERROR_INVALID_BLOCK_TYPE = "invalidBlockType";
    public static final String ERROR_INVALID_MESSAGE_LENGTH = "invalidMessageLength";
    public static final String ERROR_UNAUTHENTICATED = "unauthenticated";
    public static final String ERROR_RATE_LIMIT_EXCEEDED = "rateLimitExceeded";
    
    // Error messages
    public static final String MSG_INVALID_COORDINATES = "Invalid coordinates";
    public static final String MSG_INVALID_BLOCK_TYPE = "Invalid or blocked block type";
    public static final String MSG_MESSAGE_TOO_LONG = "Message too long";
    public static final String MSG_AUTHENTICATE_FIRST = "Please authenticate first";
    public static final String MSG_RATE_LIMIT_EXCEEDED = "Too many commands. Please slow down.";
    
    // JSON response templates
    public static final String JSON_ERROR_TEMPLATE = "{\"type\":\"error\",\"error\":\"%s\",\"message\":\"%s\"}";
    public static final String JSON_WELCOME_MESSAGE = "{\"type\":\"welcome\",\"status\":\"connected\",\"message\":\"Minecraft Collaboration System Ready\",\"version\":\"1.0.0\"}";
    
    // Connection and networking
    public static final String CONNECTION_PREFIX = "conn_";
    public static final String UNKNOWN_CONNECTION = "unknown";
    
    // Default values
    public static final String DEFAULT_AGENT_NAME = "Agent";
    public static final String DEFAULT_DISTANCE = "1";
    public static final String DEFAULT_FOLLOW = "true";
    public static final String AIR_BLOCK = "air";
}