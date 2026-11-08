package com.github.minecraftedu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 試験用の偽物（ログを覚える。試験は lines を見る）
public class MinecraftEduMod {
    public static final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    public static final List<String> withTrace = Collections.synchronizedList(new ArrayList<>());

    public static class FakeLogger {
        public void info(String m) { lines.add("INFO " + m); }
        public void warn(String m) { lines.add("WARN " + m); }
        public void debug(String m) { lines.add("DEBUG " + m); }
        public void error(String m) { lines.add("ERROR " + m); }
        public void error(String m, Throwable t) {
            lines.add("ERROR " + m + " " + t);
            withTrace.add(m);
        }
    }

    public static final FakeLogger LOGGER = new FakeLogger();
}
