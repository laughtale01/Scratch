package com.yourname.minecraftcollaboration.server;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import com.yourname.minecraftcollaboration.collaboration.CollaborationManager;
import com.yourname.minecraftcollaboration.models.Invitation;
import com.yourname.minecraftcollaboration.models.VisitRequest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinates collaboration between multiple Minecraft instances
 * Manages invitations, visits, and world switching for the collaboration system
 */
public class CollaborationCoordinator {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final MinecraftServer minecraftServer;
    private ExecutorService executorService;
    private volatile boolean isRunning;
    
    public CollaborationCoordinator(MinecraftServer server) {
        this.minecraftServer = server;
        this.isRunning = false;
    }
    
    /**
     * Start the collaboration coordination system
     */
    public void start() {
        if (isRunning) {
            LOGGER.warn("Collaboration coordinator is already running");
            return;
        }
        
        LOGGER.info("Starting collaboration coordinator...");
        
        executorService = Executors.newFixedThreadPool(2);
        isRunning = true;
        
        // Start background tasks
        startHeartbeat();
        
        LOGGER.info("Collaboration coordinator started successfully");
    }
    
    /**
     * Stop the collaboration coordination system
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        LOGGER.info("Stopping collaboration coordinator...");
        
        isRunning = false;
        
        if (executorService != null) {
            executorService.shutdown();
            try {
                // Wait for tasks to complete with timeout
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    LOGGER.warn("Forcing shutdown of executor service");
                    executorService.shutdownNow();
                    
                    if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        LOGGER.error("Executor service did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while shutting down executor service", e);
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        LOGGER.info("Collaboration coordinator stopped");
    }
    
    /**
     * Check if the coordinator is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Send an invitation to a friend
     */
    public CompletableFuture<Boolean> sendInvitation(String senderName, String friendName) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Sending invitation from {} to {}", senderName, friendName);
            
            // Create invitation through CollaborationManager
            CollaborationManager manager = CollaborationManager.getInstance();
            Invitation invitation = manager.createInvitation(senderName, friendName);
            
            if (invitation != null) {
                LOGGER.info("Invitation created successfully with ID: {}", invitation.getId());
                
                // Find the recipient player and notify them
                MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    ServerPlayer recipient = server.getPlayerList().getPlayerByName(friendName);
                    if (recipient != null) {
                        manager.notifyPlayer(recipient, senderName + " さんからワールド招待が届きました！");
                    }
                }
                return true;
            }
            
            return false;
        }, executorService);
    }
    
    /**
     * Request to visit a friend's world
     */
    public CompletableFuture<Boolean> requestVisit(String requesterName, String friendName) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Processing visit request from {} to {}", requesterName, friendName);
            
            // Create visit request through CollaborationManager
            CollaborationManager manager = CollaborationManager.getInstance();
            VisitRequest request = manager.createVisitRequest(requesterName, friendName);
            
            if (request != null) {
                LOGGER.info("Visit request created successfully with ID: {}", request.getId());
                
                // Find the host player and notify them
                MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    ServerPlayer host = server.getPlayerList().getPlayerByName(friendName);
                    if (host != null) {
                        manager.notifyPlayer(host, requesterName + " さんがあなたのワールドへの訪問を申請しています！");
                    }
                }
                return true;
            }
            
            return false;
        }, executorService);
    }
    
    /**
     * Approve a visit request
     */
    public CompletableFuture<Boolean> approveVisit(String hostName, String visitorName) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Approving visit from {} to {}'s world", visitorName, hostName);
            
            CollaborationManager manager = CollaborationManager.getInstance();
            
            // Find the matching visit request
            List<VisitRequest> requests = manager.getVisitRequestsForHost(hostName);
            Optional<VisitRequest> matchingRequest = requests.stream()
                .filter(req -> req.getRequesterName().equals(visitorName))
                .findFirst();
                
            if (matchingRequest.isPresent()) {
                MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    boolean approved = manager.approveVisitRequest(matchingRequest.get().getId(), server);
                    if (approved) {
                        LOGGER.info("Visit approved and visitor transferred successfully");
                        return true;
                    }
                }
            } else {
                LOGGER.warn("No matching visit request found from {} to {}", visitorName, hostName);
            }
            
            return false;
        }, executorService);
    }
    
    /**
     * Return a player to their home world
     */
    public CompletableFuture<Boolean> returnPlayerHome(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.info("Returning player {} to home world", playerName);
            
            MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                if (player != null) {
                    CollaborationManager manager = CollaborationManager.getInstance();
                    boolean returned = manager.returnPlayerHome(player);
                    
                    if (returned) {
                        LOGGER.info("Player {} returned home successfully", playerName);
                        return true;
                    }
                }
            }
            
            LOGGER.warn("Failed to return player {} home", playerName);
            return false;
        }, executorService);
    }
    
    /**
     * Emergency return - immediately return player to home world
     */
    public CompletableFuture<Boolean> emergencyReturn(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            LOGGER.warn("Emergency return initiated for player {}", playerName);
            
            MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                if (player != null) {
                    CollaborationManager manager = CollaborationManager.getInstance();
                    boolean returned = manager.emergencyReturnPlayer(player);
                    
                    if (returned) {
                        LOGGER.info("Emergency return completed for player {}", playerName);
                        
                        // Log the emergency event
                        LOGGER.warn("EMERGENCY EVENT: Player {} used emergency return at {}", 
                            playerName, new java.util.Date());
                            
                        // Notify administrators
                        manager.broadcastToAllPlayers(server, 
                            "[緊急] " + playerName + " さんが緊急帰宅機能を使用しました。");
                            
                        return true;
                    }
                }
            }
            
            LOGGER.error("Failed emergency return for player {}", playerName);
            return false;
        }, executorService);
    }
    
    /**
     * Get the current world name for a player
     */
    public String getCurrentWorld(String playerName) {
        CollaborationManager manager = CollaborationManager.getInstance();
        String worldName = manager.getPlayerCurrentWorld(playerName);
        
        if ("unknown".equals(worldName)) {
            // Try to get from server
            MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                if (player != null) {
                    worldName = player.level().dimension().location().toString();
                    manager.setPlayerWorld(playerName, worldName);
                }
            }
        }
        
        return worldName;
    }
    
    /**
     * Start heartbeat system to maintain coordination
     */
    private void startHeartbeat() {
        executorService.submit(() -> {
            while (isRunning) {
                try {
                    // Heartbeat every 30 seconds
                    Thread.sleep(30000);
                    
                    if (isRunning) {
                        LOGGER.debug("Collaboration coordinator heartbeat");
                        
                        // TODO: Implement heartbeat logic
                        // This could involve:
                        // 1. Checking connection to other servers
                        // 2. Synchronizing player states
                        // 3. Cleaning up old invitations/requests
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
}