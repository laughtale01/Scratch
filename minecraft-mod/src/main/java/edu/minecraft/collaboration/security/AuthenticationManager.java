package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Base64;
import java.time.Instant;

/**
 * Manages authentication for WebSocket connections
 * Implements token-based authentication for secure connections
 */
public class AuthenticationManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static AuthenticationManager instance;
    
    // Token configuration
    private static final int TOKEN_LENGTH = 32; // bytes
    private static final long TOKEN_EXPIRY_HOURS = 24;
    private static final long TOKEN_EXPIRY_MS = TimeUnit.HOURS.toMillis(TOKEN_EXPIRY_HOURS);
    
    // Secure random for token generation
    private final SecureRandom secureRandom = new SecureRandom();
    
    // Store active tokens with expiry time
    private final Map<String, TokenInfo> activeTokens = new ConcurrentHashMap<>();
    
    // Store authenticated connections
    private final Map<String, String> authenticatedConnections = new ConcurrentHashMap<>();
    
    // Teacher/Admin tokens (in production, these would be stored securely)
    private final Map<String, UserRole> teacherTokens = new ConcurrentHashMap<>();
    
    public enum UserRole {
        STUDENT,
        TEACHER,
        ADMIN
    }
    
    private static class TokenInfo {
        final String token;
        final long expiryTime;
        final UserRole role;
        final String username;
        
        TokenInfo(String token, long expiryTime, UserRole role, String username) {
            this.token = token;
            this.expiryTime = expiryTime;
            this.role = role;
            this.username = username;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
    
    private AuthenticationManager() {
        // Schedule cleanup of expired tokens
        MinecraftCollaborationMod.getExecutor().scheduleAtFixedRate(
            this::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
    }
    
    public static AuthenticationManager getInstance() {
        if (instance == null) {
            instance = new AuthenticationManager();
        }
        return instance;
    }
    
    /**
     * Generate a new authentication token
     * @param username The username to associate with the token
     * @param role The user role
     * @return The generated token
     */
    public String generateToken(String username, UserRole role) {
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
        long expiryTime = System.currentTimeMillis() + TOKEN_EXPIRY_MS;
        TokenInfo tokenInfo = new TokenInfo(token, expiryTime, role, username);
        
        activeTokens.put(token, tokenInfo);
        
        LOGGER.info("Generated new token for user: {} with role: {}", username, role);
        return token;
    }
    
    /**
     * Validate a token
     * @param token The token to validate
     * @return true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        TokenInfo tokenInfo = activeTokens.get(token);
        if (tokenInfo == null) {
            LOGGER.warn("Invalid token attempted");
            return false;
        }
        
        if (tokenInfo.isExpired()) {
            LOGGER.warn("Expired token attempted for user: {}", tokenInfo.username);
            activeTokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * Authenticate a connection
     * @param connectionId The connection identifier
     * @param token The authentication token
     * @return true if authenticated successfully
     */
    public boolean authenticateConnection(String connectionId, String token) {
        if (!validateToken(token)) {
            return false;
        }
        
        TokenInfo tokenInfo = activeTokens.get(token);
        authenticatedConnections.put(connectionId, tokenInfo.username);
        
        LOGGER.info("Connection {} authenticated as user: {} with role: {}", 
            connectionId, tokenInfo.username, tokenInfo.role);
        return true;
    }
    
    /**
     * Check if a connection is authenticated
     * @param connectionId The connection identifier
     * @return true if authenticated
     */
    public boolean isAuthenticated(String connectionId) {
        return authenticatedConnections.containsKey(connectionId);
    }
    
    /**
     * Get the username for an authenticated connection
     * @param connectionId The connection identifier
     * @return The username or null if not authenticated
     */
    public String getUsername(String connectionId) {
        return authenticatedConnections.get(connectionId);
    }
    
    /**
     * Get the role for a token
     * @param token The authentication token
     * @return The user role or STUDENT if not found
     */
    public UserRole getRole(String token) {
        TokenInfo tokenInfo = activeTokens.get(token);
        return tokenInfo != null ? tokenInfo.role : UserRole.STUDENT;
    }
    
    /**
     * Get the role for a connection
     * @param connectionId The connection identifier
     * @return The user role or STUDENT if not found
     */
    public UserRole getRoleForConnection(String connectionId) {
        String username = authenticatedConnections.get(connectionId);
        if (username == null) {
            return UserRole.STUDENT;
        }
        
        // Find the token info by username
        for (TokenInfo tokenInfo : activeTokens.values()) {
            if (tokenInfo.username.equals(username) && !tokenInfo.isExpired()) {
                return tokenInfo.role;
            }
        }
        
        return UserRole.STUDENT;
    }
    
    /**
     * Revoke a token
     * @param token The token to revoke
     */
    public void revokeToken(String token) {
        TokenInfo tokenInfo = activeTokens.remove(token);
        if (tokenInfo != null) {
            // Remove any authenticated connections using this token
            authenticatedConnections.entrySet().removeIf(entry -> 
                entry.getValue().equals(tokenInfo.username));
            
            LOGGER.info("Revoked token for user: {}", tokenInfo.username);
        }
    }
    
    /**
     * Remove authentication for a connection
     * @param connectionId The connection identifier
     */
    public void removeConnection(String connectionId) {
        String username = authenticatedConnections.remove(connectionId);
        if (username != null) {
            LOGGER.info("Removed authentication for connection: {} (user: {})", 
                connectionId, username);
        }
    }
    
    /**
     * Clean up expired tokens
     */
    private void cleanupExpiredTokens() {
        int removed = 0;
        for (Map.Entry<String, TokenInfo> entry : activeTokens.entrySet()) {
            if (entry.getValue().isExpired()) {
                activeTokens.remove(entry.getKey());
                removed++;
            }
        }
        
        if (removed > 0) {
            LOGGER.info("Cleaned up {} expired tokens", removed);
        }
    }
    
    /**
     * Add a teacher token (for initial setup)
     * @param username The teacher username
     * @return The generated token
     */
    public String addTeacherToken(String username) {
        String token = generateToken(username, UserRole.TEACHER);
        teacherTokens.put(username, UserRole.TEACHER);
        return token;
    }
    
    /**
     * Check if a user has elevated privileges
     * @param connectionId The connection identifier
     * @return true if the user is a teacher or admin
     */
    public boolean hasElevatedPrivileges(String connectionId) {
        UserRole role = getRoleForConnection(connectionId);
        return role == UserRole.TEACHER || role == UserRole.ADMIN;
    }
    
    /**
     * Get statistics about active tokens
     * @return Map of statistics
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new ConcurrentHashMap<>();
        stats.put("activeTokens", activeTokens.size());
        stats.put("authenticatedConnections", authenticatedConnections.size());
        stats.put("teacherTokens", teacherTokens.size());
        
        // Count by role
        int students = 0, teachers = 0, admins = 0;
        for (TokenInfo tokenInfo : activeTokens.values()) {
            if (!tokenInfo.isExpired()) {
                switch (tokenInfo.role) {
                    case STUDENT -> students++;
                    case TEACHER -> teachers++;
                    case ADMIN -> admins++;
                }
            }
        }
        
        stats.put("activeStudents", students);
        stats.put("activeTeachers", teachers);
        stats.put("activeAdmins", admins);
        
        return stats;
    }
}