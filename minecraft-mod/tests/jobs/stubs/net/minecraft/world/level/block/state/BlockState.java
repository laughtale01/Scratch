package net.minecraft.world.level.block.state;

import java.util.concurrent.ConcurrentHashMap;

// 試験用の偽物（本物と同じく、同じブロックの状態は 1 つだけ。FillJob は == で比べるため）
public final class BlockState {
    private static final ConcurrentHashMap<String, BlockState> ALL = new ConcurrentHashMap<>();
    public static final BlockState AIR = of("air");

    private final String name;

    private BlockState(String name) {
        this.name = name;
    }

    public static BlockState of(String name) {
        return ALL.computeIfAbsent(name, BlockState::new);
    }

    public boolean isAir() {
        return name.equals("air");
    }

    @Override
    public String toString() {
        return name;
    }
}
