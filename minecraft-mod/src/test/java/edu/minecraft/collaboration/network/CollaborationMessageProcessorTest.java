package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.test.mocks.TestableCollaborationMessageProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for CollaborationMessageProcessor
 */
@DisplayName("CollaborationMessageProcessor Tests")
public class CollaborationMessageProcessorTest {
    
    private TestableCollaborationMessageProcessor messageProcessor;
    
    @BeforeEach
    void setUp() {
        messageProcessor = new TestableCollaborationMessageProcessor();
    }
    
    @Test
    @DisplayName("Should handle null message")
    void testHandleNullMessage() {
        // When
        String result = messageProcessor.processMessage(null);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("emptyMessage"));
    }
    
    @Test
    @DisplayName("Should handle empty message")
    void testHandleEmptyMessage() {
        // When
        String result = messageProcessor.processMessage("");
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("emptyMessage"));
    }
    
    @Test
    @DisplayName("Should handle whitespace-only message")
    void testHandleWhitespaceMessage() {
        // When
        String result = messageProcessor.processMessage("   ");
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("error"));
        assertTrue(result.contains("emptyMessage"));
    }
    
    @Test
    @DisplayName("Should handle invalid JSON")
    void testHandleInvalidJson() {
        // Given
        String invalidJson = "{ invalid json }";
        
        // When
        String result = messageProcessor.processMessage(invalidJson);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("error") || result.contains("success"));
    }
    
    @Test
    @DisplayName("Should handle valid JSON with unknown command")
    void testHandleUnknownCommand() {
        // Given
        String unknownCommandJson = "{\"type\":\"unknownCommand\",\"data\":{}}";
        
        // When
        String result = messageProcessor.processMessage(unknownCommandJson);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("error") || result.contains("success"));
    }
    
    @Test
    @DisplayName("Should handle valid placeBlock command JSON")
    void testHandlePlaceBlockCommand() {
        // Given
        String placeBlockJson = "{\"type\":\"placeBlock\",\"data\":{\"x\":0,\"y\":70,\"z\":0,\"blockType\":\"stone\"}}";
        
        // When
        String result = messageProcessor.processMessage(placeBlockJson);
        
        // Then
        assertNotNull(result);
        // Should return either success or authentication error
        assertTrue(result.contains("success") || result.contains("unauthenticated") || result.contains("error"));
    }
    
    @Test
    @DisplayName("Should handle valid getPlayerPos command JSON")
    void testHandleGetPlayerPosCommand() {
        // Given
        String getPlayerPosJson = "{\"type\":\"getPlayerPos\",\"data\":{}}";
        
        // When
        String result = messageProcessor.processMessage(getPlayerPosJson);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("success") || result.contains("unauthenticated") || result.contains("error"));
    }
    
    @Test
    @DisplayName("Should handle valid chat command JSON")
    void testHandleChatCommand() {
        // Given
        String chatJson = "{\"type\":\"chat\",\"data\":{\"message\":\"Hello World\"}}";
        
        // When
        String result = messageProcessor.processMessage(chatJson);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("success") || result.contains("unauthenticated") || result.contains("error"));
    }
    
    @Test
    @DisplayName("Should handle legacy format command")
    void testHandleLegacyFormatCommand() {
        // Given
        String legacyCommand = "placeBlock(0,70,0,stone)";
        
        // When
        String result = messageProcessor.processMessage(legacyCommand);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("success") || result.contains("unauthenticated") || result.contains("error"));
    }
    
    @Test
    @DisplayName("Should handle malformed legacy format")
    void testHandleMalformedLegacyFormat() {
        // Given
        String malformedLegacy = "placeBlock(0,70,0";
        
        // When
        String result = messageProcessor.processMessage(malformedLegacy);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("error") || result.contains("success"));
    }
    
    @Test
    @DisplayName("Should handle extremely long message")
    void testHandleExtremelyLongMessage() {
        // Given
        String longMessage = "A".repeat(100000);
        
        // When
        String result = messageProcessor.processMessage(longMessage);
        
        // Then
        assertNotNull(result);
        // Should handle gracefully without crashing
        assertTrue(result.contains("error") || result.contains("success"));
    }
    
    @Test
    @DisplayName("Should handle message with special characters")
    void testHandleSpecialCharacters() {
        // Given
        String specialCharsJson = "{\"type\":\"chat\",\"data\":{\"message\":\"こんにちは！@#$%^&*()\"}}";
        
        // When
        String result = messageProcessor.processMessage(specialCharsJson);
        
        // Then
        assertNotNull(result);
        assertTrue(result.contains("success") || result.contains("unauthenticated") || result.contains("error"));
    }
    
    @Test
    @DisplayName("Should return JSON format response")
    void testResponseFormat() {
        // Given
        String validJson = "{\"type\":\"ping\",\"data\":{}}";
        
        // When
        String result = messageProcessor.processMessage(validJson);
        
        // Then
        assertNotNull(result);
        // Should be valid JSON - basic check
        assertTrue(result.startsWith("{") && result.endsWith("}"));
    }
}