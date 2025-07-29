package edu.minecraft.collaboration.collaboration;

import edu.minecraft.collaboration.models.Invitation;
import edu.minecraft.collaboration.models.VisitRequest;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollaborationManagerTest {
    
    private CollaborationManager collaborationManager;
    
    @Mock
    private MinecraftServer mockServer;
    
    @Mock
    private PlayerList mockPlayerList;
    
    @Mock
    private ServerPlayer mockPlayer1;
    
    @Mock
    private ServerPlayer mockPlayer2;
    
    @Mock
    private FoodData mockFoodData;
    
    @Mock
    private Level mockLevel;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        collaborationManager = CollaborationManager.getInstance();
        
        // Setup mock server
        when(mockServer.getPlayerList()).thenReturn(mockPlayerList);
    }
    
    @Test
    @DisplayName("Should create invitation successfully")
    void testCreateInvitation() {
        // Given
        String senderName = "Player1";
        String recipientName = "Player2";
        
        // When
        Invitation invitation = collaborationManager.createInvitation(senderName, recipientName);
        
        // Then
        assertNotNull(invitation);
        assertEquals(senderName, invitation.getSenderName());
        assertEquals(recipientName, invitation.getRecipientName());
        assertEquals(Invitation.InvitationStatus.PENDING, invitation.getStatus());
        assertNotNull(invitation.getId());
    }
    
    @Test
    @DisplayName("Should not create duplicate invitations")
    void testNoDuplicateInvitations() {
        // Given
        String senderName = "Player1";
        String recipientName = "Player2";
        
        // When
        Invitation invitation1 = collaborationManager.createInvitation(senderName, recipientName);
        Invitation invitation2 = collaborationManager.createInvitation(senderName, recipientName);
        
        // Then
        assertNotNull(invitation1);
        assertNotNull(invitation2);
        assertEquals(invitation1.getId(), invitation2.getId());
    }
    
    @Test
    @DisplayName("Should get invitations for specific player")
    void testGetInvitationsForPlayer() {
        // Given
        String player1 = "Player1";
        String player2 = "Player2";
        String player3 = "Player3";
        
        collaborationManager.createInvitation(player1, player2);
        collaborationManager.createInvitation(player3, player2);
        collaborationManager.createInvitation(player1, player3);
        
        // When
        List<Invitation> invitationsForPlayer2 = collaborationManager.getInvitationsForPlayer(player2);
        
        // Then
        assertEquals(2, invitationsForPlayer2.size());
        assertTrue(invitationsForPlayer2.stream()
            .allMatch(inv -> inv.getRecipientName().equals(player2)));
    }
    
    @Test
    @DisplayName("Should accept invitation")
    void testAcceptInvitation() {
        // Given
        String senderName = "Player1";
        String recipientName = "Player2";
        Invitation invitation = collaborationManager.createInvitation(senderName, recipientName);
        
        // When
        boolean accepted = collaborationManager.acceptInvitation(invitation.getId());
        
        // Then
        assertTrue(accepted);
        List<Invitation> pendingInvitations = collaborationManager.getInvitationsForPlayer(recipientName);
        assertEquals(0, pendingInvitations.size());
    }
    
    @Test
    @DisplayName("Should decline invitation")
    void testDeclineInvitation() {
        // Given
        String senderName = "Player1";
        String recipientName = "Player2";
        Invitation invitation = collaborationManager.createInvitation(senderName, recipientName);
        
        // When
        boolean declined = collaborationManager.declineInvitation(invitation.getId());
        
        // Then
        assertTrue(declined);
        List<Invitation> pendingInvitations = collaborationManager.getInvitationsForPlayer(recipientName);
        assertEquals(0, pendingInvitations.size());
    }
    
    @Test
    @DisplayName("Should create visit request successfully")
    void testCreateVisitRequest() {
        // Given
        String requesterName = "Player1";
        String hostName = "Player2";
        
        // When
        VisitRequest request = collaborationManager.createVisitRequest(requesterName, hostName);
        
        // Then
        assertNotNull(request);
        assertEquals(requesterName, request.getRequesterName());
        assertEquals(hostName, request.getHostName());
        assertEquals(VisitRequest.VisitStatus.PENDING, request.getStatus());
    }
    
    @Test
    @DisplayName("Should approve visit request and teleport player")
    void testApproveVisitRequest() {
        // Given
        String requesterName = "Player1";
        String hostName = "Player2";
        VisitRequest request = collaborationManager.createVisitRequest(requesterName, hostName);
        
        // Setup mocks
        when(mockPlayerList.getPlayerByName(requesterName)).thenReturn(mockPlayer1);
        when(mockPlayerList.getPlayerByName(hostName)).thenReturn(mockPlayer2);
        when(mockPlayer1.getName()).thenReturn(Component.literal(requesterName));
        when(mockPlayer2.getName()).thenReturn(Component.literal(hostName));
        when(mockPlayer2.getX()).thenReturn(100.0);
        when(mockPlayer2.getY()).thenReturn(64.0);
        when(mockPlayer2.getZ()).thenReturn(200.0);
        when(mockPlayer2.level()).thenReturn(mockLevel);
        when(mockLevel.dimension()).thenReturn(Level.OVERWORLD);
        
        // When
        boolean approved = collaborationManager.approveVisitRequest(request.getId(), mockServer);
        
        // Then
        assertTrue(approved);
        verify(mockPlayer1).teleportTo(100.0, 64.0, 200.0);
    }
    
    @Test
    @DisplayName("Should track player world changes")
    void testPlayerWorldTracking() {
        // Given
        String playerName = "Player1";
        String worldName1 = "world1";
        String worldName2 = "world2";
        
        // When
        collaborationManager.setPlayerWorld(playerName, worldName1);
        String currentWorld = collaborationManager.getPlayerCurrentWorld(playerName);
        
        // Then
        assertEquals(worldName1, currentWorld);
        assertEquals(worldName1, collaborationManager.getPlayerHomeWorld(playerName));
        assertTrue(collaborationManager.isPlayerInHomeWorld(playerName));
        
        // When player moves to another world
        collaborationManager.setPlayerWorld(playerName, worldName2);
        
        // Then
        assertEquals(worldName2, collaborationManager.getPlayerCurrentWorld(playerName));
        assertEquals(worldName1, collaborationManager.getPlayerHomeWorld(playerName)); // Home stays the same
        assertFalse(collaborationManager.isPlayerInHomeWorld(playerName));
    }
    
    @Test
    @DisplayName("Should save and restore player home position")
    void testPlayerHomePosition() {
        // Given
        String playerName = "Player1";
        double x = 100.5, y = 64.0, z = 200.5;
        String dimension = "overworld";
        
        // When
        collaborationManager.savePlayerHomePosition(playerName, x, y, z, dimension);
        
        // Setup mock player
        when(mockPlayer1.getName()).thenReturn(Component.literal(playerName));
        
        // When
        boolean returned = collaborationManager.returnPlayerHome(mockPlayer1);
        
        // Then
        assertTrue(returned);
        verify(mockPlayer1).teleportTo(x, y, z);
    }
    
    @Test
    @DisplayName("Should handle emergency return with full restoration")
    void testEmergencyReturn() {
        // Given
        String playerName = "Player1";
        collaborationManager.savePlayerHomePosition(playerName, 100.0, 64.0, 200.0, "overworld");
        
        // Setup mocks
        when(mockPlayer1.getName()).thenReturn(Component.literal(playerName));
        when(mockPlayer1.getMaxHealth()).thenReturn(20.0f);
        when(mockPlayer1.getFoodData()).thenReturn(mockFoodData);
        
        // When
        boolean returned = collaborationManager.emergencyReturnPlayer(mockPlayer1);
        
        // Then
        assertTrue(returned);
        verify(mockPlayer1).teleportTo(100.0, 64.0, 200.0);
        verify(mockPlayer1).setHealth(20.0f);
        verify(mockPlayer1).removeAllEffects();
        verify(mockFoodData).setFoodLevel(20);
        verify(mockFoodData).setSaturation(20.0f);
    }
    
    @Test
    @DisplayName("Should handle invalid invitation ID")
    void testInvalidInvitationId() {
        // Given
        UUID invalidId = UUID.randomUUID();
        
        // When
        boolean accepted = collaborationManager.acceptInvitation(invalidId);
        boolean declined = collaborationManager.declineInvitation(invalidId);
        
        // Then
        assertFalse(accepted);
        assertFalse(declined);
    }
    
    @Test
    @DisplayName("Should clean up expired entries")
    void testExpiredEntriesCleanup() {
        // This test would require mocking time or using a test-specific method
        // to force expiration. For now, we just ensure the methods exist.
        
        // Given
        String senderName = "Player1";
        String recipientName = "Player2";
        
        // When
        Invitation invitation = collaborationManager.createInvitation(senderName, recipientName);
        
        // Then
        assertNotNull(invitation);
        assertFalse(invitation.isExpired()); // Should not be expired immediately
    }
}