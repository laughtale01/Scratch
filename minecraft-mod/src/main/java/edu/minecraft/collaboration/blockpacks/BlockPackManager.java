package edu.minecraft.collaboration.blockpacks;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.world.level.block.Blocks;

/**
 * Manager for block packs - predefined collections of blocks for educational purposes
 */
public final class BlockPackManager {

    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static volatile BlockPackManager instance;
    private static final Object LOCK = new Object();
    private final Map<String, BlockPack> blockPacks;
    private String currentPackId = "basic";

    private BlockPackManager() {
        this.blockPacks = new HashMap<>();
        initializeDefaultBlockPacks();
    }

    public static BlockPackManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new BlockPackManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize default block packs
     */
    private void initializeDefaultBlockPacks() {
        createBasicBlockPack();
        createEducationalBlockPack();
        createAdvancedBlockPack();
        createCreativeBlockPack();
        createRedstoneBlockPack();
    }

    /**
     * Create basic block pack for beginners
     */
    private void createBasicBlockPack() {
        List<String> blocks = Arrays.asList(
            "stone", "dirt", "wood", "planks", "cobblestone",
            "sand", "gravel", "glass", "brick", "wool"
        );

        Map<String, String> names = new HashMap<>();
        names.put("en_US", "Basic Blocks");
        names.put("ja_JP", "Basic Blocks");
        names.put("zh_CN", "Basic Blocks");
        names.put("zh_TW", "Basic Blocks");
        names.put("ko_KR", "Basic Blocks");
        names.put("es_ES", "Bloques Básicos");
        names.put("fr_FR", "Blocs de Base");
        names.put("de_DE", "Grundblöcke");

        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "The most fundamental building blocks. Perfect for beginners.");
        descriptions.put("en_US", "The most fundamental building blocks. Perfect for beginners.");
        descriptions.put("zh_CN", "The most fundamental building blocks. Perfect for beginners.");
        descriptions.put("zh_TW", "The most fundamental building blocks. Perfect for beginners.");
        descriptions.put("ko_KR", "The most fundamental building blocks. Perfect for beginners.");
        descriptions.put("es_ES", "El conjunto de bloques de construcción más fundamental. Perfecto para principiantes.");
        descriptions.put("fr_FR", "L'ensemble de blocs de construction le plus fondamental. Parfait pour les débutants.");
        descriptions.put("de_DE", "Die grundlegendsten Baublöcke. Perfekt für Anfänger.");

        // Convert string list to Block list
        List<Block> blockList = convertStringListToBlocks(blocks);
        BlockPack basicPack = new BlockPack("basic", names, descriptions, blockList,
            BlockPackCategory.BASIC, DifficultyLevel.BEGINNER, false);
        blockPacks.put("basic", basicPack);
    }

    /**
     * Create educational block pack with colorful blocks
     */
    private void createEducationalBlockPack() {
        List<String> blocks = Arrays.asList(
            "white_wool", "red_wool", "blue_wool", "green_wool", "yellow_wool",
            "orange_wool", "purple_wool", "pink_wool", "lime_wool", "cyan_wool",
            "light_blue_wool", "magenta_wool", "brown_wool", "gray_wool", "black_wool"
        );

        Map<String, String> names = new HashMap<>();
        names.put("en_US", "Educational Blocks");
        names.put("ja_JP", "Educational Blocks");
        names.put("zh_CN", "Educational Blocks");
        names.put("zh_TW", "Educational Blocks");
        names.put("ko_KR", "Educational Blocks");
        names.put("es_ES", "Bloques Educativos");
        names.put("fr_FR", "Blocs Éducatifs");
        names.put("de_DE", "Bildungsblöcke");

        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "Colorful blocks designed for educational purposes. Perfect for teaching.");
        descriptions.put("en_US", "Colorful blocks designed for educational purposes. Perfect for teaching.");
        descriptions.put("zh_CN", "Colorful blocks designed for educational purposes. Perfect for teaching.");
        descriptions.put("zh_TW", "Colorful blocks designed for educational purposes. Perfect for teaching.");
        descriptions.put("ko_KR", "Colorful blocks designed for educational purposes. Perfect for teaching.");
        descriptions.put("es_ES", "Bloques coloridos diseñados para fines educativos. Perfecto para enseñar.");
        descriptions.put("fr_FR", "Blocs colorés conçus à des fins éducatives. Parfait pour enseigner.");
        descriptions.put("de_DE", "Bunte Blöcke für Bildungszwecke. Perfekt zum Unterrichten.");

        List<Block> blockList = convertStringListToBlocks(blocks);
        BlockPack educationalPack = new BlockPack("educational", names, descriptions, blockList,
            BlockPackCategory.EDUCATIONAL, DifficultyLevel.BEGINNER, false);
        blockPacks.put("educational", educationalPack);
    }

    /**
     * Create advanced block pack
     */
    private void createAdvancedBlockPack() {
        List<String> blocks = Arrays.asList(
            "quartz", "nether_brick", "end_stone", "prismarine", "purpur_block",
            "concrete", "terracotta", "glazed_terracotta", "shulker_box", "observer"
        );

        Map<String, String> names = new HashMap<>();
        names.put("en_US", "Advanced Blocks");
        names.put("ja_JP", "Advanced Blocks");
        names.put("zh_CN", "Advanced Blocks");
        names.put("zh_TW", "Advanced Blocks");
        names.put("ko_KR", "Advanced Blocks");
        names.put("es_ES", "Bloques Avanzados");
        names.put("fr_FR", "Blocs Avancés");
        names.put("de_DE", "Erweiterte Blöcke");

        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "Advanced blocks for experienced builders. Great for detailed construction.");
        descriptions.put("en_US", "Advanced blocks for experienced builders. Great for detailed construction.");
        descriptions.put("zh_CN", "Advanced blocks for experienced builders. Great for detailed construction.");
        descriptions.put("zh_TW", "Advanced blocks for experienced builders. Great for detailed construction.");
        descriptions.put("ko_KR", "Advanced blocks for experienced builders. Great for detailed construction.");
        descriptions.put("es_ES", "Bloques avanzados para constructores experimentados. Genial para construcción detallada.");
        descriptions.put("fr_FR", "Blocs avancés pour les constructeurs expérimentés. Idéal pour la construction détaillée.");
        descriptions.put("de_DE", "Erweiterte Blöcke für erfahrene Baumeister. Großartig für detaillierte Konstruktion.");

        List<Block> blockList = convertStringListToBlocks(blocks);
        BlockPack advancedPack = new BlockPack("advanced", names, descriptions, blockList,
            BlockPackCategory.ADVANCED, DifficultyLevel.INTERMEDIATE, true);
        blockPacks.put("advanced", advancedPack);
    }

    /**
     * Create creative block pack
     */
    private void createCreativeBlockPack() {
        List<String> blocks = Arrays.asList(
            "diamond_block", "gold_block", "iron_block", "emerald_block",
            "lapis_block", "redstone_block", "coal_block", "quartz_block",
            "beacon", "conduit", "dragon_egg", "elytra"
        );

        Map<String, String> names = new HashMap<>();
        names.put("en_US", "Creative Blocks");
        names.put("ja_JP", "Creative Blocks");
        names.put("zh_CN", "Creative Blocks");
        names.put("zh_TW", "Creative Blocks");
        names.put("ko_KR", "Creative Blocks");
        names.put("es_ES", "Bloques Creativos");
        names.put("fr_FR", "Blocs Créatifs");
        names.put("de_DE", "Kreative Blöcke");

        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "Special blocks for creative building. Perfect for artistic projects.");
        descriptions.put("en_US", "Special blocks for creative building. Perfect for artistic projects.");
        descriptions.put("zh_CN", "Special blocks for creative building. Perfect for artistic projects.");
        descriptions.put("zh_TW", "Special blocks for creative building. Perfect for artistic projects.");
        descriptions.put("ko_KR", "Special blocks for creative building. Perfect for artistic projects.");
        descriptions.put("es_ES", "Bloques especiales para construcción creativa. Perfecto para proyectos artísticos.");
        descriptions.put("fr_FR", "Blocs spéciaux pour la construction créative. Parfait pour les projets artistiques.");
        descriptions.put("de_DE", "Besondere Blöcke für kreatives Bauen. Perfekt für künstlerische Projekte.");

        List<Block> blockList = convertStringListToBlocks(blocks);
        BlockPack creativePack = new BlockPack("creative", names, descriptions, blockList,
            BlockPackCategory.CREATIVE, DifficultyLevel.ADVANCED, false);
        blockPacks.put("creative", creativePack);
    }

    /**
     * Create redstone block pack for programming concepts
     */
    private void createRedstoneBlockPack() {
        List<String> blocks = Arrays.asList(
            "redstone", "redstone_torch", "repeater", "comparator",
            "piston", "sticky_piston", "lever", "button",
            "pressure_plate", "tripwire_hook", "observer", "dispenser"
        );

        Map<String, String> names = new HashMap<>();
        names.put("en_US", "Redstone Blocks");
        names.put("ja_JP", "Redstone Blocks");
        names.put("zh_CN", "Redstone Blocks");
        names.put("zh_TW", "Redstone Blocks");
        names.put("ko_KR", "Redstone Blocks");
        names.put("es_ES", "Bloques de Redstone");
        names.put("fr_FR", "Blocs de Redstone");
        names.put("de_DE", "Redstone-Blöcke");

        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("ja_JP", "Redstone blocks for learning programming concepts and logic circuits.");
        descriptions.put("en_US", "Redstone blocks for learning programming concepts and logic circuits.");
        descriptions.put("zh_CN", "Redstone blocks for learning programming concepts and logic circuits.");
        descriptions.put("zh_TW", "Redstone blocks for learning programming concepts and logic circuits.");
        descriptions.put("ko_KR", "Redstone blocks for learning programming concepts and logic circuits.");
        descriptions.put("es_ES", "Bloques de redstone para aprender conceptos de programación y circuitos lógicos.");
        descriptions.put("fr_FR", "Blocs de redstone pour apprendre les concepts de programmation et les circuits logiques.");
        descriptions.put("de_DE", "Redstone-Blöcke zum Erlernen von Programmierkonzepten und Logikschaltungen.");

        List<Block> blockList = convertStringListToBlocks(blocks);
        BlockPack redstonePack = new BlockPack("redstone", names, descriptions, blockList,
            BlockPackCategory.PROGRAMMING, DifficultyLevel.EXPERT, true);
        blockPacks.put("redstone", redstonePack);
    }

    /**
     * Get all available block packs
     */
    public Collection<BlockPack> getAllBlockPacks() {
        return blockPacks.values();
    }

    /**
     * Get a specific block pack by ID
     */
    public BlockPack getBlockPack(String packId) {
        return blockPacks.get(packId);
    }

    /**
     * Get the current active block pack
     */
    public BlockPack getCurrentBlockPack() {
        return blockPacks.get(currentPackId);
    }

    /**
     * Set the current active block pack
     */
    public boolean setCurrentBlockPack(String packId) {
        if (blockPacks.containsKey(packId)) {
            currentPackId = packId;
            LOGGER.info("Changed current block pack to: {}", packId);
            return true;
        }
        return false;
    }

    /**
     * Check if a block is allowed in the current pack
     */
    public boolean isBlockAllowed(String blockType) {
        BlockPack currentPack = getCurrentBlockPack();
        if (currentPack == null) {
            return false;
        }
        Block block = getBlockByName(blockType);
        return block != null && currentPack.getAllowedBlocks().contains(block);
    }

    /**
     * Get all allowed blocks for the current pack
     */
    public List<String> getAllowedBlocks() {
        BlockPack currentPack = getCurrentBlockPack();
        if (currentPack == null) {
            return new ArrayList<>();
        }

        // Convert Block objects back to string names
        List<String> blockNames = new ArrayList<>();
        for (Block block : currentPack.getAllowedBlocks()) {
            String blockName = getBlockName(block);
            blockNames.add(blockName);
        }
        return blockNames;
    }

    /**
     * Create a custom block pack
     */
    public boolean createCustomBlockPack(String packId, Map<String, String> names,
                                       Map<String, String> descriptions, List<String> blocks) {
        if (blockPacks.containsKey(packId)) {
            return false; // Pack already exists
        }

        List<Block> blockList = convertStringListToBlocks(blocks);
        BlockPack customPack = new BlockPack(packId, names, descriptions, blockList,
            BlockPackCategory.CUSTOM, DifficultyLevel.INTERMEDIATE, true);
        blockPacks.put(packId, customPack);
        LOGGER.info("Created custom block pack: {}", packId);
        return true;
    }

    /**
     * Remove a custom block pack (cannot remove default packs)
     */
    public boolean removeCustomBlockPack(String packId) {
        // Prevent removal of default packs
        if (Arrays.asList("basic", "educational", "advanced", "creative", "redstone").contains(packId)) {
            return false;
        }

        if (blockPacks.containsKey(packId)) {
            blockPacks.remove(packId);

            // If this was the current pack, switch to basic
            if (currentPackId.equals(packId)) {
                currentPackId = "basic";
            }

            LOGGER.info("Removed custom block pack: {}", packId);
            return true;
        }
        return false;
    }

    /**
     * Get pack info for display
     */
    public Map<String, Object> getPackInfo(String packId, String languageCode) {
        BlockPack pack = blockPacks.get(packId);
        if (pack == null) {
            return null;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("id", packId);
        info.put("name", pack.getName(languageCode));
        info.put("description", pack.getDescription(languageCode));
        info.put("blockCount", pack.getAllowedBlocks().size());
        info.put("blocks", pack.getAllowedBlocks());

        return info;
    }

    /**
     * Convert list of block names to Block objects
     */
    private List<Block> convertStringListToBlocks(List<String> blockNames) {
        List<Block> blocks = new ArrayList<>();
        for (String blockName : blockNames) {
            try {
                Block block = getBlockByName(blockName);
                if (block != null) {
                    blocks.add(block);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to convert block name '{}' to Block object", blockName);
            }
        }
        return blocks;
    }

    /**
     * Get Block by name
     */
    private Block getBlockByName(String blockName) {
        String normalizedName = blockName.toLowerCase();

        // Try common blocks first
        Block commonBlock = getCommonBlock(normalizedName);
        if (commonBlock != null) {
            return commonBlock;
        }

        // Try building blocks
        Block buildingBlock = getBuildingBlock(normalizedName);
        if (buildingBlock != null) {
            return buildingBlock;
        }

        // Try utility blocks
        Block utilityBlock = getUtilityBlock(normalizedName);
        if (utilityBlock != null) {
            return utilityBlock;
        }

        // Try redstone blocks
        Block redstoneBlock = getRedstoneBlock(normalizedName);
        if (redstoneBlock != null) {
            return redstoneBlock;
        }

        // Fall back to registry lookup
        return getBlockFromRegistry(normalizedName);
    }

    /**
     * Get common basic blocks
     * @param blockName Normalized block name
     * @return Block or null if not found
     */
    private Block getCommonBlock(String blockName) {
        switch (blockName) {
            case "dirt": return Blocks.DIRT;
            case "stone": return Blocks.STONE;
            case "grass_block": return Blocks.GRASS_BLOCK;
            case "cobblestone": return Blocks.COBBLESTONE;
            case "sand": return Blocks.SAND;
            case "gravel": return Blocks.GRAVEL;
            case "bedrock": return Blocks.BEDROCK;
            case "water": return Blocks.WATER;
            case "lava": return Blocks.LAVA;
            default: return null;
        }
    }

    /**
     * Get building and construction blocks
     * @param blockName Normalized block name
     * @return Block or null if not found
     */
    private Block getBuildingBlock(String blockName) {
        switch (blockName) {
            case "oak_planks": return Blocks.OAK_PLANKS;
            case "oak_log": return Blocks.OAK_LOG;
            case "oak_leaves": return Blocks.OAK_LEAVES;
            case "glass": return Blocks.GLASS;
            case "brick": return Blocks.BRICKS;
            case "bookshelf": return Blocks.BOOKSHELF;
            case "mossy_cobblestone": return Blocks.MOSSY_COBBLESTONE;
            case "obsidian": return Blocks.OBSIDIAN;
            case "oak_door": return Blocks.OAK_DOOR;
            case "iron_door": return Blocks.IRON_DOOR;
            case "oak_fence": return Blocks.OAK_FENCE;
            case "oak_fence_gate": return Blocks.OAK_FENCE_GATE;
            case "stairs": return Blocks.OAK_STAIRS;
            case "wool": return Blocks.WHITE_WOOL;
            default: return null;
        }
    }

    /**
     * Get utility and functional blocks
     * @param blockName Normalized block name
     * @return Block or null if not found
     */
    private Block getUtilityBlock(String blockName) {
        switch (blockName) {
            case "torch": return Blocks.TORCH;
            case "chest": return Blocks.CHEST;
            case "crafting_table": return Blocks.CRAFTING_TABLE;
            case "furnace": return Blocks.FURNACE;
            case "ladder": return Blocks.LADDER;
            case "tnt": return Blocks.TNT;
            default: return null;
        }
    }

    /**
     * Get redstone and ore blocks
     * @param blockName Normalized block name
     * @return Block or null if not found
     */
    private Block getRedstoneBlock(String blockName) {
        switch (blockName) {
            case "coal_ore": return Blocks.COAL_ORE;
            case "iron_ore": return Blocks.IRON_ORE;
            case "gold_ore": return Blocks.GOLD_ORE;
            case "diamond_ore": return Blocks.DIAMOND_ORE;
            case "redstone_ore": return Blocks.REDSTONE_ORE;
            case "redstone_wire": return Blocks.REDSTONE_WIRE;
            case "redstone_torch": return Blocks.REDSTONE_TORCH;
            case "lever": return Blocks.LEVER;
            case "stone_button": return Blocks.STONE_BUTTON;
            case "redstone_lamp": return Blocks.REDSTONE_LAMP;
            default: return null;
        }
    }

    /**
     * Get block from Minecraft registry as fallback
     * @param blockName Normalized block name
     * @return Block from registry or dirt as default
     */
    private Block getBlockFromRegistry(String blockName) {
        try {
            ResourceLocation location = new ResourceLocation("minecraft", blockName);
            Block registryBlock = BuiltInRegistries.BLOCK.get(location);

            // Check if the block was actually found (registry returns air for unknown blocks)
            if (registryBlock != Blocks.AIR) {
                return registryBlock;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get block from registry: {}", blockName, e);
        }

        LOGGER.warn("Unknown block name: {}, using dirt as fallback", blockName);
        return Blocks.DIRT; // Default fallback
    }

    /**
     * Get block name from Block object
     */
    private String getBlockName(Block block) {
        ResourceLocation location = BuiltInRegistries.BLOCK.getKey(block);
        return location.getPath();
    }
}
