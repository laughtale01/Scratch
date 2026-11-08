package net.minecraft;
// 試験用の偽物（Minecraft の時計。ミリ秒）
public class Util {
    public static long getMillis() {
        return System.nanoTime() / 1_000_000L;
    }
}
