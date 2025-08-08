package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.commands.CollaborationCommandHandler;
import edu.minecraft.collaboration.collaboration.CollaborationManager;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.constants.ErrorConstants;
import org.slf4j.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import edu.minecraft.collaboration.monitoring.MetricsCollector;
import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.security.InputValidator;
import java.util.Map;
import java.util.HashMap;

/**
 * Processes messages from Scratch extension and converts them to Minecraft commands
 * Supports both JSON format and legacy format messages
 */
public class CollaborationMessageProcessor {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private final CollaborationCommandHandler commandHandler;
    private final Gson gson;
    private final MetricsCollector metrics;
    private final AuthenticationManager authManager;
    private final CollaborationManager collaborationManager;
    
    // Delegated processors for handling specific command categories
    private final BuildingCommandProcessor buildingProcessor;
    private final CollaborationCommandProcessor collaborationProcessor;
    
    public CollaborationMessageProcessor() {
        DependencyInjector injector = DependencyInjector.getInstance();
        this.metrics = injector.getService(MetricsCollector.class);
        this.authManager = injector.getService(AuthenticationManager.class);
        this.collaborationManager = injector.getService(CollaborationManager.class);
        this.commandHandler = new CollaborationCommandHandler();
        this.gson = new Gson();
        this.buildingProcessor = new BuildingCommandProcessor(commandHandler);
        this.collaborationProcessor = new CollaborationCommandProcessor();
    }
    
    // Current WebSocket connection identifier for authentication
    private String currentConnectionId;
    
    public void setConnectionId(String connectionId) {
        this.currentConnectionId = connectionId;
        this.collaborationProcessor.setConnectionId(connectionId);
    }
    
    /**
     * Process incoming message from WebSocket connection
     * This is the primary method for handling WebSocket messages
     */
    public String processMessage(String message, org.java_websocket.WebSocket webSocket) {
        // Set the current connection context for authentication
        if (webSocket != null) {
            this.currentConnectionId = webSocket.getRemoteSocketAddress().toString();
        }
        
        return processMessage(message);
    }
    
    /**
     * Process incoming message from Scratch extension
     * Supports JSON format: {"command": "cmdName", "args": {...}}
     * Also supports legacy format: "command(arg1,arg2,arg3)"
     */
    public String processMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return createErrorResponse("emptyMessage", "Empty message received");
        }
        
        try {
            LOGGER.debug("Processing message: {}", message);
            
            // Try to parse as JSON first
            if (message.trim().startsWith("{")) {
                return processJsonMessage(message);
            } else {
                // Fall back to legacy format
                return processLegacyMessage(message);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error processing message: {}", message, e);
            return createErrorResponse("processing", e.getMessage());
        }
    }
    
    /**
     * Process JSON format messages from Scratch
     */
    private String processJsonMessage(String message) {
        LOGGER.info("Processing JSON message: {}", message);
        try {
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            
            // Validate required fields
            if (jsonMessage == null || !jsonMessage.has("command")) {
                LOGGER.error("Missing required 'command' field in JSON message: {}", message);
                metrics.incrementCounter(MetricsCollector.Metrics.COMMANDS_FAILED);
                return createErrorResponse("missingCommand", "Command field is required");
            }
            
            JsonElement commandElement = jsonMessage.get("command");
            if (commandElement == null || !commandElement.isJsonPrimitive()) {
                LOGGER.error("Invalid 'command' field type in JSON message: {}", message);
                metrics.incrementCounter(MetricsCollector.Metrics.COMMANDS_FAILED);
                return createErrorResponse("invalidCommand", "Command must be a string");
            }
            
            String command = commandElement.getAsString();
            JsonObject args = jsonMessage.has("args") ? jsonMessage.getAsJsonObject("args") : new JsonObject();
            
            LOGGER.info("Processing JSON command: {} with args: {}", command, args);
            
            // Start timing
            try (MetricsCollector.TimingContext timing = metrics.startTiming(MetricsCollector.Metrics.COMMAND_TIMING_PREFIX + command)) {
                // Convert JSON args to string array for compatibility
                Map<String, String> argsMap = new HashMap<>();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : args.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().isJsonPrimitive()) {
                        argsMap.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                
                String result = routeJsonCommand(command, argsMap);
                
                LOGGER.info("Command result for '{}': {}", command, result);
                
                // Update metrics
                metrics.incrementCounter(MetricsCollector.Metrics.COMMANDS_EXECUTED);
                metrics.incrementCounter(MetricsCollector.Metrics.WS_MESSAGES_SENT);
                
                return result;
            }
            
        } catch (JsonSyntaxException e) {
            LOGGER.error("Invalid JSON syntax: {}", message, e);
            metrics.incrementCounter(MetricsCollector.Metrics.COMMANDS_FAILED);
            metrics.incrementCounter(MetricsCollector.Metrics.WS_ERRORS);
            return createErrorResponse("invalidJson", "Invalid JSON syntax");
        } catch (Exception e) {
            LOGGER.error("Unexpected error processing JSON message: {}", message, e);
            metrics.incrementCounter(MetricsCollector.Metrics.COMMANDS_FAILED);
            metrics.incrementCounter(MetricsCollector.Metrics.WS_ERRORS);
            return createErrorResponse("processingError", "Failed to process message");
        }
    }
    
    /**
     * Process legacy format messages
     */
    private String processLegacyMessage(String message) {
        // Parse command and arguments
        String[] parts = message.split("\\(", 2);
        if (parts.length < 1) {
            return createErrorResponse("invalidFormat", "Expected: command(args)");
        }
        
        String command = parts[0].trim();
        String argsString = "";
        
        if (parts.length > 1) {
            argsString = parts[1];
            // Remove trailing parenthesis
            if (argsString.endsWith(")")) {
                argsString = argsString.substring(0, argsString.length() - 1);
            }
        }
        
        // Split arguments
        String[] args = argsString.isEmpty() ? new String[0] : argsString.split(",");
        
        // Trim arguments
        for (int i = 0; i < args.length; i++) {
            args[i] = args[i].trim();
        }
        
        LOGGER.debug("Parsed legacy command: '{}' with {} arguments", command, args.length);
        
        // Route to appropriate handler based on command
        return routeCommand(command, args);
    }
    
    /**
     * Route JSON commands to appropriate handlers
     */
    private String routeJsonCommand(final String command, final Map<String, String> args) {
        // Route based on command category
        if (isAuthenticationCommand(command)) {
            return handleAuthenticationCommands(command, args);
        }
        if (isConnectionCommand(command)) {
            return handleConnectionCommands(command, args);
        }
        if (isBlockCommand(command)) {
            return handleBlockCommands(command, args);
        }
        if (isPlayerCommand(command)) {
            return handlePlayerCommands(command, args);
        }
        if (isBuildingCommand(command)) {
            return handleBuildingCommands(command, args);
        }
        if (isWorldCommand(command)) {
            return handleWorldCommands(command, args);
        }
        if (isChatCommand(command)) {
            return handleChatCommands(command, args);
        }
        if (isCollaborationCommand(command)) {
            return handleCollaborationCommands(command, args);
        }
        if (isAgentCommand(command)) {
            return handleAgentCommands(command, args);
        }
        
        // Unknown command
        LOGGER.warn("Unknown JSON command received: {}", command);
        return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
    }
    
    /**
     * Check if command is an authentication command
     */
    private boolean isAuthenticationCommand(final String command) {
        return "auth".equals(command) || "getUserInfo".equals(command);
    }
    
    /**
     * Handle authentication-related commands
     */
    private String handleAuthenticationCommands(final String command, final Map<String, String> args) {
        switch (command) {
            case "auth":
                return collaborationProcessor.handleAuthentication(args);
            case "getUserInfo":
                return collaborationProcessor.handleGetUserInfo(args);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Check if command is a connection command
     */
    private boolean isConnectionCommand(final String command) {
        return "connect".equals(command) || "status".equals(command) || "getPlayerPosition".equals(command) ||
               "ping".equals(command) || "getPlayerPos".equals(command);
    }
    
    /**
     * Handle connection-related commands
     */
    private String handleConnectionCommands(final String command, final Map<String, String> args) {
        switch (command) {
            case "connect":
                return commandHandler.handleConnect(new String[0]);
            case "status":
                return commandHandler.handleStatus(new String[0]);
            case "getPlayerPosition":
            case "getPlayerPos":
                return commandHandler.handleGetPlayerPosition(new String[0]);
            case "ping":
                return commandHandler.handlePing(new String[0]);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Check if command is a block operation command
     */
    private boolean isBlockCommand(final String command) {
        return "setBlock".equals(command) || "fillArea".equals(command) || 
               "placeBlock".equals(command) || "removeBlock".equals(command) || 
               "getBlock".equals(command);
    }
    
    /**
     * Handle block operation commands
     */
    private String handleBlockCommands(final String command, final Map<String, String> args) {
        switch (command) {
            case "setBlock":
                return commandHandler.handleSetBlock(new String[] {
                    args.get("x"), args.get("y"), args.get("z"), args.get("blockType")
                });
            case "fillArea":
                return commandHandler.handleFillArea(new String[] {
                    args.get("x1"), args.get("y1"), args.get("z1"), 
                    args.get("x2"), args.get("y2"), args.get("z2"), 
                    args.get("blockType")
                });
            case "placeBlock":
                return handlePlaceBlockCommand(args);
            case "removeBlock":
                return commandHandler.handleSetBlock(new String[] {
                    args.get("x"), args.get("y"), args.get("z"), ErrorConstants.AIR_BLOCK
                });
            case "getBlock":
                return commandHandler.handleGetBlock(new String[] {
                    args.get("x"), args.get("y"), args.get("z")
                });
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Handle place block command with validation
     */
    private String handlePlaceBlockCommand(final Map<String, String> args) {
        // Validate coordinates
        if (!InputValidator.validateCoordinates(args.get("x"), args.get("y"), args.get("z"))) {
            return createErrorResponse(ErrorConstants.ERROR_INVALID_COORDINATES, ErrorConstants.MSG_INVALID_COORDINATES);
        }
        // Validate block type
        if (!InputValidator.validateBlockType(args.get("block"))) {
            return createErrorResponse(ErrorConstants.ERROR_INVALID_BLOCK_TYPE, ErrorConstants.MSG_INVALID_BLOCK_TYPE);
        }
        return commandHandler.handleSetBlock(new String[] {
            args.get("x"), args.get("y"), args.get("z"), args.get("block")
        });
    }
    
    /**
     * Check if command is a player operation command
     */
    private boolean isPlayerCommand(final String command) {
        return "getPlayerPos".equals(command) || "teleport".equals(command) || "gamemode".equals(command);
    }
    
    /**
     * Handle player operation commands
     */
    private String handlePlayerCommands(final String command, final Map<String, String> args) {
        switch (command) {
            case "getPlayerPos":
                return commandHandler.handleGetPlayerPosition(new String[0]);
            case "teleport":
                return buildingProcessor.handleTeleportPlayer(args);
            case "gamemode":
                return buildingProcessor.handleSetGameMode(args);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Check if command is a building operation command
     */
    private boolean isBuildingCommand(final String command) {
        return "fill".equals(command) || "buildCircle".equals(command) || 
               "buildSphere".equals(command) || "buildWall".equals(command) || 
               "buildHouse".equals(command);
    }
    
    /**
     * Handle building operation commands
     */
    private String handleBuildingCommands(final String command, final Map<String, String> args) {
        switch (command) {
            case "fill":
                return buildingProcessor.handleFillBlocks(args);
            case "buildCircle":
                return buildingProcessor.handleBuildCircle(args);
            case "buildSphere":
                return buildingProcessor.handleBuildSphere(args);
            case "buildWall":
                return buildingProcessor.handleBuildWall(args);
            case "buildHouse":
                return buildingProcessor.handleBuildHouse(args);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Check if command is a world operation command
     */
    private boolean isWorldCommand(final String command) {
        return "time".equals(command) || "weather".equals(command);
    }
    
    /**
     * Handle world operation commands
     */
    private String handleWorldCommands(final String command, final Map<String, String> args) {
        switch (command) {
            case "time":
                return buildingProcessor.handleSetTime(args);
            case "weather":
                return buildingProcessor.handleSetWeather(args);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Check if command is a chat command
     */
    private boolean isChatCommand(final String command) {
        return "chat".equals(command);
    }
    
    /**
     * Handle chat commands
     */
    private String handleChatCommands(final String command, final Map<String, String> args) {
        if ("chat".equals(command)) {
            return commandHandler.handleChatMessage(new String[]{args.get("message")});
        }
        return createErrorResponse("unknownCommand", command);
    }
    
    /**
     * Check if command is a collaboration command
     */
    private boolean isCollaborationCommand(final String command) {
        return "getInvitations".equals(command) || "getCurrentWorld".equals(command);
    }
    
    /**
     * Handle collaboration commands
     */
    private String handleCollaborationCommands(final String command, final Map<String, String> args) {
        switch (command) {
            case "getInvitations":
                return commandHandler.handleGetInvitations(new String[0]);
            case "getCurrentWorld":
                return collaborationProcessor.handleGetCurrentWorld();
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Check if command is an agent command
     */
    private boolean isAgentCommand(final String command) {
        return "summonAgent".equals(command) || "moveAgent".equals(command) || 
               "agentFollow".equals(command) || "agentAction".equals(command) || 
               "dismissAgent".equals(command);
    }
    
    /**
     * Handle agent commands
     */
    private String handleAgentCommands(final String command, final Map<String, String> args) {
        switch (command) {
            case "summonAgent":
                return commandHandler.handleSummonAgent(new String[]{
                    args.getOrDefault("name", ErrorConstants.DEFAULT_AGENT_NAME)
                });
            case "moveAgent":
                return handleMoveAgentCommand(args);
            case "agentFollow":
                return commandHandler.handleAgentFollow(new String[]{
                    args.getOrDefault("follow", ErrorConstants.DEFAULT_FOLLOW)
                });
            case "agentAction":
                return commandHandler.handleAgentAction(new String[]{
                    args.get("action")
                });
            case "dismissAgent":
                return commandHandler.handleDismissAgent(new String[0]);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Handle move agent command with conditional logic
     */
    private String handleMoveAgentCommand(final Map<String, String> args) {
        if (args.containsKey("direction")) {
            return commandHandler.handleMoveAgent(new String[]{
                args.get("direction"),
                args.getOrDefault("distance", ErrorConstants.DEFAULT_DISTANCE)
            });
        } else {
            return commandHandler.handleMoveAgent(new String[]{
                args.get("x"), args.get("y"), args.get("z")
            });
        }
    }
    
    /**
     * Route legacy commands to appropriate handlers
     */
    private String routeCommand(final String command, final String[] args) {
        // Route based on command prefix to reduce complexity
        if (command.startsWith("minecraft.")) {
            return handleMinecraftCommands(command, args);
        }
        if (command.startsWith("collaboration.")) {
            return handleLegacyCollaborationCommands(command, args);
        }
        if (command.startsWith("player.") || command.startsWith("world.") || command.startsWith("chat.")) {
            return handleBasicMinecraftCommands(command, args);
        }
        if (command.startsWith("agent.")) {
            return handleLegacyAgentCommands(command, args);
        }
        
        // Unknown command
        LOGGER.warn("Unknown command received: {}", command);
        return createErrorResponse("unknownCommand", command);
    }
    
    /**
     * Handle minecraft.* commands
     */
    private String handleMinecraftCommands(final String command, final String[] args) {
        switch (command) {
            case "minecraft.connect":
                return commandHandler.handleConnect(args);
            case "minecraft.status":
                return commandHandler.handleStatus(args);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Handle collaboration.* commands
     */
    private String handleLegacyCollaborationCommands(final String command, final String[] args) {
        switch (command) {
            case "collaboration.invite":
                return commandHandler.handleInviteFriend(args);
            case "collaboration.getInvitations":
                return commandHandler.handleGetInvitations(args);
            case "collaboration.requestVisit":
                return commandHandler.handleRequestVisit(args);
            case "collaboration.approveVisit":
                return commandHandler.handleApproveVisit(args);
            case "collaboration.getCurrentWorld":
                return commandHandler.handleGetCurrentWorld(args);
            case "collaboration.returnHome":
                return commandHandler.handleReturnHome(args);
            case "collaboration.emergencyReturn":
                return commandHandler.handleEmergencyReturn(args);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Handle basic minecraft commands (backward compatibility)
     */
    private String handleBasicMinecraftCommands(final String command, final String[] args) {
        switch (command) {
            case "player.getPos":
                return commandHandler.handleGetPlayerPosition(args);
            case "world.setBlock":
                return commandHandler.handleSetBlock(args);
            case "world.getBlock":
                return commandHandler.handleGetBlock(args);
            case "chat.post":
                return commandHandler.handleChatMessage(args);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Handle agent.* commands
     */
    private String handleLegacyAgentCommands(final String command, final String[] args) {
        switch (command) {
            case "agent.summon":
                return commandHandler.handleSummonAgent(args);
            case "agent.move":
                return commandHandler.handleMoveAgent(args);
            default:
                return createErrorResponse(ErrorConstants.ERROR_UNKNOWN_COMMAND, command);
        }
    }
    
    /**
     * Convert args map to array based on command requirements
     */
    private String[] convertArgsForCommand(final String command, final Map<String, String> args) {
        // This is a placeholder - implement specific conversions as needed
        return args.values().toArray(new String[0]);
    }
    
    /**
     * Create standardized error response
     */
    private String createErrorResponse(final String errorType, final String details) {
        return String.format(ErrorConstants.JSON_ERROR_TEMPLATE, errorType, details);
    }
    
    // === Delegated command handlers ===
    // All heavy command logic has been moved to specialized processors
    // This class now focuses on routing and coordination
}