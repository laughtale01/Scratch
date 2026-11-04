package com.github.minecraftedu.commands;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

/**
 * 箱の形の範囲にブロックを置いていく仕事（BuildQueue に並べる。時間が来たら途中で止めて、続きから置く）
 *
 * 置く順は、チャンクごと（x → z）、その中を 16 マスの立方体（区画）ごとに下から上へ、区画の中は x → y → z。
 * 区画ごとに置くので、クライアントが同じ区画を描き直す回数がなるべく少なくなる。範囲のいちばん最後のマス（x・y・z が
 * いちばん大きいマス）は、いつも最後に置く。
 * 空気しかない区画に空気を置くときは、その区画を飛ばす。もう同じブロックがあるマスも飛ばす
 * （どちらも置いても何も変わらないため。「周りをきれいにする」や、同じ所を埋め直すときが速くなる）
 *
 * 置いている間は、範囲のチャンクを読み込んだままにする（チケットを付ける）。
 * プレイヤーから遠い所は、Minecraft が付ける一時的なチケットが 1 tick で切れてチャンクが下ろされるので、
 * 途中で止めるたびに読み込み直しになり、ほとんど進まなくなるため（「周りをきれいにする」で起きた）。
 * となりを読む置き方（となりに知らせるフラグ 1 か、となりの形を直すとき＝フラグ 16 が無いとき）は、
 * 範囲がチャンクの端に触れている側のとなりのチャンクにも付ける
 * （端に置いたブロックがとなりを読むので、そこが読み込み直しになると、メインスレッドが読み込みを待って止まるため）。
 * チャンクの読み込みは待たずに次の回へ回す（メインスレッドで読み込みを待つと、そのあいだ止まるため）。
 * 待つのは、これから置くチャンク（とそのとなりのチケットを付けたチャンク）だけ。
 */
class FillJob implements BuildQueue.Job {

    // 置いている間、チャンクを読み込んだままにするチケット（時間で切れない。終わったら外す）
    // 同じチャンクに同じチケットを 2 回付けても 1 つにまとまり、1 回外すと無くなる。
    // いまチケットを同時に持つのは「周りをきれいにする」の列（先の 16 列にも付ける）だけで、どの列もチャンクの境にそろい、
    // となりのチャンクには付けない置き方（フラグ 2|16）なので、重ならない
    // （となりにも付ける置き方にしたり、仕事を並べて進めるようにしたりするときは注意）
    private static final TicketType<ChunkPos> BUILD_TICKET =
        TicketType.create("minecraftedu_build", Comparator.comparingLong(ChunkPos::toLong));

    // 時間を確かめる間隔（ブロックの数）
    private static final int CHECK_EVERY = 256;
    // 1 つのチャンクの読み込みを待つ長さの上限（初めて待ったときから数える。これを過ぎたら、待たずに置く。
    // そのときは読み込みをメインスレッドで待つ）
    private static final long MAX_LOAD_WAIT_NANOS = TimeUnit.SECONDS.toNanos(5);

    private final MinecraftServer server;
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    // 高さ（y）ごとに置くブロック（「ブロックで埋める」はいつも同じ。「周りをきれいにする」は地面の層ごとに変わる）
    private final IntFunction<BlockState> stateForY;
    // Level.setBlock に渡すフラグ
    private final int flags;
    // チケットを付けるチャンクの範囲（範囲のチャンクと、端に触れている側のとなり）
    private final int ticketMinX, ticketMaxX, ticketMinZ, ticketMaxZ;

    // いまの区画（チャンクの x・z と、16 マスの高さの段）
    private int cx, cz, sy;
    // 区画の中の次に置く場所
    private int x, y, z;
    // 区画の途中か
    private boolean inCell;
    // 全部置いたか
    private boolean finished;
    // 置いた数（飛ばした区画のマスも数える）
    private long placed;
    // チケットを付けたワールド（付けていなければ null）
    private ServerLevel ticketLevel;
    // 読み込みがそろったと分かっているチャンク（Long.MIN_VALUE はまだ無い）と、読み込みを待ち始めた時刻（0 は待っていない）
    private long readyChunk = Long.MIN_VALUE;
    private long loadWaitStart;
    // いまの区画が入っているチャンクの段（置く前に、そのマスにいまあるブロックを見て、同じなら置かないため。
    // 区画に入るときに取り、区画を出たら null にする。チャンクが読み込まれていないときも null）
    private LevelChunkSection cellSection;

    FillJob(MinecraftServer server, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            IntFunction<BlockState> stateForY, int flags) {
        this.server = server;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.stateForY = stateForY;
        this.flags = flags;
        // となりを読む置き方か（フラグ 1: となりに知らせる。フラグ 16 が無い: となりの形を直す）
        boolean notifies = (flags & 1) != 0 || (flags & 16) == 0;
        this.ticketMinX = (minX >> 4) - (notifies && (minX & 15) == 0 ? 1 : 0);
        this.ticketMaxX = (maxX >> 4) + (notifies && (maxX & 15) == 15 ? 1 : 0);
        this.ticketMinZ = (minZ >> 4) - (notifies && (minZ & 15) == 0 ? 1 : 0);
        this.ticketMaxZ = (maxZ >> 4) + (notifies && (maxZ & 15) == 15 ? 1 : 0);
        this.cx = minX >> 4;
        this.cz = minZ >> 4;
        this.sy = minY >> 4;
    }

    @Override
    public boolean step(long deadlineNanos) {
        ServerLevel world = server.overworld();
        if (world == null) {
            removeTickets();
            return true;  // ワールドが無ければ、やめる
        }
        try {
            prepare(world);
            int count = 0;
            while (!finished) {
                if (!inCell) {
                    if (!chunkReady(world)) {
                        return false;  // これから置くチャンクがまだ読み込み中。次の回にまた見る
                    }
                    if (canSkipCell(world)) {
                        placed += (long) (cellX1() - cellX0() + 1) * (cellY1() - cellY0() + 1) * (cellZ1() - cellZ0() + 1);
                        nextCell();
                    } else {
                        x = cellX0();
                        y = cellY0();
                        z = cellZ0();
                        cellSection = sectionNow(world);
                        inCell = true;
                    }
                } else {
                    BlockState state = stateForY.apply(y);
                    // もう同じブロックがあるマスは置かない（置いても Minecraft は何もしないので、結果は同じ）
                    if (cellSection == null || cellSection.getBlockState(x & 15, y & 15, z & 15) != state) {
                        world.setBlock(new BlockPos(x, y, z), state, flags);
                    }
                    placed++;
                    // 区画の中の次の場所へ（z → y → x の順にくり上げる）
                    if (++z > cellZ1()) {
                        z = cellZ0();
                        if (++y > cellY1()) {
                            y = cellY0();
                            if (++x > cellX1()) {
                                inCell = false;
                                cellSection = null;
                                nextCell();
                            }
                        }
                    }
                }
                if (++count % CHECK_EVERY == 0 && System.nanoTime() - deadlineNanos >= 0 && !finished) {
                    return false;  // 時間が来たので、次はここから
                }
            }
        } catch (RuntimeException e) {
            // この仕事はやめになるので、チケットを外してから知らせる
            removeTickets();
            throw e;
        }
        removeTickets();
        return true;
    }

    /**
     * チケットを付ける（まだなら）。先に付けておくと、置く順番が来る前にチャンクの読み込みが進む
     * （「周りをきれいにする」は、次のいくつかの列の分を先に呼ぶ）
     */
    void prepare(ServerLevel world) {
        if (ticketLevel != null || finished) {
            return;
        }
        ticketLevel = world;
        for (int tx = ticketMinX; tx <= ticketMaxX; tx++) {
            for (int tz = ticketMinZ; tz <= ticketMaxZ; tz++) {
                ChunkPos pos = new ChunkPos(tx, tz);
                world.getChunkSource().addRegionTicket(BUILD_TICKET, pos, 0, pos);
            }
        }
    }

    /**
     * 付けたチケットを外す（付けていなければ何もしない）
     */
    void removeTickets() {
        if (ticketLevel == null) {
            return;
        }
        for (int tx = ticketMinX; tx <= ticketMaxX; tx++) {
            for (int tz = ticketMinZ; tz <= ticketMaxZ; tz++) {
                ChunkPos pos = new ChunkPos(tx, tz);
                ticketLevel.getChunkSource().removeRegionTicket(BUILD_TICKET, pos, 0, pos);
            }
        }
        ticketLevel = null;
    }

    /**
     * これから置くチャンクと、そのとなりのチケットを付けたチャンクが読み込まれたか
     * 読み込み中なら false。ただし、そのチャンクを初めて待ってから MAX_LOAD_WAIT_NANOS を過ぎたら true
     * （そのあとは setBlock がメインスレッドで読み込みを待つ）
     */
    private boolean chunkReady(ServerLevel world) {
        long key = ChunkPos.asLong(cx, cz);
        if (readyChunk == key) {
            return true;
        }
        boolean ready = true;
        for (int tx = Math.max(ticketMinX, cx - 1); tx <= Math.min(ticketMaxX, cx + 1) && ready; tx++) {
            for (int tz = Math.max(ticketMinZ, cz - 1); tz <= Math.min(ticketMaxZ, cz + 1); tz++) {
                if (world.getChunkSource().getChunkNow(tx, tz) == null) {
                    ready = false;
                    break;
                }
            }
        }
        if (!ready) {
            long now = System.nanoTime();
            if (loadWaitStart == 0) {
                loadWaitStart = now;
            }
            if (now - loadWaitStart < MAX_LOAD_WAIT_NANOS) {
                return false;
            }
        }
        readyChunk = key;
        loadWaitStart = 0;
        return true;
    }

    /**
     * いまの区画が入っているチャンクの段（チャンクが読み込まれていなければ null）
     */
    private LevelChunkSection sectionNow(ServerLevel world) {
        LevelChunk chunk = world.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) {
            return null;
        }
        int index = chunk.getSectionIndexFromSectionY(sy);
        if (index < 0 || index >= chunk.getSectionsCount()) {
            return null;
        }
        return chunk.getSection(index);
    }

    /**
     * いまの区画を飛ばしてよいか（置くのが全部空気で、区画にもともと空気しかないとき）
     * （洞窟の空気なども空気として扱う。Minecraft の hasOnlyAir と同じ）
     */
    private boolean canSkipCell(ServerLevel world) {
        for (int yy = cellY0(); yy <= cellY1(); yy++) {
            if (!stateForY.apply(yy).isAir()) {
                return false;
            }
        }
        LevelChunkSection section = sectionNow(world);
        return section != null && section.hasOnlyAir();
    }

    /**
     * 次の区画へ（高さの段 → z → x の順にくり上げる。全部終わったら finished）
     */
    private void nextCell() {
        if (++sy > (maxY >> 4)) {
            sy = minY >> 4;
            if (++cz > (maxZ >> 4)) {
                cz = minZ >> 4;
                if (++cx > (maxX >> 4)) {
                    finished = true;
                }
            }
        }
    }

    // いまの区画の、範囲の中の端
    private int cellX0() {
        return Math.max(minX, cx << 4);
    }

    private int cellX1() {
        return Math.min(maxX, (cx << 4) + 15);
    }

    private int cellY0() {
        return Math.max(minY, sy << 4);
    }

    private int cellY1() {
        return Math.min(maxY, (sy << 4) + 15);
    }

    private int cellZ0() {
        return Math.max(minZ, cz << 4);
    }

    private int cellZ1() {
        return Math.min(maxZ, (cz << 4) + 15);
    }

    @Override
    public long weight() {
        return ((long) maxX - minX + 1) * ((long) maxY - minY + 1) * ((long) maxZ - minZ + 1);
    }

    /**
     * 置いた数
     */
    long placed() {
        return placed;
    }
}
