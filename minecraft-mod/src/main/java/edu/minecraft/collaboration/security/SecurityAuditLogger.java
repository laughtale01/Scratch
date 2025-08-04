package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.security.AuthenticationManager.UserRole;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Security Audit Logger for comprehensive security event tracking
 * Supports Zero-Trust security model with detailed audit trails
 */
public class SecurityAuditLogger implements AutoCloseable {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    
    private final Map<String, AtomicLong> securityMetrics = new ConcurrentHashMap<>();
    private final Map<String, SecurityEvent> recentEvents = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    
    public SecurityAuditLogger() {
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "SecurityAuditLogger-Scheduler");
            t.setDaemon(true);
            return t;
        });
        
        // Clean up old events every 5 minutes
        scheduler.scheduleAtFixedRate(this::cleanupOldEvents, 5, 5, TimeUnit.MINUTES);
        
        LOGGER.info("SecurityAuditLogger initialized");
    }
    
    // JWT Token Events
    public void logTokenGeneration(String username, UserRole role, String tokenId) {
        logSecurityEvent("TOKEN_GENERATED", username, Map.of(
            "role", role.name(),
            "tokenId", tokenId,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("tokens.generated");
    }
    
    public void logTokenGenerationFailure(String username, Exception error) {
        logSecurityEvent("TOKEN_GENERATION_FAILED", username, Map.of(
            "error", error.getMessage(),
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("tokens.generation.failed");
    }
    
    public void logTokenRefresh(String username, String oldTokenId) {
        logSecurityEvent("TOKEN_REFRESHED", username, Map.of(
            "oldTokenId", oldTokenId,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("tokens.refreshed");
    }
    
    public void logTokenRefreshFailure(String username, String reason) {
        logSecurityEvent("TOKEN_REFRESH_FAILED", username, Map.of(
            "reason", reason,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("tokens.refresh.failed");
    }
    
    public void logTokenRevocation(String username, String tokenId) {
        logSecurityEvent("TOKEN_REVOKED", username, Map.of(
            "tokenId", tokenId,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("tokens.revoked");
    }
    
    // Authentication Events
    public void logSuccessfulAuthentication(String username, UserRole role) {
        logSecurityEvent("AUTH_SUCCESS", username, Map.of(
            "role", role.name(),
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("auth.success");
    }
    
    public void logAuthenticationFailure(String username, String reason) {
        logSecurityEvent("AUTH_FAILED", username, Map.of(
            "reason", reason,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("auth.failed");
        
        // Check for brute force attempts
        checkBruteForceAttempt(username);
    }
    
    // Authorization Events
    public void logAuthorizationFailure(String username, String operation, String resource, String reason) {
        logSecurityEvent("AUTHZ_DENIED", username, Map.of(
            "operation", operation,
            "resource", resource,
            "reason", reason,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("authz.denied");
    }
    
    public void logPrivilegeEscalationAttempt(String username, String attemptedRole) {
        logSecurityEvent("PRIVILEGE_ESCALATION_ATTEMPT", username, Map.of(
            "attemptedRole", attemptedRole,
            "timestamp", Instant.now().toString(),
            "severity", "HIGH"
        ));
        incrementMetric("security.privilege_escalation");
        LOGGER.warn("SECURITY ALERT: Privilege escalation attempt by user: {} to role: {}", username, attemptedRole);
    }
    
    // Access Control Events
    public void logAccessGranted(String username, String operation, String resource) {
        logSecurityEvent("ACCESS_GRANTED", username, Map.of(
            "operation", operation,
            "resource", resource,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("access.granted");
    }
    
    public void logAccessDenied(String username, String operation, String resource, String reason) {
        logSecurityEvent("ACCESS_DENIED", username, Map.of(
            "operation", operation,
            "resource", resource,
            "reason", reason,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("access.denied");
    }
    
    // Threat Detection Events
    public void logSuspiciousActivity(String username, String activity, Map<String, Object> context) {
        Map<String, Object> eventData = new ConcurrentHashMap<>(context);
        eventData.put("activity", activity);
        eventData.put("timestamp", Instant.now().toString());
        eventData.put("severity", "MEDIUM");
        
        logSecurityEvent("SUSPICIOUS_ACTIVITY", username, eventData);
        incrementMetric("threats.suspicious_activity");
        LOGGER.warn("SECURITY ALERT: Suspicious activity detected for user: {} - {}", username, activity);
    }
    
    public void logSecurityThreat(String username, String threatType, String severity, Map<String, Object> context) {
        Map<String, Object> eventData = new ConcurrentHashMap<>(context);
        eventData.put("threatType", threatType);
        eventData.put("severity", severity);
        eventData.put("timestamp", Instant.now().toString());
        
        logSecurityEvent("SECURITY_THREAT", username, eventData);
        incrementMetric("threats." + threatType.toLowerCase());
        LOGGER.error("SECURITY THREAT: {} threat detected for user: {} - Severity: {}", threatType, username, severity);
    }
    
    // Rate Limiting Events
    public void logRateLimitExceeded(String username, String operation, int currentRate, int limit) {
        logSecurityEvent("RATE_LIMIT_EXCEEDED", username, Map.of(
            "operation", operation,
            "currentRate", String.valueOf(currentRate),
            "limit", String.valueOf(limit),
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("rate_limit.exceeded");
    }
    
    // Network Security Events
    public void logSuspiciousConnection(String ipAddress, String reason) {
        logSecurityEvent("SUSPICIOUS_CONNECTION", "unknown", Map.of(
            "ipAddress", ipAddress,
            "reason", reason,
            "timestamp", Instant.now().toString(),
            "severity", "HIGH"
        ));
        incrementMetric("network.suspicious_connection");
        LOGGER.warn("SECURITY ALERT: Suspicious connection from IP: {} - {}", ipAddress, reason);
    }
    
    public void logConnectionBlocked(String ipAddress, String reason) {
        logSecurityEvent("CONNECTION_BLOCKED", "unknown", Map.of(
            "ipAddress", ipAddress,
            "reason", reason,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("network.blocked");
    }
    
    // Data Access Events
    public void logDataAccess(String username, String dataType, String operation) {
        logSecurityEvent("DATA_ACCESS", username, Map.of(
            "dataType", dataType,
            "operation", operation,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("data." + operation.toLowerCase());
    }
    
    public void logDataModification(String username, String dataType, String objectId) {
        logSecurityEvent("DATA_MODIFIED", username, Map.of(
            "dataType", dataType,
            "objectId", objectId,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("data.modified");
    }
    
    // Configuration Changes
    public void logConfigurationChange(String username, String configKey, String oldValue, String newValue) {
        logSecurityEvent("CONFIG_CHANGED", username, Map.of(
            "configKey", configKey,
            "oldValue", oldValue != null ? oldValue : "null",
            "newValue", newValue != null ? newValue : "null",
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("config.changed");
    }
    
    // Session Management
    public void logSessionCreated(String username, String sessionId) {
        logSecurityEvent("SESSION_CREATED", username, Map.of(
            "sessionId", sessionId,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("sessions.created");
    }
    
    public void logSessionExpired(String username, String sessionId) {
        logSecurityEvent("SESSION_EXPIRED", username, Map.of(
            "sessionId", sessionId,
            "timestamp", Instant.now().toString()
        ));
        incrementMetric("sessions.expired");
    }
    
    private void logSecurityEvent(String eventType, String username, Map<String, Object> eventData) {
        SecurityEvent event = new SecurityEvent(eventType, username, eventData);
        String eventKey = eventType + "_" + username + "_" + System.currentTimeMillis();
        recentEvents.put(eventKey, event);
        
        // Log to application logger
        LOGGER.info("SECURITY_EVENT: {} - User: {} - Data: {}", eventType, username, eventData);
    }
    
    private void incrementMetric(String metricName) {
        securityMetrics.computeIfAbsent(metricName, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    private void checkBruteForceAttempt(String username) {
        long failedAttempts = securityMetrics.getOrDefault("auth.failed", new AtomicLong(0)).get();
        if (failedAttempts > 0 && failedAttempts % 5 == 0) {
            logSuspiciousActivity(username, "POTENTIAL_BRUTE_FORCE", Map.of(
                "failedAttempts", String.valueOf(failedAttempts),
                "timeWindow", "5 minutes"
            ));
        }
    }
    
    private void cleanupOldEvents() {
        long cutoffTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        recentEvents.entrySet().removeIf(entry -> {
            String[] parts = entry.getKey().split("_");
            if (parts.length > 0) {
                try {
                    long eventTime = Long.parseLong(parts[parts.length - 1]);
                    return eventTime < cutoffTime;
                } catch (NumberFormatException e) {
                    return true; // Remove malformed entries
                }
            }
            return true;
        });
    }
    
    public Map<String, Long> getSecurityMetrics() {
        Map<String, Long> metrics = new ConcurrentHashMap<>();
        securityMetrics.forEach((key, value) -> metrics.put(key, value.get()));
        return metrics;
    }
    
    public long getMetric(String metricName) {
        return securityMetrics.getOrDefault(metricName, new AtomicLong(0)).get();
    }
    
    @Override
    public void close() {
        LOGGER.info("Closing SecurityAuditLogger");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        recentEvents.clear();
        securityMetrics.clear();
    }
    
    /**
     * Security Event data structure
     */
    private static class SecurityEvent {
        private final String eventType;
        private final String username;
        private final Map<String, Object> eventData;
        private final Instant timestamp;
        
        public SecurityEvent(String eventType, String username, Map<String, Object> eventData) {
            this.eventType = eventType;
            this.username = username;
            this.eventData = new ConcurrentHashMap<>(eventData);
            this.timestamp = Instant.now();
        }
        
        public String getEventType() { return eventType; }
        public String getUsername() { return username; }
        public Map<String, Object> getEventData() { return eventData; }
        public Instant getTimestamp() { return timestamp; }
    }
}