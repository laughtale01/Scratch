package net.minecraft.server.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 試験用の偽物（ブロックはチャンクの段に書く。高さは -64〜319）
public class ServerLevel {
    private final MinecraftServer server;
    private final ServerChunkCache chunkSource = new ServerChunkCache();
    private final List<ServerPlayer> players = new ArrayList<>();
    // はじめの場所（試験が書きかえる）
    public final LevelData levelData = new LevelData();
    // setBlock を呼んだ回数（場所ごと）と、呼んだ順
    public final Map<BlockPos, Integer> setCount = new HashMap<>();
    public final List<BlockPos> setOrder = new ArrayList<>();
    // 読み込まれていないチャンクに置こうとした回数（本物では、メインスレッドが読み込みを待って止まる）
    public int setOnUnloaded;
    // ここに置こうとしたら例外を投げる（null なら投げない）
    public BlockPos throwAt;

    public ServerLevel(MinecraftServer server) {
        this.server = server;
    }

    public ServerChunkCache getChunkSource() {
        return chunkSource;
    }

    public MinecraftServer getServer() {
        return server;
    }

    public List<ServerPlayer> players() {
        return players;
    }

    public LevelData getLevelData() {
        return levelData;
    }

    public boolean isOutsideBuildHeight(BlockPos pos) {
        return pos.getY() < -64 || pos.getY() >= 320;
    }

    public boolean setBlock(BlockPos pos, BlockState state, int flags) {
        if (pos.equals(throwAt)) {
            throw new IllegalStateException("試験で投げる例外（setBlock）");
        }
        setCount.merge(pos, 1, Integer::sum);
        setOrder.add(pos);
        LevelChunk chunk = chunkSource.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk == null) {
            setOnUnloaded++;
            return false;
        }
        chunk.set(pos.getX(), pos.getY(), pos.getZ(), state);
        return true;
    }

    /**
     * いまのブロック（読み込まれていなければ null）
     */
    public BlockState get(int x, int y, int z) {
        LevelChunk chunk = chunkSource.getChunkNow(x >> 4, z >> 4);
        return chunk == null ? null : chunk.get(x, y, z);
    }

    /**
     * 試験の準備で、置いたことにせずにブロックを書く
     */
    public void put(int x, int y, int z, BlockState state) {
        chunkSource.load(x >> 4, z >> 4).set(x, y, z, state);
    }
}
