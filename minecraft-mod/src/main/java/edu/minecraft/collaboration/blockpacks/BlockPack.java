package edu.minecraft.collaboration.blockpacks;

import net.minecraft.world.level.block.Block;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;

/**
 * ブロックパック縺ｮ螳夂ｾｩ繧ｯ繝ｩ繧ｹ
 */
public class BlockPack {
    private final String id;
    private final Map<String, String> names;
    private final Map<String, String> descriptions;
    private final List<Block> allowedBlocks;
    private final BlockPackCategory category;
    private final DifficultyLevel difficulty;
    private final boolean studentAccessible;
    private final LocalDateTime createdAt;
    private final Map<String, Object> metadata;

    public BlockPack(String id, Map<String, String> names, Map<String, String> descriptions,
                    List<Block> allowedBlocks, BlockPackCategory category,
                    DifficultyLevel difficulty, boolean studentAccessible) {
        this.id = id;
        this.names = new HashMap<>(names);
        this.descriptions = new HashMap<>(descriptions);
        this.allowedBlocks = new ArrayList<>(allowedBlocks);
        this.category = category;
        this.difficulty = difficulty;
        this.studentAccessible = studentAccessible;
        this.createdAt = LocalDateTime.now();
        this.metadata = new HashMap<>();
    }

    /**
     * 險隱槭↓蠢懊§縺溷錐蜑阪ｒ蜿門ｾ・     */
    public String getName(String language) {
        return names.getOrDefault(language, names.getOrDefault("ja_JP", id));
    }

    /**
     * 險隱槭↓蠢懊§縺溯ｪｬ譏弱ｒ蜿門ｾ・     */
    public String getDescription(String language) {
        return descriptions.getOrDefault(language, descriptions.getOrDefault("ja_JP", ""));
    }

    /**
     * ブロック謨ｰ繧貞叙蠕・     */
    public int getBlockCount() {
        return allowedBlocks.size();
    }

    /**
     * 迚ｹ螳壹・ブロック縺悟性縺ｾ繧後※縺・ｋ縺九メ繧ｧ繝・け
     */
    public boolean containsBlock(Block block) {
        return allowedBlocks.contains(block);
    }

    /**
     * ブロック繝ｪ繧ｹ繝医ｒ螳牙・縺ｫ蜿門ｾ・     */
    public List<Block> getAllowedBlocks() {
        return new ArrayList<>(allowedBlocks);
    }

    /**
     * 繝｡繧ｿ繝（繧ｿ繧定ｿｽ蜉
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * 繝｡繧ｿ繝（繧ｿ繧貞叙蠕・     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * パック諠・ｱ繧偵お繧ｯ繧ｹ繝昴・繝・     */
    public Map<String, Object> exportData() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("names", new HashMap<>(names));
        data.put("descriptions", new HashMap<>(descriptions));
        data.put("blockCount", allowedBlocks.size());
        data.put("category", category.name());
        data.put("difficulty", difficulty.name());
        data.put("studentAccessible", studentAccessible);
        data.put("createdAt", createdAt.toString());
        data.put("metadata", new HashMap<>(metadata));

        return data;
    }

    /**
     * 隧ｳ邏ｰ縺ｪ隱ｬ譏弱ｒ逕滓・
     */
    public String getDetailedInfo(String language) {
        StringBuilder info = new StringBuilder();
        info.append("Pack: ").append(getName(language)).append("\n");
        info.append("Description: ").append(getDescription(language)).append("\n");
        info.append("Category: ").append(category.getDisplayName(language)).append("\n");
        info.append("Difficulty: ").append(difficulty.getDisplayName(language)).append("\n");
        info.append("Blocks: ").append(allowedBlocks.size()).append("\n");
        info.append("Student Access: ").append(studentAccessible ? "Yes" : "No").append("\n");

        return info.toString();
    }

    // Getters
    public String getId() {
        return id;
    }
    public Map<String, String> getNames() {
        return new HashMap<>(names);
    }
    public Map<String, String> getDescriptions() {
        return new HashMap<>(descriptions);
    }
    public BlockPackCategory getCategory() {
        return category;
    }
    public DifficultyLevel getDifficulty() {
        return difficulty;
    }
    public boolean isStudentAccessible() {
        return studentAccessible;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BlockPack blockPack = (BlockPack) obj;
        return Objects.equals(id, blockPack.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("BlockPack[id=%s, category=%s, difficulty=%s, blocks=%d, studentAccess=%s]",
            id, category, difficulty, allowedBlocks.size(), studentAccessible);
    }
}
