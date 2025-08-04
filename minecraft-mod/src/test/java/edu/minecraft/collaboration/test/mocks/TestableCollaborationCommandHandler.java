package edu.minecraft.collaboration.test.mocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Testable version of CollaborationCommandHandler that doesn't require Minecraft runtime
 */
public class TestableCollaborationCommandHandler {
    
    private final Map<String, String> mockResponses = new HashMap<>();
    
    public TestableCollaborationCommandHandler() {
        // Don't call super - avoid Minecraft dependency
        setupMockResponses();
    }
    
    private void setupMockResponses() {
        // Setup default mock responses
        mockResponses.put("getPlayerPos", "{\"status\":\"success\",\"x\":100,\"y\":64,\"z\":200}");
        mockResponses.put("chat", "{\"status\":\"success\",\"message\":\"Message sent\"}");
        mockResponses.put("placeBlock", "{\"status\":\"success\",\"message\":\"Block placed\"}");
        mockResponses.put("getBlock", "{\"status\":\"success\",\"block\":\"stone\"}");
        mockResponses.put("setHome", "{\"status\":\"success\",\"message\":\"Home position set\"}");
        mockResponses.put("goHome", "{\"status\":\"success\",\"message\":\"Teleported home\"}");
        mockResponses.put("createInvitation", "{\"status\":\"success\",\"message\":\"Invitation created\"}");
        mockResponses.put("acceptInvitation", "{\"status\":\"success\",\"message\":\"Invitation accepted\"}");
        mockResponses.put("requestVisit", "{\"status\":\"success\",\"message\":\"Visit requested\"}");
        mockResponses.put("approveVisit", "{\"status\":\"success\",\"message\":\"Visit approved\"}");
        mockResponses.put("buildCircle", "{\"status\":\"success\",\"message\":\"Circle built\"}");
        mockResponses.put("buildSphere", "{\"status\":\"success\",\"message\":\"Sphere built\"}");
        mockResponses.put("buildWall", "{\"status\":\"success\",\"message\":\"Wall built\"}");
        mockResponses.put("buildHouse", "{\"status\":\"success\",\"message\":\"House built\"}");
        mockResponses.put("fillArea", "{\"status\":\"success\",\"message\":\"Area filled\"}");
        mockResponses.put("teleport", "{\"status\":\"success\",\"message\":\"Teleported\"}");
        mockResponses.put("emergencyReturn", "{\"status\":\"success\",\"message\":\"Emergency return completed\"}");
        mockResponses.put("createAgent", "{\"status\":\"success\",\"message\":\"Agent created\"}");
        mockResponses.put("moveAgent", "{\"status\":\"success\",\"message\":\"Agent moved\"}");
        mockResponses.put("turnAgent", "{\"status\":\"success\",\"message\":\"Agent turned\"}");
        mockResponses.put("agentPlace", "{\"status\":\"success\",\"message\":\"Agent placed block\"}");
        mockResponses.put("agentBreak", "{\"status\":\"success\",\"message\":\"Agent broke block\"}");
        mockResponses.put("startClassroom", "{\"status\":\"success\",\"message\":\"Classroom mode started\"}");
        mockResponses.put("setDifficulty", "{\"status\":\"success\",\"message\":\"Difficulty set\"}");
        mockResponses.put("restrictBlocks", "{\"status\":\"success\",\"message\":\"Blocks restricted\"}");
    }
    
    public String executeCommand(String command, String[] args) {
        // Handle special cases for testing
        if (command == null || command.isEmpty()) {
            return "{\"status\":\"error\",\"message\":\"No command specified\"}";
        }
        
        // For coordinate validation tests
        if (command.equals("placeBlock") && args != null && args.length >= 4) {
            try {
                Integer.parseInt(args[0]);
                Integer.parseInt(args[1]);
                Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                return "{\"status\":\"error\",\"message\":\"Invalid coordinates\"}";
            }
        }
        
        // For chat message tests
        if (command.equals("chat") && args != null && args.length > 0) {
            String message = String.join(" ", args);
            if (message.length() > 10000) {
                return "{\"status\":\"error\",\"message\":\"Message too long\"}";
            }
            return "{\"status\":\"success\",\"message\":\"" + message + "\"}";
        }
        
        // Return mock response
        return mockResponses.getOrDefault(command, 
            "{\"status\":\"error\",\"message\":\"Unknown command: " + command + "\"}");
    }
    
    public boolean hasPermission(String playerName, String permission) {
        // Always return true for testing
        return true;
    }
    
    public void setMockResponse(String command, String response) {
        mockResponses.put(command, response);
    }
    
    // Command handler method mappings
    public String handleSetBlock(String[] args) {
        if (args == null || args.length < 4) {
            return "{\"status\":\"error\",\"message\":\"Invalid arguments for setBlock\"}";
        }
        return executeCommand("placeBlock", args);
    }
    
    public String handleGetPlayerPosition(String[] args) {
        return executeCommand("getPlayerPos", args);
    }
    
    public String handleChatMessage(String[] args) {
        if (args == null || args.length == 0) {
            return "{\"status\":\"error\",\"message\":\"No message provided\"}";
        }
        return executeCommand("chat", args);
    }
    
    public String handleConnect(String[] args) {
        return "{\"status\":\"success\",\"message\":\"Connected\"}";
    }
    
    public String handleStatus(String[] args) {
        return "{\"status\":\"success\",\"players\":1}";
    }
    
    public String handleGetCurrentWorld(String[] args) {
        return "{\"status\":\"success\",\"world\":\"overworld\"}";
    }
    
    public String handleGetInvitations(String[] args) {
        return "{\"status\":\"success\",\"invitations\":[]}";
    }
    
    public String handleReturnHome(String[] args) {
        return executeCommand("goHome", args);
    }
    
    public String handleEmergencyReturn(String[] args) {
        return executeCommand("emergencyReturn", args);
    }
    
    public String handleInviteFriend(String[] args) {
        return executeCommand("createInvitation", args);
    }
    
    public String handleRequestVisit(String[] args) {
        return executeCommand("requestVisit", args);
    }
    
    public String handleApproveVisit(String[] args) {
        return executeCommand("approveVisit", args);
    }
    
    public String handleSummonAgent(String[] args) {
        return executeCommand("createAgent", args);
    }
    
    public String handleMoveAgent(String[] args) {
        return executeCommand("moveAgent", args);
    }
    
    public String handleAgentAction(String[] args) {
        return executeCommand("agentPlace", args);
    }
    
    public String handleAgentFollow(String[] args) {
        return "{\"status\":\"success\",\"message\":\"Agent following\"}";
    }
    
    public String handleDismissAgent(String[] args) {
        return "{\"status\":\"success\",\"message\":\"Agent dismissed\"}";
    }
    
    public String handleGetBlock(String[] args) {
        return executeCommand("getBlock", args);
    }
    
    public String handleFillArea(String[] args) {
        return executeCommand("fillArea", args);
    }
    
    public String handleToggleClassroomMode(String[] args) {
        return executeCommand("startClassroom", args);
    }
    
    public String handleRegisterTeacher(String[] args) {
        return "{\"status\":\"success\",\"message\":\"Teacher registered\"}";
    }
    
    public String handleSetGlobalPermissions(String[] args) {
        return "{\"status\":\"success\",\"message\":\"Permissions set\"}";
    }
}