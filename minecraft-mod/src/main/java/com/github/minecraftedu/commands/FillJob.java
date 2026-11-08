package com.github.minecraftedu.commands;

import com.github.minecraftedu.MinecraftEduMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
 * 範囲がチャンクの端から 2 マスの中に触れている側のとなりのチャンクも、読み込まれるのを待つ
 * （端の近くで置いたり消したりしたブロックがとなりを読むので、そこが読み込まれていないと、メインスレッドが読み込みを待って止まるため。
 *   チェストなどは、比較器へ知らせるときに 2 マス先まで読む。消すときは、どのフラグでも読む）。
 * となりを読む置き方（となりに知らせるフラグ 1 か、となりの形を直すとき＝フラグ 16 が無いとき）は、となりにも同じチケットを付ける。
 * そうでない置き方（「周りをきれいにする」のフラグ 2|16）は、となりには時間で切れる別のチケット（BlockJob と同じ）を付けて待つ
 * （となりは、となりの列の範囲でもあり、その列が置くときに読み込んだままにするので、ここではずっと持たなくてよいため）。
 * チャンクの読み込みは待たずに次の回へ回す（メインスレッドで読み込みを待つと、そのあいだ止まるため）。
 * 待つのは、これから置くチャンクとそのとなりだけ。
 */
class FillJob implements BuildQueue.Job {

    // 置いている間、チャンクを読み込んだままにするチケット（時間で切れない。終わったら外す）
    // チケットの値は仕事ごとの番号（ticketKey）。同じチャンクに同じ値のチケットを 2 回付けると 1 つにまとまり、1 回外すと無くなるので、
    // 仕事ごとに値を変えて、ほかの仕事が同じチャンクに付けたチケットを外さないようにする
    // （止めるボタンですぐ同じ所を埋め直したとき、古い仕事の片づけが新しい仕事のチケットを外していた）
    private static final TicketType<Long> BUILD_TICKET =
        TicketType.create("minecraftedu_build", Long::compare);
    // 次に作る仕事のチケットの番号
    private static final AtomicLong NEXT_TICKET_KEY = new AtomicLong();

    // 時間を確かめる間隔（ブロックの数）
    private static final int CHECK_EVERY = 256;
    // 1 つのチャンクの読み込みを待つ長さの上限（初めて待ったときから数える。1 人用のワールドが止まっている（ポーズ中の）間は
    // 数えず、動き出してから数え直す。これを過ぎたら、あきらめて仕事をやめる。
    // 前は 5 秒で待つのをやめて置いていたが、そのときはメインスレッドが読み込み・地形の生成を待ち、そのあいだ止まっていた）
    // 1 つのマスを触る仕事（BlockJob）も、この長さまで待つ。
    // 本番はいつも 30 秒（試験 tests/jobs が、あきらめる道を短い時間で通すために書きかえる。パッケージの中だけ）
    static volatile long maxLoadWaitNanos = TimeUnit.SECONDS.toNanos(30);

    private final MinecraftServer server;
    private final int minX, minY, minZ, maxX, maxY, maxZ;
    // 高さ（y）ごとに置くブロック（「ブロックで埋める」はいつも同じ。「周りをきれいにする」は地面の層ごとに変わる）
    private final IntFunction<BlockState> stateForY;
    // Level.setBlock に渡すフラグ
    private final int flags;
    // 読み込まれるのを待つチャンクの範囲（範囲のチャンクと、端から 2 マスの中に触れている側のとなり）
    private final int waitMinX, waitMaxX, waitMinZ, waitMaxZ;
    // BUILD_TICKET を付けるチャンクの範囲（となりを読む置き方なら待つ範囲と同じ。そうでなければ範囲のチャンクだけ）
    private final int ticketMinX, ticketMaxX, ticketMinZ, ticketMaxZ;
    // となりを読む置き方か
    private final boolean notifies;

    // いまの区画（チャンクの x・z と、16 マスの高さの段）
    private int cx, cz, sy;
    // 区画の中の次に置く場所
    private int x, y, z;
    // 区画の途中か
    private boolean inCell;
    // 一時停止から再開した区画の途中で、次に置く前に、読み込みを確かめ直して区画の段を取り直すか（resumed で立てる）
    private boolean recheckCell;
    // 全部置いたか
    private boolean finished;
    // チャンクが読み込まれず、あきらめたか
    private boolean gaveUp;
    // 置いた数（飛ばした区画のマスも数える）
    private long placed;
    // チケットを付けたワールド（付けていなければ null）
    private ServerLevel ticketLevel;
    // この仕事のチケットの値（ほかの仕事と同じにならない）
    private final Long ticketKey = NEXT_TICKET_KEY.getAndIncrement();
    // 読み込みがそろったと分かっているチャンク（Long.MIN_VALUE はまだ無い）と、読み込みを待ち始めた時刻（0 は待っていない）
    private long readyChunk = Long.MIN_VALUE;
    private long loadWaitStart;
    // 読み込みを待っているかを最後に見たときの PauseWatch.holds（変わっていたら、あきらめるまでの時間を数え直す）
    private long seenHolds;
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
        this.notifies = (flags & 1) != 0 || (flags & 16) == 0;
        // 端から BlockJob.NEIGHBOR_MARGIN マスの中に触れていれば、その側のとなりも待つ
        // （チェストなどを置いたり消したりすると、比較器へ知らせるときに 2 マス先まで読むため）
        int margin = BlockJob.NEIGHBOR_MARGIN;
        this.waitMinX = (minX >> 4) - ((minX & 15) < margin ? 1 : 0);
        this.waitMaxX = (maxX >> 4) + ((maxX & 15) >= 16 - margin ? 1 : 0);
        this.waitMinZ = (minZ >> 4) - ((minZ & 15) < margin ? 1 : 0);
        this.waitMaxZ = (maxZ >> 4) + ((maxZ & 15) >= 16 - margin ? 1 : 0);
        this.ticketMinX = notifies ? waitMinX : minX >> 4;
        this.ticketMaxX = notifies ? waitMaxX : maxX >> 4;
        this.ticketMinZ = notifies ? waitMinZ : minZ >> 4;
        this.ticketMaxZ = notifies ? waitMaxZ : maxZ >> 4;
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
            if (ticketLevel == null && !finished) {
                // チケットを付けられなかった（止まっていて、範囲のまだ読み込まれていないチャンクが PauseWatch の数に収まらない）。動き出すまで置かない
                // （チケット無しで置き始めると、動き出した直後にチャンクが下ろされて、メインスレッドで読み直すことがあるため）
                return false;
            }
            int count = 0;
            while (!finished) {
                if (!inCell || recheckCell) {
                    if (!chunkReady(world)) {
                        if (gaveUp) {
                            break;  // 読み込まれなかった。ここでやめる（チケットは下で外す）
                        }
                        return false;  // これから置くチャンクがまだ読み込み中。次の回にまた見る
                    }
                    if (recheckCell) {
                        // 一時停止から再開した区画の途中。となりも入れて読み込みを確かめ直したので、止めたマスから続ける
                        // （区画のチャンク自身は BUILD_TICKET で持っているので下ろされないが、区画の段は念のため取り直す）
                        recheckCell = false;
                        cellSection = sectionNow(world);
                        continue;
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
        // 1 人用のワールドが止まっている（ポーズ中）間は、範囲のまだ読み込まれていないチャンクが PauseWatch の数に収まるときだけ付ける
        // （止まっている間はチャンクが下ろされないので、たまってメモリを使い切らないように。収まらなければ、動き出してから付ける）。
        // 全部読み込まれていれば、いつも付ける（新しく読み込むものは無いのでたまらない。近くを埋める命令は、止まっている間も進む）
        if (PauseWatch.paused()
            && !PauseWatch.withinBudget(world, ticketMinX, ticketMaxX, ticketMinZ, ticketMaxZ)) {
            return;
        }
        ticketLevel = world;
        for (int tx = ticketMinX; tx <= ticketMaxX; tx++) {
            for (int tz = ticketMinZ; tz <= ticketMaxZ; tz++) {
                ChunkPos pos = new ChunkPos(tx, tz);
                world.getChunkSource().addRegionTicket(BUILD_TICKET, pos, 0, ticketKey);
            }
        }
    }

    /**
     * となりを読まない置き方で、待つ範囲のチャンク（範囲のチャンクと、範囲が端から 2 マスの中に触れている側のとなり）に
     * 時間で切れるチケット（BlockJob）を付けて、先に読み込みを頼む（範囲のチャンクには BUILD_TICKET もあるので、こちらは、
     * となりのためのもの）
     * （付けてあれば付け直す。「周りをきれいにする」が、prepare と一緒に、この先の列の分を回ごとに呼ぶ。
     *   呼ばないと、列ごとにとなりの読み込みを待つので、ずっと遅くなる）
     */
    void prefetchNeighbors(ServerLevel world) {
        if (notifies || finished) {
            return;
        }
        BlockJob.requestLoaded(world, waitMinX, waitMaxX, waitMinZ, waitMaxZ, true);
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
                ticketLevel.getChunkSource().removeRegionTicket(BUILD_TICKET, pos, 0, ticketKey);
            }
        }
        ticketLevel = null;
    }

    /**
     * これから置くチャンクと、待つ範囲の中のそのとなりのチャンクが読み込まれたか
     * 読み込み中なら false。そのチャンクを初めて待ってから maxLoadWaitNanos を過ぎたら、gaveUp を立てて false
     * （止まっている間は数えない）
     * （待たずに置くと、メインスレッドが読み込み・地形の生成を待って Minecraft が止まるため）
     * 1 つでも読み込まれていなければ、見たチャンク全部に、時間で切れるチケット（BlockJob）も付けて読み込みを頼む。
     * となりを読まない置き方は、となりに BUILD_TICKET が無いので、区画に入るたびに時間で切れるチケットを付け直す
     * （置いている途中で切れて、となりが下ろされないように）
     */
    private boolean chunkReady(ServerLevel world) {
        long key = ChunkPos.asLong(cx, cz);
        if (readyChunk == key && notifies) {
            return true;
        }
        boolean ready = BlockJob.requestLoaded(world, Math.max(waitMinX, cx - 1), Math.min(waitMaxX, cx + 1),
            Math.max(waitMinZ, cz - 1), Math.min(waitMaxZ, cz + 1), !notifies);
        if (!ready) {
            long now = System.nanoTime();
            // 止まっている間と、前に見たあとで列がまるごと待たされていた（PauseWatch.overLimit）ときは、
            // あきらめるまでの時間に数えない（動き出してから数え直す。止まっていた時間を数えて、動き出してすぐあきらめないように）
            boolean held = PauseWatch.holds() != seenHolds;
            seenHolds = PauseWatch.holds();
            if (PauseWatch.paused() || held) {
                loadWaitStart = 0;
            }
            if (PauseWatch.paused()) {
                return false;
            }
            if (loadWaitStart == 0) {
                loadWaitStart = now;
            }
            if (now - loadWaitStart >= maxLoadWaitNanos) {
                gaveUp = true;
            }
            return false;
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
     * 一時停止から再開した（BuildQueue がメインスレッドで、再開してから初めて進める前に呼ぶ。止めた状態をメインスレッドが
     * 見ていたときだけ。「周りをきれいにする」・「埋める」の包みからも、この FillJob の分を呼ぶ）。
     * 止めていた間を、チャンクの読み込みをあきらめるまでの時間に数えないよう、待ち始めた時刻を忘れる。
     * 区画の途中なら、止めたマスから続けるが、次に置く前に、区画に入るときと同じ読み込みの確かめ（となりも）を通し、
     * 区画の段を取り直す（区画の途中は読み込みを確かめずに置くが、止めている間も Minecraft は動いているので、
     * となりを読まない置き方では、となりのチャンクの時間で切れるチケットが切れて下ろされていることがある）。
     * 区画を始めから置き直すことはしない（止めている間に落ちた砂や、子ども・ほかのタブが変えたマスを、置き直して変えないように）
     */
    @Override
    public void resumed() {
        loadWaitStart = 0;
        readyChunk = Long.MIN_VALUE;
        if (inCell) {
            recheckCell = true;
            // 区画の途中から続けることを debug に残す（stop_check.py の 10 節が、この道を通ったかを見る）
            MinecraftEduMod.LOGGER.debug("FillJob resumed mid-cell at " + x + "," + y + "," + z);
        }
    }

    /**
     * 置いた数
     */
    long placed() {
        return placed;
    }

    /**
     * チャンクが読み込まれず、途中でやめたか（step が true を返したあとに見る）
     */
    boolean gaveUp() {
        return gaveUp;
    }
}
