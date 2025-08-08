package edu.minecraft.collaboration.test.mocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Testable version of CollaborationMessageProcessor that doesn't require Minecraft runtime
 */
public class TestableCollaborationMessageProcessor {
    
    private final Map<String, Boolean> mockAuthenticationStatus = new HashMap<>();
    private final TestableCollaborationCommandHandler testableCommandHandler;
    
    public TestableCollaborationMessageProcessor() {
        // Use our testable version
        this.testableCommandHandler = new TestableCollaborationCommandHandler();
    }
    
    public String processMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "{\"status\":\"error\",\"message\":\"emptyMessage\"}";
        }
        
        // Try to parse as JSON first
        if (message.trim().startsWith("{")) {
            try {
                return processJsonMessage(message);
            } catch (Exception e) {
                // Fall back to legacy format
            }
        }
        
        // Process as legacy format
        return processLegacyMessage(message);
    }
    
    private String processJsonMessage(String message) {
        try {
            // Simple JSON parsing for testing
            if (message.contains("\"type\":\"placeBlock\"")) {
                // For placeBlock, simulate authentication requirement
                return "{\"status\":\"error\",\"message\":\"unauthenticated\"}";
            } else if (message.contains("\"type\":\"getPlayerPos\"")) {
                return "{\"status\":\"success\",\"x\":100,\"y\":64,\"z\":200}";
            } else if (message.contains("\"type\":\"chat\"")) {
                return "{\"status\":\"success\",\"message\":\"Message sent\"}";
            } else if (message.contains("\"type\":\"ping\"")) {
                return "{\"status\":\"success\",\"message\":\"pong\"}";
            } else {
                return "{\"status\":\"error\",\"message\":\"Unknown command type\"}";
            }
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"Invalid JSON format\"}";
        }
    }
    
    private String processLegacyMessage(String message) {
        try {
            // Handle legacy format: command(args)
            int parenIndex = message.indexOf('(');
            if (parenIndex > 0 && message.endsWith(")")) {
                String command = message.substring(0, parenIndex);
                String argsStr = message.substring(parenIndex + 1, message.length() - 1);
                String[] args = argsStr.isEmpty() ? new String[0] : argsStr.split(",");
                
                // Trim args
                for (int i = 0; i < args.length; i++) {
                    args[i] = args[i].trim();
                }
                
                return testableCommandHandler.executeCommand(command, args);
            }
            
            // Simple command without args
            return testableCommandHandler.executeCommand(message.trim(), new String[0]);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"Invalid command format\"}";
        }
    }
    
    public void setAuthenticated(String connectionId, boolean authenticated) {
        mockAuthenticationStatus.put(connectionId, authenticated);
    }
    
    public void setMockResponse(String command, String response) {
        testableCommandHandler.setMockResponse(command, response);
    }
}