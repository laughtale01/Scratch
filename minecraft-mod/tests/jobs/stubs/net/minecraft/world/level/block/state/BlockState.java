package net.minecraft.world.level.block.state;

import net.minecraft.world.level.block.Block;

import java.util.concurrent.ConcurrentHashMap;

// 試験用の偽物（本物と同じく、同じブロックの状態は 1 つだけ。FillJob は == で比べるため）
public final class BlockState {
    private static final ConcurrentHashMap<String, BlockState> ALL = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Block> BLOCKS = new ConcurrentHashMap<>();
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

    // チェスト・樽（中身の決まりを持てる入れ物）と、かまど（中身の決まりを持たないブロックエンティティ）はブロックエンティティを持つ
    public boolean hasBlockEntity() {
        return name.startsWith("chest") || name.startsWith("barrel") || name.startsWith("furnace");
    }

    // ブロックの種類（名前の [ より前。chest[facing=east] と chest[facing=north] は同じ種類）
    public Block getBlock() {
        int cut = name.indexOf('[');
        String base = cut < 0 ? name : name.substring(0, cut);
        return BLOCKS.computeIfAbsent(base, Block::new);
    }

    public boolean is(Block block) {
        return getBlock() == block;
    }

    @Override
    public String toString() {
        return name;
    }
}
