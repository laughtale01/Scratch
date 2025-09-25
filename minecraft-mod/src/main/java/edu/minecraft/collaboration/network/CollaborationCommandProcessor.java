package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.collaboration.CollaborationManager;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.security.InputValidator;
import edu.minecraft.collaboration.util.ResponseHelper;
import org.slf4j.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Map;

/**
 * Handles collaboration-related commands from the Scratch extension
 * Includes authentication, user management, and world management
 */
public class CollaborationCommandProcessor {

    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private final AuthenticationManager authManager;
    private final CollaborationManager collaborationManager;
    private final Gson gson;

    // Current WebSocket connection identifier for authentication
    private String currentConnectionId;

    public CollaborationCommandProcessor() {
        DependencyInjector injector = DependencyInjector.getInstance();
        this.authManager = injector.getService(AuthenticationManager.class);
        this.collaborationManager = injector.getService(CollaborationManager.class);
        this.gson = new Gson();
    }

    public void setConnectionId(String connectionId) {
        this.currentConnectionId = connectionId;
    }

    /**
     * Handle authentication requests
     */
    public String handleAuthentication(Map<String, String> args) {
        String username = args.get("username");
        String token = args.get("token");

        // Validate username
        if (!InputValidator.validateUsername(username)) {
            return createErrorResponse("invalidUsername", "Invalid username format");
        }

        // If token is provided, validate it
        if (token != null && !token.isEmpty()) {
            if (authManager.validateToken(token)) {
                // For now, we'll use a simple connection ID based on username
                String connectionId = "conn_" + username;
                if (authManager.authenticateConnection(connectionId, token)) {
                    AuthenticationManager.UserRole role = authManager.getRole(token);
                    return createSuccessResponse("auth", "Authenticated as " + role.toString().toLowerCase());
                }
            }
            return createErrorResponse("authFailed", "Invalid token");
        }

        // Generate new token for student
        String newToken = authManager.generateToken(username, AuthenticationManager.UserRole.STUDENT);

        // Authenticate both username-based and WebSocket-based connection IDs
        String usernameConnectionId = "conn_" + username;
        if (authManager.authenticateConnection(usernameConnectionId, newToken)) {
            LOGGER.info("New student authenticated: {} with username connection ID: {}", username, usernameConnectionId);
        } else {
            LOGGER.error("Failed to authenticate new student username connection: {}", username);
        }

        // Also authenticate the actual WebSocket connection ID if available
        if (currentConnectionId != null) {
            if (authManager.authenticateConnection(currentConnectionId, newToken)) {
                LOGGER.info("New student authenticated: {} with WebSocket connection ID: {}", username, currentConnectionId);
            } else {
                LOGGER.error("Failed to authenticate new student WebSocket connection: {}", username);
            }
        }

        return "{\"type\":\"auth\",\"status\":\"success\",\"token\":\"" + newToken + "\",\"role\":\"student\"}";
    }

    /**
     * Handle user info requests
     */
    public String handleGetUserInfo(Map<String, String> args) {
        String connectionId = args.get("connectionId");
        if (connectionId == null) {
            connectionId = "conn_" + args.get("username");
        }

        if (!authManager.isAuthenticated(connectionId)) {
            return createErrorResponse("unauthenticated", "User not authenticated");
        }

        String username = authManager.getUsername(connectionId);
        AuthenticationManager.UserRole role = authManager.getRoleForConnection(connectionId);
        boolean hasElevated = authManager.hasElevatedPrivileges(connectionId);

        return "{\"type\":\"userInfo\",\"username\":\"" + username
               + "\",\"role\":\"" + role.toString().toLowerCase()
               + "\",\"hasElevatedPrivileges\":" + hasElevated + "}";
    }

    /**
     * Get current world name for player
     */
    public String handleGetCurrentWorld() {
        try {
            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                String playerName = player.getName().getString();

                // Get current world from CollaborationManager
                String worldName = collaborationManager.getPlayerCurrentWorld(playerName);

                // If unknown, get from server
                if ("unknown".equals(worldName)) {
                    worldName = player.level().dimension().location().toString();
                    collaborationManager.setPlayerWorld(playerName, worldName);
                }

                return ResponseHelper.currentWorld(worldName);
            }
            return ResponseHelper.currentWorld("overworld");
        } catch (Exception e) {
            LOGGER.error("Error getting current world", e);
            return ResponseHelper.error("getCurrentWorld", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }

    // Helper methods for creating responses
    private String createErrorResponse(String errorType, String details) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "error");
        response.addProperty("error", errorType);
        response.addProperty("message", details);
        return gson.toJson(response);
    }

    private String createSuccessResponse(String type, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("type", type);
        response.addProperty("status", "success");
        response.addProperty("message", message);
        return gson.toJson(response);
    }
}
