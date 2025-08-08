package edu.minecraft.collaboration.security.jwt;

import edu.minecraft.collaboration.security.AuthenticationManager.UserRole;
import edu.minecraft.collaboration.security.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for JWT Authentication Provider
 */
@DisplayName("JWT Authentication Provider Tests")
public class JWTAuthenticationProviderTest {
    
    private JWTAuthenticationProvider jwtProvider;
    
    @Mock
    private SecurityAuditLogger auditLogger;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtProvider = new JWTAuthenticationProvider(auditLogger);
    }
    
    @Test
    @DisplayName("Should generate valid token pair for admin user")
    void testGenerateTokenPairForAdmin() {
        // Given
        String username = "admin_user";
        UserRole role = UserRole.ADMIN;
        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put("server", "test-server");
        
        // When
        JWTAuthenticationProvider.JWTTokenPair tokenPair = 
            jwtProvider.generateTokenPair(username, role, additionalClaims);
        
        // Then
        assertNotNull(tokenPair);
        assertNotNull(tokenPair.getAccessToken());
        assertNotNull(tokenPair.getRefreshToken());
        assertTrue(tokenPair.getExpiresIn() > 0);
        
        // Verify audit logging
        verify(auditLogger).logTokenGeneration(eq(username), eq(role), anyString());
    }
    
    @Test
    @DisplayName("Should generate valid token pair for teacher user")
    void testGenerateTokenPairForTeacher() {
        // Given
        String username = "teacher_user";
        UserRole role = UserRole.TEACHER;
        
        // When
        JWTAuthenticationProvider.JWTTokenPair tokenPair = 
            jwtProvider.generateTokenPair(username, role, new HashMap<>());
        
        // Then
        assertNotNull(tokenPair);
        assertNotNull(tokenPair.getAccessToken());
        assertNotNull(tokenPair.getRefreshToken());
        assertEquals(Duration.ofHours(24).toSeconds(), tokenPair.getExpiresIn());
    }
    
    @Test
    @DisplayName("Should generate valid token pair for student user")
    void testGenerateTokenPairForStudent() {
        // Given
        String username = "student_user";
        UserRole role = UserRole.STUDENT;
        
        // When
        JWTAuthenticationProvider.JWTTokenPair tokenPair = 
            jwtProvider.generateTokenPair(username, role, new HashMap<>());
        
        // Then
        assertNotNull(tokenPair);
        assertNotNull(tokenPair.getAccessToken());
        assertNotNull(tokenPair.getRefreshToken());
    }
    
    @Test
    @DisplayName("Should authenticate valid access token")
    void testAuthenticateValidToken() {
        // Given
        String username = "test_user";
        UserRole role = UserRole.TEACHER;
        JWTAuthenticationProvider.JWTTokenPair tokenPair = 
            jwtProvider.generateTokenPair(username, role, new HashMap<>());
        
        // When
        JWTAuthenticationProvider.AuthenticationResult result = 
            jwtProvider.authenticate(tokenPair.getAccessToken());
        
        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getUser());
        assertEquals(username, result.getUser().getUsername());
        assertEquals(role, result.getUser().getRole());
        
        // Verify audit logging
        verify(auditLogger).logSuccessfulAuthentication(username, role);
    }
    
    @Test
    @DisplayName("Should reject null token")
    void testAuthenticateNullToken() {
        // When
        JWTAuthenticationProvider.AuthenticationResult result = 
            jwtProvider.authenticate(null);
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("Token is required", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should reject empty token")
    void testAuthenticateEmptyToken() {
        // When
        JWTAuthenticationProvider.AuthenticationResult result = 
            jwtProvider.authenticate("  ");
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("Token is required", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Should reject invalid token")
    void testAuthenticateInvalidToken() {
        // When
        JWTAuthenticationProvider.AuthenticationResult result = 
            jwtProvider.authenticate("invalid.token.here");
        
        // Then
        assertFalse(result.isSuccess());
        assertEquals("Invalid token", result.getErrorMessage());
        
        // Verify audit logging
        verify(auditLogger).logAuthenticationFailure(eq("unknown"), contains("Invalid token"));
    }
    
    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshToken() {
        // Given
        String username = "refresh_user";
        UserRole role = UserRole.STUDENT;
        JWTAuthenticationProvider.JWTTokenPair originalPair = 
            jwtProvider.generateTokenPair(username, role, new HashMap<>());
        
        // When
        JWTAuthenticationProvider.JWTTokenPair newPair = null;
        try {
            newPair = jwtProvider.refreshToken(originalPair.getRefreshToken());
        } catch (JWTAuthenticationProvider.JWTAuthenticationException e) {
            fail("Should not throw exception for valid refresh token");
        }
        
        // Then
        assertNotNull(newPair);
        assertNotNull(newPair.getAccessToken());
        assertNotNull(newPair.getRefreshToken());
        assertNotEquals(originalPair.getAccessToken(), newPair.getAccessToken());
        
        // Verify audit logging
        verify(auditLogger).logTokenRefresh(eq(username), anyString());
    }
    
    @Test
    @DisplayName("Should reject refresh with access token")
    void testRefreshTokenWithAccessToken() {
        // Given
        String username = "test_user";
        UserRole role = UserRole.TEACHER;
        JWTAuthenticationProvider.JWTTokenPair tokenPair = 
            jwtProvider.generateTokenPair(username, role, new HashMap<>());
        
        // When & Then
        assertThrows(JWTAuthenticationProvider.JWTAuthenticationException.class, () -> {
            jwtProvider.refreshToken(tokenPair.getAccessToken());
        });
        
        // Verify audit logging
        verify(auditLogger).logTokenRefreshFailure(eq(username), contains("Invalid token type"));
    }
    
    @Test
    @DisplayName("Should revoke token successfully")
    void testRevokeToken() {
        // Given
        String username = "revoke_user";
        UserRole role = UserRole.ADMIN;
        JWTAuthenticationProvider.JWTTokenPair tokenPair = 
            jwtProvider.generateTokenPair(username, role, new HashMap<>());
        
        // When
        boolean revoked = jwtProvider.revokeToken(tokenPair.getAccessToken());
        
        // Then
        assertTrue(revoked);
        
        // Verify token is no longer valid
        JWTAuthenticationProvider.AuthenticationResult result = 
            jwtProvider.authenticate(tokenPair.getAccessToken());
        assertFalse(result.isSuccess());
        assertEquals("Token has been revoked", result.getErrorMessage());
        
        // Verify audit logging
        verify(auditLogger).logTokenRevocation(eq(username), anyString());
    }
    
    @Test
    @DisplayName("Should handle revoke of invalid token gracefully")
    void testRevokeInvalidToken() {
        // When
        boolean revoked = jwtProvider.revokeToken("invalid.token");
        
        // Then
        assertFalse(revoked);
    }
    
    @Test
    @DisplayName("Should cleanup expired tokens")
    void testCleanupExpiredTokens() {
        // Given
        String username1 = "user1";
        String username2 = "user2";
        jwtProvider.generateTokenPair(username1, UserRole.STUDENT, new HashMap<>());
        jwtProvider.generateTokenPair(username2, UserRole.TEACHER, new HashMap<>());
        
        // When
        jwtProvider.cleanupExpiredTokens();
        
        // Then
        // No exception should be thrown
        JWTAuthenticationProvider.TokenStatistics stats = jwtProvider.getTokenStatistics();
        assertNotNull(stats);
        assertTrue(stats.getActiveTokens() >= 0);
    }
    
    @Test
    @DisplayName("Should get token statistics")
    void testGetTokenStatistics() {
        // Given
        jwtProvider.generateTokenPair("user1", UserRole.STUDENT, new HashMap<>());
        jwtProvider.generateTokenPair("user2", UserRole.TEACHER, new HashMap<>());
        jwtProvider.generateTokenPair("user3", UserRole.ADMIN, new HashMap<>());
        
        // When
        JWTAuthenticationProvider.TokenStatistics stats = jwtProvider.getTokenStatistics();
        
        // Then
        assertNotNull(stats);
        assertEquals(3, stats.getActiveTokens());
        assertEquals(0, stats.getExpiredTokens());
    }
    
    @Test
    @DisplayName("Should handle concurrent token generation")
    void testConcurrentTokenGeneration() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String username = "concurrent_user_" + index;
                JWTAuthenticationProvider.JWTTokenPair tokenPair = 
                    jwtProvider.generateTokenPair(username, UserRole.STUDENT, new HashMap<>());
                assertNotNull(tokenPair);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then
        JWTAuthenticationProvider.TokenStatistics stats = jwtProvider.getTokenStatistics();
        assertEquals(threadCount, stats.getActiveTokens());
    }
    
    @Test
    @DisplayName("Should validate token claims correctly")
    void testTokenClaimsValidation() {
        // Given
        String username = "claims_user";
        UserRole role = UserRole.TEACHER;
        Map<String, Object> additionalClaims = new HashMap<>();
        additionalClaims.put("school", "Test School");
        additionalClaims.put("grade", "10");
        
        JWTAuthenticationProvider.JWTTokenPair tokenPair = 
            jwtProvider.generateTokenPair(username, role, additionalClaims);
        
        // When
        JWTAuthenticationProvider.AuthenticationResult result = 
            jwtProvider.authenticate(tokenPair.getAccessToken());
        
        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getUser().getClaims());
        assertEquals("Test School", result.getUser().getClaims().get("school"));
        assertEquals("10", result.getUser().getClaims().get("grade"));
    }
    
    @Test
    @DisplayName("Should handle token generation failure gracefully")
    void testTokenGenerationFailure() {
        // Given
        SecurityAuditLogger failingAuditLogger = mock(SecurityAuditLogger.class);
        doThrow(new RuntimeException("Audit failure")).when(failingAuditLogger)
            .logTokenGeneration(anyString(), any(UserRole.class), anyString());
        
        JWTAuthenticationProvider failingProvider = new JWTAuthenticationProvider(failingAuditLogger);
        
        // When & Then
        assertThrows(JWTAuthenticationProvider.JWTAuthenticationException.class, () -> {
            failingProvider.generateTokenPair("fail_user", UserRole.ADMIN, new HashMap<>());
        });
        
        // Verify error logging
        verify(failingAuditLogger).logTokenGenerationFailure(eq("fail_user"), any(Exception.class));
    }
}