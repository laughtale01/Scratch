package com.github.minecraftedu.network;

import com.github.minecraftedu.MinecraftEduMod;
import com.github.minecraftedu.commands.CommandExecutor;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class MinecraftWebSocketHandler {

    private final MinecraftServer minecraftServer;
    private final Gson gson;
    private final CommandExecutor commandExecutor;
    private String sessionId;

    public MinecraftWebSocketHandler(MinecraftServer minecraftServer) {
        this.minecraftServer = minecraftServer;
        this.gson = new Gson();
        this.commandExecutor = new CommandExecutor(minecraftServer);
    }

    public String handleMessage(String request) {
        MinecraftEduMod.LOGGER.debug("Received: " + request);

        try {
            JsonObject message = gson.fromJson(request, JsonObject.class);
            return processMessage(message);
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Error processing message", e);
            return createError("INTERNAL_ERROR", "Failed to process message: " + e.getMessage());
        }
    }

    private String processMessage(JsonObject message) {
        String type = message.get("type").getAsString();

        switch (type) {
            case "connect":
                return handleConnect(message);

            case "command":
                return handleCommand(message);

            case "query":
                return handleQuery(message);

            case "heartbeat":
                return handleHeartbeat();

            default:
                return createError("UNKNOWN_TYPE", "Unknown message type: " + type);
        }
    }

    private String handleConnect(JsonObject message) {
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

        // 元のリクエストのmessageIdを含める
        if (message.has("messageId")) {
            response.addProperty("requestId", message.get("messageId").getAsString());
        }

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

        return gson.toJson(response);
    }

    private String handleCommand(JsonObject message) {
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

            // 元のリクエストのmessageIdを含める
            if (message.has("messageId")) {
                response.addProperty("requestId", message.get("messageId").getAsString());
            }

            JsonObject responsePayload = new JsonObject();
            responsePayload.addProperty("success", success);
            responsePayload.addProperty("action", action);

            if (success) {
                // コマンド実行結果を取得
                JsonObject result = commandExecutor.getLastResult();
                if (result.size() == 0) {
                    // 結果データがない場合はデフォルトメッセージ
                    result.addProperty("message", "Command executed successfully");
                }
                responsePayload.add("result", result);
            } else {
                responsePayload.addProperty("errorCode", "COMMAND_FAILED");
                responsePayload.addProperty("errorMessage", "Failed to execute command");
            }

            response.add("payload", responsePayload);
            return gson.toJson(response);

        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Command execution error", e);
            return createError("COMMAND_FAILED", e.getMessage());
        }
    }

    private String handleQuery(JsonObject message) {
        JsonObject payload = message.getAsJsonObject("payload");
        String action = payload.get("action").getAsString();
        JsonObject params = payload.has("params") ? payload.getAsJsonObject("params") : new JsonObject();

        MinecraftEduMod.LOGGER.info("Executing query: " + action);

        try {
            // クエリ実行（コマンドExecutorを使用）
            boolean success = commandExecutor.execute(action, params);

            // レスポンス
            JsonObject response = new JsonObject();
            response.addProperty("version", "1.0");
            response.addProperty("messageId", UUID.randomUUID().toString());
            response.addProperty("timestamp", System.currentTimeMillis());
            response.addProperty("sessionId", sessionId);
            response.addProperty("type", "query_response");

            // 元のリクエストのmessageIdを含める
            if (message.has("messageId")) {
                response.addProperty("requestId", message.get("messageId").getAsString());
            }

            JsonObject responsePayload = new JsonObject();
            responsePayload.addProperty("success", success);
            responsePayload.addProperty("action", action);

            if (success) {
                // クエリ実行結果を取得
                JsonObject result = commandExecutor.getLastResult();
                if (result.size() == 0) {
                    // 結果データがない場合はデフォルトメッセージ
                    result.addProperty("message", "Query executed successfully");
                }
                responsePayload.add("result", result);
            } else {
                responsePayload.addProperty("errorCode", "QUERY_FAILED");
                responsePayload.addProperty("errorMessage", "Failed to execute query");
            }

            response.add("payload", responsePayload);
            return gson.toJson(response);

        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Query execution error", e);
            return createError("QUERY_FAILED", e.getMessage());
        }
    }

    private String handleHeartbeat() {
        JsonObject response = new JsonObject();
        response.addProperty("version", "1.0");
        response.addProperty("messageId", UUID.randomUUID().toString());
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("sessionId", sessionId);
        response.addProperty("type", "heartbeat");

        JsonObject payload = new JsonObject();
        payload.addProperty("serverTime", System.currentTimeMillis());
        response.add("payload", payload);

        return gson.toJson(response);
    }

    private String createError(String errorCode, String errorMessage) {
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

        return gson.toJson(response);
    }
}
