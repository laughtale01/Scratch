package com.github.minecraftedu;
// 試験用の偽物（ログを標準出力に出すだけ）
public class MinecraftEduMod {
    public static class FakeLogger {
        public void info(String m) { System.out.println("INFO  " + m); }
        public void warn(String m) { System.out.println("WARN  " + m); }
        public void debug(String m) { System.out.println("DEBUG " + m); }
        public void error(String m) { System.out.println("ERROR " + m); }
        public void error(String m, Throwable t) { System.out.println("ERROR " + m + " " + t); }
    }
    public static final FakeLogger LOGGER = new FakeLogger();
}
