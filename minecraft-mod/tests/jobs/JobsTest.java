package com.github.minecraftedu.commands;

import com.github.minecraftedu.MinecraftEduMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;

/**
 * 「埋める」（FillJob）・「周りをきれいにする」（ClearAreaJob）・1 マスの仕事（BlockJob）と、遠すぎる場所を断るきまり（Reach）の試験
 * （本物の FillJob・BlockJob・ClearAreaJob・Reach・BuildQueue を、偽物のワールドの上で動かす）。
 * Reach は、2,000 マスの境目・はじめの場所とプレイヤー・マイナスの切り下げ・箱は同じ 1 つの基準から、を確かめる。
 * BlockJob は、場所を決められないときにチケットを付けないこと・先読みしてよくない場所を先読みしないこと・あきらめを確かめる。
 * 本物の列で、先頭が読み込みを待つ間の先読みは、先読みするときのプレイヤーの位置で決めること（近づけば先読みし、離れたら先読みし直さない）、
 * 判定はメインスレッドで聞くことも確かめる。
 * 置く順・同じブロックを飛ばす・空気だけの区画を飛ばす・時間が来たら続きから・読み込みを待つ・あきらめる・止まっている間は数えない・
 * 例外でもチケットを外す・一時停止から区画の途中で再開しても置き直さない・仕事ごとのチケット（ほかの仕事のチケットを外さない）、
 * 「周りをきれいにする」の進み具合・取り消し・あきらめ・列の例外・再開の知らせ、本物の列で止めるボタンを押したときの片づけを確かめる
 */
public class JobsTest {

    private static int passed;
    private static int failed;

    static final BlockState AIR = BlockState.AIR;
    static final BlockState STONE = BlockState.of("stone");
    static final BlockState GOLD = BlockState.of("gold");
    static final BlockState BEDROCK = BlockState.of("bedrock");
    static final BlockState DIRT = BlockState.of("dirt");
    static final BlockState GRASS = BlockState.of("grass");

    static final String BUILD = "minecraftedu_build";
    static final String LINGER = "minecraftedu_block";
    // 「周りをきれいにする」と同じフラグ（となりに知らせない・となりの形を直さない）と、「埋める」のフラグ
    static final int CLEAR_FLAGS = 2 | 16;
    static final int FILL_FLAGS = 3;

    static void check(String name, boolean ok, Object detail) {
        if (ok) {
            passed++;
            System.out.println("  ok     " + name);
        } else {
            failed++;
            System.out.println("  NG     " + name + "  → " + detail);
        }
    }

    // 試験ごとに、新しいサーバーとワールド
    static MinecraftServer server;
    static ServerLevel world;

    static void fresh() {
        server = new MinecraftServer();
        world = new ServerLevel(server);
        server.world = world;
        PauseWatch.paused = false;
        PauseWatch.budgetOk = true;
        FillJob.maxLoadWaitNanos = TimeUnit.SECONDS.toNanos(30);
        MinecraftEduMod.lines.clear();
    }

    static ServerChunkCache chunks() {
        return world.getChunkSource();
    }

    static long far() {
        return System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
    }

    // もう時間が来ている（1 回で CHECK_EVERY = 256 回まで進めて止まる。区画に入るのも 1 回に数えるので、置くのは 255 個まで）
    static long past() {
        return System.nanoTime() - 1;
    }

    // 時間が来ている形で、置き始めるまで進める（はじめの回は、チャンクとそのとなりの読み込みを頼むだけで終わるため）。
    // 区画の途中で止まったところで戻る
    static void startPlacing(BuildQueue.Job job) {
        for (int i = 0; i < 100 && world.setOrder.isEmpty(); i++) {
            job.step(past());
        }
    }

    static FillJob fill(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState state, int flags) {
        return new FillJob(server, minX, minY, minZ, maxX, maxY, maxZ, y -> state, flags);
    }

    // 終わるまで進める（回数を返す。1000 回で終わらなければ -1）
    static int runToEnd(BuildQueue.Job job) {
        for (int i = 1; i <= 1000; i++) {
            if (job.step(far())) {
                return i;
            }
        }
        return -1;
    }

    // 範囲の全部のマスが state か
    static boolean allAre(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, IntFunction<BlockState> state) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.get(x, y, z) != state.apply(y)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static int maxSetCount() {
        return world.setCount.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    static boolean logged(String part) {
        synchronized (MinecraftEduMod.lines) {
            return MinecraftEduMod.lines.stream().anyMatch(l -> l.contains(part));
        }
    }

    static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 「周りをきれいにする」が出したチャット・失敗のチャット・入力の誤り
    static class Notices implements ClearAreaJob.Notices {
        final List<String> chat = Collections.synchronizedList(new ArrayList<>());
        final List<String> errorChat = Collections.synchronizedList(new ArrayList<>());
        final List<String> inputError = Collections.synchronizedList(new ArrayList<>());
        int tooFar;

        @Override
        public void chat(String message) {
            chat.add(message);
        }

        @Override
        public void errorChat(String message) {
            errorChat.add(message);
        }

        @Override
        public void inputError(String key, String message) {
            inputError.add(key);
        }

        @Override
        public void tooFar() {
            tooFar++;
        }

        long count(String part) {
            synchronized (chat) {
                return chat.stream().filter(c -> c.contains(part)).count();
            }
        }
    }

    // CommandExecutor の executeClearArea と同じ層（下から岩盤・土 2 段・草、その上は空気）
    static final IntFunction<BlockState> LAYER = y -> {
        if (y == -64) {
            return BEDROCK;
        } else if (y >= -63 && y <= -62) {
            return DIRT;
        } else if (y == -61) {
            return GRASS;
        }
        return AIR;
    };

    static final String GAVE_UP_CHAT = "まわりを きれいにするのを とちゅうで やめました";

    // executeClearArea と同じ形で、チャンク列ごとの FillJob を作る（x・z は minX〜maxX、高さは -64〜maxY）
    static List<FillJob> clearColumns(int minX, int minZ, int maxX, int maxZ, int maxY) {
        List<FillJob> columns = new ArrayList<>();
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            int cxStart = Math.max(minX, chunkX << 4);
            int cxEnd = Math.min(maxX, (chunkX << 4) + 15);
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                int czStart = Math.max(minZ, chunkZ << 4);
                int czEnd = Math.min(maxZ, (chunkZ << 4) + 15);
                columns.add(new FillJob(server, cxStart, -64, czStart, cxEnd, maxY, czEnd, LAYER, CLEAR_FLAGS));
            }
        }
        return columns;
    }

    public static void main(String[] args) throws Exception {
        reachTests();
        blockJobTests();
        fillTests();
        clearTests();
        queueTest();
        prefetchQueueTest();
        System.out.println("結果: " + passed + " 件成功 / " + failed + " 件失敗");
        System.exit(failed == 0 ? 0 : 1);
    }

    static void reachTests() {
        System.out.println("--- 遠すぎる場所（Reach）: はじめの場所とプレイヤーから 2,000 マスまで（横は x・z のうち大きいほう）");
        long[] spawnOnly = {0, 0};
        check("ちょうど 2,000 マスは通し、2,001 マスは断る（x）", Reach.within(spawnOnly, 2000, 0) && !Reach.within(spawnOnly, 2001, 0), "");
        check("z も同じ（ちょうど 2,000 は通し、2,001 は断る）", Reach.within(spawnOnly, 0, -2000) && !Reach.within(spawnOnly, 0, -2001), "");
        check("ななめは大きいほうで数える（2,000・2,000 は通し、2,000・2,001 は断る）",
            Reach.within(spawnOnly, 2000, 2000) && !Reach.within(spawnOnly, 2000, 2001), "");
        long[] players = {0, 0, 10000, 10000, -10000, 5000};
        check("はじめの場所から遠くても、どれかのプレイヤーから 2,000 マスの中なら通す（2 人目でも）",
            Reach.within(players, 11999, 10000) && Reach.within(players, -12000, 5000) && !Reach.within(players, 12001, 10000), "");
        fresh();
        world.levelData.xSpawn = 100;
        world.levelData.zSpawn = -100;
        world.players().add(new ServerPlayer(-5000.5, 0.0));
        long[] refs = Reach.references(world);
        check("はじめの場所と、このワールドのプレイヤーのいるマス（マイナスは切り下げ）を基準にする",
            Arrays.equals(refs, new long[]{100, -100, -5001, 0}), Arrays.toString(refs));
        check("プレイヤーのいるマスから 2,000 マスは通し、2,001 マスは断る",
            Reach.within(world, -7001, 0) && !Reach.within(world, -7002, 0), "");
        check("はじめの場所の近くは、プレイヤーが遠くにいても通す", Reach.within(world, 2100, -100) && !Reach.within(world, 2101, -100), "");

        System.out.println("--- 遠すぎる場所（Reach）: 箱は、4 すみが同じ 1 つの基準から 2,000 マスの中のときだけ通す");
        long[] split = {0, 0, 4200, 0};
        check("はじめの場所とプレイヤーの間をまたぐ箱は、4 すみがそれぞれ近くても断る（まん中が両方から遠い）",
            !Reach.boxWithin(split, 1990, 0, 2210, 10) && Reach.within(split, 1990, 0) && Reach.within(split, 2210, 10), "");
        check("1 つの基準から 4 すみとも 2,000 マスの中の箱は通す", Reach.boxWithin(split, 2200, -2000, 6200, 2000), "");
        check("x の端だけ 2,001 マスなら断る", !Reach.boxWithin(spawnOnly, -2000, -2000, 2001, 2000)
            && Reach.boxWithin(spawnOnly, -2000, -2000, 2000, 2000), "");
        check("z の端だけ 2,001 マスでも断る", !Reach.boxWithin(spawnOnly, -2000, -2001, 2000, 2000), "");
    }

    static void blockJobTests() {
        System.out.println("--- 1 マスの仕事（BlockJob）: 場所を決められない（遠すぎるなど）なら、チケットを付けずに終わる。先読みも、してよい場所だけ");
        fresh();
        int[] acted = {0};
        int[] gaveUp = {0};
        BlockJob refused = new BlockJob(server, w -> null, new BlockPos(40000, 200, 0), (w, at) -> false, true,
            (w, at) -> acted[0]++, () -> gaveUp[0]++);
        check("先読みしてよくない場所は、先読みのチャンクを出さない", refused.prefetchChunk() == BuildQueue.NO_PREFETCH, refused.prefetchChunk());
        refused.prefetch();
        boolean refusedDone = refused.step(far());
        check("場所を決められなければ 1 回で終わり、チケットを付けず、何もしない",
            refusedDone && chunks().addCalls.isEmpty() && acted[0] == 0 && gaveUp[0] == 0, chunks().addCalls);
        BlockPos near = new BlockPos(100, 200, 100);
        BlockJob allowed = new BlockJob(server, w -> near, near, (w, at) -> true, true, (w, at) -> acted[0]++, () -> gaveUp[0]++);
        check("先読みしてよい場所は、そのチャンクを返す", allowed.prefetchChunk() == ChunkPos.asLong(6, 6), allowed.prefetchChunk());
        allowed.prefetch();
        check("先読みで、そのチャンクにチケットを付けて読み込みを頼む", chunks().ticketChunks(LINGER).contains(ChunkPos.asLong(6, 6)),
            chunks().tickets(LINGER));
        boolean allowedDone = allowed.step(far());
        check("読み込まれていれば、その場所で 1 回だけする", allowedDone && acted[0] == 1 && gaveUp[0] == 0, acted[0]);

        System.out.println("--- 1 マスの仕事（BlockJob）: 読み込まれないまま決まった時間を過ぎたら、何もせずに、あきらめを 1 回だけ知らせる");
        fresh();
        chunks().blocked.add(ChunkPos.asLong(10, 10));
        FillJob.maxLoadWaitNanos = TimeUnit.MILLISECONDS.toNanos(50);
        int[] acted2 = {0};
        int[] gaveUp2 = {0};
        BlockPos stuck = new BlockPos(168, 200, 168);
        BlockJob waiting = new BlockJob(server, w -> stuck, stuck, null, false, (w, at) -> acted2[0]++, () -> gaveUp2[0]++);
        boolean before = waiting.step(far());
        boolean waitingFlag = waiting.waitingForLoad();
        sleep(80);
        boolean after = waiting.step(far());
        check("時間を過ぎるまでは待ち（読み込みを待っている印も立つ）、過ぎたら何もせずに終わる",
            !before && waitingFlag && after && acted2[0] == 0 && gaveUp2[0] == 1, before + " / " + waitingFlag + " / " + after + " / " + gaveUp2[0]);
    }

    static void fillTests() {
        System.out.println("--- 埋める: チャンクの境をまたぐ範囲を、全部 1 回ずつ置き、いちばん最後のマスを最後に置き、チケットを外す");
        fresh();
        FillJob f1 = fill(-3, 0, 5, 17, 20, 9, STONE, FILL_FLAGS);
        int volume = 21 * 21 * 5;
        boolean done = f1.step(far());
        check("1 回で置き終わる", done && !f1.gaveUp(), done);
        check("全部のマスに 1 回ずつ置いた（読み込まれていない所には置かない）",
            world.setCount.size() == volume && maxSetCount() == 1 && world.setOnUnloaded == 0,
            world.setCount.size() + " / " + maxSetCount() + " / " + world.setOnUnloaded);
        check("全部が石", allAre(-3, 0, 5, 17, 20, 9, y -> STONE), "");
        check("いちばん最後のマス（x・y・z がいちばん大きい）を最後に置く",
            world.setOrder.get(world.setOrder.size() - 1).equals(new BlockPos(17, 20, 9)), world.setOrder.get(world.setOrder.size() - 1));
        check("置いた数は体積と同じ", f1.placed() == volume, f1.placed());
        check("範囲のチャンク 3 つにチケットを付け、終わったら外す",
            chunks().addCalls.getOrDefault(BUILD, 0) == 3 && chunks().tickets(BUILD).isEmpty(),
            chunks().addCalls.get(BUILD) + " / " + chunks().tickets(BUILD));

        System.out.println("--- 埋める: もう同じブロックがあるマスには置かない");
        fresh();
        for (int x = 0; x <= 15; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = 0; z <= 15; z++) {
                    world.put(x, y, z, STONE);
                }
            }
        }
        FillJob f2 = fill(0, 0, 0, 15, 7, 15, STONE, FILL_FLAGS);
        runToEnd(f2);
        check("石がある下の 4 段には置かず、上の 4 段だけ置く（置いた数は全部で数える）",
            world.setCount.size() == 16 * 4 * 16 && f2.placed() == 16 * 8 * 16 && allAre(0, 0, 0, 15, 7, 15, y -> STONE),
            world.setCount.size() + " / " + f2.placed());

        System.out.println("--- 埋める: 空気で埋めるとき、空気しかない区画は飛ばし、石のある区画は空気にする");
        fresh();
        chunks().load(0, 0);
        world.put(5, 20, 5, STONE);
        FillJob f3 = fill(0, 0, 0, 15, 47, 15, AIR, CLEAR_FLAGS);
        runToEnd(f3);
        check("石のあったマスだけ空気にし、ほかは置かない（置いた数は全部で数える）",
            world.get(5, 20, 5) == AIR && world.setCount.size() == 1 && f3.placed() == 16 * 48 * 16,
            world.get(5, 20, 5) + " / " + world.setCount.size() + " / " + f3.placed());

        System.out.println("--- 埋める: 時間が来たら止めて、次は続きから（同じマスを 2 回置かない）");
        fresh();
        FillJob f4 = fill(0, 0, 0, 20, 10, 20, STONE, FILL_FLAGS);
        boolean first = f4.step(past());
        check("時間が来ていたら、全部は置かずに途中で止まる（256 回まで）",
            !first && world.setOrder.size() > 0 && world.setOrder.size() <= 256, world.setOrder.size());
        int calls = 1;
        while (!f4.step(past()) && calls < 10000) {
            calls++;
        }
        check("続きから置いて、全部 1 回ずつ置き終わる",
            world.setCount.size() == 21 * 11 * 21 && maxSetCount() == 1 && allAre(0, 0, 0, 20, 10, 20, y -> STONE),
            world.setCount.size() + " / " + maxSetCount());

        System.out.println("--- 埋める: チャンクが読み込まれるまで置かずに待ち、読み込まれたら置く");
        fresh();
        chunks().autoLoad = false;
        FillJob f5 = fill(32, 0, 32, 40, 4, 40, STONE, FILL_FLAGS);
        boolean waited = f5.step(far());
        check("読み込まれていなければ置かずに待つ（チケットを付けて読み込みを頼む）",
            !waited && world.setCount.isEmpty() && chunks().ticketChunks(BUILD).contains(ChunkPos.asLong(2, 2))
                && chunks().addCalls.getOrDefault(LINGER, 0) > 0,
            waited + " / " + world.setCount.size() + " / " + chunks().addCalls);
        chunks().loadPending();
        boolean loadedDone = f5.step(far());
        check("読み込まれたら置き終わり、チケットを外す",
            loadedDone && allAre(32, 0, 32, 40, 4, 40, y -> STONE) && chunks().tickets(BUILD).isEmpty(), loadedDone);

        System.out.println("--- 埋める: 読み込まれないまま決まった時間を過ぎたら、あきらめてチケットを外す");
        fresh();
        chunks().blocked.add(ChunkPos.asLong(4, 4));
        FillJob.maxLoadWaitNanos = TimeUnit.MILLISECONDS.toNanos(50);
        FillJob f6 = fill(64, 0, 64, 70, 3, 70, STONE, FILL_FLAGS);
        boolean early = f6.step(far());
        sleep(80);
        boolean late = f6.step(far());
        check("時間を過ぎるまでは待ち、過ぎたらやめる（gaveUp）",
            !early && late && f6.gaveUp() && world.setCount.isEmpty(), early + " / " + late + " / " + f6.gaveUp());
        check("あきらめたら、付けたチケットを外す", chunks().tickets(BUILD).isEmpty(), chunks().tickets(BUILD));

        System.out.println("--- 埋める: 1 人用のワールドが止まっている間は、あきらめるまでの時間に数えない");
        fresh();
        chunks().blocked.add(ChunkPos.asLong(4, 4));
        FillJob.maxLoadWaitNanos = TimeUnit.MILLISECONDS.toNanos(50);
        FillJob f7 = fill(64, 0, 64, 70, 3, 70, STONE, FILL_FLAGS);
        f7.step(far());
        PauseWatch.paused = true;
        f7.step(far());
        sleep(80);
        boolean pausedStep = f7.step(far());
        check("止まっている間は、時間を過ぎてもあきらめない", !pausedStep && !f7.gaveUp(), pausedStep + " / " + f7.gaveUp());
        PauseWatch.paused = false;
        boolean afterPause = f7.step(far());
        check("動き出したら数え直す（すぐにはあきらめない）", !afterPause && !f7.gaveUp(), afterPause + " / " + f7.gaveUp());
        sleep(80);
        boolean afterWait = f7.step(far());
        check("動き出してから時間を過ぎたら、あきらめる", afterWait && f7.gaveUp(), afterWait + " / " + f7.gaveUp());

        System.out.println("--- 埋める: 置く途中で例外が出ても、チケットを外してから知らせる");
        fresh();
        world.throwAt = new BlockPos(3, 1, 3);
        FillJob f8 = fill(0, 0, 0, 7, 3, 7, STONE, FILL_FLAGS);
        RuntimeException thrown = null;
        try {
            f8.step(far());
        } catch (RuntimeException e) {
            thrown = e;
        }
        check("例外はそのまま投げ、チケットは外してある", thrown != null && chunks().tickets(BUILD).isEmpty(),
            thrown + " / " + chunks().tickets(BUILD));
        f8.removeTickets();
        check("もう一度外しても何も起きない", chunks().tickets(BUILD).isEmpty(), chunks().tickets(BUILD));

        System.out.println("--- 埋める: 一時停止から区画の途中で再開したら、読み込みを確かめ直し、止めたマスから続ける（置き直さない）");
        fresh();
        FillJob f9 = fill(0, 0, 0, 15, 15, 15, STONE, CLEAR_FLAGS);
        startPlacing(f9);
        int before = world.setOrder.size();
        BlockPos changed = world.setOrder.get(0);
        // 止めている間に、ほかのタブが置いたマスを金にし、となりのチャンクが下ろされた
        world.put(changed.getX(), changed.getY(), changed.getZ(), GOLD);
        chunks().autoLoad = false;
        chunks().unload(-1, 0);
        f9.resumed();
        boolean resumedStep = f9.step(far());
        check("再開したら、置く前に、となりのチャンクの読み込みを確かめ直して待つ",
            !resumedStep && world.setOrder.size() == before, resumedStep + " / " + (world.setOrder.size() - before));
        chunks().loadPending();
        boolean resumedDone = f9.step(far());
        check("読み込まれたら、止めたマスから続けて置き終わる（全部 1 回ずつ）",
            resumedDone && world.setCount.size() == 16 * 16 * 16 && maxSetCount() == 1,
            resumedDone + " / " + world.setCount.size() + " / " + maxSetCount());
        check("止めている間にほかのタブが変えたマスは、置き直さない", world.get(changed.getX(), changed.getY(), changed.getZ()) == GOLD,
            world.get(changed.getX(), changed.getY(), changed.getZ()));
        check("区画の途中から続けたことを debug に残す", logged("FillJob resumed mid-cell"), MinecraftEduMod.lines);

        System.out.println("--- 埋める: チケットは仕事ごと（古い仕事の片づけが、同じチャンクの新しい仕事のチケットを外さない）");
        fresh();
        // チャンク (3, 3) の中だけ（端から 2 マスより内側なので、となりのチャンクにはチケットを付けない）
        FillJob oldJob = fill(52, 0, 52, 54, 2, 54, STONE, FILL_FLAGS);
        FillJob newJob = fill(52, 0, 52, 54, 2, 54, GOLD, FILL_FLAGS);
        oldJob.prepare(world);
        newJob.prepare(world);
        check("同じチャンクに、仕事ごとに別のチケットが付く", chunks().tickets(BUILD).size() == 2, chunks().tickets(BUILD));
        oldJob.removeTickets();
        check("古い仕事のチケットを外しても、新しい仕事のチケットは残る",
            chunks().tickets(BUILD).size() == 1 && chunks().ticketChunks(BUILD).contains(ChunkPos.asLong(3, 3)), chunks().tickets(BUILD));
        newJob.removeTickets();

        System.out.println("--- 埋める: 止まっていて、新しく読み込む数が予算に収まらなければ、チケットを付けずに待つ");
        fresh();
        chunks().autoLoad = false;
        PauseWatch.paused = true;
        PauseWatch.budgetOk = false;
        FillJob f11 = fill(96, 0, 96, 99, 2, 99, STONE, FILL_FLAGS);
        boolean overBudget = f11.step(far());
        check("止まっている間は、チケットも付けず、置かない", !overBudget && chunks().tickets(BUILD).isEmpty() && world.setCount.isEmpty(),
            overBudget + " / " + chunks().tickets(BUILD));
        PauseWatch.paused = false;
        PauseWatch.budgetOk = true;
        f11.step(far());
        chunks().loadPending();
        boolean budgetDone = f11.step(far());
        check("動き出したら、チケットを付けて読み込み、置き終わる", budgetDone && allAre(96, 0, 96, 99, 2, 99, y -> STONE), budgetDone);

        System.out.println("--- 埋める: となりを読む置き方はとなりにもチケット、そうでない置き方はとなりに時間で切れるチケットだけ");
        fresh();
        chunks().autoLoad = false;
        FillJob notify = fill(0, 0, 40, 3, 2, 43, STONE, FILL_FLAGS);
        notify.step(far());
        boolean notifyNeighbor = chunks().ticketChunks(BUILD).contains(ChunkPos.asLong(-1, 2));
        fresh();
        chunks().autoLoad = false;
        FillJob quiet = fill(0, 0, 40, 3, 2, 43, STONE, CLEAR_FLAGS);
        quiet.step(far());
        boolean quietBuild = chunks().ticketChunks(BUILD).contains(ChunkPos.asLong(-1, 2));
        boolean quietLinger = chunks().ticketChunks(LINGER).contains(ChunkPos.asLong(-1, 2));
        check("フラグ 3 は、端に触れている側のとなりにも置いている間のチケットを付ける", notifyNeighbor, chunks().tickets(BUILD));
        check("「周りをきれいにする」のフラグは、となりには時間で切れるチケットだけ付ける", !quietBuild && quietLinger,
            quietBuild + " / " + quietLinger);
    }

    static void clearTests() {
        System.out.println("--- 周りをきれいにする: 列を順に進め、開始・25% ごと・完了をチャットに出し、印を下ろし、チケットを全部外す");
        fresh();
        Notices n1 = new Notices();
        AtomicBoolean inProgress = new AtomicBoolean(true);
        List<FillJob> columns = clearColumns(0, 0, 47, 47, -50);
        ClearAreaJob c1 = new ClearAreaJob(server, columns, 24, 24, Long.MAX_VALUE, inProgress, w -> true, n1, GAVE_UP_CHAT);
        boolean firstStep = c1.step(past());
        check("1 回目は、この先の列（ここでは 9 列なので全部）にも先にチケットを付ける",
            !firstStep && chunks().ticketChunks(BUILD).size() == 9, chunks().ticketChunks(BUILD).size());
        int steps = runToEnd(c1);
        check("最後まで進む", steps > 0, steps);
        check("開始は 1 回だけ、25% ごとの進み具合が 4 回、完了が 1 回",
            n1.count("周囲クリア開始 (9チャンク)") == 1 && n1.count("進行中") == 4 && n1.count("周囲クリア完了: 中心(24, 24)") == 1
                && n1.chat.get(n1.chat.size() - 1).contains("完了"), n1.chat);
        check("終わったら印を下ろし、チケットを全部外す", !inProgress.get() && chunks().tickets(BUILD).isEmpty(),
            inProgress.get() + " / " + chunks().tickets(BUILD).size());
        check("岩盤・土・草・空気の層になる", allAre(0, -64, 0, 47, -50, 47, LAYER), "");

        System.out.println("--- 周りをきれいにする: 重さは、いちばん大きい「埋める」と同じまで");
        List<FillJob> heavy = clearColumns(0, 0, 47, 47, -50);
        long sum = heavy.stream().mapToLong(FillJob::weight).sum();
        ClearAreaJob capped = new ClearAreaJob(server, heavy, 0, 0, 1000, new AtomicBoolean(true), w -> true, new Notices(), GAVE_UP_CHAT);
        ClearAreaJob uncapped = new ClearAreaJob(server, heavy, 0, 0, Long.MAX_VALUE, new AtomicBoolean(true), w -> true, new Notices(), GAVE_UP_CHAT);
        check("上限より重ければ上限、軽ければそのまま", capped.weight() == 1000 && uncapped.weight() == sum,
            capped.weight() + " / " + uncapped.weight() + " / " + sum);

        System.out.println("--- 周りをきれいにする: 取り消したら印をすぐ下ろし、完了とは知らせず、片づけでチケットを全部外す");
        fresh();
        Notices n3 = new Notices();
        AtomicBoolean inProgress3 = new AtomicBoolean(true);
        ClearAreaJob c3 = new ClearAreaJob(server, clearColumns(0, 0, 47, 47, -50), 24, 24, Long.MAX_VALUE, inProgress3, w -> true, n3, GAVE_UP_CHAT);
        c3.step(past());
        c3.cancel();
        check("取り消したら、印をすぐ下ろす", !inProgress3.get(), inProgress3.get());
        runToEnd(c3);
        c3.release();
        check("取り消したあと最後まで進んでも、完了とは知らせない", n3.count("完了") == 0, n3.chat);
        check("片づけで、全部の列のチケットを外す", chunks().tickets(BUILD).isEmpty(), chunks().tickets(BUILD).size());

        System.out.println("--- 周りをきれいにする: 列のチャンクが読み込まれなければ、残りもやめて知らせ、印を下ろし、チケットを外す");
        fresh();
        Notices n4 = new Notices();
        AtomicBoolean inProgress4 = new AtomicBoolean(true);
        chunks().blocked.add(ChunkPos.asLong(0, 1));
        FillJob.maxLoadWaitNanos = TimeUnit.MILLISECONDS.toNanos(50);
        ClearAreaJob c4 = new ClearAreaJob(server, clearColumns(0, 0, 47, 47, -50), 24, 24, Long.MAX_VALUE, inProgress4, w -> true, n4, GAVE_UP_CHAT);
        boolean waiting4 = c4.step(far());
        sleep(80);
        boolean gaveUp4 = c4.step(far());
        check("待ちきれなければ、そこで終わる", !waiting4 && gaveUp4, waiting4 + " / " + gaveUp4);
        check("あきらめたことを 1 回だけ失敗のチャットで知らせ、ログにも出す（完了とは知らせない）",
            n4.errorChat.equals(List.of(GAVE_UP_CHAT)) && n4.inputError.equals(List.of("clearArea chunk not loaded")) && n4.count("完了") == 0,
            n4.errorChat + " / " + n4.inputError + " / " + n4.chat);
        check("印を下ろし、先に付けた残りの列のチケットも外す", !inProgress4.get() && chunks().tickets(BUILD).isEmpty(),
            inProgress4.get() + " / " + chunks().tickets(BUILD).size());
        // 別の接続が次の「周りをきれいにする」を始めた（印が立つ）あとに、古い仕事の取り消しが来ても、印を下ろさない
        inProgress4.set(true);
        c4.cancel();
        check("あきらめたあとの古い取り消しは、次の「周りをきれいにする」の印を下ろさない", inProgress4.get(), inProgress4.get());

        System.out.println("--- 周りをきれいにする: 一時停止から再開したら、いまの列に知らせる");
        fresh();
        ClearAreaJob c6 = new ClearAreaJob(server, clearColumns(0, 0, 47, 47, -50), 24, 24, Long.MAX_VALUE, new AtomicBoolean(true), w -> true,
            new Notices(), GAVE_UP_CHAT);
        startPlacing(c6);
        c6.resumed();
        check("いまの列が、区画の途中から続けることを debug に残す", logged("FillJob resumed mid-cell"), MinecraftEduMod.lines);
        runToEnd(c6);

        System.out.println("--- 周りをきれいにする: 順番が来たときに遠すぎる場所なら、始めずに断る（チケットも付けない）");
        fresh();
        Notices n8 = new Notices();
        AtomicBoolean inProgress8 = new AtomicBoolean(true);
        ClearAreaJob c8 = new ClearAreaJob(server, clearColumns(0, 0, 47, 47, -50), 24, 24, Long.MAX_VALUE, inProgress8, w -> false, n8,
            GAVE_UP_CHAT);
        boolean refused = c8.step(far());
        check("1 回目で終わり、遠すぎることを 1 回だけ知らせ、印を下ろす（開始とは知らせない）",
            refused && n8.tooFar == 1 && !inProgress8.get() && n8.chat.isEmpty(), refused + " / " + n8.tooFar + " / " + inProgress8.get() + " / " + n8.chat);
        check("チケットを付けず、何も置かない", chunks().addCalls.isEmpty() && world.setCount.isEmpty(), chunks().addCalls);
        inProgress8.set(true);
        c8.cancel();
        check("断ったあとの古い取り消しは、次の「周りをきれいにする」の印を下ろさない", inProgress8.get(), inProgress8.get());

        System.out.println("--- 周りをきれいにする: 1 つの列で例外が出ても、その列のチケットを外して次の列へ進み、完了する");
        fresh();
        Notices n7 = new Notices();
        AtomicBoolean inProgress7 = new AtomicBoolean(true);
        world.throwAt = new BlockPos(3, -63, 3);
        ClearAreaJob c7 = new ClearAreaJob(server, clearColumns(0, 0, 47, 47, -50), 24, 24, Long.MAX_VALUE, inProgress7, w -> true, n7, GAVE_UP_CHAT);
        runToEnd(c7);
        check("例外をログに出し、残りの列を進めて完了する",
            logged("チャンク処理エラー (index=0)") && n7.count("完了") == 1 && !inProgress7.get() && allAre(16, -64, 0, 47, -50, 47, LAYER),
            n7.chat);
        check("例外の列も入れて、チケットを全部外す", chunks().tickets(BUILD).isEmpty(), chunks().tickets(BUILD).size());
    }

    // メインスレッドの待ち列を、done が true になるまで（長くて ms ミリ秒）流す（速さに頼らないように、決まった時間ではなく状態で待つ）
    static boolean runMainUntil(java.util.function.BooleanSupplier done, long ms) throws InterruptedException {
        long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(ms);
        while (System.nanoTime() < until) {
            if (done.getAsBoolean()) {
                return true;
            }
            server.runOne(20);
        }
        return done.getAsBoolean();
    }

    static void prefetchQueueTest() throws InterruptedException {
        System.out.println("--- 本物の列: 先頭が読み込みを待つ間の先読みは、先読みするときのプレイヤーの位置で決める（近づけば先読みし、離れたらし直さない）");
        fresh();
        // 先頭は、読み込まれないチャンクを待ち続ける（その間、列は後ろの命令のチャンクを先読みする）
        chunks().blocked.add(ChunkPos.asLong(2, 2));
        BuildQueue queue = new BuildQueue(server, () -> false);
        queue.pumpRetryMillis = 20;
        Object owner = new Object();
        BlockPos headPos = new BlockPos(40, 200, 40);
        BlockJob head = new BlockJob(server, w -> headPos, headPos, null, false, (w, at) -> { }, () -> { });
        // 後ろの命令は、はじめの場所 (0, 0) から 5 万マス先（プレイヤーがいなければ遠すぎる）
        BlockPos farPos = new BlockPos(50008, 200, 8);
        ChunkPos farChunkPos = new ChunkPos(50008 >> 4, 0);
        long farChunk = farChunkPos.toLong();
        List<Boolean> askedOnMain = Collections.synchronizedList(new ArrayList<>());
        BlockJob far = new BlockJob(server, w -> farPos, farPos, (w, at) -> {
            askedOnMain.add(server.isSameThread());
            return Reach.within(w, at.getX(), at.getZ());
        }, false, (w, at) -> { }, () -> { });
        check("2 つとも列に並ぶ", queue.offer(head, owner) && queue.offer(far, owner), "");
        // 判定を 2 回以上聞くまで流す（1 回目のあとも先読みしていないことを見るため）
        boolean asked = runMainUntil(() -> askedOnMain.size() >= 2, 5000);
        check("遠い間は、後ろの命令のチャンクを先読みしない（先読みしてよいかは聞いている）",
            asked && !chunks().ticketChunks(LINGER).contains(farChunk), askedOnMain.size() + " / " + chunks().tickets(LINGER));
        // プレイヤーが、後ろの命令の近くへ来た（試験のスレッドが runOne でメインスレッドの仕事を動かしているので、
        // 仕事と仕事のあいだに、同じスレッドで書きかえる。ほかのスレッドは players を読まない）
        world.players().add(new ServerPlayer(50000.5, 0.5));
        boolean prefetched = runMainUntil(() -> chunks().ticketChunks(LINGER).contains(farChunk), 5000);
        check("近づいたら、そのときの位置で決めて先読みする", prefetched, chunks().tickets(LINGER));
        // プレイヤーが離れ、先読みのチケットも切れた（偽物のチケットは時間で切れないので、外して切れたことにする）
        world.players().clear();
        chunks().removeRegionTicket(net.minecraft.server.level.TicketType.create(LINGER, null), farChunkPos, 0, farChunkPos);
        // 外せたかを先に見る（本物のチケットの付け方が変わって外せなかったときに、「先読みし直した」と取りちがえないように）
        check("離れる前の先読みのチケットを外せた（試験の準備）", !chunks().ticketChunks(LINGER).contains(farChunk), chunks().tickets(LINGER));
        int askedBefore = askedOnMain.size();
        boolean askedAgain = runMainUntil(() -> askedOnMain.size() >= askedBefore + 2, 5000);
        check("離れたら、先読みし直さない", askedAgain && !chunks().ticketChunks(LINGER).contains(farChunk),
            (askedOnMain.size() - askedBefore) + " / " + chunks().tickets(LINGER));
        long offMain;
        synchronized (askedOnMain) {
            offMain = askedOnMain.stream().filter(b -> !b).count();
        }
        check("先読みしてよいかは、いつもメインスレッドで聞く", !askedOnMain.isEmpty() && offMain == 0,
            "メインスレッドの外で " + offMain + " 回 / 全部で " + askedOnMain.size() + " 回");
        queue.cancel(owner);
        boolean idle = runMainUntil(queue::idle, 5000);
        check("やめたら、列は空く", idle, queue.idle());
    }

    static void queueTest() throws InterruptedException {
        System.out.println("--- 本物の列: 「周りをきれいにする」の途中で止めるボタン（その接続の仕事をやめる）を押す");
        fresh();
        // チャンクを読み込まないので、仕事は必ず途中のまま（処理が速くても、取り消す前に終わってしまわないように）
        chunks().autoLoad = false;
        BuildQueue queue = new BuildQueue(server, () -> false);
        queue.pumpRetryMillis = 20;
        Notices notices = new Notices();
        AtomicBoolean inProgress = new AtomicBoolean(true);
        Object owner = new Object();
        ClearAreaJob job = new ClearAreaJob(server, clearColumns(0, 0, 79, 79, -40), 40, 40, Long.MAX_VALUE, inProgress, w -> true, notices,
            GAVE_UP_CHAT);
        check("列に並ぶ", queue.offer(job, owner), "");
        // 少しだけ進める（読み込みを待つので、全部は終わらない）
        for (int i = 0; i < 3; i++) {
            server.runOne(200);
        }
        boolean started = notices.count("周囲クリア開始") == 1 && !chunks().tickets(BUILD).isEmpty();
        // 受け口のスレッドで取り消す（本物と同じく、メインスレッドの外から）
        Thread receiver = new Thread(() -> queue.cancel(owner));
        receiver.start();
        receiver.join();
        check("始まって、チケットを付けている途中で取り消すと、印はすぐ下りる", started && !inProgress.get(),
            started + " / " + inProgress.get());
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            boolean ran = server.runOne(20);
            if (!ran && queue.idle() && !server.hasTasks()) {
                break;
            }
        }
        check("メインスレッドの片づけで、全部の列のチケットを外す（完了とは知らせない）",
            queue.idle() && chunks().tickets(BUILD).isEmpty() && notices.count("完了") == 0,
            queue.idle() + " / " + chunks().tickets(BUILD).size() + " / " + notices.chat);
    }
}
