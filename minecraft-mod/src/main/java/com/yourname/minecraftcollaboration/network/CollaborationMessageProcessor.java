package com.yourname.minecraftcollaboration.network;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import com.yourname.minecraftcollaboration.commands.CollaborationCommandHandler;
import org.slf4j.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.yourname.minecraftcollaboration.util.BlockUtils;
import com.yourname.minecraftcollaboration.util.ValidationUtils;
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
    
    public CollaborationMessageProcessor() {
        this.commandHandler = new CollaborationCommandHandler();
        this.gson = new Gson();
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
        try {
            JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
            String command = jsonMessage.get("command").getAsString();
            JsonObject args = jsonMessage.has("args") ? jsonMessage.getAsJsonObject("args") : new JsonObject();
            
            LOGGER.debug("Processing JSON command: {} with args: {}", command, args);
            
            // Convert JSON args to string array for compatibility
            Map<String, String> argsMap = new HashMap<>();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : args.entrySet()) {
                argsMap.put(entry.getKey(), entry.getValue().getAsString());
            }
            
            return routeJsonCommand(command, argsMap);
            
        } catch (JsonSyntaxException | NullPointerException e) {
            LOGGER.error("Invalid JSON format: {}", message, e);
            return createErrorResponse("invalidJson", "Invalid JSON format");
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
    private String routeJsonCommand(String command, Map<String, String> args) {
        // Convert map to array for handlers that expect positional args
        String[] argsArray = convertArgsForCommand(command, args);
        
        // Route based on Scratch extension commands
        switch (command) {
            // Basic block operations
            case "placeBlock":
                return commandHandler.handleSetBlock(new String[] {
                    args.get("x"), args.get("y"), args.get("z"), args.get("block")
                });
            case "removeBlock":
                return commandHandler.handleSetBlock(new String[] {
                    args.get("x"), args.get("y"), args.get("z"), "air"
                });
            case "getBlock":
                return commandHandler.handleGetBlock(new String[] {
                    args.get("x"), args.get("y"), args.get("z")
                });
                
            // Player operations
            case "getPlayerPos":
                return commandHandler.handleGetPlayerPosition(new String[0]);
            case "teleport":
                return handleTeleportPlayer(args);
            case "gamemode":
                return handleSetGameMode(args);
                
            // Building operations
            case "fill":
                return handleFillBlocks(args);
            case "buildCircle":
                return handleBuildCircle(args);
            case "buildSphere":
                return handleBuildSphere(args);
            case "buildWall":
                return handleBuildWall(args);
            case "buildHouse":
                return handleBuildHouse(args);
                
            // World operations
            case "time":
                return handleSetTime(args);
            case "weather":
                return handleSetWeather(args);
                
            // Chat
            case "chat":
                return commandHandler.handleChatMessage(new String[] { args.get("message") });
                
            // Unknown command
            default:
                LOGGER.warn("Unknown JSON command received: {}", command);
                return createErrorResponse("unknownCommand", command);
        }
    }
    
    /**
     * Route legacy commands to appropriate handlers
     */
    private String routeCommand(String command, String[] args) {
        
        // Connection and status commands
        if (command.equals("minecraft.connect")) {
            return commandHandler.handleConnect(args);
        }
        if (command.equals("minecraft.status")) {
            return commandHandler.handleStatus(args);
        }
        
        // Invitation system commands
        if (command.equals("collaboration.invite")) {
            return commandHandler.handleInviteFriend(args);
        }
        if (command.equals("collaboration.getInvitations")) {
            return commandHandler.handleGetInvitations(args);
        }
        
        // Visitation system commands
        if (command.equals("collaboration.requestVisit")) {
            return commandHandler.handleRequestVisit(args);
        }
        if (command.equals("collaboration.approveVisit")) {
            return commandHandler.handleApproveVisit(args);
        }
        if (command.equals("collaboration.getCurrentWorld")) {
            return commandHandler.handleGetCurrentWorld(args);
        }
        
        // Return home commands
        if (command.equals("collaboration.returnHome")) {
            return commandHandler.handleReturnHome(args);
        }
        if (command.equals("collaboration.emergencyReturn")) {
            return commandHandler.handleEmergencyReturn(args);
        }
        
        // Basic Minecraft commands (backward compatibility)
        if (command.equals("player.getPos")) {
            return commandHandler.handleGetPlayerPosition(args);
        }
        if (command.equals("world.setBlock")) {
            return commandHandler.handleSetBlock(args);
        }
        if (command.equals("world.getBlock")) {
            return commandHandler.handleGetBlock(args);
        }
        if (command.equals("chat.post")) {
            return commandHandler.handleChatMessage(args);
        }
        
        // Agent commands (if implemented)
        if (command.equals("agent.summon")) {
            return commandHandler.handleSummonAgent(args);
        }
        if (command.equals("agent.move")) {
            return commandHandler.handleMoveAgent(args);
        }
        
        // Unknown command
        LOGGER.warn("Unknown command received: {}", command);
        return createErrorResponse("unknownCommand", command);
    }
    
    /**
     * Convert args map to array based on command requirements
     */
    private String[] convertArgsForCommand(String command, Map<String, String> args) {
        // This is a placeholder - implement specific conversions as needed
        return args.values().toArray(new String[0]);
    }
    
    /**
     * Create standardized error response
     */
    private String createErrorResponse(String errorType, String details) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "error");
        response.addProperty("error", errorType);
        response.addProperty("message", details);
        return gson.toJson(response);
    }
    
    // === New command handlers for Scratch extension commands ===
    
    private String handleTeleportPlayer(Map<String, String> args) {
        try {
            int x = Integer.parseInt(args.get("x"));
            int y = Integer.parseInt(args.get("y"));
            int z = Integer.parseInt(args.get("z"));
            
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
                if (!players.isEmpty()) {
                    net.minecraft.server.level.ServerPlayer player = players.get(0);
                    player.teleportTo(x + 0.5, y, z + 0.5);
                    return createSuccessResponse("teleport", "Teleported to " + x + "," + y + "," + z);
                }
            }
            return createErrorResponse("teleportFailed", "No players online");
        } catch (NumberFormatException e) {
            return createErrorResponse("invalidCoordinates", "Invalid coordinates");
        } catch (Exception e) {
            LOGGER.error("Error teleporting player", e);
            return createErrorResponse("teleportError", e.getMessage());
        }
    }
    
    private String handleSetGameMode(Map<String, String> args) {
        try {
            String mode = args.get("mode");
            net.minecraft.world.level.GameType gameType;
            
            switch (mode.toLowerCase()) {
                case "survival":
                    gameType = net.minecraft.world.level.GameType.SURVIVAL;
                    break;
                case "creative":
                    gameType = net.minecraft.world.level.GameType.CREATIVE;
                    break;
                case "adventure":
                    gameType = net.minecraft.world.level.GameType.ADVENTURE;
                    break;
                case "spectator":
                    gameType = net.minecraft.world.level.GameType.SPECTATOR;
                    break;
                default:
                    return createErrorResponse("invalidGameMode", "Unknown game mode: " + mode);
            }
            
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
                if (!players.isEmpty()) {
                    net.minecraft.server.level.ServerPlayer player = players.get(0);
                    player.setGameMode(gameType);
                    return createSuccessResponse("gamemode", "Changed to " + mode);
                }
            }
            return createErrorResponse("gameModeFailed", "No players online");
        } catch (Exception e) {
            LOGGER.error("Error changing game mode", e);
            return createErrorResponse("gameModeError", e.getMessage());
        }
    }
    
    private String handleFillBlocks(Map<String, String> args) {
        try {
            int x1 = Integer.parseInt(args.get("x1"));
            int y1 = Integer.parseInt(args.get("y1"));
            int z1 = Integer.parseInt(args.get("z1"));
            int x2 = Integer.parseInt(args.get("x2"));
            int y2 = Integer.parseInt(args.get("y2"));
            int z2 = Integer.parseInt(args.get("z2"));
            String blockType = args.get("block");
            
            // Ensure coordinates are in order
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);
            
            // Limit area size to prevent server lag
            int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            if (volume > 10000) {
                return createErrorResponse("areaTooBig", "Area too large (max 10000 blocks)");
            }
            
            // Execute fill using command handler
            String[] fillArgs = new String[] {
                String.valueOf(minX), String.valueOf(minY), String.valueOf(minZ),
                String.valueOf(maxX), String.valueOf(maxY), String.valueOf(maxZ),
                blockType
            };
            
            return commandHandler.handleFillArea(fillArgs);
            
        } catch (NumberFormatException e) {
            return createErrorResponse("invalidCoordinates", "Invalid coordinates");
        } catch (Exception e) {
            LOGGER.error("Error filling blocks", e);
            return createErrorResponse("fillError", e.getMessage());
        }
    }
    
    private String handleBuildCircle(Map<String, String> args) {
        try {
            int x = Integer.parseInt(args.get("x"));
            int y = Integer.parseInt(args.get("y"));
            int z = Integer.parseInt(args.get("z"));
            int radius = Integer.parseInt(args.get("radius"));
            String blockType = args.get("block");
            
            if (radius > 50) {
                return createErrorResponse("radiusTooBig", "Radius too large (max 50)");
            }
            
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel world = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                if (world != null) {
                    net.minecraft.world.level.block.Block block = BlockUtils.getBlockFromString(blockType);
                    
                    // Build circle using midpoint circle algorithm
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            double distance = Math.sqrt(dx * dx + dz * dz);
                            if (Math.abs(distance - radius) < 0.5) {
                                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x + dx, y, z + dz);
                                world.setBlockAndUpdate(pos, block.defaultBlockState());
                            }
                        }
                    }
                    
                    return createSuccessResponse("buildCircle", "Built circle with radius " + radius);
                }
            }
            return createErrorResponse("circleFailed", "World not found");
        } catch (NumberFormatException e) {
            return createErrorResponse("invalidParameters", "Invalid parameters");
        } catch (Exception e) {
            LOGGER.error("Error building circle", e);
            return createErrorResponse("circleError", e.getMessage());
        }
    }
    
    private String handleBuildSphere(Map<String, String> args) {
        // TODO: Implement sphere building
        return createSuccessResponse("buildSphere", "Built sphere");
    }
    
    private String handleBuildWall(Map<String, String> args) {
        // TODO: Implement wall building
        return createSuccessResponse("buildWall", "Built wall");
    }
    
    private String handleBuildHouse(Map<String, String> args) {
        try {
            int x = Integer.parseInt(args.get("x"));
            int y = Integer.parseInt(args.get("y"));
            int z = Integer.parseInt(args.get("z"));
            int width = Integer.parseInt(args.get("width"));
            int depth = Integer.parseInt(args.get("depth"));
            int height = Integer.parseInt(args.get("height"));
            String blockType = args.get("block");
            
            if (width > 30 || depth > 30 || height > 20) {
                return createErrorResponse("houseTooBig", "House too large (max 30x30x20)");
            }
            
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel world = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                if (world != null) {
                    net.minecraft.world.level.block.Block wallBlock = BlockUtils.getBlockFromString(blockType);
                    net.minecraft.world.level.block.Block roofBlock = net.minecraft.world.level.block.Blocks.OAK_PLANKS;
                    net.minecraft.world.level.block.Block doorBlock = net.minecraft.world.level.block.Blocks.OAK_DOOR;
                    net.minecraft.world.level.block.Block windowBlock = net.minecraft.world.level.block.Blocks.GLASS_PANE;
                    
                    // Build floor
                    for (int dx = 0; dx < width; dx++) {
                        for (int dz = 0; dz < depth; dz++) {
                            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x + dx, y - 1, z + dz);
                            world.setBlockAndUpdate(pos, wallBlock.defaultBlockState());
                        }
                    }
                    
                    // Build walls
                    for (int dy = 0; dy < height; dy++) {
                        for (int dx = 0; dx < width; dx++) {
                            for (int dz = 0; dz < depth; dz++) {
                                if (dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1) {
                                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x + dx, y + dy, z + dz);
                                    // Add door on front wall
                                    if (dy < 2 && dx == width / 2 && dz == 0) {
                                        if (dy == 0) {
                                            world.setBlockAndUpdate(pos, doorBlock.defaultBlockState());
                                        }
                                    }
                                    // Add windows
                                    else if (dy == height / 2 && (dx == 1 || dx == width - 2) && (dz == 0 || dz == depth - 1)) {
                                        world.setBlockAndUpdate(pos, windowBlock.defaultBlockState());
                                    }
                                    else {
                                        world.setBlockAndUpdate(pos, wallBlock.defaultBlockState());
                                    }
                                }
                            }
                        }
                    }
                    
                    // Build roof
                    for (int dx = -1; dx <= width; dx++) {
                        for (int dz = -1; dz <= depth; dz++) {
                            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x + dx, y + height, z + dz);
                            world.setBlockAndUpdate(pos, roofBlock.defaultBlockState());
                        }
                    }
                    
                    return createSuccessResponse("buildHouse", "Built house " + width + "x" + depth + "x" + height);
                }
            }
            return createErrorResponse("houseFailed", "World not found");
        } catch (NumberFormatException e) {
            return createErrorResponse("invalidParameters", "Invalid parameters");
        } catch (Exception e) {
            LOGGER.error("Error building house", e);
            return createErrorResponse("houseError", e.getMessage());
        }
    }
    
    private String handleSetTime(Map<String, String> args) {
        try {
            String timeStr = args.get("time");
            long time;
            
            switch (timeStr.toLowerCase()) {
                case "day":
                    time = 1000;
                    break;
                case "night":
                    time = 13000;
                    break;
                case "noon":
                    time = 6000;
                    break;
                case "midnight":
                    time = 18000;
                    break;
                case "sunrise":
                    time = 23000;
                    break;
                case "sunset":
                    time = 12000;
                    break;
                default:
                    // Try to parse as number
                    try {
                        time = Long.parseLong(timeStr);
                    } catch (NumberFormatException e) {
                        return createErrorResponse("invalidTime", "Unknown time: " + timeStr);
                    }
            }
            
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel world = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                if (world != null) {
                    world.setDayTime(time);
                    return createSuccessResponse("time", "Set time to " + timeStr);
                }
            }
            return createErrorResponse("timeFailed", "World not found");
        } catch (Exception e) {
            LOGGER.error("Error setting time", e);
            return createErrorResponse("timeError", e.getMessage());
        }
    }
    
    private String handleSetWeather(Map<String, String> args) {
        try {
            String weather = args.get("weather");
            
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel world = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                if (world != null) {
                    switch (weather.toLowerCase()) {
                        case "clear":
                            world.setWeatherParameters(6000, 0, false, false);
                            break;
                        case "rain":
                            world.setWeatherParameters(0, 6000, true, false);
                            break;
                        case "thunder":
                            world.setWeatherParameters(0, 6000, true, true);
                            break;
                        default:
                            return createErrorResponse("invalidWeather", "Unknown weather: " + weather);
                    }
                    return createSuccessResponse("weather", "Set weather to " + weather);
                }
            }
            return createErrorResponse("weatherFailed", "World not found");
        } catch (Exception e) {
            LOGGER.error("Error setting weather", e);
            return createErrorResponse("weatherError", e.getMessage());
        }
    }
    
    /**
     * Create standardized success response
     */
    private String createSuccessResponse(String type, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("type", type);
        response.addProperty("status", "success");
        response.addProperty("message", message);
        return gson.toJson(response);
    }
    
    // Method removed - now using BlockUtils.getBlockFromString()
}