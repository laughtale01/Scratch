package com.yourname.minecraftcollaboration.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for creating consistent JSON responses
 */
public class ResponseHelper {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Response types
    public static final String TYPE_SUCCESS = "success";
    public static final String TYPE_ERROR = "error";
    public static final String TYPE_EVENT = "event";
    public static final String TYPE_DATA = "data";
    
    // Common error codes
    public static final String ERROR_INVALID_PARAMS = "invalidParameters";
    public static final String ERROR_NOT_FOUND = "notFound";
    public static final String ERROR_PERMISSION_DENIED = "permissionDenied";
    public static final String ERROR_RATE_LIMIT = "rateLimitExceeded";
    public static final String ERROR_INTERNAL = "internalError";
    public static final String ERROR_NOT_IMPLEMENTED = "notImplemented";
    
    /**
     * Create a success response
     */
    public static String success(String command, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", TYPE_SUCCESS);
        response.put("command", command);
        response.put("message", message);
        response.put("status", "success");
        return gson.toJson(response);
    }
    
    /**
     * Create a success response with data
     */
    public static String successWithData(String command, String message, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", TYPE_SUCCESS);
        response.put("command", command);
        response.put("message", message);
        response.put("data", data);
        response.put("status", "success");
        return gson.toJson(response);
    }
    
    /**
     * Create an error response
     */
    public static String error(String command, String errorCode, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", TYPE_ERROR);
        response.put("command", command);
        response.put("error", errorCode);
        response.put("message", message);
        response.put("status", "error");
        return gson.toJson(response);
    }
    
    /**
     * Create a data response
     */
    public static String data(String dataType, Map<String, Object> data) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", TYPE_DATA);
        response.put("dataType", dataType);
        response.put("data", data);
        response.put("status", "success");
        return gson.toJson(response);
    }
    
    /**
     * Create an event response
     */
    public static String event(String eventType, Map<String, Object> eventData) {
        Map<String, Object> response = new HashMap<>();
        response.put("type", TYPE_EVENT);
        response.put("event", eventType);
        response.put("data", eventData);
        response.put("status", "success");
        return gson.toJson(response);
    }
    
    /**
     * Create player position response
     */
    public static String playerPosition(double x, double y, double z) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        return data("playerPos", data);
    }
    
    /**
     * Create block info response
     */
    public static String blockInfo(String blockType, int x, int y, int z) {
        Map<String, Object> data = new HashMap<>();
        data.put("block", blockType);
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        return data("blockInfo", data);
    }
    
    /**
     * Create invitation count response
     */
    public static String invitationCount(int count) {
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return data("invitations", data);
    }
    
    /**
     * Create current world response
     */
    public static String currentWorld(String worldName) {
        Map<String, Object> data = new HashMap<>();
        data.put("world", worldName);
        return data("currentWorld", data);
    }
    
    /**
     * Create welcome message
     */
    public static String welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "welcome");
        response.put("message", "Connected to Minecraft Collaboration System v1.0");
        response.put("protocol", "1.0");
        
        // Add available commands
        String[] commands = {
            "placeBlock", "removeBlock", "getBlock", "fill",
            "buildCircle", "buildSphere", "buildWall", "buildHouse",
            "getPlayerPos", "teleport", "gamemode", "time", "weather",
            "chat", "invite", "requestVisit", "approveVisit",
            "returnHome", "emergencyReturn", "getCurrentWorld", "getInvitations"
        };
        response.put("availableCommands", commands);
        
        return gson.toJson(response);
    }
}