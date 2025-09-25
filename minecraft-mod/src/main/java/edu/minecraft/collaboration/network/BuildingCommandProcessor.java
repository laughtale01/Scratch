package edu.minecraft.collaboration.network;

import edu.minecraft.collaboration.MinecraftCollaborationMod;
import edu.minecraft.collaboration.commands.CollaborationCommandHandler;
import edu.minecraft.collaboration.util.BlockUtils;
import org.slf4j.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.Map;

/**
 * Handles building-related commands from the Scratch extension
 * Separated from CollaborationMessageProcessor to reduce complexity
 */
public class BuildingCommandProcessor {

    /**
     * Parameters for house building operations
     */
    private static class HouseBuildingParams {
        final int x, y, z;
        final int width, depth, height;
        final net.minecraft.world.level.block.Block wallBlock;
        final net.minecraft.world.level.block.Block roofBlock;
        final net.minecraft.world.level.block.Block doorBlock;
        final net.minecraft.world.level.block.Block windowBlock;
        final net.minecraft.server.level.ServerLevel world;

        HouseBuildingParams(int x, int y, int z, int width, int depth, int height,
                           net.minecraft.world.level.block.Block wallBlock,
                           net.minecraft.world.level.block.Block roofBlock,
                           net.minecraft.world.level.block.Block doorBlock,
                           net.minecraft.world.level.block.Block windowBlock,
                           net.minecraft.server.level.ServerLevel world) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = width;
            this.depth = depth;
            this.height = height;
            this.wallBlock = wallBlock;
            this.roofBlock = roofBlock;
            this.doorBlock = doorBlock;
            this.windowBlock = windowBlock;
            this.world = world;
        }
    }

    private static final Logger LOGGER = MinecraftCollaborationMod.getLogger();
    private final CollaborationCommandHandler commandHandler;
    private final Gson gson;

    public BuildingCommandProcessor(CollaborationCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
        this.gson = new Gson();
    }

    /**
     * Handle fill blocks command
     */
    public String handleFillBlocks(Map<String, String> args) {
        try {
            int x1 = Integer.parseInt(args.get("x1"));
            int y1 = Integer.parseInt(args.get("y1"));
            int z1 = Integer.parseInt(args.get("z1"));
            int x2 = Integer.parseInt(args.get("x2"));
            int y2 = Integer.parseInt(args.get("y2"));
            int z2 = Integer.parseInt(args.get("z2"));
            String blockType = args.get("block");

            // Ensure coordinates are in order
            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            // Limit area size to prevent server lag
            int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            if (volume > 10000) {
                return createErrorResponse("areaTooBig", "Area too large (max 10000 blocks)");
            }

            // Execute fill using command handler
            String[] fillArgs = new String[] {
                String.valueOf(minX), String.valueOf(minY), String.valueOf(minZ),
                String.valueOf(maxX), String.valueOf(maxY), String.valueOf(maxZ),
                blockType
            };

            return commandHandler.handleFillArea(fillArgs);

        } catch (NumberFormatException e) {
            return createErrorResponse("invalidCoordinates", "Invalid coordinates");
        } catch (Exception e) {
            LOGGER.error("Error filling blocks", e);
            return createErrorResponse("fillError", e.getMessage());
        }
    }

    /**
     * Handle build circle command
     */
    public String handleBuildCircle(Map<String, String> args) {
        try {
            int x = Integer.parseInt(args.get("x"));
            int y = Integer.parseInt(args.get("y"));
            int z = Integer.parseInt(args.get("z"));
            int radius = Integer.parseInt(args.get("radius"));
            String blockType = args.get("block");

            if (radius > 50) {
                return createErrorResponse("radiusTooBig", "Radius too large (max 50)");
            }

            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel world = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                if (world != null) {
                    net.minecraft.world.level.block.Block block = BlockUtils.getBlockFromString(blockType);

                    // Build circle using midpoint circle algorithm
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            double distance = Math.sqrt(dx * dx + dz * dz);
                            if (Math.abs(distance - radius) < 0.5) {
                                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x + dx, y, z + dz);
                                world.setBlockAndUpdate(pos, block.defaultBlockState());
                            }
                        }
                    }

                    return createSuccessResponse("buildCircle", "Built circle with radius " + radius);
                }
            }
            return createErrorResponse("circleFailed", "World not found");
        } catch (NumberFormatException e) {
            return createErrorResponse("invalidParameters", "Invalid parameters");
        } catch (Exception e) {
            LOGGER.error("Error building circle", e);
            return createErrorResponse("circleError", e.getMessage());
        }
    }

    /**
     * Handle build sphere command
     */
    public String handleBuildSphere(Map<String, String> args) {
        try {
            int centerX = Integer.parseInt(args.get("x"));
            int centerY = Integer.parseInt(args.get("y"));
            int centerZ = Integer.parseInt(args.get("z"));
            int radius = Integer.parseInt(args.get("radius"));
            String blockType = args.get("block");

            if (radius > 50) {
                return createErrorResponse("sphereTooBig", "Sphere too large (max radius 50)");
            }

            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel world = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                if (world != null) {
                    net.minecraft.world.level.block.Block block = BlockUtils.getBlockFromString(blockType);

                    // Build sphere using 3D distance formula
                    for (int x = -radius; x <= radius; x++) {
                        for (int y = -radius; y <= radius; y++) {
                            for (int z = -radius; z <= radius; z++) {
                                double distance = Math.sqrt(x * x + y * y + z * z);
                                if (distance <= radius && distance >= radius - 1) {
                                    // Only place blocks on the outer shell
                                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                                        centerX + x, centerY + y, centerZ + z
                                    );
                                    world.setBlockAndUpdate(pos, block.defaultBlockState());
                                }
                            }
                        }
                    }

                    return createSuccessResponse("buildSphere", "Built sphere with radius " + radius);
                }
            }
            return createErrorResponse("worldNotFound", "World not found");

        } catch (NumberFormatException e) {
            return createErrorResponse("invalidParameters", "Invalid parameters");
        } catch (Exception e) {
            LOGGER.error("Error building sphere", e);
            return createErrorResponse("sphereError", e.getMessage());
        }
    }

    /**
     * Handle build wall command
     */
    public String handleBuildWall(Map<String, String> args) {
        try {
            int x1 = Integer.parseInt(args.get("x1"));
            int z1 = Integer.parseInt(args.get("z1"));
            int x2 = Integer.parseInt(args.get("x2"));
            int z2 = Integer.parseInt(args.get("z2"));
            int height = Integer.parseInt(args.get("height"));
            String blockType = args.get("block");

            if (height > 50) {
                return createErrorResponse("wallTooHigh", "Wall too high (max height 50)");
            }

            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel world = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                if (world != null) {
                    net.minecraft.world.level.block.Block block = BlockUtils.getBlockFromString(blockType);

                    // Get ground level at starting position
                    int groundY = 64; // Default ground level
                    for (int y = 255; y >= 0; y--) {
                        net.minecraft.core.BlockPos checkPos = new net.minecraft.core.BlockPos(x1, y, z1);
                        if (!world.getBlockState(checkPos).isAir()) {
                            groundY = y + 1;
                            break;
                        }
                    }

                    // Build wall along the line from (x1,z1) to (x2,z2)
                    int dx = x2 - x1;
                    int dz = z2 - z1;
                    int steps = Math.max(Math.abs(dx), Math.abs(dz));

                    if (steps == 0) {
                        steps = 1;
                    }

                    for (int i = 0; i <= steps; i++) {
                        int x = x1 + (dx * i) / steps;
                        int z = z1 + (dz * i) / steps;

                        for (int y = 0; y < height; y++) {
                            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, groundY + y, z);
                            world.setBlockAndUpdate(pos, block.defaultBlockState());
                        }
                    }

                    return createSuccessResponse("buildWall", "Built wall from (" + x1 + "," + z1 + ") to (" + x2 + "," + z2 + ")");
                }
            }
            return createErrorResponse("worldNotFound", "World not found");

        } catch (NumberFormatException e) {
            return createErrorResponse("invalidParameters", "Invalid parameters");
        } catch (Exception e) {
            LOGGER.error("Error building wall", e);
            return createErrorResponse("wallError", e.getMessage());
        }
    }

    /**
     * Build the floor of the house
     */
    private void buildFloor(HouseBuildingParams params) {
        for (int dx = 0; dx < params.width; dx++) {
            for (int dz = 0; dz < params.depth; dz++) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                    params.x + dx, params.y - 1, params.z + dz);
                params.world.setBlockAndUpdate(pos, params.wallBlock.defaultBlockState());
            }
        }
    }

    /**
     * Build the walls of the house with door and window placement
     */
    private void buildWalls(HouseBuildingParams params) {
        for (int dy = 0; dy < params.height; dy++) {
            buildWallsAtHeight(params, dy);
        }
    }

    /**
     * Build walls at a specific height with door and window logic
     */
    private void buildWallsAtHeight(HouseBuildingParams params, int dy) {
        for (int dx = 0; dx < params.width; dx++) {
            for (int dz = 0; dz < params.depth; dz++) {
                if (isWallPosition(dx, dz, params.width, params.depth)) {
                    net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                        params.x + dx, params.y + dy, params.z + dz);

                    net.minecraft.world.level.block.state.BlockState blockState =
                        getBlockStateForWallPosition(params, dx, dz, dy);
                    params.world.setBlockAndUpdate(pos, blockState);
                }
            }
        }
    }

    /**
     * Determine if a position is on the wall (perimeter)
     */
    private boolean isWallPosition(int dx, int dz, int width, int depth) {
        return dx == 0 || dx == width - 1 || dz == 0 || dz == depth - 1;
    }

    /**
     * Get the appropriate block state for a wall position (door, window, or wall)
     */
    private net.minecraft.world.level.block.state.BlockState getBlockStateForWallPosition(
            HouseBuildingParams params, int dx, int dz, int dy) {

        // Add door on front wall
        if (isDoorPosition(dx, dz, dy, params.width, params.height)) {
            return params.doorBlock.defaultBlockState();
        }

        // Add windows
        if (isWindowPosition(dx, dz, dy, params.width, params.depth, params.height)) {
            return params.windowBlock.defaultBlockState();
        }

        // Default wall block
        return params.wallBlock.defaultBlockState();
    }

    /**
     * Check if current position should have a door
     */
    private boolean isDoorPosition(int dx, int dz, int dy, int width, int height) {
        return dy < 2 && dx == width / 2 && dz == 0 && dy == 0;
    }

    /**
     * Check if current position should have a window
     */
    private boolean isWindowPosition(int dx, int dz, int dy, int width, int depth, int height) {
        return dy == height / 2
               && (dx == 1 || dx == width - 2)
               && (dz == 0 || dz == depth - 1);
    }

    /**
     * Build the roof of the house
     */
    private void buildRoof(HouseBuildingParams params) {
        for (int dx = -1; dx <= params.width; dx++) {
            for (int dz = -1; dz <= params.depth; dz++) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
                    params.x + dx, params.y + params.height, params.z + dz);
                params.world.setBlockAndUpdate(pos, params.roofBlock.defaultBlockState());
            }
        }
    }

    /**
     * Handle build house command
     */
    public String handleBuildHouse(Map<String, String> args) {
        try {
            // Parse parameters
            int x = Integer.parseInt(args.get("x"));
            int y = Integer.parseInt(args.get("y"));
            int z = Integer.parseInt(args.get("z"));
            int width = Integer.parseInt(args.get("width"));
            int depth = Integer.parseInt(args.get("depth"));
            int height = Integer.parseInt(args.get("height"));
            String blockType = args.get("block");

            // Validate house size
            if (width > 30 || depth > 30 || height > 20) {
                return createErrorResponse("houseTooBig", "House too large (max 30x30x20)");
            }

            // Get world
            net.minecraft.server.level.ServerLevel world = getOverworld();
            if (world == null) {
                return createErrorResponse("houseFailed", "World not found");
            }

            // Create building parameters
            HouseBuildingParams params = createHouseBuildingParams(
                x, y, z, width, depth, height, blockType, world);

            // Build house components
            buildFloor(params);
            buildWalls(params);
            buildRoof(params);

            return createSuccessResponse("buildHouse", "Built house " + width + "x" + depth + "x" + height);

        } catch (NumberFormatException e) {
            return createErrorResponse("invalidParameters", "Invalid parameters");
        } catch (Exception e) {
            LOGGER.error("Error building house", e);
            return createErrorResponse("houseError", e.getMessage());
        }
    }

    /**
     * Get the overworld level
     */
    private net.minecraft.server.level.ServerLevel getOverworld() {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
        }
        return null;
    }

    /**
     * Create house building parameters
     */
    private HouseBuildingParams createHouseBuildingParams(int x, int y, int z, int width, int depth,
                                                         int height, String blockType,
                                                         net.minecraft.server.level.ServerLevel world) {
        net.minecraft.world.level.block.Block wallBlock = BlockUtils.getBlockFromString(blockType);
        net.minecraft.world.level.block.Block roofBlock = net.minecraft.world.level.block.Blocks.OAK_PLANKS;
        net.minecraft.world.level.block.Block doorBlock = net.minecraft.world.level.block.Blocks.OAK_DOOR;
        net.minecraft.world.level.block.Block windowBlock = net.minecraft.world.level.block.Blocks.GLASS_PANE;

        return new HouseBuildingParams(x, y, z, width, depth, height,
                                     wallBlock, roofBlock, doorBlock, windowBlock, world);
    }

    /**
     * Handle set time command
     */
    public String handleSetTime(Map<String, String> args) {
        try {
            String timeStr = args.get("time");
            long time;

            switch (timeStr.toLowerCase()) {
                case "day":
                    time = 1000;
                    break;
                case "night":
                    time = 13000;
                    break;
                case "noon":
                    time = 6000;
                    break;
                case "midnight":
                    time = 18000;
                    break;
                case "sunrise":
                    time = 23000;
                    break;
                case "sunset":
                    time = 12000;
                    break;
                default:
                    // Try to parse as number
                    try {
                        time = Long.parseLong(timeStr);
                    } catch (NumberFormatException e) {
                        return createErrorResponse("invalidTime", "Unknown time: " + timeStr);
                    }
            }

            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel world = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                if (world != null) {
                    world.setDayTime(time);
                    return createSuccessResponse("time", "Set time to " + timeStr);
                }
            }
            return createErrorResponse("timeFailed", "World not found");
        } catch (Exception e) {
            LOGGER.error("Error setting time", e);
            return createErrorResponse("timeError", e.getMessage());
        }
    }

    /**
     * Handle set weather command
     */
    public String handleSetWeather(Map<String, String> args) {
        try {
            String weather = args.get("weather");

            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                net.minecraft.server.level.ServerLevel world = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                if (world != null) {
                    switch (weather.toLowerCase()) {
                        case "clear":
                            world.setWeatherParameters(6000, 0, false, false);
                            break;
                        case "rain":
                            world.setWeatherParameters(0, 6000, true, false);
                            break;
                        case "thunder":
                            world.setWeatherParameters(0, 6000, true, true);
                            break;
                        default:
                            return createErrorResponse("invalidWeather", "Unknown weather: " + weather);
                    }
                    return createSuccessResponse("weather", "Set weather to " + weather);
                }
            }
            return createErrorResponse("weatherFailed", "World not found");
        } catch (Exception e) {
            LOGGER.error("Error setting weather", e);
            return createErrorResponse("weatherError", e.getMessage());
        }
    }

    /**
     * Handle teleport player command
     */
    public String handleTeleportPlayer(Map<String, String> args) {
        try {
            int x = Integer.parseInt(args.get("x"));
            int y = Integer.parseInt(args.get("y"));
            int z = Integer.parseInt(args.get("z"));

            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
                if (!players.isEmpty()) {
                    net.minecraft.server.level.ServerPlayer player = players.get(0);
                    player.teleportTo(x + 0.5, y, z + 0.5);
                    return createSuccessResponse("teleport", "Teleported to " + x + "," + y + "," + z);
                }
            }
            return createErrorResponse("teleportFailed", "No players online");
        } catch (NumberFormatException e) {
            return createErrorResponse("invalidCoordinates", "Invalid coordinates");
        } catch (Exception e) {
            LOGGER.error("Error teleporting player", e);
            return createErrorResponse("teleportError", e.getMessage());
        }
    }

    /**
     * Handle set game mode command
     */
    public String handleSetGameMode(Map<String, String> args) {
        try {
            String mode = args.get("mode");
            net.minecraft.world.level.GameType gameType;

            switch (mode.toLowerCase()) {
                case "survival":
                    gameType = net.minecraft.world.level.GameType.SURVIVAL;
                    break;
                case "creative":
                    gameType = net.minecraft.world.level.GameType.CREATIVE;
                    break;
                case "adventure":
                    gameType = net.minecraft.world.level.GameType.ADVENTURE;
                    break;
                case "spectator":
                    gameType = net.minecraft.world.level.GameType.SPECTATOR;
                    break;
                default:
                    return createErrorResponse("invalidGameMode", "Unknown game mode: " + mode);
            }

            net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
                if (!players.isEmpty()) {
                    net.minecraft.server.level.ServerPlayer player = players.get(0);
                    player.setGameMode(gameType);
                    return createSuccessResponse("gamemode", "Changed to " + mode);
                }
            }
            return createErrorResponse("gameModeFailed", "No players online");
        } catch (Exception e) {
            LOGGER.error("Error changing game mode", e);
            return createErrorResponse("gameModeError", e.getMessage());
        }
    }

    // Helper methods for creating responses
    private String createErrorResponse(String errorType, String details) {
        JsonObject response = new JsonObject();
        response.addProperty("type", "error");
        response.addProperty("error", errorType);
        response.addProperty("message", details);
        return gson.toJson(response);
    }

    private String createSuccessResponse(String type, String message) {
        JsonObject response = new JsonObject();
        response.addProperty("type", type);
        response.addProperty("status", "success");
        response.addProperty("message", message);
        return gson.toJson(response);
    }
}
