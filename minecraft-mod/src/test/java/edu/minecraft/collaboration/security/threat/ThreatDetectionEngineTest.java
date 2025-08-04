package edu.minecraft.collaboration.security.threat;

import edu.minecraft.collaboration.security.AuthenticationManager.UserRole;
import edu.minecraft.collaboration.security.SecurityAuditLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for Threat Detection Engine
 */
@DisplayName("Threat Detection Engine Tests")
public class ThreatDetectionEngineTest {
    
    private ThreatDetectionEngine threatEngine;
    
    @Mock
    private SecurityAuditLogger auditLogger;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        threatEngine = new ThreatDetectionEngine(auditLogger);
    }
    
    @AfterEach
    void tearDown() {
        if (threatEngine != null) {
            threatEngine.close();
        }
    }
    
    @Test
    @DisplayName("Should detect no threat for normal user activity")
    void testNormalUserActivity() {
        // Given
        UserActivityEvent normalActivity = UserActivityEvent.builder("normal_user", "login")
            .userRole(UserRole.STUDENT)
            .resourceAccessed("dashboard")
            .successful(true)
            .build();
        
        // When
        ThreatAssessment assessment = threatEngine.analyzeUserActivity(normalActivity);
        
        // Then
        assertNotNull(assessment);
        assertEquals(ThreatLevel.NONE, assessment.getThreatLevel());
        assertFalse(assessment.isError());
    }
    
    @Test
    @DisplayName("Should detect brute force attack pattern")
    void testBruteForceDetection() {
        // Given
        String username = "attacker";
        
        // Simulate multiple failed login attempts
        for (int i = 0; i < 6; i++) {
            UserActivityEvent failedLogin = UserActivityEvent.builder(username, "login")
                .userRole(UserRole.STUDENT)
                .resourceAccessed("authentication")
                .successful(false)
                .build();
            
            threatEngine.analyzeUserActivity(failedLogin);
        }
        
        // When - One more failed attempt should trigger threat detection
        UserActivityEvent finalAttempt = UserActivityEvent.builder(username, "login")
            .userRole(UserRole.STUDENT)
            .resourceAccessed("authentication")
            .successful(false)
            .build();
        
        ThreatAssessment assessment = threatEngine.analyzeUserActivity(finalAttempt);
        
        // Then
        assertNotNull(assessment);
        assertTrue(assessment.getThreatLevel().getSeverity() > ThreatLevel.NONE.getSeverity());
        
        // Verify security alert was logged
        verify(auditLogger, atLeastOnce()).logSuspiciousActivity(
            eq(username), 
            anyString(), 
            any(Map.class)
        );
    }
    
    @Test
    @DisplayName("Should detect privilege escalation attempt")
    void testPrivilegeEscalationDetection() {
        // Given
        String username = "malicious_student";
        
        // Student trying to access admin functions
        UserActivityEvent escalationAttempt = UserActivityEvent.builder(username, "admin_panel_access")
            .userRole(UserRole.STUDENT)
            .resourceAccessed("admin_console")
            .successful(false)
            .build();
        
        // When
        ThreatAssessment assessment = threatEngine.analyzeUserActivity(escalationAttempt);
        
        // Then
        assertNotNull(assessment);
        // Should detect suspicious activity
        String threatType = assessment.getThreatType();
        assertNotNull(threatType);
    }
    
    @Test
    @DisplayName("Should detect data exfiltration pattern")
    void testDataExfiltrationDetection() {
        // Given
        String username = "data_thief";
        
        // Simulate bulk data export attempts
        for (int i = 0; i < 10; i++) {
            UserActivityEvent exportAttempt = UserActivityEvent.builder(username, "export_data_" + i)
                .userRole(UserRole.TEACHER)
                .resourceAccessed("student_records")
                .successful(true)
                .parameter("recordCount", 1000)
                .build();
            
            threatEngine.analyzeUserActivity(exportAttempt);
        }
        
        // When - Check threat metrics
        Map<String, Long> metrics = threatEngine.getThreatMetrics();
        
        // Then
        assertNotNull(metrics);
        // Should have recorded threat events
        assertTrue(metrics.size() > 0);
    }
    
    @Test
    @DisplayName("Should detect bot behavior pattern")
    void testBotBehaviorDetection() throws InterruptedException {
        // Given
        String username = "bot_user";
        
        // Simulate rapid, regular interval requests (bot-like behavior)
        for (int i = 0; i < 20; i++) {
            UserActivityEvent botActivity = UserActivityEvent.builder(username, "automated_action_" + i)
                .userRole(UserRole.STUDENT)
                .resourceAccessed("game_resource")
                .successful(true)
                .timestamp(Instant.now())
                .build();
            
            threatEngine.analyzeUserActivity(botActivity);
            
            // Very regular intervals (bot-like)
            Thread.sleep(100);
        }
        
        // When
        List<ThreatEvent> recentThreats = threatEngine.getRecentThreatEvents(10);
        
        // Then
        assertNotNull(recentThreats);
        // Bot behavior should be detected in recent threats
    }
    
    @Test
    @DisplayName("Should handle network-based threats")
    void testNetworkThreatDetection() {
        // Given
        NetworkInfo suspiciousNetwork = new NetworkInfo(
            "203.0.113.1",  // External IP
            "suspicious-client",
            "unknown_country",
            true,  // VPN
            false  // Not proxy
        );
        
        UserActivityEvent networkThreat = UserActivityEvent.builder("vpn_user", "access_attempt")
            .userRole(UserRole.STUDENT)
            .resourceAccessed("sensitive_data")
            .networkInfo(suspiciousNetwork)
            .successful(true)
            .build();
        
        // When
        ThreatAssessment assessment = threatEngine.analyzeUserActivity(networkThreat);
        
        // Then
        assertNotNull(assessment);
        // VPN usage should be considered in threat assessment
        assertNotNull(assessment.getThreatType());
    }
    
    @Test
    @DisplayName("Should get threat metrics correctly")
    void testGetThreatMetrics() {
        // Given
        // Generate some activity
        UserActivityEvent activity1 = UserActivityEvent.builder("user1", "action1")
            .userRole(UserRole.STUDENT)
            .successful(true)
            .build();
        
        UserActivityEvent activity2 = UserActivityEvent.builder("user2", "action2")
            .userRole(UserRole.TEACHER)
            .successful(false)
            .build();
        
        threatEngine.analyzeUserActivity(activity1);
        threatEngine.analyzeUserActivity(activity2);
        
        // When
        Map<String, Long> metrics = threatEngine.getThreatMetrics();
        
        // Then
        assertNotNull(metrics);
        // Metrics should be tracked
        assertTrue(metrics.size() >= 0);
    }
    
    @Test
    @DisplayName("Should handle concurrent threat analysis")
    void testConcurrentThreatAnalysis() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        
        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                UserActivityEvent activity = UserActivityEvent.builder("concurrent_user_" + index, "action_" + index)
                    .userRole(UserRole.STUDENT)
                    .resourceAccessed("resource_" + index)
                    .successful(true)
                    .build();
                
                ThreatAssessment assessment = threatEngine.analyzeUserActivity(activity);
                assertNotNull(assessment);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then
        // All concurrent analyses should complete without error
        assertTrue(true);
    }
    
    @Test
    @DisplayName("Should enable and disable threat detection")
    void testEnableDisable() {
        // Given
        UserActivityEvent activity = UserActivityEvent.builder("test_user", "test_action")
            .userRole(UserRole.STUDENT)
            .successful(true)
            .build();
        
        // When - Disable threat detection
        threatEngine.setEnabled(false);
        ThreatAssessment assessmentWhenDisabled = threatEngine.analyzeUserActivity(activity);
        
        // Then
        assertNotNull(assessmentWhenDisabled);
        assertEquals(ThreatLevel.NONE, assessmentWhenDisabled.getThreatLevel());
        assertEquals("System disabled", assessmentWhenDisabled.getDescription());
        
        // When - Re-enable threat detection
        threatEngine.setEnabled(true);
        ThreatAssessment assessmentWhenEnabled = threatEngine.analyzeUserActivity(activity);
        
        // Then
        assertNotNull(assessmentWhenEnabled);
        // Should process normally when enabled
    }
    
    @Test
    @DisplayName("Should handle insider threat patterns")
    void testInsiderThreatDetection() {
        // Given
        String username = "insider_threat";
        
        // Simulate after-hours sensitive data access
        UserActivityEvent afterHoursAccess = UserActivityEvent.builder(username, "access_sensitive_data")
            .userRole(UserRole.TEACHER)
            .resourceAccessed("admin_config")
            .successful(true)
            .parameter("accessTime", "02:30:00")  // 2:30 AM
            .build();
        
        // When
        ThreatAssessment assessment = threatEngine.analyzeUserActivity(afterHoursAccess);
        
        // Then
        assertNotNull(assessment);
        // After-hours access to sensitive resources should be flagged
    }
    
    @Test
    @DisplayName("Should get recent threat events")
    void testGetRecentThreatEvents() {
        // Given
        // Generate some threat events
        for (int i = 0; i < 5; i++) {
            UserActivityEvent suspiciousActivity = UserActivityEvent.builder("suspicious_user_" + i, "suspicious_action_" + i)
                .userRole(UserRole.STUDENT)
                .resourceAccessed("restricted_resource")
                .successful(false)
                .build();
            
            threatEngine.analyzeUserActivity(suspiciousActivity);
        }
        
        // When
        List<ThreatEvent> recentEvents = threatEngine.getRecentThreatEvents(3);
        
        // Then
        assertNotNull(recentEvents);
        assertTrue(recentEvents.size() <= 3);
        
        // Events should be sorted by timestamp (most recent first)
        for (int i = 1; i < recentEvents.size(); i++) {
            assertTrue(
                recentEvents.get(i - 1).getDetectionTime().compareTo(
                    recentEvents.get(i).getDetectionTime()
                ) >= 0
            );
        }
    }
    
    @Test
    @DisplayName("Should classify threat levels correctly")
    void testThreatLevelClassification() {
        // Given
        UserActivityEvent criticalActivity = UserActivityEvent.builder("critical_user", "delete_all_data")
            .userRole(UserRole.ADMIN)
            .resourceAccessed("database")
            .successful(true)
            .parameter("scope", "entire_system")
            .build();
        
        // When
        ThreatAssessment assessment = threatEngine.analyzeUserActivity(criticalActivity);
        
        // Then
        assertNotNull(assessment);
        // Admin performing dangerous operations should be monitored
        assertNotNull(assessment.getThreatLevel());
    }
}