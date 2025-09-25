package edu.minecraft.collaboration.server;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.collaboration.CollaborationManager;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.models.Invitation;
import edu.minecraft.collaboration.models.VisitRequest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates collaboration activities between players across the server
 */
public final class CollaborationCoordinator {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static CollaborationCoordinator instance;

    private final CollaborationManager collaborationManager;
    private final Map<String, Set<String>> playerFriends = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> activeCollaborations = new ConcurrentHashMap<>();

    // Coordination metrics
    private final Map<String, Integer> collaborationCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastActivityTimes = new ConcurrentHashMap<>();

    private CollaborationCoordinator() {
        this.collaborationManager = DependencyInjector.getInstance().getService(CollaborationManager.class);
        LOGGER.info("Collaboration coordinator initialized");
    }

    public static synchronized CollaborationCoordinator getInstance() {
        if (instance == null) {
            instance = new CollaborationCoordinator();
        }
        return instance;
    }

    /**
     * Handle friend invitation process
     */
    public boolean inviteFriend(String senderName, String friendName) {
        if (senderName.equals(friendName)) {
            LOGGER.warn("Player {} tried to invite themselves", senderName);
            return false;
        }

        // Check if already friends
        if (areFriends(senderName, friendName)) {
            LOGGER.info("Players {} and {} are already friends", senderName, friendName);
            return false;
        }

        // Create invitation
        Invitation invitation = collaborationManager.createInvitation(senderName, friendName);
        if (invitation != null) {
            // Notify recipient
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer recipient = server.getPlayerList().getPlayerByName(friendName);
                if (recipient != null) {
                    collaborationManager.notifyPlayer(recipient,
                        senderName + " has sent you a friend invitation. Use /accept or /decline to respond.");
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Handle visit request process
     */
    public boolean requestVisit(String requesterName, String hostName) {
        if (requesterName.equals(hostName)) {
            LOGGER.warn("Player {} tried to visit themselves", requesterName);
            return false;
        }

        // Create visit request
        VisitRequest request = collaborationManager.createVisitRequest(requesterName, hostName);
        if (request != null) {
            // Notify host
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                ServerPlayer host = server.getPlayerList().getPlayerByName(hostName);
                if (host != null) {
                    collaborationManager.notifyPlayer(host,
                        requesterName + " wants to visit your world. Use /approve or /deny to respond.");
                }
            }
            return true;
        }

        return false;
    }

    /**
     * Process friend invitation acceptance
     */
    public boolean acceptFriendInvitation(String playerName, UUID invitationId) {
        boolean accepted = collaborationManager.acceptInvitation(invitationId);
        if (accepted) {
            // Find the invitation to get sender info
            List<Invitation> invitations = collaborationManager.getInvitationsForPlayer(playerName);
            for (Invitation inv : invitations) {
                if (inv.getId().equals(invitationId)) {
                    addFriendship(inv.getSenderName(), playerName);

                    // Notify both players
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null) {
                        ServerPlayer sender = server.getPlayerList().getPlayerByName(inv.getSenderName());
                        ServerPlayer recipient = server.getPlayerList().getPlayerByName(playerName);

                        if (sender != null) {
                            collaborationManager.notifyPlayer(sender,
                                playerName + " accepted your friend invitation!");
                        }
                        if (recipient != null) {
                            collaborationManager.notifyPlayer(recipient,
                                "You are now friends with " + inv.getSenderName() + "!");
                        }
                    }
                    break;
                }
            }
        }
        return accepted;
    }

    /**
     * Process visit request approval
     */
    public boolean approveVisitRequest(String hostName, UUID requestId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        boolean approved = collaborationManager.approveVisitRequest(requestId, server);

        if (approved) {
            // Track collaboration activity
            recordCollaborationActivity(hostName);
            updateLastActivity(hostName);

            LOGGER.info("Visit request {} approved by {}", requestId, hostName);
        }

        return approved;
    }

    /**
     * Return player to home safely
     */
    public boolean returnPlayerHome(String playerName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
            if (player != null) {
                boolean returned = collaborationManager.returnPlayerHome(player);
                if (returned) {
                    LOGGER.info("Player {} returned home", playerName);
                }
                return returned;
            }
        }
        return false;
    }

    /**
     * Emergency return with full restoration
     */
    public boolean emergencyReturn(String playerName) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
            if (player != null) {
                boolean returned = collaborationManager.emergencyReturnPlayer(player);
                if (returned) {
                    LOGGER.info("Emergency return completed for player {}", playerName);

                    // Broadcast emergency return to all players
                    collaborationManager.broadcastToAllPlayers(server,
                        playerName + " used emergency return and is now safe at home.");
                }
                return returned;
            }
        }
        return false;
    }

    /**
     * Start collaboration session between players
     */
    public boolean startCollaboration(String player1, String player2) {
        if (player1.equals(player2)) {
            return false;
        }

        // Create collaboration session
        Set<String> collaboration = new HashSet<>();
        collaboration.add(player1);
        collaboration.add(player2);

        String sessionId = generateCollaborationId(player1, player2);
        activeCollaborations.put(sessionId, collaboration);

        // Notify participants
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer p1 = server.getPlayerList().getPlayerByName(player1);
            ServerPlayer p2 = server.getPlayerList().getPlayerByName(player2);

            if (p1 != null) {
                collaborationManager.notifyPlayer(p1,
                    "Collaboration session started with " + player2);
            }
            if (p2 != null) {
                collaborationManager.notifyPlayer(p2,
                    "Collaboration session started with " + player1);
            }
        }

        // Record activity
        recordCollaborationActivity(player1);
        recordCollaborationActivity(player2);

        LOGGER.info("Collaboration session started between {} and {}", player1, player2);
        return true;
    }

    /**
     * End collaboration session
     */
    public boolean endCollaboration(String sessionId) {
        Set<String> collaboration = activeCollaborations.remove(sessionId);
        if (collaboration != null) {
            // Notify participants
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (String playerName : collaboration) {
                    ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
                    if (player != null) {
                        collaborationManager.notifyPlayer(player,
                            "Collaboration session ended");
                    }
                }
            }

            LOGGER.info("Collaboration session {} ended", sessionId);
            return true;
        }
        return false;
    }

    /**
     * Check if two players are friends
     */
    public boolean areFriends(String player1, String player2) {
        Set<String> friends1 = playerFriends.get(player1);
        Set<String> friends2 = playerFriends.get(player2);

        return (friends1 != null && friends1.contains(player2)) ||
             (friends2 != null && friends2.contains(player1));
    }

    /**
     * Add friendship between two players
     */
    private void addFriendship(String player1, String player2) {
        playerFriends.computeIfAbsent(player1, k -> new HashSet<>()).add(player2);
        playerFriends.computeIfAbsent(player2, k -> new HashSet<>()).add(player1);

        LOGGER.info("Friendship established between {} and {}", player1, player2);
    }

    /**
     * Get player's friends list
     */
    public Set<String> getPlayerFriends(String playerName) {
        return playerFriends.getOrDefault(playerName, new HashSet<>());
    }

    /**
     * Get active collaborations for a player
     */
    public List<String> getPlayerActiveCollaborations(String playerName) {
        List<String> playerCollabs = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : activeCollaborations.entrySet()) {
            if (entry.getValue().contains(playerName)) {
                playerCollabs.add(entry.getKey());
            }
        }

        return playerCollabs;
    }

    /**
     * Record collaboration activity
     */
    private void recordCollaborationActivity(String playerName) {
        collaborationCounts.merge(playerName, 1, Integer::sum);
        updateLastActivity(playerName);
    }

    /**
     * Update last activity time
     */
    private void updateLastActivity(String playerName) {
        lastActivityTimes.put(playerName, System.currentTimeMillis());
    }

    /**
     * Generate unique collaboration session ID
     */
    private String generateCollaborationId(String player1, String player2) {
        List<String> players = Arrays.asList(player1, player2);
        players.sort(String::compareTo);
        return "collab_" + String.join("_", players) + "_" + System.currentTimeMillis();
    }

    /**
     * Get collaboration statistics for a player
     */
    public Map<String, Object> getPlayerStats(String playerName) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("friendsCount", getPlayerFriends(playerName).size());
        stats.put("collaborationCount", collaborationCounts.getOrDefault(playerName, 0));
        stats.put("activeCollaborations", getPlayerActiveCollaborations(playerName).size());
        stats.put("lastActivity", lastActivityTimes.get(playerName));

        return stats;
    }

    /**
     * Get overall collaboration statistics
     */
    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalFriendships", playerFriends.size());
        stats.put("activeCollaborations", activeCollaborations.size());
        stats.put("totalCollaborationActivities",
            collaborationCounts.values().stream().mapToInt(Integer::intValue).sum());

        return stats;
    }

    /**
     * Clean up expired sessions and data
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        long expireTime = 24 * 60 * 60 * 1000; // 24 hours

        // Remove old last activity records
        lastActivityTimes.entrySet().removeIf(entry ->
            (now - entry.getValue()) > expireTime);

        LOGGER.debug("Collaboration coordinator cleanup completed");
    }

    /**
     * Save player home position when they join
     */
    public void initializePlayerHome(ServerPlayer player) {
        String playerName = player.getName().getString();
        collaborationManager.savePlayerHomePosition(
            playerName,
            player.getX(),
            player.getY(),
            player.getZ(),
            player.level().dimension().location().toString()
        );

        collaborationManager.setPlayerWorld(playerName,
            player.level().dimension().location().toString());

        LOGGER.debug("Initialized home position for player {}", playerName);
    }

    /**
     * Start coordination services
     */
    public void start() {
        // Start background coordination tasks
        LOGGER.info("Collaboration coordinator started");
    }

    /**
     * Stop coordination services
     */
    public void stop() {
        // Stop background coordination tasks
        LOGGER.info("Collaboration coordinator stopped");
    }

    /**
     * Check if coordinator is running
     */
    public boolean isRunning() {
        return true; // Simplified - always running when instantiated
    }
}
