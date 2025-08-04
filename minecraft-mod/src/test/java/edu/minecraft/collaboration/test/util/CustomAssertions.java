package edu.minecraft.collaboration.test.util;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Custom assertions for improved test readability and quality
 */
public class CustomAssertions {
    
    /**
     * Assert that a response is a valid success JSON
     */
    public static void assertSuccessResponse(String response) {
        assertNotNull(response, "Response should not be null");
        assertValidJson(response);
        
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(json.has("status"), "Response should have 'status' field");
        assertEquals("success", json.get("status").getAsString(), 
            "Response status should be 'success'");
    }
    
    /**
     * Assert that a response is a valid error JSON
     */
    public static void assertErrorResponse(String response) {
        assertNotNull(response, "Response should not be null");
        assertValidJson(response);
        
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(json.has("status") || json.has("error"), 
            "Response should have 'status' or 'error' field");
        
        if (json.has("status")) {
            assertEquals("error", json.get("status").getAsString(), 
                "Response status should be 'error'");
        }
    }
    
    /**
     * Assert that a response contains a specific message
     */
    public static void assertResponseContains(String response, String expectedContent) {
        assertNotNull(response, "Response should not be null");
        assertTrue(response.contains(expectedContent), 
            "Response should contain: " + expectedContent + "\nActual: " + response);
    }
    
    /**
     * Assert that a string is valid JSON
     */
    public static void assertValidJson(String json) {
        assertNotNull(json, "JSON string should not be null");
        try {
            JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            fail("Invalid JSON: " + e.getMessage() + "\nJSON: " + json);
        }
    }
    
    /**
     * Assert that a response has specific fields
     */
    public static void assertHasFields(String response, String... fields) {
        assertValidJson(response);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        for (String field : fields) {
            assertTrue(json.has(field), 
                "Response should have field '" + field + "'");
        }
    }
    
    /**
     * Assert coordinate values are valid
     */
    public static void assertValidCoordinates(String response) {
        assertValidJson(response);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        
        assertHasFields(response, "x", "y", "z");
        
        // Check that coordinates are numbers
        assertTrue(json.get("x").isJsonPrimitive(), "X should be a number");
        assertTrue(json.get("y").isJsonPrimitive(), "Y should be a number");
        assertTrue(json.get("z").isJsonPrimitive(), "Z should be a number");
        
        // Check Y bounds (0-384 for Minecraft 1.20)
        int y = json.get("y").getAsInt();
        assertTrue(y >= -64 && y <= 320, 
            "Y coordinate should be between -64 and 320, but was: " + y);
    }
    
    /**
     * Assert rate limit response
     */
    public static void assertRateLimited(String response) {
        assertErrorResponse(response);
        assertTrue(response.toLowerCase().contains("rate") || 
                  response.toLowerCase().contains("limit") ||
                  response.toLowerCase().contains("too many"),
            "Response should indicate rate limiting");
    }
    
    /**
     * Assert authentication required response
     */
    public static void assertAuthenticationRequired(String response) {
        assertErrorResponse(response);
        assertTrue(response.toLowerCase().contains("auth") || 
                  response.toLowerCase().contains("unauthenticated") ||
                  response.toLowerCase().contains("permission"),
            "Response should indicate authentication required");
    }
}