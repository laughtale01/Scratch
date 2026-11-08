package net.minecraft.server.level;

import java.util.Comparator;

// 試験用の偽物（名前と、時間で切れるか）
public class TicketType<T> {
    public final String name;
    public final long lifespan;

    private TicketType(String name, long lifespan) {
        this.name = name;
        this.lifespan = lifespan;
    }

    public static <T> TicketType<T> create(String name, Comparator<T> comparator) {
        return new TicketType<>(name, 0);
    }

    public static <T> TicketType<T> create(String name, Comparator<T> comparator, int lifespan) {
        return new TicketType<>(name, lifespan);
    }

    @Override
    public String toString() {
        return name;
    }
}
