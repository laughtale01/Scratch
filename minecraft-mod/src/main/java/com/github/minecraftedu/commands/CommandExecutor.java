package com.github.minecraftedu.commands;

import com.github.minecraftedu.MinecraftEduMod;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class CommandExecutor {

    private final MinecraftServer server;

    public CommandExecutor(MinecraftServer server) {
        this.server = server;
    }

    public boolean execute(String action, JsonObject params) {
        try {
            switch (action) {
                case "chat":
                    return executeChat(params);

                case "setBlock":
                    return executeSetBlock(params);

                case "summonEntity":
                    return executeSummonEntity(params);

                case "teleport":
                    return executeTeleport(params);

                case "setWeather":
                    return executeSetWeather(params);

                case "setTime":
                    return executeSetTime(params);

                default:
                    MinecraftEduMod.LOGGER.warn("Unknown command: " + action);
                    return false;
            }
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Error executing command: " + action, e);
            return false;
        }
    }

    private boolean executeChat(JsonObject params) {
        String message = params.get("message").getAsString();

        server.execute(() -> {
            server.getPlayerList().getPlayers().forEach(player -> {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            });
        });

        MinecraftEduMod.LOGGER.info("Chat message sent: " + message);
        return true;
    }

    private boolean executeSetBlock(JsonObject params) {
        String blockType = params.get("blockType").getAsString();

        // 座標取得（絶対または相対）
        int x, y, z;
        ServerPlayer player = getFirstPlayer();

        if (params.has("x")) {
            // 絶対座標
            x = params.get("x").getAsInt();
            y = params.get("y").getAsInt();
            z = params.get("z").getAsInt();
        } else {
            // 相対座標
            if (player == null) {
                MinecraftEduMod.LOGGER.warn("No player found for relative coordinates");
                return false;
            }

            int relX = params.get("relativeX").getAsInt();
            int relY = params.get("relativeY").getAsInt();
            int relZ = params.get("relativeZ").getAsInt();

            x = (int) player.getX() + relX;
            y = (int) player.getY() + relY;
            z = (int) player.getZ() + relZ;
        }

        // ブロックタイプ取得
        ResourceLocation blockId = new ResourceLocation(blockType);
        Block block = BuiltInRegistries.BLOCK.get(blockId);

        if (block == null) {
            MinecraftEduMod.LOGGER.warn("Unknown block type: " + blockType);
            return false;
        }

        BlockState blockState = block.defaultBlockState();
        BlockPos pos = new BlockPos(x, y, z);

        // ブロック配置
        server.execute(() -> {
            ServerLevel world = server.overworld();
            world.setBlock(pos, blockState, 3);
        });

        MinecraftEduMod.LOGGER.info("Block placed: " + blockType + " at " + x + "," + y + "," + z);
        return true;
    }

    private boolean executeSummonEntity(JsonObject params) {
        String entityType = params.get("entityType").getAsString();
        double x = params.get("x").getAsDouble();
        double y = params.get("y").getAsDouble();
        double z = params.get("z").getAsDouble();

        ResourceLocation entityId = new ResourceLocation(entityType);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);

        if (type == null) {
            MinecraftEduMod.LOGGER.warn("Unknown entity type: " + entityType);
            return false;
        }

        server.execute(() -> {
            ServerLevel world = server.overworld();
            net.minecraft.world.entity.Entity entity = type.create(world);

            if (entity != null) {
                entity.setPos(x, y, z);
                world.addFreshEntity(entity);
            }
        });

        MinecraftEduMod.LOGGER.info("Entity summoned: " + entityType + " at " + x + "," + y + "," + z);
        return true;
    }

    private boolean executeTeleport(JsonObject params) {
        double x = params.get("x").getAsDouble();
        double y = params.get("y").getAsDouble();
        double z = params.get("z").getAsDouble();

        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("No player found for teleport");
            return false;
        }

        server.execute(() -> {
            player.teleportTo(x, y, z);
        });

        MinecraftEduMod.LOGGER.info("Player teleported to " + x + "," + y + "," + z);
        return true;
    }

    private boolean executeSetWeather(JsonObject params) {
        String weather = params.get("weather").getAsString();

        server.execute(() -> {
            ServerLevel world = server.overworld();

            switch (weather) {
                case "clear":
                    world.setWeatherParameters(6000, 0, false, false);
                    break;
                case "rain":
                    world.setWeatherParameters(0, 6000, true, false);
                    break;
                case "thunder":
                    world.setWeatherParameters(0, 6000, true, true);
                    break;
            }
        });

        MinecraftEduMod.LOGGER.info("Weather set to: " + weather);
        return true;
    }

    private boolean executeSetTime(JsonObject params) {
        long time = params.get("time").getAsLong();

        server.execute(() -> {
            ServerLevel world = server.overworld();
            world.setDayTime(time);
        });

        MinecraftEduMod.LOGGER.info("Time set to: " + time);
        return true;
    }

    private ServerPlayer getFirstPlayer() {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return null;
        }
        return server.getPlayerList().getPlayers().get(0);
    }
}
