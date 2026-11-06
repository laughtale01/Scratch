package com.github.minecraftedu.commands;

import com.github.minecraftedu.MinecraftEduMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

/**
 * 世界を変える仕事を、届いた順に 1 本の列に並べて、メインスレッドで少しずつ進める
 *
 * 大きな仕事（「ブロックで埋める」「周りをきれいにする」）を 1 回で全部やると Minecraft が止まるので、
 * tick と tick のあいだの空いた時間（次の tick が始まる少し前まで、長くても MAX_SLICE_MILLIS）だけ進め、
 * 次の tick が終わってから続きをやる。
 * 後ろの仕事は前の仕事が終わるまで待つので、Scratch のスクリプトの順番どおりに置かれる
 * （前は「周りをきれいにする」の途中に、あとから置いたブロックが割りこみ、まだの区画で空気に上書きされていた）
 *
 * 進める処理は Minecraft のメインスレッドの待ち列（server.execute）で動かす。サーバーの tick のイベントは使わない
 * （1 人用のワールドはポーズ中（ほかのウィンドウに移ったときなど）に tick のイベントが来ないので、
 *   Scratch のブラウザを触っている間に何も置かれなくなるため。server.execute の待ち列はポーズ中も動く）
 */
public class BuildQueue {

    /**
     * 列に並べる仕事
     */
    public interface Job {
        /**
         * メインスレッドで、deadlineNanos（System.nanoTime の値）まで進める
         * 時間が来たとき（またはチャンクの読み込みを待つとき）は途中で止めて false を返す。
         * false を返したら、その回はそこで終わり、次の回に続きから呼ばれる
         *
         * @param deadlineNanos ここまでに止める時刻
         * @return 終わったら true
         */
        boolean step(long deadlineNanos);

        /**
         * 列にたまる量の上限に数える重さ（置くブロックの数。ふつうの仕事は 1）
         */
        default long weight() {
            return 1;
        }

        /**
         * チャンクの読み込みを待っているか（先頭の仕事が待っていれば、後ろの仕事のチャンクも先に読み込みを頼む。
         * いまは BlockJob だけ。「埋める」などが先頭のときは先読みしない）
         */
        default boolean waitingForLoad() {
            return false;
        }

        /**
         * 前もって読み込みを頼めるチャンク（ChunkPos.asLong の値）。場所が前もって分からない仕事は NO_PREFETCH
         * （同じチャンクに何度も頼まないよう、BuildQueue が見分けるのに使う。ここでは読み込みを頼まない）
         */
        default long prefetchChunk() {
            return NO_PREFETCH;
        }

        /**
         * 前もって読み込みを頼む（メインスレッド）。見るチャンク（となりも見る置き方なら、となりも）にチケットを付ける。
         * 読み込み済みでも付け直す（先頭が待つ間に、先に読み込んだチャンクが下ろされて、読み直しにならないように。
         * 頼む数は、BuildQueue が PREFETCH_CHUNKS で抑える）
         */
        default void prefetch() {
        }
    }

    /**
     * 1 回で終わる仕事を作る（チャット・テレポート・ブロック 1 つなど）
     */
    public static Job of(Runnable task) {
        return deadlineNanos -> {
            task.run();
            return true;
        };
    }

    // 1 回に進める時間の長さの上限と下限（ミリ秒。Minecraft の 1 tick は 50 ミリ秒）
    // 下限は、Minecraft が遅れていて空いた時間が無いときも、ある程度は進めるため（遅い PC で何分もかからないように）
    private static final long MAX_SLICE_MILLIS = 20;
    private static final long MIN_SLICE_MILLIS = 5;
    // 次の tick が始まる、この時間（ミリ秒）前には止める
    private static final long TICK_MARGIN_MILLIS = 3;
    // この重さ以下の仕事は「小さな仕事」（ブロック 1 つ、16×16×16 までの「埋める」、チャットなど）
    public static final long SMALL_JOB_WEIGHT = 4096;
    // 大きな仕事を断る、列にたまった重さ（大きな仕事と小さな仕事の合計。いちばん大きい「ブロックで埋める」の 2 回分）
    public static final long MAX_PENDING_WEIGHT = 4_000_000L;
    // 小さな仕事を断る、列にたまった小さな仕事だけの重さ（「ずっと」の中で小さく埋め続けても、
    // Scratch を止めたあとに置き続ける時間が、遅い PC でも 1 分ほどで済むように（Mac で 1 秒に約 23 万ブロック置けたので、
    // 遅い PC はその 3〜4 分の 1 と見て 20 秒〜1 分ほど）。ふつうは届かない）
    public static final long MAX_PENDING_SMALL_WEIGHT = 2_000_000L;
    // どんな仕事も断る、列にたまった仕事の数（メモリを使いすぎないため。ふつうは届かない）
    public static final int MAX_PENDING_JOBS = 100_000;
    // 先頭の仕事がチャンクの読み込みを待っている間に、後ろの仕事のために先に読み込みを頼む範囲
    // （遠くに並べるとき、Minecraft がチャンクを 1 つずつ順に作るのではなく、まとめて作れるように）。
    // 先頭も入れて、別々の（仕事の場所の）チャンク PREFETCH_CHUNKS 個までと、見る仕事 PREFETCH_SCAN_JOBS 件まで
    // （先頭が相対座標で、場所が前もって分からないときは、後ろだけで PREFETCH_CHUNKS 個）。
    // 読み込み済みのチャンクも数に入れる（先へ先へと作り続けないように。数に入れたチャンクは、先頭が待つ間は読み込んだままにする）。
    // となりのチャンクも待つ置き方では、となりも含めて、その 4 倍まで頼むことがある。8 と 32 で速さが変わらなかったので、少しだけ先まで
    private static final int PREFETCH_CHUNKS = 8;
    private static final int PREFETCH_SCAN_JOBS = 1024;
    // 前もって読み込みを頼めるチャンクが無いときの、prefetchChunk の値
    public static final long NO_PREFETCH = Long.MIN_VALUE;

    private final MinecraftServer server;
    // サーバーが止まりかけているか（CommandExecutor の serverStopping）
    private final BooleanSupplier stopping;
    private final ConcurrentLinkedQueue<Job> jobs = new ConcurrentLinkedQueue<>();
    // 列にたまっている仕事の重さの合計と、そのうち小さな仕事の重さと、仕事の数
    private final AtomicLong pendingWeight = new AtomicLong();
    private final AtomicLong pendingSmallWeight = new AtomicLong();
    private final AtomicInteger pendingJobs = new AtomicInteger();
    // 進める処理をメインスレッドに頼んであるか（動いている間も true。2 つ同時に頼まないため）
    private final AtomicBoolean pumpRequested = new AtomicBoolean(false);
    // 間をあけてから続きを頼むためのタイマー（Minecraft が終わるのを止めないよう、デーモンのスレッド 1 つ）
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "MinecraftEdu-BuildQueue");
        thread.setDaemon(true);
        return thread;
    });

    public BuildQueue(MinecraftServer server, BooleanSupplier stopping) {
        this.server = server;
        this.stopping = stopping;
    }

    /**
     * 重さ weight の仕事を、いま列に足してよいか
     * 大きな仕事は、列にたまった重さ（小さな仕事も入れた合計）が MAX_PENDING_WEIGHT に届いていたら断る
     * （「ずっと」の中で大きく埋め続けて、Scratch を止めたあとも何分も置き続けることがないように）。
     * 小さな仕事（ブロック 1 つ・小さな「埋める」）は、列にたまった小さな仕事だけの重さで決める。
     * 大きな仕事の後ろでも断らない（Scratch は返事を待たないので、断ると子どもの作品からブロックが黙って抜けるため）。
     * 小さな仕事だけで MAX_PENDING_SMALL_WEIGHT に届いたときと、仕事の数がとても多いときだけ断る
     * （確かめてから足すまでの間に、ほかの接続が足すことがあるので、上限はおおよそ）
     */
    public boolean hasRoomFor(long weight) {
        if (pendingJobs.get() >= MAX_PENDING_JOBS) {
            return false;
        }
        return isSmall(weight)
            ? pendingSmallWeight.get() < MAX_PENDING_SMALL_WEIGHT
            : pendingWeight.get() < MAX_PENDING_WEIGHT;
    }

    /**
     * 小さな仕事か（ブロック 1 つ、16×16×16 までの「埋める」、チャットなど）
     */
    private static boolean isSmall(long weight) {
        return weight <= SMALL_JOB_WEIGHT;
    }

    /**
     * 列にたまっている仕事の重さ（読む命令が、列で待つかどうかを決めるのに使う）
     */
    public long pendingWeight() {
        return pendingWeight.get();
    }

    /**
     * 列の最後に仕事を足す（どのスレッドから呼んでもよい）
     * 仕事の数が上限に届いていたら、足さずに false を返す（チャット・テレポートなど、
     * hasRoomFor を確かめない仕事も、これでたまり続けないようにする）
     *
     * @return 足したら true
     */
    public boolean offer(Job job) {
        if (stopping.getAsBoolean()) {
            return false;
        }
        if (pendingJobs.incrementAndGet() > MAX_PENDING_JOBS) {
            pendingJobs.decrementAndGet();
            return false;
        }
        long weight = job.weight();
        pendingWeight.addAndGet(weight);
        if (isSmall(weight)) {
            pendingSmallWeight.addAndGet(weight);
        }
        jobs.add(job);
        requestPump();
        return true;
    }

    /**
     * 列を空にして、タイマーを止める（サーバーが止まるとき）
     */
    public void clear() {
        jobs.clear();
        pendingWeight.set(0);
        pendingSmallWeight.set(0);
        pendingJobs.set(0);
        timer.shutdownNow();
    }

    /**
     * まだ頼んでいなければ、進める処理をすぐメインスレッドに頼む
     */
    private void requestPump() {
        if (pumpRequested.compareAndSet(false, true)) {
            postPump();
        }
    }

    /**
     * 進める処理をメインスレッドの待ち列に入れる
     */
    private void postPump() {
        if (stopping.getAsBoolean() || !server.isRunning()) {
            pumpRequested.set(false);
            return;
        }
        try {
            server.execute(() -> {
                // Minecraft が落ちたあとは、頼んだ処理が呼んだスレッドの上でそのまま動くので、そのときは動かさない
                // （落ちたときは止める知らせが来ないので、isStopped でも見る）
                if (stopping.getAsBoolean() || !server.isRunning() || server.isStopped() || !server.isSameThread()) {
                    pumpRequested.set(false);
                    return;
                }
                pump();
            });
        } catch (RuntimeException e) {
            // 頼めなかった（サーバーが止まりかけているなど）。印を下ろして、次に足されたときに頼み直せるようにする
            pumpRequested.set(false);
            MinecraftEduMod.LOGGER.debug("BuildQueue: could not schedule pump: " + e);
        }
    }

    /**
     * 列の先頭から、決まった時間だけ仕事を進める（メインスレッド）
     */
    private void pump() {
        // 次の tick が始まる少し前まで（遅れが出ないように）。getNextTickTime は Util.getMillis と同じ物差し
        long untilNextTick = server.getNextTickTime() - Util.getMillis() - TICK_MARGIN_MILLIS;
        long sliceMillis = Math.max(MIN_SLICE_MILLIS, Math.min(MAX_SLICE_MILLIS, untilNextTick));
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(sliceMillis);
        Job job;
        while ((job = jobs.peek()) != null) {
            boolean done;
            try {
                done = job.step(deadline);
            } catch (Exception e) {
                // 1 つの仕事のエラーで列が止まらないよう、その仕事はやめて次へ進む
                MinecraftEduMod.LOGGER.error("BuildQueue job error", e);
                done = true;
            }
            if (!done) {
                // 時間が来たか、チャンクの読み込みを待っている。この回はここまで
                // 読み込みを待っているなら、後ろの仕事のチャンクも先に頼んでおく（置く順番は変えない）
                if (job.waitingForLoad()) {
                    prefetchAhead(job);
                }
                break;
            }
            jobs.poll();
            long weight = job.weight();
            pendingWeight.addAndGet(-weight);
            if (isSmall(weight)) {
                pendingSmallWeight.addAndGet(-weight);
            }
            pendingJobs.decrementAndGet();
            if (System.nanoTime() - deadline >= 0) {
                break;
            }
        }

        if (jobs.isEmpty()) {
            pumpRequested.set(false);
            // 上で空と見てから印を下ろすまでの間に足された仕事を、取りこぼさない
            if (!jobs.isEmpty()) {
                requestPump();
            }
            return;
        }
        // 時間で止めたので、次の tick が始まってから続きを頼む（印は付けたまま）
        // （頼んだ処理は、その tick が終わってから動くので、次の空いた時間をまるごと使える）
        long untilTickStarts = Math.max(1, server.getNextTickTime() - Util.getMillis() + 1);
        try {
            timer.schedule(this::postPump, untilTickStarts, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            // サーバーが止まって、タイマーも止めたあと
            pumpRequested.set(false);
        }
    }

    /**
     * 先頭の仕事のすぐ後ろから、別々のチャンクを（先頭のチャンクも入れて）PREFETCH_CHUNKS 個まで見て、
     * 先に読み込みを頼む（読み込み済みなら付け直す。メインスレッド）。見るのは PREFETCH_SCAN_JOBS 件まで
     */
    private void prefetchAhead(Job head) {
        try {
            LongSet seen = new LongOpenHashSet();
            long headChunk = head.prefetchChunk();
            if (headChunk != NO_PREFETCH) {
                seen.add(headChunk);
            }
            int scanned = 0;
            Iterator<Job> it = jobs.iterator();
            if (it.hasNext()) {
                it.next();  // 先頭（いま待っている仕事）は、自分で頼んでいる
            }
            while (it.hasNext() && scanned++ < PREFETCH_SCAN_JOBS && seen.size() < PREFETCH_CHUNKS) {
                Job job = it.next();
                long chunk = job.prefetchChunk();
                if (chunk != NO_PREFETCH && seen.add(chunk)) {
                    job.prefetch();
                }
            }
        } catch (Exception e) {
            // 先読みは速くするためだけなので、失敗しても先読みをやめるだけにする（列を止めない）
            MinecraftEduMod.LOGGER.error("BuildQueue prefetch error", e);
        }
    }
}
