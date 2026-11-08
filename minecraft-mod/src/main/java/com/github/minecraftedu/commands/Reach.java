package com.github.minecraftedu.commands;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * 遠すぎる場所への命令を断るきまり（G2）。
 * ワールドのはじめの場所（スポーン）と、このワールドにいるどのプレイヤーからも MAX マスより遠い場所への
 * 置く・読む・埋める・きれいにする・生き物を出す・入れ物に入れるを、列で順番が来たときに断る
 * （遠くにばらばらに置き続けると、チャンクを読み込んで地形を作り続け、ワールドが何百 MB もふくらみ、空くまで何分もかかるため。
 *   遠くに 300 個置くと約 760 MB・3 分）。
 * 横は x・z のうち大きいほうで数える（四角）。縦は建てられる高さで決まるので見ない。
 * 基準を読むのはメインスレッドだけ（プレイヤーの一覧とはじめの場所は、メインスレッドが書きかえるため）
 */
final class Reach {

    // 断る距離（マス）
    static final int MAX = 2000;

    private Reach() {
    }

    /**
     * 基準の x・z を並べる（[はじめの場所の x, z, 1 人目の x, z, …]。メインスレッドで呼ぶ）。
     * はじめの場所は、記録された値をそのまま読む（getSharedSpawnPos は、はじめの場所がワールドの境界の外だと、
     * 境界のまん中のチャンクを読んで高さを調べるため）。
     * プレイヤーは、このワールドにいる人だけ（ネザーにいる人の座標で、オーバーワールドを近いと見ないように）
     */
    static long[] references(ServerLevel world) {
        List<ServerPlayer> players = world.players();
        long[] refs = new long[2 + players.size() * 2];
        refs[0] = world.getLevelData().getXSpawn();
        refs[1] = world.getLevelData().getZSpawn();
        int i = 2;
        for (ServerPlayer player : players) {
            // いるマスは切り下げで決める（0 の方への切り捨てだと、マイナスの座標で 1 マスずれるため）
            refs[i++] = (long) Math.floor(player.getX());
            refs[i++] = (long) Math.floor(player.getZ());
        }
        return refs;
    }

    /**
     * x・z が、どれかの基準から MAX マスの中か
     */
    static boolean within(long[] refs, long x, long z) {
        for (int i = 0; i + 1 < refs.length; i += 2) {
            if (near(refs[i], refs[i + 1], x, z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 箱の横の 4 すみが、どれも同じ 1 つの基準から MAX マスの中か
     * （すみごとに別の基準でよいとすると、はじめの場所と遠くのプレイヤーの間をまたぐ箱の、まん中あたりが
     *   どちらからも MAX より遠くなるため）
     */
    static boolean boxWithin(long[] refs, long minX, long minZ, long maxX, long maxZ) {
        for (int i = 0; i + 1 < refs.length; i += 2) {
            long rx = refs[i];
            long rz = refs[i + 1];
            if (near(rx, rz, minX, minZ) && near(rx, rz, minX, maxZ) && near(rx, rz, maxX, minZ) && near(rx, rz, maxX, maxZ)) {
                return true;
            }
        }
        return false;
    }

    /**
     * x・z が、このワールドの基準のどれかから MAX マスの中か（メインスレッドで呼ぶ）
     */
    static boolean within(ServerLevel world, long x, long z) {
        return within(references(world), x, z);
    }

    /**
     * 箱の横の 4 すみが、このワールドの同じ 1 つの基準から MAX マスの中か（メインスレッドで呼ぶ）
     */
    static boolean boxWithin(ServerLevel world, long minX, long minZ, long maxX, long maxZ) {
        return boxWithin(references(world), minX, minZ, maxX, maxZ);
    }

    private static boolean near(long refX, long refZ, long x, long z) {
        return Math.max(Math.abs(x - refX), Math.abs(z - refZ)) <= MAX;
    }
}
