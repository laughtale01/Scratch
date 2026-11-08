package net.minecraft.server;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// 試験用の偽物（メインスレッドの待ち列と、ワールド 1 つ。試験が runOne で、自分のスレッドをメインスレッドにして流す）
public class MinecraftServer {
    private final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private volatile Thread mainThread;
    private final PlayerList playerList = new PlayerList();
    // overworld が返すワールド（null にすると「ワールドが無い」）
    public ServerLevel world;

    public ServerLevel overworld() {
        return world;
    }

    public PlayerList getPlayerList() {
        return playerList;
    }

    public void execute(Runnable task) {
        tasks.add(task);
    }

    public boolean isRunning() {
        return true;
    }

    public boolean isStopped() {
        return false;
    }

    public boolean isSameThread() {
        return Thread.currentThread() == mainThread;
    }

    // 次の tick が始まる時刻（いつも 6 ミリ秒先）
    public long getNextTickTime() {
        return net.minecraft.Util.getMillis() + 6;
    }

    /**
     * 待ち列の処理を 1 つ、このスレッドをメインスレッドとして動かす（waitMillis まで待つ。動かしたら true）
     */
    public boolean runOne(long waitMillis) throws InterruptedException {
        Runnable task = tasks.poll(waitMillis, TimeUnit.MILLISECONDS);
        if (task == null) {
            return false;
        }
        mainThread = Thread.currentThread();
        try {
            task.run();
        } finally {
            mainThread = null;
        }
        return true;
    }

    public boolean hasTasks() {
        return !tasks.isEmpty();
    }
}
