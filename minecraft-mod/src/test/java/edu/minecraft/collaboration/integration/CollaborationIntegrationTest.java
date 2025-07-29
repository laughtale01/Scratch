package com.yourname.minecraftcollaboration.integration;

import com.yourname.minecraftcollaboration.collaboration.CollaborationManager;
import com.yourname.minecraftcollaboration.models.Invitation;
import com.yourname.minecraftcollaboration.models.VisitRequest;
import com.yourname.minecraftcollaboration.server.CollaborationCoordinator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import net.minecraft.network.chat.Component;

/**
 * Integration tests for collaboration features
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CollaborationIntegrationTest {
    
    private CollaborationManager collaborationManager;
    private CollaborationCoordinator coordinator;
    
    @Mock
    private MinecraftServer mockServer;
    
    @Mock
    private ServerPlayer player1;
    
    @Mock
    private ServerPlayer player2;
    
    private AutoCloseable mocks;
    
    @BeforeAll
    public void setupAll() {
        mocks = MockitoAnnotations.openMocks(this);
    }
    
    @BeforeEach
    public void setup() {
        // Initialize collaboration components
        collaborationManager = CollaborationManager.getInstance();
        coordinator = new CollaborationCoordinator();
        coordinator.start();
        
        // Setup mock players
        when(player1.getName()).thenReturn(Component.literal("Player1"));
        when(player2.getName()).thenReturn(Component.literal("Player2"));
        when(player1.getUUID()).thenReturn(UUID.randomUUID());
        when(player2.getUUID()).thenReturn(UUID.randomUUID());
    }
    
    @AfterEach
    public void teardown() {
        coordinator.stop();
    }
    
    @AfterAll
    public void teardownAll() throws Exception {
        mocks.close();
    }
    
    @Test
    @DisplayName("Test invitation creation and retrieval")
    public void testInvitationFlow() {
        // Create invitation
        Invitation invitation = collaborationManager.createInvitation("Player1", "Player2");
        
        assertNotNull(invitation, "Invitation should be created");
        assertEquals("Player1", invitation.getSenderName());
        assertEquals("Player2", invitation.getRecipientName());
        assertEquals(Invitation.InvitationStatus.PENDING, invitation.getStatus());
        
        // Retrieve invitations for player
        List<Invitation> invitations = collaborationManager.getInvitationsForPlayer("Player2");
        
        assertEquals(1, invitations.size(), "Player2 should have 1 invitation");
        assertEquals(invitation.getId(), invitations.get(0).getId());
    }
    
    @Test
    @DisplayName("Test invitation acceptance")
    public void testInvitationAcceptance() {
        // Create invitation
        Invitation invitation = collaborationManager.createInvitation("Player1", "Player2");
        UUID invitationId = invitation.getId();
        
        // Accept invitation
        boolean accepted = collaborationManager.acceptInvitation(invitationId);
        
        assertTrue(accepted, "Invitation should be accepted");
        assertEquals(Invitation.InvitationStatus.ACCEPTED, invitation.getStatus());
        
        // Verify invitation no longer appears in pending list
        List<Invitation> pendingInvitations = collaborationManager.getInvitationsForPlayer("Player2");
        assertTrue(pendingInvitations.isEmpty(), "No pending invitations should remain");
    }
    
    @Test
    @DisplayName("Test visit request flow")
    public void testVisitRequestFlow() {
        // Create visit request
        VisitRequest request = collaborationManager.createVisitRequest("Player1", "Player2");
        
        assertNotNull(request, "Visit request should be created");
        assertEquals("Player1", request.getRequesterName());
        assertEquals("Player2", request.getHostName());
        assertEquals(VisitRequest.VisitStatus.PENDING, request.getStatus());
        
        // Retrieve visit requests for host
        List<VisitRequest> requests = collaborationManager.getVisitRequestsForHost("Player2");
        
        assertEquals(1, requests.size(), "Player2 should have 1 visit request");
        assertEquals(request.getId(), requests.get(0).getId());
    }
    
    @Test
    @DisplayName("Test coordinator invitation sending")
    public void testCoordinatorSendInvitation() throws Exception {
        // Send invitation through coordinator
        CompletableFuture<Boolean> future = coordinator.sendInvitation("Player1", "Player2");
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        assertTrue(result, "Invitation should be sent successfully");
        
        // Verify invitation exists
        List<Invitation> invitations = collaborationManager.getInvitationsForPlayer("Player2");
        assertEquals(1, invitations.size(), "Player2 should have received the invitation");
    }
    
    @Test
    @DisplayName("Test coordinator visit request")
    public void testCoordinatorVisitRequest() throws Exception {
        // Request visit through coordinator
        boolean result = coordinator.requestVisit("Player1", "Player2");
        Boolean result = future.get(5, TimeUnit.SECONDS);
        
        assertTrue(result, "Visit request should be sent successfully");
        
        // Verify visit request exists
        List<VisitRequest> requests = collaborationManager.getVisitRequestsForHost("Player2");
        assertEquals(1, requests.size(), "Player2 should have received the visit request");
    }
    
    @Test
    @DisplayName("Test player world tracking")
    public void testPlayerWorldTracking() {
        String playerName = "TestPlayer";
        String worldName = "minecraft:overworld";
        
        // Set player world
        collaborationManager.setPlayerWorld(playerName, worldName);
        
        // Verify world tracking
        String currentWorld = collaborationManager.getPlayerCurrentWorld(playerName);
        assertEquals(worldName, currentWorld);
        
        String homeWorld = collaborationManager.getPlayerHomeWorld(playerName);
        assertEquals(worldName, homeWorld, "First world should be set as home");
        
        assertTrue(collaborationManager.isPlayerInHomeWorld(playerName));
        
        // Change world
        String newWorld = "minecraft:nether";
        collaborationManager.setPlayerWorld(playerName, newWorld);
        
        assertEquals(newWorld, collaborationManager.getPlayerCurrentWorld(playerName));
        assertEquals(worldName, collaborationManager.getPlayerHomeWorld(playerName), "Home world should not change");
        assertFalse(collaborationManager.isPlayerInHomeWorld(playerName));
    }
    
    @Test
    @DisplayName("Test duplicate invitation prevention")
    public void testDuplicateInvitationPrevention() {
        // Create first invitation
        Invitation invitation1 = collaborationManager.createInvitation("Player1", "Player2");
        
        // Try to create duplicate
        Invitation invitation2 = collaborationManager.createInvitation("Player1", "Player2");
        
        // Should return the existing invitation
        assertEquals(invitation1.getId(), invitation2.getId(), "Should return existing invitation");
        
        // Verify only one invitation exists
        List<Invitation> invitations = collaborationManager.getInvitationsForPlayer("Player2");
        assertEquals(1, invitations.size(), "Only one invitation should exist");
    }
    
    @Test
    @DisplayName("Test invitation expiration handling")
    public void testInvitationExpiration() {
        // Create invitation with short expiration for testing
        Invitation invitation = collaborationManager.createInvitation("Player1", "Player2");
        
        // Manually expire the invitation
        invitation.setStatus(Invitation.InvitationStatus.EXPIRED);
        
        // Try to accept expired invitation
        boolean accepted = collaborationManager.acceptInvitation(invitation.getId());
        
        assertFalse(accepted, "Expired invitation should not be accepted");
        
        // Verify invitation no longer appears in pending list
        List<Invitation> pendingInvitations = collaborationManager.getInvitationsForPlayer("Player2");
        assertTrue(pendingInvitations.isEmpty(), "Expired invitations should not appear in pending list");
    }
    
    @Test
    @DisplayName("Test concurrent invitation handling")
    public void testConcurrentInvitations() throws Exception {
        int numInvitations = 10;
        CompletableFuture<Invitation>[] futures = new CompletableFuture[numInvitations];
        
        // Create multiple invitations concurrently
        for (int i = 0; i < numInvitations; i++) {
            final int index = i;
            futures[i] = CompletableFuture.supplyAsync(() -> 
                collaborationManager.createInvitation("Player" + index, "TargetPlayer")
            );
        }
        
        // Wait for all to complete
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);
        
        // Verify all invitations were created
        List<Invitation> invitations = collaborationManager.getInvitationsForPlayer("TargetPlayer");
        assertEquals(numInvitations, invitations.size(), "All invitations should be created");
        
        // Verify each invitation is from a different sender
        long uniqueSenders = invitations.stream()
            .map(Invitation::getSenderName)
            .distinct()
            .count();
        assertEquals(numInvitations, uniqueSenders, "Each invitation should have a unique sender");
    }
}