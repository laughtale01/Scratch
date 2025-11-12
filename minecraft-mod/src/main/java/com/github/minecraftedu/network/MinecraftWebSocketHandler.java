package com.github.minecraftedu.network;

import com.github.minecraftedu.MinecraftEduMod;
import com.github.minecraftedu.commands.CommandExecutor;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class MinecraftWebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final MinecraftServer minecraftServer;
    private final Gson gson;
    private final CommandExecutor commandExecutor;
    private String sessionId;

    public MinecraftWebSocketHandler(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
        this.gson = new Gson();
        this.commandExecutor = new CommandExecutor(minecraftServer);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        MinecraftEduMod.LOGGER.info("New client connected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        MinecraftEduMod.LOGGER.info("Client disconnected: " + ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            String request = ((TextWebSocketFrame) frame).text();
            MinecraftEduMod.LOGGER.debug("Received: " + request);

            try {
                JsonObject message = gson.fromJson(request, JsonObject.class);
                handleMessage(ctx, message);
            } catch (Exception e) {
                MinecraftEduMod.LOGGER.error("Error processing message", e);
                sendError(ctx, "INTERNAL_ERROR", "Failed to process message: " + e.getMessage());
            }
        }
    }

    private void handleMessage(ChannelHandlerContext ctx, JsonObject message) {
        String type = message.get("type").getAsString();

        switch (type) {
            case "connect":
                handleConnect(ctx, message);
                break;

            case "command":
                handleCommand(ctx, message);
                break;

            case "query":
                handleQuery(ctx, message);
                break;

            case "heartbeat":
                handleHeartbeat(ctx);
                break;

            default:
                sendError(ctx, "UNKNOWN_TYPE", "Unknown message type: " + type);
        }
    }

    private void handleConnect(ChannelHandlerContext ctx, JsonObject message) {
        // セッションID生成
        sessionId = UUID.randomUUID().toString();

        JsonObject payload = message.getAsJsonObject("payload");
        String clientId = payload.get("clientId").getAsString();

        MinecraftEduMod.LOGGER.info("Client connected: " + clientId + " with session: " + sessionId);

        // 接続レスポンス
        JsonObject response = new JsonObject();
        response.addProperty("version", "1.0");
        response.addProperty("messageId", UUID.randomUUID().toString());
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("sessionId", sessionId);
        response.addProperty("type", "connect_response");

        JsonObject responsePayload = new JsonObject();
        responsePayload.addProperty("success", true);
        responsePayload.addProperty("sessionId", sessionId);
        responsePayload.addProperty("clientName", clientId);
        responsePayload.addProperty("role", "STUDENT_FULL");

        JsonObject serverInfo = new JsonObject();
        serverInfo.addProperty("version", "0.1.0");
        serverInfo.addProperty("minecraftVersion", "1.20.1");
        serverInfo.addProperty("maxClients", 10);
        serverInfo.addProperty("currentClients", 1);

        responsePayload.add("serverInfo", serverInfo);
        response.add("payload", responsePayload);

        sendMessage(ctx, response);
    }

    private void handleCommand(ChannelHandlerContext ctx, JsonObject message) {
        JsonObject payload = message.getAsJsonObject("payload");
        String action = payload.get("action").getAsString();
        JsonObject params = payload.getAsJsonObject("params");

        MinecraftEduMod.LOGGER.info("Executing command: " + action);

        try {
            // コマンド実行
            boolean success = commandExecutor.execute(action, params);

            // レスポンス
            JsonObject response = new JsonObject();
            response.addProperty("version", "1.0");
            response.addProperty("messageId", UUID.randomUUID().toString());
            response.addProperty("timestamp", System.currentTimeMillis());
            response.addProperty("sessionId", sessionId);
            response.addProperty("type", "command_response");

            JsonObject responsePayload = new JsonObject();
            responsePayload.addProperty("success", success);
            responsePayload.addProperty("action", action);

            if (success) {
                JsonObject result = new JsonObject();
                result.addProperty("message", "Command executed successfully");
                responsePayload.add("result", result);
            } else {
                responsePayload.addProperty("errorCode", "COMMAND_FAILED");
                responsePayload.addProperty("errorMessage", "Failed to execute command");
            }

            response.add("payload", responsePayload);
            sendMessage(ctx, response);

        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Command execution error", e);
            sendError(ctx, "COMMAND_FAILED", e.getMessage());
        }
    }

    private void handleQuery(ChannelHandlerContext ctx, JsonObject message) {
        // TODO: クエリ処理実装
        sendError(ctx, "NOT_IMPLEMENTED", "Query not implemented yet");
    }

    private void handleHeartbeat(ChannelHandlerContext ctx) {
        JsonObject response = new JsonObject();
        response.addProperty("version", "1.0");
        response.addProperty("messageId", UUID.randomUUID().toString());
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("sessionId", sessionId);
        response.addProperty("type", "heartbeat");

        JsonObject payload = new JsonObject();
        payload.addProperty("serverTime", System.currentTimeMillis());
        response.add("payload", payload);

        sendMessage(ctx, response);
    }

    private void sendMessage(ChannelHandlerContext ctx, JsonObject message) {
        String json = gson.toJson(message);
        ctx.writeAndFlush(new TextWebSocketFrame(json));
    }

    private void sendError(ChannelHandlerContext ctx, String errorCode, String errorMessage) {
        JsonObject response = new JsonObject();
        response.addProperty("version", "1.0");
        response.addProperty("messageId", UUID.randomUUID().toString());
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("sessionId", sessionId);
        response.addProperty("type", "error");

        JsonObject payload = new JsonObject();
        payload.addProperty("errorCode", errorCode);
        payload.addProperty("errorMessage", errorMessage);
        response.add("payload", payload);

        sendMessage(ctx, response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        MinecraftEduMod.LOGGER.error("Exception in WebSocket handler", cause);
        ctx.close();
    }
}
