package edu.minecraft.collaboration.security.zerotrust;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Access context for zero trust access control tests
 */
public class AccessContext {
    private final String userId;
    private final String resource;
    private final String operation;
    private final Instant timestamp;
    private final Map<String, Object> attributes;
    
    private AccessContext(Builder builder) {
        this.userId = builder.userId;
        this.resource = builder.resource;
        this.operation = builder.operation;
        this.timestamp = builder.timestamp;
        this.attributes = new HashMap<>(builder.attributes);
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getResource() {
        return resource;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    public Object getUser() {
        return attributes.get("user");
    }
    
    public Object getNetworkContext() {
        return attributes.get("networkContext");
    }
    
    public Object getTimeContext() {
        return attributes.get("timeContext");
    }
    
    public Object getSessionContext() {
        return attributes.get("sessionContext");
    }
    
    public Object getDeviceContext() {
        return attributes.get("deviceContext");
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String userId;
        private String resource;
        private String operation;
        private Instant timestamp = Instant.now();
        private Map<String, Object> attributes = new HashMap<>();
        
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }
        
        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }
        
        public Builder operation(String operation) {
            this.operation = operation;
            return this;
        }
        
        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder attribute(String key, Object value) {
            this.attributes.put(key, value);
            return this;
        }
        
        public Builder user(Object user) {
            this.attributes.put("user", user);
            return this;
        }
        
        public Builder operation(Object operation) {
            this.attributes.put("operation", operation);
            return this;
        }
        
        public Builder resource(Object resource) {
            this.attributes.put("resource", resource);
            return this;
        }
        
        public Builder networkContext(Object networkContext) {
            this.attributes.put("networkContext", networkContext);
            return this;
        }
        
        public AccessContext build() {
            return new AccessContext(this);
        }
    }
}