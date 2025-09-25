package edu.minecraft.collaboration.security.threat;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

/**
 * Represents a user activity event for threat detection
 */
public class UserActivityEvent {

    private final String username;
    private final String action;
    private final Instant timestamp;
    private final String ipAddress;
    private final Map<String, Object> metadata;

    private UserActivityEvent(Builder builder) {
        this.username = builder.username;
        this.action = builder.action;
        this.timestamp = builder.timestamp;
        this.ipAddress = builder.ipAddress;
        this.metadata = new HashMap<>(builder.metadata);
    }

    public String getUsername() { return username; }
    public String getAction() { return action; }
    public Instant getTimestamp() { return timestamp; }
    public String getIpAddress() { return ipAddress; }
    public Map<String, Object> getMetadata() { return metadata; }

    public static Builder builder(String username, String action) {
        return new Builder(username, action);
    }

    public static class Builder {
        private final String username;
        private final String action;
        private Instant timestamp = Instant.now();
        private String ipAddress = "127.0.0.1";
        private Map<String, Object> metadata = new HashMap<>();
        private edu.minecraft.collaboration.security.AuthenticationManager.UserRole userRole;

        public Builder(String username, String action) {
            this.username = username;
            this.action = action;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder userRole(edu.minecraft.collaboration.security.AuthenticationManager.UserRole role) {
            this.userRole = role;
            this.metadata.put("userRole", role.name());
            return this;
        }

        /**
         * Add resource accessed information
         */
        public Builder resourceAccessed(String resourceName) {
            this.metadata.put("resourceAccessed", resourceName);
            return this;
        }

        /**
         * Set whether the activity was successful
         */
        public Builder successful(boolean success) {
            this.metadata.put("successful", success);
            return this;
        }

        /**
         * Add a parameter to the event
         */
        public Builder parameter(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * Add network info to the event (for test compatibility)
         */
        public Builder networkInfo(Object networkInfo) {
            this.metadata.put("networkInfo", networkInfo);
            return this;
        }

        public UserActivityEvent build() {
            if (userRole != null) {
                metadata.put("userRole", userRole.name());
            }
            return new UserActivityEvent(this);
        }
    }
}
