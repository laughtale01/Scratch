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
    
    /**
     * Log access attempt (for Zero Trust compatibility)
     */
    public void logAccessAttempt(String userId, String resource, boolean allowed, String reason) {
        String event = allowed ? "ACCESS_ALLOWED" : "ACCESS_BLOCKED";
        incrementEventCount(event);
        
        LOGGER.info("[SECURITY AUDIT] {} - User: {}, Resource: {}, Reason: {}, Time: {}", 
            event, userId, resource, reason, formatter.format(Instant.now()));
    }
    
    /**
     * Log security event (for Threat Detection compatibility)
     */
    public void logSecurityEvent(String component, String message, String level) {
        incrementEventCount("SECURITY_EVENT_" + level);
        
        String logMessage = String.format("[SECURITY AUDIT] %s - Component: %s, Message: %s, Time: %s", 
            level, component, message, formatter.format(Instant.now()));
            
        if ("ERROR".equals(level)) {
            LOGGER.error(logMessage);
        } else if ("WARN".equals(level)) {
            LOGGER.warn(logMessage);
        } else {
            LOGGER.info(logMessage);
        }
    }
    
    /**
     * Log threat detection (for Threat Detection Engine compatibility)
     */
    public void logThreatDetected(String userId, String action, double threatScore) {
        incrementEventCount("THREAT_DETECTED");
        
        LOGGER.warn("[SECURITY AUDIT] THREAT_DETECTED - User: {}, Action: {}, Score: {}, Time: {}", 
            userId, action, threatScore, formatter.format(Instant.now()));
    }
    
    /**
     * Log successful authentication (for JWT compatibility)
     */
    public void logSuccessfulAuthentication(String username, AuthenticationManager.UserRole role) {
        incrementEventCount("SUCCESSFUL_AUTHENTICATION");
        
        LOGGER.info("[SECURITY AUDIT] SUCCESSFUL_AUTHENTICATION - User: {}, Role: {}, Time: {}", 
            username, role, formatter.format(Instant.now()));
    }
    
    /**
     * Log token revocation (for JWT compatibility)
     */
    public void logTokenRevocation(String username, String tokenId) {
        incrementEventCount("TOKEN_REVOKED");
        
        LOGGER.info("[SECURITY AUDIT] TOKEN_REVOKED - User: {}, TokenId: {}, Time: {}", 
            username, tokenId, formatter.format(Instant.now()));
    }
    
    /**
     * Log token refresh failure (for JWT compatibility)
     */
    public void logTokenRefreshFailure(String username, String reason) {
        incrementEventCount("TOKEN_REFRESH_FAILURE");
        
        LOGGER.warn("[SECURITY AUDIT] TOKEN_REFRESH_FAILURE - User: {}, Reason: {}, Time: {}", 
            username, reason, formatter.format(Instant.now()));
    }
    
    /**
     * Log suspicious activity detected by threat detection system
     */
    public void logSuspiciousActivity(String username, String activity, Map<String, Object> context) {
        incrementEventCount("SUSPICIOUS_ACTIVITY");
        
        StringBuilder contextStr = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            contextStr.append(" [Context: ");
            context.forEach((key, value) -> contextStr.append(key).append("=").append(value).append(", "));
            contextStr.setLength(contextStr.length() - 2); // Remove last comma and space
            contextStr.append("]");
        }
        
        LOGGER.warn("[SECURITY AUDIT] SUSPICIOUS_ACTIVITY - User: {}, Activity: {}, Time: {}{}", 
            username, activity, formatter.format(Instant.now()), contextStr.toString());
    }
    
    /**
     * Log access granted event
     */
    public void logAccessGranted(String username, String resource, String operation) {
        incrementEventCount("ACCESS_GRANTED");
        
        LOGGER.info("[SECURITY AUDIT] ACCESS_GRANTED - User: {}, Resource: {}, Operation: {}, Time: {}", 
            username, resource, operation, formatter.format(Instant.now()));
    }
    
    /**
     * Log access denied event
     */
    public void logAccessDenied(String username, String resource, String operation, String reason) {
        incrementEventCount("ACCESS_DENIED");
        
        LOGGER.warn("[SECURITY AUDIT] ACCESS_DENIED - User: {}, Resource: {}, Operation: {}, Reason: {}, Time: {}", 
            username, resource, operation, reason, formatter.format(Instant.now()));
    }
    
    /**
     * Log token generation
     */
    public void logTokenGeneration(String username, AuthenticationManager.UserRole role, String tokenId) {
        incrementEventCount("TOKEN_GENERATED");
        
        LOGGER.info("[SECURITY AUDIT] TOKEN_GENERATED - User: {}, Role: {}, TokenId: {}, Time: {}", 
            username, role, tokenId, formatter.format(Instant.now()));
    }
    
    /**
     * Log authentication failure
     */
    public void logAuthenticationFailure(String username, String reason) {
        incrementEventCount("AUTH_FAILURE");
        
        LOGGER.warn("[SECURITY AUDIT] AUTH_FAILURE - User: {}, Reason: {}, Time: {}", 
            username, reason, formatter.format(Instant.now()));
    }
    
    /**
     * Log token refresh
     */
    public void logTokenRefresh(String username, String tokenId) {
        incrementEventCount("TOKEN_REFRESHED");
        
        LOGGER.info("[SECURITY AUDIT] TOKEN_REFRESHED - User: {}, TokenId: {}, Time: {}", 
            username, tokenId, formatter.format(Instant.now()));
    }
    
    /**
     * Log token generation failure
     */
    public void logTokenGenerationFailure(String username, Exception exception) {
        incrementEventCount("TOKEN_GENERATION_FAILURE");
        
        LOGGER.error("[SECURITY AUDIT] TOKEN_GENERATION_FAILURE - User: {}, Error: {}, Time: {}", 
            username, exception.getMessage(), formatter.format(Instant.now()));
    }
}