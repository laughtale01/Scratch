package edu.minecraft.collaboration.collaboration;

import edu.minecraft.collaboration.collaboration.CollaborationManager;
import edu.minecraft.collaboration.core.DependencyInjector;
import edu.minecraft.collaboration.models.Invitation;
import edu.minecraft.collaboration.models.VisitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Basic unit tests for CollaborationManager
 */
@DisplayName("CollaborationManager Tests")
public class CollaborationManagerTest {
    
    private CollaborationManager collaborationManager;
    
    @BeforeEach
    void setUp() {
        collaborationManager = DependencyInjector.getInstance().getService(CollaborationManager.class);
    }
    
    @Test
    @DisplayName("Should create invitation successfully")
    void testCreateInvitation() {
        // When
        Invitation invitation = collaborationManager.createInvitation("Sender", "Recipient");
        
        // Then
        assertNotNull(invitation);
        assertEquals("Sender", invitation.getSenderName());
        assertEquals("Recipient", invitation.getRecipientName());
        assertEquals(Invitation.InvitationStatus.PENDING, invitation.getStatus());
    }
    
    @Test
    @DisplayName("Should create visit request successfully")
    void testCreateVisitRequest() {
        // When
        VisitRequest request = collaborationManager.createVisitRequest("Visitor", "Host");
        
        // Then
        assertNotNull(request);
        assertEquals("Visitor", request.getRequesterName());
        assertEquals("Host", request.getHostName());
        assertEquals(VisitRequest.VisitStatus.PENDING, request.getStatus());
    }
    
    @Test
    @DisplayName("Should get singleton instance")
    void testSingletonInstance() {
        // When
        DependencyInjector injector = DependencyInjector.getInstance();
        CollaborationManager instance1 = injector.getService(CollaborationManager.class);
        CollaborationManager instance2 = injector.getService(CollaborationManager.class);
        
        // Then
        assertSame(instance1, instance2);
    }
}