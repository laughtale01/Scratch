package edu.minecraft.collaboration.util;

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
    public static final String ERROR_INVALID_ARGS = "invalidArguments";
    public static final String ERROR_NOT_FOUND = "notFound";
    public static final String ERROR_PERMISSION_DENIED = "permissionDenied";
    public static final String ERROR_RATE_LIMIT = "rateLimitExceeded";
    public static final String ERROR_INTERNAL = "internalError";
    public static final String ERROR_NOT_IMPLEMENTED = "notImplemented";
    public static final String ERROR_AGENT_LIMIT = "agentLimitReached";
    public static final String ERROR_NO_PLAYER = "noPlayerOnline";
    public static final String ERROR_NO_AGENT = "noActiveAgent";
    public static final String ERROR_INVALID_COORDS = "invalidCoordinates";
    public static final String ERROR_MOVE_FAILED = "moveFailed";
    public static final String ERROR_INVALID_MESSAGE = "invalidMessage";
    public static final String ERROR_REGISTRATION_FAILED = "registrationFailed";
    public static final String ERROR_ACTION_FAILED = "actionFailed";
    public static final String ERROR_DISMISS_FAILED = "dismissFailed";
    public static final String ERROR_INVALID_BLOCK = "invalidBlock";
    public static final String ERROR_SERVER_UNAVAILABLE = "serverUnavailable";
    public static final String ERROR_AREA_TOO_LARGE = "areaTooLarge";
    public static final String ERROR_FORBIDDEN = "forbidden";
    public static final String ERROR_STUDENT_NOT_FOUND = "studentNotFound";
    
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
    
    /**
     * Create agent summoned response
     */
    public static String agentSummoned(String agentName, int x, int y, int z) {
        Map<String, Object> data = new HashMap<>();
        data.put("agentName", agentName);
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        return successWithData("summonAgent", "Agent summoned successfully", data);
    }
    
    /**
     * Create teacher registered response
     */
    public static String teacherRegistered(String username) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("role", "teacher");
        return successWithData("registerTeacher", "Teacher registered successfully", data);
    }
    
    /**
     * Create not found error
     */
    public static String notFound(String command, String itemType) {
        return error(command, ERROR_NOT_FOUND, itemType + " not found");
    }
    
    /**
     * Create permission denied error
     */
    public static String permissionDenied(String command, String reason) {
        return error(command, ERROR_PERMISSION_DENIED, "Permission denied: " + reason);
    }
    
    /**
     * Create invalid arguments error
     */
    public static String invalidArguments(String command, String expected) {
        return error(command, ERROR_INVALID_ARGS, "Invalid arguments. Expected: " + expected);
    }
    
    /**
     * Create agent moved response
     */
    public static String agentMoved(int x, int y, int z) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        return successWithData("moveAgent", "Agent moved successfully", data);
    }
    
    /**
     * Create students summoned response
     */
    public static String studentsSummoned(int count, int x, int y, int z) {
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        return successWithData("summonAllStudents", "Students summoned successfully", data);
    }
    
    /**
     * Create student activities response
     */
    public static String studentActivities(Object activities) {
        Map<String, Object> data = new HashMap<>();
        data.put("activities", activities);
        return successWithData("getStudentActivities", "Student activities retrieved", data);
    }
    
    /**
     * Create broadcast sent response
     */
    public static String broadcastSent(String message, int sentCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        data.put("sentCount", sentCount);
        return successWithData("broadcastToStudents", "Message broadcast successfully", data);
    }
    
    /**
     * Create classroom mode toggled response
     */
    public static String classroomModeToggled(boolean enabled) {
        Map<String, Object> data = new HashMap<>();
        data.put("classroomMode", enabled);
        return successWithData("toggleClassroomMode", "Classroom mode " + (enabled ? "enabled" : "disabled"), data);
    }
    
    /**
     * Create permissions set response
     */
    public static String permissionsSet(String level) {
        Map<String, Object> data = new HashMap<>();
        data.put("permissionLevel", level);
        return successWithData("setGlobalPermissions", "Global permissions set to " + level, data);
    }
    
    /**
     * Create agent following response
     */
    public static String agentFollowing(boolean following) {
        Map<String, Object> data = new HashMap<>();
        data.put("following", following);
        return successWithData("agentFollow", following ? "Agent is now following" : "Agent stopped following", data);
    }
    
    /**
     * Create agent action result response
     */
    public static String agentActionResult(String action, String result) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put("result", result);
        return successWithData("agentAction", "Agent action completed", data);
    }
    
    /**
     * Create agent dismissed response
     */
    public static String agentDismissed(String agentName) {
        Map<String, Object> data = new HashMap<>();
        data.put("agentName", agentName);
        return successWithData("dismissAgent", "Agent dismissed successfully", data);
    }
    
    /**
     * Create connected response
     */
    public static String connected() {
        Map<String, Object> response = new HashMap<>();
        response.put("type", "connection");
        response.put("status", "connected");
        response.put("message", "Successfully connected to Minecraft Collaboration System");
        return gson.toJson(response);
    }
    
    /**
     * Create status response
     */
    public static String status(String status, int playerCount) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("playerCount", playerCount);
        return successWithData("status", "System status retrieved", data);
    }
    
    /**
     * Create block set response
     */
    public static String blockSet(int x, int y, int z, String blockType) {
        Map<String, Object> data = new HashMap<>();
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        data.put("blockType", blockType);
        return successWithData("setBlock", "Block placed successfully", data);
    }
    
    /**
     * Create chat sent response
     */
    public static String chatSent(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        return successWithData("chat", "Message sent successfully", data);
    }
    
    /**
     * Create fill completed response
     */
    public static String fillCompleted(int x1, int y1, int z1, int x2, int y2, int z2, String blockType, int blocksPlaced) {
        Map<String, Object> data = new HashMap<>();
        data.put("x1", x1);
        data.put("y1", y1);
        data.put("z1", z1);
        data.put("x2", x2);
        data.put("y2", y2);
        data.put("z2", z2);
        data.put("blockType", blockType);
        data.put("blocksPlaced", blocksPlaced);
        return successWithData("fill", "Fill operation completed", data);
    }
    
    /**
     * Create area filled response (alias for fillCompleted)
     */
    public static String areaFilled(int x1, int y1, int z1, int x2, int y2, int z2, String blockType, int blocksSet) {
        return fillCompleted(x1, y1, z1, x2, y2, z2, blockType, blocksSet);
    }
    
    /**
     * Create time limit set response
     */
    public static String timeLimitSet(String studentName, int minutes) {
        Map<String, Object> data = new HashMap<>();
        data.put("studentName", studentName);
        data.put("timeLimit", minutes);
        return successWithData("setStudentTimeLimit", "Time limit set successfully", data);
    }
    
    /**
     * Create restriction added response
     */
    public static String restrictionAdded(String studentName, String restriction) {
        Map<String, Object> data = new HashMap<>();
        data.put("studentName", studentName);
        data.put("restriction", restriction);
        return successWithData("addStudentRestriction", "Restriction added successfully", data);
    }
    
    /**
     * Create students changed response
     */
    public static String studentsChanged(String action, int count) {
        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put("affectedStudents", count);
        return successWithData("freezeStudents", "Students " + action + " successfully", data);
    }
}