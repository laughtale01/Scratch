package edu.minecraft.collaboration.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import edu.minecraft.collaboration.security.InputValidator;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Handles message parsing and routing for WebSocket communications
 */
public final class MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);
    private static final Gson GSON = new Gson();

    // Private constructor to prevent instantiation
    private MessageHandler() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Message types supported by the system
     */
    public enum MessageType {
        COMMAND("command"),
        QUERY("query"),
        EVENT("event"),
        RESPONSE("response"),
        ERROR("error"),
        HEARTBEAT("heartbeat");

        private final String type;

        MessageType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

        public static MessageType fromString(String type) {
            for (MessageType mt : values()) {
                if (mt.type.equalsIgnoreCase(type)) {
                    return mt;
                }
            }
            return null;
        }
    }

    /**
     * Parse incoming message and determine format
     * @param message Raw message string
     * @return Parsed message object
     */
    public static Message parseMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new Message(MessageType.ERROR, "error", "Empty message");
        }

        // Try JSON format first
        if (message.trim().startsWith("{")) {
            try {
                JsonObject json = GSON.fromJson(message, JsonObject.class);
                String type = json.has("type") ? json.get("type").getAsString() : "command";
                String action = json.has("action") ? json.get("action").getAsString()
                               : json.has("command") ? json.get("command").getAsString() : "unknown";

                MessageType messageType = MessageType.fromString(type);
                if (messageType == null) {
                    messageType = MessageType.COMMAND;
                }

                return new Message(messageType, action, json);
            } catch (JsonSyntaxException e) {
                LOGGER.warn("Failed to parse JSON message: {}", e.getMessage());
            }
        }

        // Fall back to legacy format: command(arg1,arg2,...)
        return parseLegacyFormat(message);
    }

    /**
     * Parse legacy command format
     * @param message Message in format "command(arg1,arg2,...)"
     * @return Parsed message object
     */
    private static Message parseLegacyFormat(String message) {
        String command;
        String[] args = new String[0];

        int parenIndex = message.indexOf('(');
        if (parenIndex > 0 && message.endsWith(")")) {
            command = message.substring(0, parenIndex).trim();
            String argsString = message.substring(parenIndex + 1, message.length() - 1);
            if (!argsString.isEmpty()) {
                args = argsString.split(",");
                for (int i = 0; i < args.length; i++) {
                    args[i] = args[i].trim();
                }
            }
        } else {
            command = message.trim();
        }

        // Validate command
        if (!InputValidator.validateCommand(command)) {
            return new Message(MessageType.ERROR, "error", "Invalid command format");
        }

        // Create JSON representation for consistency
        JsonObject json = new JsonObject();
        json.addProperty("command", command);
        json.add("args", GSON.toJsonTree(args));

        return new Message(MessageType.COMMAND, command, json);
    }

    /**
     * Create a response message
     * @param success Whether the operation was successful
     * @param data Response data
     * @return JSON response string
     */
    public static String createResponse(boolean success, Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("type", MessageType.RESPONSE.getType());
        response.addProperty("success", success);
        response.addProperty("timestamp", System.currentTimeMillis());

        if (data instanceof String) {
            response.addProperty("data", (String) data);
        } else if (data instanceof Number) {
            response.addProperty("data", (Number) data);
        } else if (data instanceof Boolean) {
            response.addProperty("data", (Boolean) data);
        } else {
            response.add("data", GSON.toJsonTree(data));
        }

        return GSON.toJson(response);
    }

    /**
     * Create an error response
     * @param error Error message
     * @param details Additional error details
     * @return JSON error response string
     */
    public static String createError(String error, String details) {
        JsonObject response = new JsonObject();
        response.addProperty("type", MessageType.ERROR.getType());
        response.addProperty("success", false);
        response.addProperty("error", error);
        if (details != null) {
            response.addProperty("details", details);
        }
        response.addProperty("timestamp", System.currentTimeMillis());

        return GSON.toJson(response);
    }

    /**
     * Create a heartbeat message
     * @return JSON heartbeat string
     */
    public static String createHeartbeat() {
        JsonObject heartbeat = new JsonObject();
        heartbeat.addProperty("type", MessageType.HEARTBEAT.getType());
        heartbeat.addProperty("timestamp", System.currentTimeMillis());
        heartbeat.addProperty("status", "alive");

        return GSON.toJson(heartbeat);
    }

    /**
     * Send message asynchronously
     * @param connection WebSocket connection
     * @param message Message to send
     * @return CompletableFuture for the send operation
     */
    public static CompletableFuture<Void> sendAsync(WebSocket connection, String message) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (connection != null && connection.isOpen()) {
                    connection.send(message);
                    LOGGER.debug("Sent message to {}: {}",
                        connection.getRemoteSocketAddress(), message);
                } else {
                    LOGGER.warn("Cannot send message - connection closed");
                }
            } catch (Exception e) {
                LOGGER.error("Error sending message", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Message wrapper class
     */
    public static class Message {
        private final MessageType type;
        private final String action;
        private final JsonObject data;

        public Message(MessageType type, String action, Object data) {
            this.type = type;
            this.action = action;

            if (data instanceof JsonObject) {
                this.data = (JsonObject) data;
            } else if (data instanceof String) {
                this.data = new JsonObject();
                this.data.addProperty("message", (String) data);
            } else {
                this.data = new JsonObject();
                this.data.add("data", GSON.toJsonTree(data));
            }
        }

        public MessageType getType() {
            return type;
        }

        public String getAction() {
            return action;
        }

        public JsonObject getData() {
            return data;
        }

        public String[] getLegacyArgs() {
            if (data.has("args") && data.get("args").isJsonArray()) {
                return GSON.fromJson(data.get("args"), String[].class);
            }
            return new String[0];
        }

        @Override
        public String toString() {
            return String.format("Message[type=%s, action=%s, data=%s]",
                type, action, data);
        }
    }
}
