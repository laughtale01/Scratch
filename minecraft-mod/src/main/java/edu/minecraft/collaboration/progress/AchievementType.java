package edu.minecraft.collaboration.progress;

/**
 * Types of achievements that can be earned
 */
public enum AchievementType {
    BUILDING("Building & Construction"),
    COLLABORATION("Teamwork & Communication"),
    PROGRAMMING("Programming & Logic"),
    CREATIVE("Creativity & Innovation"),
    TIME_BASED("Dedication & Persistence"),
    PROBLEM_SOLVING("Problem Solving"),
    LEADERSHIP("Leadership"),
    EXPLORATION("Exploration & Discovery");

    private final String displayName;

    AchievementType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
