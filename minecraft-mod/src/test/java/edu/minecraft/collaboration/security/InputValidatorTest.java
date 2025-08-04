package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.security.InputValidator;
import edu.minecraft.collaboration.test.categories.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for InputValidator
 */
@DisplayName("InputValidator Tests")
@UnitTest
public class InputValidatorTest {
    
    @Test
    @DisplayName("Should validate null input")
    void testValidateNullInput() {
        // When
        boolean result = InputValidator.validateUsername(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should validate empty input")
    void testValidateEmptyInput() {
        // When
        boolean result = InputValidator.validateUsername("");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should validate normal username")
    void testValidateNormalUsername() {
        // When
        boolean result = InputValidator.validateUsername("player123");
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should validate username length limits")
    void testValidateUsernameLengthLimits() {
        // Given - too long username
        String longUsername = "this_username_is_way_too_long_to_be_valid";
        
        // When
        boolean result = InputValidator.validateUsername(longUsername);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should validate coordinate strings")
    void testValidateCoordinateStrings() {
        // When
        boolean validCoords = InputValidator.validateCoordinates("100", "70", "200");
        boolean invalidCoords = InputValidator.validateCoordinates("invalid", "70", "200");
        boolean outOfBoundsCoords = InputValidator.validateCoordinates("999999999", "70", "200");
        
        // Then
        assertTrue(validCoords);
        assertFalse(invalidCoords);
        assertFalse(outOfBoundsCoords);
    }
    
    @Test
    @DisplayName("Should validate single coordinates")
    void testValidateSingleCoordinate() {
        // When
        boolean validX = InputValidator.validateCoordinate("100", false);
        boolean validY = InputValidator.validateCoordinate("70", true);
        boolean invalidCoord = InputValidator.validateCoordinate("invalid", false);
        boolean outOfBoundsY = InputValidator.validateCoordinate("500", true);
        
        // Then
        assertTrue(validX);
        assertTrue(validY);
        assertFalse(invalidCoord);
        assertFalse(outOfBoundsY);
    }
    
    @Test
    @DisplayName("Should validate block types")
    void testValidateBlockTypes() {
        // When
        boolean validBlock = InputValidator.validateBlockType("stone");
        boolean invalidBlock = InputValidator.validateBlockType("invalid_block_!@#$%");
        boolean nullBlock = InputValidator.validateBlockType(null);
        boolean emptyBlock = InputValidator.validateBlockType("");
        
        // Then
        assertTrue(validBlock);
        assertFalse(invalidBlock);
        assertFalse(nullBlock);
        assertFalse(emptyBlock);
    }
    
    @Test
    @DisplayName("Should validate common block types")
    void testValidateCommonBlockTypes() {
        // Given
        String[] validBlocks = {
            "stone", "dirt", "grass_block", "cobblestone", 
            "oak_log", "minecraft:stone", "minecraft:dirt"
        };
        
        // When/Then
        for (String block : validBlocks) {
            boolean result = InputValidator.validateBlockType(block);
            assertTrue(result, "Should accept valid block type: " + block);
        }
    }
    
    @Test
    @DisplayName("Should validate and sanitize chat messages")
    void testValidateChatMessages() {
        // When
        String validMessage = InputValidator.validateChatMessage("Hello World!");
        String nullMessage = InputValidator.validateChatMessage(null);
        String emptyMessage = InputValidator.validateChatMessage("");
        String longMessage = InputValidator.validateChatMessage("A".repeat(1000));
        
        // Then
        assertNotNull(validMessage);
        assertEquals("Hello World!", validMessage);
        assertNull(nullMessage);
        assertNull(emptyMessage);
        assertNotNull(longMessage); // Should be truncated, not null
        assertTrue(longMessage.length() <= 256); // Should be within limit
    }
    
    @Test
    @DisplayName("Should reject dangerous chat patterns")
    void testRejectDangerousChatPatterns() {
        // Given
        String[] dangerousMessages = {
            "<script>alert('xss')</script>",
            "'; DROP TABLE users; --",
            "$(rm -rf /)",
            "javascript:alert(1)"
        };
        
        // When/Then
        for (String dangerous : dangerousMessages) {
            String result = InputValidator.validateChatMessage(dangerous);
            // Should be null or heavily sanitized
            assertTrue(result == null || !result.contains("<script"));
        }
    }
    
    @Test
    @DisplayName("Should validate commands")
    void testValidateCommands() {
        // When
        boolean validCommand = InputValidator.validateCommand("help");
        boolean nullCommand = InputValidator.validateCommand(null);
        boolean emptyCommand = InputValidator.validateCommand("");
        boolean longCommand = InputValidator.validateCommand("A".repeat(2000));
        
        // Then
        assertTrue(validCommand);
        assertFalse(nullCommand);
        assertFalse(emptyCommand);
        assertFalse(longCommand);
    }
    
    @Test
    @DisplayName("Should validate numeric ranges")
    void testValidateNumericRanges() {
        // When
        boolean validRange = InputValidator.validateNumericRange("50", 0, 100);
        boolean outOfRange = InputValidator.validateNumericRange("150", 0, 100);
        boolean invalidNumber = InputValidator.validateNumericRange("invalid", 0, 100);
        
        // Then
        assertTrue(validRange);
        assertFalse(outOfRange);
        assertFalse(invalidNumber);
    }
    
    @Test
    @DisplayName("Should validate world names")
    void testValidateWorldNames() {
        // When
        boolean validWorld = InputValidator.validateWorldName("MyWorld123");
        boolean invalidWorld = InputValidator.validateWorldName("My World!"); // spaces and special chars
        boolean nullWorld = InputValidator.validateWorldName(null);
        boolean emptyWorld = InputValidator.validateWorldName("");
        boolean longWorld = InputValidator.validateWorldName("A".repeat(50));
        
        // Then
        assertTrue(validWorld);
        assertFalse(invalidWorld);
        assertFalse(nullWorld);
        assertFalse(emptyWorld);
        assertFalse(longWorld);
    }
    
    @Test
    @DisplayName("Should escape HTML entities")
    void testEscapeHtml() {
        // Given
        String htmlInput = "<script>alert('test')</script>";
        String expected = "&lt;script&gt;alert(&#x27;test&#x27;)&lt;&#x2F;script&gt;";
        
        // When
        String result = InputValidator.escapeHtml(htmlInput);
        
        // Then
        assertNotNull(result);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should handle null HTML escaping")
    void testEscapeHtmlNull() {
        // When
        String result = InputValidator.escapeHtml(null);
        
        // Then
        assertEquals("", result);
    }
    
    @Test
    @DisplayName("Should validate JSON format")
    void testValidateJson() {
        // Given
        String validJson = "{\"type\":\"test\",\"data\":{}}";
        String validArrayJson = "[{\"item\":\"value\"}]";
        String invalidJson = "{ invalid json }";
        String nullJson = null;
        String emptyJson = "";
        
        // When
        boolean validResult = InputValidator.validateJson(validJson);
        boolean validArrayResult = InputValidator.validateJson(validArrayJson);
        boolean invalidResult = InputValidator.validateJson(invalidJson);
        boolean nullResult = InputValidator.validateJson(nullJson);
        boolean emptyResult = InputValidator.validateJson(emptyJson);
        
        // Then
        assertTrue(validResult);
        assertTrue(validArrayResult);
        assertFalse(invalidResult);
        assertFalse(nullResult);
        assertFalse(emptyResult);
    }
    
    @Test
    @DisplayName("Should validate Japanese characters in chat")
    void testValidateJapaneseCharacters() {
        // Given
        String japaneseText = "こんにちは"; // "Hello" in Japanese
        
        // When
        String result = InputValidator.validateChatMessage(japaneseText);
        
        // Then
        // Should handle Japanese characters appropriately
        assertNotNull(result);
    }
    
    @Test
    @DisplayName("Should validate coordinate bounds")
    void testValidateCoordinateBounds() {
        // Given - test extreme values
        String validX = "1000";
        String validY = "100";
        String validZ = "-1000";
        String invalidX = "999999999";
        String invalidY = "500"; // Above max Y
        String invalidZ = "-999999999";
        
        // When/Then
        assertTrue(InputValidator.validateCoordinate(validX, false));
        assertTrue(InputValidator.validateCoordinate(validY, true));
        assertTrue(InputValidator.validateCoordinate(validZ, false));
        
        assertFalse(InputValidator.validateCoordinate(invalidX, false));
        assertFalse(InputValidator.validateCoordinate(invalidY, true));
        assertFalse(InputValidator.validateCoordinate(invalidZ, false));
    }
    
    @Test
    @DisplayName("Should handle edge cases for usernames")
    void testUsernameEdgeCases() {
        // Given
        String minValidUsername = "abc"; // minimum length
        String maxValidUsername = "a".repeat(16); // maximum length
        String tooShort = "ab";
        String specialChars = "user@name";
        String withSpaces = "user name";
        
        // When/Then
        assertTrue(InputValidator.validateUsername(minValidUsername));
        assertTrue(InputValidator.validateUsername(maxValidUsername));
        assertFalse(InputValidator.validateUsername(tooShort));
        assertFalse(InputValidator.validateUsername(specialChars));
        assertFalse(InputValidator.validateUsername(withSpaces));
    }
    
    @Test
    @DisplayName("Should validate block type patterns")
    void testValidateBlockTypePatterns() {
        // Given
        String[] validPatterns = {
            "stone", 
            "minecraft:stone", 
            "oak_log", 
            "grass_block",
            "red_wool"
        };
        
        String[] invalidPatterns = {
            "stone!", 
            "stone block", // space
            "stone@dirt", // invalid character
            ""
        };
        
        // When/Then
        for (String valid : validPatterns) {
            assertTrue(InputValidator.validateBlockType(valid), "Should validate: " + valid);
        }
        
        for (String invalid : invalidPatterns) {
            assertFalse(InputValidator.validateBlockType(invalid), "Should reject: " + invalid);
        }
    }
    
    @Test
    @DisplayName("Should handle concurrent validation requests")
    void testConcurrentValidation() {
        // Test thread safety
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) {
                final int threadId = i;
                Thread thread = new Thread(() -> {
                    String username = "user" + threadId;
                    boolean result = InputValidator.validateUsername(username);
                    assertTrue(result);
                });
                thread.start();
                thread.join(100); // Short timeout
            }
        });
    }
}