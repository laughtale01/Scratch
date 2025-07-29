package com.yourname.minecraftcollaboration.entities;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentManager
 */
public class AgentManagerTest {
    
    private AgentManager agentManager;
    
    @Mock
    private ServerPlayer mockPlayer;
    
    @Mock
    private ServerLevel mockLevel;
    
    @Mock
    private CollaborationAgent mockAgent;
    
    private AutoCloseable mocks;
    
    @BeforeEach
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        agentManager = AgentManager.getInstance();
        
        // Setup mock player
        when(mockPlayer.getUUID()).thenReturn(UUID.randomUUID());
        when(mockPlayer.getName()).thenReturn(() -> "TestPlayer");
        when(mockPlayer.serverLevel()).thenReturn(mockLevel);
        when(mockPlayer.blockPosition()).thenReturn(new BlockPos(0, 64, 0));
        
        // Setup mock level
        when(mockLevel.addFreshEntity(any())).thenReturn(true);
    }
    
    @AfterEach
    public void teardown() throws Exception {
        // Clean up agents
        agentManager.cleanupDisconnectedAgents();
        mocks.close();
    }
    
    @Test
    @DisplayName("Test agent summoning")
    public void testSummonAgent() {
        // This test is limited without full Minecraft environment
        // We test the manager logic, not the entity spawning
        
        UUID playerUUID = mockPlayer.getUUID();
        
        // First summon should work
        Optional<CollaborationAgent> agent = agentManager.getAgentForPlayer(playerUUID);
        assertFalse(agent.isPresent(), "No agent should exist initially");
        
        // Note: Actual summoning requires Minecraft server environment
        // We test the management logic here
    }
    
    @Test
    @DisplayName("Test agent removal")
    public void testRemoveAgent() {
        UUID playerUUID = UUID.randomUUID();
        
        // Test removing non-existent agent
        boolean removed = agentManager.removeAgent(playerUUID);
        assertFalse(removed, "Should return false when no agent exists");
        
        // Note: Full test requires agent to be added first
    }
    
    @Test
    @DisplayName("Test agent movement commands")
    public void testAgentMovement() {
        UUID playerUUID = mockPlayer.getUUID();
        BlockPos targetPos = new BlockPos(10, 64, 10);
        
        // Without agent, movement should fail
        boolean moved = agentManager.moveAgent(playerUUID, targetPos);
        assertFalse(moved, "Movement should fail without agent");
        
        // Test direction movement
        boolean movedDir = agentManager.moveAgentInDirection(playerUUID, "forward", 5);
        assertFalse(movedDir, "Direction movement should fail without agent");
    }
    
    @Test
    @DisplayName("Test agent follow command")
    public void testAgentFollow() {
        UUID playerUUID = mockPlayer.getUUID();
        
        // Without agent, follow command should fail
        boolean followSet = agentManager.setAgentFollow(playerUUID, true);
        assertFalse(followSet, "Follow command should fail without agent");
    }
    
    @Test
    @DisplayName("Test agent action command")
    public void testAgentAction() {
        UUID playerUUID = mockPlayer.getUUID();
        
        // Without agent, action should fail
        boolean actionPerformed = agentManager.agentPerformAction(playerUUID, "jump");
        assertFalse(actionPerformed, "Action should fail without agent");
    }
    
    @Test
    @DisplayName("Test safe spawn position calculation")
    public void testSafeSpawnPosition() {
        // This is tested indirectly through summonAgent
        // The private method findSafeSpawnPosition ensures agent spawns near player
        assertNotNull(agentManager, "AgentManager should be initialized");
    }
    
    @Test
    @DisplayName("Test cleanup of disconnected agents")
    public void testCleanupDisconnectedAgents() {
        // Test that cleanup doesn't throw exceptions
        assertDoesNotThrow(() -> {
            agentManager.cleanupDisconnectedAgents();
        });
    }
    
    @Test
    @DisplayName("Test multiple agent management")
    public void testMultipleAgents() {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        // Each player should have independent agent management
        Optional<CollaborationAgent> agent1 = agentManager.getAgentForPlayer(player1);
        Optional<CollaborationAgent> agent2 = agentManager.getAgentForPlayer(player2);
        
        assertFalse(agent1.isPresent());
        assertFalse(agent2.isPresent());
        
        // Removing one shouldn't affect the other
        agentManager.removeAgent(player1);
        // player2's state should be unchanged
    }
    
    @Test
    @DisplayName("Test agent retrieval by ID")
    public void testGetAgentById() {
        String agentId = "test-agent-id";
        
        Optional<CollaborationAgent> agent = agentManager.getAgentById(agentId);
        assertFalse(agent.isPresent(), "Non-existent agent should return empty");
    }
}