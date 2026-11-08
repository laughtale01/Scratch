package it.unimi.dsi.fastutil.longs;

import java.util.HashSet;

// 試験用の偽物（BuildQueue が使う所だけ）
public class LongOpenHashSet implements LongSet {
    private final HashSet<Long> set = new HashSet<>();

    public boolean add(long value) {
        return set.add(value);
    }

    public int size() {
        return set.size();
    }
}
