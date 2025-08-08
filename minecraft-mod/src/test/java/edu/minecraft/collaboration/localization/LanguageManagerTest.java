package edu.minecraft.collaboration.localization;

import edu.minecraft.collaboration.localization.LanguageManager;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.test.categories.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for LanguageManager
 */
@DisplayName("LanguageManager Tests")
@UnitTest
public class LanguageManagerTest {
    
    private LanguageManager languageManager;
    private UUID testPlayerUUID;
    
    @BeforeEach
    void setUp() {
        languageManager = DependencyInjector.getInstance().getService(LanguageManager.class);
        testPlayerUUID = UUID.randomUUID();
    }
    
    @Test
    @DisplayName("Should get singleton instance")
    void testSingletonInstance() {
        // When
        DependencyInjector injector = DependencyInjector.getInstance();
        LanguageManager instance1 = injector.getService(LanguageManager.class);
        LanguageManager instance2 = injector.getService(LanguageManager.class);
        
        // Then
        assertSame(instance1, instance2);
    }
    
    @Test
    @DisplayName("Should handle player language setting")
    void testPlayerLanguageSetting() {
        // Given
        String[] supportedLanguages = {"en_US", "ja_JP", "ko_KR", "zh_CN", "es_ES", "fr_FR", "de_DE"};
        
        // When/Then
        for (String lang : supportedLanguages) {
            assertDoesNotThrow(() -> {
                languageManager.setPlayerLanguage(testPlayerUUID, lang);
                String currentLang = languageManager.getPlayerLanguage(testPlayerUUID);
                assertNotNull(currentLang);
                assertEquals(lang, currentLang);
            });
        }
    }
    
    @Test
    @DisplayName("Should handle invalid language codes")
    void testInvalidLanguageCodes() {
        // Given
        String[] invalidLanguages = {"invalid", "xyz", "toolong_language_code"};
        String originalLang = languageManager.getPlayerLanguage(testPlayerUUID);
        
        // When/Then
        for (String invalidLang : invalidLanguages) {
            assertDoesNotThrow(() -> {
                languageManager.setPlayerLanguage(testPlayerUUID, invalidLang);
                // Should keep original language or default
                String currentLang = languageManager.getPlayerLanguage(testPlayerUUID);
                assertNotNull(currentLang);
                assertEquals(originalLang, currentLang); // Should not change to invalid language
            });
        }
    }
    
    @Test
    @DisplayName("Should translate common messages using player UUID")
    void testMessageTranslationWithPlayerUUID() {
        // Given
        String[] messageKeys = {
            "welcome", "system.started", "system.stopped", "teacher.registered",
            "invitation.sent", "visit.approved", "agent.summoned", "error.not_found"
        };
        
        // When/Then
        for (String key : messageKeys) {
            String translated = languageManager.getMessage(testPlayerUUID, key);
            assertNotNull(translated);
            assertFalse(translated.trim().isEmpty());
        }
    }
    
    @Test
    @DisplayName("Should translate common messages using language code")
    void testMessageTranslationWithLanguageCode() {
        // Given
        String[] messageKeys = {
            "welcome", "system.started", "system.stopped", "teacher.registered"
        };
        
        String language = "en_US";
        
        // When/Then
        for (String key : messageKeys) {
            String translated = languageManager.getMessage(language, key);
            assertNotNull(translated);
            assertFalse(translated.trim().isEmpty());
        }
    }
    
    @Test
    @DisplayName("Should handle missing message keys")
    void testMissingMessageKeys() {
        // Given
        String[] missingKeys = {
            "non_existent_key", "missing.message", "undefined"
        };
        
        // When/Then
        for (String key : missingKeys) {
            String result = languageManager.getMessage(testPlayerUUID, key);
            assertNotNull(result);
            // Should return key itself or default message when not found
            assertEquals(key, result);
        }
    }
    
    @Test
    @DisplayName("Should translate messages with parameters")
    void testParameterizedMessages() {
        // Given
        String messageKey = "student.time_limit_set"; // "Time limit set: {0} minutes"
        String parameter = "30";
        
        // When
        String result = languageManager.getMessage(testPlayerUUID, messageKey, parameter);
        
        // Then
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
        assertTrue(result.contains(parameter));
    }
    
    @Test
    @DisplayName("Should handle Japanese localization")
    void testJapaneseLocalization() {
        // Given
        languageManager.setPlayerLanguage(testPlayerUUID, "ja_JP");
        
        // When
        String welcome = languageManager.getMessage(testPlayerUUID, "welcome");
        String error = languageManager.getMessage(testPlayerUUID, "error.not_found");
        
        // Then
        assertNotNull(welcome);
        assertNotNull(error);
        assertFalse(welcome.trim().isEmpty());
        assertFalse(error.trim().isEmpty());
    }
    
    @Test
    @DisplayName("Should handle English localization")
    void testEnglishLocalization() {
        // Given
        languageManager.setPlayerLanguage(testPlayerUUID, "en_US");
        
        // When
        String welcome = languageManager.getMessage(testPlayerUUID, "welcome");
        String error = languageManager.getMessage(testPlayerUUID, "error.not_found");
        
        // Then
        assertNotNull(welcome);
        assertNotNull(error);
        assertFalse(welcome.trim().isEmpty());
        assertFalse(error.trim().isEmpty());
    }
    
    @Test
    @DisplayName("Should get supported languages")
    void testGetSupportedLanguages() {
        // When
        var supportedLanguages = languageManager.getSupportedLanguages();
        
        // Then
        assertNotNull(supportedLanguages);
        assertTrue(supportedLanguages.size() > 0);
        
        // Should contain at least Japanese and English
        assertTrue(supportedLanguages.contains("ja_JP"));
        assertTrue(supportedLanguages.contains("en_US"));
    }
    
    @Test
    @DisplayName("Should handle default language fallback")
    void testDefaultLanguageFallback() {
        // Given - player with no set language
        UUID newPlayerUUID = UUID.randomUUID();
        
        // When
        String currentLang = languageManager.getPlayerLanguage(newPlayerUUID);
        String message = languageManager.getMessage(newPlayerUUID, "welcome");
        
        // Then
        assertNotNull(currentLang);
        assertNotNull(message);
        assertFalse(currentLang.trim().isEmpty());
        assertFalse(message.trim().isEmpty());
        
        // Should fall back to default language
        assertEquals(languageManager.getDefaultLanguage(), currentLang);
    }
    
    @Test
    @DisplayName("Should handle language persistence per player")
    void testLanguagePersistencePerPlayer() {
        // Given
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        String lang1 = "ja_JP";
        String lang2 = "en_US";
        
        // When
        languageManager.setPlayerLanguage(player1, lang1);
        languageManager.setPlayerLanguage(player2, lang2);
        
        // Then
        assertEquals(lang1, languageManager.getPlayerLanguage(player1));
        assertEquals(lang2, languageManager.getPlayerLanguage(player2));
        
        // Languages should be independent
        assertNotEquals(languageManager.getPlayerLanguage(player1), 
                       languageManager.getPlayerLanguage(player2));
    }
    
    @Test
    @DisplayName("Should check if language is supported")
    void testIsLanguageSupported() {
        // Given
        String validLanguage = "en_US";
        String invalidLanguage = "invalid_lang";
        
        // When/Then
        assertTrue(languageManager.isLanguageSupported(validLanguage));
        assertFalse(languageManager.isLanguageSupported(invalidLanguage));
    }
    
    @Test
    @DisplayName("Should get language display names")
    void testGetLanguageDisplayName() {
        // Given
        String[] languageCodes = {"en_US", "ja_JP", "zh_CN", "ko_KR", "es_ES", "fr_FR", "de_DE"};
        
        // When/Then
        for (String code : languageCodes) {
            String displayName = languageManager.getLanguageDisplayName(code);
            assertNotNull(displayName);
            assertFalse(displayName.trim().isEmpty());
            assertNotEquals(code, displayName); // Display name should be different from code
        }
    }
    
    @Test
    @DisplayName("Should get message count for languages")
    void testGetMessageCount() {
        // Given
        String language = "en_US";
        
        // When
        int messageCount = languageManager.getMessageCount(language);
        
        // Then
        assertTrue(messageCount > 0);
    }
    
    @Test
    @DisplayName("Should handle custom message addition")
    void testAddCustomMessage() {
        // Given
        String language = "en_US";
        String key = "custom.test.message";
        String message = "This is a custom test message";
        
        // When
        languageManager.addMessage(language, key, message);
        String retrievedMessage = languageManager.getMessage(language, key);
        
        // Then
        assertNotNull(retrievedMessage);
        assertEquals(message, retrievedMessage);
    }
    
    @Test
    @DisplayName("Should remove player language preference")
    void testRemovePlayerLanguage() {
        // Given
        String language = "ja_JP";
        languageManager.setPlayerLanguage(testPlayerUUID, language);
        assertEquals(language, languageManager.getPlayerLanguage(testPlayerUUID));
        
        // When
        languageManager.removePlayerLanguage(testPlayerUUID);
        
        // Then
        String currentLanguage = languageManager.getPlayerLanguage(testPlayerUUID);
        assertEquals(languageManager.getDefaultLanguage(), currentLanguage);
    }
    
    @Test
    @DisplayName("Should handle default language setting")
    void testSetDefaultLanguage() {
        // Given
        String originalDefault = languageManager.getDefaultLanguage();
        String newDefault = "ja_JP";
        
        // When
        languageManager.setDefaultLanguage(newDefault);
        
        // Then
        assertEquals(newDefault, languageManager.getDefaultLanguage());
        
        // Cleanup - restore original
        languageManager.setDefaultLanguage(originalDefault);
    }
    
    @Test
    @DisplayName("Should handle concurrent language operations")
    void testConcurrentLanguageOperations() {
        // Test thread safety
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                final int threadId = i;
                final UUID playerUUID = UUID.randomUUID();
                Thread thread = new Thread(() -> {
                    String language = threadId % 2 == 0 ? "ja_JP" : "en_US";
                    languageManager.setPlayerLanguage(playerUUID, language);
                    String message = languageManager.getMessage(playerUUID, "welcome");
                    assertNotNull(message);
                });
                thread.start();
                thread.join(100); // Short timeout
            }
        });
    }
    
    @Test
    @DisplayName("Should handle null UUID gracefully")
    void testNullUUIDHandling() {
        // When/Then - Should not throw exceptions
        assertDoesNotThrow(() -> {
            String message = languageManager.getMessage((UUID) null, "welcome");
            // Should fallback to default language
            assertNotNull(message);
        });
        
        assertDoesNotThrow(() -> {
            languageManager.setPlayerLanguage(null, "en_US");
        });
        
        assertDoesNotThrow(() -> {
            languageManager.removePlayerLanguage(null);
        });
    }
}