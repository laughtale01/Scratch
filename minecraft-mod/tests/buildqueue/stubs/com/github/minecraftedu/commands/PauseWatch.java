package com.github.minecraftedu.commands;

import net.minecraft.server.MinecraftServer;

// 試験用の偽物（1 人用のワールドは止まっていない）
class PauseWatch {
    // 試験で、仕事と関係のない所から例外を出すため（0 より大きい間は、1 回ごとに 1 減らして投げる）
    static volatile int throwCount;

    static boolean overLimit(MinecraftServer server) {
        if (throwCount > 0) {
            throwCount--;
            throw new IllegalStateException("試験で投げる例外（仕事と関係のない所）");
        }
        return false;
    }
}
