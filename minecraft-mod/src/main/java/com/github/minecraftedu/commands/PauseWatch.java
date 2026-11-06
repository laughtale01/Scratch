package com.github.minecraftedu.commands;

import com.github.minecraftedu.MinecraftEduMod;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.concurrent.TimeUnit;

/**
 * 1 人用のワールドが止まっている（ポーズ中）かを見て、止まっている間に新しく読み込むチャンクの数を抑える
 * （Esc のポーズ画面や、ブラウザの Scratch をクリックしたときに止まる）
 *
 * 止まっている間は、サーバーの tick が進まない（Minecraft は tickPaused だけを呼び、tick の知らせも出さない）。
 * 一方で、命令の列（server.execute）とチャンクの読み込みは進む。チケットは tick で数えるので切れず、チャンクも下ろされない。
 * そのため、止まっている間に遠くへ置き続けたりテレポートし続けたりすると、読み込んだチャンクがたまってメモリを使い切る
 * （ふつうの地形で、遠くの 600 チャンクに置くと 770 MB ほど、遠くへテレポートすると 1 回で 1500 チャンク・190 MB ほど増えた）。
 * そこで、止まっている間に新しく頼めるチャンクを PAUSED_CHUNK_BUDGET 個分まで（離れた場所は重いので多めに数える）、
 * テレポートを 1 回までにする。
 * 頼んだものは、最後に頼んでから（チケットを付け直したときも入れて）動いている tick が SETTLE_TICKS 回より多く進むまで数えたままにする
 * （その間にチケットが切れて下ろされるので。動き出してすぐ数え直すと、止める・動かすを短くくり返したときに、
 *   下ろされる前に次の分を頼んでたまるため）。
 * そのうえ、ピストンなど、Minecraft が置いたブロックの周りを読むときは、この数を通らずにチャンクを読み込むので、
 * Minecraft が持つチャンクの数そのものが、止まる前より決まった数より増えたときと、
 * 長く残るものの置き場（Old Gen）がその上限に近づいたときは、
 * 命令の列をまるごと動き出すまで待たせる（overLimit）。
 * MinecraftEduMod で Forge のイベントバスにつなぐ。ここの値は、どれもサーバーのスレッドだけで使う
 */
public final class PauseWatch {

    // サーバーの tick がこの時間動かなければ、止まっていると見る（画面側の「止まっているか」を読めないときのため）
    private static final long PAUSE_DETECT_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    // 止まっている間に、まだ読み込まれていないチャンクの読み込みを新しく頼める数（離れた場所は ISOLATED_EXTRA を足して数える）。
    // 0 にはしない（止まっている間も、遠くを 1 か所読む・テレポートした先に建てる、などはできるように）。
    // となり合ったチャンクで 64 個はおよそ 90 MB（ふつうの地形で、1 チャンクおよそ 1.5 MB）
    private static final int PAUSED_CHUNK_BUDGET = 64;
    // 離れた場所（範囲とその辺で接するチャンクに、読み込み済みのチャンクも、頼んだチャンクも無い）に頼むとき、1 回ごとに足して数える数。
    // Minecraft は、チャンクを作るとき、周りのチャンクも作りかけにする（辺で接していれば多く分け合える。ななめだけでは少ない）。
    // ふつうの地形で、1024 マスずつ離した 64 か所に置くとメモリが 850 MB ほど増え、1 か所あたり、となり合ったときの 9 倍ほどだった
    private static final int ISOLATED_EXTRA = 8;
    // 止まっている間に頼んだもの（チャンク・テレポート）を数えたままにする、動いている tick の数（これより多く進んだら外す）。
    // BlockJob のチケットが切れる 100 tick に、下ろすのにかかる分を足した長さ（およそ 7 秒）
    private static final long SETTLE_TICKS = 140;
    // テレポートした先で、Minecraft が持つチャンク（作りかけも入れる）の、見える範囲の外の幅（チャンク）。
    // 描画距離 16 で、遠くへ 1 回テレポートすると 3721 個 = 61×61 増えた（見える範囲 16 の外に 14）
    private static final int VIEW_SHELL = 14;
    // 止まっている間に増えてよいチャンクの数のうち、予算の分（いちばん重い、離れた場所 7 か所。ふつうの地形で 6 か所に置くと
    // 3750 個（1 か所およそ 625 個）増えたので、7 か所でおよそ 4400 個）に、余裕を足した数。テレポート 1 回分は、見える範囲の広さから別に足す
    private static final int BUDGET_HOLDERS = 5000;
    // 止まっている間に、長く残るものの置き場（Old Gen）が、その上限のこの割合を超えたら、命令の列をまるごと動き出すまで待たせる
    // （チャンクの数は、作りかけのチャンクが本物のチャンクに変わっても増えないので、数だけではメモリを抑えきれないため。最後の守り。
    //   チャンクが増えたかどうかは見ない。止まる前に作りかけの範囲を広げておき、止まってからその内側を本物にすると、数が増えないため）
    private static final double PAUSED_HEAP_LIMIT = 0.75;
    // 止まっている間にテレポートを通すかを決める、メモリの見積もり（行き先はあとから読み込まれて止められないので、
    // 今の使用量にテレポート 2 回分と予算の分を足しても PAUSED_HEAP_LIMIT に収まるときだけ通す。2 回分は、止まっている間の 1 回と、
    // 止まる直前に動いていて、止まったあとに読み込まれる 1 回。止まる直前に始めた大きな「埋める」の分も、この余白と残りの 25% で収める）。
    // テレポート 1 回分は、チャンクの持ち主 1 個あたりの量 × 見える範囲とその周りの数（描画距離 16 で 3721 個・190 MB ほど増えた。
    // 描画距離が大きいと 2 乗で増えるので、描画距離から数える）。予算の分は、となり合った 56 個で 82 MB、離れた 7 か所で 84 MB だったのに余裕を足した量
    private static final long TELEPORT_BYTES_PER_HOLDER = 54_000;
    private static final long BUDGET_BYTES = 100L * 1024 * 1024;
    // 長く残るものの置き場（G1 なら "G1 Old Gen"、Parallel は "PS Old Gen"、Serial は "Tenured Gen"）。
    // Parallel・Serial は、大きな片づけ（full GC）まで要らなくなったものも残るので、早めに待つことがある。
    // 読み込んだチャンクは、若い側の GC を何回か生き残ると、ここへ移る（止まっている間も画面を描くので、若い側の GC は数秒ごとに来る）。
    // ヒープ全体の使用量には、画面を描くたびに出る、まだ片づけていないごみが入り、上限の近くまで上がるので使わない。
    // ここにも、移ってから要らなくなったものは入るが、G1 はふつう半分ほどで片づけ始めるので、75% を超えるのは本当にメモリが足りないとき
    // （ZGC・Shenandoah など、見つからない GC のときは、ヒープ全体の使用量で見る。そのときは早めに待つことがある）
    private static final MemoryPoolMXBean OLD_GEN = ManagementFactory.getMemoryPoolMXBeans().stream()
        .filter(pool -> pool.getType() == MemoryType.HEAP
            && (pool.getName().contains("Old Gen") || pool.getName().contains("Tenured")))
        .findFirst().orElse(null);
    // tick と tick の間がこれより長くあいたら、止まっていた（か、とても遅れた）と見て、そのあと SETTLE_TICKS の間は、
    // overLimit の基準の数を上げない（止まっている間に読み込んで、まだ下ろされていないチャンクを、基準に入れないため。
    // 遅い PC で遅れが続く間は延び続け、基準は下がる方向にしか動かない。そのときは早めに待つだけで困らない）
    private static final long STALL_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
    // 1 人用のワールドを開ける側（画面のある Minecraft）で動いているか（専用サーバーでは画面のクラスを読まない）
    private static final boolean CLIENT = FMLEnvironment.dist.isClient();

    // tick が最後に終わった時刻（System.nanoTime の値）と、ここまでに終わった tick の数（止まっている間は増えない）
    private static long lastTickNanos;
    private static long ticks;
    // overLimit の基準の数。最後に動いた tick が終わったときに Minecraft が持っていたチャンクの数（作りかけも入れる。まだ無ければ -1）。
    // ただし止まったあと ticks が baseHoldUntil になるまでは、それより大きくしない
    private static int runningChunkCount = -1;
    private static long baseHoldUntil;
    // 止まっている間に読み込みを頼んだチャンク（ChunkPos.asLong の値）と、最後に頼んだときの ticks
    private static final Long2LongMap requestedChunks = new Long2LongOpenHashMap();
    // 止まっている間に、離れた場所に頼んだもの（その場所のチャンクの 1 つを代わりにして、最後に頼んだときの ticks。
    // 1 つごとに ISOLATED_EXTRA 個分と数える）と、その場所のチャンクごとの代わりのチャンク
    private static final Long2LongMap isolatedRequests = new Long2LongOpenHashMap();
    private static final Long2LongMap isolatedOwner = new Long2LongOpenHashMap();
    // 止まっている間にテレポートを通したときの ticks（通していなければ hasTeleported が false）
    private static long teleportTick;
    private static boolean hasTeleported;
    // この一時停止で、列を待たせたことをもうログに出したか（動いた tick が終わったら戻す）
    private static boolean holdLogged;
    // readMemory が最後に見た使用量と上限（ログに出すため）
    private static long memoryUsed;
    private static long memoryMax;
    // overLimit で列をまるごと待たせた回数（BlockJob・FillJob が、待たされていた間をあきらめるまでの時間に数えないように見る）
    private static long holds;

    private PauseWatch() {
    }

    /**
     * tick が終わるたびに、時刻と数と、Minecraft が持つチャンクの数を覚える
     * （命令の列は tick と tick の間に進むので、1 tick が長くても、そのころには新しい時刻になっている）
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            long now = System.nanoTime();
            if (lastTickNanos != 0 && now - lastTickNanos > STALL_NANOS) {
                baseHoldUntil = ticks + SETTLE_TICKS;
            }
            lastTickNanos = now;
            ticks++;
            holdLogged = false;
            int count = loadedChunks(event.getServer());
            // 止まったあと少しの間は、下がるときだけ覚え直す（止める・動かすを短くくり返して、基準が上がっていかないように）
            runningChunkCount = runningChunkCount >= 0 && ticks <= baseHoldUntil ? Math.min(runningChunkCount, count) : count;
        }
    }

    /**
     * ワールドを閉じたら、頼んだものの数を忘れる（次に開くワールドは別のチャンクなので）
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        requestedChunks.clear();
        isolatedRequests.clear();
        isolatedOwner.clear();
        hasTeleported = false;
        lastTickNanos = 0;
        runningChunkCount = -1;
        baseHoldUntil = 0;
        holdLogged = false;
    }

    /**
     * 止まっているか（サーバーのスレッドで呼ぶ）
     * 画面側が止まっていれば true（Minecraft のサーバーも、この値を見て止まる。LAN に公開していると止まらないので false）。
     * そうでなくても、tick が終わってから PAUSE_DETECT_NANOS より長く次の tick が終わっていなければ true
     * （ワールドを開いて最初の tick が終わる前も true になるが、命令が少し待つだけなので困らない）
     */
    static boolean paused() {
        if (CLIENT && ClientPause.paused()) {
            return true;
        }
        return System.nanoTime() - lastTickNanos > PAUSE_DETECT_NANOS;
    }

    /**
     * 止まっていて、Minecraft が持つチャンク（作りかけも入れる）が止まる前より増えすぎたか、メモリが多すぎるか（memoryHigh）
     * （サーバーのスレッドで呼ぶ）。true なら、BuildQueue は動き出すまで列を進めない。待たせたときは、止まるたびに 1 回だけログに出す。
     * チャンクが増えてよいのは、テレポート 1 回分（見える範囲とその周り VIEW_SHELL の作りかけ）と BUDGET_HOLDERS
     * （止まる直前に動いていたテレポートや「埋める」の読み込みが、止まったあとに進んだ分も、ここで数える）。
     * メモリが多すぎるときは、近くに置くだけの命令も待つ（本当にメモリが足りないときだけなので）。
     * 待たせるたびに holds を 1 増やす
     */
    static boolean overLimit(MinecraftServer server) {
        if (!paused()) {
            return false;
        }
        if (memoryHigh()) {
            holds++;
            logHold("memory " + memoryUsed / (1024 * 1024) + " MB > " + (int) (PAUSED_HEAP_LIMIT * 100)
                + "% of " + memoryMax / (1024 * 1024) + " MB");
            return true;
        }
        if (runningChunkCount < 0) {
            return false;
        }
        int count = loadedChunks(server);
        int side = teleportSide(server);
        int limit = runningChunkCount + side * side + BUDGET_HOLDERS;
        if (count > limit) {
            holds++;
            logHold("chunks " + count + " > limit " + limit);
            return true;
        }
        return false;
    }

    /**
     * overLimit で列をまるごと待たせた回数（前に見たときから変わっていれば、その間は待たされていた）
     */
    static long holds() {
        return holds;
    }

    /**
     * テレポート 1 回で Minecraft が持つチャンク（見える範囲とその周り VIEW_SHELL の作りかけ）の、1 辺の数
     * （1 人用のワールドの見える範囲は 2〜32。専用サーバーは設定の値をそのまま持つので、同じ幅に丸める）
     */
    private static int teleportSide(MinecraftServer server) {
        int view = Mth.clamp(server.getPlayerList().getViewDistance(), 2, 32);
        return 2 * (view + VIEW_SHELL) + 1;
    }

    /**
     * 長く残るものの置き場（OLD_GEN）の使用量が、その上限の PAUSED_HEAP_LIMIT を超えたか
     */
    private static boolean memoryHigh() {
        readMemory();
        return memoryMax != Long.MAX_VALUE && memoryUsed > memoryMax * PAUSED_HEAP_LIMIT;
    }

    /**
     * 長く残るものの置き場の使用量と上限を読み、memoryUsed・memoryMax に入れる。
     * 置き場の上限が決まっていなければヒープの上限にする（G1 はヒープ全体まで広がる。Parallel・Serial は 3 分の 2 ほどまで）。
     * 置き場が見つからないか、使用量を読めないときは、ヒープ全体の使用量にする（ヒープの上限も決まっていなければ Long.MAX_VALUE で、
     * 比べる側は見ない）
     */
    private static void readMemory() {
        MemoryUsage usage = OLD_GEN == null ? null : OLD_GEN.getUsage();
        Runtime runtime = Runtime.getRuntime();
        memoryUsed = usage != null ? usage.getUsed() : runtime.totalMemory() - runtime.freeMemory();
        memoryMax = usage != null && usage.getMax() > 0 ? usage.getMax() : runtime.maxMemory();
    }

    /**
     * 止まっている間に列を待たせたことを、止まるたびに 1 回だけログに出す（先生が latest.log で気づけるように）
     */
    private static void logHold(String reason) {
        if (!holdLogged) {
            holdLogged = true;
            MinecraftEduMod.LOGGER.info("Build queue waits until the world is unpaused (" + reason + ")");
        }
    }

    /**
     * Minecraft が持つチャンクの数（すべてのディメンションの、作りかけも入れた数）
     */
    private static int loadedChunks(MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            count += level.getChunkSource().getLoadedChunksCount();
        }
        return count;
    }

    /**
     * 止まっている間に、チャンクの x minX〜maxX・z minZ〜maxZ のまだ読み込まれていないものを頼んでも、PAUSED_CHUNK_BUDGET 個に収まるか。
     * 離れた場所なら、ISOLATED_EXTRA 個分を足して数える。
     * 収まれば、頼んだものとして覚えて true（同じチャンクは何度頼んでも 1 個と数える。全部読み込まれていれば、いつも true）
     */
    static boolean withinBudget(ServerLevel world, int minX, int maxX, int minZ, int maxZ) {
        // 最後に頼んでから動いている tick が SETTLE_TICKS 回より多く進んだものは、もう下ろされたか、使われているので数えない
        requestedChunks.long2LongEntrySet().removeIf(entry -> ticks - entry.getLongValue() > SETTLE_TICKS);
        isolatedRequests.long2LongEntrySet().removeIf(entry -> ticks - entry.getLongValue() > SETTLE_TICKS);
        isolatedOwner.long2LongEntrySet().removeIf(entry -> !isolatedRequests.containsKey(entry.getLongValue()));
        LongSet added = new LongOpenHashSet();
        for (int tx = minX; tx <= maxX; tx++) {
            for (int tz = minZ; tz <= maxZ; tz++) {
                long key = ChunkPos.asLong(tx, tz);
                if (world.getChunkSource().getChunkNow(tx, tz) == null && !requestedChunks.containsKey(key)) {
                    added.add(key);
                }
            }
        }
        if (added.isEmpty()) {
            return true;
        }
        // メモリが多すぎるときは、予算が残っていても新しく頼まない（列を通らない読む命令も、ここを通るので止まる）
        if (memoryHigh()) {
            return false;
        }
        boolean isolated = isolated(world, minX, maxX, minZ, maxZ);
        int used = requestedChunks.size() + ISOLATED_EXTRA * isolatedRequests.size();
        if (used + added.size() + (isolated ? ISOLATED_EXTRA : 0) > PAUSED_CHUNK_BUDGET) {
            return false;
        }
        for (long key : added) {
            requestedChunks.put(key, ticks);
        }
        if (isolated) {
            long owner = added.iterator().nextLong();
            isolatedRequests.put(owner, ticks);
            for (long key : added) {
                isolatedOwner.put(key, owner);
            }
        }
        return true;
    }

    /**
     * チャンクにチケットを付けた（付け直した）ことを知らせる（BlockJob が呼ぶ。止まっていても、動いていても）。
     * 止まっている間に頼んだものとして数えているチャンクなら、数えたままにする時間を、ここから数え直す
     * （チケットは付け直すと切れるまでの時間が初めに戻るので、それより先に数えるのをやめないように）。
     * 離れた場所の ISOLATED_EXTRA 個分も、その場所のどのチャンクで付け直しても延ばす。
     * もう数える時間を過ぎていたものは延ばさずに外す（下ろされたあとで付け直しても、止まっている間に頼んだ分ではないため）
     */
    static void ticketRefreshed(long key) {
        refresh(requestedChunks, key);
        if (isolatedOwner.containsKey(key)) {
            refresh(isolatedRequests, isolatedOwner.get(key));
        }
    }

    /**
     * map に key があれば、数える時間の内なら ticks を今にし、過ぎていれば外す
     */
    private static void refresh(Long2LongMap map, long key) {
        if (!map.containsKey(key)) {
            return;
        }
        if (ticks - map.get(key) > SETTLE_TICKS) {
            map.remove(key);
        } else {
            map.put(key, ticks);
        }
    }

    /**
     * チャンクの x minX〜maxX・z minZ〜maxZ と、その辺で接するチャンクに、読み込み済みのチャンクも、止まっている間に頼んだチャンクも無いか
     * （無ければ、周りのチャンクの作りかけをあまり分け合えない、離れた場所。ななめだけで接していても離れた場所と見る）
     */
    private static boolean isolated(ServerLevel world, int minX, int maxX, int minZ, int maxZ) {
        for (int tx = minX - 1; tx <= maxX + 1; tx++) {
            for (int tz = minZ - 1; tz <= maxZ + 1; tz++) {
                boolean corner = (tx < minX || tx > maxX) && (tz < minZ || tz > maxZ);
                if (!corner && (world.getChunkSource().getChunkNow(tx, tz) != null
                    || requestedChunks.containsKey(ChunkPos.asLong(tx, tz)))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 止まっている間に、テレポートを通してよいか。よければ、通したものとして覚えて true
     * （止まるたびに 1 回まで。テレポートすると行き先の見える範囲を読み込むので、止まっている間にくり返すとたまるため。
     * 前に通してから動いている tick が SETTLE_TICKS 回より多く進んでいなければ、止まり直しても通さない。
     * 長く残るものの置き場に、テレポート 2 回分と予算の分を足すと PAUSED_HEAP_LIMIT を超えるときも通さない）
     */
    static boolean takeTeleport(MinecraftServer server) {
        if (hasTeleported && ticks - teleportTick <= SETTLE_TICKS) {
            return false;
        }
        // テレポートの行き先は、画面側の返事が届いてから読み込まれ、通したあとは止められないので、
        // 描画距離から見積もった 2 回分（1 回で、描画距離 16 なら 190 MB ほど、32 なら 450 MB ほど）と予算の分の余白が残っているときだけ通す
        // （止まる直前に動いていたテレポートの行き先も、止まったあとに読み込まれ、まだ使用量に入っていないことがあるため）
        readMemory();
        int side = teleportSide(server);
        long need = 2L * side * side * TELEPORT_BYTES_PER_HOLDER + BUDGET_BYTES;
        if (memoryMax != Long.MAX_VALUE && memoryUsed + need > memoryMax * PAUSED_HEAP_LIMIT) {
            logHold("teleport: memory " + memoryUsed / (1024 * 1024) + " MB + " + need / (1024 * 1024) + " MB > "
                + (int) (PAUSED_HEAP_LIMIT * 100) + "% of " + memoryMax / (1024 * 1024) + " MB");
            return false;
        }
        hasTeleported = true;
        teleportTick = ticks;
        return true;
    }
}
