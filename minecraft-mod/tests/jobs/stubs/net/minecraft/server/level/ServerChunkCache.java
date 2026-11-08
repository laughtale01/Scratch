package net.minecraft.server.level;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

// 試験用の偽物（チャンクの読み込みとチケット）。
// チケットは本物と同じく（種類・チャンク・段・値）でまとまる: 同じものを 2 回付けても 1 つ、1 回外すと無くなる
public class ServerChunkCache {
    // 読み込まれたチャンク
    private final Map<Long, LevelChunk> loaded = new HashMap<>();
    // 読み込みを頼まれたが、まだ読み込んでいないチャンク（autoLoad が false のとき。loadPending で読み込む）
    private final Set<Long> pending = new HashSet<>();
    // 何度頼まれても読み込まないチャンク
    public final Set<Long> blocked = new HashSet<>();
    // チケットが付いたら、すぐ読み込むか
    public boolean autoLoad = true;
    // いま付いているチケット
    private final Set<Ticket> tickets = new HashSet<>();
    // チケットを付けた回数（種類の名前ごと）
    public final Map<String, Integer> addCalls = new HashMap<>();

    public record Ticket(String type, long chunk, int level, Object value) {
    }

    public LevelChunk getChunkNow(int x, int z) {
        return loaded.get(ChunkPos.asLong(x, z));
    }

    public <T> void addRegionTicket(TicketType<T> type, ChunkPos pos, int level, T value) {
        tickets.add(new Ticket(type.name, pos.toLong(), level, value));
        addCalls.merge(type.name, 1, Integer::sum);
        long key = pos.toLong();
        if (!loaded.containsKey(key) && !blocked.contains(key)) {
            if (autoLoad) {
                load(pos.x, pos.z);
            } else {
                pending.add(key);
            }
        }
    }

    public <T> void removeRegionTicket(TicketType<T> type, ChunkPos pos, int level, T value) {
        tickets.remove(new Ticket(type.name, pos.toLong(), level, value));
    }

    /**
     * チャンクを読み込む（はじめから読み込まれていることにするときも使う）
     */
    public LevelChunk load(int x, int z) {
        return loaded.computeIfAbsent(ChunkPos.asLong(x, z), k -> new LevelChunk());
    }

    /**
     * チャンクを下ろす（止めている間に下ろされたことにする）
     */
    public void unload(int x, int z) {
        loaded.remove(ChunkPos.asLong(x, z));
    }

    /**
     * 頼まれていたチャンクを全部読み込む
     */
    public void loadPending() {
        for (long key : new ArrayList<>(pending)) {
            load((int) key, (int) (key >> 32));
        }
        pending.clear();
    }

    /**
     * いま付いている、この種類のチケットのチャンク（重なりは 1 つ）
     */
    public Set<Long> ticketChunks(String type) {
        Set<Long> out = new HashSet<>();
        for (Ticket t : tickets) {
            if (t.type().equals(type)) {
                out.add(t.chunk());
            }
        }
        return out;
    }

    /**
     * いま付いている、この種類のチケット（値も入れて）
     */
    public List<Ticket> tickets(String type) {
        List<Ticket> out = new ArrayList<>();
        for (Ticket t : tickets) {
            if (Objects.equals(t.type(), type)) {
                out.add(t);
            }
        }
        return out;
    }
}
