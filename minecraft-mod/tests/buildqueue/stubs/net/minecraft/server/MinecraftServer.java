package net.minecraft.server;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// 試験用の偽物（メインスレッドの待ち列だけ。試験が runMain で、自分のスレッドをメインスレッドにして流す）
public class MinecraftServer {
    private final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
    private volatile Thread mainThread;

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

    // 次の tick が始まる時刻（いつも 6 ミリ秒先。1 回に進める時間は下限の 5 ミリ秒になる）
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
