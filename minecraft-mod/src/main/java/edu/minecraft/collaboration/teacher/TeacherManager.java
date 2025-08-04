package edu.minecraft.collaboration.teacher;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.localization.LanguageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

/**
 * Teacher management system for monitoring and controlling student activities
 */
public final class TeacherManager {
    
    /**
     * Permission levels for global settings
     */
    public enum PermissionLevel {
        FULL,
        LIMITED,
        READONLY,
        RESTRICTED
    }
    
    /**
     * Restriction types for students
     */
    public enum RestrictionType {
        NO_TNT,
        NO_FIRE,
        NO_PVP,
        AREA_LIMIT,
        TIME_LIMIT
    }
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static volatile TeacherManager instance;
    private static final Object LOCK = new Object();
    private final LanguageManager languageManager;
    
    // Teacher accounts
    private final Set<UUID> teacherAccounts = new HashSet<>();
    
    // Student activity tracking
    private final Map<UUID, StudentActivity> studentActivities = new ConcurrentHashMap<>();
    
    // Time limits (in minutes)
    private final Map<UUID, Integer> studentTimeLimits = new ConcurrentHashMap<>();
    
    // Blocked actions per student
    private final Map<UUID, Set<String>> studentRestrictions = new ConcurrentHashMap<>();
    
    // Global classroom settings
    private boolean classroomMode = false;
    private boolean allowBuilding = true;
    private boolean allowChat = true;
    private boolean allowVisits = true;
    private int maxSessionMinutes = 60;
    
    private TeacherManager() {
        this.languageManager = DependencyInjector.getInstance().getService(LanguageManager.class);
    }
    
    public static TeacherManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new TeacherManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a teacher account
     */
    public void registerTeacher(UUID playerUUID, String password) {
        // In production, use proper authentication
        teacherAccounts.add(playerUUID);
        LOGGER.info("Registered teacher account: {}", playerUUID);
    }
    
    /**
     * Check if player is a teacher
     */
    public boolean isTeacher(UUID playerUUID) {
        return teacherAccounts.contains(playerUUID);
    }
    
    /**
     * Enable/disable classroom mode
     */
    public void setClassroomMode(boolean enabled) {
        this.classroomMode = enabled;
        LOGGER.info("Classroom mode: {}", enabled ? "ENABLED" : "DISABLED");
    }
    
    /**
     * Get classroom mode status
     */
    public boolean isClassroomMode() {
        return classroomMode;
    }
    
    /**
     * Set global permissions
     */
    public void setGlobalPermissions(boolean building, boolean chat, boolean visits) {
        this.allowBuilding = building;
        this.allowChat = chat;
        this.allowVisits = visits;
        
        LOGGER.info("Global permissions updated - Building: {}, Chat: {}, Visits: {}", 
            building, chat, visits);
    }
    
    /**
     * Check if action is allowed
     */
    public boolean isActionAllowed(UUID playerUUID, String action) {
        if (!classroomMode) {
            return true; // All actions allowed outside classroom mode
        }
        
        // Check global permissions
        switch (action.toLowerCase()) {
            case "build":
            case "place_block":
            case "break_block":
                if (!allowBuilding) {
                    return false;
                }
                break;
            case "chat":
                if (!allowChat) {
                    return false;
                }
                break;
            case "visit":
            case "invite":
                if (!allowVisits) {
                    return false;
                }
                break;
            default:
                // Unknown action - allow by default
                break;
        }
        
        // Check student-specific restrictions
        Set<String> restrictions = studentRestrictions.get(playerUUID);
        if (restrictions != null && restrictions.contains(action.toLowerCase())) {
            return false;
        }
        
        // Check time limits
        Integer timeLimit = studentTimeLimits.get(playerUUID);
        if (timeLimit != null && timeLimit <= 0) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Track student activity
     */
    public void trackActivity(UUID playerUUID, String activity, String details) {
        StudentActivity activityLog = studentActivities.computeIfAbsent(
            playerUUID, k -> new StudentActivity(playerUUID)
        );
        
        activityLog.addActivity(activity, details);
        
        // Check for concerning patterns
        if (activity.equals("emergency_return")) {
            notifyTeachers(playerUUID + " used emergency return");
        }
    }
    
    /**
     * Get student activity report
     */
    public StudentActivity getStudentActivity(UUID playerUUID) {
        return studentActivities.get(playerUUID);
    }
    
    /**
     * Get all student activities
     */
    public Map<UUID, StudentActivity> getAllStudentActivities() {
        return new HashMap<>(studentActivities);
    }
    
    /**
     * Set time limit for student
     */
    public void setTimeLimit(UUID playerUUID, int minutes) {
        if (minutes <= 0) {
            studentTimeLimits.remove(playerUUID);
        } else {
            studentTimeLimits.put(playerUUID, minutes);
        }
        
        LOGGER.info("Set time limit for {}: {} minutes", playerUUID, minutes);
    }
    
    /**
     * Add restriction for student
     */
    public void addStudentRestriction(UUID playerUUID, String action) {
        studentRestrictions.computeIfAbsent(playerUUID, k -> new HashSet<>())
            .add(action.toLowerCase());
        
        LOGGER.info("Added restriction for {}: {}", playerUUID, action);
    }
    
    /**
     * Remove restriction for student
     */
    public void removeStudentRestriction(UUID playerUUID, String action) {
        Set<String> restrictions = studentRestrictions.get(playerUUID);
        if (restrictions != null) {
            restrictions.remove(action.toLowerCase());
            if (restrictions.isEmpty()) {
                studentRestrictions.remove(playerUUID);
            }
        }
    }
    
    /**
     * Broadcast message to all students
     */
    public void broadcastToStudents(net.minecraft.server.MinecraftServer server, String message) {
        if (server != null) {
            Component msg = Component.literal("§e[蜈育函繧医ｊ] ﾂｧf" + message);
            server.getPlayerList().getPlayers().stream()
                .filter(player -> !isTeacher(player.getUUID()))
                .forEach(player -> player.sendSystemMessage(msg));
        }
    }
    
    /**
     * Notify all teachers
     */
    private void notifyTeachers(String message) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            Component msg = Component.literal("§c[Teacher Alert] ﾂｧf" + message);
            server.getPlayerList().getPlayers().stream()
                .filter(player -> isTeacher(player.getUUID()))
                .forEach(player -> player.sendSystemMessage(msg));
        }
    }
    
    /**
     * Freeze/unfreeze all students
     */
    public void freezeAllStudents(boolean freeze) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getPlayerList().getPlayers().stream()
                .filter(player -> !isTeacher(player.getUUID()))
                .forEach(player -> {
                    if (freeze) {
                        player.setGameMode(GameType.SPECTATOR);
                        String message = languageManager.getMessage(player.getUUID(), "student.activity_paused");
                        player.sendSystemMessage(Component.literal("§e" + message));
                    } else {
                        player.setGameMode(GameType.CREATIVE);
                        String message = languageManager.getMessage(player.getUUID(), "student.activity_resumed");
                        player.sendSystemMessage(Component.literal("§a" + message));
                    }
                });
        }
    }
    
    /**
     * Teleport all students to teacher
     */
    public void summonAllStudents(ServerPlayer teacher) {
        if (teacher != null && isTeacher(teacher.getUUID())) {
            net.minecraft.server.MinecraftServer server = teacher.getServer();
            if (server != null) {
                server.getPlayerList().getPlayers().stream()
                    .filter(player -> !isTeacher(player.getUUID()))
                    .forEach(player -> {
                        player.teleportTo(teacher.getX(), teacher.getY(), teacher.getZ());
                        String message = languageManager.getMessage(player.getUUID(), "student.summoned");
                        player.sendSystemMessage(Component.literal("§e" + message));
                    });
                
                String teacherMessage = languageManager.getMessage(teacher.getUUID(), "classroom.students_summoned");
                teacher.sendSystemMessage(Component.literal("§a" + teacherMessage));
            }
        }
    }
    
    /**
     * Clear all student activities
     */
    public void clearAllActivities() {
        studentActivities.clear();
        LOGGER.info("Cleared all student activities");
    }
    
    /**
     * Generate classroom report
     */
    public String generateClassroomReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Classroom Report ===\n");
        report.append("Time: ").append(LocalDateTime.now()).append("\n");
        report.append("Classroom Mode: ").append(classroomMode).append("\n");
        report.append("Active Students: ").append(studentActivities.size()).append("\n\n");
        
        for (Map.Entry<UUID, StudentActivity> entry : studentActivities.entrySet()) {
            StudentActivity activity = entry.getValue();
            report.append("Student: ").append(entry.getKey()).append("\n");
            report.append("  Total Actions: ").append(activity.getTotalActions()).append("\n");
            report.append("  Session Time: ").append(activity.getSessionDuration()).append(" minutes\n");
            report.append("  Recent Activities:\n");
            
            activity.getRecentActivities(5).forEach(log -> {
                report.append("    - ").append(log).append("\n");
            });
            report.append("\n");
        }
        
        return report.toString();
    }
    
    
    /**
     * Get student activities by name
     */
    public List<StudentActivity> getStudentActivities(String studentName) {
        List<StudentActivity> activities = new ArrayList<>();
        for (StudentActivity activity : studentActivities.values()) {
            if (activity.getPlayerName().equals(studentName)) {
                activities.add(activity);
            }
        }
        return activities;
    }
    
    /**
     * Check if player is a student
     */
    public boolean isStudent(String playerName) {
        // In this implementation, everyone who isn't a teacher is a student
        try {
            UUID playerUUID = UUID.nameUUIDFromBytes(playerName.getBytes());
            return !isTeacher(playerUUID);
        } catch (Exception e) {
            return true; // Default to student
        }
    }
    
    /**
     * Get active teacher count
     */
    public int getActiveTeacherCount() {
        // Count online teachers
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return (int) server.getPlayerList().getPlayers().stream()
                .filter(player -> isTeacher(player.getUUID()))
                .count();
        }
        return teacherAccounts.size();
    }
    
    /**
     * Set global permissions
     */
    public void setGlobalPermissions(PermissionLevel level) {
        switch (level) {
            case FULL:
                allowBuilding = true;
                allowChat = true;
                allowVisits = true;
                break;
            case LIMITED:
                allowBuilding = true;
                allowChat = false;
                allowVisits = false;
                break;
            case READONLY:
                allowBuilding = false;
                allowChat = false;
                allowVisits = false;
                break;
            case RESTRICTED:
                allowBuilding = false;
                allowChat = true;
                allowVisits = false;
                break;
            default:
                LOGGER.warn("Unknown permission level: {}", level);
                break;
        }
    }
    
    /**
     * Set student time limit
     */
    public boolean setStudentTimeLimit(String studentName, int minutes) {
        try {
            UUID studentUUID = UUID.nameUUIDFromBytes(studentName.getBytes());
            studentTimeLimits.put(studentUUID, minutes);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Add student restriction
     */
    public boolean addStudentRestriction(String studentName, RestrictionType restriction) {
        try {
            UUID studentUUID = UUID.nameUUIDFromBytes(studentName.getBytes());
            studentRestrictions.computeIfAbsent(studentUUID, k -> new HashSet<>()).add(restriction.name());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
}