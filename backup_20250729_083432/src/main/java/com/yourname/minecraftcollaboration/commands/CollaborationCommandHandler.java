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
import com.yourname.minecraftcollaboration.util.ResponseHelper;
import com.yourname.minecraftcollaboration.collaboration.CollaborationManager;
import com.yourname.minecraftcollaboration.models.Invitation;
import com.yourname.minecraftcollaboration.models.VisitRequest;
import com.yourname.minecraftcollaboration.entities.AgentManager;
import com.yourname.minecraftcollaboration.entities.CollaborationAgent;
import com.yourname.minecraftcollaboration.teacher.TeacherManager;
import com.yourname.minecraftcollaboration.teacher.StudentActivity;
import com.yourname.minecraftcollaboration.progress.ProgressTracker;
import com.yourname.minecraftcollaboration.localization.LanguageManager;
import com.yourname.minecraftcollaboration.blockpacks.BlockPackManager;
import com.yourname.minecraftcollaboration.blockpacks.BlockPack;
import com.yourname.minecraftcollaboration.offline.OfflineModeManager;
import com.yourname.minecraftcollaboration.monitoring.MetricsCollector;
import com.yourname.minecraftcollaboration.monitoring.MetricsReporter;

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
                player.sendSystemMessage(Component.literal("§a教師アカウントに登録されました！"));
                
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
                
                String message = newMode ? "授業モードが有効になりました" : "授業モードが無効になりました";
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
                
                String message = String.format("権限更新: 建築=%s, チャット=%s, 訪問=%s", 
                    building ? "許可" : "禁止", 
                    chat ? "許可" : "禁止", 
                    visits ? "許可" : "禁止");
                teacherManager.broadcastToStudents(server, message);
                
                return ResponseHelper.success("setGlobalPermissions", "Permissions updated");
            }
            return ResponseHelper.error("setGlobalPermissions", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error setting global permissions", e);
            return ResponseHelper.error("setGlobalPermissions", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleSetStudentTimeLimit(String[] args) {
        if (args.length < 2) {
            return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_INVALID_PARAMS, "studentName,minutes required");
        }
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer teacher = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(teacher.getUUID())) {
                    return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                String studentName = args[0];
                int minutes = Integer.parseInt(args[1]);
                
                ServerPlayer student = server.getPlayerList().getPlayerByName(studentName);
                if (student != null) {
                    teacherManager.setTimeLimit(student.getUUID(), minutes);
                    
                    if (minutes > 0) {
                        student.sendSystemMessage(Component.literal("§e制限時間が設定されました: " + minutes + "分"));
                    } else {
                        student.sendSystemMessage(Component.literal("§a制限時間が解除されました"));
                    }
                    
                    return ResponseHelper.success("setStudentTimeLimit", "Time limit set for " + studentName);
                } else {
                    return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_NOT_FOUND, "Student not found: " + studentName);
                }
            }
            return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (NumberFormatException e) {
            return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_INVALID_PARAMS, "Invalid minutes value");
        } catch (Exception e) {
            LOGGER.error("Error setting student time limit", e);
            return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleAddStudentRestriction(String[] args) {
        if (args.length < 2) {
            return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_INVALID_PARAMS, "studentName,action required");
        }
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer teacher = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(teacher.getUUID())) {
                    return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                String studentName = args[0];
                String action = args[1];
                
                ServerPlayer student = server.getPlayerList().getPlayerByName(studentName);
                if (student != null) {
                    teacherManager.addStudentRestriction(student.getUUID(), action);
                    student.sendSystemMessage(Component.literal("§c制限が追加されました: " + action));
                    
                    return ResponseHelper.success("addStudentRestriction", "Restriction added for " + studentName);
                } else {
                    return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_NOT_FOUND, "Student not found: " + studentName);
                }
            }
            return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error adding student restriction", e);
            return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleFreezeAllStudents(String[] args) {
        boolean freeze = args.length > 0 && Boolean.parseBoolean(args[0]);
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer teacher = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(teacher.getUUID())) {
                    return ResponseHelper.error("freezeAllStudents", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                teacherManager.freezeAllStudents(freeze);
                
                String action = freeze ? "frozen" : "unfrozen";
                return ResponseHelper.success("freezeAllStudents", "All students " + action);
            }
            return ResponseHelper.error("freezeAllStudents", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error freezing students", e);
            return ResponseHelper.error("freezeAllStudents", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleSummonAllStudents(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer teacher = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(teacher.getUUID())) {
                    return ResponseHelper.error("summonAllStudents", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                teacherManager.summonAllStudents(teacher);
                
                return ResponseHelper.success("summonAllStudents", "All students summoned");
            }
            return ResponseHelper.error("summonAllStudents", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error summoning students", e);
            return ResponseHelper.error("summonAllStudents", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleGetStudentActivities(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer teacher = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(teacher.getUUID())) {
                    return ResponseHelper.error("getStudentActivities", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                if (args.length > 0) {
                    // Get specific student activity
                    String studentName = args[0];
                    ServerPlayer student = server.getPlayerList().getPlayerByName(studentName);
                    if (student != null) {
                        StudentActivity activity = teacherManager.getStudentActivity(student.getUUID());
                        if (activity != null) {
                            StudentActivity.ActivitySummary summary = activity.getSummary();
                            return ResponseHelper.success("getStudentActivities", summary.toString());
                        } else {
                            return ResponseHelper.error("getStudentActivities", ResponseHelper.ERROR_NOT_FOUND, "No activity data for " + studentName);
                        }
                    } else {
                        return ResponseHelper.error("getStudentActivities", ResponseHelper.ERROR_NOT_FOUND, "Student not found: " + studentName);
                    }
                } else {
                    // Get classroom report
                    String report = teacherManager.generateClassroomReport();
                    return ResponseHelper.success("getStudentActivities", "Report generated");
                }
            }
            return ResponseHelper.error("getStudentActivities", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error getting student activities", e);
            return ResponseHelper.error("getStudentActivities", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleBroadcastToStudents(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("broadcastToStudents", ResponseHelper.ERROR_INVALID_PARAMS, "Message required");
        }
        
        String message = String.join(" ", args);
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer teacher = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(teacher.getUUID())) {
                    return ResponseHelper.error("broadcastToStudents", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                teacherManager.broadcastToStudents(server, message);
                
                return ResponseHelper.success("broadcastToStudents", "Message broadcast to all students");
            }
            return ResponseHelper.error("broadcastToStudents", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error broadcasting to students", e);
            return ResponseHelper.error("broadcastToStudents", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    // === Progress Tracking Commands ===
    
    public String handleGetStudentProgress(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                if (args.length > 0) {
                    // Get specific student progress (teacher only)
                    if (!teacherManager.isTeacher(player.getUUID())) {
                        return ResponseHelper.error("getStudentProgress", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                    }
                    
                    String studentName = args[0];
                    ServerPlayer student = server.getPlayerList().getPlayerByName(studentName);
                    if (student != null) {
                        String report = progressTracker.generateProgressReport(student.getUUID());
                        return ResponseHelper.success("getStudentProgress", "Progress report generated");
                    } else {
                        return ResponseHelper.error("getStudentProgress", ResponseHelper.ERROR_NOT_FOUND, "Student not found: " + studentName);
                    }
                } else {
                    // Get own progress
                    String report = progressTracker.generateProgressReport(player.getUUID());
                    return ResponseHelper.success("getStudentProgress", "Your progress report generated");
                }
            }
            return ResponseHelper.error("getStudentProgress", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error getting student progress", e);
            return ResponseHelper.error("getStudentProgress", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleConfigureProgressTracking(String[] args) {
        if (args.length < 4) {
            return ResponseHelper.error("configureProgressTracking", ResponseHelper.ERROR_INVALID_PARAMS, "enabled,blockPoints,commandPoints,collabPoints required");
        }
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(player.getUUID())) {
                    return ResponseHelper.error("configureProgressTracking", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                boolean enabled = Boolean.parseBoolean(args[0]);
                int blockPoints = Integer.parseInt(args[1]);
                int commandPoints = Integer.parseInt(args[2]);
                int collabPoints = Integer.parseInt(args[3]);
                
                progressTracker.configureTracking(enabled, blockPoints, commandPoints, collabPoints);
                
                return ResponseHelper.success("configureProgressTracking", "Progress tracking configured");
            }
            return ResponseHelper.error("configureProgressTracking", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (NumberFormatException e) {
            return ResponseHelper.error("configureProgressTracking", ResponseHelper.ERROR_INVALID_PARAMS, "Invalid number format");
        } catch (Exception e) {
            LOGGER.error("Error configuring progress tracking", e);
            return ResponseHelper.error("configureProgressTracking", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleResetStudentProgress(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("resetStudentProgress", ResponseHelper.ERROR_INVALID_PARAMS, "studentName required");
        }
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer teacher = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(teacher.getUUID())) {
                    return ResponseHelper.error("resetStudentProgress", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                String studentName = args[0];
                ServerPlayer student = server.getPlayerList().getPlayerByName(studentName);
                if (student != null) {
                    progressTracker.resetStudentProgress(student.getUUID());
                    student.sendSystemMessage(Component.literal("§e学習進捗がリセットされました"));
                    
                    return ResponseHelper.success("resetStudentProgress", "Progress reset for " + studentName);
                } else {
                    return ResponseHelper.error("resetStudentProgress", ResponseHelper.ERROR_NOT_FOUND, "Student not found: " + studentName);
                }
            }
            return ResponseHelper.error("resetStudentProgress", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error resetting student progress", e);
            return ResponseHelper.error("resetStudentProgress", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleGetMyProgress(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                String report = progressTracker.generateProgressReport(player.getUUID());
                player.sendSystemMessage(Component.literal("§a学習進捗レポートが生成されました"));
                
                return ResponseHelper.success("getMyProgress", "Your progress report generated");
            }
            return ResponseHelper.error("getMyProgress", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error getting player progress", e);
            return ResponseHelper.error("getMyProgress", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    // === Language Management Commands ===
    
    public String handleSetLanguage(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("setLanguage", ResponseHelper.ERROR_INVALID_PARAMS, "Language code required (ja_JP, en_US, zh_CN, zh_TW, ko_KR, es_ES, fr_FR, de_DE)");
        }
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                String languageCode = args[0];
                
                if (languageManager.getSupportedLanguages().contains(languageCode)) {
                    languageManager.setPlayerLanguage(player.getUUID(), languageCode);
                    
                    // 設定完了メッセージを新しい言語で送信
                    String message = languageManager.getMessage(player.getUUID(), "language.changed", 
                        languageManager.getLanguageDisplayName(languageCode));
                    player.sendSystemMessage(Component.literal("§a" + message));
                    
                    return ResponseHelper.success("setLanguage", "Language set to " + languageCode);
                } else {
                    String supportedLangs = String.join(", ", languageManager.getSupportedLanguages());
                    return ResponseHelper.error("setLanguage", ResponseHelper.ERROR_INVALID_PARAMS, 
                        "Unsupported language. Supported: " + supportedLangs);
                }
            }
            return ResponseHelper.error("setLanguage", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error setting language", e);
            return ResponseHelper.error("setLanguage", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleGetLanguage(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                String currentLang = languageManager.getPlayerLanguage(player.getUUID());
                String displayName = languageManager.getLanguageDisplayName(currentLang);
                
                return ResponseHelper.success("getLanguage", "Current language: " + displayName + " (" + currentLang + ")");
            }
            return ResponseHelper.error("getLanguage", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error getting language", e);
            return ResponseHelper.error("getLanguage", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleListLanguages(String[] args) {
        try {
            StringBuilder response = new StringBuilder("Supported languages:\\n");
            
            for (String lang : languageManager.getSupportedLanguages()) {
                String displayName = languageManager.getLanguageDisplayName(lang);
                int messageCount = languageManager.getMessageCount(lang);
                response.append("- ").append(displayName).append(" (").append(lang).append(") - ")
                       .append(messageCount).append(" messages\\n");
            }
            
            return ResponseHelper.success("listLanguages", response.toString());
            
        } catch (Exception e) {
            LOGGER.error("Error listing languages", e);
            return ResponseHelper.error("listLanguages", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleSetDefaultLanguage(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("setDefaultLanguage", ResponseHelper.ERROR_INVALID_PARAMS, "Language code required");
        }
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                if (!teacherManager.isTeacher(player.getUUID())) {
                    return ResponseHelper.error("setDefaultLanguage", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                String languageCode = args[0];
                if (languageManager.getSupportedLanguages().contains(languageCode)) {
                    languageManager.setDefaultLanguage(languageCode);
                    
                    String message = "Default language set to " + languageManager.getLanguageDisplayName(languageCode);
                    return ResponseHelper.success("setDefaultLanguage", message);
                } else {
                    String supportedLangs = String.join(", ", languageManager.getSupportedLanguages());
                    return ResponseHelper.error("setDefaultLanguage", ResponseHelper.ERROR_INVALID_PARAMS, 
                        "Unsupported language. Supported: " + supportedLangs);
                }
            }
            return ResponseHelper.error("setDefaultLanguage", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error setting default language", e);
            return ResponseHelper.error("setDefaultLanguage", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    // === Block Pack Management Commands ===
    
    public String handleApplyBlockPack(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("applyBlockPack", ResponseHelper.ERROR_INVALID_ARGUMENT, "Block pack ID required");
        }
        
        try {
            String packId = args[0];
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                UUID playerUUID = player.getUUID();
                
                // Check permissions for restricted packs
                BlockPack pack = blockPackManager.getBlockPack(packId);
                if (pack != null && !pack.isStudentAccessible() && !teacherManager.isTeacher(playerUUID)) {
                    return ResponseHelper.error("applyBlockPack", ResponseHelper.ERROR_FORBIDDEN, "This block pack requires teacher access");
                }
                
                boolean success = blockPackManager.applyBlockPack(playerUUID, packId);
                if (success) {
                    String language = languageManager.getPlayerLanguage(playerUUID);
                    String packName = pack != null ? pack.getName(language) : packId;
                    
                    // Notify player
                    String message = languageManager.getMessage(playerUUID, "blockpack.applied", packName);
                    player.sendSystemMessage(Component.literal("§a" + message));
                    
                    return ResponseHelper.success("applyBlockPack", "Block pack applied: " + packName);
                } else {
                    return ResponseHelper.error("applyBlockPack", ResponseHelper.ERROR_NOT_FOUND, "Block pack not found: " + packId);
                }
            }
            return ResponseHelper.error("applyBlockPack", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error applying block pack", e);
            return ResponseHelper.error("applyBlockPack", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleGetAvailableBlockPacks(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                UUID playerUUID = player.getUUID();
                String language = languageManager.getPlayerLanguage(playerUUID);
                boolean isTeacher = teacherManager.isTeacher(playerUUID);
                
                List<BlockPack> availablePacks = blockPackManager.getAvailableBlockPacks(isTeacher);
                
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < availablePacks.size(); i++) {
                    BlockPack pack = availablePacks.get(i);
                    if (i > 0) result.append("|");
                    result.append(pack.getId()).append(",")
                          .append(pack.getName(language)).append(",")
                          .append(pack.getCategory().getDisplayName(language)).append(",")
                          .append(pack.getDifficulty().getDisplayName(language)).append(",")
                          .append(pack.getBlockCount());
                }
                
                return ResponseHelper.success("getAvailableBlockPacks", result.toString());
            }
            return ResponseHelper.error("getAvailableBlockPacks", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error getting available block packs", e);
            return ResponseHelper.error("getAvailableBlockPacks", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleGetCurrentBlockPack(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                UUID playerUUID = player.getUUID();
                String language = languageManager.getPlayerLanguage(playerUUID);
                
                BlockPack currentPack = blockPackManager.getPlayerBlockPack(playerUUID);
                if (currentPack != null) {
                    String result = currentPack.getId() + "," + 
                                  currentPack.getName(language) + "," +
                                  currentPack.getCategory().getDisplayName(language) + "," +
                                  currentPack.getDifficulty().getDisplayName(language) + "," +
                                  currentPack.getBlockCount();
                    
                    return ResponseHelper.success("getCurrentBlockPack", result);
                } else {
                    return ResponseHelper.error("getCurrentBlockPack", ResponseHelper.ERROR_NOT_FOUND, "No active block pack");
                }
            }
            return ResponseHelper.error("getCurrentBlockPack", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error getting current block pack", e);
            return ResponseHelper.error("getCurrentBlockPack", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleGetBlockPackInfo(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("getBlockPackInfo", ResponseHelper.ERROR_INVALID_ARGUMENT, "Block pack ID required");
        }
        
        try {
            String packId = args[0];
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                String language = languageManager.getPlayerLanguage(player.getUUID());
                
                BlockPack pack = blockPackManager.getBlockPack(packId);
                if (pack != null) {
                    String detailedInfo = pack.getDetailedInfo(language);
                    return ResponseHelper.success("getBlockPackInfo", detailedInfo);
                } else {
                    return ResponseHelper.error("getBlockPackInfo", ResponseHelper.ERROR_NOT_FOUND, "Block pack not found: " + packId);
                }
            }
            return ResponseHelper.error("getBlockPackInfo", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error getting block pack info", e);
            return ResponseHelper.error("getBlockPackInfo", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleCreateCustomBlockPack(String[] args) {
        if (args.length < 4) {
            return ResponseHelper.error("createCustomBlockPack", ResponseHelper.ERROR_INVALID_ARGUMENT, 
                "Usage: createCustomBlockPack <id> <name> <description> <blocks...>");
        }
        
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                UUID playerUUID = player.getUUID();
                
                // Only teachers can create custom packs
                if (!teacherManager.isTeacher(playerUUID)) {
                    return ResponseHelper.error("createCustomBlockPack", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                String packId = args[0];
                String packName = args[1];
                String packDescription = args[2];
                
                // Parse block list (comma-separated)
                String[] blockNames = args[3].split(",");
                List<Block> blocks = new java.util.ArrayList<>();
                for (String blockName : blockNames) {
                    Block block = BlockUtils.getBlockFromString(blockName.trim());
                    if (block != null && block != Blocks.AIR) {
                        blocks.add(block);
                    }
                }
                
                if (blocks.isEmpty()) {
                    return ResponseHelper.error("createCustomBlockPack", ResponseHelper.ERROR_INVALID_ARGUMENT, "No valid blocks specified");
                }
                
                // Create multilingual names and descriptions
                java.util.Map<String, String> names = new java.util.HashMap<>();
                java.util.Map<String, String> descriptions = new java.util.HashMap<>();
                
                names.put("ja_JP", packName);
                names.put("en_US", packName);
                descriptions.put("ja_JP", packDescription);
                descriptions.put("en_US", packDescription);
                
                BlockPack customPack = blockPackManager.createCustomBlockPack(
                    packId, names, descriptions, blocks,
                    com.yourname.minecraftcollaboration.blockpacks.BlockPackCategory.CUSTOM,
                    com.yourname.minecraftcollaboration.blockpacks.DifficultyLevel.INTERMEDIATE,
                    true
                );
                
                if (customPack != null) {
                    String message = languageManager.getMessage(playerUUID, "blockpack.created", packName);
                    player.sendSystemMessage(Component.literal("§a" + message));
                    
                    return ResponseHelper.success("createCustomBlockPack", "Custom block pack created: " + packName);
                } else {
                    return ResponseHelper.error("createCustomBlockPack", ResponseHelper.ERROR_INTERNAL, "Failed to create custom pack");
                }
            }
            return ResponseHelper.error("createCustomBlockPack", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error creating custom block pack", e);
            return ResponseHelper.error("createCustomBlockPack", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    // === Offline Mode Commands ===
    
    public String handleSetOfflineMode(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("setOfflineMode", ResponseHelper.ERROR_INVALID_ARGUMENT, "Enabled/disabled required");
        }
        
        try {
            boolean enabled = Boolean.parseBoolean(args[0]);
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                UUID playerUUID = player.getUUID();
                
                // 教師のみがオフラインモードを制御可能
                if (!teacherManager.isTeacher(playerUUID)) {
                    return ResponseHelper.error("setOfflineMode", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                offlineModeManager.setOfflineModeEnabled(enabled);
                
                String message = languageManager.getMessage(playerUUID, 
                    enabled ? "offline.mode_enabled" : "offline.mode_disabled");
                player.sendSystemMessage(Component.literal("§a" + message));
                
                return ResponseHelper.success("setOfflineMode", "Offline mode " + (enabled ? "enabled" : "disabled"));
            }
            return ResponseHelper.error("setOfflineMode", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error setting offline mode", e);
            return ResponseHelper.error("setOfflineMode", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleGetOfflineStatus(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                UUID playerUUID = player.getUUID();
                String language = languageManager.getPlayerLanguage(playerUUID);
                
                java.util.Map<String, Object> stats = offlineModeManager.getOfflineStatistics();
                
                StringBuilder result = new StringBuilder();
                result.append("Offline Mode: ").append(stats.get("offlineModeEnabled")).append("|");
                result.append("Pending Actions: ").append(stats.get("pendingActionsCount")).append("|");
                result.append("Cached Students: ").append(stats.get("cachedStudentsCount")).append("|");
                result.append("Last Sync: ").append(stats.get("lastSyncTime")).append("|");
                result.append("Auto Sync: ").append(stats.get("autoSyncEnabled"));
                
                return ResponseHelper.success("getOfflineStatus", result.toString());
            }
            return ResponseHelper.error("getOfflineStatus", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error getting offline status", e);
            return ResponseHelper.error("getOfflineStatus", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleSyncOfflineData(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                UUID playerUUID = player.getUUID();
                
                // 教師のみが同期を実行可能
                if (!teacherManager.isTeacher(playerUUID)) {
                    return ResponseHelper.error("syncOfflineData", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                boolean success = offlineModeManager.syncPendingActions();
                
                String message = languageManager.getMessage(playerUUID, 
                    success ? "offline.sync_success" : "offline.sync_partial");
                player.sendSystemMessage(Component.literal(success ? "§a" : "§e" + message));
                
                int remainingActions = offlineModeManager.getPendingActionsCount();
                
                return ResponseHelper.success("syncOfflineData", 
                    "Sync completed. Remaining actions: " + remainingActions);
            }
            return ResponseHelper.error("syncOfflineData", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error syncing offline data", e);
            return ResponseHelper.error("syncOfflineData", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleExportOfflineData(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                UUID playerUUID = player.getUUID();
                
                // 教師のみがエクスポート可能
                if (!teacherManager.isTeacher(playerUUID)) {
                    return ResponseHelper.error("exportOfflineData", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                String exportData = offlineModeManager.exportOfflineData();
                
                String message = languageManager.getMessage(playerUUID, "offline.data_exported");
                player.sendSystemMessage(Component.literal("§a" + message));
                
                return ResponseHelper.success("exportOfflineData", "Data exported successfully");
            }
            return ResponseHelper.error("exportOfflineData", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error exporting offline data", e);
            return ResponseHelper.error("exportOfflineData", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    public String handleSetAutoSync(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("setAutoSync", ResponseHelper.ERROR_INVALID_ARGUMENT, "Enabled/disabled required");
        }
        
        try {
            boolean enabled = Boolean.parseBoolean(args[0]);
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                UUID playerUUID = player.getUUID();
                
                // 教師のみが自動同期を制御可能
                if (!teacherManager.isTeacher(playerUUID)) {
                    return ResponseHelper.error("setAutoSync", ResponseHelper.ERROR_FORBIDDEN, "Teacher access required");
                }
                
                offlineModeManager.setAutoSyncEnabled(enabled);
                
                String message = languageManager.getMessage(playerUUID, 
                    enabled ? "offline.autosync_enabled" : "offline.autosync_disabled");
                player.sendSystemMessage(Component.literal("§a" + message));
                
                return ResponseHelper.success("setAutoSync", "Auto sync " + (enabled ? "enabled" : "disabled"));
            }
            return ResponseHelper.error("setAutoSync", ResponseHelper.ERROR_NOT_FOUND, "No players online");
            
        } catch (Exception e) {
            LOGGER.error("Error setting auto sync", e);
            return ResponseHelper.error("setAutoSync", ResponseHelper.ERROR_INTERNAL, e.getMessage());
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
    
    // === Utility Methods ===
    
    // Method removed - now using BlockUtils.getBlockFromString()
}