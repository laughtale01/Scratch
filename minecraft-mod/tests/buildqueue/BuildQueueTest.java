package com.github.minecraftedu.commands;

import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BuildQueue の試験（本物の BuildQueue を、偽物の MinecraftServer の上で動かす）
 * 接続ごとの列の順番、自分の接続の仕事だけを外すこと、重さを 1 回だけ引くこと、
 * 取り消した仕事の片づけ（release）が次の仕事より先に動くこと、途中の仕事を追い越させないこと、
 * 先読みがどの接続の仕事も届いた順に見ることを確かめる
 */
public class BuildQueueTest {

    private static int passed;
    private static int failed;

    // 起きたこと（step:名前・release:名前・cancel:名前・prefetch:名前）を順に書く
    // 仕事が呼ばれた順のログ
    static final List<String> log = Collections.synchronizedList(new ArrayList<>());

    /**
     * 試験用の仕事（steps 回目の step で終わる。それまでは false を返す）
     */
    static class TestJob implements BuildQueue.Job {
        final String name;
        final int steps;
        final long weight;
        long chunk = BuildQueue.NO_PREFETCH;
        boolean waiting;
        Runnable duringStep;
        RuntimeException throwOnStep;
        RuntimeException throwOnCancel;
        RuntimeException throwOnRelease;
        RuntimeException throwOnFootprint;
        Runnable duringCancel;
        boolean throwOnWaitingOnce;
        boolean throwOnWaitingAlways;
        // waitingForLoad の何回目で投げるか（1 から数える）
        final Set<Integer> throwOnWaitingAt = new HashSet<>();
        int waitingCalls;
        volatile long footprint = 1;
        volatile int stepped;
        volatile int released;
        volatile int resumedCount;

        TestJob(String name) {
            this(name, 1, 1);
        }

        TestJob(String name, int steps, long weight) {
            this.name = name;
            this.steps = steps;
            this.weight = weight;
        }

        @Override
        public boolean step(long deadlineNanos) {
            stepped++;
            log.add("step:" + name);
            if (duringStep != null) {
                duringStep.run();
            }
            if (throwOnStep != null) {
                throw throwOnStep;
            }
            return stepped >= steps;
        }

        @Override
        public long weight() {
            return weight;
        }

        @Override
        public boolean waitingForLoad() {
            waitingCalls++;
            if (throwOnWaitingOnce || throwOnWaitingAlways || throwOnWaitingAt.contains(waitingCalls)) {
                throwOnWaitingOnce = false;
                throw new IllegalStateException("試験で投げる例外（列の処理の守り）");
            }
            return waiting && stepped < steps;
        }

        @Override
        public long footprint() {
            if (throwOnFootprint != null) {
                throw throwOnFootprint;
            }
            return footprint;
        }

        @Override
        public long prefetchChunk() {
            return chunk;
        }

        @Override
        public void prefetch() {
            log.add("prefetch:" + name);
        }

        @Override
        public void cancel() {
            log.add("cancel:" + name);
            if (duringCancel != null) {
                duringCancel.run();
            }
            if (throwOnCancel != null) {
                throw throwOnCancel;
            }
        }

        @Override
        public void resumed() {
            resumedCount++;
            log.add("resumed:" + name);
        }

        @Override
        public void release() {
            released++;
            log.add("release:" + name);
            if (throwOnRelease != null) {
                throw throwOnRelease;
            }
        }
    }

    /**
     * 試験用の列（思わぬ例外のあとの頼み直しを 20 ms にする。本物の 1 秒を待つ節が多く、試験に時間がかかるため。
     * 1 秒あくことは 17 節が本物の値で確かめる）
     */
    static BuildQueue newQueue(MinecraftServer server) {
        BuildQueue queue = new BuildQueue(server, () -> false);
        queue.pumpRetryMillis = 20;
        return queue;
    }

    static void check(String name, boolean ok, Object detail) {
        if (ok) {
            passed++;
            System.out.println("  ok     " + name);
        } else {
            failed++;
            System.out.println("  NG     " + name + "  → " + detail);
        }
    }

    /**
     * 列の処理が終わるまで、メインスレッドの待ち列を流す（タイマーが頼む続きも待つ。5 秒まで）
     * 終わらなければ NG に数えて戻る（試験全体を止めず、そのあとの確かめと節も流すため）
     * 列の処理が例外をメインスレッドの外まで投げたときも、NG に数えて戻る（本物の Minecraft では、列の処理がそこで止まったままになる）
     */
    static void runUntilIdle(MinecraftServer server, BuildQueue queue) throws InterruptedException {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            boolean ran;
            try {
                ran = server.runOne(20);
            } catch (RuntimeException e) {
                check("列の処理が、例外をメインスレッドの外まで投げない", false, e);
                return;
            }
            if (!ran && queue.idle() && !server.hasTasks()) {
                return;
            }
        }
        check("列の処理が 5 秒で終わる", false, counts(queue));
    }

    static long count(List<String> lines, String name) {
        return lines.stream().filter(name::equals).count();
    }

    static List<String> only(String prefix) {
        List<String> out = new ArrayList<>();
        synchronized (log) {
            for (String line : log) {
                if (line.startsWith(prefix)) {
                    out.add(line.substring(prefix.length()));
                }
            }
        }
        return out;
    }

    static boolean empty(BuildQueue queue) {
        return queue.pendingJobs() == 0 && queue.pendingWeight() == 0 && queue.pendingSmallWeight() == 0
            && queue.pendingRelease() == 0;
    }

    static String counts(BuildQueue queue) {
        return "jobs=" + queue.pendingJobs() + " weight=" + queue.pendingWeight() + " small=" + queue.pendingSmallWeight()
            + " release=" + queue.pendingRelease();
    }

    public static void main(String[] args) throws Exception {
        Object a = new Object();
        Object b = new Object();

        System.out.println("--- 1. 2 つの接続と接続の無い仕事は、届いた順に進む");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            queue.offer(new TestJob("A1"), a);
            queue.offer(new TestJob("B1"), b);
            queue.offer(new TestJob("A2"), a);
            queue.offer(new TestJob("N1"), null);
            queue.offer(new TestJob("B2", 1, 5000), b);
            check("足したあとの重さ", queue.pendingJobs() == 5 && queue.pendingWeight() == 5004 && queue.pendingSmallWeight() == 4, counts(queue));
            runUntilIdle(server, queue);
            check("A1 B1 A2 N1 B2 の順", only("step:").equals(List.of("A1", "B1", "A2", "N1", "B2")), only("step:"));
            check("終わったら重さは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 2. 止めると自分の接続の仕事だけが外れ、片づけはほかの仕事より先");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            queue.offer(new TestJob("A1"), a);
            queue.offer(new TestJob("B1"), b);
            queue.offer(new TestJob("A2", 1, 5000), a);
            int cancelled = queue.cancel(a);
            check("A の 2 つを外す", cancelled == 2, cancelled);
            check("外したらすぐ重さが減る", queue.pendingJobs() == 1 && queue.pendingWeight() == 1 && queue.pendingSmallWeight() == 1, counts(queue));
            check("cancel はすぐ呼ぶ", only("cancel:").equals(List.of("A1", "A2")), only("cancel:"));
            check("release はまだ（メインスレッドで呼ぶ）", only("release:").isEmpty(), only("release:"));
            runUntilIdle(server, queue);
            check("B1 だけが進む", only("step:").equals(List.of("B1")), only("step:"));
            check("release は B1 より先", log.indexOf("release:A2") >= 0 && log.indexOf("release:A2") < log.indexOf("step:B1"), log);
            check("ほかの接続の取り消しは 0 個", queue.cancel(b) == 0 && queue.cancel(new Object()) == 0 && queue.cancel(null) == 0, "");
            check("終わったら重さは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 3. 途中の仕事を止めて同じ接続ですぐ始め直すと、古い片づけが新しい仕事より先に動く");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob f1 = new TestJob("F1", 100, 100_000);
            queue.offer(f1, a);
            server.runOne(100);  // 1 回進める（途中で止まる）
            int cancelled = queue.cancel(a);
            queue.offer(new TestJob("F2", 2, 100_000), a);
            runUntilIdle(server, queue);
            check("F1 を外した（1 個）", cancelled == 1, cancelled);
            check("F1 は外したあと進まない", f1.stepped == 1, f1.stepped);
            check("release:F1 は step:F2 より先", log.indexOf("release:F1") >= 0 && log.indexOf("release:F1") < log.indexOf("step:F2"), log);
            check("F2 は最後まで進む", only("step:").equals(List.of("F1", "F2", "F2")), only("step:"));
            check("終わったら重さは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 4. 進めている間に取り消されても、重さは 1 回だけ引く");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            // 終わる回に取り消される
            TestJob x = new TestJob("X", 1, 7);
            x.duringStep = () -> queue.cancel(a);
            // 途中の回に取り消される（そのあとは進めない）
            TestJob y = new TestJob("Y", 5, 9000);
            y.duringStep = () -> queue.cancel(b);
            queue.offer(x, a);
            queue.offer(y, b);
            queue.offer(new TestJob("Z"), null);
            runUntilIdle(server, queue);
            check("X は 1 回、Y も 1 回だけ進む", x.stepped == 1 && y.stepped == 1, x.stepped + "," + y.stepped);
            check("Z は進む", only("step:").contains("Z"), only("step:"));
            check("X・Y とも release が呼ばれる", only("release:").containsAll(List.of("X", "Y")), only("release:"));
            check("release:Y は step:Z より先", log.indexOf("release:Y") < log.indexOf("step:Z"), log);
            check("重さは 0（マイナスにならない）", empty(queue), counts(queue));
        }

        System.out.println("--- 5. 接続が切れたら列ごと外し、同じ印で足した仕事は新しい列で進む");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            queue.offer(new TestJob("A1"), a);
            queue.offer(new TestJob("A2"), a);
            int forgot = queue.forget(a);
            check("2 個外す", forgot == 2, forgot);
            check("もう一度は 0 個", queue.forget(a) == 0 && queue.forget(null) == 0, "");
            queue.offer(new TestJob("A3"), a);
            runUntilIdle(server, queue);
            check("A3 だけが進む", only("step:").equals(List.of("A3")), only("step:"));
            check("終わったら重さは 0", empty(queue), counts(queue));
            queue.forget(a);
            check("切れた接続の列は消える", queue.laneCount() == 0, queue.laneCount());
            for (int i = 0; i < 100; i++) {
                Object owner = new Object();
                queue.offer(new TestJob("C" + i), owner);
                queue.forget(owner);
            }
            check("つないでは切るを 100 回くり返しても、列は残らない", queue.laneCount() == 0, queue.laneCount());
            runUntilIdle(server, queue);
            check("その仕事は進まず、重さは 0", only("step:").equals(List.of("A3")) && empty(queue), only("step:") + " " + counts(queue));
        }

        System.out.println("--- 6. 途中の仕事は、あとから入った番号の小さい仕事に追い越されない");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            queue.offer(new TestJob("A1", 3, 100_000), a);
            server.runOne(100);  // A1 を 1 回進める（途中）
            // 番号を取ってから列に入れるまでの間に追い越された形を作る（通し番号を 0 に戻してから B1 を足す）
            Field field = BuildQueue.class.getDeclaredField("nextSeq");
            field.setAccessible(true);
            ((AtomicLong) field.get(queue)).set(-1);
            queue.offer(new TestJob("B1"), b);
            runUntilIdle(server, queue);
            check("A1 を終えてから B1", only("step:").equals(List.of("A1", "A1", "A1", "B1")), only("step:"));
            check("終わったら重さは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 7. 先頭が読み込みを待つ間の先読みは、どの接続の仕事も届いた順に見る");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob head = new TestJob("H", 2, 1);
            head.waiting = true;
            head.chunk = 1;
            TestJob b1 = new TestJob("B1");
            b1.chunk = 2;
            TestJob a2 = new TestJob("A2");
            a2.chunk = 3;
            TestJob a3 = new TestJob("A3");
            a3.chunk = 2;  // B1 と同じチャンクは頼み直さない
            queue.offer(head, a);
            queue.offer(b1, b);
            queue.offer(a2, a);
            queue.offer(a3, a);
            server.runOne(100);  // H が待つ（1 回目）
            check("B1 → A2 の順に先読みし、同じチャンクの A3 と先頭は頼まない", only("prefetch:").equals(List.of("B1", "A2")), only("prefetch:"));
            runUntilIdle(server, queue);
            check("進む順は H B1 A2 A3", only("step:").equals(List.of("H", "H", "B1", "A2", "A3")), only("step:"));
            check("終わったら重さは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 8. 別のスレッドから足しながら止めても、重さは合い、片づけは取り消した仕事ごとに 1 回");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            List<TestJob> sent = Collections.synchronizedList(new ArrayList<>());
            AtomicReference<Throwable> senderError = new AtomicReference<>();
            int[] cancelledTotal = {0};
            Thread sender = new Thread(() -> {
                try {
                    for (int i = 0; i < 20_000; i++) {
                        TestJob job = new TestJob("S" + i, 1, (i % 7 == 0) ? 5000 : 1);
                        sent.add(job);
                        queue.offer(job, a);
                        if (i % 1000 == 999) {
                            cancelledTotal[0] += queue.cancel(a);
                        }
                    }
                } catch (Throwable t) {
                    senderError.set(t);
                }
            });
            sender.start();
            while (sender.isAlive()) {
                server.runOne(1);
            }
            sender.join();
            cancelledTotal[0] += queue.cancel(a);
            runUntilIdle(server, queue);
            check("送る側のスレッドが最後まで送った（例外なし）", senderError.get() == null && sent.size() == 20_000,
                senderError.get() + " sent=" + sent.size());
            check("最後は重さが 0", empty(queue), counts(queue));
            int steppedJobs = 0;
            int releasedJobs = 0;
            // ちょうど終わったところで取り消した仕事（進めたうえに、取り消した数にも入る。片づけは呼ばれるが何もしない）
            int both = 0;
            boolean once = true;
            for (TestJob job : sent) {
                steppedJobs += job.stepped > 0 ? 1 : 0;
                releasedJobs += job.released > 0 ? 1 : 0;
                both += job.stepped > 0 && job.released > 0 ? 1 : 0;
                once &= job.released <= 1 && job.stepped <= 1 && !(job.stepped > 0 && job.released > 0 && log.indexOf("release:" + job.name) < log.indexOf("step:" + job.name));
            }
            check("進めた数 + 取り消した数 − 両方に入った数 = 送った数", steppedJobs + cancelledTotal[0] - both == 20_000,
                "stepped=" + steppedJobs + " cancelled=" + cancelledTotal[0] + " both=" + both);
            check("片づけの数 = 取り消した数（どの仕事も 1 回まで、片づけたあとは進めない）", releasedJobs == cancelledTotal[0] && once,
                "released=" + releasedJobs + " cancelled=" + cancelledTotal[0] + " once=" + once);
            check("列のつながりにも残っていない", queue.linkedEntries() == 0, queue.linkedEntries());
        }

        System.out.println("--- 9. ほかの接続の仕事を進めている途中でも、送っては止めるのをくり返して、取り消した仕事が残り続けない");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob longJob = new TestJob("LONG", 1_000_000, 100_000);
            queue.offer(longJob, a);
            server.runOne(100);  // LONG が途中になる（current）
            for (int round = 0; round < 5; round++) {
                for (int i = 0; i < 20_000; i++) {
                    queue.offer(new TestJob("B" + round), b);
                }
                queue.cancel(b);
                // 列の処理を何回か流す（LONG は終わらないので、空くまでは待たない）
                for (int k = 0; k < 5 && server.runOne(50); k++) {
                }
            }
            check("取り消した B の仕事は、列のつながりから外れている（LONG だけ）", queue.linkedEntries() == 1, queue.linkedEntries());
            check("LONG を進め続けている", longJob.stepped > 1, longJob.stepped);
            check("B の仕事は 1 つも進んでいない", only("step:").stream().allMatch("LONG"::equals), "");
            queue.cancel(a);
            runUntilIdle(server, queue);
            check("LONG を取り消したら、片づけは 1 回で、列は空", longJob.released == 1 && empty(queue) && queue.linkedEntries() == 0,
                "released=" + longJob.released + " " + counts(queue) + " linked=" + queue.linkedEntries());
        }

        System.out.println("--- 10. 進めている間に、別のスレッドから止める・接続が切れる");
        for (boolean byForget : new boolean[] {false, true}) {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch cancelled = new CountDownLatch(1);
            TestJob slow = new TestJob("SLOW", 3, 9000);
            slow.duringStep = () -> {
                started.countDown();
                try {
                    cancelled.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            };
            TestJob later = new TestJob("LATER");
            TestJob other = new TestJob("OTHER");
            queue.offer(slow, a);
            queue.offer(later, a);
            queue.offer(other, b);
            int[] count = {-1};
            Thread canceller = new Thread(() -> {
                try {
                    started.await(2, TimeUnit.SECONDS);
                    count[0] = byForget ? queue.forget(a) : queue.cancel(a);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    cancelled.countDown();
                }
            });
            canceller.start();
            runUntilIdle(server, queue);
            canceller.join();
            String how = byForget ? "切れた" : "止めた";
            check(how + ": 2 個外す", count[0] == 2, count[0]);
            check(how + ": SLOW は 1 回だけ進み、LATER は進まない", slow.stepped == 1 && later.stepped == 0, slow.stepped + "," + later.stepped);
            check(how + ": 片づけは SLOW・LATER とも 1 回", slow.released == 1 && later.released == 1, slow.released + "," + later.released);
            check(how + ": ほかの接続の OTHER は進む", other.stepped == 1 && other.released == 0, other.stepped + "," + other.released);
            check(how + ": 重さは 0、列のつながりも空", empty(queue) && queue.linkedEntries() == 0, counts(queue) + " linked=" + queue.linkedEntries());
        }

        System.out.println("--- 11. 途中の仕事だけを止めて、ほかに仕事が無いとき");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob only1 = new TestJob("ONLY", 5, 100_000);
            queue.offer(only1, a);
            server.runOne(100);
            queue.cancel(a);
            runUntilIdle(server, queue);
            check("片づけは 1 回、そのあとは進めない", only1.released == 1 && only1.stepped == 1, only1.released + "," + only1.stepped);
            check("列の処理は休み、重さは 0、つながりも空", queue.idle() && empty(queue) && queue.linkedEntries() == 0,
                counts(queue) + " linked=" + queue.linkedEntries());
        }

        System.out.println("--- 12. 仕事が例外を投げても、片づけは 1 回だけで、次の仕事へ進む");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob broken = new TestJob("BROKEN", 1, 5000);
            broken.throwOnStep = new IllegalStateException("試験で投げる例外");
            // 進めている間に取り消され、そのあと例外を投げる（片づけは取り消した側の 1 回だけ）
            TestJob brokenCancelled = new TestJob("BROKEN2", 1, 5000);
            brokenCancelled.duringStep = () -> queue.cancel(b);
            brokenCancelled.throwOnStep = new IllegalStateException("試験で投げる例外");
            TestJob next = new TestJob("NEXT");
            queue.offer(broken, a);
            queue.offer(brokenCancelled, b);
            queue.offer(next, null);
            runUntilIdle(server, queue);
            check("BROKEN の片づけは 1 回", broken.released == 1, broken.released);
            check("BROKEN2 の片づけも 1 回（2 回呼ばない）", brokenCancelled.released == 1, brokenCancelled.released);
            // 例外でやめた仕事も、取り消したときと同じく cancel を先に呼ぶ（「周りをきれいにする」の印を下ろすため。E1）
            check("BROKEN は cancel を 1 回呼んでから片づける", count(only("cancel:"), "BROKEN") == 1
                && log.indexOf("cancel:BROKEN") < log.indexOf("release:BROKEN"), log);
            check("BROKEN2 の cancel も 1 回（取り消した側だけ）", count(only("cancel:"), "BROKEN2") == 1, only("cancel:"));
            check("NEXT は進む", next.stepped == 1, next.stepped);
            check("重さは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 13. 9 と同じことを、別のスレッドから止める");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob longJob = new TestJob("LONG", 1_000_000, 100_000);
            queue.offer(longJob, a);
            server.runOne(100);
            AtomicReference<Throwable> senderError = new AtomicReference<>();
            Thread sender = new Thread(() -> {
                try {
                    for (int round = 0; round < 200; round++) {
                        for (int i = 0; i < 500; i++) {
                            queue.offer(new TestJob("B"), b);
                        }
                        queue.cancel(b);
                    }
                } catch (Throwable t) {
                    senderError.set(t);
                }
            });
            sender.start();
            while (sender.isAlive()) {
                server.runOne(1);
            }
            sender.join();
            for (int k = 0; k < 5 && server.runOne(50); k++) {
            }
            check("送る側のスレッドが最後まで送った", senderError.get() == null, senderError.get());
            check("取り消した B の仕事は、列のつながりから外れている（LONG だけ）", queue.linkedEntries() == 1, queue.linkedEntries());
            check("B の仕事は 1 つも進んでいない", only("step:").stream().allMatch("LONG"::equals), "");
            check("片づけ待ちは 0", queue.pendingRelease() == 0, queue.pendingRelease());
            queue.cancel(a);
            runUntilIdle(server, queue);
            check("最後は重さが 0、つながりも空", empty(queue) && queue.linkedEntries() == 0, counts(queue) + " linked=" + queue.linkedEntries());
        }

        System.out.println("--- 14. cancel・release が例外を投げても、ほかの仕事の片づけと列は止まらない");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob x = new TestJob("X");
            x.throwOnCancel = new IllegalStateException("試験で投げる例外（cancel）");
            TestJob y = new TestJob("Y");
            y.throwOnRelease = new IllegalStateException("試験で投げる例外（release）");
            TestJob z = new TestJob("Z");
            TestJob w = new TestJob("W");
            queue.offer(x, a);
            queue.offer(y, a);
            queue.offer(z, a);
            queue.offer(w, b);
            int cancelled = queue.cancel(a);
            runUntilIdle(server, queue);
            check("3 個外す（cancel の例外で止まらない）", cancelled == 3, cancelled);
            check("片づけは X・Y・Z とも 1 回", x.released == 1 && y.released == 1 && z.released == 1, x.released + "," + y.released + "," + z.released);
            check("ほかの接続の W は進む", w.stepped == 1, w.stepped);
            check("重さ・片づけ待ちは 0、つながりも空", empty(queue) && queue.pendingRelease() == 0 && queue.linkedEntries() == 0,
                counts(queue) + " release=" + queue.pendingRelease() + " linked=" + queue.linkedEntries());
            // step の例外のあとの片づけが例外を投げても、次へ進む
            log.clear();
            TestJob e = new TestJob("E");
            e.throwOnStep = new IllegalStateException("試験で投げる例外（step）");
            e.throwOnRelease = new IllegalStateException("試験で投げる例外（release）");
            TestJob after = new TestJob("AFTER");
            queue.offer(e, a);
            queue.offer(after, a);
            runUntilIdle(server, queue);
            check("step の例外のあとの片づけが例外を投げても、次の仕事は進む", e.released == 1 && after.stepped == 1 && empty(queue),
                e.released + "," + after.stepped + " " + counts(queue));
        }

        System.out.println("--- 15. 取り消しの途中（印を立ててから片づけを頼むまで）に、次の仕事が進んでも、片づけは 1 回");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            CountDownLatch inCancel = new CountDownLatch(1);
            CountDownLatch letGo = new CountDownLatch(1);
            TestJob f1 = new TestJob("F1", 5, 100_000);
            f1.duringCancel = () -> {
                inCancel.countDown();
                try {
                    letGo.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            };
            TestJob n = new TestJob("N");
            queue.offer(f1, a);
            server.runOne(100);  // F1 が途中になる
            queue.offer(n, b);
            Thread canceller = new Thread(() -> queue.cancel(a));
            canceller.start();
            inCancel.await(2, TimeUnit.SECONDS);
            // 取り消す側が cancel の中で止まっている間に、メインスレッドが進める
            for (int k = 0; k < 5 && server.runOne(50); k++) {
            }
            boolean nRanFirst = n.stepped == 1 && f1.released == 0;
            letGo.countDown();
            canceller.join();
            runUntilIdle(server, queue);
            check("取り消しの途中でも、F1 はもう進めず、N が進む（片づけはまだ）", nRanFirst && f1.stepped == 1, n.stepped + "," + f1.released + "," + f1.stepped);
            check("そのあと F1 の片づけは 1 回", f1.released == 1, f1.released);
            check("重さ・片づけ待ちは 0", empty(queue) && queue.pendingRelease() == 0, counts(queue) + " release=" + queue.pendingRelease());
        }

        System.out.println("--- 16. 片づけを待つ仕事が多すぎると新しい仕事を断り、片づけたらまた受け付ける（C1）");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            // いっぱいの列を 1 回止めても、すぐ次を受け付ける（緑の旗で始め直すため）
            for (int i = 0; i < BuildQueue.MAX_PENDING_JOBS; i++) {
                queue.offer(new TestJob("Q"), a);
            }
            int cancelled = queue.cancel(a);
            check("いっぱいの列を止めた", cancelled == BuildQueue.MAX_PENDING_JOBS, cancelled);
            check("片づけ待ちは、止めた数と同じ", queue.pendingRelease() == BuildQueue.MAX_PENDING_JOBS, queue.pendingRelease());
            check("1 回止めただけなら、すぐ次を受け付ける", queue.hasRoomFor(1) && queue.offer(new TestJob("RESTART"), a), queue.pendingRelease());
            // メインスレッドが動かない間に、「周りをきれいにする」と止めるをくり返す（1 つで 676 列）
            int accepted = 0;
            boolean refused = false;
            boolean exact = true;
            for (int i = 0; i < 1000 && !refused; i++) {
                TestJob clear = new TestJob("CLEAR");
                clear.footprint = 676;
                if (queue.hasRoomFor(clear.weight()) && queue.offer(clear, b)) {
                    accepted++;
                    queue.cancel(b);
                    exact &= queue.pendingRelease() == BuildQueue.MAX_PENDING_JOBS + 676L * accepted;
                } else {
                    refused = true;
                }
            }
            // 片づけ待ちが上限に届くまで受け付ける数（上限 − いっぱいの列の分 を 676 で割って切り上げ）
            long expectedAccepted = (BuildQueue.MAX_PENDING_RELEASE - BuildQueue.MAX_PENDING_JOBS + 675) / 676;
            check("片づけ待ちは、受け付けるたびに 676 ずつ増える", exact, queue.pendingRelease());
            check("片づけ待ちが上限に届いたら断る（ちょうど " + expectedAccepted + " 回受け付けたあと）", refused && accepted == expectedAccepted, accepted);
            // hasRoomFor を見ずに足す仕事（チャット・テレポートなど）も、offer そのものが断る
            check("hasRoomFor を見ない offer も断る", !queue.offer(new TestJob("CHAT"), b), queue.pendingRelease());
            check("片づけ待ちは、いっぱいの列の分 + 676 × 受け付けた数", queue.pendingRelease() == BuildQueue.MAX_PENDING_JOBS + 676L * expectedAccepted,
                queue.pendingRelease());
            runUntilIdle(server, queue);
            check("片づけたら 0 に戻り、また受け付ける", queue.pendingRelease() == 0 && queue.hasRoomFor(1) && queue.offer(new TestJob("AGAIN"), b),
                queue.pendingRelease());
            runUntilIdle(server, queue);
            check("RESTART と AGAIN は進む", only("step:").equals(List.of("RESTART", "AGAIN")), only("step:"));
            check("重さは 0、つながりも空", empty(queue) && queue.linkedEntries() == 0, counts(queue) + " linked=" + queue.linkedEntries());
        }

        System.out.println("--- 17. 列の処理の中で思わぬ例外が出ても、列は止まったままにならない");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = new BuildQueue(server, () -> false);  // 本物の 1 秒で頼み直す（1 秒あくことを確かめる）
            TestJob odd = new TestJob("ODD", 2, 1);
            odd.throwOnWaitingOnce = true;
            TestJob next = new TestJob("NEXT");
            queue.offer(odd, a);
            queue.offer(next, a);
            long t0 = System.nanoTime();
            runUntilIdle(server, queue);
            double seconds = (System.nanoTime() - t0) / 1e9;
            check("1 秒ほどあとに続きから進み、NEXT まで終わる", odd.stepped == 2 && next.stepped == 1 && seconds >= 0.9,
                odd.stepped + "," + next.stepped + " " + seconds);
            check("重さは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 18. 列の処理の中で例外が出ても、途中の仕事はあとから入った番号の小さい仕事に追い越されない");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob cur = new TestJob("CURRENT", 3, 100_000);
            cur.waiting = true;
            cur.throwOnWaitingOnce = true;  // 1 回目に止まったところで例外（列の処理の守りに入る）
            queue.offer(cur, a);
            server.runOne(100);
            // 番号を取ってから列に入れるまでの間に追い越された形を作る（6 と同じ）
            Field field = BuildQueue.class.getDeclaredField("nextSeq");
            field.setAccessible(true);
            ((AtomicLong) field.get(queue)).set(-1);
            queue.offer(new TestJob("OLDER"), b);
            runUntilIdle(server, queue);
            check("CURRENT を終えてから OLDER", only("step:").equals(List.of("CURRENT", "CURRENT", "CURRENT", "OLDER")), only("step:"));
            check("重さ・片づけ待ちは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 19. 同じ途中の仕事で例外が続いたら、その仕事をやめて次へ進む（片づけは 1 回）");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob bad = new TestJob("BAD", 1_000_000, 100_000);
            bad.waiting = true;
            bad.throwOnWaitingAlways = true;
            TestJob next = new TestJob("NEXT");
            TestJob other = new TestJob("OTHER");
            queue.offer(bad, a);
            queue.offer(next, a);
            queue.offer(other, b);
            runUntilIdle(server, queue);
            check("BAD は 2 回だけ進めて、やめる", bad.stepped == 2, bad.stepped);
            check("BAD の片づけは 1 回", bad.released == 1, bad.released);
            check("BAD は cancel を 1 回呼んでから片づける", count(only("cancel:"), "BAD") == 1
                && log.indexOf("cancel:BAD") < log.indexOf("release:BAD"), log);
            check("NEXT と OTHER は進む", next.stepped == 1 && other.stepped == 1, next.stepped + "," + other.stepped);
            check("重さ・片づけ待ちは 0、つながりも空", empty(queue) && queue.linkedEntries() == 0, counts(queue) + " linked=" + queue.linkedEntries());
        }

        // 20. やめる直前に取り消された・切れたとき（2 回目の step の中で取り消し、そのあと例外。片づけは取り消した側の 1 回だけ）
        for (String how : List.of("cancel", "forget")) {
            System.out.println("--- 20. 例外が続いた仕事が、やめる直前に " + how + " で外されても、片づけ・数は 1 回だけ");
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob bad = new TestJob("BAD", 1_000_000, 100_000);
            bad.waiting = true;
            bad.throwOnWaitingAlways = true;
            bad.duringStep = () -> {
                if (bad.stepped == 2) {
                    if (how.equals("cancel")) {
                        queue.cancel(a);
                    } else {
                        queue.forget(a);
                    }
                }
            };
            TestJob other = new TestJob("OTHER");
            queue.offer(bad, a);
            queue.offer(other, b);
            runUntilIdle(server, queue);
            check("BAD は 2 回だけ進める", bad.stepped == 2, bad.stepped);
            check("BAD の片づけ・cancel は 1 回ずつ", bad.released == 1 && count(only("cancel:"), "BAD") == 1,
                bad.released + " " + only("cancel:"));
            check("OTHER は進む", other.stepped == 1, other.stepped);
            check("重さ・片づけ待ちは 0、つながりも空", empty(queue) && queue.linkedEntries() == 0, counts(queue) + " linked=" + queue.linkedEntries());
        }

        System.out.println("--- 21. 続いていない例外では、やめない（うまく進んだら数え直す・別の仕事の例外は続きと見ない）");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            // 同じ仕事が「例外 → うまく進む → 例外」
            TestJob flaky = new TestJob("FLAKY", 5, 1);
            flaky.waiting = true;
            flaky.throwOnWaitingAt.addAll(List.of(1, 3));
            queue.offer(flaky, a);
            runUntilIdle(server, queue);
            check("FLAKY は最後まで進む（やめない）", flaky.stepped == 5 && flaky.released == 0, flaky.stepped + "," + flaky.released);
            check("重さは 0", empty(queue), counts(queue));
        }
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            // A が例外を 1 回出したあと取り消され、続けて B が初めて例外を出す
            TestJob first = new TestJob("A", 3, 1);
            first.waiting = true;
            first.throwOnWaitingOnce = true;
            TestJob second = new TestJob("B", 3, 1);
            second.waiting = true;
            second.throwOnWaitingOnce = true;
            queue.offer(first, a);
            queue.offer(second, b);
            server.runOne(100);
            queue.cancel(a);
            runUntilIdle(server, queue);
            check("A は 1 回で止まり、片づけは 1 回", first.stepped == 1 && first.released == 1, first.stepped + "," + first.released);
            check("B は 1 回目の例外でやめず、最後まで進む", second.stepped == 3 && second.released == 0, second.stepped + "," + second.released);
            check("重さ・片づけ待ちは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 22. 取り消してから片づけるまでに footprint が変わっても、片づけ待ちは取り消したときの値を引く（F3）");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob clear = new TestJob("CLEAR");
            clear.footprint = 676;
            queue.offer(clear, a);
            queue.cancel(a);
            check("取り消したときは 676", queue.pendingRelease() == 676, queue.pendingRelease());
            clear.footprint = 1;
            runUntilIdle(server, queue);
            check("片づけたら 0 に戻る", queue.pendingRelease() == 0 && clear.released == 1, counts(queue));
        }

        System.out.println("--- 23. 仕事と関係のない所の例外が続いて途中の仕事をやめても、cancel で印が下りる（E1）");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            // 「周りをきれいにする」と同じく、cancel で「とちゅう」の印を下ろす仕事
            java.util.concurrent.atomic.AtomicBoolean inProgress = new java.util.concurrent.atomic.AtomicBoolean(true);
            TestJob area = new TestJob("AREA", 10, 1);
            area.waiting = true;
            area.duringStep = () -> PauseWatch.throwCount = 2;  // 次の 2 回の pump は、進める前に例外
            area.duringCancel = () -> inProgress.set(false);
            TestJob other = new TestJob("OTHER");
            queue.offer(area, a);
            queue.offer(other, b);
            runUntilIdle(server, queue);
            PauseWatch.throwCount = 0;
            check("AREA は 1 回だけ進めて、やめる", area.stepped == 1 && area.released == 1, area.stepped + "," + area.released);
            check("やめた AREA の印は下りる（cancel が呼ばれる）", !inProgress.get() && count(only("cancel:"), "AREA") == 1, only("cancel:"));
            check("OTHER は進む", other.stepped == 1, other.stepped);
            check("重さ・片づけ待ちは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 24. 仕事をやめたら例外の数を数え直し、すぐあとの別の仕事の例外もスタックトレースを出す（L1）");
        {
            log.clear();
            com.github.minecraftedu.MinecraftEduMod.withTrace.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob bad = new TestJob("BAD", 1_000_000, 1);
            bad.waiting = true;
            bad.throwOnWaitingAlways = true;
            TestJob later = new TestJob("LATER", 2, 1);
            later.waiting = true;
            later.throwOnWaitingOnce = true;
            queue.offer(bad, a);
            queue.offer(later, b);
            runUntilIdle(server, queue);
            long traces = count(com.github.minecraftedu.MinecraftEduMod.withTrace, "BuildQueue pump error");
            check("スタックトレースは BAD の最初の 1 回と LATER の 1 回（2 回目の BAD は出さない）", traces == 2,
                com.github.minecraftedu.MinecraftEduMod.withTrace);
            check("LATER は最後まで進む", later.stepped == 2 && later.released == 0, later.stepped + "," + later.released);
            check("重さ・片づけ待ちは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 25. 例外でやめた仕事の cancel・release が例外を投げても、片づけは最後まで進み、列の処理の守りまで漏れない");
        // step の例外でやめる道と、列の処理の例外が続いてやめる道（dropFailing）の両方で、cancel が投げるときと release が投げるとき
        // （dropFailing の道で漏れると、列の処理が頼み直しを予約できず、本物の Minecraft では列が止まったままになる）
        for (String how : List.of("step", "drop")) {
            for (String thrower : List.of("cancel", "release")) {
                String tag = how + "/" + thrower;
                log.clear();
                com.github.minecraftedu.MinecraftEduMod.withTrace.clear();
                MinecraftServer server = new MinecraftServer();
                BuildQueue queue = newQueue(server);
                TestJob both = new TestJob("BOTH", how.equals("step") ? 1 : 1_000_000, 100_000);
                if (how.equals("step")) {
                    both.throwOnStep = new IllegalStateException("試験で投げる例外（step）");
                } else {
                    both.waiting = true;
                    both.throwOnWaitingAlways = true;
                }
                if (thrower.equals("cancel")) {
                    both.throwOnCancel = new IllegalStateException("試験で投げる例外（cancel）");
                } else {
                    both.throwOnRelease = new IllegalStateException("試験で投げる例外（release）");
                }
                TestJob after = new TestJob("AFTER");
                queue.offer(both, a);
                queue.offer(after, a);
                runUntilIdle(server, queue);
                if (how.equals("step")) {
                    // cancel・release の例外は abandon の中で受け止める（列の処理の守りまで漏れると、本物では 1 秒あけて頼み直すことになる。
                    // 守りが受け止めると、スタックトレース付きの「BuildQueue pump error」が出るので、それが無いことを見る）
                    check(tag + ": " + thrower + " の例外は、列の処理の守りまで漏れない",
                        count(com.github.minecraftedu.MinecraftEduMod.withTrace, "BuildQueue pump error") == 0,
                        com.github.minecraftedu.MinecraftEduMod.withTrace);
                }
                check(tag + ": " + thrower + " が投げても、cancel と片づけは 1 回ずつ", both.released == 1 && count(only("cancel:"), "BOTH") == 1,
                    both.released + " " + only("cancel:"));
                check(tag + ": cancel のあとに片づける", log.indexOf("cancel:BOTH") >= 0
                    && log.indexOf("cancel:BOTH") < log.indexOf("release:BOTH"), log);
                check(tag + ": 次の仕事は進み、重さ・片づけ待ちは 0、つながりも空", after.stepped == 1 && empty(queue) && queue.linkedEntries() == 0,
                    after.stepped + " " + counts(queue) + " linked=" + queue.linkedEntries());
            }
        }

        // 26. 取り消しの途中で footprint が例外を投げても、その仕事の片づけと、同じ接続の後ろの仕事の取り消しは最後まで進む
        for (String how : List.of("cancel", "forget")) {
            System.out.println("--- 26. 取り消しの途中で footprint が例外を投げても、" + how + " は最後まで進み、片づけは 1 回ずつ");
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob x = new TestJob("X");
            x.throwOnFootprint = new IllegalStateException("試験で投げる例外（footprint）");
            TestJob y = new TestJob("Y");
            TestJob z = new TestJob("Z");
            TestJob w = new TestJob("W");
            queue.offer(x, a);
            queue.offer(y, a);
            queue.offer(z, a);
            queue.offer(w, b);
            int cancelled;
            try {
                cancelled = how.equals("cancel") ? queue.cancel(a) : queue.forget(a);
            } catch (RuntimeException e) {
                // 受け口のスレッドまで例外が抜けると、本物ではその接続の stopAll が失敗し、後ろの仕事が外れない
                check(how + ": footprint の例外が " + how + " の外へ抜けない", false, e);
                cancelled = -1;
            }
            check(how + ": 3 個とも外す（footprint の例外で止まらない）", cancelled == 3, cancelled);
            // 数えられなかった X は 1 と見て、片づけ待ちに足す（X・Y・Z で 3）
            check(how + ": 片づけ待ちは 3（数えられない仕事は 1 と見る）", queue.pendingRelease() == 3, queue.pendingRelease());
            runUntilIdle(server, queue);
            check(how + ": 片づけは X・Y・Z とも 1 回、どれも進めない", x.released == 1 && y.released == 1 && z.released == 1
                && x.stepped == 0 && y.stepped == 0 && z.stepped == 0,
                x.released + "," + y.released + "," + z.released + " step=" + x.stepped + y.stepped + z.stepped);
            check(how + ": ほかの接続の W は進む", w.stepped == 1 && w.released == 0, w.stepped + "," + w.released);
            check(how + ": 重さ・片づけ待ちは 0、つながりも空", empty(queue) && queue.pendingRelease() == 0 && queue.linkedEntries() == 0,
                counts(queue) + " release=" + queue.pendingRelease() + " linked=" + queue.linkedEntries());
        }

        System.out.println("--- 27. 例外が続いて仕事をやめたら、頼み直しを待たずにすぐ次の仕事へ進む（本物の 1 秒で）");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = new BuildQueue(server, () -> false);  // 本物の 1 秒で頼み直す
            TestJob bad = new TestJob("BAD", 1_000_000, 1);
            bad.waiting = true;
            bad.throwOnWaitingAlways = true;
            TestJob next = new TestJob("NEXT");
            queue.offer(bad, a);
            queue.offer(next, a);
            long t0 = System.nanoTime();
            runUntilIdle(server, queue);
            double seconds = (System.nanoTime() - t0) / 1e9;
            // 1 回目の例外のあとは 1 秒あけ、2 回目でやめたらすぐ続ける（約 1 秒）。やめたあとも 1 秒待つと約 2 秒
            check("BAD をやめたら、すぐ NEXT へ進む（全部で 1.8 秒より短い）", bad.stepped == 2 && next.stepped == 1 && seconds < 1.8,
                bad.stepped + "," + next.stepped + " " + seconds);
            check("重さ・片づけ待ちは 0", empty(queue), counts(queue));
        }

        System.out.println("--- 28. 一時停止した列は、途中の仕事もそこで止まり、ほかの接続は進む。再開したら続きから進む");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob big = new TestJob("BIG", 5, 1);
            TestJob after = new TestJob("AFTER");
            queue.offer(big, a);
            queue.offer(after, a);
            server.runOne(100);
            check("BIG を 1 回進めた（途中）", big.stepped == 1, big.stepped);
            int waiting = queue.pause(a);
            check("止めたときの返事は、並んでいる 2 個（途中の BIG も入れる）", waiting == 2, waiting);
            TestJob other = new TestJob("OTHER");
            TestJob late = new TestJob("LATE");
            queue.offer(other, b);
            queue.offer(late, a);
            runUntilIdle(server, queue);
            check("止めている間、BIG・AFTER・止めてから届いた LATE は進めない", big.stepped == 1 && after.stepped == 0 && late.stepped == 0,
                big.stepped + "," + after.stepped + "," + late.stepped);
            check("ほかの接続の OTHER は進む", other.stepped == 1, other.stepped);
            check("止めている列の仕事だけが残ったら、列の処理は休む", queue.idle(), counts(queue));
            check("止めている間も、重さと仕事の数には数えたまま（3 個）", !empty(queue) && counts(queue).contains("jobs=3"), counts(queue));
            check("止めている間は、再開の知らせを出さない", big.resumedCount == 0, big.resumedCount);
            log.clear();
            int resumed = queue.resume(a);
            check("再開したときの返事は、並んでいる 3 個", resumed == 3, resumed);
            runUntilIdle(server, queue);
            check("再開したら BIG は続きから最後まで（合わせて 5 回）、そのあと AFTER・LATE", big.stepped == 5 && after.stepped == 1 && late.stepped == 1
                && only("step:").equals(List.of("BIG", "BIG", "BIG", "BIG", "AFTER", "LATE")), only("step:"));
            check("再開した列の先頭の BIG に、進める前に 1 回だけ知らせる（AFTER・LATE・ほかの接続の OTHER には知らせない）",
                big.resumedCount == 1 && after.resumedCount == 0 && late.resumedCount == 0 && other.resumedCount == 0
                && log.indexOf("resumed:BIG") >= 0 && log.indexOf("resumed:BIG") < log.indexOf("step:BIG"), log);
            check("片づけは呼ばない、重さ・片づけ待ちは 0", big.released == 0 && empty(queue) && queue.pendingRelease() == 0, counts(queue));
        }

        System.out.println("--- 29. まだ列の無い接続を止めても、そのあと届いた仕事は止まる。止めていない列の再開は何もしない");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            check("止める前に列が無くても、返事は 0 個", queue.pause(a) == 0, "");
            TestJob x = new TestJob("X");
            queue.offer(x, a);
            runUntilIdle(server, queue);
            check("止めてから届いた X は進めない", x.stepped == 0 && queue.idle(), x.stepped);
            check("列の無い接続の再開は何もしない（返事は 0 個）", queue.resume(b) == 0, "");
            queue.resume(a);
            // 2 回目は止めていないので何もしない（並んでいる数だけ返す。偽物のサーバーは runOne の中でしか進めないので、X はまだ並んでいる）
            check("2 回目の再開は、並んでいる数（X の 1 個）だけを返す", queue.resume(a) == 1, "");
            runUntilIdle(server, queue);
            check("再開したら X は進み、知らせは 1 回だけ（2 回続けて再開しても）", x.stepped == 1 && x.resumedCount == 1 && empty(queue),
                x.stepped + "," + x.resumedCount + " " + counts(queue));
            // 止めていない列を、列の処理が一度回ったあとで再開しても、次に並べた仕事には知らせない
            queue.resume(a);
            TestJob y = new TestJob("Y");
            queue.offer(y, a);
            runUntilIdle(server, queue);
            check("止めていない列の再開では、知らせない（Y は 0 回）", y.stepped == 1 && y.resumedCount == 0, y.stepped + "," + y.resumedCount);
            check("null の接続は止めない・再開しない（0 個）", queue.pause(null) == 0 && queue.resume(null) == 0, "");
        }

        // 30. 止めている列を、止めるボタン（cancel）・切断（forget）で外す
        for (String how : List.of("cancel", "forget")) {
            System.out.println("--- 30. 止めている列を " + how + " で外すと、仕事は片づけ、そのあと届いた仕事はすぐ進む");
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob big = new TestJob("BIG", 5, 1);
            TestJob y = new TestJob("Y");
            queue.offer(big, a);
            queue.offer(y, a);
            server.runOne(100);
            queue.pause(a);
            int cancelled = how.equals("cancel") ? queue.cancel(a) : queue.forget(a);
            check(how + ": 止めている列の 2 個を外す", cancelled == 2, cancelled);
            runUntilIdle(server, queue);
            check(how + ": 片づけは BIG・Y とも 1 回、そのあと進めない", big.released == 1 && y.released == 1 && big.stepped == 1 && y.stepped == 0,
                big.released + "," + y.released + " step=" + big.stepped + "," + y.stepped);
            TestJob next = new TestJob("NEXT");
            queue.offer(next, a);
            runUntilIdle(server, queue);
            check(how + ": そのあと同じ接続から届いた NEXT は、止めずに進む", next.stepped == 1, next.stepped);
            check(how + ": 重さ・片づけ待ちは 0", empty(queue) && queue.pendingRelease() == 0, counts(queue));
        }

        System.out.println("--- 31. 先頭がチャンクの読み込みを待つ間の先読みは、止めている列の仕事のチャンクを頼まない");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob a1 = new TestJob("A1");
            a1.chunk = 100;
            queue.offer(a1, a);
            queue.pause(a);
            TestJob head = new TestJob("HEAD", 3, 1);
            head.waiting = true;
            head.chunk = 200;
            TestJob b2 = new TestJob("B2");
            b2.chunk = 300;
            queue.offer(head, b);
            queue.offer(b2, b);
            server.runOne(100);
            check("止めていない列の B2 は先に頼む", only("prefetch:").contains("B2"), only("prefetch:"));
            check("止めている列の A1 は頼まない", !only("prefetch:").contains("A1"), only("prefetch:"));
            queue.cancel(a);
            runUntilIdle(server, queue);
            check("重さ・片づけ待ちは 0", empty(queue) && queue.pendingRelease() == 0, counts(queue));
        }

        System.out.println("--- 32. ほかの接続の仕事を進めている途中に再開しても、同じ回のうちに、再開した仕事を進める前に知らせる");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob j = new TestJob("J", 3, 1);
            queue.offer(j, a);
            server.runOne(100);
            queue.pause(a);
            TestJob k = new TestJob("K");
            // K を進めている間に（受け口のスレッドから届いたつもりで）A を再開する
            k.duringStep = () -> queue.resume(a);
            queue.offer(k, b);
            log.clear();
            // 1 回だけ回す（K のあと、同じ回のうちに J へ進む）
            server.runOne(100);
            check("同じ回のうちに K のあと J へ進み、J を進める前に知らせる", only("step:").equals(List.of("K", "J"))
                && log.indexOf("resumed:J") > log.indexOf("step:K") && log.indexOf("resumed:J") < log.indexOf("step:J"), log);
            check("ほかの接続の K には知らせない", k.resumedCount == 0, k.resumedCount);
            runUntilIdle(server, queue);
            check("J は最後まで進み、知らせは 1 回、重さは 0", j.stepped == 3 && j.resumedCount == 1 && empty(queue), j.stepped + "," + j.resumedCount + " " + counts(queue));
        }

        System.out.println("--- 33. 止めている間にほかの接続の仕事が途中になったら、再開しても、その途中の仕事を追い越さない");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob first = new TestJob("A", 3, 1);
            queue.offer(first, a);
            server.runOne(100);
            queue.pause(a);
            TestJob second = new TestJob("B", 3, 1);
            queue.offer(second, b);
            server.runOne(100);
            check("止めている間に B が途中になる", second.stepped == 1 && first.stepped == 1, first.stepped + "," + second.stepped);
            queue.resume(a);
            runUntilIdle(server, queue);
            check("再開しても、途中の B を終えてから A の続き（A B B B A A）", only("step:").equals(List.of("A", "B", "B", "B", "A", "A")), only("step:"));
            check("知らせは A に 1 回だけ（B には知らせない）", first.resumedCount == 1 && second.resumedCount == 0, first.resumedCount + "," + second.resumedCount);
            check("重さは 0", empty(queue), counts(queue));
        }

        // 34. 仕事を進めている最中に止める（その回で終わらないとき・ちょうど終わるとき）
        for (String how : List.of("途中", "終わる")) {
            System.out.println("--- 34. 仕事を進めている最中に止めると（" + how + "）、その回が終わったところで止まり、再開したら続きから");
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob first = new TestJob("A", how.equals("途中") ? 3 : 1, 1);
            first.duringStep = () -> {
                if (first.stepped == 1) {
                    queue.pause(a);
                }
            };
            TestJob next = new TestJob("NEXT");
            queue.offer(first, a);
            queue.offer(next, a);
            runUntilIdle(server, queue);
            check(how + ": 止めた回のあとは進めない（A は 1 回、NEXT は 0 回）、列の処理は休む", first.stepped == 1 && next.stepped == 0 && queue.idle(),
                first.stepped + "," + next.stepped);
            queue.resume(a);
            runUntilIdle(server, queue);
            boolean finishedFirst = how.equals("途中") ? first.stepped == 3 : first.stepped == 1;
            check(how + ": 再開したら A の続きと NEXT が進む", finishedFirst && next.stepped == 1 && empty(queue),
                first.stepped + "," + next.stepped + " " + counts(queue));
            // 途中で止めたなら A に、ちょうど終わっていたなら、次に先頭になった NEXT に知らせる
            check(how + ": 知らせは、再開した列で次に進めた仕事に 1 回", how.equals("途中")
                ? first.resumedCount == 1 && next.resumedCount == 0 : first.resumedCount == 0 && next.resumedCount == 1,
                first.resumedCount + "," + next.resumedCount);
        }

        System.out.println("--- 35. 再開の知らせが例外を投げても、列は止まらず、その仕事は進む");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob odd = new TestJob("ODD", 2, 1) {
                @Override
                public void resumed() {
                    super.resumed();
                    throw new IllegalStateException("試験で投げる例外（resumed）");
                }
            };
            queue.offer(odd, a);
            server.runOne(100);
            queue.pause(a);
            // メインスレッドに止めた状態を見せてから再開する（見ないうちの再開では、知らせないため。37 節）
            runUntilIdle(server, queue);
            queue.resume(a);
            com.github.minecraftedu.MinecraftEduMod.withTrace.clear();
            runUntilIdle(server, queue);
            check("知らせの例外は、列の処理の守りまで漏れない", count(com.github.minecraftedu.MinecraftEduMod.withTrace, "BuildQueue pump error") == 0,
                com.github.minecraftedu.MinecraftEduMod.withTrace);
            check("ODD は最後まで進む（片づけは呼ばない）", odd.stepped == 2 && odd.released == 0 && odd.resumedCount == 1 && empty(queue),
                odd.stepped + "," + odd.released + "," + odd.resumedCount + " " + counts(queue));
        }

        System.out.println("--- 36. 同じ途中の仕事を 2 回止めて再開しても、そのたびに、進める前に知らせる");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob j = new TestJob("J", 5, 1);
            queue.offer(j, a);
            server.runOne(100);
            for (int round = 1; round <= 2; round++) {
                queue.pause(a);
                runUntilIdle(server, queue);
                int before = j.stepped;
                queue.resume(a);
                log.clear();
                server.runOne(100);
                check(round + " 回目の再開: J を 1 回進める前に知らせる（知らせは合わせて " + round + " 回）",
                    j.resumedCount == round && j.stepped == before + 1
                    && log.indexOf("resumed:J") >= 0 && log.indexOf("resumed:J") < log.indexOf("step:J"), log + " " + j.resumedCount);
            }
            runUntilIdle(server, queue);
            check("J は最後まで進み、重さは 0", j.stepped == 5 && empty(queue), j.stepped + " " + counts(queue));
        }

        System.out.println("--- 37. メインスレッドが止めた状態を見ないうちに再開しても、仕事は止まっていないので知らせない（くり返して時間を戻し続けられない）");
        {
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob j = new TestJob("J", 10, 1);
            queue.offer(j, a);
            server.runOne(100);
            // 止めると再開が、メインスレッドの 1 回の間に両方届く（を 3 回くり返す）
            for (int round = 0; round < 3; round++) {
                queue.pause(a);
                queue.resume(a);
                server.runOne(100);
            }
            check("止めた状態を見ないうちの再開では、J に知らせない", j.resumedCount == 0 && j.stepped == 4, j.resumedCount + "," + j.stepped);
            // 止めた状態を見たあとの再開では、知らせる
            queue.pause(a);
            runUntilIdle(server, queue);
            queue.resume(a);
            server.runOne(100);
            check("止めた状態を見たあとの再開では、J に 1 回知らせる", j.resumedCount == 1, j.resumedCount);
            // 知らせたあとは印を下ろす（下ろさないと、そのあとの見ないうちの止めると再開でも、知らせ続けてしまう）
            for (int round = 0; round < 3; round++) {
                queue.pause(a);
                queue.resume(a);
                server.runOne(100);
            }
            check("知らせたあとの、見ないうちの止めると再開では、もう知らせない（1 回のまま）", j.resumedCount == 1, j.resumedCount);
            runUntilIdle(server, queue);
            check("J は最後まで進み、重さは 0", j.stepped == 10 && empty(queue), j.stepped + " " + counts(queue));
        }
        {
            // 止めた状態を見たあと、止めるボタン（cancel）で終わったら、その印は古いので、次の仕事の「見ないうちの再開」で知らせない
            log.clear();
            MinecraftServer server = new MinecraftServer();
            BuildQueue queue = newQueue(server);
            TestJob old = new TestJob("OLD", 10, 1);
            queue.offer(old, a);
            server.runOne(100);
            queue.pause(a);
            runUntilIdle(server, queue);
            queue.cancel(a);
            TestJob k = new TestJob("K", 5, 1);
            queue.offer(k, a);
            server.runOne(100);
            queue.pause(a);
            queue.resume(a);
            server.runOne(100);
            check("止めるボタンのあとの仕事 K は、見ないうちの再開で知らせない（古い印を使わない）", k.resumedCount == 0 && k.stepped == 2, k.resumedCount + "," + k.stepped);
            runUntilIdle(server, queue);
            check("K は最後まで進み、OLD は片づけ 1 回、重さは 0", k.stepped == 5 && old.released == 1 && empty(queue), k.stepped + "," + old.released + " " + counts(queue));
        }

        System.out.println("結果: " + passed + " 件成功 / " + failed + " 件失敗");
        System.exit(failed == 0 ? 0 : 1);
    }
}
