package edu.minecraft.collaboration.security.zerotrust;

import java.util.HashMap;
import java.util.Map;

/**
 * Access Context for Zero-Trust Security Evaluation
 * Contains all relevant information for making authorization decisions
 */
public class AccessContext {
    
    private final ZeroTrustAccessControl.User user;
    private final ZeroTrustAccessControl.Operation operation;
    private final ZeroTrustAccessControl.Resource resource;
    private final ZeroTrustAccessControl.NetworkContext networkContext;
    private final ZeroTrustAccessControl.TimeContext timeContext;
    private final ZeroTrustAccessControl.SessionContext sessionContext;
    private final ZeroTrustAccessControl.DeviceContext deviceContext;
    private final Map<String, Object> additionalContext;
    
    private AccessContext(Builder builder) {
        this.user = builder.user;
        this.operation = builder.operation;
        this.resource = builder.resource;
        this.networkContext = builder.networkContext;
        this.timeContext = builder.timeContext;
        this.sessionContext = builder.sessionContext;
        this.deviceContext = builder.deviceContext;
        this.additionalContext = new HashMap<>(builder.additionalContext);
    }
    
    public ZeroTrustAccessControl.User getUser() { return user; }
    public ZeroTrustAccessControl.Operation getOperation() { return operation; }
    public ZeroTrustAccessControl.Resource getResource() { return resource; }
    public ZeroTrustAccessControl.NetworkContext getNetworkContext() { return networkContext; }
    public ZeroTrustAccessControl.TimeContext getTimeContext() { return timeContext; }
    public ZeroTrustAccessControl.SessionContext getSessionContext() { return sessionContext; }
    public ZeroTrustAccessControl.DeviceContext getDeviceContext() { return deviceContext; }
    public Map<String, Object> getAdditionalContext() { return additionalContext; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ZeroTrustAccessControl.User user;
        private ZeroTrustAccessControl.Operation operation;
        private ZeroTrustAccessControl.Resource resource;
        private ZeroTrustAccessControl.NetworkContext networkContext;
        private ZeroTrustAccessControl.TimeContext timeContext;
        private ZeroTrustAccessControl.SessionContext sessionContext;
        private ZeroTrustAccessControl.DeviceContext deviceContext;
        private Map<String, Object> additionalContext = new HashMap<>();
        
        public Builder user(ZeroTrustAccessControl.User user) {
            this.user = user;
            return this;
        }
        
        public Builder operation(ZeroTrustAccessControl.Operation operation) {
            this.operation = operation;
            return this;
        }
        
        public Builder resource(ZeroTrustAccessControl.Resource resource) {
            this.resource = resource;
            return this;
        }
        
        public Builder networkContext(ZeroTrustAccessControl.NetworkContext networkContext) {
            this.networkContext = networkContext;
            return this;
        }
        
        public Builder timeContext(ZeroTrustAccessControl.TimeContext timeContext) {
            this.timeContext = timeContext;
            return this;
        }
        
        public Builder sessionContext(ZeroTrustAccessControl.SessionContext sessionContext) {
            this.sessionContext = sessionContext;
            return this;
        }
        
        public Builder deviceContext(ZeroTrustAccessControl.DeviceContext deviceContext) {
            this.deviceContext = deviceContext;
            return this;
        }
        
        public Builder additionalContext(String key, Object value) {
            this.additionalContext.put(key, value);
            return this;
        }
        
        public AccessContext build() {
            if (user == null || operation == null || resource == null) {
                throw new IllegalArgumentException("User, operation, and resource are required");
            }
            
            // Set defaults for optional contexts
            if (networkContext == null) {
                networkContext = new ZeroTrustAccessControl.NetworkContext();
            }
            if (timeContext == null) {
                timeContext = new ZeroTrustAccessControl.TimeContext();
            }
            if (sessionContext == null) {
                sessionContext = new ZeroTrustAccessControl.SessionContext("unknown", timeContext.getTimestamp());
            }
            if (deviceContext == null) {
                deviceContext = new ZeroTrustAccessControl.DeviceContext("unknown", "unknown");
            }
            
            return new AccessContext(this);
        }
    }
}