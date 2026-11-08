package net.minecraft.world.level.block.entity;

import net.minecraft.resources.ResourceLocation;

// 試験用の偽物（まだ開けていない宝箱など。中身の決まり＝loot table を持つ）
public class RandomizableContainerBlockEntity extends BlockEntity {
    public ResourceLocation lootTable;

    public RandomizableContainerBlockEntity(ResourceLocation lootTable) {
        this.lootTable = lootTable;
    }

    public void setLootTable(ResourceLocation lootTable, long seed) {
        this.lootTable = lootTable;
    }
}
