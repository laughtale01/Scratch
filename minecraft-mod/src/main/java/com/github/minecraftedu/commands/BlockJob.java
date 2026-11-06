package com.github.minecraftedu.commands;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 1 つのマスを触る仕事（ブロックを 1 つ置く・入れ物にアイテムを入れる。BuildQueue に並べる）
 *
 * そのマスのチャンクが読み込まれていれば、すぐ触る。まだなら、チケットを付けて読み込みを頼み、
 * 読み込まれるまで次の回へ回す（その場で触ると、メインスレッドが読み込み・地形の生成を待ち、そのあいだ Minecraft が止まるため。
 * ふつうの地形のワールドで、まだ作られていないチャンク 1 つにつき 0.1 秒ほど止まった）。
 * FillJob.MAX_LOAD_WAIT_NANOS 待っても読み込まれなければ、あきらめて onGiveUp を呼ぶ
 */
class BlockJob implements BuildQueue.Job {

    // 読み込みを頼んだチャンクを、しばらく読み込んだままにするチケット（LINGER_TICKS tick たつと Minecraft が外す。こちらでは外さない）
    // 同じあたりに続けて置くとき、1 つごとに読み込み直さないように。読み込みを待っている間は回ごとに付け直す
    // （付け直すと、切れるまでの時間が初めに戻る）。
    // 短めにする（「ずっと、x を増やしながら置く」で、通ったチャンクをたくさん読み込んだままにしないように）
    // tick で数えるので、1 人用のワールドが止まっている（ポーズ中の）間は切れない（止まっている間に読み込んだチャンクは、
    // 動き出してから LINGER_TICKS で外れる。Minecraft がその場で読み込むときのチケットも、同じ数え方）
    // FillJob のチケットとは別の種類にする（FillJob のチケットは 1 回外すと無くなるので、
    // 同じ種類を付け外しすると、「周りをきれいにする」が先に付けたチケットまで外してしまうため）
    private static final int LINGER_TICKS = 100;
    private static final TicketType<ChunkPos> LINGER_TICKET =
        TicketType.create("minecraftedu_block", Comparator.comparingLong(ChunkPos::toLong), LINGER_TICKS);
    // となりを読む置き方で、となりのチャンクも待つ、チャンクの端からの幅（マス）
    // 置いたブロックや入れ物が、比較器へ知らせるとき（チェスト・かまどなど）、Minecraft は 1 マス先が石などなら、
    // 2 マス先を読み込まれているか確かめずに読む（Level.updateNeighbourForOutputSignal）。端から 2 マスまでは、となりも待つ
    // FillJob が読み込みを待つ範囲も同じ幅にする
    // （レッドストーンの粉・ピストン・トリップワイヤー・スポンジ（水を 6 マス先までたどる）・パワードレール（8 本先までたどる）
    //   などは、もっと先まで読む。信号はどこまでも伝わるので、決まった幅では
    //   ふさぎきれない。まだ読み込まれていない遠くのチャンクの端の近くに置いたときだけ、チャンク 1 つ分ほど止まることがある。
    //   まれで短いので受け入れた。広くすると、遠くに建てるたびに地形を作る量が何倍にもなるため）
    static final int NEIGHBOR_MARGIN = 2;

    private final MinecraftServer server;
    // 触る場所を決める（順番が来たときに 1 回だけ呼ぶ。相対座標は、そのときのプレイヤーの位置で決めるため。
    // 読み込みを待つ間にプレイヤーが動いても、場所は変わらない。null ならやめる）
    private final Function<ServerLevel, BlockPos> target;
    // となりのチャンクも読み込みを待つか（となりを読む置き方のとき。マスがチャンクの端から NEIGHBOR_MARGIN マスの中なら、そのとなりも待つ）
    private final boolean neighbors;
    // メインスレッドで、決めた場所を触る
    private final BiConsumer<ServerLevel, BlockPos> action;
    // 読み込まれずにあきらめたときに呼ぶ（メインスレッド）
    private final Runnable onGiveUp;
    // 前もって分かっている場所（前の仕事が読み込みを待っている間に、先に読み込みを頼むのに使う）。
    // 相対座標は、順番が来たときのプレイヤーの位置で決まるので、前もっては分からない（null）
    private final BlockPos prefetchPos;

    // 決めた場所（まだ決めていなければ null）
    private BlockPos pos;
    // 読み込みを待ち始めた時刻（0 は待っていない）
    private long loadWaitStart;

    BlockJob(MinecraftServer server, Function<ServerLevel, BlockPos> target, BlockPos prefetchPos, boolean neighbors,
             BiConsumer<ServerLevel, BlockPos> action, Runnable onGiveUp) {
        this.server = server;
        this.target = target;
        this.prefetchPos = prefetchPos;
        this.neighbors = neighbors;
        this.action = action;
        this.onGiveUp = onGiveUp;
    }

    @Override
    public boolean step(long deadlineNanos) {
        ServerLevel world = server.overworld();
        if (world == null) {
            return true;  // ワールドが無ければ、やめる
        }
        if (pos == null) {
            pos = target.apply(world);
            if (pos == null) {
                return true;  // 場所を決められなかった（理由は target が知らせている）
            }
        }
        // 建てられる高さの外は、Minecraft がチャンクを読まずに断るので、待たない
        if (!world.isOutsideBuildHeight(pos) && !requestLoaded(world, pos, neighbors)) {
            long now = System.nanoTime();
            if (loadWaitStart == 0) {
                loadWaitStart = now;
            }
            if (now - loadWaitStart >= FillJob.MAX_LOAD_WAIT_NANOS) {
                onGiveUp.run();
                return true;
            }
            return false;  // 読み込み中。次の回にまた見る
        }
        action.accept(world, pos);
        return true;
    }

    /**
     * pos のチャンクが読み込まれているか（メインスレッドで呼ぶ）
     * neighbors なら、pos がチャンクの端から NEIGHBOR_MARGIN マスの中にあるとき、その側のとなり（角ならななめも）も見る。
     * 見るチャンクに読み込まれていないものがあれば、見るチャンク全部にチケットを付けて読み込みを頼む
     * （付けてあれば付け直す。先に読み込めたチャンクのチケットが、ほかを待つ間に切れないように）。
     * 全部読み込まれていても、プレイヤーの見える範囲の外なら付け直す（遠くで建て続けている間に、LINGER_TICKS ごとに下ろされて
     * 読み込み直しにならないように）。見える範囲の中なら付けない（近くに置くたびにチケットを付け外ししないように）
     */
    static boolean requestLoaded(ServerLevel world, BlockPos pos, boolean neighbors) {
        return requestLoaded(world, pos, neighbors, outsideView(world, pos.getX() >> 4, pos.getZ() >> 4));
    }

    /**
     * pos のチャンク（neighbors ならとなりも）について、上の requestLoaded と同じ。refreshLoaded なら、全部読み込まれていても付け直す
     */
    private static boolean requestLoaded(ServerLevel world, BlockPos pos, boolean neighbors, boolean refreshLoaded) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        int minX = cx, maxX = cx, minZ = cz, maxZ = cz;
        if (neighbors) {
            minX -= (pos.getX() & 15) < NEIGHBOR_MARGIN ? 1 : 0;
            maxX += (pos.getX() & 15) >= 16 - NEIGHBOR_MARGIN ? 1 : 0;
            minZ -= (pos.getZ() & 15) < NEIGHBOR_MARGIN ? 1 : 0;
            maxZ += (pos.getZ() & 15) >= 16 - NEIGHBOR_MARGIN ? 1 : 0;
        }
        return requestLoaded(world, minX, maxX, minZ, maxZ, refreshLoaded);
    }

    /**
     * チャンクの x minX〜maxX・z minZ〜maxZ が全部読み込まれているか（メインスレッドで呼ぶ）
     * 読み込まれていないものがあれば、全部にこの種類のチケット（時間で切れる）を付けて読み込みを頼む。
     * refreshLoaded なら、全部読み込まれていても付け直す
     * （FillJob も、これから置くチャンクとそのとなりを待つのに使う。となりを読まない置き方では、
     *   BUILD_TICKET を付けないとなりを、このチケットで読み込んだままにする）
     */
    static boolean requestLoaded(ServerLevel world, int minX, int maxX, int minZ, int maxZ, boolean refreshLoaded) {
        boolean ready = true;
        for (int tx = minX; tx <= maxX && ready; tx++) {
            for (int tz = minZ; tz <= maxZ; tz++) {
                if (world.getChunkSource().getChunkNow(tx, tz) == null) {
                    ready = false;
                    break;
                }
            }
        }
        if (!ready || refreshLoaded) {
            for (int tx = minX; tx <= maxX; tx++) {
                for (int tz = minZ; tz <= maxZ; tz++) {
                    ChunkPos chunk = new ChunkPos(tx, tz);
                    world.getChunkSource().addRegionTicket(LINGER_TICKET, chunk, 0, chunk);
                }
            }
        }
        return ready;
    }

    /**
     * チャンクが、どのプレイヤーの見える範囲（Minecraft が読み込んだままにしておく範囲）の外か
     * （端のチャンクは読み込みの段階が浅いことがあるので、端も外として扱う）
     */
    private static boolean outsideView(ServerLevel world, int cx, int cz) {
        int view = world.getServer().getPlayerList().getViewDistance();
        for (ServerPlayer player : world.players()) {
            ChunkPos at = player.chunkPosition();
            if (Math.max(Math.abs(at.x - cx), Math.abs(at.z - cz)) < view) {
                return false;
            }
        }
        return true;
    }

    /**
     * pos のチャンクが読み込まれているか（読み込みは頼まない。メインスレッドで呼ぶ。ほかのスレッドからはいつも false）
     */
    static boolean isLoaded(ServerLevel world, BlockPos pos) {
        return world.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    @Override
    public long weight() {
        return 1;
    }

    @Override
    public boolean waitingForLoad() {
        return loadWaitStart != 0;
    }

    /**
     * 前もって分かっている場所のチャンク（建てられる高さの外は、順番が来ても読み込まないので無し）
     */
    @Override
    public long prefetchChunk() {
        ServerLevel world = server.overworld();
        if (prefetchPos == null || world == null || world.isOutsideBuildHeight(prefetchPos)) {
            return BuildQueue.NO_PREFETCH;
        }
        return ChunkPos.asLong(prefetchPos.getX() >> 4, prefetchPos.getZ() >> 4);
    }

    /**
     * 前もって分かっている場所のチャンク（となりも待つなら、となりも）にチケットを付ける（順番が来たときと同じチケット。LINGER_TICKS で切れる）。
     * 読み込み済みでも付け直す（先頭が長く待つ間に、先読みしたチャンクが切れて下ろされ、読み直しにならないように。
     * 付け直すのは BuildQueue が見る PREFETCH_CHUNKS 個の中だけなので、読み込んだままにするチャンクの数は増えない）
     */
    @Override
    public void prefetch() {
        ServerLevel world = server.overworld();
        if (prefetchChunk() == BuildQueue.NO_PREFETCH || world == null) {
            return;
        }
        requestLoaded(world, prefetchPos, neighbors, true);
    }
}
