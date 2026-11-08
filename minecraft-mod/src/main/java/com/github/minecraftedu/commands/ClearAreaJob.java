package com.github.minecraftedu.commands;

import com.github.minecraftedu.MinecraftEduMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * 「周りをきれいにする」の仕事（チャンク列ごとの FillJob を順に進め、25% ごとに進み具合をチャットに出す）
 * （前は CommandExecutor の中のクラスだった。試験で本物を動かせるように、使う物をコンストラクタで受け取る形にして外へ出した。
 *   外へ出したことでは動きを変えていない。そのあとで、遠すぎる場所なら始めずに断る確かめ（inReach）を足した）
 */
class ClearAreaJob implements BuildQueue.Job {
    /**
     * チャットとログに出す先（CommandExecutor の sendChatMessage・sendErrorChat・logInputError）
     */
    interface Notices {
        // チャットに出す
        void chat(String message);

        // 失敗をチャットに出す（赤い文字）
        void errorChat(String message);

        // 入力の誤りをログに出す
        void inputError(String key, String message);

        // 遠すぎる場所なので断ったことを、ログとチャットで知らせる
        void tooFar();
    }

    // 先にチケットを付けておく、この先の列の数（置く順番が来る前に、チャンクの読み込みを進めておくため。
    // 少ないと、列ごとに読み込みを待って遅くなる）
    private static final int PREFETCH_COLUMNS = 16;

    private final MinecraftServer server;
    private final List<FillJob> columns;
    private final int centerX, centerZ;
    // 「周りをきれいにする」の途中の印（CommandExecutor と同じもの。終わる・やめる・取り消すときに下ろす）
    private final AtomicBoolean inProgress;
    // チャットとログに出す先
    private final Notices notices;
    // 読み込みを待ちきれずにやめたときに出すチャット
    private final String gaveUpChat;
    // 順番が来たときに、遠すぎない場所か（プレイヤーとワールドのはじめの場所から決まった距離の中か）
    private final Predicate<ServerLevel> inReach;
    // 列にたまる量の重さ（並べたときと終わったときで同じ値を使うので、先に決めておく）
    // 置くブロックの数（約 2,650 万）のままだと、きれいにしている間ずっと上限を超えて、
    // そのあとに届いた「埋める」（土台など）がみな断られ、作品から黙って抜けるので、いちばん大きい「埋める」と同じ重さまでにする
    private final long weight;
    // 次に進めるチャンク列
    private int index = 0;
    // 始めたことをチャットで知らせたか
    private boolean announced = false;
    // まだ終わっていないか（終わる・やめる・取り消すのうち、最初の 1 つだけが「周りをきれいにする」の途中の印を下ろす。
    // 取り消しは受け口のスレッドで起きるので、取り消したあとに別の接続が始めた印を、こちらが下ろさないように）
    private final AtomicBoolean active = new AtomicBoolean(true);

    /**
     * @param maxWeight 重さの上限（いちばん大きい「埋める」と同じ）
     */
    ClearAreaJob(MinecraftServer server, List<FillJob> columns, int centerX, int centerZ, long maxWeight,
                 AtomicBoolean inProgress, Predicate<ServerLevel> inReach, Notices notices, String gaveUpChat) {
        this.server = server;
        this.inReach = inReach;
        this.columns = columns;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.inProgress = inProgress;
        this.notices = notices;
        this.gaveUpChat = gaveUpChat;
        long sum = 0;
        for (FillJob column : columns) {
            sum += column.weight();
        }
        this.weight = Math.min(sum, maxWeight);
    }

    @Override
    public long weight() {
        return weight;
    }

    @Override
    public void cancel() {
        // 止めた・切れたので、そこでやめる。印はすぐ下ろす（緑の旗で始め直した「周りをきれいにする」を断らないように）
        if (active.compareAndSet(true, false)) {
            inProgress.set(false);
        }
    }

    @Override
    public void release() {
        // 先に付けたチケットを、全部の列から外す（メインスレッドで BuildQueue が呼ぶ。外すのは自分の列のチケットだけなので、
        // 次の仕事が始まったあとでもよい。もう外した列は何もしない）
        columns.forEach(FillJob::removeTickets);
    }

    @Override
    public long footprint() {
        // 片づけを待つ間も、列（FillJob）を全部持っている
        return columns.size();
    }

    @Override
    public void resumed() {
        // 一時停止から再開した。いまの列の、あきらめるまでの時間を数え直し、区画の途中なら読み込みを確かめ直す
        // （先の列のチケットは、step が回ごとに付け直す）
        if (index < columns.size()) {
            columns.get(index).resumed();
        }
    }

    @Override
    public boolean step(long deadlineNanos) {
        // 始めたことは、列で順番が来たときに知らせる（列に足せなかったときに「開始」だけ出ないように。
        // メインスレッドの上なので、チャットはその場で出る）
        if (!announced) {
            // 遠すぎる場所なら、始めずに断る（順番が来たときのプレイヤーの位置で決める。チャンクにチケットを付ける前に）
            ServerLevel here = server.overworld();
            if (here != null && !inReach.test(here)) {
                if (active.compareAndSet(true, false)) {
                    inProgress.set(false);
                }
                notices.tooFar();
                return true;
            }
            announced = true;
            notices.chat("§a周囲クリア開始 (" + columns.size() + "チャンク)");
        }
        while (index < columns.size()) {
            // この先の列のチケットを先に付ける（失敗しても、いまの列はそのまま進める）
            try {
                ServerLevel world = server.overworld();
                if (world != null) {
                    for (int ahead = index + 1; ahead <= index + PREFETCH_COLUMNS && ahead < columns.size(); ahead++) {
                        columns.get(ahead).prepare(world);
                        // 消すときに読むとなりのチャンクも、先に読み込みを頼む（時間で切れるチケットなので、回ごとに付け直す）
                        columns.get(ahead).prefetchNeighbors(world);
                    }
                }
            } catch (Exception e) {
                MinecraftEduMod.LOGGER.error("チャンクの先読みエラー (index=" + index + "): " + e.getMessage());
            }
            try {
                if (!columns.get(index).step(deadlineNanos)) {
                    return false;  // 時間が来たか、チャンクの読み込みを待っている。次はこの列の続きから
                }
            } catch (Exception e) {
                // 1 つの列のエラーで全体を止めない（終わりの印を下ろせなくなるため）
                MinecraftEduMod.LOGGER.error("チャンク処理エラー (index=" + index + "): " + e.getMessage());
            }
            if (columns.get(index).gaveUp()) {
                // チャンクの読み込みを待ちきれなかった。残りもやめる（列ごとに待って知らせると、何分も続くため）
                // （上の catch の外で呼ぶ。中で例外が起きても、次の列へ進まないように）
                giveUp();
                return true;
            }
            index++;
            // 25%ごとに進捗をチャットに表示
            int quarter = columns.size() / 4;
            if (quarter > 0 && index < columns.size() && index % quarter == 0) {
                notices.chat("§a周囲クリア進行中... " + (index * 100) / columns.size() + "%");
            }
            if (index < columns.size() && System.nanoTime() - deadlineNanos >= 0) {
                return false;
            }
        }
        if (!active.compareAndSet(true, false)) {
            return true;  // 進めている間に取り消された（止めた・切れた）。完了とは知らせない
        }
        inProgress.set(false);
        notices.chat("§a周囲クリア完了: 中心(" + centerX + ", " + centerZ + ")");
        MinecraftEduMod.LOGGER.info("チャンク分割タスク完了: " + columns.size() + "チャンク処理済み");
        return true;
    }

    /**
     * 途中でやめる（先に付けた、この先の列のチケットを外し、終わりの印を下ろして知らせる）
     */
    private void giveUp() {
        if (!active.compareAndSet(true, false)) {
            return;  // 進めている間に取り消された。チケットは取り消しの側が外す
        }
        // 印は先に下ろす（チケットを外す途中で例外が起きても、次の「周りをきれいにする」を受け付けられるように）
        inProgress.set(false);
        for (int rest = index + 1; rest < columns.size(); rest++) {
            columns.get(rest).removeTickets();
        }
        notices.inputError("clearArea chunk not loaded", "clearArea: gave up waiting for a chunk to load at column "
            + index + "/" + columns.size());
        notices.errorChat(gaveUpChat);
    }
}
