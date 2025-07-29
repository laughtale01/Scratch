package com.yourname.minecraftcollaboration.teacher;

import com.yourname.minecraftcollaboration.MinecraftCollaborationMod;
import com.yourname.minecraftcollaboration.localization.LanguageManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.Duration;

/**
 * Teacher management system for monitoring and controlling student activities
 */
public class TeacherManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static TeacherManager instance;
    private final LanguageManager languageManager;
    
    // Teacher accounts
    private final Set<UUID> teacherAccounts = new HashSet<>();
    
    // Student activity tracking
    private final Map<UUID, StudentActivity> studentActivities = new ConcurrentHashMap<>();
    
    // Time limits
    private final Map<UUID, TimeLimit> studentTimeLimits = new ConcurrentHashMap<>();
    
    // Blocked actions per student
    private final Map<UUID, Set<String>> studentRestrictions = new ConcurrentHashMap<>();
    
    // Global classroom settings
    private boolean classroomMode = false;
    private boolean allowBuilding = true;
    private boolean allowChat = true;
    private boolean allowVisits = true;
    private int maxSessionMinutes = 60;
    
    private TeacherManager() {
        this.languageManager = LanguageManager.getInstance();
    }
    
    public static TeacherManager getInstance() {
        if (instance == null) {
            instance = new TeacherManager();
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
                if (!allowBuilding) return false;
                break;
            case "chat":
                if (!allowChat) return false;
                break;
            case "visit":
            case "invite":
                if (!allowVisits) return false;
                break;
        }
        
        // Check student-specific restrictions
        Set<String> restrictions = studentRestrictions.get(playerUUID);
        if (restrictions != null && restrictions.contains(action.toLowerCase())) {
            return false;
        }
        
        // Check time limits
        TimeLimit timeLimit = studentTimeLimits.get(playerUUID);
        if (timeLimit != null && timeLimit.isExpired()) {
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
            studentTimeLimits.put(playerUUID, new TimeLimit(minutes));
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
            Component msg = Component.literal("§e[先生より] §f" + message);
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
            Component msg = Component.literal("§c[Teacher Alert] §f" + message);
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
     * Time limit tracking
     */
    private static class TimeLimit {
        private final LocalDateTime expiryTime;
        
        public TimeLimit(int minutes) {
            this.expiryTime = LocalDateTime.now().plusMinutes(minutes);
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
        
        public Duration getTimeRemaining() {
            return Duration.between(LocalDateTime.now(), expiryTime);
        }
    }
}