package com.yourname.minecraftcollaboration.commands;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
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
import com.yourname.minecraftcollaboration.util.BlockUtils;
import com.yourname.minecraftcollaboration.util.ValidationUtils;
import com.yourname.minecraftcollaboration.collaboration.CollaborationManager;
import com.yourname.minecraftcollaboration.models.Invitation;
import com.yourname.minecraftcollaboration.models.VisitRequest;

import java.util.List;
import java.util.UUID;

/**
 * Handles all collaboration and basic Minecraft commands from Scratch extension
 * Minecraft Forge 1.20.1 compatible version
 */
public class CollaborationCommandHandler {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private final CollaborationManager collaborationManager;
    
    public CollaborationCommandHandler() {
        LOGGER.debug("CollaborationCommandHandler initialized");
        this.collaborationManager = CollaborationManager.getInstance();
    }
    
    // === Connection Commands ===
    
    public String handleConnect(String[] args) {
        LOGGER.info("Handling connect command");
        return "connection.success(Minecraft Collaboration System Ready)";
    }
    
    public String handleStatus(String[] args) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Use safer method to get player count
            try {
                int playerCount = server.getPlayerCount();
                return "status.connected(Players: " + playerCount + ")";
            } catch (Exception e) {
                // Fallback method
                return "status.connected(Server active)";
            }
        }
        return "status.disconnected()";
    }
    
    // === Collaboration Commands ===
    
    public String handleInviteFriend(String[] args) {
        if (args.length < 1) {
            return "error.missingArgument(friendName required)";
        }
        
        String friendName = args[0];
        LOGGER.info("Handling invite friend: {}", friendName);
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            ServerPlayer sender = server.getPlayerList().getPlayers().get(0);
            String senderName = sender.getName().getString();
            
            // Create invitation
            Invitation invitation = collaborationManager.createInvitation(senderName, friendName);
            
            // Notify the friend if they're online
            ServerPlayer friend = server.getPlayerList().getPlayerByName(friendName);
            if (friend != null) {
                collaborationManager.notifyPlayer(friend, 
                    senderName + "さんから招待が届きました！");
            }
            
            return "invitation.sent(" + friendName + "," + invitation.getId() + ")";
        }
        
        return "error.serverNotFound()";
    }
    
    public String handleGetInvitations(String[] args) {
        LOGGER.debug("Handling get invitations");
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            String playerName = player.getName().getString();
            
            List<Invitation> invitations = collaborationManager.getInvitationsForPlayer(playerName);
            
            // Build response with invitation details
            StringBuilder response = new StringBuilder("invitations.list(");
            response.append(invitations.size()).append(",");
            
            for (int i = 0; i < invitations.size() && i < 5; i++) { // Limit to 5
                Invitation inv = invitations.get(i);
                response.append(inv.getSenderName()).append(",");
            }
            
            // Remove trailing comma if any
            if (response.charAt(response.length() - 1) == ',') {
                response.setLength(response.length() - 1);
            }
            response.append(")");
            
            return response.toString();
        }
        
        return "invitations.count(0)";
    }
    
    public String handleRequestVisit(String[] args) {
        if (args.length < 1) {
            return "error.missingArgument(friendName required)";
        }
        
        String friendName = args[0];
        LOGGER.info("Handling visit request to: {}", friendName);
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            ServerPlayer requester = server.getPlayerList().getPlayers().get(0);
            String requesterName = requester.getName().getString();
            
            // Create visit request
            VisitRequest request = collaborationManager.createVisitRequest(requesterName, friendName);
            
            // Notify the host if they're online
            ServerPlayer host = server.getPlayerList().getPlayerByName(friendName);
            if (host != null) {
                collaborationManager.notifyPlayer(host, 
                    requesterName + "さんが訪問を希望しています！");
            }
            
            return "visitRequest.sent(" + friendName + "," + request.getId() + ")";
        }
        
        return "error.serverNotFound()";
    }
    
    public String handleApproveVisit(String[] args) {
        if (args.length < 1) {
            return "error.missingArgument(visitorName required)";
        }
        
        String visitorName = args[0];
        LOGGER.info("Handling approve visit from: {}", visitorName);
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            ServerPlayer host = server.getPlayerList().getPlayers().get(0);
            String hostName = host.getName().getString();
            
            // Find the visit request
            List<VisitRequest> requests = collaborationManager.getVisitRequestsForHost(hostName);
            VisitRequest requestToApprove = requests.stream()
                .filter(req -> req.getRequesterName().equals(visitorName))
                .findFirst()
                .orElse(null);
                
            if (requestToApprove != null) {
                if (collaborationManager.approveVisitRequest(requestToApprove.getId())) {
                    // Teleport the visitor if they're online
                    ServerPlayer visitor = server.getPlayerList().getPlayerByName(visitorName);
                    if (visitor != null) {
                        // Teleport visitor to host's location
                        visitor.teleportTo(host.getX(), host.getY(), host.getZ());
                        collaborationManager.notifyPlayer(visitor, 
                            "訪問が承認されました！" + hostName + "さんの世界へようこそ！");
                        collaborationManager.setPlayerWorld(visitorName, hostName + "_world");
                    }
                    
                    return "visitApproval.sent(" + visitorName + ")";
                }
            }
            
            return "error.visitRequestNotFound(" + visitorName + ")";
        }
        
        return "error.serverNotFound()";
    }
    
    public String handleGetCurrentWorld(String[] args) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            String playerName = player.getName().getString();
            
            String currentWorld = collaborationManager.getPlayerCurrentWorld(playerName);
            boolean isHome = collaborationManager.isPlayerInHomeWorld(playerName);
            
            return "currentWorld.info(" + currentWorld + "," + isHome + ")";
        }
        
        return "currentWorld.name(unknown)";
    }
    
    public String handleReturnHome(String[] args) {
        LOGGER.info("Handling return home");
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            String playerName = player.getName().getString();
            
            if (collaborationManager.isPlayerInHomeWorld(playerName)) {
                return "returnHome.alreadyHome()";
            }
            
            // Get home world info and teleport back
            String homeWorld = collaborationManager.getPlayerHomeWorld(playerName);
            
            // For now, teleport to spawn point
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BlockPos spawnPos = overworld.getSharedSpawnPos();
                player.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                
                collaborationManager.setPlayerWorld(playerName, homeWorld);
                collaborationManager.notifyPlayer(player, "ホームワールドに帰還しました！");
                
                return "returnHome.success(" + homeWorld + ")";
            }
        }
        
        return "error.returnHomeFailed()";
    }
    
    public String handleEmergencyReturn(String[] args) {
        LOGGER.warn("Handling emergency return - immediate action");
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            String playerName = player.getName().getString();
            
            // Immediate teleport to spawn point without checks
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BlockPos spawnPos = overworld.getSharedSpawnPos();
                
                // Force teleport
                player.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5);
                
                // Reset player state
                player.setHealth(20.0f); // Full health
                player.getFoodData().setFoodLevel(20); // Full hunger
                player.clearFire(); // Remove fire
                player.removeAllEffects(); // Remove all potion effects
                
                // Update world tracking
                String homeWorld = collaborationManager.getPlayerHomeWorld(playerName);
                collaborationManager.setPlayerWorld(playerName, homeWorld);
                
                // Notify
                collaborationManager.notifyPlayer(player, "【緊急帰宅】安全にホームワールドに帰還しました！");
                collaborationManager.broadcastToAllPlayers(server, 
                    playerName + "さんが緊急帰宅を使用しました。");
                
                LOGGER.warn("Emergency return completed for player: {}", playerName);
                return "emergencyReturn.success(" + homeWorld + ")";
            }
        }
        
        return "error.emergencyReturnFailed()";
    }
    
    // === Basic Minecraft Commands ===
    
    public String handleGetPlayerPosition(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                List<ServerPlayer> players = server.getPlayerList().getPlayers();
                if (!players.isEmpty()) {
                    ServerPlayer player = players.get(0);
                    BlockPos pos = player.blockPosition();
                    // Return JSON format for consistency
                    return "{\"type\":\"playerPos\",\"data\":{\"x\":" + pos.getX() + 
                           ",\"y\":" + pos.getY() + ",\"z\":" + pos.getZ() + "}}";
                }
            }
            return "{\"type\":\"error\",\"error\":\"noPlayers\",\"message\":\"No players online\"}";
            
        } catch (Exception e) {
            LOGGER.error("Error getting player position", e);
            return "{\"type\":\"error\",\"error\":\"playerPosition\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
    
    public String handleSetBlock(String[] args) {
        if (args.length < 4) {
            return "error.missingArguments(x,y,z,blockType required)";
        }
        
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            String blockType = args[3];
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel world = server.getLevel(Level.OVERWORLD);
                if (world != null) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = BlockUtils.getBlockFromString(blockType);
                    
                    world.setBlockAndUpdate(pos, block.defaultBlockState());
                    return "block.set(" + x + "," + y + "," + z + "," + blockType + ")";
                }
            }
            return "error.worldNotFound()";
            
        } catch (NumberFormatException e) {
            return "error.invalidCoordinates()";
        } catch (Exception e) {
            LOGGER.error("Error setting block", e);
            return "error.setBlock(" + e.getMessage() + ")";
        }
    }
    
    public String handleGetBlock(String[] args) {
        if (args.length < 3) {
            return "error.missingArguments(x,y,z required)";
        }
        
        try {
            int x = Integer.parseInt(args[0]);
            int y = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel world = server.getLevel(Level.OVERWORLD);
                if (world != null) {
                    BlockPos pos = new BlockPos(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    return "block.type(" + block.getDescriptionId() + ")";
                }
            }
            return "error.worldNotFound()";
            
        } catch (NumberFormatException e) {
            return "error.invalidCoordinates()";
        } catch (Exception e) {
            LOGGER.error("Error getting block", e);
            return "error.getBlock(" + e.getMessage() + ")";
        }
    }
    
    public String handleChatMessage(String[] args) {
        if (args.length < 1) {
            return "error.missingArgument(message required)";
        }
        
        String message = String.join(" ", args);
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Component chatComponent = Component.literal("[Scratch] " + message);
                
                List<ServerPlayer> players = server.getPlayerList().getPlayers();
                for (ServerPlayer player : players) {
                    player.sendSystemMessage(chatComponent);
                }
                
                return "chat.sent(" + message + ")";
            }
            return "error.serverNotFound()";
            
        } catch (Exception e) {
            LOGGER.error("Error sending chat message", e);
            return "error.chat(" + e.getMessage() + ")";
        }
    }
    
    public String handleFillArea(String[] args) {
        if (args.length < 7) {
            return "{\"type\":\"error\",\"error\":\"missingArguments\",\"message\":\"x1,y1,z1,x2,y2,z2,blockType required\"}";
        }
        
        try {
            int x1 = Integer.parseInt(args[0]);
            int y1 = Integer.parseInt(args[1]);
            int z1 = Integer.parseInt(args[2]);
            int x2 = Integer.parseInt(args[3]);
            int y2 = Integer.parseInt(args[4]);
            int z2 = Integer.parseInt(args[5]);
            String blockType = args[6];
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerLevel world = server.getLevel(Level.OVERWORLD);
                if (world != null) {
                    Block block = BlockUtils.getBlockFromString(blockType);
                    
                    // Fill the area
                    for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
                        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                                BlockPos pos = new BlockPos(x, y, z);
                                world.setBlockAndUpdate(pos, block.defaultBlockState());
                            }
                        }
                    }
                    
                    return "{\"type\":\"fill\",\"status\":\"success\",\"message\":\"Filled area with " + blockType + "\"}";
                }
            }
            return "{\"type\":\"error\",\"error\":\"worldNotFound\",\"message\":\"World not found\"}";
            
        } catch (NumberFormatException e) {
            return "{\"type\":\"error\",\"error\":\"invalidCoordinates\",\"message\":\"Invalid coordinates\"}";
        } catch (Exception e) {
            LOGGER.error("Error filling area", e);
            return "{\"type\":\"error\",\"error\":\"fillError\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
    
    // === Agent Commands (Placeholder) ===
    
    public String handleSummonAgent(String[] args) {
        // TODO: Implement agent summoning when agent system is ready
        return "agent.summon(not_implemented)";
    }
    
    public String handleMoveAgent(String[] args) {
        // TODO: Implement agent movement when agent system is ready
        return "agent.move(not_implemented)";
    }
    
    // === Utility Methods ===
    
    // Method removed - now using BlockUtils.getBlockFromString()
}