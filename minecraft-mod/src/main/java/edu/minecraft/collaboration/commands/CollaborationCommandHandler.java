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
import edu.minecraft.collaboration.collaboration.CollaborationManager;
import edu.minecraft.collaboration.models.Invitation;
import edu.minecraft.collaboration.models.VisitRequest;
import edu.minecraft.collaboration.entities.AgentManager;
import edu.minecraft.collaboration.entities.CollaborationAgent;
import edu.minecraft.collaboration.teacher.TeacherManager;
import edu.minecraft.collaboration.teacher.StudentActivity;
import edu.minecraft.collaboration.progress.ProgressTracker;
import edu.minecraft.collaboration.localization.LanguageManager;
import edu.minecraft.collaboration.blockpacks.BlockPackManager;
import edu.minecraft.collaboration.blockpacks.BlockPack;
import edu.minecraft.collaboration.offline.OfflineModeManager;
import edu.minecraft.collaboration.monitoring.MetricsCollector;
import edu.minecraft.collaboration.monitoring.MetricsReporter;

import java.util.List;
import java.util.UUID;

/**
 * Handles all collaboration and basic Minecraft commands from Scratch extension
 * Minecraft Forge 1.20.1 compatible version
 */
public class CollaborationCommandHandler {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private final CollaborationManager collaborationManager;
    private final TeacherManager teacherManager;
    private final ProgressTracker progressTracker;
    private final LanguageManager languageManager;
    private final BlockPackManager blockPackManager;
    private final OfflineModeManager offlineModeManager;
    private final MetricsCollector metricsCollector;
    private final MetricsReporter metricsReporter;
    
    // Specialized command handlers
    private final BasicCommandHandler basicHandler;
    private final AgentCommandHandler agentHandler;
    private final TeacherCommandHandler teacherHandler;
    
    public CollaborationCommandHandler() {
        LOGGER.debug("CollaborationCommandHandler initialized");
        this.collaborationManager = CollaborationManager.getInstance();
        this.teacherManager = TeacherManager.getInstance();
        this.progressTracker = ProgressTracker.getInstance();
        this.languageManager = LanguageManager.getInstance();
        this.blockPackManager = BlockPackManager.getInstance();
        this.offlineModeManager = OfflineModeManager.getInstance();
        this.metricsCollector = MetricsCollector.getInstance();
        this.metricsReporter = MetricsReporter.getInstance();
        
        // Initialize specialized handlers
        this.basicHandler = new BasicCommandHandler(metricsCollector);
        this.agentHandler = new AgentCommandHandler(metricsCollector);
        this.teacherHandler = new TeacherCommandHandler(teacherManager, metricsCollector);
    }
    
    // === Connection Commands ===
    
    public String handleConnect(String[] args) {
        return basicHandler.handleConnect(args);
    }
    
    public String handleStatus(String[] args) {
        return basicHandler.handleStatus(args);
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
                    senderName + " has sent you an invitation.");
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
            
            // Return standardized invitation count response
            return ResponseHelper.invitationCount(invitations.size());
        }
        
        return ResponseHelper.invitationCount(0);
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
                    requesterName + " wants to visit your world.");
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
                if (collaborationManager.approveVisitRequest(requestToApprove.getId(), server)) {
                    // Track activity for classroom mode
                    trackActivityIfClassroomMode("visit_approved", "Visited " + hostName + "'s world");
                    
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
            
            // Use CollaborationManager's returnPlayerHome method
            if (collaborationManager.returnPlayerHome(player)) {
                return "returnHome.success(" + collaborationManager.getPlayerHomeWorld(playerName) + ")";
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
            
            // Use CollaborationManager's emergencyReturnPlayer method
            if (collaborationManager.emergencyReturnPlayer(player)) {
                // Track activity for classroom mode
                trackActivityIfClassroomMode("emergency_return", "Emergency return to home world");
                
                LOGGER.warn("Emergency return completed for player: {}", playerName);
                return "emergencyReturn.success(" + collaborationManager.getPlayerHomeWorld(playerName) + ")";
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
                    return ResponseHelper.playerPosition(pos.getX(), pos.getY(), pos.getZ());
                }
            }
            return ResponseHelper.error("getPlayerPos", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error getting player position", e);
            return ResponseHelper.error("getPlayerPos", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleSetBlock(String[] args) {
        if (args.length < 4) {
            return ResponseHelper.error("setBlock", ResponseHelper.ERROR_INVALID_PARAMS, "x,y,z,blockType required");
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
                    
                    // Track activity for classroom mode
                    trackActivityIfClassroomMode("place_block", blockType + " at (" + x + "," + y + "," + z + ")");
                    
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
                
                // Track activity for classroom mode
                trackActivityIfClassroomMode("chat", message);
                
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
    
    // === Agent Commands ===
    
    public String handleSummonAgent(String[] args) {
        String agentName = args.length > 0 ? args[0] : "Agent";
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                AgentManager agentManager = AgentManager.getInstance();
                CollaborationAgent agent = agentManager.summonAgent(player, agentName);
                
                if (agent != null) {
                    return ResponseHelper.success("summonAgent", 
                        "Summoned agent '" + agent.getAgentName() + "' with ID: " + agent.getAgentId().substring(0, 8));
                } else {
                    return ResponseHelper.error("summonAgent", ResponseHelper.ERROR_INTERNAL, "Failed to summon agent");
                }
            }
            return ResponseHelper.error("summonAgent", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error summoning agent", e);
            return ResponseHelper.error("summonAgent", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleMoveAgent(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_PARAMS, "Direction or coordinates required");
        }
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                AgentManager agentManager = AgentManager.getInstance();
                
                // Check if first argument is a direction or coordinate
                String firstArg = args[0].toLowerCase();
                boolean moved = false;
                
                if (firstArg.matches("forward|backward|left|right|up|down|north|south|east|west")) {
                    // Direction-based movement
                    int distance = args.length > 1 ? ValidationUtils.parseIntSafely(args[1], 1) : 1;
                    distance = ValidationUtils.clamp(distance, 1, 10); // Limit distance
                    
                    moved = agentManager.moveAgentInDirection(player.getUUID(), firstArg, distance);
                    
                    if (moved) {
                        return ResponseHelper.success("moveAgent", 
                            "Moved agent " + firstArg + " by " + distance + " blocks");
                    }
                } else if (args.length >= 3) {
                    // Coordinate-based movement
                    int x = ValidationUtils.parseIntSafely(args[0], 0);
                    int y = ValidationUtils.parseIntSafely(args[1], 64);
                    int z = ValidationUtils.parseIntSafely(args[2], 0);
                    
                    if (!ValidationUtils.isValidBlockPos(new BlockPos(x, y, z))) {
                        return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_PARAMS, "Invalid coordinates");
                    }
                    
                    moved = agentManager.moveAgent(player.getUUID(), new BlockPos(x, y, z));
                    
                    if (moved) {
                        return ResponseHelper.success("moveAgent", 
                            "Moving agent to (" + x + ", " + y + ", " + z + ")");
                    }
                }
                
                if (!moved) {
                    return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_NOT_FOUND, "No agent found for player");
                }
            }
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error moving agent", e);
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleAgentFollow(String[] args) {
        boolean follow = args.length > 0 && "true".equalsIgnoreCase(args[0]);
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                AgentManager agentManager = AgentManager.getInstance();
                
                if (agentManager.setAgentFollow(player.getUUID(), follow)) {
                    return ResponseHelper.success("agentFollow", 
                        follow ? "Agent is now following you" : "Agent stopped following");
                } else {
                    return ResponseHelper.error("agentFollow", ResponseHelper.ERROR_NOT_FOUND, "No agent found for player");
                }
            }
            return ResponseHelper.error("agentFollow", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error setting agent follow", e);
            return ResponseHelper.error("agentFollow", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleAgentAction(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("agentAction", ResponseHelper.ERROR_INVALID_PARAMS, "Action required (jump, spin, dance)");
        }
        
        String action = args[0];
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                AgentManager agentManager = AgentManager.getInstance();
                
                if (agentManager.agentPerformAction(player.getUUID(), action)) {
                    return ResponseHelper.success("agentAction", "Agent performed action: " + action);
                } else {
                    return ResponseHelper.error("agentAction", ResponseHelper.ERROR_NOT_FOUND, "No agent found for player");
                }
            }
            return ResponseHelper.error("agentAction", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error performing agent action", e);
            return ResponseHelper.error("agentAction", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleDismissAgent(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                AgentManager agentManager = AgentManager.getInstance();
                
                if (agentManager.removeAgent(player.getUUID())) {
                    return ResponseHelper.success("dismissAgent", "Agent dismissed");
                } else {
                    return ResponseHelper.error("dismissAgent", ResponseHelper.ERROR_NOT_FOUND, "No agent found for player");
                }
            }
            return ResponseHelper.error("dismissAgent", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error dismissing agent", e);
            return ResponseHelper.error("dismissAgent", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    // === Teacher Management Commands ===
    
    public String handleRegisterTeacher(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("registerTeacher", ResponseHelper.ERROR_INVALID_PARAMS, "Password required");
        }
        
        String password = args[0];
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                teacherManager.registerTeacher(player.getUUID(), password);
                player.sendSystemMessage(Component.literal("§aTeacher account registered successfully."));
                
                return ResponseHelper.success("registerTeacher", "Teacher account registered");
            }
            return ResponseHelper.error("registerTeacher", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error registering teacher", e);
            return ResponseHelper.error("registerTeacher", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleToggleClassroomMode(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(player.getUUID())) {
                    return ResponseHelper.error("toggleClassroomMode", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                boolean newMode = !teacherManager.isClassroomMode();
                teacherManager.setClassroomMode(newMode);
                
                String message = newMode ? "Classroom mode enabled" : "Classroom mode disabled";
                teacherManager.broadcastToStudents(server, message);
                
                return ResponseHelper.success("toggleClassroomMode", "Classroom mode: " + newMode);
            }
            return ResponseHelper.error("toggleClassroomMode", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error toggling classroom mode", e);
            return ResponseHelper.error("toggleClassroomMode", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleSetGlobalPermissions(String[] args) {
        if (args.length < 3) {
            return ResponseHelper.error("setGlobalPermissions", ResponseHelper.ERROR_INVALID_PARAMS, "building,chat,visits required (true/false)");
        }
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(player.getUUID())) {
                    return ResponseHelper.error("setGlobalPermissions", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                boolean building = Boolean.parseBoolean(args[0]);
                boolean chat = Boolean.parseBoolean(args[1]);
                boolean visits = Boolean.parseBoolean(args[2]);
                
                teacherManager.setGlobalPermissions(building, chat, visits);
                
                String message = String.format("Global permissions updated: Building=%s, Chat=%s, Visits=%s", 
                    building ? "Enabled" : "Disabled", 
                    chat ? "Enabled" : "Disabled", 
                    visits ? "Enabled" : "Disabled");
                teacherManager.broadcastToStudents(server, message);
                
                return ResponseHelper.success("setGlobalPermissions", "Permissions updated");
            }
            return ResponseHelper.error("setGlobalPermissions", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error setting global permissions", e);
            return ResponseHelper.error("setGlobalPermissions", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    // === Activity Tracking Integration ===
    
    /**
     * Track activity for teacher management system and progress tracking
     */
    private void trackActivityIfClassroomMode(String activity, String details) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            UUID playerUUID = player.getUUID();
            
            // Track in teacher management system if classroom mode is enabled
            if (teacherManager.isClassroomMode() && !teacherManager.isTeacher(playerUUID)) {
                teacherManager.trackActivity(playerUUID, activity, details);
            }
            
            // Always track in progress system (unless player is a teacher)
            if (!teacherManager.isTeacher(playerUUID)) {
                progressTracker.trackActivity(playerUUID, activity, details);
            }
            
            // Track in offline system if offline mode is enabled
            if (offlineModeManager.isOfflineModeEnabled() && !teacherManager.isTeacher(playerUUID)) {
                java.util.Map<String, Object> actionData = new java.util.HashMap<>();
                actionData.put("playerUUID", playerUUID.toString());
                actionData.put("activity", activity);
                actionData.put("details", details);
                actionData.put("timestamp", java.time.LocalDateTime.now().toString());
                
                offlineModeManager.recordOfflineAction(activity, actionData);
            }
        }
    }
}