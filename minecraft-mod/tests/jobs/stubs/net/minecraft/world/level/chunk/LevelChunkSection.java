package net.minecraft.world.level.chunk;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

// 試験用の偽物（16×16×16 のブロック。はじめは空気）
public class LevelChunkSection {
    private final BlockState[] states = new BlockState[4096];

    public LevelChunkSection() {
        Arrays.fill(states, BlockState.AIR);
    }

    public BlockState getBlockState(int x, int y, int z) {
        return states[(y << 8) | (z << 4) | x];
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        states[(y << 8) | (z << 4) | x] = state;
    }

    public boolean hasOnlyAir() {
        for (BlockState s : states) {
            if (!s.isAir()) {
                return false;
            }
        }
        return true;
    }
}
