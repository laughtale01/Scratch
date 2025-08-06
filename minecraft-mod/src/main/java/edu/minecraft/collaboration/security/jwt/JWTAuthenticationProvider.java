package edu.minecraft.collaboration.security.jwt;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.security.SecurityAuditLogger;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified JWT Authentication Provider
 * Basic JWT-like token management without external dependencies
 */
public class JWTAuthenticationProvider {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static final String ISSUER = "minecraft-collaboration";
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);
    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);
    
    private final Map<String, TokenMetadata> activeTokens = new ConcurrentHashMap<>();
    private final SecurityAuditLogger auditLogger;
    private final String secretKey;
    
    public JWTAuthenticationProvider(SecurityAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
        this.secretKey = generateSecretKey();
        
        LOGGER.info("JWT Authentication Provider initialized");
    }
    
    /**
     * Generate a new access token
     */
    public String generateToken(String username, String role) {
        try {
            String tokenId = UUID.randomUUID().toString();
            Instant expiry = Instant.now().plus(TOKEN_VALIDITY);
            
            // Create simple token payload
            String payload = String.format("%s|%s|%s|%s|%d", 
                tokenId, username, role, ISSUER, expiry.toEpochMilli());
            
            // Encode token (simplified - in production use proper JWT library)
            String token = Base64.getEncoder().encodeToString(payload.getBytes());
            
            // Store token metadata
            activeTokens.put(tokenId, new TokenMetadata(username, role, expiry, TokenType.ACCESS));
            
            LOGGER.debug("Generated access token for user: {}", username);
            auditLogger.logTokenValidation(tokenId, true);
            
            return token;
        } catch (Exception e) {
            LOGGER.error("Failed to generate token for user: {}", username, e);
            return null;
        }
    }
    
    /**
     * Validate a token
     */
    public TokenValidation validateToken(String token) {
        try {
            // Decode token
            String payload = new String(Base64.getDecoder().decode(token));
            String[] parts = payload.split("\\|");
            
            if (parts.length != 5) {
                auditLogger.logTokenValidation("invalid", false);
                return TokenValidation.invalid("Invalid token format");
            }
            
            String tokenId = parts[0];
            String username = parts[1];
            String role = parts[2];
            String issuer = parts[3];
            long expiryMillis = Long.parseLong(parts[4]);
            
            // Check issuer
            if (!ISSUER.equals(issuer)) {
                auditLogger.logTokenValidation(tokenId, false);
                return TokenValidation.invalid("Invalid issuer");
            }
            
            // Check expiry
            if (Instant.now().toEpochMilli() > expiryMillis) {
                auditLogger.logTokenValidation(tokenId, false);
                return TokenValidation.invalid("Token expired");
            }
            
            // Check if token is active
            TokenMetadata metadata = activeTokens.get(tokenId);
            if (metadata == null) {
                auditLogger.logTokenValidation(tokenId, false);
                return TokenValidation.invalid("Token not found");
            }
            
            auditLogger.logTokenValidation(tokenId, true);
            return TokenValidation.valid(username, role);
            
        } catch (Exception e) {
            LOGGER.error("Token validation failed", e);
            auditLogger.logTokenValidation("unknown", false);
            return TokenValidation.invalid("Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Refresh a token
     */
    public String refreshToken(String refreshToken) {
        TokenValidation validation = validateToken(refreshToken);
        if (validation.isValid()) {
            return generateToken(validation.getUsername(), validation.getRole());
        }
        return null;
    }
    
    /**
     * Revoke a token
     */
    public void revokeToken(String tokenId) {
        activeTokens.remove(tokenId);
        LOGGER.info("Revoked token: {}", tokenId);
    }
    
    /**
     * Clean up expired tokens
     */
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        activeTokens.entrySet().removeIf(entry -> entry.getValue().expiry.isBefore(now));
    }
    
    /**
     * Generate a secret key
     */
    private String generateSecretKey() {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
    }
    
    /**
     * Token metadata
     */
    private static class TokenMetadata {
        final String username;
        final String role;
        final Instant expiry;
        final TokenType type;
        
        TokenMetadata(String username, String role, Instant expiry, TokenType type) {
            this.username = username;
            this.role = role;
            this.expiry = expiry;
            this.type = type;
        }
    }
    
    /**
     * Token type
     */
    private enum TokenType {
        ACCESS, REFRESH
    }
    
    /**
     * Token validation result
     */
    public static class TokenValidation {
        private final boolean valid;
        private final String username;
        private final String role;
        private final String error;
        
        private TokenValidation(boolean valid, String username, String role, String error) {
            this.valid = valid;
            this.username = username;
            this.role = role;
            this.error = error;
        }
        
        public static TokenValidation valid(String username, String role) {
            return new TokenValidation(true, username, role, null);
        }
        
        public static TokenValidation invalid(String error) {
            return new TokenValidation(false, null, null, error);
        }
        
        public boolean isValid() { return valid; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getError() { return error; }
    }
}