package edu.minecraft.collaboration.security.threat;

import edu.minecraft.collaboration.security.AuthenticationManager.UserRole;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a user activity event for threat analysis
 */
public class UserActivityEvent {
    
    private final String username;
    private final UserRole userRole;
    private final String activity;
    private final String resourceAccessed;
    private final Instant timestamp;
    private final NetworkInfo networkInfo;
    private final Map<String, Object> parameters;
    private final boolean successful;
    
    private UserActivityEvent(Builder builder) {
        this.username = builder.username;
        this.userRole = builder.userRole;
        this.activity = builder.activity;
        this.resourceAccessed = builder.resourceAccessed;
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.networkInfo = builder.networkInfo;
        this.parameters = new HashMap<>(builder.parameters);
        this.successful = builder.successful;
    }
    
    public String getUsername() { return username; }
    public UserRole getUserRole() { return userRole; }
    public String getActivity() { return activity; }
    public String getResourceAccessed() { return resourceAccessed; }
    public Instant getTimestamp() { return timestamp; }
    public NetworkInfo getNetworkInfo() { return networkInfo; }
    public Map<String, Object> getParameters() { return parameters; }
    public boolean isSuccessful() { return successful; }
    
    public static Builder builder(String username, String activity) {
        return new Builder(username, activity);
    }
    
    public static class Builder {
        private final String username;
        private final String activity;
        private UserRole userRole = UserRole.STUDENT;
        private String resourceAccessed;
        private Instant timestamp;
        private NetworkInfo networkInfo;
        private Map<String, Object> parameters = new HashMap<>();
        private boolean successful = true;
        
        public Builder(String username, String activity) {
            this.username = username;
            this.activity = activity;
        }
        
        public Builder userRole(UserRole userRole) {
            this.userRole = userRole;
            return this;
        }
        
        public Builder resourceAccessed(String resourceAccessed) {
            this.resourceAccessed = resourceAccessed;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder networkInfo(NetworkInfo networkInfo) {
            this.networkInfo = networkInfo;
            return this;
        }
        
        public Builder parameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }
        
        public Builder successful(boolean successful) {
            this.successful = successful;
            return this;
        }
        
        public UserActivityEvent build() {
            return new UserActivityEvent(this);
        }
    }
}

/**
 * Network information for threat analysis
 */
class NetworkInfo {
    private final String ipAddress;
    private final String userAgent;
    private final String country;
    private final boolean isVpn;
    private final boolean isProxy;
    
    public NetworkInfo(String ipAddress, String userAgent) {
        this(ipAddress, userAgent, "unknown", false, false);
    }
    
    public NetworkInfo(String ipAddress, String userAgent, String country, boolean isVpn, boolean isProxy) {
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.country = country;
        this.isVpn = isVpn;
        this.isProxy = isProxy;
    }
    
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getCountry() { return country; }
    public boolean isVpn() { return isVpn; }
    public boolean isProxy() { return isProxy; }
}