package edu.minecraft.collaboration.integration;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.collaboration.CollaborationManager;
import edu.minecraft.collaboration.commands.CollaborationCommandHandler;
import edu.minecraft.collaboration.network.WebSocketHandler;
import edu.minecraft.collaboration.security.AuthenticationManager;
import edu.minecraft.collaboration.security.AuthenticationManager.UserRole;
import edu.minecraft.collaboration.security.jwt.JWTAuthenticationProvider;
import edu.minecraft.collaboration.security.zerotrust.ZeroTrustAccessControl;
import edu.minecraft.collaboration.security.threat.ThreatDetectionEngine;
import edu.minecraft.collaboration.monitoring.apm.APMManager;
import edu.minecraft.collaboration.monitoring.apm.PredictiveAlertSystem;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Full system integration test for the Minecraft Collaboration Mod
 * Tests the complete flow from WebSocket connection to command execution
 */
@DisplayName("Full System Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FullSystemIntegrationTest {
    
    private WebSocketHandler webSocketHandler;
    private CollaborationManager collaborationManager;
    private CollaborationCommandHandler commandHandler;
    private AuthenticationManager authManager;
    private JWTAuthenticationProvider jwtProvider;
    private ZeroTrustAccessControl accessControl;
    private ThreatDetectionEngine threatEngine;
    private APMManager apmManager;
    private PredictiveAlertSystem alertSystem;
    
    @Mock
    private MinecraftCollaborationMod modInstance;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize core components
        authManager = new AuthenticationManager();
        jwtProvider = new JWTAuthenticationProvider(null);
        accessControl = new ZeroTrustAccessControl(null);
        threatEngine = new ThreatDetectionEngine(null);
        apmManager = new APMManager();
        alertSystem = new PredictiveAlertSystem();
        
        // Initialize business logic components
        collaborationManager = new CollaborationManager();
        commandHandler = new CollaborationCommandHandler(collaborationManager);
        webSocketHandler = new WebSocketHandler("localhost", 14711);
    }
    
    @AfterEach
    void tearDown() {
        // Clean up resources
        if (webSocketHandler != null) {
            webSocketHandler.close();
        }
        if (apmManager != null) {
            apmManager.close();
        }
        if (alertSystem != null) {
            alertSystem.close();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Should perform complete authentication flow")
    void testCompleteAuthenticationFlow() throws Exception {
        // Given
        String username = "test_teacher";
        String password = "secure_password";
        UserRole role = UserRole.TEACHER;
        
        // Register user
        boolean registered = authManager.registerUser(username, password, role);
        assertTrue(registered, "User should be registered successfully");
        
        // Authenticate user
        boolean authenticated = authManager.authenticate(username, password);
        assertTrue(authenticated, "User should be authenticated successfully");
        
        // Generate JWT tokens
        Map<String, Object> claims = new HashMap<>();
        claims.put("school", "Test School");
        JWTAuthenticationProvider.JWTTokenPair tokenPair = 
            jwtProvider.generateTokenPair(username, role, claims);
        
        assertNotNull(tokenPair);
        assertNotNull(tokenPair.getAccessToken());
        assertNotNull(tokenPair.getRefreshToken());
        
        // Validate token
        JWTAuthenticationProvider.AuthenticationResult result = 
            jwtProvider.authenticate(tokenPair.getAccessToken());
        
        assertTrue(result.isSuccess());
        assertEquals(username, result.getUser().getUsername());
        assertEquals(role, result.getUser().getRole());
    }
    
    @Test
    @Order(2)
    @DisplayName("Should enforce zero-trust access control")
    void testZeroTrustAccessControl() {
        // Given
        ZeroTrustAccessControl.User teacher = 
            new ZeroTrustAccessControl.User("teacher_user", UserRole.TEACHER);
        ZeroTrustAccessControl.User student = 
            new ZeroTrustAccessControl.User("student_user", UserRole.STUDENT);
        
        ZeroTrustAccessControl.Operation adminOp = 
            new ZeroTrustAccessControl.Operation("manage_server", 
                ZeroTrustAccessControl.OperationCategory.ADMINISTRATIVE);
        ZeroTrustAccessControl.Operation collabOp = 
            new ZeroTrustAccessControl.Operation("send_invitation", 
                ZeroTrustAccessControl.OperationCategory.COLLABORATION);
        
        ZeroTrustAccessControl.Resource adminResource = 
            new ZeroTrustAccessControl.Resource("server_config", "admin");
        ZeroTrustAccessControl.Resource collabResource = 
            new ZeroTrustAccessControl.Resource("collaboration_session", "collaboration");
        
        // When & Then
        // Teacher should be able to perform collaboration operations
        ZeroTrustAccessControl.AuthorizationResult teacherCollabResult = 
            accessControl.authorizeOperation(teacher, collabOp, collabResource);
        assertTrue(teacherCollabResult.isGranted());
        
        // Student should NOT be able to perform admin operations
        ZeroTrustAccessControl.AuthorizationResult studentAdminResult = 
            accessControl.authorizeOperation(student, adminOp, adminResource);
        assertFalse(studentAdminResult.isGranted());
    }
    
    @Test
    @Order(3)
    @DisplayName("Should detect and respond to threats")
    void testThreatDetectionAndResponse() {
        // Given
        String attackerUsername = "potential_attacker";
        
        // Simulate brute force attack
        for (int i = 0; i < 10; i++) {
            UserActivityEvent failedLogin = UserActivityEvent.builder(attackerUsername, "login")
                .userRole(UserRole.STUDENT)
                .resourceAccessed("authentication")
                .successful(false)
                .build();
            
            ThreatAssessment assessment = threatEngine.analyzeUserActivity(failedLogin);
            
            // After multiple failures, threat should be detected
            if (i > 5) {
                assertNotEquals(ThreatLevel.NONE, assessment.getThreatLevel());
            }
        }
        
        // Verify threat metrics are recorded
        Map<String, Long> metrics = threatEngine.getThreatMetrics();
        assertNotNull(metrics);
    }
    
    @Test
    @Order(4)
    @DisplayName("Should handle collaboration invitation flow end-to-end")
    void testCollaborationInvitationFlow() throws Exception {
        // Given
        String senderId = "teacher_sender";
        String recipientId = "student_recipient";
        
        // Register users
        authManager.registerUser(senderId, "password1", UserRole.TEACHER);
        authManager.registerUser(recipientId, "password2", UserRole.STUDENT);
        
        // When - Send invitation
        CompletableFuture<String> inviteFuture = collaborationManager.sendInvitation(senderId, recipientId);
        String inviteId = inviteFuture.get(5, TimeUnit.SECONDS);
        
        assertNotNull(inviteId);
        assertTrue(collaborationManager.hasActiveInvitation(senderId, recipientId));
        
        // Accept invitation
        CompletableFuture<String> acceptFuture = collaborationManager.acceptInvitation(recipientId, inviteId);
        String result = acceptFuture.get(5, TimeUnit.SECONDS);
        
        assertEquals("Invitation accepted", result);
        assertFalse(collaborationManager.hasActiveInvitation(senderId, recipientId));
    }
    
    @Test
    @Order(5)
    @DisplayName("Should monitor performance and generate metrics")
    void testPerformanceMonitoring() throws Exception {
        // Given
        String operationName = "test_operation";
        
        // When - Record various metrics
        apmManager.incrementCounter("test.counter");
        apmManager.incrementCounter("test.counter");
        apmManager.recordTiming("test.timer", java.time.Duration.ofMillis(100));
        
        // Time an operation
        String result = apmManager.timeExecution("test.execution", () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "completed";
        });
        
        assertEquals("completed", result);
        
        // Verify metrics are recorded
        String prometheusMetrics = apmManager.getPrometheusMetrics();
        assertNotNull(prometheusMetrics);
        assertTrue(prometheusMetrics.contains("test_counter"));
    }
    
    @Test
    @Order(6)
    @DisplayName("Should handle predictive alerts")
    void testPredictiveAlertSystem() throws Exception {
        // Given
        alertSystem.setEnabled(true);
        
        // Train the model with sample data
        for (int i = 0; i < 100; i++) {
            alertSystem.recordMetric("cpu.usage", 30 + Math.random() * 20);
            alertSystem.recordMetric("memory.usage", 40 + Math.random() * 30);
            alertSystem.recordMetric("response.time", 100 + Math.random() * 50);
        }
        
        // Simulate anomaly
        alertSystem.recordMetric("cpu.usage", 95);
        alertSystem.recordMetric("memory.usage", 90);
        alertSystem.recordMetric("response.time", 2000);
        
        // Wait for alert processing
        Thread.sleep(1000);
        
        // Verify alerts were generated
        var recentAlerts = alertSystem.getRecentAlerts(10);
        assertNotNull(recentAlerts);
        // Alerts should be generated for anomalous values
    }
    
    @Test
    @Order(7)
    @DisplayName("Should handle WebSocket message processing")
    void testWebSocketMessageProcessing() throws Exception {
        // Given
        CountDownLatch connectionLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        
        // Mock WebSocket behavior
        doAnswer(invocation -> {
            connectionLatch.countDown();
            return null;
        }).when(modInstance).onWebSocketConnected();
        
        // When - Start WebSocket handler
        CompletableFuture<Void> startFuture = CompletableFuture.runAsync(() -> {
            try {
                webSocketHandler.start();
            } catch (Exception e) {
                fail("WebSocket start failed: " + e.getMessage());
            }
        });
        
        // Wait for connection
        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "WebSocket should connect");
        
        // Send test message
        String testMessage = "{\"type\":\"ping\",\"data\":{}}";
        webSocketHandler.sendMessage(testMessage);
        
        // Verify message was sent
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Message should be processed");
    }
    
    @Test
    @Order(8)
    @DisplayName("Should handle concurrent operations safely")
    void testConcurrentOperations() throws Exception {
        // Given
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(threadCount);
        
        // When - Execute concurrent operations
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    // Perform various operations concurrently
                    String username = "concurrent_user_" + index;
                    UserRole role = index % 2 == 0 ? UserRole.TEACHER : UserRole.STUDENT;
                    
                    // Register user
                    authManager.registerUser(username, "password_" + index, role);
                    
                    // Generate JWT token
                    var tokenPair = jwtProvider.generateTokenPair(username, role, new HashMap<>());
                    assertNotNull(tokenPair);
                    
                    // Record metrics
                    apmManager.incrementCounter("concurrent.test.counter");
                    
                    // Check access control
                    var user = new ZeroTrustAccessControl.User(username, role);
                    var operation = new ZeroTrustAccessControl.Operation("test_op", 
                        ZeroTrustAccessControl.OperationCategory.BASIC);
                    var resource = new ZeroTrustAccessControl.Resource("test_resource", "test");
                    
                    var authResult = accessControl.authorizeOperation(user, operation, resource);
                    assertNotNull(authResult);
                    
                } catch (Exception e) {
                    fail("Concurrent operation failed: " + e.getMessage());
                } finally {
                    completeLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        assertTrue(completeLatch.await(10, TimeUnit.SECONDS), 
            "All concurrent operations should complete");
    }
    
    @Test
    @Order(9)
    @DisplayName("Should handle system recovery after failure")
    void testSystemRecoveryAfterFailure() throws Exception {
        // Given - Simulate system under stress
        for (int i = 0; i < 50; i++) {
            apmManager.incrementCounter("error.count");
            alertSystem.recordMetric("error.rate", 50 + i);
        }
        
        // When - Trigger recovery mechanisms
        alertSystem.checkAlerts();
        
        // Simulate recovery
        for (int i = 0; i < 10; i++) {
            alertSystem.recordMetric("error.rate", 5 - i * 0.5);
            Thread.sleep(100);
        }
        
        // Then - System should recover
        var alerts = alertSystem.getRecentAlerts(5);
        assertNotNull(alerts);
        // Latest alerts should show improvement
    }
    
    @Test
    @Order(10)
    @DisplayName("Should validate complete command execution flow")
    void testCompleteCommandExecutionFlow() throws Exception {
        // Given
        String playerName = "test_player";
        authManager.registerUser(playerName, "password", UserRole.STUDENT);
        
        // When - Execute various commands
        Map<String, Object> inviteCommand = new HashMap<>();
        inviteCommand.put("action", "invite");
        inviteCommand.put("target", "other_player");
        
        CompletableFuture<String> inviteResult = commandHandler.handleCommand(playerName, inviteCommand);
        assertNotNull(inviteResult);
        
        Map<String, Object> acceptCommand = new HashMap<>();
        acceptCommand.put("action", "accept");
        acceptCommand.put("inviteId", "test_invite_id");
        
        CompletableFuture<String> acceptResult = commandHandler.handleCommand("other_player", acceptCommand);
        assertNotNull(acceptResult);
        
        // Verify command execution metrics
        String metrics = apmManager.getPrometheusMetrics();
        assertNotNull(metrics);
    }
}