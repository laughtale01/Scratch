package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.config.ConfigurationManager;
import edu.minecraft.collaboration.core.DependencyInjector;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Security configuration for the Minecraft Collaboration System
 * Ensures safe operation for educational use
 * Now uses ConfigurationManager for externalized configuration
 */
public final class SecurityConfig {

    private static ConfigurationManager configManager;

    private SecurityConfig() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Get the configuration manager instance
     */
    private static ConfigurationManager getConfigManager() {
        if (configManager == null) {
            configManager = DependencyInjector.getInstance().getService(ConfigurationManager.class);
        }
        return configManager;
    }

    // WebSocket configuration - now loaded from properties
    public static int getWebSocketPort() {
        return getConfigManager().getIntProperty("websocket.port", 14711);
    }

    public static String getWebSocketHost() {
        return getConfigManager().getProperty("websocket.host", "localhost");
    }

    public static int getMaxConnections() {
        return getConfigManager().getIntProperty("websocket.max.connections", 10);
    }

    public static int getConnectionTimeoutMs() {
        return getConfigManager().getIntProperty("websocket.connection.timeout.ms", 30000);
    }

    // Command restrictions - now loaded from properties
    public static int getMaxCommandLength() {
        return getConfigManager().getIntProperty("security.max.command.length", 1024);
    }

    public static int getMaxChatLength() {
        return getConfigManager().getIntProperty("security.max.chat.length", 256);
    }

    public static int getCommandRateLimitPerSecond() {
        return getConfigManager().getIntProperty("security.command.rate.limit.per.second", 10);
    }

    // World modification limits - now loaded from properties
    public static int getMaxBlocksPerOperation() {
        return getConfigManager().getIntProperty("security.max.blocks.per.operation", 10000);
    }

    public static int getMaxTeleportDistance() {
        return getConfigManager().getIntProperty("security.max.teleport.distance", 1000);
    }

    // Legacy constants for backward compatibility
    @Deprecated
    public static final int WEBSOCKET_PORT = 14711;
    @Deprecated
    public static final String WEBSOCKET_HOST = "localhost";
    @Deprecated
    public static final int MAX_CONNECTIONS = 10;
    @Deprecated
    public static final int CONNECTION_TIMEOUT_MS = 30000;
    @Deprecated
    public static final int MAX_COMMAND_LENGTH = 1024;
    @Deprecated
    public static final int MAX_CHAT_LENGTH = 256;
    @Deprecated
    public static final int COMMAND_RATE_LIMIT_PER_SECOND = 10;
    @Deprecated
    public static final int MAX_BLOCKS_PER_OPERATION = 10000;
    @Deprecated
    public static final int MAX_TELEPORT_DISTANCE = 1000;

    // Blocked items/blocks - now loaded from configuration
    private static Set<String> getBlockedBlocks() {
        String blockedBlocksStr = getConfigManager().getProperty("security.blocked.blocks",
            "tnt,minecraft:tnt,end_crystal,minecraft:end_crystal,respawn_anchor,minecraft:respawn_anchor,bed,minecraft:bed");
        return new HashSet<>(Arrays.asList(blockedBlocksStr.split(",")));
    }

    // Blocked commands - now loaded from configuration
    private static Set<String> getBlockedCommands() {
        String blockedCommandsStr = getConfigManager().getProperty("security.blocked.commands",
            "op,deop,stop,kick,ban,pardon,save-all,save-on,save-off,whitelist,reload,restart,execute,function,trigger,publish,debug,worldborder,setworldspawn,defaultgamemode,gamerule");
        return new HashSet<>(Arrays.asList(blockedCommandsStr.split(",")));
    }

    /**
     * Check if a block type is allowed
     * @param blockType The block type to check
     * @return true if allowed
     */
    public static boolean isBlockAllowed(String blockType) {
        if (blockType == null) {
            return false;
        }
        return !getBlockedBlocks().contains(blockType.toLowerCase());
    }

    /**
     * Check if a command is allowed
     * @param command The command to check
     * @return true if allowed
     */
    public static boolean isCommandAllowed(String command) {
        if (command == null) {
            return false;
        }
        return !getBlockedCommands().contains(command.toLowerCase());
    }

    /**
     * Sanitize chat messages
     * @param message The message to sanitize
     * @return Sanitized message
     */
    public static String sanitizeChatMessage(String message) {
        if (message == null) {
            return "";
        }

        // Remove potential command injections
        message = message.replaceAll("[\\r\\n]", " ");

        // Limit length using configuration
        int maxChatLength = getMaxChatLength();
        if (message.length() > maxChatLength) {
            message = message.substring(0, maxChatLength);
        }

        // Remove control characters
        message = message.replaceAll("[\\p{Cntrl}]", "");

        return message.trim();
    }

    /**
     * Check if an IP address is allowed to connect
     * @param address The IP address
     * @return true if allowed based on configuration
     */
    public static boolean isAddressAllowed(String address) {
        if (address == null) {
            return false;
        }

        // Check explicitly allowed addresses from configuration
        String allowedAddressesStr = getConfigManager().getProperty("security.allowed.addresses",
            "localhost,127.0.0.1,::1,0:0:0:0:0:0:0:1");
        Set<String> allowedAddresses = new HashSet<>(Arrays.asList(allowedAddressesStr.split(",")));

        if (allowedAddresses.contains(address)) {
            return true;
        }

        // Check if localhost addresses are allowed
        if (isLocalhostAddress(address)) {
            return true;
        }

        // Check if private networks are allowed
        boolean allowPrivateNetworks = getConfigManager().getBooleanProperty("security.allow.private.networks", true);
        if (allowPrivateNetworks && isPrivateNetworkAddress(address)) {
            return true;
        }

        return false;
    }

    /**
     * Check if address is localhost (IPv4 or IPv6)
     * @param address The IP address
     * @return true if localhost
     */
    private static boolean isLocalhostAddress(String address) {
        return address.equals("localhost") ||
             address.equals("127.0.0.1")
            || address.startsWith("127.") ||
             address.equals("::1")
            || address.equals("0:0:0:0:0:0:0:1");
    }

    /**
     * Check if address is in private network ranges
     * @param address The IP address
     * @return true if private network
     */
    private static boolean isPrivateNetworkAddress(String address) {
        ConfigurationManager config = getConfigManager();

        boolean allowClassA = config.getBooleanProperty("security.allow.class.a.private", true);
        boolean allowClassB = config.getBooleanProperty("security.allow.class.b.private", true);
        boolean allowClassC = config.getBooleanProperty("security.allow.class.c.private", true);

        return (allowClassA && isClassAPrivateNetwork(address)) ||
             (allowClassB && isClassBPrivateNetwork(address))
            || (allowClassC && isClassCPrivateNetwork(address));
    }

    /**
     * Check if address is in Class A private network (10.0.0.0/8)
     * @param address The IP address
     * @return true if Class A private
     */
    private static boolean isClassAPrivateNetwork(String address) {
        return address.startsWith("10.");
    }

    /**
     * Check if address is in Class B private network (172.16.0.0/12)
     * @param address The IP address
     * @return true if Class B private
     */
    private static boolean isClassBPrivateNetwork(String address) {
        return address.startsWith("172.16.") ||
             address.startsWith("172.17.")
            || address.startsWith("172.18.") ||
             address.startsWith("172.19.")
            || isClassBPrivateNetworkRange20to23(address) ||
             isClassBPrivateNetworkRange24to27(address)
            || isClassBPrivateNetworkRange28to31(address);
    }

    /**
     * Check if address is in Class B private network range 172.20-23.0.0/12
     * @param address The IP address
     * @return true if in range
     */
    private static boolean isClassBPrivateNetworkRange20to23(String address) {
        return address.startsWith("172.20.") ||
             address.startsWith("172.21.")
            || address.startsWith("172.22.") ||
             address.startsWith("172.23.");
    }

    /**
     * Check if address is in Class B private network range 172.24-27.0.0/12
     * @param address The IP address
     * @return true if in range
     */
    private static boolean isClassBPrivateNetworkRange24to27(String address) {
        return address.startsWith("172.24.") ||
             address.startsWith("172.25.")
            || address.startsWith("172.26.") ||
             address.startsWith("172.27.");
    }

    /**
     * Check if address is in Class B private network range 172.28-31.0.0/12
     * @param address The IP address
     * @return true if in range
     */
    private static boolean isClassBPrivateNetworkRange28to31(String address) {
        return address.startsWith("172.28.") ||
             address.startsWith("172.29.")
            || address.startsWith("172.30.") ||
             address.startsWith("172.31.");
    }

    /**
     * Check if address is in Class C private network (192.168.0.0/16)
     * @param address The IP address
     * @return true if Class C private
     */
    private static boolean isClassCPrivateNetwork(String address) {
        return address.startsWith("192.168.");
    }
}
