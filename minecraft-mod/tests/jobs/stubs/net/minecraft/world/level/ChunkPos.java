package net.minecraft.world.level;

// 試験用の偽物（チャンクの場所。asLong は本物と同じ詰め方）
public final class ChunkPos {
    public final int x, z;

    public ChunkPos(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public static long asLong(int x, int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    public long toLong() {
        return asLong(x, z);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChunkPos p && p.x == x && p.z == z;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(toLong());
    }
}
