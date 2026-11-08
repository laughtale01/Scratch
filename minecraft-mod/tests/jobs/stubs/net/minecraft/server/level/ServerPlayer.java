package net.minecraft.server.level;

import net.minecraft.world.level.ChunkPos;

// 試験用の偽物（いる場所の x・z と、そのチャンク）
public class ServerPlayer {
    private final double x;
    private final double z;

    public ServerPlayer(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public ChunkPos chunkPosition() {
        return new ChunkPos((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
    }
}
