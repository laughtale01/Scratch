package com.github.minecraftedu.commands;

import com.github.minecraftedu.MinecraftEduMod;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
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
 * 列は送った接続（Scratch のタブ）ごとに分けて持ち、仕事には届いた順の通し番号を付ける。
 * 進めるときは、各接続の列の先頭のうち番号がいちばん小さい仕事を選ぶので、届いた順は 1 本の列と同じになる
 * （接続ごとに分けるのは、あとで 1 つのタブの仕事だけを止めたり外したりしても、ほかのタブの仕事が待たされないようにするため）
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
         * 取り消してから片づけるまでの間に持っているものの多さ（ふつうの仕事は 1。「周りをきれいにする」は列の数）
         * 片づけを待つ仕事が多すぎるときに、新しい仕事を断るのに使う（MAX_PENDING_RELEASE）
         */
        default long footprint() {
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

        /**
         * 送った接続が止まった・切れたので、列から外した（cancel を呼んだスレッドで、すぐ呼ぶ。ふつうは受け口のスレッド）。
         * そのため、メインスレッドが同じ仕事の step を進めている最中に呼ばれることがある（すぐあとに同じスレッドで footprint も呼ぶ）。
         * step と同時に動いても困らないようにする（ClearAreaJob は active の AtomicBoolean で守っている）。
         * 例外でやめた仕事にも、release の前に呼ぶ。外す印を取った側のスレッドで 1 回だけ呼ぶ
         * （ふつうは列の処理の側＝メインスレッド。取り消しとぶつかって、取り消した側が先に印を取ったときは、取り消した側のスレッド）。
         * すぐ下ろしたい印（「周りをきれいにする」の途中の印など）だけを下ろす。チケットを外すなど、
         * メインスレッドでしかできない片づけは release でする。
         * 列にあって、まだ始まっていない仕事にも呼ぶ。ちょうど終わったところで外すこともあるので、
         * 終わったあとに呼ばれても困らないようにする
         */
        default void cancel() {
        }

        /**
         * 取り消したあとの片づけ（メインスレッド。チケットを外すなど）。取り消した仕事・例外でやめた仕事に、1 回だけ呼ぶ。
         * 例外でやめるのは、step が例外を投げたときと、列の処理の例外が同じ途中の仕事で続いたとき（どちらも cancel のあとに呼び、
         * cancel が例外を投げても呼ぶ）。
         * ふつうに終わったときに BuildQueue のほうから呼ぶことは無い（終わった仕事の片づけは、step の中で自分でする）。
         * ただし、ちょうど終わるところで取り消された仕事や、まだ始まっていない仕事にも呼ぶので、
         * 片づけが済んでいる・何も持っていなければ何もしないようにする。
         * 取り消したときに進めている途中だった仕事は、その回の step が終わってから呼ぶ。release のあとに step は呼ばないが、
         * その回の終わりに waitingForLoad・prefetchChunk を呼ぶことがある（読むだけにしておく）。
         * BuildQueue は、ふつうは次の仕事を進める前に呼ぶが、取り消しと同時に次の仕事が始まると、あとになることがある
         * （そのため、片づけで外すのは自分が付けたものだけにする。FillJob のチケットは仕事ごとに値を変えてある）
         */
        default void release() {
        }

        /**
         * 一時停止から再開した（BuildQueue が、再開した列の先頭のこの仕事を、再開してから初めて進める前に、メインスレッドで呼ぶ。
         * ただし、メインスレッドがその列を止めた状態で見ていたときだけ。止めると再開がメインスレッドの 1 回の間に両方届いたときは、
         * 仕事は止まっていないので呼ばない。そのときは時間を数え直すことも、チャンクを確かめ直すことも要らない）。
         * 止めていた間を、チャンクの読み込みをあきらめるまでの時間に数えないよう、待ち始めた時刻を忘れる。
         * 止めている間に下ろされたかもしれないチャンクは、進める前に確かめ直す（止めている間も Minecraft は動いているので、
         * 時間で切れるチケットは切れる）。呼ぶのは、再開した列の仕事だけ（ほかのタブの仕事の時間には触らない）。
         * まだ一度も進めていない仕事に呼ぶこともある（止めるボタンのあとの古い印などで）ので、何も持っていなければ何もしない
         */
        default void resumed() {
        }
    }

    /**
     * 列の 1 つ分（仕事と、それを送った接続と、届いた順の番号）
     */
    private static final class Entry {
        final Job job;
        // 届いた順の通し番号（どの接続の列の先頭から進めるかを決める。小さいほど先に届いた）
        final long seq;
        // 列から外したか（終わったか、取り消した）。重さを 1 回だけ引くため
        final AtomicBoolean removed = new AtomicBoolean(false);
        // 入っている列（進めている途中の仕事が、止めた列のものかを見るため）
        final Lane lane;

        Entry(Job job, long seq, Lane lane) {
            this.job = job;
            this.seq = seq;
            this.lane = lane;
        }
    }

    /**
     * 1 つの接続の列（その接続から届いた順に並ぶ）
     * 列のつながりから仕事を外すのはメインスレッドだけ（head の poll）。取り消す側は removed の印を立てるだけにする
     * （ほかのスレッドが途中を外すと、ConcurrentLinkedQueue.remove が列を最後までたどることがあるため）
     */
    private static final class Lane {
        final ConcurrentLinkedQueue<Entry> entries = new ConcurrentLinkedQueue<>();
        // 一時停止しているか（pause で立て、resume・cancel で下ろす。立っている間は、この列の仕事を進めない）
        volatile boolean paused;
        // この列を一時停止から再開した回数（どのスレッドからでも足す）と、メインスレッドが最後に見た値
        // （変わっていて、pauseSeen が立っていたら、この列の仕事を進める前に Job.resumed を呼ぶ。メインスレッドだけが seenResumes を触る）
        final AtomicLong resumes = new AtomicLong();
        long seenResumes;
        // メインスレッドが、この列を止めた状態で見たか（nextEntry が止めた列を飛ばした。メインスレッドだけが触る）。
        // 再開の知らせは、これが立っているときだけ出す（止めると再開が、メインスレッドの 1 回の間に両方届いたときは、
        // 仕事は止まっていないので、あきらめるまでの時間を数え直させない。くり返し送って時間を戻し続けられないように）
        boolean pauseSeen;

        /**
         * まだ外していない仕事の数（一時停止の返事で知らせるため。数えるのは遅いが、受け口のスレッドで止めるとき・再開するときだけ）
         */
        int waiting() {
            int count = 0;
            for (Entry entry : entries) {
                if (!entry.removed.get()) {
                    count++;
                }
            }
            return count;
        }

        /**
         * 先頭の、まだ外していない仕事（終わった・取り消した仕事が先頭に残っていたら、ここで捨てる。メインスレッド）
         * 足すのは後ろだけで、前から外すのはメインスレッドだけなので、peek で見た仕事をそのまま poll で外せる
         */
        Entry head() {
            Entry entry;
            while ((entry = entries.peek()) != null && entry.removed.get()) {
                entries.poll();
            }
            return entry;
        }
    }

    /**
     * 片づけを待つ仕事と、取り消したときに数えた footprint（引くときも同じ値を使う。footprint を呼び直すと、値が変わったときに数がずれるため）
     */
    private static final class Released {
        final Job job;
        final long footprint;

        Released(Job job, long footprint) {
            this.job = job;
            this.footprint = footprint;
        }
    }

    // 送った接続が無い仕事（null。メインスレッドの中から足したものなど）の列の印。取り消さない
    private static final Object NO_OWNER = new Object();

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
    // 取り消して片づけを待っている仕事の多さ（footprint の合計）がこれに届いたら、新しい仕事を断る
    // （取り消すと数はすぐ減るが、片づけはメインスレッドの番が来るまで待つので、Minecraft が重い間に足しては止めるのを
    //   くり返されると、片づけ待ちがたまるため。いっぱいの列を 1 回止めても断らないよう、MAX_PENDING_JOBS の 2 倍）
    public static final long MAX_PENDING_RELEASE = 2L * MAX_PENDING_JOBS;
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
    // 接続ごとの列（鍵は送った接続。接続が無い仕事は NO_OWNER）。接続が切れたら forget で外す
    private final ConcurrentHashMap<Object, Lane> lanes = new ConcurrentHashMap<>();
    // 次に足す仕事の通し番号
    private final AtomicLong nextSeq = new AtomicLong();
    // 取り消した仕事（メインスレッドで、ふつうは次の仕事を進める前に release を呼ぶ）と、その footprint の合計
    private final ConcurrentLinkedQueue<Released> toRelease = new ConcurrentLinkedQueue<>();
    private final AtomicLong pendingRelease = new AtomicLong();
    // 進めている途中の仕事（step が false を返した仕事。終わるか取り消すまで、これを続ける。メインスレッドだけが触る）
    private Entry current;
    // 前の回に pump が例外で抜けたときの途中の仕事（同じ仕事で続けて例外が出たら、やめるため。メインスレッドだけが触る）
    private Entry failedEntry;
    // pump が続けて例外で抜けた回数（ログを最初の 1 回だけ詳しく出すため。メインスレッドだけが触る）
    private int pumpErrors;
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

    // 列の処理が思わぬ例外で抜けたあと、頼み直すまでの間（ミリ秒。pump の説明を見る）。
    // 試験だけが、同じパッケージの中から短くする（本物の 1 秒を待つ節が多く、試験に時間がかかるため）
    volatile long pumpRetryMillis = 1000;

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
        if (pendingJobs.get() >= MAX_PENDING_JOBS || pendingRelease.get() >= MAX_PENDING_RELEASE) {
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
     * 列にたまっている小さな仕事の重さ（試験で、重さが 1 回だけ引かれたかを見るため）
     */
    long pendingSmallWeight() {
        return pendingSmallWeight.get();
    }

    /**
     * 列にたまっている仕事の数（試験で見るため）
     */
    int pendingJobs() {
        return pendingJobs.get();
    }

    /**
     * 取り消して片づけを待っている仕事の footprint の合計（試験で見るため）
     */
    long pendingRelease() {
        return pendingRelease.get();
    }

    /**
     * 接続ごとの列の数（接続が無い仕事の列も入れる。試験で、切れた接続の列が残らないかを見るため）
     */
    int laneCount() {
        return lanes.size();
    }

    /**
     * 列のつながりに残っている仕事の数（外したのに、まだつながりに残っているものも入れる。試験で見るため。数えるのは遅い）
     */
    int linkedEntries() {
        int count = 0;
        for (Lane lane : lanes.values()) {
            count += lane.entries.size();
        }
        return count;
    }

    /**
     * 進める処理をメインスレッドに頼んでいないか（試験で、列の処理が終わったかを見るため）
     */
    boolean idle() {
        return !pumpRequested.get();
    }

    /**
     * 列の最後に仕事を足す（どのスレッドから呼んでもよい）
     * 仕事の数が上限に届いていたら、足さずに false を返す（チャット・テレポートなど、
     * hasRoomFor を確かめない仕事も、これでたまり続けないようにする）
     *
     * @param owner 送った接続（止まった・切れたときに cancel で外す。null なら外さない）
     * @return 足したら true
     */
    public boolean offer(Job job, Object owner) {
        if (stopping.getAsBoolean()) {
            return false;
        }
        // 片づけを待つ仕事が多すぎる（止めるのを短い間にくり返された）ときも断る
        if (pendingRelease.get() >= MAX_PENDING_RELEASE) {
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
        // 同じ接続の仕事は、いつも同じ受け口のスレッドから足すので、その列の中は番号の順に並ぶ
        // （ほかの接続とは、番号を取ってから列に入れるまでの間に前後することがあるが、別のタブどうしなので順番は決まっていない）
        Lane lane = lanes.computeIfAbsent(owner == null ? NO_OWNER : owner, key -> new Lane());
        lane.entries.add(new Entry(job, nextSeq.getAndIncrement(), lane));
        requestPump();
        return true;
    }

    /**
     * owner が送った仕事を、列から全部外す（どのスレッドから呼んでもよい。Scratch の止めるボタン・接続が切れたとき）
     * いま進めている途中の仕事（大きな「埋める」など）も外し、そこでやめる（置いたところまでは残る）。
     * 呼んだときに列にある仕事だけを外す（そのあとに同じ接続から届いた仕事は外さない。緑の旗は、止めてから始めるため）
     *
     * @return 外した仕事の数
     */
    public int cancel(Object owner) {
        if (owner == null) {
            return 0;
        }
        Lane lane = lanes.get(owner);
        int cancelled = cancelLane(lane);
        // 一時停止していたら、やめる（止めるボタン・緑の旗のあとに届いた仕事は、すぐ進める。
        // 列にあった仕事は全部外したので、あきらめるまでの時間を数え直させる仕事は無い）
        if (lane != null) {
            lane.paused = false;
        }
        return cancelled;
    }

    /**
     * owner の列を一時停止する（Scratch の一時停止ボタン）。止めている間は、その接続の仕事を進めない
     * （進めている途中の大きな「埋める」などもそこで止まり、再開したら続きから進める）。止めている間に届いた仕事は列に並ぶ。
     * ほかの接続の仕事は進む。止めている列の仕事も、重さ・仕事の数の上限や「周りをきれいにする」の途中の印には数えたまま。
     * 列がまだ無ければ作る（止めてから届いた仕事も止めるため）。止めている印は resume・cancel で下ろし、forget で列ごと外す。
     * その接続の offer と同じスレッド（受け口のスレッド）から呼ぶこと（別のスレッドから呼ぶと、forget で外したあとに列を作り直して、
     * 誰にも外されない列が残ることがある）
     *
     * @return その列に並んでいる仕事の数（null なら 0）
     */
    public int pause(Object owner) {
        if (owner == null) {
            return 0;
        }
        Lane lane = lanes.computeIfAbsent(owner, key -> new Lane());
        lane.paused = true;
        return lane.waiting();
    }

    /**
     * owner の列の一時停止をやめ、続きを進める（どのスレッドから呼んでもよい。止めていなければ何もしない）
     * その列の再開した数を先に足してから印を下ろす（メインスレッドは、印が下りたのを見たら、再開した数の変わりも必ず見える。
     * その列の仕事を進める前にそれを見て、その列を止めた状態で見ていたら Job.resumed を呼ぶ）。そのあと、進める処理を頼む
     *
     * @return その列に並んでいる仕事の数（列が無ければ 0）
     */
    public int resume(Object owner) {
        if (owner == null) {
            return 0;
        }
        Lane lane = lanes.get(owner);
        if (lane == null) {
            return 0;
        }
        if (lane.paused) {
            lane.resumes.incrementAndGet();
            lane.paused = false;
            requestPump();
        }
        return lane.waiting();
    }

    /**
     * 接続が切れたので、その接続の列を外し、残っていた仕事も全部外す
     * その接続の offer と同じスレッド（その接続の受け口のスレッド。受け取りのくり返しを抜けたあと）から呼ぶこと。
     * 別のスレッドから呼ぶと、offer が列を取ってから仕事を入れるまでの間に列を外して、
     * 入れた仕事が誰にも進められず、重さも減らないまま残ることがある
     *
     * @return 外した仕事の数
     */
    public int forget(Object owner) {
        if (owner == null) {
            return 0;
        }
        return cancelLane(lanes.remove(owner));
    }

    /**
     * 1 つの接続の列の仕事を、いま入っている分だけ全部外す（lane が null なら何もしない）
     * 印を立てて重さを引き、cancel を呼ぶ。チケットを外すなどの片づけ（release）は、メインスレッドで（ふつうは次の仕事より先に）する
     * （列のつながりから外すのも、メインスレッドが片づけるとき。片づけを待つ間は pendingRelease に数える）
     */
    private int cancelLane(Lane lane) {
        if (lane == null) {
            return 0;
        }
        int count = 0;
        for (Entry entry : lane.entries) {
            if (!entry.removed.compareAndSet(false, true)) {
                continue;
            }
            subtract(entry.job.weight());
            count++;
            try {
                entry.job.cancel();
            } catch (Exception e) {
                // 片づけの失敗で、ほかの仕事を外すのをやめない
                MinecraftEduMod.LOGGER.error("BuildQueue cancel error", e);
            }
            long footprint;
            try {
                footprint = Math.max(1, entry.job.footprint());
            } catch (Exception e) {
                // 数えられなければ 1 と見る（片づけを頼み損ねないことのほうが大事）
                MinecraftEduMod.LOGGER.error("BuildQueue footprint error", e);
                footprint = 1;
            }
            pendingRelease.addAndGet(footprint);
            toRelease.add(new Released(entry.job, footprint));
        }
        if (count > 0) {
            // 片づけをメインスレッドで早めにする（列が空でも）
            requestPump();
        }
        return count;
    }

    /**
     * 列から外した仕事の重さを、たまっている量から引く
     */
    private void subtract(long weight) {
        pendingWeight.addAndGet(-weight);
        if (isSmall(weight)) {
            pendingSmallWeight.addAndGet(-weight);
        }
        pendingJobs.decrementAndGet();
    }

    /**
     * 列を空にして、タイマーを止める（サーバーが止まるとき。チケットはワールドといっしょに捨てられるので、片づけは呼ばない）
     */
    public void clear() {
        lanes.clear();
        toRelease.clear();
        pendingRelease.set(0);
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
     * 思わぬ例外で抜けても、列が止まったままにならないよう、少しあけて頼み直す
     * （進める処理を頼んだ印が立ったままになると、そのあと足した仕事も進まなくなるため。毎回すぐ頼み直すと、
     *   同じ例外がくり返すときにメインスレッドを占めるので、1 秒あける）。
     * 進めている途中の仕事（current）はそのまま残す（消すと、続きを始める前に、ほかの接続の仕事に追い越させてしまうため）。
     * 同じ途中の仕事で 2 回続けて例外が出たら、その仕事は step の例外と同じようにやめる（列全体が 1 秒に 1 回ずつ止まり続けないように）
     */
    private void pump() {
        try {
            pumpOnce();
            failedEntry = null;
            pumpErrors = 0;
        } catch (RuntimeException e) {
            Entry failing = current;
            long retryMillis = pumpRetryMillis;
            // スタックトレースは続いた最初の 1 回だけ（同じ例外がくり返しても、ログが毎秒増えないように）
            if (++pumpErrors == 1) {
                MinecraftEduMod.LOGGER.error("BuildQueue pump error", e);
            } else {
                MinecraftEduMod.LOGGER.debug("BuildQueue pump error again (" + pumpErrors + "): " + e);
            }
            if (failing != null && failing == failedEntry) {
                MinecraftEduMod.LOGGER.warn("BuildQueue: dropped a job after repeated errors: " + e);
                dropFailing(failing);
                failedEntry = null;
                // 数え直す（このあと別の不具合で出た例外も、スタックトレースを latest.log に残すため）
                pumpErrors = 0;
                retryMillis = 1;  // やめた仕事のあとは、すぐ続ける
            } else {
                failedEntry = failing;
            }
            try {
                timer.schedule(this::postPump, retryMillis, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException rejected) {
                // サーバーが止まって、タイマーも止めたあと
                pumpRequested.set(false);
            }
        }
    }

    /**
     * 例外が続いた途中の仕事をやめる（メインスレッド。外す印を取れたときだけ、重さを引いて片づける。取り消されていれば、取り消した側が片づける）
     */
    private void dropFailing(Entry failing) {
        current = null;
        if (failing.removed.compareAndSet(false, true)) {
            subtract(failing.job.weight());
            abandon(failing.job);
        }
    }

    /**
     * 例外でやめた仕事を、取り消したときと同じように片づける（メインスレッド。外す印を取れた側が 1 回だけ呼ぶ）
     * cancel で印（「周りをきれいにする」の途中の印など）を下ろしてから、release でチケットを外す
     * （release だけだと、「周りをきれいにする」の印が立ったままになり、再起動まで次の「周りをきれいにする」を断り続けるため）
     */
    private static void abandon(Job job) {
        try {
            job.cancel();
        } catch (Exception cancelError) {
            MinecraftEduMod.LOGGER.error("BuildQueue cancel error", cancelError);
        }
        try {
            job.release();
        } catch (Exception releaseError) {
            MinecraftEduMod.LOGGER.error("BuildQueue release error", releaseError);
        }
    }

    /**
     * pump の中身（決まった時間だけ仕事を進める。メインスレッド）
     */
    private void pumpOnce() {
        // 次の tick が始まる少し前まで（遅れが出ないように）。getNextTickTime は Util.getMillis と同じ物差し
        long untilNextTick = server.getNextTickTime() - Util.getMillis() - TICK_MARGIN_MILLIS;
        long sliceMillis = Math.max(MIN_SLICE_MILLIS, Math.min(MAX_SLICE_MILLIS, untilNextTick));
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(sliceMillis);
        // 取り消した仕事の片づけを、先に済ませる
        releaseCancelled();
        Entry entry;
        // 1 人用のワールドが止まっている（ポーズ中の）間に、Minecraft が持つチャンクが増えすぎたら、動き出すまで進めない
        // （ピストンなど、Minecraft が置いたブロックの周りを読むときは、PauseWatch の予算を通らずにチャンクを読み込むため。1 つ進めるごとに見る）
        while ((entry = nextEntry()) != null && !PauseWatch.overLimit(server)) {
            // この仕事の列を一時停止から再開していて、止めた状態を見ていたら、進める前に知らせる（あきらめるまでの時間を数え直させる。
            // nextEntry が、この列の印が下りたのを見たあとに読むので、再開した数は必ず見える。同じ回のうちに、
            // ほかの列の仕事を進めている間に再開した列も、ここで見る）
            noteResumed(entry);
            Job job = entry.job;
            boolean done;
            try {
                done = job.step(deadline);
            } catch (Exception e) {
                // 1 つの仕事のエラーで列が止まらないよう、その仕事はやめて次へ進む
                MinecraftEduMod.LOGGER.error("BuildQueue job error", e);
                done = true;
                // 付けたチケットなどが残らないよう片づける。外す印をここで取れたときだけ（取り消されていれば、取り消した側が片づけを頼む。
                // 片づけは仕事ごとに 1 回だけ。重さもここで引くので、下の印の取り合いは負ける）
                if (entry.removed.compareAndSet(false, true)) {
                    subtract(job.weight());
                    abandon(job);
                }
            }
            if (!done) {
                // 時間が来たか、チャンクの読み込みを待っている。この回はここまで。次の回もこの仕事から続ける
                current = entry;
                // 進めている間に取り消されていたら、その片づけは次の回を待たずにする
                // （先読みより先にする。取り消した仕事が列に残ったまま、先読みの見る件数を使わないように）
                releaseCancelled();
                // 読み込みを待っているなら、後ろの仕事のチャンクも先に頼んでおく（置く順番は変えない）
                if (job.waitingForLoad()) {
                    prefetchAhead(entry);
                }
                break;
            }
            current = null;
            // 重さは、外した側が 1 回だけ引く（列のつながりから外すのは、次に先頭を見たとき）
            if (entry.removed.compareAndSet(false, true)) {
                subtract(job.weight());
            }
            // 進めている間に取り消された仕事の片づけを、次の仕事の前に済ませる
            releaseCancelled();
            if (System.nanoTime() - deadline >= 0) {
                break;
            }
        }

        if (nextEntry() == null) {
            pumpRequested.set(false);
            // 上で空と見てから印を下ろすまでの間に足された仕事・取り消した仕事を、取りこぼさない
            if (nextEntry() != null || !toRelease.isEmpty()) {
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
     * 取り消した仕事の片づけ（release）をし、全部の列の先頭から、取り消した仕事を外す（メインスレッド）
     * 外すのは、ほかの接続の仕事が進めている途中（current）でも、ここで済ませる
     * （nextEntry は途中の仕事があるとほかの列の先頭を見ないので、ここで外さないと、ほかのタブで送っては止めるのをくり返したとき、
     *   外した仕事が列のつながりに残り続け、仕事の数の上限も効かずにたまっていくため）。
     * 取り消しは、その時点で列にある仕事に全部印を付けるので、取り消した仕事はいつも列の先頭に続けて並び、head で全部外れる
     */
    private void releaseCancelled() {
        boolean released = false;
        Released item;
        while ((item = toRelease.poll()) != null) {
            released = true;
            try {
                item.job.release();
            } catch (Exception e) {
                // 1 つの片づけの失敗で、ほかの片づけと列を止めない
                MinecraftEduMod.LOGGER.error("BuildQueue release error", e);
            }
            pendingRelease.addAndGet(-item.footprint);
        }
        if (released) {
            for (Lane lane : lanes.values()) {
                lane.head();
            }
        }
    }

    /**
     * entry の列を、前に見たあとで一時停止から再開していて、メインスレッドがその列を止めた状態で見ていたら、
     * その仕事の Job.resumed を呼ぶ（メインスレッド。止めた状態を見ていなければ、仕事は止まっていないので呼ばない）
     * （再開した列で、次に進めるのはいつもその列の先頭なので、止める前に読み込みを待ち始めていた仕事に届く。
     *   ほかの列の仕事の、あきらめるまでの時間には触らない）。
     * 知られた限り: 止めるが、メインスレッドが仕事を進めている最中にちょうど届き、その回の終わりの nextEntry が止めた状態を見て、
     * 次の回の前に再開すると、仕事は 1 回も飛ばされていないのに知らせる。これを狙って送り続けると、読み込めない仕事の
     * あきらめるまでの時間を戻し続けられる（ほんの短い間を狙って送り続ける必要があり、一時停止ボタンを押すだけでは起きない。
     * 本当に飛ばした回があったかを数える作りは、列が空いて休んでいる間の一時停止と見分けにくいので、入れていない）
     */
    private void noteResumed(Entry entry) {
        Lane lane = entry.lane;
        long now = lane.resumes.get();
        if (now == lane.seenResumes) {
            // 再開していないのに、この列の仕事を進める（止めるボタンで止めた状態が終わったときなど）。止めた状態を見た印は、もう古い
            // （止めるボタンのあと、一度も進めていない仕事が先頭のうちに止めて再開すると、古い印のまま知らせることがあるが、
            //   その仕事はまだ何も持っていないので、知らせても何も変わらない）
            lane.pauseSeen = false;
            return;
        }
        lane.seenResumes = now;
        if (!lane.pauseSeen) {
            // 止めた状態を一度も見ないうちに再開した（仕事は止まっていないので、知らせない）
            return;
        }
        lane.pauseSeen = false;
        try {
            entry.job.resumed();
        } catch (Exception e) {
            // 知らせの失敗で列を止めない（進めると、止めていた間を数えて、あきらめることがあるだけ）
            MinecraftEduMod.LOGGER.error("BuildQueue resumed error", e);
        }
    }

    /**
     * 次に進める仕事（無ければ null。メインスレッド）
     * 進めている途中の仕事があれば、終わるか取り消すまでそれを続ける（前の 1 本の列と同じ。
     * 番号を取ってから列に入れるまでの間に、ほかの接続の番号の小さい仕事が入っても、途中の仕事を追い越させない）。
     * 無ければ、各接続の列の先頭のうち、番号がいちばん小さいもの（いちばん先に届いたもの）。
     * 接続の数は MAX_CLIENTS（8）と接続が無い分の 1 つまでなので、毎回すべての列の先頭を比べる。
     * 一時停止している列は見ない。進めている途中の仕事が一時停止した列のものなら、外さずにそこで止める
     * （列の先頭に残るので、再開したら続きから進める。止めている間は、ほかの接続の仕事を進める）
     */
    private Entry nextEntry() {
        Entry running = current;
        if (running != null) {
            if (!running.removed.get() && !running.lane.paused) {
                return running;
            }
            current = null;
        }
        Entry best = null;
        for (Lane lane : lanes.values()) {
            if (lane.paused) {
                // 止めた状態を見た（止めた列の途中の仕事を上で手放したときも、ここを通る）。再開したら、続きを進める前に知らせる
                lane.pauseSeen = true;
                continue;
            }
            Entry head = lane.head();
            if (head != null && (best == null || head.seq < best.seq)) {
                best = head;
            }
        }
        return best;
    }

    /**
     * 先頭の仕事のすぐ後ろから、別々のチャンクを（先頭のチャンクも入れて）PREFETCH_CHUNKS 個まで見て、
     * 先に読み込みを頼む（読み込み済みなら付け直す。メインスレッド）。見るのは PREFETCH_SCAN_JOBS 件まで
     * どの接続の仕事も、届いた順（番号の順）に合わせて見る（進める順と同じ順に頼むため。同じ接続の仕事だけを見ると、
     * 1 人用のワールドが止まっている間に、その接続の分で PauseWatch の予算を使い切り、次に進めるほかの接続の仕事が読み込めないことがある）
     */
    @SuppressWarnings("unchecked")
    private void prefetchAhead(Entry head) {
        try {
            LongSet seen = new LongOpenHashSet();
            long headChunk = head.job.prefetchChunk();
            if (headChunk != NO_PREFETCH) {
                seen.add(headChunk);
            }
            // 接続ごとの列を、番号の小さい順にまぜて見る（列の中は番号の順に並んでいる）。
            // 一時停止している列は見ない（進めない仕事のために、チャンクの読み込みと PauseWatch の予算を使わないように）
            Lane[] laneArray = lanes.values().stream().filter(lane -> !lane.paused).toArray(Lane[]::new);
            Iterator<Entry>[] iterators = new Iterator[laneArray.length];
            Entry[] nexts = new Entry[laneArray.length];
            for (int i = 0; i < laneArray.length; i++) {
                iterators[i] = laneArray[i].entries.iterator();
                nexts[i] = iterators[i].hasNext() ? iterators[i].next() : null;
            }
            int scanned = 0;
            while (seen.size() < PREFETCH_CHUNKS) {
                int min = -1;
                for (int i = 0; i < nexts.length; i++) {
                    if (nexts[i] != null && (min < 0 || nexts[i].seq < nexts[min].seq)) {
                        min = i;
                    }
                }
                if (min < 0) {
                    break;
                }
                Entry entry = nexts[min];
                nexts[min] = iterators[min].hasNext() ? iterators[min].next() : null;
                // 先頭（いま待っている仕事）は、自分で頼んでいる
                if (entry == head) {
                    continue;
                }
                if (scanned++ >= PREFETCH_SCAN_JOBS) {
                    break;
                }
                if (entry.removed.get()) {
                    continue;
                }
                Job job = entry.job;
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
