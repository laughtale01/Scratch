package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security Audit Logger for tracking security-related events
 */
public class SecurityAuditLogger {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static SecurityAuditLogger instance;
    
    private final Map<String, Integer> eventCounts = new ConcurrentHashMap<>();
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
    
    private SecurityAuditLogger() {
        // Private constructor for singleton
    }
    
    public static SecurityAuditLogger getInstance() {
        if (instance == null) {
            synchronized (SecurityAuditLogger.class) {
                if (instance == null) {
                    instance = new SecurityAuditLogger();
                }
            }
        }
        return instance;
    }
    
    /**
     * Log an authentication attempt
     */
    public void logAuthAttempt(String username, boolean success, String ipAddress) {
        String event = success ? "AUTH_SUCCESS" : "AUTH_FAILURE";
        incrementEventCount(event);
        
        LOGGER.info("[SECURITY AUDIT] {} - User: {}, IP: {}, Time: {}", 
            event, username, ipAddress, formatter.format(Instant.now()));
    }
    
    /**
     * Log an access control decision
     */
    public void logAccessControl(String username, String resource, boolean granted) {
        String event = granted ? "ACCESS_GRANTED" : "ACCESS_DENIED";
        incrementEventCount(event);
        
        LOGGER.info("[SECURITY AUDIT] {} - User: {}, Resource: {}, Time: {}", 
            event, username, resource, formatter.format(Instant.now()));
    }
    
    /**
     * Log a security violation
     */
    public void logSecurityViolation(String type, String details) {
        incrementEventCount("SECURITY_VIOLATION");
        
        LOGGER.warn("[SECURITY AUDIT] VIOLATION - Type: {}, Details: {}, Time: {}", 
            type, details, formatter.format(Instant.now()));
    }
    
    /**
     * Log a rate limit violation
     */
    public void logRateLimitViolation(String identifier, String action) {
        incrementEventCount("RATE_LIMIT_VIOLATION");
        
        LOGGER.warn("[SECURITY AUDIT] RATE_LIMIT - Identifier: {}, Action: {}, Time: {}", 
            identifier, action, formatter.format(Instant.now()));
    }
    
    /**
     * Log token validation
     */
    public void logTokenValidation(String tokenId, boolean valid) {
        String event = valid ? "TOKEN_VALID" : "TOKEN_INVALID";
        incrementEventCount(event);
        
        LOGGER.info("[SECURITY AUDIT] {} - TokenId: {}, Time: {}", 
            event, tokenId, formatter.format(Instant.now()));
    }
    
    /**
     * Log permission change
     */
    public void logPermissionChange(String username, String permission, String action) {
        incrementEventCount("PERMISSION_CHANGE");
        
        LOGGER.info("[SECURITY AUDIT] PERMISSION_CHANGE - User: {}, Permission: {}, Action: {}, Time: {}", 
            username, permission, action, formatter.format(Instant.now()));
    }
    
    /**
     * Get event statistics
     */
    public Map<String, Integer> getEventStatistics() {
        return new ConcurrentHashMap<>(eventCounts);
    }
    
    /**
     * Clear event statistics
     */
    public void clearStatistics() {
        eventCounts.clear();
    }
    
    private void incrementEventCount(String event) {
        eventCounts.merge(event, 1, Integer::sum);
    }
}