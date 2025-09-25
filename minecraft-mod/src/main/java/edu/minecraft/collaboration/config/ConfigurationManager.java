package edu.minecraft.collaboration.config;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration management system for the Minecraft Collaboration Mod.
 * Supports environment-specific configurations and property overrides.
 *
 * Features:
 * - Environment-specific property files (dev, prod, test)
 * - Property value caching for performance
 * - Type-safe property accessors
 * - Default value support
 * - System property and environment variable overrides
 */
public class ConfigurationManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();

    // Configuration file names
    private static final String DEFAULT_CONFIG = "application.properties";
    private static final String ENV_CONFIG_TEMPLATE = "application-%s.properties";

    // Environment variable and system property keys
    private static final String ENV_PROFILE_KEY = "MINECRAFT_COLLABORATION_PROFILE";
    private static final String SYS_PROFILE_KEY = "minecraft.collaboration.profile";

    // Property cache
    private final Map<String, String> propertyCache = new ConcurrentHashMap<>();
    private final Properties properties = new Properties();

    // Current environment profile
    private String activeProfile = "default";

    public ConfigurationManager() {
        loadConfigurations();
    }

    /**
     * Load configurations from properties files
     */
    private void loadConfigurations() {
        try {
            // Determine active profile
            determineActiveProfile();

            // Load default configuration
            loadPropertiesFile(DEFAULT_CONFIG);

            // Load environment-specific configuration if profile is set
            if (!"default".equals(activeProfile)) {
                String envConfigFile = String.format(ENV_CONFIG_TEMPLATE, activeProfile);
                loadPropertiesFile(envConfigFile);
            }

            // Cache all properties
            cacheProperties();

            LOGGER.info("Configuration loaded successfully with profile: {}", activeProfile);
            LOGGER.info("Loaded {} configuration properties", propertyCache.size());

        } catch (Exception e) {
            LOGGER.error("Failed to load configuration", e);
            throw new RuntimeException("Configuration loading failed", e);
        }
    }

    /**
     * Determine the active profile from environment variables or system properties
     */
    private void determineActiveProfile() {
        // Check system property first
        String profile = System.getProperty(SYS_PROFILE_KEY);

        // Fall back to environment variable
        if (profile == null || profile.trim().isEmpty()) {
            profile = System.getenv(ENV_PROFILE_KEY);
        }

        // Default to "default" if nothing is set
        if (profile != null && !profile.trim().isEmpty()) {
            activeProfile = profile.trim().toLowerCase();
            LOGGER.info("Active profile set to: {}", activeProfile);
        } else {
            LOGGER.info("No profile specified, using default configuration");
        }
    }

    /**
     * Load properties from a specific file
     */
    private void loadPropertiesFile(String filename) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (inputStream != null) {
                Properties tempProps = new Properties();
                tempProps.load(inputStream);

                // Merge into main properties (later files override earlier ones)
                properties.putAll(tempProps);

                LOGGER.debug("Loaded configuration file: {}", filename);
            } else {
                if (DEFAULT_CONFIG.equals(filename)) {
                    LOGGER.warn("Default configuration file not found: {}", filename);
                } else {
                    LOGGER.debug("Environment-specific configuration file not found: {}", filename);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error loading configuration file: {}", filename, e);
        }
    }

    /**
     * Cache all properties for quick access
     */
    private void cacheProperties() {
        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (value != null) {
                propertyCache.put(key, value.trim());
            }
        }
    }

    /**
     * Get a string property value
     */
    public String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * Get a string property value with default
     */
    public String getProperty(String key, String defaultValue) {
        // Check system property override first
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.trim().isEmpty()) {
            return systemValue.trim();
        }

        // Check environment variable override (convert key to env format)
        String envKey = key.toUpperCase().replace('.', '_');
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }

        // Get from cache or return default
        return propertyCache.getOrDefault(key, defaultValue);
    }

    /**
     * Get an integer property value
     */
    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid integer value for property {}: {}, using default: {}",
                       key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Get a boolean property value
     */
    public boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        return Boolean.parseBoolean(value);
    }

    /**
     * Get a long property value
     */
    public long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid long value for property {}: {}, using default: {}",
                       key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Get a double property value
     */
    public double getDoubleProperty(String key, double defaultValue) {
        String value = getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid double value for property {}: {}, using default: {}",
                       key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Set a property value (for runtime configuration or testing)
     */
    public void setProperty(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        propertyCache.put(key, value);
        properties.setProperty(key, value);

        LOGGER.debug("Set property {} = {}", key, value);
    }

    /**
     * Check if a property exists
     */
    public boolean hasProperty(String key) {
        return propertyCache.containsKey(key) ||
             System.getProperty(key) != null
               || System.getenv(key.toUpperCase().replace('.', '_')) != null;
    }

    /**
     * Get all property keys
     */
    public java.util.Set<String> getPropertyKeys() {
        return java.util.Collections.unmodifiableSet(propertyCache.keySet());
    }

    /**
     * Get the active profile
     */
    public String getActiveProfile() {
        return activeProfile;
    }

    /**
     * Reload configurations (useful for testing or runtime updates)
     */
    public void reload() {
        propertyCache.clear();
        properties.clear();
        loadConfigurations();
        LOGGER.info("Configuration reloaded");
    }

    /**
     * Get configuration statistics
     */
    public ConfigurationStatistics getStatistics() {
        return new ConfigurationStatistics(
            activeProfile,
            propertyCache.size(),
            properties.size()
        );
    }

    /**
     * Configuration statistics class
     */
    public static class ConfigurationStatistics {
        private final String activeProfile;
        private final int cachedProperties;
        private final int totalProperties;

        public ConfigurationStatistics(String activeProfile, int cachedProperties, int totalProperties) {
            this.activeProfile = activeProfile;
            this.cachedProperties = cachedProperties;
            this.totalProperties = totalProperties;
        }

        public String getActiveProfile() {
            return activeProfile;
        }

        public int getCachedProperties() {
            return cachedProperties;
        }

        public int getTotalProperties() {
            return totalProperties;
        }

        @Override
        public String toString() {
            return String.format(
                "ConfigurationStatistics{activeProfile='%s', cachedProperties=%d, totalProperties=%d}",
                activeProfile, cachedProperties, totalProperties
            );
        }
    }
}
