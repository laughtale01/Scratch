package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.regex.Pattern;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Comprehensive input validation for all user inputs
 * Prevents injection attacks and ensures data integrity
 */
public class InputValidator {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    // Validation patterns
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^-?\\d+$");
    private static final Pattern BLOCK_TYPE_PATTERN = Pattern.compile("^[a-zA-Z0-9:_]+$");
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s.,!?()\\-_]+$");
    
    // Dangerous patterns to block
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(".*<script.*>.*</script>.*", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SQL_PATTERN = Pattern.compile(".*(union|select|insert|update|delete|drop)\\s+.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(".*[;&|`$].*");
    
    // Maximum lengths
    private static final int MAX_USERNAME_LENGTH = 16;
    private static final int MAX_CHAT_MESSAGE_LENGTH = 256;
    private static final int MAX_COMMAND_LENGTH = 1024;
    private static final int MAX_BLOCK_NAME_LENGTH = 64;
    
    // Coordinate bounds
    private static final int MAX_COORDINATE = 30000000;
    private static final int MIN_COORDINATE = -30000000;
    private static final int MAX_Y_COORDINATE = 320;
    private static final int MIN_Y_COORDINATE = -64;
    
    // Blocked characters for general text
    private static final Set<Character> BLOCKED_CHARS = new HashSet<>(Arrays.asList(
        '\0', '\r', '\n', '\t', '\f', '\b', '<', '>', '&', '"', '\'', '\\', '/', ';', '|', '`', '$'
    ));
    
    /**
     * Validate a username
     * @param username The username to validate
     * @return true if valid
     */
    public static boolean validateUsername(String username) {
        if (username == null || username.isEmpty()) {
            return false;
        }
        
        if (username.length() > MAX_USERNAME_LENGTH) {
            LOGGER.warn("Username too long: {} characters", username.length());
            return false;
        }
        
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            LOGGER.warn("Invalid username format: {}", username);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate coordinates
     * @param x X coordinate
     * @param y Y coordinate  
     * @param z Z coordinate
     * @return true if all coordinates are valid
     */
    public static boolean validateCoordinates(String x, String y, String z) {
        try {
            int xCoord = Integer.parseInt(x);
            int yCoord = Integer.parseInt(y);
            int zCoord = Integer.parseInt(z);
            
            if (xCoord < MIN_COORDINATE || xCoord > MAX_COORDINATE) {
                LOGGER.warn("X coordinate out of bounds: {}", xCoord);
                return false;
            }
            
            if (yCoord < MIN_Y_COORDINATE || yCoord > MAX_Y_COORDINATE) {
                LOGGER.warn("Y coordinate out of bounds: {}", yCoord);
                return false;
            }
            
            if (zCoord < MIN_COORDINATE || zCoord > MAX_COORDINATE) {
                LOGGER.warn("Z coordinate out of bounds: {}", zCoord);
                return false;
            }
            
            return true;
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid coordinate format: x={}, y={}, z={}", x, y, z);
            return false;
        }
    }
    
    /**
     * Validate a single coordinate
     * @param coord The coordinate value
     * @param isY Whether this is a Y coordinate
     * @return true if valid
     */
    public static boolean validateCoordinate(String coord, boolean isY) {
        if (coord == null || !COORDINATE_PATTERN.matcher(coord).matches()) {
            return false;
        }
        
        try {
            int value = Integer.parseInt(coord);
            if (isY) {
                return value >= MIN_Y_COORDINATE && value <= MAX_Y_COORDINATE;
            } else {
                return value >= MIN_COORDINATE && value <= MAX_COORDINATE;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate a block type
     * @param blockType The block type to validate
     * @return true if valid
     */
    public static boolean validateBlockType(String blockType) {
        if (blockType == null || blockType.isEmpty()) {
            return false;
        }
        
        if (blockType.length() > MAX_BLOCK_NAME_LENGTH) {
            LOGGER.warn("Block type name too long: {} characters", blockType.length());
            return false;
        }
        
        if (!BLOCK_TYPE_PATTERN.matcher(blockType).matches()) {
            LOGGER.warn("Invalid block type format: {}", blockType);
            return false;
        }
        
        // Check against blocked blocks
        if (!SecurityConfig.isBlockAllowed(blockType)) {
            LOGGER.warn("Blocked block type: {}", blockType);
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate and sanitize chat messages
     * @param message The chat message
     * @return Sanitized message or null if invalid
     */
    public static String validateChatMessage(String message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        
        if (message.length() > MAX_CHAT_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_CHAT_MESSAGE_LENGTH);
        }
        
        // Check for dangerous patterns
        if (containsDangerousPatterns(message)) {
            LOGGER.warn("Chat message contains dangerous patterns");
            return null;
        }
        
        // Remove blocked characters
        StringBuilder cleaned = new StringBuilder();
        for (char c : message.toCharArray()) {
            if (!BLOCKED_CHARS.contains(c) && !Character.isISOControl(c)) {
                cleaned.append(c);
            }
        }
        
        String cleanedMessage = cleaned.toString().trim();
        
        // Final validation
        if (!SAFE_TEXT_PATTERN.matcher(cleanedMessage).matches()) {
            LOGGER.warn("Chat message contains invalid characters");
            return null;
        }
        
        return cleanedMessage;
    }
    
    /**
     * Validate a command string
     * @param command The command to validate
     * @return true if valid
     */
    public static boolean validateCommand(String command) {
        if (command == null || command.isEmpty()) {
            return false;
        }
        
        if (command.length() > MAX_COMMAND_LENGTH) {
            LOGGER.warn("Command too long: {} characters", command.length());
            return false;
        }
        
        // Check for dangerous patterns
        if (containsDangerousPatterns(command)) {
            LOGGER.warn("Command contains dangerous patterns");
            return false;
        }
        
        // Check against blocked commands
        String commandName = command.split("\\s+")[0].toLowerCase();
        if (!SecurityConfig.isCommandAllowed(commandName)) {
            LOGGER.warn("Blocked command: {}", commandName);
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if a string contains dangerous patterns
     * @param input The input to check
     * @return true if dangerous patterns found
     */
    private static boolean containsDangerousPatterns(String input) {
        if (SCRIPT_PATTERN.matcher(input).matches()) {
            return true;
        }
        
        if (SQL_PATTERN.matcher(input).matches()) {
            return true;
        }
        
        if (COMMAND_INJECTION_PATTERN.matcher(input).matches()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Validate a numeric value within bounds
     * @param value The value to validate
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return true if valid
     */
    public static boolean validateNumericRange(String value, int min, int max) {
        try {
            int numValue = Integer.parseInt(value);
            return numValue >= min && numValue <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Validate a world name
     * @param worldName The world name to validate
     * @return true if valid
     */
    public static boolean validateWorldName(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return false;
        }
        
        if (worldName.length() > 32) {
            return false;
        }
        
        return ALPHANUMERIC_PATTERN.matcher(worldName).matches();
    }
    
    /**
     * Escape HTML entities in a string
     * @param input The input string
     * @return Escaped string
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;")
                   .replace("/", "&#x2F;");
    }
    
    /**
     * Validate JSON string
     * @param json The JSON string to validate
     * @return true if appears to be valid JSON
     */
    public static boolean validateJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        
        // Basic check for JSON structure
        json = json.trim();
        return (json.startsWith("{") && json.endsWith("}")) ||
               (json.startsWith("[") && json.endsWith("]"));
    }
}