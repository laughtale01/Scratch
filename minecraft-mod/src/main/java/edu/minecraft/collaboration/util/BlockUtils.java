package edu.minecraft.collaboration.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for block-related operations
 * Centralizes block name to Block object mapping
 */
public final class BlockUtils {
    
    private static final Map<String, Block> BLOCK_MAP = new HashMap<>();
    
    // Private constructor to prevent instantiation
    private BlockUtils() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    static {
        // Initialize block mappings
        BLOCK_MAP.put("stone", Blocks.STONE);
        BLOCK_MAP.put("dirt", Blocks.DIRT);
        BLOCK_MAP.put("grass", Blocks.GRASS_BLOCK);
        BLOCK_MAP.put("grass_block", Blocks.GRASS_BLOCK);
        BLOCK_MAP.put("wood", Blocks.OAK_PLANKS);
        BLOCK_MAP.put("oak_planks", Blocks.OAK_PLANKS);
        BLOCK_MAP.put("glass", Blocks.GLASS);
        BLOCK_MAP.put("diamond_block", Blocks.DIAMOND_BLOCK);
        BLOCK_MAP.put("gold_block", Blocks.GOLD_BLOCK);
        BLOCK_MAP.put("iron_block", Blocks.IRON_BLOCK);
        BLOCK_MAP.put("air", Blocks.AIR);
        BLOCK_MAP.put("cobblestone", Blocks.COBBLESTONE);
        BLOCK_MAP.put("sand", Blocks.SAND);
        BLOCK_MAP.put("gravel", Blocks.GRAVEL);
        BLOCK_MAP.put("water", Blocks.WATER);
        BLOCK_MAP.put("lava", Blocks.LAVA);
        BLOCK_MAP.put("oak_log", Blocks.OAK_LOG);
        BLOCK_MAP.put("leaves", Blocks.OAK_LEAVES);
        BLOCK_MAP.put("oak_leaves", Blocks.OAK_LEAVES);
        BLOCK_MAP.put("brick", Blocks.BRICKS);
        BLOCK_MAP.put("bricks", Blocks.BRICKS);
        BLOCK_MAP.put("wool", Blocks.WHITE_WOOL);
        BLOCK_MAP.put("white_wool", Blocks.WHITE_WOOL);
        
        // Add more block types
        BLOCK_MAP.put("obsidian", Blocks.OBSIDIAN);
        BLOCK_MAP.put("bedrock", Blocks.BEDROCK);
        BLOCK_MAP.put("netherrack", Blocks.NETHERRACK);
        BLOCK_MAP.put("glowstone", Blocks.GLOWSTONE);
        BLOCK_MAP.put("emerald_block", Blocks.EMERALD_BLOCK);
        BLOCK_MAP.put("redstone_block", Blocks.REDSTONE_BLOCK);
        BLOCK_MAP.put("quartz_block", Blocks.QUARTZ_BLOCK);
        BLOCK_MAP.put("snow", Blocks.SNOW_BLOCK);
        BLOCK_MAP.put("ice", Blocks.ICE);
        BLOCK_MAP.put("packed_ice", Blocks.PACKED_ICE);
    }
    
    /**
     * Get Minecraft block from string name
     * @param blockType The string name of the block
     * @return The corresponding Block object, or STONE if not found
     */
    public static Block getBlockFromString(String blockType) {
        if (blockType == null || blockType.isEmpty()) {
            return Blocks.STONE;
        }
        
        Block block = BLOCK_MAP.get(blockType.toLowerCase());
        return block != null ? block : Blocks.STONE;
    }
    
    /**
     * Check if a block type is valid
     * @param blockType The string name of the block
     * @return true if the block type is recognized
     */
    public static boolean isValidBlockType(String blockType) {
        return blockType != null && BLOCK_MAP.containsKey(blockType.toLowerCase());
    }
    
    /**
     * Get all available block types
     * @return An array of available block type names
     */
    public static String[] getAvailableBlockTypes() {
        return BLOCK_MAP.keySet().toArray(new String[0]);
    }
    
    /**
     * Get block name from Block object
     */
    public static String getBlockName(Block block) {
        if (block == null) {
            return "air";
        }
        
        // Get the registry name of the block
        net.minecraft.resources.ResourceLocation location = 
            net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        
        if (location != null) {
            return location.getPath();
        }
        
        return "unknown";
    }
}