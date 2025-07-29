package edu.minecraft.collaboration.commands;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Component chatComponent = Component.literal("[Scratch] " + message);
                server.getPlayerList().broadcastSystemMessage(chatComponent, false);
                metricsCollector.incrementCounter("commands.chat");
                return ResponseHelper.chatSent(message);
            }
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
            int x1 = Integer.parseInt(args[0]);
            int y1 = Integer.parseInt(args[1]);
            int z1 = Integer.parseInt(args[2]);
            int x2 = Integer.parseInt(args[3]);
            int y2 = Integer.parseInt(args[4]);
            int z2 = Integer.parseInt(args[5]);
            String blockType = args[6];
            
            // Validate coordinates
            if (!ValidationUtils.isValidCoordinate(x1, y1, z1) || !ValidationUtils.isValidCoordinate(x2, y2, z2)) {
                return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INVALID_COORDS, "Invalid coordinates");
            }
            
            // Validate block type
            if (!ValidationUtils.isValidBlockType(blockType)) {
                return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INVALID_BLOCK, "Invalid block type: " + blockType);
            }
            
            // Calculate area size and validate
            int volume = Math.abs(x2 - x1 + 1) * Math.abs(y2 - y1 + 1) * Math.abs(z2 - z1 + 1);
            if (volume > 10000) {
                return ResponseHelper.error("fillArea", ResponseHelper.ERROR_AREA_TOO_LARGE, "Area too large (max 10000 blocks)");
            }
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel world = server.getLevel(Level.OVERWORLD);
                if (world != null) {
                    Block block = BlockUtils.getBlockFromString(blockType);
                    
                    if (block != null) {
                        // Fill the area
                        int minX = Math.min(x1, x2);
                        int maxX = Math.max(x1, x2);
                        int minY = Math.min(y1, y2);
                        int maxY = Math.max(y1, y2);
                        int minZ = Math.min(z1, z2);
                        int maxZ = Math.max(z1, z2);
                        
                        int blocksSet = 0;
                        for (int x = minX; x <= maxX; x++) {
                            for (int y = minY; y <= maxY; y++) {
                                for (int z = minZ; z <= maxZ; z++) {
                                    BlockPos pos = new BlockPos(x, y, z);
                                    world.setBlockAndUpdate(pos, block.defaultBlockState());
                                    blocksSet++;
                                }
                            }
                        }
                        
                        metricsCollector.incrementCounter("commands.fillArea");
                        return ResponseHelper.areaFilled(minX, minY, minZ, maxX, maxY, maxZ, blockType, blocksSet);
                    } else {
                        return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INVALID_BLOCK, "Unknown block type: " + blockType);
                    }
                }
            }
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_SERVER_UNAVAILABLE, "Server not available");
        } catch (NumberFormatException e) {
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INVALID_ARGS, "Invalid coordinate format");
        } catch (Exception e) {
            LOGGER.error("Error filling area", e);
            return ResponseHelper.error("fillArea", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
}