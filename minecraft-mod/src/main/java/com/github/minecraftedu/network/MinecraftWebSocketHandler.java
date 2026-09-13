package com.github.minecraftedu.network;

import com.github.minecraftedu.MinecraftEduMod;
import com.github.minecraftedu.commands.CommandExecutor;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public class MinecraftWebSocketHandler {

    private final MinecraftServer minecraftServer;
    private final Gson gson;
    private final CommandExecutor commandExecutor;
    private String sessionId;

    public MinecraftWebSocketHandler(MinecraftServer minecraftServer, CommandExecutor commandExecutor) {
        this.minecraftServer = minecraftServer;
        this.gson = new Gson();
        this.commandExecutor = commandExecutor;  // 外部から受け取る（全クライアントで共有）
    }

    public String handleMessage(String request) {
        MinecraftEduMod.LOGGER.debug("Received: " + request);

        // catch でも requestId を載せられるよう、try の外で宣言する
        JsonObject message = null;
        try {
            message = gson.fromJson(request, JsonObject.class);
            return processMessage(message);
        } catch (Exception e) {
            // 例外の詳細はログのみ。応答に載せると内部情報が子どもの画面に届きうる
            MinecraftEduMod.LOGGER.error("Error processing message", e);
            return createError("INTERNAL_ERROR", "Failed to process message", getRequestId(message));
        }
    }

    private String processMessage(JsonObject message) {
        String type = message.has("type") ? message.get("type").getAsString() : "unknown";

        // Defense in depth: サーバ停止中は heartbeat 以外を全て拒否
        // CommandExecutor.execute() の入口チェックでも防がれるが、
        // connect / heartbeat は CommandExecutor を経由しないため、ここでもガードする
        if (commandExecutor != null && commandExecutor.isServerStopping()) {
            if (!"heartbeat".equals(type)) {
                return createError("SERVER_STOPPING",
                    "Server is stopping, please reconnect after world reload",
                    getRequestId(message));
            }
        }

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
                return createError("UNKNOWN_TYPE", "Unknown message type: " + type,
                    getRequestId(message));
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

    /**
     * 実行結果から「理由付きの失敗」を取り出す
     * result が null だと失敗理由を Scratch 側へ伝えられないため、
     * success:false + errorCode を持つ結果を失敗とみなす。
     * getAsBoolean() は Boolean 以外が入っていると文字列として解釈してしまうので、
     * boolean であることを確かめてから読む。
     * @param result 実行結果（null 不可）
     * @param defaultCode errorCode が無い場合に使うコード
     * @return 失敗なら errorCode、成功なら null
     */
    private String extractFailureCode(JsonObject result, String defaultCode) {
        JsonElement successElement = result.get("success");
        if (successElement == null
                || !successElement.isJsonPrimitive()
                || !successElement.getAsJsonPrimitive().isBoolean()
                || successElement.getAsBoolean()) {
            return null;
        }
        JsonElement codeElement = result.get("errorCode");
        if (codeElement != null && codeElement.isJsonPrimitive()) {
            return codeElement.getAsString();
        }
        return defaultCode;
    }

    private String handleCommand(JsonObject message) {
        try {
            // payload の取り出しも try の内側に入れる。外に出すと payload 欠落時の NPE が
            // handleMessage の catch に抜け、requestId 無しの応答になってハングする
            JsonObject payload = message.getAsJsonObject("payload");
            String action = payload.get("action").getAsString();
            JsonObject params = payload.getAsJsonObject("params");

            MinecraftEduMod.LOGGER.info("Executing command: " + action);

            // コマンド実行（結果はJsonObjectで直接返される）
            JsonObject result = commandExecutor.execute(action, params);
            boolean success = (result != null);

            // コマンド側が「理由付きの失敗」を返した場合はエラーとして扱う
            String errorCode = "COMMAND_FAILED";
            if (success) {
                String failureCode = extractFailureCode(result, "COMMAND_FAILED");
                if (failureCode != null) {
                    success = false;
                    errorCode = failureCode;
                }
            }

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
                if (result.size() == 0) {
                    // 結果データがない場合はデフォルトメッセージ
                    result.addProperty("message", "Command executed successfully");
                }
                responsePayload.add("result", result);
            } else {
                // errorMessage はログ・デバッグ用。子どもへの表示文言は Scratch 側が
                // errorCode から組み立てるため、ここに内部情報を載せない。
                responsePayload.addProperty("errorCode", errorCode);
                responsePayload.addProperty("errorMessage", "Failed to execute command");
            }

            response.add("payload", responsePayload);
            return gson.toJson(response);

        } catch (Exception e) {
            // 例外の詳細はログのみ。応答に載せると内部情報が子どもの画面に届きうる
            MinecraftEduMod.LOGGER.error("Command execution error", e);
            return createError("COMMAND_FAILED", "Command execution error", getRequestId(message));
        }
    }

    private String handleQuery(JsonObject message) {
        try {
            // payload の取り出しも try の内側に入れる（handleCommand と同じ理由）
            JsonObject payload = message.getAsJsonObject("payload");
            String action = payload.get("action").getAsString();
            JsonObject params = payload.has("params") ? payload.getAsJsonObject("params") : new JsonObject();

            MinecraftEduMod.LOGGER.info("Executing query: " + action);

            // クエリ実行（結果はJsonObjectで直接返される）
            JsonObject result = commandExecutor.execute(action, params);
            boolean success = (result != null);

            // command 側と同じく「理由付きの失敗」を失敗として扱う。
            // 揃えないと同じ実行結果がクエリ経路でだけ成功と解釈される
            String errorCode = "QUERY_FAILED";
            if (success) {
                String failureCode = extractFailureCode(result, "QUERY_FAILED");
                if (failureCode != null) {
                    success = false;
                    errorCode = failureCode;
                }
            }

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
                if (result.size() == 0) {
                    // 結果データがない場合はデフォルトメッセージ
                    result.addProperty("message", "Query executed successfully");
                }
                responsePayload.add("result", result);
            } else {
                // errorMessage はログ・デバッグ用。内部情報を載せない
                responsePayload.addProperty("errorCode", errorCode);
                responsePayload.addProperty("errorMessage", "Failed to execute query");
            }

            response.add("payload", responsePayload);
            return gson.toJson(response);

        } catch (Exception e) {
            // 例外の詳細はログのみ。応答に載せると内部情報が子どもの画面に届きうる
            MinecraftEduMod.LOGGER.error("Query execution error", e);
            return createError("QUERY_FAILED", "Query execution error", getRequestId(message));
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

    /**
     * リクエストのmessageIdを取り出す
     * これを応答に載せないと Scratch 側が応答待ちを解除できず、タイムアウトまでハングする
     * @param message 受信したメッセージ
     * @return messageId。無ければ null
     */
    private String getRequestId(JsonObject message) {
        if (message != null && message.has("messageId")) {
            JsonElement id = message.get("messageId");
            if (id != null && id.isJsonPrimitive()) {
                return id.getAsString();
            }
        }
        return null;
    }

    /**
     * エラー応答を作る
     * @param errorCode 失敗理由を表すコード。Scratch 側はこれを見て表示文言を決める
     * @param errorMessage ログ・デバッグ用の説明。子どもの画面には出さない
     * @param requestId 元のリクエストのmessageId。応答待ちの解除に必要
     * @return JSON文字列
     */
    private String createError(String errorCode, String errorMessage, String requestId) {
        JsonObject response = new JsonObject();
        response.addProperty("version", "1.0");
        response.addProperty("messageId", UUID.randomUUID().toString());
        response.addProperty("timestamp", System.currentTimeMillis());
        response.addProperty("sessionId", sessionId);
        response.addProperty("type", "error");
        if (requestId != null) {
            response.addProperty("requestId", requestId);
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("errorCode", errorCode);
        payload.addProperty("errorMessage", errorMessage);
        response.add("payload", payload);

        return gson.toJson(response);
    }
}
