package edu.minecraft.collaboration.commands;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import edu.minecraft.collaboration.util.ValidationUtils;
import edu.minecraft.collaboration.util.ResponseHelper;
import edu.minecraft.collaboration.teacher.TeacherManager;
import edu.minecraft.collaboration.teacher.StudentActivity;
import edu.minecraft.collaboration.monitoring.MetricsCollector;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Handler for teacher-specific commands (classroom management, permissions, student monitoring)
 */
public class TeacherCommandHandler {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private final TeacherManager teacherManager;
    private final MetricsCollector metricsCollector;
    
    public TeacherCommandHandler(TeacherManager teacherManager, MetricsCollector metricsCollector) {
        this.teacherManager = teacherManager;
        this.metricsCollector = metricsCollector;
    }
    
    /**
     * Handle register teacher command
     */
    public String handleRegisterTeacher(String[] args) {
        if (args.length < 2) {
            return ResponseHelper.error("registerTeacher", ResponseHelper.ERROR_INVALID_ARGS, "Expected: username password");
        }
        
        try {
            String username = args[0];
            String password = args[1];
            
            if (!ValidationUtils.isValidUsername(username)) {
                return ResponseHelper.error("registerTeacher", ResponseHelper.ERROR_INVALID_ARGS, "Invalid username");
            }
            
            // Generate a UUID for the teacher account (in production, use proper user management)
            UUID teacherUUID = UUID.nameUUIDFromBytes(username.getBytes());
            teacherManager.registerTeacher(teacherUUID, password);
            metricsCollector.incrementCounter("commands.registerTeacher");
            return ResponseHelper.teacherRegistered(username);
        } catch (Exception e) {
            LOGGER.error("Error registering teacher", e);
            return ResponseHelper.error("registerTeacher", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle toggle classroom mode command
     */
    public String handleToggleClassroomMode(String[] args) {
        try {
            boolean enabled = args.length == 0 || Boolean.parseBoolean(args[0]);
            
            teacherManager.setClassroomMode(enabled);
            metricsCollector.incrementCounter("commands.toggleClassroomMode");
            
            return ResponseHelper.classroomModeToggled(enabled);
        } catch (Exception e) {
            LOGGER.error("Error toggling classroom mode", e);
            return ResponseHelper.error("toggleClassroomMode", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle set global permissions command
     */
    public String handleSetGlobalPermissions(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("setGlobalPermissions", ResponseHelper.ERROR_INVALID_ARGS, "Expected: permission level");
        }
        
        try {
            String permissionLevel = args[0].toLowerCase();
            
            switch (permissionLevel) {
                case "full":
                    teacherManager.setGlobalPermissions(TeacherManager.PermissionLevel.FULL);
                    break;
                case "limited":
                    teacherManager.setGlobalPermissions(TeacherManager.PermissionLevel.LIMITED);
                    break;
                case "readonly":
                    teacherManager.setGlobalPermissions(TeacherManager.PermissionLevel.READONLY);
                    break;
                case "restricted":
                    teacherManager.setGlobalPermissions(TeacherManager.PermissionLevel.RESTRICTED);
                    break;
                default:
                    return ResponseHelper.error("setGlobalPermissions", ResponseHelper.ERROR_INVALID_ARGS, "Invalid permission level");
            }
            
            metricsCollector.incrementCounter("commands.setGlobalPermissions");
            return ResponseHelper.permissionsSet(permissionLevel);
        } catch (Exception e) {
            LOGGER.error("Error setting global permissions", e);
            return ResponseHelper.error("setGlobalPermissions", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle set student time limit command
     */
    public String handleSetStudentTimeLimit(String[] args) {
        if (args.length < 2) {
            return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_INVALID_ARGS, "Expected: student minutes");
        }
        
        try {
            String studentName = args[0];
            int minutes = Integer.parseInt(args[1]);
            
            if (!ValidationUtils.isValidUsername(studentName)) {
                return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_INVALID_ARGS, "Invalid student name");
            }
            
            if (minutes < 1 || minutes > 1440) { // Max 24 hours
                return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_INVALID_ARGS, "Time limit must be between 1 and 1440 minutes");
            }
            
            boolean success = teacherManager.setStudentTimeLimit(studentName, minutes);
            if (success) {
                metricsCollector.incrementCounter("commands.setStudentTimeLimit");
                return ResponseHelper.timeLimitSet(studentName, minutes);
            } else {
                return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_STUDENT_NOT_FOUND, "Student not found");
            }
        } catch (NumberFormatException e) {
            return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_INVALID_ARGS, "Invalid time format");
        } catch (Exception e) {
            LOGGER.error("Error setting student time limit", e);
            return ResponseHelper.error("setStudentTimeLimit", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle add student restriction command
     */
    public String handleAddStudentRestriction(String[] args) {
        if (args.length < 2) {
            return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_INVALID_ARGS, "Expected: student restriction");
        }
        
        try {
            String studentName = args[0];
            String restriction = args[1].toLowerCase();
            
            if (!ValidationUtils.isValidUsername(studentName)) {
                return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_INVALID_ARGS, "Invalid student name");
            }
            
            boolean success = false;
            switch (restriction) {
                case "no_tnt":
                    success = teacherManager.addStudentRestriction(studentName, TeacherManager.RestrictionType.NO_TNT);
                    break;
                case "no_fire":
                    success = teacherManager.addStudentRestriction(studentName, TeacherManager.RestrictionType.NO_FIRE);
                    break;
                case "no_pvp":
                    success = teacherManager.addStudentRestriction(studentName, TeacherManager.RestrictionType.NO_PVP);
                    break;
                case "area_limit":
                    success = teacherManager.addStudentRestriction(studentName, TeacherManager.RestrictionType.AREA_LIMIT);
                    break;
                case "time_limit":
                    success = teacherManager.addStudentRestriction(studentName, TeacherManager.RestrictionType.TIME_LIMIT);
                    break;
                default:
                    return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_INVALID_ARGS, "Unknown restriction type");
            }
            
            if (success) {
                metricsCollector.incrementCounter("commands.addStudentRestriction");
                return ResponseHelper.restrictionAdded(studentName, restriction);
            } else {
                return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_STUDENT_NOT_FOUND, "Student not found");
            }
        } catch (Exception e) {
            LOGGER.error("Error adding student restriction", e);
            return ResponseHelper.error("addStudentRestriction", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle freeze all students command
     */
    public String handleFreezeAllStudents(String[] args) {
        try {
            boolean freeze = args.length == 0 || Boolean.parseBoolean(args[0]);
            
            teacherManager.freezeAllStudents(freeze);
            int frozenCount = 0; // In a real implementation, this would return the actual count
            metricsCollector.incrementCounter("commands.freezeAllStudents");
            
            return ResponseHelper.studentsChanged(freeze ? "frozen" : "unfrozen", frozenCount);
        } catch (Exception e) {
            LOGGER.error("Error freezing students", e);
            return ResponseHelper.error("freezeAllStudents", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle summon all students command
     */
    public String handleSummonAllStudents(String[] args) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null || server.getPlayerList().getPlayers().isEmpty()) {
                return ResponseHelper.error("summonAllStudents", ResponseHelper.ERROR_NO_PLAYER, "No teacher online");
            }
            
            ServerPlayer teacher = server.getPlayerList().getPlayers().get(0);
            BlockPos teacherPos = teacher.blockPosition();
            
            teacherManager.summonAllStudents(teacher);
            // Count summoned students (simplified)
            int summonedCount = server.getPlayerList().getPlayerCount() - 1; // Exclude teacher
            metricsCollector.incrementCounter("commands.summonAllStudents");
            
            return ResponseHelper.studentsSummoned(summonedCount, teacherPos.getX(), teacherPos.getY(), teacherPos.getZ());
        } catch (Exception e) {
            LOGGER.error("Error summoning students", e);
            return ResponseHelper.error("summonAllStudents", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle get student activities command
     */
    public String handleGetStudentActivities(String[] args) {
        try {
            List<StudentActivity> activities;
            
            if (args.length > 0) {
                String studentName = args[0];
                if (!ValidationUtils.isValidUsername(studentName)) {
                    return ResponseHelper.error("getStudentActivities", ResponseHelper.ERROR_INVALID_ARGS, "Invalid student name");
                }
                activities = teacherManager.getStudentActivities(studentName);
            } else {
                // Convert Map to List for consistent return type
                activities = new ArrayList<>(teacherManager.getAllStudentActivities().values());
            }
            
            metricsCollector.incrementCounter("commands.getStudentActivities");
            return ResponseHelper.studentActivities(activities);
        } catch (Exception e) {
            LOGGER.error("Error getting student activities", e);
            return ResponseHelper.error("getStudentActivities", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle broadcast to students command
     */
    public String handleBroadcastToStudents(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("broadcastToStudents", ResponseHelper.ERROR_INVALID_ARGS, "Expected: message");
        }
        
        try {
            String message = String.join(" ", args);
            
            if (!ValidationUtils.isValidChatMessage(message)) {
                return ResponseHelper.error("broadcastToStudents", ResponseHelper.ERROR_INVALID_MESSAGE, "Invalid message");
            }
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                Component broadcastComponent = Component.literal("[Teacher] " + message);
                
                int sentCount = 0;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    if (teacherManager.isStudent(player.getName().getString())) {
                        player.sendSystemMessage(broadcastComponent);
                        sentCount++;
                    }
                }
                
                metricsCollector.incrementCounter("commands.broadcastToStudents");
                return ResponseHelper.broadcastSent(message, sentCount);
            }
            return ResponseHelper.error("broadcastToStudents", ResponseHelper.ERROR_SERVER_UNAVAILABLE, "Server not available");
        } catch (Exception e) {
            LOGGER.error("Error broadcasting to students", e);
            return ResponseHelper.error("broadcastToStudents", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
}