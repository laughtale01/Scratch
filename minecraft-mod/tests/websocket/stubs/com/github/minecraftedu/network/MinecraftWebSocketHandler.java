package com.github.minecraftedu.network;
import com.github.minecraftedu.commands.CommandExecutor;
import net.minecraft.server.MinecraftServer;
// 試験用の偽物（受け取った文の長さと先頭を返す）
public class MinecraftWebSocketHandler {
    public MinecraftWebSocketHandler(MinecraftServer s, CommandExecutor c) {}
    // つないだあとの接続が切れたときに呼ばれる（試験で、呼ばれた回数をログで数える）
    public void connectionClosed() {
        System.out.println("INFO  stub connectionClosed");
    }
    public String handleMessage(String request) {
        return "len=" + request.length() + ":" + request.substring(0, Math.min(10, request.length()));
    }
}
