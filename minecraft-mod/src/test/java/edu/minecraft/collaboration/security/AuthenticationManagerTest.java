package edu.minecraft.collaboration.security;

import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.test.base.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for AuthenticationManager
 */
@DisplayName("AuthenticationManager Tests")
public class AuthenticationManagerTest extends BaseUnitTest {
    
    private AuthenticationManager authManager;
    
    @BeforeEach
    void setUp() {
        authManager = DependencyInjector.getInstance().getService(AuthenticationManager.class);
        authManager.clearAllForTesting();
    }
    
    @Test
    @DisplayName("Should get singleton instance")
    void testSingletonInstance() {
        // When
        DependencyInjector injector = DependencyInjector.getInstance();
        AuthenticationManager instance1 = injector.getService(AuthenticationManager.class);
        AuthenticationManager instance2 = injector.getService(AuthenticationManager.class);
        
        // Then
        assertSame(instance1, instance2);
    }
    
    @Test
    @DisplayName("Should generate token for user")
    void testGenerateToken() {
        // Given
        String username = "testuser";
        AuthenticationManager.UserRole role = AuthenticationManager.UserRole.STUDENT;
        
        // When
        String token = authManager.generateToken(username, role);
        
        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
    
    @Test
    @DisplayName("Should validate generated token")
    void testValidateToken() {
        // Given
        String username = "testuser";
        AuthenticationManager.UserRole role = AuthenticationManager.UserRole.STUDENT;
        String token = authManager.generateToken(username, role);
        
        // When
        boolean result = authManager.validateToken(token);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should reject null token")
    void testValidateNullToken() {
        // When
        boolean result = authManager.validateToken(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should reject empty token")
    void testValidateEmptyToken() {
        // When
        boolean result = authManager.validateToken("");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should authenticate connection with valid token")
    void testAuthenticateConnection() {
        // Given
        String username = "testuser";
        String connectionId = "conn123";
        AuthenticationManager.UserRole role = AuthenticationManager.UserRole.STUDENT;
        String token = authManager.generateToken(username, role);
        
        // When
        boolean result = authManager.authenticateConnection(connectionId, token);
        
        // Then
        assertTrue(result);
        assertTrue(authManager.isAuthenticated(connectionId));
    }
    
    @Test
    @DisplayName("Should reject connection with invalid token")
    void testAuthenticateConnectionInvalidToken() {
        // Given
        String connectionId = "conn123";
        String invalidToken = "invalid_token";
        
        // When
        boolean result = authManager.authenticateConnection(connectionId, invalidToken);
        
        // Then
        assertFalse(result);
        assertFalse(authManager.isAuthenticated(connectionId));
    }
    
    @Test
    @DisplayName("Should check if connection is authenticated")
    void testIsAuthenticated() {
        // Given
        String username = "testuser";
        String connectionId = "conn123";
        AuthenticationManager.UserRole role = AuthenticationManager.UserRole.STUDENT;
        String token = authManager.generateToken(username, role);
        authManager.authenticateConnection(connectionId, token);
        
        // When
        boolean result = authManager.isAuthenticated(connectionId);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should return false for non-authenticated connection")
    void testIsNotAuthenticated() {
        // When
        boolean result = authManager.isAuthenticated("nonexistent");
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should get username for authenticated connection")
    void testGetUsername() {
        // Given
        String username = "testuser";
        String connectionId = "conn123";
        AuthenticationManager.UserRole role = AuthenticationManager.UserRole.STUDENT;
        String token = authManager.generateToken(username, role);
        authManager.authenticateConnection(connectionId, token);
        
        // When
        String retrievedUsername = authManager.getUsername(connectionId);
        
        // Then
        assertEquals(username, retrievedUsername);
    }
    
    @Test
    @DisplayName("Should return null for non-authenticated connection")
    void testGetUsernameNonAuthenticated() {
        // When
        String username = authManager.getUsername("nonexistent");
        
        // Then
        assertNull(username);
    }
    
    @Test
    @DisplayName("Should get role for token")
    void testGetRoleForToken() {
        // Given
        String username = "testuser";
        AuthenticationManager.UserRole expectedRole = AuthenticationManager.UserRole.TEACHER;
        String token = authManager.generateToken(username, expectedRole);
        
        // When
        AuthenticationManager.UserRole actualRole = authManager.getRole(token);
        
        // Then
        assertEquals(expectedRole, actualRole);
    }
    
    @Test
    @DisplayName("Should return STUDENT role for invalid token")
    void testGetRoleForInvalidToken() {
        // When
        AuthenticationManager.UserRole role = authManager.getRole("invalid_token");
        
        // Then
        assertEquals(AuthenticationManager.UserRole.STUDENT, role);
    }
    
    @Test
    @DisplayName("Should get role for connection")
    void testGetRoleForConnection() {
        // Given
        String username = "testuser";
        String connectionId = "conn123";
        AuthenticationManager.UserRole expectedRole = AuthenticationManager.UserRole.TEACHER;
        String token = authManager.generateToken(username, expectedRole);
        authManager.authenticateConnection(connectionId, token);
        
        // When
        AuthenticationManager.UserRole actualRole = authManager.getRoleForConnection(connectionId);
        
        // Then
        assertEquals(expectedRole, actualRole);
    }
    
    @Test
    @DisplayName("Should revoke token")
    void testRevokeToken() {
        // Given
        String username = "testuser";
        String connectionId = "conn123";
        AuthenticationManager.UserRole role = AuthenticationManager.UserRole.STUDENT;
        String token = authManager.generateToken(username, role);
        authManager.authenticateConnection(connectionId, token);
        
        // Verify token is valid
        assertTrue(authManager.validateToken(token));
        assertTrue(authManager.isAuthenticated(connectionId));
        
        // When
        authManager.revokeToken(token);
        
        // Then
        assertFalse(authManager.validateToken(token));
        assertFalse(authManager.isAuthenticated(connectionId));
    }
    
    @Test
    @DisplayName("Should remove connection")
    void testRemoveConnection() {
        // Given
        String username = "testuser";
        String connectionId = "conn123";
        AuthenticationManager.UserRole role = AuthenticationManager.UserRole.STUDENT;
        String token = authManager.generateToken(username, role);
        authManager.authenticateConnection(connectionId, token);
        
        // Verify connection is authenticated
        assertTrue(authManager.isAuthenticated(connectionId));
        
        // When
        authManager.removeConnection(connectionId);
        
        // Then
        assertFalse(authManager.isAuthenticated(connectionId));
    }
    
    @Test
    @DisplayName("Should add teacher token")
    void testAddTeacherToken() {
        // Given
        String teacherUsername = "teacher123";
        
        // When
        String teacherToken = authManager.addTeacherToken(teacherUsername);
        
        // Then
        assertNotNull(teacherToken);
        assertTrue(authManager.validateToken(teacherToken));
        assertEquals(AuthenticationManager.UserRole.TEACHER, authManager.getRole(teacherToken));
    }
    
    @Test
    @DisplayName("Should check elevated privileges")
    void testHasElevatedPrivileges() {
        // Given
        String adminUser = "admin";
        String regularUser = "user";
        String adminConnectionId = "admin_conn";
        String userConnectionId = "user_conn";
        
        String adminToken = authManager.generateToken(adminUser, AuthenticationManager.UserRole.ADMIN);
        String userToken = authManager.generateToken(regularUser, AuthenticationManager.UserRole.STUDENT);
        
        // When
        authManager.authenticateConnection(adminConnectionId, adminToken);
        authManager.authenticateConnection(userConnectionId, userToken);
        
        // Then
        assertTrue(authManager.hasElevatedPrivileges(adminConnectionId));
        assertFalse(authManager.hasElevatedPrivileges(userConnectionId));
    }
    
    @Test
    @DisplayName("Should get authentication statistics")
    void testGetAuthenticationStatistics() {
        // Given
        String user1 = "user1";
        String user2 = "user2";
        String conn1 = "conn1";
        String conn2 = "conn2";
        
        String token1 = authManager.generateToken(user1, AuthenticationManager.UserRole.STUDENT);
        String token2 = authManager.generateToken(user2, AuthenticationManager.UserRole.TEACHER);
        
        // When
        authManager.authenticateConnection(conn1, token1);
        authManager.authenticateConnection(conn2, token2);
        
        var stats = authManager.getStatistics();
        
        // Then
        assertNotNull(stats);
        assertTrue(stats.containsKey("activeTokens"));
        assertTrue(stats.containsKey("authenticatedConnections"));
        assertTrue(stats.get("activeTokens") >= 2);
        assertTrue(stats.get("authenticatedConnections") >= 2);
    }
    
    @Test
    @DisplayName("Should handle concurrent token generation")
    void testConcurrentTokenGeneration() {
        // Test thread safety
        assertDoesNotThrow(() -> {
            // Create multiple threads for token generation
            for (int i = 0; i < 10; i++) {
                final int threadId = i;
                Thread thread = new Thread(() -> {
                    String username = "user" + threadId;
                    AuthenticationManager.UserRole role = AuthenticationManager.UserRole.STUDENT;
                    String token = authManager.generateToken(username, role);
                    assertNotNull(token);
                });
                thread.start();
                thread.join(100); // Short timeout
            }
        });
    }
    
    @Test
    @DisplayName("Should handle null connection authentication")
    void testAuthenticateNullConnection() {
        // When
        boolean result = authManager.isAuthenticated(null);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should handle role-based access")
    void testRoleBasedAccess() {
        // Given
        String adminUser = "admin";
        String regularUser = "user";
        String adminConnectionId = "admin_conn";
        String userConnectionId = "user_conn";
        
        String adminToken = authManager.generateToken(adminUser, AuthenticationManager.UserRole.ADMIN);
        String userToken = authManager.generateToken(regularUser, AuthenticationManager.UserRole.STUDENT);
        
        // When
        authManager.authenticateConnection(adminConnectionId, adminToken);
        authManager.authenticateConnection(userConnectionId, userToken);
        
        // Then
        assertTrue(authManager.hasElevatedPrivileges(adminConnectionId));
        assertFalse(authManager.hasElevatedPrivileges(userConnectionId));
    }
}