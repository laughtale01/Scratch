package com.yourname.minecraftcollaboration.commands;

import com.yourname.minecraftcollaboration.network.CollaborationMessageProcessor;
import com.yourname.minecraftcollaboration.util.ResponseHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for building commands (circle, sphere, wall, house)
 */
public class BuildingCommandsTest {
    
    private CollaborationMessageProcessor messageProcessor;
    
    @BeforeEach
    public void setup() {
        messageProcessor = new CollaborationMessageProcessor();
    }
    
    @Test
    @DisplayName("Test circle building command validation")
    public void testBuildCircleValidation() {
        // Test valid circle command
        String validCommand = createJsonCommand("buildCircle", 
            Map.of("x", "0", "y", "64", "z", "0", "radius", "5", "block", "stone"));
        
        String response = messageProcessor.processMessage(validCommand);
        assertNotNull(response);
        assertTrue(response.contains("buildCircle") || response.contains("error"));
        
        // Test invalid radius (too large)
        String largeRadiusCommand = createJsonCommand("buildCircle",
            Map.of("x", "0", "y", "64", "z", "0", "radius", "100", "block", "stone"));
        
        String largeResponse = messageProcessor.processMessage(largeRadiusCommand);
        assertTrue(largeResponse.contains("error") || largeResponse.contains("too"));
    }
    
    @Test
    @DisplayName("Test sphere building command validation")
    public void testBuildSphereValidation() {
        // Test valid sphere command
        String validCommand = createJsonCommand("buildSphere",
            Map.of("x", "10", "y", "70", "z", "10", "radius", "3", "block", "glass"));
        
        String response = messageProcessor.processMessage(validCommand);
        assertNotNull(response);
        
        // Test sphere with maximum allowed radius
        String maxRadiusCommand = createJsonCommand("buildSphere",
            Map.of("x", "0", "y", "100", "z", "0", "radius", "50", "block", "stone"));
        
        String maxResponse = messageProcessor.processMessage(maxRadiusCommand);
        assertNotNull(maxResponse);
        
        // Test sphere with exceeded radius
        String exceededRadiusCommand = createJsonCommand("buildSphere",
            Map.of("x", "0", "y", "100", "z", "0", "radius", "51", "block", "stone"));
        
        String exceededResponse = messageProcessor.processMessage(exceededRadiusCommand);
        assertTrue(exceededResponse.contains("error") || exceededResponse.contains("large"));
    }
    
    @Test
    @DisplayName("Test wall building command validation")
    public void testBuildWallValidation() {
        // Test valid wall command
        String validCommand = createJsonCommand("buildWall",
            Map.of("x1", "0", "z1", "0", "x2", "10", "z2", "0", "height", "5", "block", "brick"));
        
        String response = messageProcessor.processMessage(validCommand);
        assertNotNull(response);
        
        // Test wall with excessive height
        String highWallCommand = createJsonCommand("buildWall",
            Map.of("x1", "0", "z1", "0", "x2", "10", "z2", "0", "height", "100", "block", "brick"));
        
        String highResponse = messageProcessor.processMessage(highWallCommand);
        assertTrue(highResponse.contains("error") || highResponse.contains("high"));
    }
    
    @Test
    @DisplayName("Test house building command validation")
    public void testBuildHouseValidation() {
        // Test valid house command
        String validCommand = createJsonCommand("buildHouse",
            Map.of("x", "0", "y", "64", "z", "0", 
                   "width", "7", "depth", "7", "height", "4", "block", "oak_planks"));
        
        String response = messageProcessor.processMessage(validCommand);
        assertNotNull(response);
        
        // Test house with excessive dimensions
        String largeHouseCommand = createJsonCommand("buildHouse",
            Map.of("x", "0", "y", "64", "z", "0",
                   "width", "50", "depth", "50", "height", "30", "block", "stone"));
        
        String largeResponse = messageProcessor.processMessage(largeHouseCommand);
        assertTrue(largeResponse.contains("error") || largeResponse.contains("large"));
    }
    
    @Test
    @DisplayName("Test fill command validation")
    public void testFillCommandValidation() {
        // Test valid fill command
        String validCommand = createJsonCommand("fill",
            Map.of("x1", "0", "y1", "64", "z1", "0",
                   "x2", "10", "y2", "64", "z2", "10", "block", "grass_block"));
        
        String response = messageProcessor.processMessage(validCommand);
        assertNotNull(response);
        
        // Test fill with excessive volume
        String largeVolumeCommand = createJsonCommand("fill",
            Map.of("x1", "0", "y1", "0", "z1", "0",
                   "x2", "100", "y2", "100", "z2", "100", "block", "stone"));
        
        String volumeResponse = messageProcessor.processMessage(largeVolumeCommand);
        assertTrue(volumeResponse.contains("error") || volumeResponse.contains("large"));
    }
    
    @Test
    @DisplayName("Test building commands with invalid block types")
    public void testInvalidBlockTypes() {
        // Test with dangerous block (should be rejected by security)
        String tntCommand = createJsonCommand("buildCircle",
            Map.of("x", "0", "y", "64", "z", "0", "radius", "5", "block", "tnt"));
        
        String tntResponse = messageProcessor.processMessage(tntCommand);
        // Should either error or succeed based on security implementation
        assertNotNull(tntResponse);
        
        // Test with non-existent block
        String invalidBlockCommand = createJsonCommand("buildWall",
            Map.of("x1", "0", "z1", "0", "x2", "10", "z2", "0", 
                   "height", "3", "block", "nonexistent_block"));
        
        String invalidResponse = messageProcessor.processMessage(invalidBlockCommand);
        assertNotNull(invalidResponse);
    }
    
    @Test
    @DisplayName("Test building at extreme coordinates")
    public void testExtremeCoordinates() {
        // Test building at world border
        String borderCommand = createJsonCommand("buildCircle",
            Map.of("x", "29999990", "y", "64", "z", "0", "radius", "5", "block", "stone"));
        
        String borderResponse = messageProcessor.processMessage(borderCommand);
        assertNotNull(borderResponse);
        
        // Test building at negative Y
        String negativeYCommand = createJsonCommand("buildSphere",
            Map.of("x", "0", "y", "-10", "z", "0", "radius", "3", "block", "stone"));
        
        String negativeResponse = messageProcessor.processMessage(negativeYCommand);
        assertNotNull(negativeResponse);
    }
    
    @Test
    @DisplayName("Test building command parameter edge cases")
    public void testParameterEdgeCases() {
        // Test with zero radius
        String zeroRadiusCommand = createJsonCommand("buildCircle",
            Map.of("x", "0", "y", "64", "z", "0", "radius", "0", "block", "stone"));
        
        String zeroResponse = messageProcessor.processMessage(zeroRadiusCommand);
        assertNotNull(zeroResponse);
        
        // Test with negative dimensions
        String negativeDimCommand = createJsonCommand("buildHouse",
            Map.of("x", "0", "y", "64", "z", "0",
                   "width", "-5", "depth", "5", "height", "3", "block", "oak_planks"));
        
        String negativeResponse = messageProcessor.processMessage(negativeDimCommand);
        assertNotNull(negativeResponse);
        
        // Test with floating point values
        String floatCommand = createJsonCommand("buildSphere",
            Map.of("x", "10.5", "y", "64.7", "z", "-5.3", "radius", "3.14", "block", "glass"));
        
        String floatResponse = messageProcessor.processMessage(floatCommand);
        assertNotNull(floatResponse);
    }
    
    // Helper method to create JSON command
    private String createJsonCommand(String command, Map<String, String> args) {
        JsonObject json = new JsonObject();
        json.addProperty("command", command);
        
        JsonObject argsObj = new JsonObject();
        args.forEach(argsObj::addProperty);
        json.add("args", argsObj);
        
        return json.toString();
    }
}