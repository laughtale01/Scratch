package com.github.minecraftedu.commands;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

// 試験用の偽物（1 人用のワールドが止まっているか・新しく読み込む数の予算・列がまるごと待った回数を、試験が決める）
class PauseWatch {
    static volatile boolean paused;
    static volatile boolean budgetOk = true;
    static volatile long holds;
    static volatile int refreshed;

    static boolean paused() {
        return paused;
    }

    static boolean withinBudget(ServerLevel world, int minX, int maxX, int minZ, int maxZ) {
        return budgetOk;
    }

    static long holds() {
        return holds;
    }

    static void ticketRefreshed(long chunk) {
        refreshed++;
    }

    static boolean overLimit(MinecraftServer server) {
        return false;
    }
}
