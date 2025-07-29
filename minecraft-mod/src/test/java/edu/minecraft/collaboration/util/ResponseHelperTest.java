package edu.minecraft.collaboration.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for ResponseHelper
 */
public class ResponseHelperTest {
    
    @Test
    public void testSuccessResponse() {
        String response = ResponseHelper.success("testCommand", "Operation completed");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("success", json.get("type").getAsString());
        assertEquals("testCommand", json.get("command").getAsString());
        assertEquals("Operation completed", json.get("message").getAsString());
        assertEquals("success", json.get("status").getAsString());
    }
    
    @Test
    public void testSuccessWithData() {
        Map<String, Object> data = new HashMap<>();
        data.put("count", 5);
        data.put("name", "test");
        
        String response = ResponseHelper.successWithData("testCommand", "Data retrieved", data);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("success", json.get("type").getAsString());
        assertEquals("testCommand", json.get("command").getAsString());
        assertEquals("Data retrieved", json.get("message").getAsString());
        assertTrue(json.has("data"));
        
        JsonObject dataJson = json.get("data").getAsJsonObject();
        assertEquals(5, dataJson.get("count").getAsInt());
        assertEquals("test", dataJson.get("name").getAsString());
    }
    
    @Test
    public void testErrorResponse() {
        String response = ResponseHelper.error("testCommand", "invalidInput", "Invalid parameters");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("error", json.get("type").getAsString());
        assertEquals("testCommand", json.get("command").getAsString());
        assertEquals("invalidInput", json.get("error").getAsString());
        assertEquals("Invalid parameters", json.get("message").getAsString());
        assertEquals("error", json.get("status").getAsString());
    }
    
    @Test
    public void testDataResponse() {
        Map<String, Object> data = new HashMap<>();
        data.put("value", 42);
        
        String response = ResponseHelper.data("measurement", data);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("data", json.get("type").getAsString());
        assertEquals("measurement", json.get("dataType").getAsString());
        assertEquals("success", json.get("status").getAsString());
        assertTrue(json.has("data"));
    }
    
    @Test
    public void testEventResponse() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("player", "Steve");
        eventData.put("action", "joined");
        
        String response = ResponseHelper.event("playerJoin", eventData);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("event", json.get("type").getAsString());
        assertEquals("playerJoin", json.get("event").getAsString());
        assertEquals("success", json.get("status").getAsString());
        assertTrue(json.has("data"));
    }
    
    @Test
    public void testPlayerPositionResponse() {
        String response = ResponseHelper.playerPosition(100.5, 64.0, -200.5);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("data", json.get("type").getAsString());
        assertEquals("playerPos", json.get("dataType").getAsString());
        
        JsonObject data = json.get("data").getAsJsonObject();
        assertEquals(100.5, data.get("x").getAsDouble());
        assertEquals(64.0, data.get("y").getAsDouble());
        assertEquals(-200.5, data.get("z").getAsDouble());
    }
    
    @Test
    public void testBlockInfoResponse() {
        String response = ResponseHelper.blockInfo("minecraft:stone", 10, 64, 20);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("data", json.get("type").getAsString());
        assertEquals("blockInfo", json.get("dataType").getAsString());
        
        JsonObject data = json.get("data").getAsJsonObject();
        assertEquals("minecraft:stone", data.get("block").getAsString());
        assertEquals(10, data.get("x").getAsInt());
        assertEquals(64, data.get("y").getAsInt());
        assertEquals(20, data.get("z").getAsInt());
    }
    
    @Test
    public void testInvitationCountResponse() {
        String response = ResponseHelper.invitationCount(3);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("data", json.get("type").getAsString());
        assertEquals("invitations", json.get("dataType").getAsString());
        
        JsonObject data = json.get("data").getAsJsonObject();
        assertEquals(3, data.get("count").getAsInt());
    }
    
    @Test
    public void testCurrentWorldResponse() {
        String response = ResponseHelper.currentWorld("minecraft:overworld");
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("data", json.get("type").getAsString());
        assertEquals("currentWorld", json.get("dataType").getAsString());
        
        JsonObject data = json.get("data").getAsJsonObject();
        assertEquals("minecraft:overworld", data.get("world").getAsString());
    }
    
    @Test
    public void testWelcomeMessage() {
        String response = ResponseHelper.welcome();
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertEquals("welcome", json.get("type").getAsString());
        assertEquals("Connected to Minecraft Collaboration System v1.0", json.get("message").getAsString());
        assertEquals("1.0", json.get("protocol").getAsString());
        
        // Check that available commands are included
        assertTrue(json.has("availableCommands"));
        assertTrue(json.get("availableCommands").isJsonArray());
        assertTrue(json.get("availableCommands").getAsJsonArray().size() > 0);
    }
    
    @Test
    public void testCommonErrorCodes() {
        // Test that common error codes are defined
        assertEquals("invalidParameters", ResponseHelper.ERROR_INVALID_PARAMS);
        assertEquals("notFound", ResponseHelper.ERROR_NOT_FOUND);
        assertEquals("permissionDenied", ResponseHelper.ERROR_PERMISSION_DENIED);
        assertEquals("rateLimitExceeded", ResponseHelper.ERROR_RATE_LIMIT);
        assertEquals("internalError", ResponseHelper.ERROR_INTERNAL);
        assertEquals("notImplemented", ResponseHelper.ERROR_NOT_IMPLEMENTED);
    }
}