package com.yourname.minecraftcollaboration.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Security configuration for the Minecraft Collaboration System
 * Ensures safe operation for educational use
 */
public class SecurityConfig {
    
    // WebSocket configuration
    public static final int WEBSOCKET_PORT = 14711;
    public static final String WEBSOCKET_HOST = "localhost";  // Local only
    public static final int MAX_CONNECTIONS = 10;
    public static final int CONNECTION_TIMEOUT_MS = 30000;
    
    // Command restrictions
    public static final int MAX_COMMAND_LENGTH = 1024;
    public static final int MAX_CHAT_LENGTH = 256;
    public static final int COMMAND_RATE_LIMIT_PER_SECOND = 10;
    
    // World modification limits
    public static final int MAX_BLOCKS_PER_OPERATION = 10000;
    public static final int MAX_TELEPORT_DISTANCE = 1000;
    
    // Blocked items/blocks for safety
    private static final Set<String> BLOCKED_BLOCKS = new HashSet<>(Arrays.asList(
        "tnt",
        "minecraft:tnt",
        "end_crystal",
        "minecraft:end_crystal",
        "respawn_anchor",
        "minecraft:respawn_anchor",
        "bed",  // Can explode in nether/end
        "minecraft:bed"
    ));
    
    // Blocked commands
    private static final Set<String> BLOCKED_COMMANDS = new HashSet<>(Arrays.asList(
        "op",
        "deop",
        "stop",
        "kick",
        "ban",
        "pardon",
        "save-all",
        "save-on",
        "save-off"
    ));
    
    /**
     * Check if a block type is allowed
     * @param blockType The block type to check
     * @return true if allowed
     */
    public static boolean isBlockAllowed(String blockType) {
        if (blockType == null) return false;
        return !BLOCKED_BLOCKS.contains(blockType.toLowerCase());
    }
    
    /**
     * Check if a command is allowed
     * @param command The command to check
     * @return true if allowed
     */
    public static boolean isCommandAllowed(String command) {
        if (command == null) return false;
        return !BLOCKED_COMMANDS.contains(command.toLowerCase());
    }
    
    /**
     * Sanitize chat messages
     * @param message The message to sanitize
     * @return Sanitized message
     */
    public static String sanitizeChatMessage(String message) {
        if (message == null) return "";
        
        // Remove potential command injections
        message = message.replaceAll("[\\r\\n]", " ");
        
        // Limit length
        if (message.length() > MAX_CHAT_LENGTH) {
            message = message.substring(0, MAX_CHAT_LENGTH);
        }
        
        // Remove control characters
        message = message.replaceAll("[\\p{Cntrl}]", "");
        
        return message.trim();
    }
    
    /**
     * Check if an IP address is allowed to connect
     * @param address The IP address
     * @return true if allowed (currently only localhost)
     */
    public static boolean isAddressAllowed(String address) {
        return address != null && (
            address.equals("localhost") ||
            address.equals("127.0.0.1") ||
            address.startsWith("127.") ||
            address.equals("::1") ||
            address.startsWith("192.168.") ||  // Local network
            address.startsWith("10.") ||        // Local network
            address.startsWith("172.16.") ||    // Local network
            address.startsWith("172.17.") ||
            address.startsWith("172.18.") ||
            address.startsWith("172.19.") ||
            address.startsWith("172.20.") ||
            address.startsWith("172.21.") ||
            address.startsWith("172.22.") ||
            address.startsWith("172.23.") ||
            address.startsWith("172.24.") ||
            address.startsWith("172.25.") ||
            address.startsWith("172.26.") ||
            address.startsWith("172.27.") ||
            address.startsWith("172.28.") ||
            address.startsWith("172.29.") ||
            address.startsWith("172.30.") ||
            address.startsWith("172.31.")
        );
    }
}