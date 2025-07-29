package edu.minecraft.collaboration.progress;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.teacher.StudentActivity;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks learning progress and achievements for students
 */
public class ProgressTracker {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static ProgressTracker instance;
    
    // Progress tracking
    private final Map<UUID, StudentProgress> studentProgress = new ConcurrentHashMap<>();
    private final Map<String, Achievement> availableAchievements = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> earnedAchievements = new ConcurrentHashMap<>();
    
    // Learning milestones
    private final Map<String, LearningMilestone> milestones = new ConcurrentHashMap<>();
    
    // Progress configuration
    private boolean trackingEnabled = true;
    private int pointsPerBlock = 1;
    private int pointsPerCommand = 2;
    private int pointsPerCollaboration = 5;
    
    private ProgressTracker() {
        initializeAchievements();
        initializeMilestones();
    }
    
    public static ProgressTracker getInstance() {
        if (instance == null) {
            instance = new ProgressTracker();
        }
        return instance;
    }
    
    /**
     * Initialize default achievements
     */
    private void initializeAchievements() {
        // Building achievements
        addAchievement(new Achievement("first_block", "", 
            "", AchievementType.BUILDING, 1, 10));
        addAchievement(new Achievement("block_master", "", 
            "100", AchievementType.BUILDING, 100, 100));
        addAchievement(new Achievement("architect", "", 
            "500", AchievementType.BUILDING, 500, 250));
        
        // Collaboration achievements
        addAchievement(new Achievement("social_butterfly", "", 
            "", AchievementType.COLLABORATION, 1, 20));
        addAchievement(new Achievement("team_player", "", 
            "5", AchievementType.COLLABORATION, 5, 50));
        
        // Programming achievements
        addAchievement(new Achievement("programmer", "", 
            "50", AchievementType.PROGRAMMING, 50, 75));
        addAchievement(new Achievement("code_master", "", 
            "200", AchievementType.PROGRAMMING, 200, 150));
        
        // Creative achievements
        addAchievement(new Achievement("artist", "Creative Artist", 
            "Create 5 unique designs", AchievementType.CREATIVE, 5, 80));
        addAchievement(new Achievement("innovator", "", 
            "", AchievementType.CREATIVE, 1, 100));
        
        // Time-based achievements
        addAchievement(new Achievement("dedicated_learner", "Dedicated Learner", 
            "Study for 60 minutes", AchievementType.TIME_BASED, 60, 120));
        addAchievement(new Achievement("persistence", "", 
            "7", AchievementType.TIME_BASED, 7, 200));
    }
    
    /**
     * Initialize learning milestones
     */
    private void initializeMilestones() {
        // Basic skills
        addMilestone(new LearningMilestone("basic_building", "", 
            "", 
            Arrays.asList("place_block", "break_block"), 5));
        
        addMilestone(new LearningMilestone("shape_creation", "", 
            "", 
            Arrays.asList("build_circle", "build_sphere"), 3));
        
        addMilestone(new LearningMilestone("collaboration_skills", "", 
            "", 
            Arrays.asList("invite_friend", "visit_request", "visit_approved"), 5));
        
        addMilestone(new LearningMilestone("programming_basics", "", 
            "", 
            Arrays.asList("command", "command", "command"), 20));
        
        addMilestone(new LearningMilestone("creative_thinking", "Creative Thinking", 
            "Demonstrate creative problem solving", 
            Arrays.asList("agent_action", "build_house", "build_wall"), 10));
        
        addMilestone(new LearningMilestone("problem_solving", "", 
            "", 
            Arrays.asList("emergency_return", "agent_follow"), 3));
        
        addMilestone(new LearningMilestone("advanced_building", "", 
            "", 
            Arrays.asList("build_house", "fill_area", "build_wall"), 15));
    }
    
    /**
     * Track student activity and update progress
     */
    public void trackActivity(UUID studentUUID, String activity, String details) {
        if (!trackingEnabled) return;
        
        StudentProgress progress = studentProgress.computeIfAbsent(
            studentUUID, k -> new StudentProgress(studentUUID)
        );
        
        // Update activity counts
        progress.recordActivity(activity, details);
        
        // Award points
        int points = calculatePoints(activity);
        progress.addPoints(points);
        
        // Check for achievements
        checkAchievements(studentUUID, progress);
        
        // Check for milestone completion
        checkMilestones(studentUUID, progress);
        
        LOGGER.debug("Tracked activity for {}: {} (+{} points)", studentUUID, activity, points);
    }
    
    /**
     * Calculate points for an activity
     */
    private int calculatePoints(String activity) {
        switch (activity.toLowerCase()) {
            case "place_block":
            case "break_block":
                return pointsPerBlock;
            case "command":
            case "agent_action":
            case "build_circle":
            case "build_sphere":
            case "build_wall":
            case "build_house":
                return pointsPerCommand;
            case "invite_friend":
            case "visit_request":
            case "visit_approved":
            case "collaboration":
                return pointsPerCollaboration;
            default:
                return 1;
        }
    }
    
    /**
     * Check and award achievements
     */
    private void checkAchievements(UUID studentUUID, StudentProgress progress) {
        Set<String> earned = earnedAchievements.computeIfAbsent(
            studentUUID, k -> new HashSet<>()
        );
        
        for (Achievement achievement : availableAchievements.values()) {
            if (earned.contains(achievement.getId())) {
                continue; // Already earned
            }
            
            boolean qualifies = false;
            
            switch (achievement.getType()) {
                case BUILDING:
                    qualifies = progress.getTotalBlocks() >= achievement.getRequirement();
                    break;
                case COLLABORATION:
                    qualifies = progress.getCollaborationCount() >= achievement.getRequirement();
                    break;
                case PROGRAMMING:
                    qualifies = progress.getTotalCommands() >= achievement.getRequirement();
                    break;
                case CREATIVE:
                    qualifies = progress.getUniqueActivities().size() >= achievement.getRequirement();
                    break;
                case TIME_BASED:
                    qualifies = progress.getSessionMinutes() >= achievement.getRequirement();
                    break;
            }
            
            if (qualifies) {
                awardAchievement(studentUUID, achievement);
                earned.add(achievement.getId());
            }
        }
    }
    
    /**
     * Check milestone completion
     */
    private void checkMilestones(UUID studentUUID, StudentProgress progress) {
        for (LearningMilestone milestone : milestones.values()) {
            if (progress.isMilestoneCompleted(milestone.getId())) {
                continue; // Already completed
            }
            
            boolean completed = true;
            for (String requiredActivity : milestone.getRequiredActivities()) {
                int required = milestone.getRequiredCount();
                int actual = progress.getActivityCount(requiredActivity);
                if (actual < required) {
                    completed = false;
                    break;
                }
            }
            
            if (completed) {
                completeMilestone(studentUUID, milestone);
                progress.completeMilestone(milestone.getId());
            }
        }
    }
    
    /**
     * Award achievement to student
     */
    private void awardAchievement(UUID studentUUID, Achievement achievement) {
        LOGGER.info("Achievement earned by {}: {}", studentUUID, achievement.getName());
        
        StudentProgress progress = studentProgress.get(studentUUID);
        if (progress != null) {
            progress.addPoints(achievement.getPointReward());
            progress.awardAchievement(achievement);
        }
    }
    
    /**
     * Complete milestone for student
     */
    private void completeMilestone(UUID studentUUID, LearningMilestone milestone) {
        LOGGER.info("Milestone completed by {}: {}", studentUUID, milestone.getName());
        
        StudentProgress progress = studentProgress.get(studentUUID);
        if (progress != null) {
            progress.addPoints(milestone.getPointReward());
        }
    }
    
    /**
     * Get student progress
     */
    public StudentProgress getStudentProgress(UUID studentUUID) {
        return studentProgress.get(studentUUID);
    }
    
    /**
     * Get all student progress
     */
    public Map<UUID, StudentProgress> getAllProgress() {
        return new HashMap<>(studentProgress);
    }
    
    /**
     * Get student achievements
     */
    public Set<String> getStudentAchievements(UUID studentUUID) {
        return earnedAchievements.getOrDefault(studentUUID, new HashSet<>());
    }
    
    /**
     * Generate progress report
     */
    public String generateProgressReport(UUID studentUUID) {
        StudentProgress progress = studentProgress.get(studentUUID);
        if (progress == null) {
            return "No progress data available for student.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("=== Learning Progress Report ===\n");
        report.append("Student: ").append(studentUUID).append("\n");
        report.append("Total Points: ").append(progress.getTotalPoints()).append("\n");
        report.append("Level: ").append(progress.getLevel()).append("\n");
        report.append("Session Time: ").append(progress.getSessionMinutes()).append(" minutes\n");
        report.append("\n=== Activities ===\n");
        report.append("Blocks Placed: ").append(progress.getTotalBlocks()).append("\n");
        report.append("Commands Executed: ").append(progress.getTotalCommands()).append("\n");
        report.append("Collaborations: ").append(progress.getCollaborationCount()).append("\n");
        
        report.append("\n=== Achievements ===\n");
        Set<String> achievements = earnedAchievements.getOrDefault(studentUUID, new HashSet<>());
        if (achievements.isEmpty()) {
            report.append("No achievements yet.\n");
        } else {
            for (String achievementId : achievements) {
                Achievement achievement = availableAchievements.get(achievementId);
                if (achievement != null) {
                    report.append(" ").append(achievement.getName())
                          .append(": ").append(achievement.getDescription()).append("\n");
                }
            }
        }
        
        report.append("\n=== Milestones ===\n");
        Set<String> completedMilestones = progress.getCompletedMilestones();
        for (LearningMilestone milestone : milestones.values()) {
            String status = completedMilestones.contains(milestone.getId()) ? "✓" : "○";
            report.append(status).append(" ").append(milestone.getName())
                  .append(": ").append(milestone.getDescription()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Add new achievement
     */
    public void addAchievement(Achievement achievement) {
        availableAchievements.put(achievement.getId(), achievement);
    }
    
    /**
     * Add new milestone
     */
    public void addMilestone(LearningMilestone milestone) {
        milestones.put(milestone.getId(), milestone);
    }
    
    /**
     * Configure tracking settings
     */
    public void configureTracking(boolean enabled, int blockPoints, int commandPoints, int collabPoints) {
        this.trackingEnabled = enabled;
        this.pointsPerBlock = blockPoints;
        this.pointsPerCommand = commandPoints;
        this.pointsPerCollaboration = collabPoints;
        
        LOGGER.info("Progress tracking configured: enabled={}, points: block={}, command={}, collab={}", 
            enabled, blockPoints, commandPoints, collabPoints);
    }
    
    /**
     * Reset student progress
     */
    public void resetStudentProgress(UUID studentUUID) {
        studentProgress.remove(studentUUID);
        earnedAchievements.remove(studentUUID);
        LOGGER.info("Reset progress for student: {}", studentUUID);
    }
    
    /**
     * Export progress data
     */
    public Map<String, Object> exportProgressData(UUID studentUUID) {
        StudentProgress progress = studentProgress.get(studentUUID);
        if (progress == null) {
            return new HashMap<>();
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("studentUUID", studentUUID.toString());
        data.put("totalPoints", progress.getTotalPoints());
        data.put("level", progress.getLevel());
        data.put("sessionMinutes", progress.getSessionMinutes());
        data.put("achievements", earnedAchievements.getOrDefault(studentUUID, new HashSet<>()));
        data.put("milestones", progress.getCompletedMilestones());
        data.put("activities", progress.getActivityCounts());
        
        return data;
    }
}