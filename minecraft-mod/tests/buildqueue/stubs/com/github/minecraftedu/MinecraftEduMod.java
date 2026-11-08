package com.github.minecraftedu;
// 試験用の偽物（ログを標準出力に出すだけ。スタックトレース付きで出したものは withTrace に覚える）
public class MinecraftEduMod {
    public static final java.util.List<String> withTrace = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
    public static class FakeLogger {
        public void info(String m) { System.out.println("INFO  " + m); }
        public void warn(String m) { System.out.println("WARN  " + m); }
        public void debug(String m) { System.out.println("DEBUG " + m); }
        public void error(String m) { System.out.println("ERROR " + m); }
        public void error(String m, Throwable t) {
            System.out.println("ERROR " + m + " " + t);
            withTrace.add(m);
        }
    }
    public static final FakeLogger LOGGER = new FakeLogger();
}
