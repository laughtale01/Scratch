package edu.minecraft.collaboration.security.jwt;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.security.AuthenticationManager;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise-grade JWT Authentication Provider
 * Implements OAuth2-compatible JWT token management with advanced security features
 */
public class JWTAuthenticationProvider {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static final String ISSUER = "minecraft-collaboration";
    private static final Duration TOKEN_VALIDITY = Duration.ofHours(24);
    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);
    
    private final SecretKey signingKey;
    private final JwtParser jwtParser;
    private final Map<String, TokenMetadata> activeTokens = new ConcurrentHashMap<>();
    private final SecurityAuditLogger auditLogger;
    
    public JWTAuthenticationProvider(SecurityAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
        this.signingKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        this.jwtParser = Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .requireIssuer(ISSUER)
            .build();
        
        LOGGER.info("JWT Authentication Provider initialized with HS256 signing");
    }
    
    /**
     * Generate JWT access token with advanced claims
     */
    public JWTTokenPair generateTokenPair(String username, AuthenticationManager.UserRole role, Map<String, Object> additionalClaims) {
        Instant now = Instant.now();
        String tokenId = UUID.randomUUID().toString();
        
        try {
            // Access Token
            String accessToken = Jwts.builder()
                .setId(tokenId)
                .setIssuer(ISSUER)
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(TOKEN_VALIDITY)))
                .claim("role", role.name())
                .claim("token_type", "access")
                .claim("session_id", UUID.randomUUID().toString())
                .addClaims(additionalClaims)
                .signWith(signingKey)
                .compact();
            
            // Refresh Token
            String refreshToken = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer(ISSUER)
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(REFRESH_TOKEN_VALIDITY)))
                .claim("token_type", "refresh")
                .claim("access_token_id", tokenId)
                .signWith(signingKey)
                .compact();
            
            // Store token metadata
            TokenMetadata metadata = new TokenMetadata(username, role, now, now.plus(TOKEN_VALIDITY));
            activeTokens.put(tokenId, metadata);
            
            auditLogger.logTokenGeneration(username, role, tokenId);
            LOGGER.debug("Generated JWT token pair for user: {} with role: {}", username, role);
            
            return new JWTTokenPair(accessToken, refreshToken, TOKEN_VALIDITY.toSeconds());
            
        } catch (Exception e) {
            auditLogger.logTokenGenerationFailure(username, e);
            LOGGER.error("Failed to generate JWT token for user: {}", username, e);
            throw new JWTAuthenticationException("Token generation failed", e);
        }
    }
    
    /**
     * Validate JWT token with comprehensive security checks
     */
    public AuthenticationResult authenticate(String token) {
        if (token == null || token.trim().isEmpty()) {
            return AuthenticationResult.failure("Token is required");
        }
        
        try {
            Claims claims = jwtParser.parseClaimsJws(token).getBody();
            String tokenId = claims.getId();
            String username = claims.getSubject();
            String roleStr = claims.get("role", String.class);
            String tokenType = claims.get("token_type", String.class);
            
            // Validate token type
            if (!"access".equals(tokenType)) {
                auditLogger.logAuthenticationFailure(username, "Invalid token type: " + tokenType);
                return AuthenticationResult.failure("Invalid token type");
            }
            
            // Check if token is still active
            TokenMetadata metadata = activeTokens.get(tokenId);
            if (metadata == null) {
                auditLogger.logAuthenticationFailure(username, "Token not found in active tokens");
                return AuthenticationResult.failure("Token has been revoked");
            }
            
            // Validate expiration
            if (Instant.now().isAfter(metadata.getExpiresAt())) {
                activeTokens.remove(tokenId);
                auditLogger.logAuthenticationFailure(username, "Token expired");
                return AuthenticationResult.failure("Token has expired");
            }
            
            // Validate role
            AuthenticationManager.UserRole role = AuthenticationManager.UserRole.valueOf(roleStr);
            if (!metadata.getRole().equals(role)) {
                auditLogger.logAuthenticationFailure(username, "Role mismatch");
                return AuthenticationResult.failure("Token validation failed");
            }
            
            // Update last access time
            metadata.updateLastAccess();
            
            auditLogger.logSuccessfulAuthentication(username, role);
            LOGGER.debug("Successfully authenticated user: {} with role: {}", username, role);
            
            return AuthenticationResult.success(new AuthenticatedUser(username, role, tokenId, claims));
            
        } catch (ExpiredJwtException e) {
            auditLogger.logAuthenticationFailure("unknown", "Token expired: " + e.getMessage());
            return AuthenticationResult.failure("Token has expired");
        } catch (JwtException e) {
            auditLogger.logAuthenticationFailure("unknown", "Invalid token: " + e.getMessage());
            LOGGER.warn("JWT validation failed: {}", e.getMessage());
            return AuthenticationResult.failure("Invalid token");
        } catch (Exception e) {
            auditLogger.logAuthenticationFailure("unknown", "Authentication error: " + e.getMessage());
            LOGGER.error("Unexpected error during JWT authentication", e);
            return AuthenticationResult.failure("Authentication failed");
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    public JWTTokenPair refreshToken(String refreshToken) {
        try {
            Claims claims = jwtParser.parseClaimsJws(refreshToken).getBody();
            String username = claims.getSubject();
            String tokenType = claims.get("token_type", String.class);
            String accessTokenId = claims.get("access_token_id", String.class);
            
            if (!"refresh".equals(tokenType)) {
                auditLogger.logTokenRefreshFailure(username, "Invalid token type");
                throw new JWTAuthenticationException("Invalid refresh token type");
            }
            
            // Get original token metadata
            TokenMetadata originalMetadata = activeTokens.get(accessTokenId);
            if (originalMetadata == null) {
                auditLogger.logTokenRefreshFailure(username, "Original token not found");
                throw new JWTAuthenticationException("Original token not found");
            }
            
            // Generate new token pair
            JWTTokenPair newTokenPair = generateTokenPair(username, originalMetadata.getRole(), Map.of());
            
            // Revoke old token
            activeTokens.remove(accessTokenId);
            
            auditLogger.logTokenRefresh(username, accessTokenId);
            LOGGER.info("Successfully refreshed token for user: {}", username);
            
            return newTokenPair;
            
        } catch (JwtException e) {
            auditLogger.logTokenRefreshFailure("unknown", "Invalid refresh token: " + e.getMessage());
            throw new JWTAuthenticationException("Invalid refresh token", e);
        }
    }
    
    /**
     * Revoke token (logout)
     */
    public boolean revokeToken(String token) {
        try {
            Claims claims = jwtParser.parseClaimsJws(token).getBody();
            String tokenId = claims.getId();
            String username = claims.getSubject();
            
            TokenMetadata removed = activeTokens.remove(tokenId);
            if (removed != null) {
                auditLogger.logTokenRevocation(username, tokenId);
                LOGGER.info("Successfully revoked token for user: {}", username);
                return true;
            }
            
            return false;
        } catch (JwtException e) {
            LOGGER.warn("Failed to revoke token: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get token statistics
     */
    public TokenStatistics getTokenStatistics() {
        long activeCount = activeTokens.size();
        long expiredCount = activeTokens.values().stream()
            .mapToLong(metadata -> Instant.now().isAfter(metadata.getExpiresAt()) ? 1 : 0)
            .sum();
        
        return new TokenStatistics(activeCount, expiredCount);
    }
    
    /**
     * Cleanup expired tokens
     */
    public void cleanupExpiredTokens() {
        Instant now = Instant.now();
        int removed = 0;
        
        var iterator = activeTokens.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now.isAfter(entry.getValue().getExpiresAt())) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            LOGGER.info("Cleaned up {} expired tokens", removed);
        }
    }
    
    // Inner classes for data structures
    public static class JWTTokenPair {
        private final String accessToken;
        private final String refreshToken;
        private final long expiresIn;
        
        public JWTTokenPair(String accessToken, String refreshToken, long expiresIn) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }
        
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public long getExpiresIn() { return expiresIn; }
    }
    
    public static class AuthenticationResult {
        private final boolean success;
        private final String errorMessage;
        private final AuthenticatedUser user;
        
        private AuthenticationResult(boolean success, String errorMessage, AuthenticatedUser user) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.user = user;
        }
        
        public static AuthenticationResult success(AuthenticatedUser user) {
            return new AuthenticationResult(true, null, user);
        }
        
        public static AuthenticationResult failure(String errorMessage) {
            return new AuthenticationResult(false, errorMessage, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public AuthenticatedUser getUser() { return user; }
    }
    
    public static class AuthenticatedUser {
        private final String username;
        private final AuthenticationManager.UserRole role;
        private final String tokenId;
        private final Claims claims;
        
        public AuthenticatedUser(String username, AuthenticationManager.UserRole role, String tokenId, Claims claims) {
            this.username = username;
            this.role = role;
            this.tokenId = tokenId;
            this.claims = claims;
        }
        
        public String getUsername() { return username; }
        public AuthenticationManager.UserRole getRole() { return role; }
        public String getTokenId() { return tokenId; }
        public Claims getClaims() { return claims; }
    }
    
    private static class TokenMetadata {
        private final String username;
        private final AuthenticationManager.UserRole role;
        private final Instant issuedAt;
        private final Instant expiresAt;
        private volatile Instant lastAccess;
        
        public TokenMetadata(String username, AuthenticationManager.UserRole role, Instant issuedAt, Instant expiresAt) {
            this.username = username;
            this.role = role;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.lastAccess = issuedAt;
        }
        
        public void updateLastAccess() {
            this.lastAccess = Instant.now();
        }
        
        public String getUsername() { return username; }
        public AuthenticationManager.UserRole getRole() { return role; }
        public Instant getIssuedAt() { return issuedAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public Instant getLastAccess() { return lastAccess; }
    }
    
    public static class TokenStatistics {
        private final long activeTokens;
        private final long expiredTokens;
        
        public TokenStatistics(long activeTokens, long expiredTokens) {
            this.activeTokens = activeTokens;
            this.expiredTokens = expiredTokens;
        }
        
        public long getActiveTokens() { return activeTokens; }
        public long getExpiredTokens() { return expiredTokens; }
    }
    
    public static class JWTAuthenticationException extends RuntimeException {
        public JWTAuthenticationException(String message) {
            super(message);
        }
        
        public JWTAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}