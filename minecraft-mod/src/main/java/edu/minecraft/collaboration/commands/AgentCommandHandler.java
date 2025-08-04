package edu.minecraft.collaboration.commands;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import edu.minecraft.collaboration.util.ValidationUtils;
import edu.minecraft.collaboration.util.ResponseHelper;
import edu.minecraft.collaboration.entities.AgentManager;
import edu.minecraft.collaboration.entities.CollaborationAgent;
import edu.minecraft.collaboration.monitoring.MetricsCollector;

/**
 * Handler for agent-related commands (summon, move, actions, dismiss)
 */
public class AgentCommandHandler {
    
    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private final MetricsCollector metricsCollector;
    
    public AgentCommandHandler(MetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
    }
    
    /**
     * Handle summon agent command
     */
    public String handleSummonAgent(String[] args) {
        try {
            String agentName = args.length > 0 ? args[0] : "Agent";
            
            // Validate agent name
            if (!ValidationUtils.isValidAgentName(agentName)) {
                return ResponseHelper.error("summonAgent", ResponseHelper.ERROR_INVALID_ARGS, "Invalid agent name");
            }
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                BlockPos playerPos = player.blockPosition();
                
                // Summon agent using the correct API
                var agent = AgentManager.getInstance().summonAgent(player, agentName);
                if (agent != null) {
                    metricsCollector.incrementCounter("commands.summonAgent");
                    BlockPos agentPos = agent.blockPosition();
                    return ResponseHelper.agentSummoned(agentName, agentPos.getX(), agentPos.getY(), agentPos.getZ());
                } else {
                    return ResponseHelper.error("summonAgent", ResponseHelper.ERROR_AGENT_LIMIT, "Cannot summon more agents");
                }
            }
            return ResponseHelper.error("summonAgent", ResponseHelper.ERROR_NO_PLAYER, "No players online");
        } catch (Exception e) {
            LOGGER.error("Error summoning agent", e);
            return ResponseHelper.error("summonAgent", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle move agent command
     */
    public String handleMoveAgent(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_ARGS, "Expected: direction [distance] OR x y z");
        }
        
        try {
            CollaborationAgent agent = AgentManager.getInstance().getActiveAgent();
            if (agent == null) {
                return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_NO_AGENT, "No active agent");
            }
            
            if (args.length == 3) {
                return handleMoveAgentToCoordinates(agent, args);
            } else {
                return handleMoveAgentInDirection(agent, args);
            }
        } catch (NumberFormatException e) {
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_ARGS, "Invalid number format");
        } catch (Exception e) {
            LOGGER.error("Error moving agent", e);
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle moving agent to specific coordinates
     * @param agent The agent to move
     * @param args Command arguments containing x, y, z coordinates
     * @return Response string
     */
    private String handleMoveAgentToCoordinates(CollaborationAgent agent, String[] args) {
        int x = Integer.parseInt(args[0]);
        int y = Integer.parseInt(args[1]);
        int z = Integer.parseInt(args[2]);
        
        if (!ValidationUtils.isValidCoordinate(x, y, z)) {
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_COORDS, "Invalid coordinates");
        }
        
        BlockPos targetPos = new BlockPos(x, y, z);
        boolean success = agent.moveToTarget(Vec3.atCenterOf(targetPos));
        
        if (success) {
            metricsCollector.incrementCounter("commands.moveAgent");
            return ResponseHelper.agentMoved(x, y, z);
        } else {
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_MOVE_FAILED, "Cannot move to that position");
        }
    }
    
    /**
     * Handle moving agent in a specific direction
     * @param agent The agent to move
     * @param args Command arguments containing direction and optional distance
     * @return Response string
     */
    private String handleMoveAgentInDirection(CollaborationAgent agent, String[] args) {
        String direction = args[0].toLowerCase();
        int distance = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        
        if (distance < 1 || distance > 50) {
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_ARGS, "Distance must be between 1 and 50");
        }
        
        BlockPos currentPos = agent.blockPosition();
        BlockPos targetPos = calculateTargetPosition(currentPos, direction, distance);
        
        if (targetPos == null) {
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_ARGS, "Invalid direction: " + direction);
        }
        
        boolean success = agent.moveToTarget(Vec3.atCenterOf(targetPos));
        
        if (success) {
            metricsCollector.incrementCounter("commands.moveAgent");
            return ResponseHelper.agentMoved(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        } else {
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_MOVE_FAILED, "Cannot move in that direction");
        }
    }
    
    /**
     * Calculate target position based on direction and distance
     * @param currentPos Current agent position
     * @param direction Direction to move
     * @param distance Distance to move
     * @return Target position or null if invalid direction
     */
    private BlockPos calculateTargetPosition(BlockPos currentPos, String direction, int distance) {
        switch (direction) {
            case "forward":
            case "north":
                return currentPos.north(distance);
            case "back":
            case "south":
                return currentPos.south(distance);
            case "left":
            case "west":
                return currentPos.west(distance);
            case "right":
            case "east":
                return currentPos.east(distance);
            case "up":
                return currentPos.above(distance);
            case "down":
                return currentPos.below(distance);
            default:
                return null;
        }
    }
    
    /**
     * Handle agent follow command
     */
    public String handleAgentFollow(String[] args) {
        try {
            boolean follow = args.length == 0 || Boolean.parseBoolean(args[0]);
            
            CollaborationAgent agent = AgentManager.getInstance().getActiveAgent();
            if (agent == null) {
                return ResponseHelper.error("agentFollow", ResponseHelper.ERROR_NO_AGENT, "No active agent");
            }
            
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null && !server.getPlayerList().getPlayers().isEmpty()) {
                ServerPlayer player = server.getPlayerList().getPlayers().get(0);
                
                if (follow) {
                    agent.setFollowTarget(player);
                    metricsCollector.incrementCounter("commands.agentFollow");
                    return ResponseHelper.agentFollowing(true);
                } else {
                    agent.setFollowTarget(null);
                    metricsCollector.incrementCounter("commands.agentFollow");
                    return ResponseHelper.agentFollowing(false);
                }
            }
            return ResponseHelper.error("agentFollow", ResponseHelper.ERROR_NO_PLAYER, "No players online");
        } catch (Exception e) {
            LOGGER.error("Error setting agent follow", e);
            return ResponseHelper.error("agentFollow", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Handle agent action command
     */
    public String handleAgentAction(String[] args) {
        if (args.length < 1) {
            return ResponseHelper.error("agentAction", ResponseHelper.ERROR_INVALID_ARGS, "Expected: action");
        }
        
        try {
            String action = args[0].toLowerCase();
            
            CollaborationAgent agent = AgentManager.getInstance().getActiveAgent();
            if (agent == null) {
                return ResponseHelper.error("agentAction", ResponseHelper.ERROR_NO_AGENT, "No active agent");
            }
            
            AgentActionResult result = executeAgentAction(agent, action, args);
            
            if (result.success) {
                metricsCollector.incrementCounter("commands.agentAction");
                return ResponseHelper.agentActionResult(action, result.message);
            } else {
                return ResponseHelper.error("agentAction", ResponseHelper.ERROR_ACTION_FAILED, result.message);
            }
        } catch (Exception e) {
            LOGGER.error("Error executing agent action", e);
            return ResponseHelper.error("agentAction", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
    
    /**
     * Execute specific agent action
     * @param agent The agent to perform action on
     * @param action The action to perform
     * @param args Command arguments
     * @return Action result
     */
    private AgentActionResult executeAgentAction(CollaborationAgent agent, String action, String[] args) {
        switch (action) {
            case "dig":
            case "mine":
                return executeDigAction(agent);
            case "place":
            case "build":
                return executePlaceAction(agent, args);
            case "collect":
            case "pickup":
                return executeCollectAction(agent);
            case "drop":
                return executeDropAction(agent);
            case "turn":
                return executeTurnAction(agent, args);
            default:
                return new AgentActionResult(false, "Unknown action: " + action);
        }
    }
    
    /**
     * Execute dig/mine action
     * @param agent The agent
     * @return Action result
     */
    private AgentActionResult executeDigAction(CollaborationAgent agent) {
        boolean success = agent.digForward();
        String message = success ? "Block dug" : "Cannot dig here";
        return new AgentActionResult(success, message);
    }
    
    /**
     * Execute place/build action
     * @param agent The agent
     * @param args Command arguments
     * @return Action result
     */
    private AgentActionResult executePlaceAction(CollaborationAgent agent, String[] args) {
        String blockType = args.length > 1 ? args[1] : "stone";
        boolean success = agent.placeBlock(blockType);
        String message = success ? "Block placed" : "Cannot place block";
        return new AgentActionResult(success, message);
    }
    
    /**
     * Execute collect/pickup action
     * @param agent The agent
     * @return Action result
     */
    private AgentActionResult executeCollectAction(CollaborationAgent agent) {
        boolean success = agent.collectItems();
        String message = success ? "Items collected" : "No items to collect";
        return new AgentActionResult(success, message);
    }
    
    /**
     * Execute drop action
     * @param agent The agent
     * @return Action result
     */
    private AgentActionResult executeDropAction(CollaborationAgent agent) {
        boolean success = agent.dropItems();
        String message = success ? "Items dropped" : "No items to drop";
        return new AgentActionResult(success, message);
    }
    
    /**
     * Execute turn action
     * @param agent The agent
     * @param args Command arguments
     * @return Action result
     */
    private AgentActionResult executeTurnAction(CollaborationAgent agent, String[] args) {
        String direction = args.length > 1 ? args[1] : "right";
        boolean success = agent.turn(direction);
        String message = success ? "Turned " + direction : "Cannot turn";
        return new AgentActionResult(success, message);
    }
    
    /**
     * Helper class to encapsulate action results
     */
    private static class AgentActionResult {
        private final boolean success;
        private final String message;
        
        AgentActionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Handle dismiss agent command
     */
    public String handleDismissAgent(String[] args) {
        try {
            CollaborationAgent agent = AgentManager.getInstance().getActiveAgent();
            if (agent == null) {
                return ResponseHelper.error("dismissAgent", ResponseHelper.ERROR_NO_AGENT, "No active agent");
            }
            
            String agentName = agent.getName().getString();
            boolean success = AgentManager.getInstance().dismissAgent(agent.getAgentIdString());
            
            if (success) {
                metricsCollector.incrementCounter("commands.dismissAgent");
                return ResponseHelper.agentDismissed(agentName);
            } else {
                return ResponseHelper.error("dismissAgent", ResponseHelper.ERROR_DISMISS_FAILED, "Failed to dismiss agent");
            }
        } catch (Exception e) {
            LOGGER.error("Error dismissing agent", e);
            return ResponseHelper.error("dismissAgent", ResponseHelper.ERROR_INTERNAL, e.getMessage());
        }
    }
}