package com.yourname.minecraftcollaboration.collaboration;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import com.yourname.minecraftcollaboration.models.Invitation;
import com.yourname.minecraftcollaboration.models.VisitRequest;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages invitations and visit requests for the collaboration system
 */
public class CollaborationManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static CollaborationManager instance;
    
    private final Map<UUID, Invitation> invitations = new ConcurrentHashMap<>();
    private final Map<UUID, VisitRequest> visitRequests = new ConcurrentHashMap<>();
    private final Map<String, String> playerWorldMap = new ConcurrentHashMap<>(); // player -> current world
    private final Map<String, String> playerHomeMap = new ConcurrentHashMap<>(); // player -> home world
    
    private CollaborationManager() {
        // Private constructor for singleton
    }
    
    public static CollaborationManager getInstance() {
        if (instance == null) {
            instance = new CollaborationManager();
        }
        return instance;
    }
    
    // === Invitation Management ===
    
    public Invitation createInvitation(String senderName, String recipientName) {
        // Clean up expired invitations first
        cleanupExpiredInvitations();
        
        // Check if there's already a pending invitation
        Optional<Invitation> existing = invitations.values().stream()
            .filter(inv -> inv.getSenderName().equals(senderName) 
                && inv.getRecipientName().equals(recipientName)
                && inv.getStatus() == Invitation.InvitationStatus.PENDING)
            .findFirst();
            
        if (existing.isPresent()) {
            LOGGER.info("Invitation already exists from {} to {}", senderName, recipientName);
            return existing.get();
        }
        
        Invitation invitation = new Invitation(senderName, recipientName);
        invitations.put(invitation.getId(), invitation);
        LOGGER.info("Created invitation from {} to {} with ID {}", senderName, recipientName, invitation.getId());
        
        return invitation;
    }
    
    public List<Invitation> getInvitationsForPlayer(String playerName) {
        return invitations.values().stream()
            .filter(inv -> inv.getRecipientName().equals(playerName) 
                && inv.getStatus() == Invitation.InvitationStatus.PENDING)
            .collect(Collectors.toList());
    }
    
    public boolean acceptInvitation(UUID invitationId) {
        Invitation invitation = invitations.get(invitationId);
        if (invitation != null && invitation.getStatus() == Invitation.InvitationStatus.PENDING) {
            invitation.setStatus(Invitation.InvitationStatus.ACCEPTED);
            LOGGER.info("Invitation {} accepted", invitationId);
            return true;
        }
        return false;
    }
    
    public boolean declineInvitation(UUID invitationId) {
        Invitation invitation = invitations.get(invitationId);
        if (invitation != null && invitation.getStatus() == Invitation.InvitationStatus.PENDING) {
            invitation.setStatus(Invitation.InvitationStatus.DECLINED);
            LOGGER.info("Invitation {} declined", invitationId);
            return true;
        }
        return false;
    }
    
    // === Visit Request Management ===
    
    public VisitRequest createVisitRequest(String requesterName, String hostName) {
        // Clean up expired requests first
        cleanupExpiredVisitRequests();
        
        // Check if there's already a pending request
        Optional<VisitRequest> existing = visitRequests.values().stream()
            .filter(req -> req.getRequesterName().equals(requesterName) 
                && req.getHostName().equals(hostName)
                && req.getStatus() == VisitRequest.VisitStatus.PENDING)
            .findFirst();
            
        if (existing.isPresent()) {
            LOGGER.info("Visit request already exists from {} to {}", requesterName, hostName);
            return existing.get();
        }
        
        VisitRequest request = new VisitRequest(requesterName, hostName);
        visitRequests.put(request.getId(), request);
        LOGGER.info("Created visit request from {} to {} with ID {}", requesterName, hostName, request.getId());
        
        return request;
    }
    
    public List<VisitRequest> getVisitRequestsForHost(String hostName) {
        return visitRequests.values().stream()
            .filter(req -> req.getHostName().equals(hostName) 
                && req.getStatus() == VisitRequest.VisitStatus.PENDING)
            .collect(Collectors.toList());
    }
    
    public boolean approveVisitRequest(UUID requestId, net.minecraft.server.MinecraftServer server) {
        VisitRequest request = visitRequests.get(requestId);
        if (request != null && request.getStatus() == VisitRequest.VisitStatus.PENDING) {
            request.setStatus(VisitRequest.VisitStatus.APPROVED);
            LOGGER.info("Visit request {} approved", requestId);
            
            // Actually teleport the player
            ServerPlayer requester = server.getPlayerList().getPlayerByName(request.getRequesterName());
            ServerPlayer host = server.getPlayerList().getPlayerByName(request.getHostName());
            
            if (requester != null && host != null) {
                // Save current position as return point
                playerHomeMap.putIfAbsent(request.getRequesterName(), requester.level().dimension().location().toString());
                
                // Teleport to host
                requester.teleportTo(host.getX(), host.getY(), host.getZ());
                
                // Update world tracking
                setPlayerWorld(request.getRequesterName(), host.level().dimension().location().toString());
                
                // Notify both players
                notifyPlayer(requester, "あなたは " + request.getHostName() + " さんのワールドにテレポートしました！");
                notifyPlayer(host, request.getRequesterName() + " さんがあなたのワールドに訪問しました！");
                
                LOGGER.info("Teleported {} to {}'s location", request.getRequesterName(), request.getHostName());
                return true;
            } else {
                LOGGER.warn("Could not find players for teleport: requester={}, host={}", 
                    request.getRequesterName(), request.getHostName());
            }
        }
        return false;
    }
    
    public boolean denyVisitRequest(UUID requestId) {
        VisitRequest request = visitRequests.get(requestId);
        if (request != null && request.getStatus() == VisitRequest.VisitStatus.PENDING) {
            request.setStatus(VisitRequest.VisitStatus.DENIED);
            LOGGER.info("Visit request {} denied", requestId);
            return true;
        }
        return false;
    }
    
    // === World Management ===
    
    // Store player's original position when they first join
    private final Map<String, PlayerPosition> playerHomePositions = new ConcurrentHashMap<>();
    
    public void setPlayerWorld(String playerName, String worldName) {
        playerWorldMap.put(playerName, worldName);
        // If this is the first world set for the player, it's their home
        playerHomeMap.putIfAbsent(playerName, worldName);
    }
    
    public String getPlayerCurrentWorld(String playerName) {
        return playerWorldMap.getOrDefault(playerName, "unknown");
    }
    
    public String getPlayerHomeWorld(String playerName) {
        return playerHomeMap.getOrDefault(playerName, playerWorldMap.getOrDefault(playerName, "unknown"));
    }
    
    public boolean isPlayerInHomeWorld(String playerName) {
        String current = getPlayerCurrentWorld(playerName);
        String home = getPlayerHomeWorld(playerName);
        return current.equals(home);
    }
    
    public void savePlayerHomePosition(String playerName, double x, double y, double z, String dimension) {
        playerHomePositions.put(playerName, new PlayerPosition(x, y, z, dimension));
    }
    
    public boolean returnPlayerHome(ServerPlayer player) {
        if (player == null) return false;
        
        String playerName = player.getName().getString();
        PlayerPosition homePos = playerHomePositions.get(playerName);
        
        if (homePos != null) {
            // Teleport to home position
            player.teleportTo(homePos.x, homePos.y, homePos.z);
            
            // Update world tracking
            setPlayerWorld(playerName, homePos.dimension);
            
            // Notify player
            notifyPlayer(player, "ホームワールドに戻りました！");
            
            LOGGER.info("Returned player {} to home position", playerName);
            return true;
        } else {
            // If no home position saved, teleport to spawn
            player.teleportTo(
                player.getRespawnPosition().getX(),
                player.getRespawnPosition().getY(), 
                player.getRespawnPosition().getZ()
            );
            notifyPlayer(player, "スポーン地点に戻りました！");
            return true;
        }
    }
    
    public boolean emergencyReturnPlayer(ServerPlayer player) {
        if (player == null) return false;
        
        // First return home
        boolean returned = returnPlayerHome(player);
        
        if (returned) {
            // Restore health and hunger
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0F);
            
            // Clear negative effects
            player.removeAllEffects();
            
            // Notify player
            notifyPlayer(player, "緊急帰宅しました！体力と空腹度が回復しました。");
            
            LOGGER.info("Emergency return for player {} completed", player.getName().getString());
        }
        
        return returned;
    }
    
    // Inner class for storing position data
    private static class PlayerPosition {
        public final double x, y, z;
        public final String dimension;
        
        public PlayerPosition(double x, double y, double z, String dimension) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dimension = dimension;
        }
    }
    
    // === Cleanup Methods ===
    
    private void cleanupExpiredInvitations() {
        invitations.entrySet().removeIf(entry -> {
            Invitation inv = entry.getValue();
            if (inv.isExpired() && inv.getStatus() == Invitation.InvitationStatus.PENDING) {
                inv.setStatus(Invitation.InvitationStatus.EXPIRED);
                LOGGER.debug("Expired invitation {}", inv.getId());
                return true;
            }
            return false;
        });
    }
    
    private void cleanupExpiredVisitRequests() {
        visitRequests.entrySet().removeIf(entry -> {
            VisitRequest req = entry.getValue();
            if (req.isExpired() && req.getStatus() == VisitRequest.VisitStatus.PENDING) {
                req.setStatus(VisitRequest.VisitStatus.EXPIRED);
                LOGGER.debug("Expired visit request {}", req.getId());
                return true;
            }
            return false;
        });
    }
    
    // === Notification Methods ===
    
    public void notifyPlayer(ServerPlayer player, String message) {
        if (player != null) {
            player.sendSystemMessage(Component.literal("[協調システム] " + message));
        }
    }
    
    public void broadcastToAllPlayers(net.minecraft.server.MinecraftServer server, String message) {
        if (server != null) {
            Component msg = Component.literal("[協調システム] " + message);
            server.getPlayerList().getPlayers().forEach(player -> 
                player.sendSystemMessage(msg)
            );
        }
    }
}