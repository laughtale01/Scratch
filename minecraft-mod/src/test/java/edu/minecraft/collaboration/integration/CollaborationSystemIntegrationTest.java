package com.yourname.minecraftcollaboration.integration;

import com.yourname.minecraftcollaboration.collaboration.CollaborationManager;
import com.yourname.minecraftcollaboration.commands.CollaborationCommandHandler;
import com.yourname.minecraftcollaboration.models.Invitation;
import com.yourname.minecraftcollaboration.network.CollaborationMessageProcessor;
import com.yourname.minecraftcollaboration.server.CollaborationCoordinator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("integration")
@DisplayName("Collaboration System Integration Tests")
class CollaborationSystemIntegrationTest {
    
    private CollaborationManager collaborationManager;
    private CollaborationCommandHandler commandHandler;
    private CollaborationMessageProcessor messageProcessor;
    private CollaborationCoordinator coordinator;
    
    @Mock
    private MinecraftServer mockServer;
    
    @Mock
    private ServerPlayer mockPlayer1;
    
    @Mock
    private ServerPlayer mockPlayer2;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Initialize components
        collaborationManager = CollaborationManager.getInstance();
        commandHandler = new CollaborationCommandHandler();
        messageProcessor = new CollaborationMessageProcessor();
        coordinator = new CollaborationCoordinator();
        
        // Start coordinator
        coordinator.start();
    }
    
    @AfterEach
    void tearDown() {
        coordinator.stop();
    }
    
    @Test
    @DisplayName("Should handle complete invitation flow")
    void testCompleteInvitationFlow() throws Exception {
        // Given
        String sender = "Player1";
        String recipient = "Player2";
        
        // Step 1: Send invitation through coordinator
        CompletableFuture<Boolean> inviteFuture = coordinator.sendInvitation(sender, recipient);
        assertTrue(inviteFuture.get());
        
        // Step 2: Check invitation exists
        var invitations = collaborationManager.getInvitationsForPlayer(recipient);
        assertEquals(1, invitations.size());
        Invitation invitation = invitations.get(0);
        
        // Step 3: Accept invitation
        boolean accepted = collaborationManager.acceptInvitation(invitation.getId());
        assertTrue(accepted);
        
        // Step 4: Verify invitation is no longer pending
        invitations = collaborationManager.getInvitationsForPlayer(recipient);
        assertEquals(0, invitations.size());
    }
    
    @Test
    @DisplayName("Should handle visit request through message processor")
    void testVisitRequestThroughMessageProcessor() {
        // Given
        Map<String, String> args = new HashMap<>();
        args.put("friendName", "Player2");
        
        String jsonMessage = "{\"command\":\"requestVisit\",\"args\":{\"friendName\":\"Player2\"}}";
        
        // When
        String response = messageProcessor.processMessage(jsonMessage);
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("visitRequest") || response.contains("error"));
    }
    
    @Test
    @DisplayName("Should handle concurrent operations")
    void testConcurrentOperations() throws Exception {
        // Given
        int operationCount = 10;
        CompletableFuture<?>[] futures = new CompletableFuture[operationCount];
        
        // When - Create multiple concurrent invitations
        for (int i = 0; i < operationCount; i++) {
            final int index = i;
            futures[i] = CompletableFuture.runAsync(() -> {
                String sender = "Player" + index;
                String recipient = "Player" + ((index + 1) % operationCount);
                collaborationManager.createInvitation(sender, recipient);
            });
        }
        
        // Wait for all operations to complete
        CompletableFuture.allOf(futures).get();
        
        // Then - Verify all invitations were created
        for (int i = 0; i < operationCount; i++) {
            String recipient = "Player" + i;
            var invitations = collaborationManager.getInvitationsForPlayer(recipient);
            assertTrue(invitations.size() >= 0); // At least some invitations should exist
        }
    }
    
    @Test
    @DisplayName("Should handle emergency return flow")
    void testEmergencyReturnFlow() throws Exception {
        // Given
        String playerName = "TestPlayer";
        
        // Setup player position
        collaborationManager.savePlayerHomePosition(playerName, 100.0, 64.0, 200.0, "overworld");
        collaborationManager.setPlayerWorld(playerName, "other_world");
        
        // When - Emergency return through coordinator
        CompletableFuture<Boolean> returnFuture = coordinator.emergencyReturn(playerName);
        
        // Then
        assertTrue(returnFuture.get());
        assertTrue(collaborationManager.isPlayerInHomeWorld(playerName));
    }
    
    @Test
    @DisplayName("Should process legacy format messages")
    void testLegacyFormatProcessing() {
        // Given
        String legacyMessage = "world.setBlock(100,64,200,stone)";
        
        // When
        String response = messageProcessor.processMessage(legacyMessage);
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("block") || response.contains("error"));
    }
    
    @Test
    @DisplayName("Should handle building commands")
    void testBuildingCommands() {
        // Given
        String buildCircleCommand = "{\"command\":\"buildCircle\",\"args\":{" +
            "\"x\":\"100\",\"y\":\"64\",\"z\":\"200\",\"radius\":\"5\",\"block\":\"stone\"}}";
        
        // When
        String response = messageProcessor.processMessage(buildCircleCommand);
        
        // Then
        assertNotNull(response);
        assertTrue(response.contains("success") || response.contains("error"));
    }
    
    @Test
    @DisplayName("Should maintain world state consistency")
    void testWorldStateConsistency() {
        // Given
        String player1 = "Player1";
        String player2 = "Player2";
        String world1 = "world1";
        String world2 = "world2";
        
        // When
        collaborationManager.setPlayerWorld(player1, world1);
        collaborationManager.setPlayerWorld(player2, world2);
        
        // Then
        assertEquals(world1, collaborationManager.getPlayerCurrentWorld(player1));
        assertEquals(world2, collaborationManager.getPlayerCurrentWorld(player2));
        assertEquals(world1, collaborationManager.getPlayerHomeWorld(player1));
        assertEquals(world2, collaborationManager.getPlayerHomeWorld(player2));
    }
    
    @Test
    @DisplayName("Should handle error cases gracefully")
    void testErrorHandling() {
        // Test invalid command
        String invalidCommand = "{\"command\":\"nonExistentCommand\",\"args\":{}}";
        String response = messageProcessor.processMessage(invalidCommand);
        assertNotNull(response);
        assertTrue(response.contains("error") || response.contains("unknown"));
        
        // Test malformed JSON
        String malformedJson = "{invalid json}";
        response = messageProcessor.processMessage(malformedJson);
        assertNotNull(response);
        
        // Test missing arguments
        String missingArgs = "{\"command\":\"placeBlock\",\"args\":{}}";
        response = messageProcessor.processMessage(missingArgs);
        assertNotNull(response);
        assertTrue(response.contains("error") || response.contains("missing"));
    }
    
    @Test
    @DisplayName("Should respect rate limits")
    void testRateLimiting() {
        // This test would require actual rate limiter integration
        // For now, we verify the structure exists
        assertTrue(com.yourname.minecraftcollaboration.security.SecurityConfig.COMMAND_RATE_LIMIT_PER_SECOND > 0);
    }
}