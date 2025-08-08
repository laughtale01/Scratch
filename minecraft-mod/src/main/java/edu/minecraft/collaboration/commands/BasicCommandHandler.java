package edu.minecraft.collaboration.commands;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import edu.minecraft.collaboration.util.BlockUtils;
import edu.minecraft.collaboration.util.ValidationUtils;
import edu.minecraft.collaboration.util.ResponseHelper;
import edu.minecraft.collaboration.monitoring.MetricsCollector;

/**
 * Handler for basic Minecraft commands (connection, blocks, player position, chat)
 */
public class BasicCommandHandler {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private final MetricsCollector metricsCollector;
    
    public BasicCommandHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
    
    /**
     * Handle connection status command
     */
    public String handleConnect(String[] args) {
        return ResponseHelper.connected();
    }
    
    /**
     * Handle status check command
     */
    public String handleStatus(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            boolean isRunning = server != null && server.isRunning();
            int playerCount = isRunning ? server.getPlayerCount() : 0;
            
            if (isRunning) {
                return ResponseHelper.status("running", playerCount);
            } else {
                return ResponseHelper.status("stopped", 0);
            }
        } catch (Exception e) {
            LOGGER.error("Error checking server status", e);
            return ResponseHelper.error("status", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle ping command
     */
    public String handlePing(String[] args) {
        return "pong";
    }
    
    /**
     * Handle get player position command
     */
    public String handleGetPlayerPosition(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos pos = player.blockPosition();
                metricsCollector.incrementCounter("commands.getPlayerPosition");
                return ResponseHelper.playerPosition(pos.getX(), pos.getY(), pos.getZ());
            }
            return ResponseHelper.error("getPlayerPosition", ResponseHelper.ERROR_NO_PLAYER, "No players online");
        } catch (Exception e) {
            LOGGER.error("Error getting player position", e);
            return ResponseHelper.error("getPlayerPosition", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle set block command
     */
    public String handleSetBlock(String[] args) {
        if (args.length < 4) {
            return ResponseHelper.error("setBlock", ResponseHelper.ERROR_INVALID_ARGS, "Expected: x y z blockType");
        }
        
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            String blockType = args[3];
            
            // Validate coordinates
            if (!ValidationUtils.isValidCoordinate(x, y, z)) {
                return ResponseHelper.error("setBlock", ResponseHelper.ERROR_INVALID_COORDS, "Invalid coordinates");
            }
            
            // Validate block type
            if (!ValidationUtils.isValidBlockType(blockType)) {
                return ResponseHelper.error("setBlock", ResponseHelper.ERROR_INVALID_BLOCK, "Invalid block type: " + blockType);
            }
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel world = server.getLevel(Level.OVERWORLD);
                if (world != null) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = BlockUtils.getBlockFromString(blockType);
                    
                    if (block != null) {
                        world.setBlockAndUpdate(pos, block.defaultBlockState());
                        metricsCollector.incrementCounter("commands.setBlock");
                        return ResponseHelper.blockSet(x, y, z, blockType);
                    } else {
                        return ResponseHelper.error("setBlock", ResponseHelper.ERROR_INVALID_BLOCK, "Unknown block type: " + blockType);
                    }
                }
            }
            return ResponseHelper.error("setBlock", ResponseHelper.ERROR_SERVER_UNAVAILABLE, "Server not available");
        } catch (NumberFormatException e) {
            return ResponseHelper.error("setBlock", ResponseHelper.ERROR_INVALID_ARGS, "Invalid coordinate format");
        } catch (Exception e) {
            LOGGER.error("Error setting block", e);
            return ResponseHelper.error("setBlock", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle get block command
     */
    public String handleGetBlock(String[] args) {
        if (args.length < 3) {
            return ResponseHelper.error("getBlock", ResponseHelper.ERROR_INVALID_ARGS, "Expected: x y z");
        }
        
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            
            // Validate coordinates
            if (!ValidationUtils.isValidCoordinate(x, y, z)) {
                return ResponseHelper.error("getBlock", ResponseHelper.ERROR_INVALID_COORDS, "Invalid coordinates");
            }
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel world = server.getLevel(Level.OVERWORLD);
                if (world != null) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    String blockName = BlockUtils.getBlockName(block);
                    metricsCollector.incrementCounter("commands.getBlock");
                    return ResponseHelper.blockInfo(blockName, x, y, z);
                }
            }
            return ResponseHelper.error("getBlock", ResponseHelper.ERROR_SERVER_UNAVAILABLE, "Server not available");
        } catch (NumberFormatException e) {
            return ResponseHelper.error("getBlock", ResponseHelper.ERROR_INVALID_ARGS, "Invalid coordinate format");
        } catch (Exception e) {
            LOGGER.error("Error getting block", e);
            return ResponseHelper.error("getBlock", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle chat message command
     */
    public String handleChatMessage(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("chat", ResponseHelper.ERROR_INVALID_ARGS, "Expected: message");
        }
        
        try {
            String message = String.join(" ", args);
            
            // Validate message
            if (!ValidationUtils.isValidChatMessage(message)) {
                return ResponseHelper.error("chat", ResponseHelper.ERROR_INVALID_MESSAGE, "Invalid message");
            }
            
            // Try to get server - this should work for both single-player and dedicated servers
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            
            // Log for debugging
            LOGGER.info("Attempting to send chat message: '{}', Server available: {}", message, (server != null));
            
            if (server != null && server.getPlayerList() != null) {
                Component chatComponent = Component.literal("[Scratch] " + message);
                
                // Try to broadcast to all players
                if (!server.getPlayerList().getPlayers().isEmpty()) {
                    // Send to all players individually to ensure delivery
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        player.sendSystemMessage(chatComponent);
                    }
                    LOGGER.info("Chat message sent to {} player(s)", server.getPlayerList().getPlayers().size());
                } else {
                    // No players online, but still report success (message was processed)
                    LOGGER.warn("No players online to receive chat message");
                }
                
                metricsCollector.incrementCounter("commands.chat");
                return ResponseHelper.chatSent(message);
            }
            
            LOGGER.warn("Server not available for chat message");
            return ResponseHelper.error("chat", ResponseHelper.ERROR_SERVER_UNAVAILABLE, "Server not available");
        } catch (Exception e) {
            LOGGER.error("Error sending chat message", e);
            return ResponseHelper.error("chat", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle fill area command
     */
    public String handleFillArea(String[] args) {
        if (args.length < 7) {
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INVALID_ARGS, "Expected: x1 y1 z1 x2 y2 z2 blockType");
        }
        
        try {
            FillAreaParams params = parseFillAreaParams(args);
            
            String validationError = validateFillAreaParams(params);
            if (validationError != null) {
                return validationError;
            }
            
            return executeFillArea(params);
        } catch (NumberFormatException e) {
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INVALID_ARGS, "Invalid coordinate format");
        } catch (Exception e) {
            LOGGER.error("Error filling area", e);
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    private FillAreaParams parseFillAreaParams(String[] args) {
        return new FillAreaParams(
            Integer.parseInt(args[0]),
            Integer.parseInt(args[1]),
            Integer.parseInt(args[2]),
            Integer.parseInt(args[3]),
            Integer.parseInt(args[4]),
            Integer.parseInt(args[5]),
            args[6]
        );
    }
    
    private String validateFillAreaParams(FillAreaParams params) {
        // Validate coordinates
        if (!ValidationUtils.isValidCoordinate(params.x1, params.y1, params.z1) || 
            !ValidationUtils.isValidCoordinate(params.x2, params.y2, params.z2)) {
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INVALID_COORDS, "Invalid coordinates");
        }
        
        // Validate block type
        if (!ValidationUtils.isValidBlockType(params.blockType)) {
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INVALID_BLOCK, "Invalid block type: " + params.blockType);
        }
        
        // Calculate and validate volume
        int volume = params.calculateVolume();
        if (volume > 10000) {
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_AREA_TOO_LARGE, "Area too large (max 10000 blocks)");
        }
        
        return null; // No validation errors
    }
    
    private String executeFillArea(FillAreaParams params) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_SERVER_UNAVAILABLE, "Server not available");
        }
        
        ServerLevel world = server.getLevel(Level.OVERWORLD);
        if (world == null) {
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_SERVER_UNAVAILABLE, "World not available");
        }
        
        Block block = BlockUtils.getBlockFromString(params.blockType);
        if (block == null) {
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INVALID_BLOCK, "Unknown block type: " + params.blockType);
        }
        
        int blocksSet = fillAreaWithBlock(world, params, block);
        metricsCollector.incrementCounter("commands.fillArea");
        
        return ResponseHelper.areaFilled(
            params.getMinX(), params.getMinY(), params.getMinZ(),
            params.getMaxX(), params.getMaxY(), params.getMaxZ(),
            params.blockType, blocksSet
        );
    }
    
    private int fillAreaWithBlock(ServerLevel world, FillAreaParams params, Block block) {
        int blocksSet = 0;
        for (int x = params.getMinX(); x <= params.getMaxX(); x++) {
            for (int y = params.getMinY(); y <= params.getMaxY(); y++) {
                for (int z = params.getMinZ(); z <= params.getMaxZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    world.setBlockAndUpdate(pos, block.defaultBlockState());
                    blocksSet++;
                }
            }
        }
        return blocksSet;
    }
    
    private static class FillAreaParams {
        final int x1, y1, z1, x2, y2, z2;
        final String blockType;
        
        FillAreaParams(int x1, int y1, int z1, int x2, int y2, int z2, String blockType) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
            this.blockType = blockType;
        }
        
        int calculateVolume() {
            return Math.abs(x2 - x1 + 1) * Math.abs(y2 - y1 + 1) * Math.abs(z2 - z1 + 1);
        }
        
        int getMinX() { return Math.min(x1, x2); }
        int getMaxX() { return Math.max(x1, x2); }
        int getMinY() { return Math.min(y1, y2); }
        int getMaxY() { return Math.max(y1, y2); }
        int getMinZ() { return Math.min(z1, z2); }
        int getMaxZ() { return Math.max(z1, z2); }
    }
}