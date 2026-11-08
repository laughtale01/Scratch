package net.minecraft.world.level.chunk;

import net.minecraft.world.level.block.state.BlockState;

// 試験用の偽物（高さ -64〜319 の 24 段）
public class LevelChunk {
    private static final int MIN_SECTION = -4;
    private final LevelChunkSection[] sections = new LevelChunkSection[24];

    public LevelChunk() {
        for (int i = 0; i < sections.length; i++) {
            sections[i] = new LevelChunkSection();
        }
    }

    public int getSectionIndexFromSectionY(int sectionY) {
        return sectionY - MIN_SECTION;
    }

    public int getSectionsCount() {
        return sections.length;
    }

    public LevelChunkSection getSection(int index) {
        return sections[index];
    }

    public BlockState get(int x, int y, int z) {
        return sections[(y >> 4) - MIN_SECTION].getBlockState(x & 15, y & 15, z & 15);
    }

    public void set(int x, int y, int z, BlockState state) {
        sections[(y >> 4) - MIN_SECTION].setBlockState(x & 15, y & 15, z & 15, state);
    }
}
