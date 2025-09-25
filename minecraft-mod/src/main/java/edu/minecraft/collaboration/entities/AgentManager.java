package edu.minecraft.collaboration.entities;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

/**
 * Manages agent entities for players
 */
public final class AgentManager {
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private static AgentManager instance;

    // Map player UUID to their agent
    private final Map<UUID, CollaborationAgent> playerAgents = new HashMap<>();
    // Map agent ID to agent entity
    private final Map<String, CollaborationAgent> agentRegistry = new HashMap<>();

    private AgentManager() { }

    public static AgentManager getInstance() {
        if (instance == null) {
            instance = new AgentManager();
        }
        return instance;
    }

    /**
     * Summon an agent for a player
     */
    public CollaborationAgent summonAgent(ServerPlayer player, String agentName) {
        try {
            // Remove existing agent if any
            removeAgent(player.getUUID());

            ServerLevel level = player.serverLevel();
            BlockPos playerPos = player.blockPosition();

            // Spawn agent near player
            BlockPos spawnPos = findSafeSpawnPosition(level, playerPos);

            CollaborationAgent agent = ModEntities.COLLABORATION_AGENT.get().create(level);
            if (agent != null) {
                agent.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                agent.setAgentName(agentName != null ? agentName : "Agent");
                agent.setOwner(player);

                level.addFreshEntity(agent);

                // Register the agent
                playerAgents.put(player.getUUID(), agent);
                agentRegistry.put(agent.getAgentId(), agent);

                LOGGER.info("Summoned agent '{}' for player {}", agent.getAgentName(), player.getName().getString());
                return agent;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to summon agent for player {}", player.getName().getString(), e);
        }

        return null;
    }

    /**
     * Get agent for a player
     */
    public Optional<CollaborationAgent> getAgentForPlayer(UUID playerUUID) {
        CollaborationAgent agent = playerAgents.get(playerUUID);
        if (agent != null && agent.isAlive()) {
            return Optional.of(agent);
        }
        return Optional.empty();
    }

    /**
     * Get active agent for primary player
     */
    public CollaborationAgent getActiveAgent() {
        // Get the first player's agent (single-player mode)
        if (!playerAgents.isEmpty()) {
            return playerAgents.values().iterator().next();
        }
        return null;
    }

    /**
     * Get agent by ID
     */
    public Optional<CollaborationAgent> getAgentById(String agentId) {
        CollaborationAgent agent = agentRegistry.get(agentId);
        if (agent != null && agent.isAlive()) {
            return Optional.of(agent);
        }
        return Optional.empty();
    }

    /**
     * Remove an agent
     */
    public boolean removeAgent(UUID playerUUID) {
        CollaborationAgent agent = playerAgents.remove(playerUUID);
        if (agent != null) {
            agentRegistry.remove(agent.getAgentId());
            agent.discard();
            LOGGER.info("Removed agent for player with UUID {}", playerUUID);
            return true;
        }
        return false;
    }

    /**
     * Dismiss agent by ID
     */
    public boolean dismissAgent(String agentId) {
        CollaborationAgent agent = agentRegistry.get(agentId);
        if (agent != null) {
            // Find the player UUID for this agent
            UUID playerUUID = null;
            for (Map.Entry<UUID, CollaborationAgent> entry : playerAgents.entrySet()) {
                if (entry.getValue().equals(agent)) {
                    playerUUID = entry.getKey();
                    break;
                }
            }

            if (playerUUID != null) {
                return removeAgent(playerUUID);
            }
        }
        return false;
    }

    /**
     * Move agent to position
     */
    public boolean moveAgent(UUID playerUUID, BlockPos targetPos) {
        Optional<CollaborationAgent> agent = getAgentForPlayer(playerUUID);
        if (agent.isPresent()) {
            agent.get().moveToPosition(targetPos);
            return true;
        }
        return false;
    }

    /**
     * Move agent in direction
     */
    public boolean moveAgentInDirection(UUID playerUUID, String direction, int distance) {
        Optional<CollaborationAgent> agent = getAgentForPlayer(playerUUID);
        if (agent.isPresent()) {
            agent.get().moveInDirection(direction, distance);
            return true;
        }
        return false;
    }

    /**
     * Make agent follow player
     */
    public boolean setAgentFollow(UUID playerUUID, boolean follow) {
        Optional<CollaborationAgent> agent = getAgentForPlayer(playerUUID);
        if (agent.isPresent()) {
            agent.get().followPlayer(follow);
            return true;
        }
        return false;
    }

    /**
     * Make agent perform action
     */
    public boolean agentPerformAction(UUID playerUUID, String action) {
        Optional<CollaborationAgent> agent = getAgentForPlayer(playerUUID);
        if (agent.isPresent()) {
            agent.get().performAction(action);
            return true;
        }
        return false;
    }

    /**
     * Find safe spawn position near player
     */
    private BlockPos findSafeSpawnPosition(ServerLevel level, BlockPos playerPos) {
        // Try positions around player
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) {
                    continue; // Skip player position
                }

                BlockPos checkPos = playerPos.offset(dx, 0, dz);

                // Check if position is safe (solid ground, air above)
                if (level.getBlockState(checkPos.below()).isSolid()
                        && level.getBlockState(checkPos).isAir()
                        && level.getBlockState(checkPos.above()).isAir()) {
                    return checkPos;
                }
            }
        }

        // Fallback to offset position
        return playerPos.offset(2, 0, 0);
    }

    /**
     * Clean up disconnected players' agents
     */
    public void cleanupDisconnectedAgents() {
        playerAgents.entrySet().removeIf(entry -> {
            CollaborationAgent agent = entry.getValue();
            if (!agent.isAlive()) {
                agentRegistry.remove(agent.getAgentId());
                return true;
            }
            return false;
        });
    }
}
