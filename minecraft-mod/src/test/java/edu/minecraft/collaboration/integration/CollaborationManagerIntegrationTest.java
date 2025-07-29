package com.yourname.minecraftcollaboration.integration;

import com.yourname.minecraftcollaboration.collaboration.CollaborationManager;
import com.yourname.minecraftcollaboration.models.Invitation;
import com.yourname.minecraftcollaboration.models.VisitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;

/**
 * Integration tests for CollaborationManager
 */
public class CollaborationManagerIntegrationTest {
    
    private CollaborationManager collaborationManager;
    
    @BeforeEach
    public void setup() {
        collaborationManager = CollaborationManager.getInstance();
    }
    
    @Test
    public void testInvitationWorkflow() {
        String sender = "Player1";
        String recipient = "Player2";
        
        // Create invitation
        Invitation invitation = collaborationManager.createInvitation(sender, recipient);
        assertNotNull(invitation);
        assertEquals(sender, invitation.getSenderName());
        assertEquals(recipient, invitation.getRecipientName());
        assertEquals(Invitation.InvitationStatus.PENDING, invitation.getStatus());
        
        // Get invitations for recipient
        List<Invitation> invitations = collaborationManager.getInvitationsForPlayer(recipient);
        assertEquals(1, invitations.size());
        assertEquals(invitation.getId(), invitations.get(0).getId());
        
        // Accept invitation
        boolean accepted = collaborationManager.acceptInvitation(invitation.getId());
        assertTrue(accepted);
        assertEquals(Invitation.InvitationStatus.ACCEPTED, invitation.getStatus());
        
        // Verify invitation no longer appears as pending
        invitations = collaborationManager.getInvitationsForPlayer(recipient);
        assertEquals(0, invitations.size());
    }
    
    @Test
    public void testVisitRequestWorkflow() {
        String requester = "Player1";
        String host = "Player2";
        
        // Create visit request
        VisitRequest request = collaborationManager.createVisitRequest(requester, host);
        assertNotNull(request);
        assertEquals(requester, request.getRequesterName());
        assertEquals(host, request.getHostName());
        assertEquals(VisitRequest.VisitStatus.PENDING, request.getStatus());
        
        // Get visit requests for host
        List<VisitRequest> requests = collaborationManager.getVisitRequestsForHost(host);
        assertEquals(1, requests.size());
        assertEquals(request.getId(), requests.get(0).getId());
        
        // Note: Cannot test actual teleportation without Minecraft server context
        // Just verify the request management works
        
        // Deny request
        boolean denied = collaborationManager.denyVisitRequest(request.getId());
        assertTrue(denied);
        assertEquals(VisitRequest.VisitStatus.DENIED, request.getStatus());
        
        // Verify request no longer appears as pending
        requests = collaborationManager.getVisitRequestsForHost(host);
        assertEquals(0, requests.size());
    }
    
    @Test
    public void testWorldTracking() {
        String playerName = "TestPlayer";
        String worldName = "TestWorld";
        
        // Set player world
        collaborationManager.setPlayerWorld(playerName, worldName);
        
        // Verify world is tracked
        assertEquals(worldName, collaborationManager.getPlayerCurrentWorld(playerName));
        assertEquals(worldName, collaborationManager.getPlayerHomeWorld(playerName));
        assertTrue(collaborationManager.isPlayerInHomeWorld(playerName));
        
        // Change to different world
        String newWorld = "VisitingWorld";
        collaborationManager.setPlayerWorld(playerName, newWorld);
        
        // Verify tracking
        assertEquals(newWorld, collaborationManager.getPlayerCurrentWorld(playerName));
        assertEquals(worldName, collaborationManager.getPlayerHomeWorld(playerName)); // Home stays the same
        assertFalse(collaborationManager.isPlayerInHomeWorld(playerName));
    }
    
    @Test
    public void testMultipleInvitations() {
        String sender1 = "Player1";
        String sender2 = "Player2";
        String recipient = "Player3";
        
        // Create multiple invitations
        Invitation inv1 = collaborationManager.createInvitation(sender1, recipient);
        Invitation inv2 = collaborationManager.createInvitation(sender2, recipient);
        
        assertNotNull(inv1);
        assertNotNull(inv2);
        assertNotEquals(inv1.getId(), inv2.getId());
        
        // Get all invitations
        List<Invitation> invitations = collaborationManager.getInvitationsForPlayer(recipient);
        assertEquals(2, invitations.size());
        
        // Decline one
        collaborationManager.declineInvitation(inv1.getId());
        
        // Verify only one remains
        invitations = collaborationManager.getInvitationsForPlayer(recipient);
        assertEquals(1, invitations.size());
        assertEquals(inv2.getId(), invitations.get(0).getId());
    }
    
    @Test
    public void testDuplicateInvitationPrevention() {
        String sender = "Player1";
        String recipient = "Player2";
        
        // Create first invitation
        Invitation inv1 = collaborationManager.createInvitation(sender, recipient);
        assertNotNull(inv1);
        
        // Try to create duplicate
        Invitation inv2 = collaborationManager.createInvitation(sender, recipient);
        assertNotNull(inv2);
        
        // Should return the same invitation
        assertEquals(inv1.getId(), inv2.getId());
        
        // Verify only one invitation exists
        List<Invitation> invitations = collaborationManager.getInvitationsForPlayer(recipient);
        assertEquals(1, invitations.size());
    }
}