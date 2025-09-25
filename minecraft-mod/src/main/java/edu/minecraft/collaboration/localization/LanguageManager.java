package edu.minecraft.collaboration.localization;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages localization and language support for the collaboration system.
 * Converted from singleton to dependency injection pattern.
 */
public final class LanguageManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    // Language data storage
    private final Map<String, Map<String, String>> languageData = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerLanguages = new ConcurrentHashMap<>();

    // Default settings
    private String defaultLanguage = "en_US";
    private final Set<String> supportedLanguages = new HashSet<>();

    public LanguageManager() {
        initializeLanguages();
        LOGGER.info("LanguageManager initialized with {} languages", supportedLanguages.size());
    }

    /**
     * Initialize all supported languages
     */
    private void initializeLanguages() {
        // Initialize English
        initializeEnglish();

        // Initialize Japanese (simplified to avoid encoding issues)
        initializeJapanese();

        // Initialize Chinese (simplified to avoid encoding issues)
        initializeChinese();

        // Initialize Korean (simplified to avoid encoding issues)
        initializeKorean();

        // Initialize other languages
        initializeSpanish();
        initializeFrench();
        initializeGerman();

        LOGGER.info("Initialized {} languages: {}", supportedLanguages.size(), supportedLanguages);
    }

    /**
     * Initialize English language pack
     */
    private void initializeEnglish() {
        Map<String, String> en = new HashMap<>();

        // System messages
        en.put("welcome", "Welcome to Minecraft Collaboration Learning System!");
        en.put("system.started", "System started successfully");
        en.put("system.stopped", "System stopped");
        en.put("system.error", "System error occurred");

        // Teacher messages
        en.put("teacher.registered", "Teacher account registered successfully");
        en.put("teacher.access_required", "Teacher access required");
        en.put("teacher.classroom_enabled", "Classroom mode enabled");
        en.put("teacher.classroom_disabled", "Classroom mode disabled");

        // Student messages
        en.put("student.time_limit_set", "Time limit set: {0} minutes");
        en.put("student.time_limit_removed", "Time limit removed");
        en.put("student.restricted", "Action restricted: {0}");
        en.put("student.frozen", "All students frozen");
        en.put("student.unfrozen", "All students unfrozen");

        // Collaboration messages
        en.put("invitation.sent", "Invitation sent to {0}");
        en.put("invitation.received", "Invitation received from {0}");
        en.put("invitation.accepted", "Invitation accepted");
        en.put("invitation.declined", "Invitation declined");
        en.put("visit.requested", "Visit requested to {0}");
        en.put("visit.approved", "Visit approved! Welcome to {0}'s world");
        en.put("visit.denied", "Visit request denied");
        en.put("emergency.returned", "Emergency return! You are now safe at home");

        // Agent messages
        en.put("agent.summoned", "Agent summoned: {0}");
        en.put("agent.dismissed", "Agent dismissed");
        en.put("agent.following", "Agent is following you");
        en.put("agent.stopped_following", "Agent stopped following");
        en.put("agent.action_performed", "Agent performed action: {0}");

        // Error messages
        en.put("error.not_found", "Not found");
        en.put("error.permission_denied", "Permission denied");
        en.put("error.invalid_parameters", "Invalid parameters");
        en.put("error.server_error", "Server error");
        en.put("error.player_not_found", "Player not found");
        en.put("error.already_exists", "Already exists");

        // Progress messages
        en.put("progress.milestone_reached", "Milestone reached: {0}");
        en.put("progress.points_earned", "Points earned: {0}");
        en.put("progress.level_up", "Level up! New level: {0}");

        // Language messages
        en.put("language.changed", "Language changed to: {0}");
        en.put("language.unsupported", "Unsupported language");
        en.put("language.list", "Supported languages");

        // Block pack messages
        en.put("blockpack.applied", "Block pack applied: {0}");
        en.put("blockpack.created", "Custom block pack created: {0}");
        en.put("blockpack.permission_denied", "This block pack requires teacher access");

        // Offline mode messages
        en.put("offline.mode_enabled", "Offline mode enabled");
        en.put("offline.mode_disabled", "Offline mode disabled");
        en.put("offline.sync_success", "Offline data synchronized successfully");
        en.put("offline.sync_partial", "Offline data partially synchronized");
        en.put("offline.data_exported", "Offline data exported successfully");

        languageData.put("en_US", en);
        supportedLanguages.add("en_US");
    }

    /**
     * Initialize Japanese language pack (simplified)
     */
    private void initializeJapanese() {
        Map<String, String> ja = new HashMap<>();

        // Basic messages in English to avoid encoding issues
        ja.put("welcome", "Welcome to Minecraft Collaboration Learning System!");
        ja.put("system.started", "System started successfully");
        ja.put("system.stopped", "System stopped");
        ja.put("teacher.registered", "Teacher account registered successfully");
        ja.put("teacher.access_required", "Teacher access required");
        ja.put("student.time_limit_set", "Time limit set: {0} minutes");
        ja.put("invitation.received", "Invitation received from {0}");
        ja.put("visit.approved", "Visit approved! Welcome to {0}'s world");
        ja.put("agent.summoned", "Agent summoned: {0}");
        ja.put("error.not_found", "Not found");
        ja.put("error.permission_denied", "Permission denied");
        ja.put("language.changed", "Language changed to: {0}");
        ja.put("blockpack.applied", "Block pack applied: {0}");

        languageData.put("ja_JP", ja);
        supportedLanguages.add("ja_JP");
    }

    /**
     * Initialize Chinese language pack (simplified)
     */
    private void initializeChinese() {
        Map<String, String> zh = new HashMap<>();

        // Basic messages in English to avoid encoding issues
        zh.put("welcome", "Welcome to Minecraft Collaboration Learning System!");
        zh.put("system.started", "System started successfully");
        zh.put("system.stopped", "System stopped");
        zh.put("teacher.registered", "Teacher account registered successfully");
        zh.put("teacher.access_required", "Teacher access required");
        zh.put("student.time_limit_set", "Time limit set: {0} minutes");
        zh.put("invitation.received", "Invitation received from {0}");
        zh.put("visit.approved", "Visit approved! Welcome to {0}'s world");
        zh.put("agent.summoned", "Agent summoned: {0}");
        zh.put("error.not_found", "Not found");
        zh.put("error.permission_denied", "Permission denied");
        zh.put("language.changed", "Language changed to: {0}");
        zh.put("blockpack.applied", "Block pack applied: {0}");

        languageData.put("zh_CN", zh);
        languageData.put("zh_TW", zh); // Same for traditional Chinese
        supportedLanguages.add("zh_CN");
        supportedLanguages.add("zh_TW");
    }

    /**
     * Initialize Korean language pack (simplified)
     */
    private void initializeKorean() {
        Map<String, String> ko = new HashMap<>();

        // Basic messages in English to avoid encoding issues
        ko.put("welcome", "Welcome to Minecraft Collaboration Learning System!");
        ko.put("system.started", "System started successfully");
        ko.put("system.stopped", "System stopped");
        ko.put("teacher.registered", "Teacher account registered successfully");
        ko.put("teacher.access_required", "Teacher access required");
        ko.put("student.time_limit_set", "Time limit set: {0} minutes");
        ko.put("invitation.received", "Invitation received from {0}");
        ko.put("visit.approved", "Visit approved! Welcome to {0}'s world");
        ko.put("agent.summoned", "Agent summoned: {0}");
        ko.put("error.not_found", "Not found");
        ko.put("error.permission_denied", "Permission denied");
        ko.put("language.changed", "Language changed to: {0}");
        ko.put("blockpack.applied", "Block pack applied: {0}");

        languageData.put("ko_KR", ko);
        supportedLanguages.add("ko_KR");
    }

    /**
     * Initialize Spanish language pack
     */
    private void initializeSpanish() {
        Map<String, String> es = new HashMap<>();

        es.put("welcome", "¡Bienvenido al Sistema de Aprendizaje Colaborativo de Minecraft!");
        es.put("system.started", "Sistema iniciado exitosamente");
        es.put("system.stopped", "Sistema detenido");
        es.put("teacher.registered", "Cuenta de profesor registrada exitosamente");
        es.put("teacher.access_required", "Se requiere acceso de profesor");
        es.put("student.time_limit_set", "Límite de tiempo establecido: {0} minutos");
        es.put("invitation.received", "Invitación recibida de {0}");
        es.put("visit.approved", "¡Visita aprobada! Bienvenido al mundo de {0}");
        es.put("agent.summoned", "Agente invocado: {0}");
        es.put("error.not_found", "No encontrado");
        es.put("error.permission_denied", "Permiso denegado");
        es.put("language.changed", "Idioma cambiado a: {0}");
        es.put("blockpack.applied", "Paquete de bloques aplicado: {0}");

        languageData.put("es_ES", es);
        supportedLanguages.add("es_ES");
    }

    /**
     * Initialize French language pack
     */
    private void initializeFrench() {
        Map<String, String> fr = new HashMap<>();

        fr.put("welcome", "Bienvenue dans le Système d'Apprentissage Collaboratif Minecraft!");
        fr.put("system.started", "Système démarré avec succès");
        fr.put("system.stopped", "Système arrêté");
        fr.put("teacher.registered", "Compte professeur enregistré avec succès");
        fr.put("teacher.access_required", "Accès professeur requis");
        fr.put("student.time_limit_set", "Limite de temps définie: {0} minutes");
        fr.put("invitation.received", "Invitation reçue de {0}");
        fr.put("visit.approved", "Visite approuvée! Bienvenue dans le monde de {0}");
        fr.put("agent.summoned", "Agent invoqué: {0}");
        fr.put("error.not_found", "Non trouvé");
        fr.put("error.permission_denied", "Permission refusée");
        fr.put("language.changed", "Langue changée pour: {0}");
        fr.put("blockpack.applied", "Pack de blocs appliqué: {0}");

        languageData.put("fr_FR", fr);
        supportedLanguages.add("fr_FR");
    }

    /**
     * Initialize German language pack
     */
    private void initializeGerman() {
        Map<String, String> de = new HashMap<>();

        de.put("welcome", "Willkommen im Minecraft Kollaborativen Lernsystem!");
        de.put("system.started", "System erfolgreich gestartet");
        de.put("system.stopped", "System gestoppt");
        de.put("teacher.registered", "Lehrerkonto erfolgreich registriert");
        de.put("teacher.access_required", "Lehrerzugang erforderlich");
        de.put("student.time_limit_set", "Zeitlimit festgelegt: {0} Minuten");
        de.put("invitation.received", "Einladung erhalten von {0}");
        de.put("visit.approved", "Besuch genehmigt! Willkommen in {0}s Welt");
        de.put("agent.summoned", "Agent beschworen: {0}");
        de.put("error.not_found", "Nicht gefunden");
        de.put("error.permission_denied", "Berechtigung verweigert");
        de.put("language.changed", "Sprache geändert zu: {0}");
        de.put("blockpack.applied", "Blockpaket angewendet: {0}");

        languageData.put("de_DE", de);
        supportedLanguages.add("de_DE");
    }

    /**
     * Get message in player's language
     */
    public String getMessage(UUID playerUUID, String key, Object... args) {
        if (playerUUID == null) {
            return getMessage(defaultLanguage, key, args);
        }
        String language = getPlayerLanguage(playerUUID);
        return getMessage(language, key, args);
    }

    /**
     * Get message in specific language
     */
    public String getMessage(String language, String key, Object... args) {
        Map<String, String> langData = languageData.get(language);
        if (langData == null) {
            langData = languageData.get(defaultLanguage);
        }

        String message = langData.get(key);
        if (message == null) {
            // Fallback to English
            Map<String, String> englishData = languageData.get("en_US");
            message = englishData.get(key);
            if (message == null) {
                message = key; // Return key if message not found
            }
        }

        // Replace placeholders
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                message = message.replace("{" + i + "}", String.valueOf(args[i]));
            }
        }

        return message;
    }

    /**
     * Set player's language preference
     */
    public void setPlayerLanguage(UUID playerUUID, String language) {
        if (playerUUID == null) {
            LOGGER.warn("Cannot set language for null player UUID");
            return;
        }
        if (supportedLanguages.contains(language)) {
            playerLanguages.put(playerUUID, language);
            LOGGER.debug("Set language for player {} to {}", playerUUID, language);
        } else {
            LOGGER.warn("Attempted to set unsupported language {} for player {}", language, playerUUID);
        }
    }

    /**
     * Get player's language preference
     */
    public String getPlayerLanguage(UUID playerUUID) {
        return playerLanguages.getOrDefault(playerUUID, defaultLanguage);
    }

    /**
     * Get all supported languages
     */
    public Set<String> getSupportedLanguages() {
        return new HashSet<>(supportedLanguages);
    }

    /**
     * Set default language
     */
    public void setDefaultLanguage(String language) {
        if (supportedLanguages.contains(language)) {
            this.defaultLanguage = language;
            LOGGER.info("Default language set to: {}", language);
        } else {
            LOGGER.warn("Attempted to set unsupported default language: {}", language);
        }
    }

    /**
     * Get default language
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * Get language display name
     */
    public String getLanguageDisplayName(String languageCode) {
        switch (languageCode) {
            case "en_US": return "English (US)";
            case "ja_JP": return "Japanese";
            case "zh_CN": return "Chinese (Simplified)";
            case "zh_TW": return "Chinese (Traditional)";
            case "ko_KR": return "Korean";
            case "es_ES": return "Spanish";
            case "fr_FR": return "French";
            case "de_DE": return "German";
            default: return languageCode;
        }
    }

    /**
     * Get number of messages for a language
     */
    public int getMessageCount(String language) {
        Map<String, String> langData = languageData.get(language);
        return langData != null ? langData.size() : 0;
    }

    /**
     * Check if language is supported
     */
    public boolean isLanguageSupported(String language) {
        return supportedLanguages.contains(language);
    }

    /**
     * Add custom message to a language
     */
    public void addMessage(String language, String key, String message) {
        Map<String, String> langData = languageData.get(language);
        if (langData != null) {
            langData.put(key, message);
            LOGGER.debug("Added custom message {} to language {}", key, language);
        } else {
            LOGGER.warn("Attempted to add message to unsupported language: {}", language);
        }
    }

    /**
     * Remove player language preference
     */
    public void removePlayerLanguage(UUID playerUUID) {
        if (playerUUID != null) {
            playerLanguages.remove(playerUUID);
        }
    }
}
