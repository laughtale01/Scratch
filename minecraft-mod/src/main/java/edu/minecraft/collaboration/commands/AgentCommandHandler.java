package edu.minecraft.collaboration.commands;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import edu.minecraft.collaboration.util.ValidationUtils;
import edu.minecraft.collaboration.util.ResponseHelper;
import edu.minecraft.collaboration.entities.AgentManager;
import edu.minecraft.collaboration.entities.CollaborationAgent;
import edu.minecraft.collaboration.monitoring.MetricsCollector;

import java.util.UUID;

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
                // Move to specific coordinates
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                int z = Integer.parseInt(args[2]);
                
                if (!ValidationUtils.isValidCoordinate(x, y, z)) {
                    return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_COORDS, "Invalid coordinates");
                }
                
                BlockPos targetPos = new BlockPos(x, y, z);
                // Convert BlockPos to Vec3 for moveToTarget method
                boolean success = agent.moveToTarget(Vec3.atCenterOf(targetPos));
                
                if (success) {
                    metricsCollector.incrementCounter("commands.moveAgent");
                    return ResponseHelper.agentMoved(x, y, z);
                } else {
                    return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_MOVE_FAILED, "Cannot move to that position");
                }
            } else {
                // Move in direction
                String direction = args[0].toLowerCase();
                int distance = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                
                if (distance < 1 || distance > 50) {
                    return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_ARGS, "Distance must be between 1 and 50");
                }
                
                BlockPos currentPos = agent.blockPosition();
                BlockPos targetPos;
                
                switch (direction) {
                    case "forward":
                    case "north":
                        targetPos = currentPos.north(distance);
                        break;
                    case "back":
                    case "south":
                        targetPos = currentPos.south(distance);
                        break;
                    case "left":
                    case "west":
                        targetPos = currentPos.west(distance);
                        break;
                    case "right":
                    case "east":
                        targetPos = currentPos.east(distance);
                        break;
                    case "up":
                        targetPos = currentPos.above(distance);
                        break;
                    case "down":
                        targetPos = currentPos.below(distance);
                        break;
                    default:
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
        } catch (NumberFormatException e) {
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INVALID_ARGS, "Invalid number format");
        } catch (Exception e) {
            LOGGER.error("Error moving agent", e);
            return ResponseHelper.error("moveAgent", ResponseHelper.ERROR_INTERNAL, e.getMessage());
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
            
            boolean success = false;
            String result = "";
            
            switch (action) {
                case "dig":
                case "mine":
                    success = agent.digForward();
                    result = success ? "Block dug" : "Cannot dig here";
                    break;
                case "place":
                case "build":
                    String blockType = args.length > 1 ? args[1] : "stone";
                    success = agent.placeBlock(blockType);
                    result = success ? "Block placed" : "Cannot place block";
                    break;
                case "collect":
                case "pickup":
                    success = agent.collectItems();
                    result = success ? "Items collected" : "No items to collect";
                    break;
                case "drop":
                    success = agent.dropItems();
                    result = success ? "Items dropped" : "No items to drop";
                    break;
                case "turn":
                    String direction = args.length > 1 ? args[1] : "right";
                    success = agent.turn(direction);
                    result = success ? "Turned " + direction : "Cannot turn";
                    break;
                default:
                    return ResponseHelper.error("agentAction", ResponseHelper.ERROR_INVALID_ARGS, "Unknown action: " + action);
            }
            
            if (success) {
                metricsCollector.incrementCounter("commands.agentAction");
                return ResponseHelper.agentActionResult(action, result);
            } else {
                return ResponseHelper.error("agentAction", ResponseHelper.ERROR_ACTION_FAILED, result);
            }
        } catch (Exception e) {
            LOGGER.error("Error executing agent action", e);
            return ResponseHelper.error("agentAction", ResponseHelper.ERROR_INTERNAL, e.getMessage());
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