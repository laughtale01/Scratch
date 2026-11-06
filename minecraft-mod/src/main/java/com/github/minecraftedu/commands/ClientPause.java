package com.github.minecraftedu.commands;

import net.minecraft.client.Minecraft;

/**
 * 画面側（1 人用のワールドを開いている Minecraft）が止まっているか
 *
 * 画面のある Minecraft にしか無いクラスを使うので、専用サーバーでは読み込ませない（PauseWatch が、画面のある側のときだけ呼ぶ）。
 * Minecraft のサーバーも、サーバーのスレッドから同じ値を読んで、止まるかを決めている（IntegratedServer.tickServer）。
 * 止まり始めてすぐ分かるので、tick が動かない時間で見分けるより、止まり始めの命令を取りこぼさない
 */
final class ClientPause {

    private ClientPause() {
    }

    /**
     * 止まっているか（Minecraft のサーバーと同じく、ワールドにつながっていて、ポーズ中なら true）
     */
    static boolean paused() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.getConnection() != null && minecraft.isPaused();
    }
}
