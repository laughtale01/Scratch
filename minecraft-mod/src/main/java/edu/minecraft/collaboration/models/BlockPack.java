package edu.minecraft.collaboration.models;

import net.minecraft.world.level.block.Block;

import java.util.*;

/**
 * Represents a block pack with categorized blocks for educational purposes
 */
public class BlockPack {
    
    public enum BlockPackCategory {
        BASIC, EDUCATIONAL, ADVANCED, CREATIVE, REDSTONE, CUSTOM
    }
    
    public enum DifficultyLevel {
        BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    }
    
    private final String id;
    private final Map<String, String> names;
    private final Map<String, String> descriptions;
    private final List<Block> allowedBlocks;
    private final BlockPackCategory category;
    private final DifficultyLevel difficulty;
    private final boolean teacherRequired;
    
    public BlockPack(String id, Map<String, String> names, Map<String, String> descriptions, 
                     List<Block> allowedBlocks, BlockPackCategory category, 
                     DifficultyLevel difficulty, boolean teacherRequired) {
        this.id = id;
        this.names = new HashMap<>(names);
        this.descriptions = new HashMap<>(descriptions);
        this.allowedBlocks = new ArrayList<>(allowedBlocks);
        this.category = category;
        this.difficulty = difficulty;
        this.teacherRequired = teacherRequired;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName(String language) {
        return names.getOrDefault(language, names.getOrDefault("en_US", id));
    }
    
    public String getDescription(String language) {
        return descriptions.getOrDefault(language, descriptions.getOrDefault("en_US", ""));
    }
    
    public List<Block> getAllowedBlocks() {
        return new ArrayList<>(allowedBlocks);
    }
    
    public BlockPackCategory getCategory() {
        return category;
    }
    
    public DifficultyLevel getDifficulty() {
        return difficulty;
    }
    
    public boolean isTeacherRequired() {
        return teacherRequired;
    }
    
    public Map<String, String> getAllNames() {
        return new HashMap<>(names);
    }
    
    public Map<String, String> getAllDescriptions() {
        return new HashMap<>(descriptions);
    }
    
    public boolean contains(Block block) {
        return allowedBlocks.contains(block);
    }
    
    public int getBlockCount() {
        return allowedBlocks.size();
    }
    
    @Override
    public String toString() {
        return "BlockPack{" +
                "id='" + id + '\'' +
                ", category=" + category +
                ", difficulty=" + difficulty +
                ", blockCount=" + allowedBlocks.size() +
                ", teacherRequired=" + teacherRequired +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockPack blockPack = (BlockPack) o;
        return Objects.equals(id, blockPack.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}