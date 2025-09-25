package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.test.categories.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WebSocketMessageValidator
 */
@DisplayName("WebSocketMessageValidator Tests")
@UnitTest
public class WebSocketMessageValidatorTest {
    
    private WebSocketMessageValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new WebSocketMessageValidator();
    }
    
    @Test
    @DisplayName("Should validate null messages")
    void testNullMessage() {
        // When
        boolean result = validator.isValidMessage(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should validate empty messages")
    void testEmptyMessage() {
        // When
        boolean result = validator.isValidMessage("");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should validate blank messages")
    void testBlankMessage() {
        // When
        boolean result = validator.isValidMessage("   ");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should validate too long messages")
    void testTooLongMessage() {
        // Given - message longer than max allowed
        String longMessage = "x".repeat(10001);
        
        // When
        boolean result = validator.isValidMessage(longMessage);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should validate valid JSON messages")
    void testValidJsonMessage() {
        // Given
        String validJson = "{\"action\":\"ping\",\"data\":{}}";
        
        // When
        boolean result = validator.isValidMessage(validJson);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should validate invalid JSON messages")
    void testInvalidJsonMessage() {
        // Given
        String invalidJson = "{ invalid json }";
        
        // When
        boolean result = validator.isValidMessage(invalidJson);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should validate dangerous content in messages")
    void testDangerousContent() {
        // Given
        String[] dangerousMessages = {
            "{\"action\":\"<script>alert('xss')</script>\"}",
            "{\"data\":\"'; DROP TABLE users; --\"}",
            "{\"command\":\"$(rm -rf /)\"}",
            "{\"message\":\"javascript:alert(1)\"}"
        };
        
        // When/Then
        for (String dangerous : dangerousMessages) {
            boolean result = validator.isValidMessage(dangerous);
            assertFalse(result, "Should reject dangerous message: " + dangerous);
        }
    }
    
    @Test
    @DisplayName("Should validate message size limits")
    void testMessageSizeLimits() {
        // Given
        String maxSizeMessage = "x".repeat(1024); // Exactly at limit
        String oversizeMessage = "x".repeat(1025); // Over limit
        
        // When/Then
        assertTrue(validator.isValidMessage(maxSizeMessage));
        assertFalse(validator.isValidMessage(oversizeMessage));
    }
    
    @Test
    @DisplayName("Should validate common command formats")
    void testCommonCommandFormats() {
        // Given
        String[] validCommands = {
            "{\"action\":\"getPosition\"}",
            "{\"action\":\"placeBlock\",\"x\":100,\"y\":64,\"z\":200,\"blockType\":\"stone\"}",
            "{\"action\":\"sendChat\",\"message\":\"Hello World\"}",
            "{\"action\":\"teleport\",\"x\":0,\"y\":100,\"z\":0}",
            "{\"type\":\"authentication\",\"token\":\"abc123\"}"
        };
        
        // When/Then
        for (String command : validCommands) {
            boolean result = validator.isValidMessage(command);
            assertTrue(result, "Should accept valid command: " + command);
        }
    }
    
    @Test
    @DisplayName("Should get validation error messages")
    void testValidationErrorMessages() {
        // When
        String nullError = validator.getLastValidationError();
        
        // First validation
        validator.isValidMessage(null);
        String errorAfterNull = validator.getLastValidationError();
        
        // Second validation
        validator.isValidMessage("{ invalid json }");
        String errorAfterInvalid = validator.getLastValidationError();
        
        // Then
        assertNotNull(errorAfterNull);
        assertNotNull(errorAfterInvalid);
        assertNotEquals(errorAfterNull, errorAfterInvalid);
    }
    
    @Test
    @DisplayName("Should handle unicode characters")
    void testUnicodeCharacters() {
        // Given
        String unicodeMessage = "{\"message\":\"こんにちは世界\",\"action\":\"sendChat\"}";
        
        // When
        boolean result = validator.isValidMessage(unicodeMessage);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should validate nested JSON structures")
    void testNestedJsonStructures() {
        // Given
        String nestedJson = "{\"action\":\"complexCommand\",\"data\":{\"player\":{\"name\":\"test\",\"position\":{\"x\":0,\"y\":64,\"z\":0}},\"items\":[\"stone\",\"dirt\"]}}";
        
        // When
        boolean result = validator.isValidMessage(nestedJson);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should validate array JSON messages")
    void testArrayJsonMessages() {
        // Given
        String arrayJson = "[{\"action\":\"first\"},{\"action\":\"second\"}]";
        
        // When
        boolean result = validator.isValidMessage(arrayJson);
        
        // Then
        assertTrue(result);
    }
}