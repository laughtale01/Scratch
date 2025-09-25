package edu.minecraft.collaboration.security.jwt;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.security.SecurityAuditLogger;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
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
    private final Duration tokenValidityDuration;

    public JWTAuthenticationProvider(SecurityAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
        this.secretKey = generateSecretKey();
        this.tokenValidityDuration = TOKEN_VALIDITY;

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
     * Generate a new refresh token
     */
    public String generateRefreshToken(String username, String role) {
        try {
            String tokenId = UUID.randomUUID().toString();
            Instant expiry = Instant.now().plus(REFRESH_TOKEN_VALIDITY);

            // Create simple refresh token payload
            String payload = String.format("%s|%s|%s|%s|%d|REFRESH",
                tokenId, username, role, ISSUER, expiry.toEpochMilli());

            // Encode token (simplified - in production use proper JWT library)
            String token = Base64.getEncoder().encodeToString(payload.getBytes());

            // Store token metadata
            activeTokens.put(tokenId, new TokenMetadata(username, role, expiry, TokenType.REFRESH));

            LOGGER.info("Generated refresh token for user: {}", username);
            return token;
        } catch (Exception e) {
            LOGGER.error("Failed to generate refresh token for user: {}", username, e);
            return null;
        }
    }

    /**
     * Validate a token
     */
    public TokenValidation validateToken(String token) {
        try {
            // Decode token
            String payload;
            try {
                payload = new String(Base64.getDecoder().decode(token));
            } catch (IllegalArgumentException e) {
                // Handle invalid Base64 input
                auditLogger.logTokenValidation("invalid", false);
                return TokenValidation.invalid("Invalid token");
            }

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

    /**
     * Authentication result for test compatibility
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final String username;
        private final String role;
        private final String token;
        private final String error;

        private AuthenticationResult(boolean success, String username, String role, String token, String error) {
            this.success = success;
            this.username = username;
            this.role = role;
            this.token = token;
            this.error = error;
        }

        public static AuthenticationResult success(String username, String role, String token) {
            return new AuthenticationResult(true, username, role, token, null);
        }

        public static AuthenticationResult failure(String error) {
            return new AuthenticationResult(false, null, null, null, error);
        }

        public boolean isSuccess() { return success; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public String getToken() { return token; }
        public String getError() { return error; }
        public String getErrorMessage() { return error; }

        public UserInfo getUser() {
            if (success && username != null && role != null) {
                return new UserInfo(username, role);
            }
            return null;
        }
    }

    /**
     * Authenticate using token (for test compatibility)
     */
    public AuthenticationResult authenticate(String token) {
        if (token == null || token.trim().isEmpty()) {
            return AuthenticationResult.failure("Token is null or empty");
        }

        TokenValidation validation = validateToken(token);
        if (validation.isValid()) {
            return AuthenticationResult.success(validation.getUsername(), validation.getRole(), token);
        } else {
            return AuthenticationResult.failure(validation.getError());
        }
    }

    /**
     * Generate token pair (for test compatibility)
     */
    public JWTTokenPair generateTokenPair(String username, edu.minecraft.collaboration.security.AuthenticationManager.UserRole role, Map<String, Object> claims) {
        String accessToken = generateToken(username, role.name());
        String refreshToken = generateRefreshToken(username, role.name());

        if (auditLogger != null) {
            auditLogger.logSuccessfulAuthentication(username, role);
        }

        // Calculate expires in based on token validity duration
        long expiresIn = tokenValidityDuration.getSeconds();
        return new JWTTokenPair(accessToken, refreshToken, expiresIn);
    }

    /**
     * Token pair for access and refresh tokens
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }

    /**
     * User information class
     */
    public static class UserInfo {
        private final String username;
        private final String role;
        private final Map<String, Object> claims;

        public UserInfo(String username, String role) {
            this.username = username;
            this.role = role;
            this.claims = new HashMap<>();
        }

        public UserInfo(String username, String role, Map<String, Object> claims) {
            this.username = username;
            this.role = role;
            this.claims = claims != null ? new HashMap<>(claims) : new HashMap<>();
        }

        public String getUsername() { return username; }
        public String getRole() { return role; }
        public Map<String, Object> getClaims() { return claims; }
    }

    /**
     * Alias for TokenPair to maintain backward compatibility
     */
    public static class JWTTokenPair extends TokenPair {
        private final long expiresIn;

        public JWTTokenPair(String accessToken, String refreshToken) {
            super(accessToken, refreshToken);
            this.expiresIn = 3600; // Default 1 hour in seconds
        }

        public JWTTokenPair(String accessToken, String refreshToken, long expiresIn) {
            super(accessToken, refreshToken);
            this.expiresIn = expiresIn;
        }

        public long getExpiresIn() {
            return expiresIn;
        }
    }

    /**
     * Refresh a token
     */
    public JWTTokenPair refreshToken(String refreshToken) throws JWTAuthenticationException {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new JWTAuthenticationException("Refresh token is null or empty");
        }

        TokenValidation validation = validateToken(refreshToken);
        if (!validation.isValid()) {
            if (auditLogger != null) {
                auditLogger.logTokenRefreshFailure(validation.getUsername(), validation.getError());
            }
            throw new JWTAuthenticationException("Invalid refresh token: " + validation.getError());
        }

        // Generate new token pair
        String username = validation.getUsername();
        String role = validation.getRole();
        String newAccessToken = generateToken(username, role);
        String newRefreshToken = generateRefreshToken(username, role);

        if (auditLogger != null) {
            auditLogger.logTokenRefresh(username, "refresh_success");
        }

        long expiresIn = tokenValidityDuration.getSeconds();
        return new JWTTokenPair(newAccessToken, newRefreshToken, expiresIn);
    }

    /**
     * Revoke a token and return success status
     */
    public boolean revokeToken(String token) {
        try {
            TokenValidation validation = validateToken(token);
            if (validation.isValid()) {
                String payload = new String(Base64.getDecoder().decode(token));
                String[] parts = payload.split("\\|");
                if (parts.length >= 1) {
                    String tokenId = parts[0];
                    activeTokens.remove(tokenId);

                    if (auditLogger != null) {
                        auditLogger.logTokenRevocation(validation.getUsername(), tokenId);
                    }

                    LOGGER.info("Token revoked successfully: {}", tokenId);
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error revoking token", e);
        }
        return false;
    }

    /**
     * Get token statistics
     */
    public TokenStatistics getTokenStatistics() {
        long currentTime = System.currentTimeMillis();
        int totalTokens = activeTokens.size();
        int expiredTokens = 0;
        int accessTokens = 0;
        int refreshTokens = 0;

        for (TokenMetadata metadata : activeTokens.values()) {
            if (metadata.expiry.toEpochMilli() < currentTime) {
                expiredTokens++;
            }

            if (metadata.type == TokenType.ACCESS) {
                accessTokens++;
            } else if (metadata.type == TokenType.REFRESH) {
                refreshTokens++;
            }
        }

        return new TokenStatistics(totalTokens, expiredTokens, accessTokens, refreshTokens);
    }

    /**
     * JWT Authentication Exception
     */
    public static class JWTAuthenticationException extends Exception {
        public JWTAuthenticationException(String message) {
            super(message);
        }

        public JWTAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Token statistics class
     */
    public static class TokenStatistics {
        private final int totalTokens;
        private final int expiredTokens;
        private final int accessTokens;
        private final int refreshTokens;

        public TokenStatistics(int totalTokens, int expiredTokens, int accessTokens, int refreshTokens) {
            this.totalTokens = totalTokens;
            this.expiredTokens = expiredTokens;
            this.accessTokens = accessTokens;
            this.refreshTokens = refreshTokens;
        }

        public int getTotalTokens() { return totalTokens; }
        public int getExpiredTokens() { return expiredTokens; }
        public int getAccessTokens() { return accessTokens; }
        public int getRefreshTokens() { return refreshTokens; }
        public int getActiveTokens() { return totalTokens - expiredTokens; }
    }
}
