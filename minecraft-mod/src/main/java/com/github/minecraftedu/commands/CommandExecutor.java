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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CommandExecutor {

    private final MinecraftServer server;
    private JsonObject lastResult;

    public CommandExecutor(MinecraftServer server) {
        this.server = server;
        this.lastResult = new JsonObject();
    }

    public boolean execute(String action, JsonObject params) {
        try {
            lastResult = new JsonObject();

            switch (action) {
                case "chat":
                    return executeChat(params);

                case "setBlock":
                    return executeSetBlock(params);

                case "getBlock":
                    return executeGetBlock(params);

                case "fillBlocks":
                    return executeFillBlocks(params);

                case "getPosition":
                    return executeGetPosition(params);

                case "summonEntity":
                    return executeSummonEntity(params);

                case "teleport":
                    return executeTeleport(params);

                case "setWeather":
                    return executeSetWeather(params);

                case "setTime":
                    return executeSetTime(params);

                case "setGameMode":
                    return executeSetGameMode(params);

                case "clearArea":
                    return executeClearArea(params);

                case "clearAllEntities":
                    return executeClearAllEntities(params);

                default:
                    MinecraftEduMod.LOGGER.warn("Unknown command: " + action);
                    return false;
            }
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Error executing command: " + action, e);
            return false;
        }
    }

    public JsonObject getLastResult() {
        return lastResult;
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

        // ブロック状態を解析（プロパティを含む）
        BlockState blockState = parseBlockState(blockType);

        if (blockState == null) {
            MinecraftEduMod.LOGGER.warn("Failed to parse block state: " + blockType);
            return false;
        }

        BlockPos pos = new BlockPos(x, y, z);

        // ブロック配置
        server.execute(() -> {
            ServerLevel world = server.overworld();
            world.setBlock(pos, blockState, 3);
        });

        MinecraftEduMod.LOGGER.info("Block placed: " + blockType + " at " + x + "," + y + "," + z);

        // 結果データを設定
        lastResult.addProperty("blockPlaced", true);
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        position.addProperty("z", z);
        lastResult.add("position", position);

        return true;
    }

    private boolean executeGetBlock(JsonObject params) {
        int x = params.get("x").getAsInt();
        int y = params.get("y").getAsInt();
        int z = params.get("z").getAsInt();

        BlockPos pos = new BlockPos(x, y, z);
        ServerLevel world = server.overworld();

        // ブロック情報取得
        BlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);

        if (blockId == null) {
            MinecraftEduMod.LOGGER.warn("Failed to get block ID at " + x + "," + y + "," + z);
            return false;
        }

        MinecraftEduMod.LOGGER.info("Block retrieved: " + blockId + " at " + x + "," + y + "," + z);

        // 結果データを設定
        lastResult.addProperty("blockType", blockId.toString());
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        position.addProperty("z", z);
        lastResult.add("position", position);
        lastResult.add("blockState", new JsonObject());

        return true;
    }

    private boolean executeFillBlocks(JsonObject params) {
        // fromとtoの座標を取得
        JsonObject from = params.getAsJsonObject("from");
        JsonObject to = params.getAsJsonObject("to");
        String blockType = params.get("blockType").getAsString();

        int fromX = from.get("x").getAsInt();
        int fromY = from.get("y").getAsInt();
        int fromZ = from.get("z").getAsInt();

        int toX = to.get("x").getAsInt();
        int toY = to.get("y").getAsInt();
        int toZ = to.get("z").getAsInt();

        // 座標範囲を正規化（小さい方から大きい方へ）
        int minX = Math.min(fromX, toX);
        int maxX = Math.max(fromX, toX);
        int minY = Math.min(fromY, toY);
        int maxY = Math.max(fromY, toY);
        int minZ = Math.min(fromZ, toZ);
        int maxZ = Math.max(fromZ, toZ);

        // 範囲チェック（最大200000ブロックまで）
        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > 200000) {
            MinecraftEduMod.LOGGER.warn("Fill volume too large: " + volume + " blocks (max: 200000)");
            return false;
        }

        // ブロック状態を解析（プロパティを含む）
        BlockState blockState = parseBlockState(blockType);

        if (blockState == null) {
            MinecraftEduMod.LOGGER.warn("Failed to parse block state: " + blockType);
            return false;
        }

        // ブロック配置
        server.execute(() -> {
            ServerLevel world = server.overworld();
            int placedCount = 0;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        world.setBlock(pos, blockState, 3);
                        placedCount++;
                    }
                }
            }

            MinecraftEduMod.LOGGER.info("Filled " + placedCount + " blocks with " + blockType);
        });

        // 結果データを設定
        lastResult.addProperty("blocksFilled", volume);
        lastResult.addProperty("blockType", blockType);
        JsonObject fromPos = new JsonObject();
        fromPos.addProperty("x", minX);
        fromPos.addProperty("y", minY);
        fromPos.addProperty("z", minZ);
        JsonObject toPos = new JsonObject();
        toPos.addProperty("x", maxX);
        toPos.addProperty("y", maxY);
        toPos.addProperty("z", maxZ);
        lastResult.add("from", fromPos);
        lastResult.add("to", toPos);

        return true;
    }

    private boolean executeGetPosition(JsonObject params) {
        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("No player found for getPosition");
            return false;
        }

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        MinecraftEduMod.LOGGER.info("Player position: " + x + "," + y + "," + z);

        // 結果データを設定
        lastResult.addProperty("x", x);
        lastResult.addProperty("y", y);
        lastResult.addProperty("z", z);
        lastResult.addProperty("yaw", yaw);
        lastResult.addProperty("pitch", pitch);

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

    private boolean executeSetGameMode(JsonObject params) {
        String mode = params.get("mode").getAsString();

        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("No player found for setGameMode");
            return false;
        }

        GameType gameType;
        switch (mode.toLowerCase()) {
            case "survival":
                gameType = GameType.SURVIVAL;
                break;
            case "creative":
                gameType = GameType.CREATIVE;
                break;
            case "adventure":
                gameType = GameType.ADVENTURE;
                break;
            case "spectator":
                gameType = GameType.SPECTATOR;
                break;
            default:
                MinecraftEduMod.LOGGER.warn("Unknown game mode: " + mode);
                return false;
        }

        server.execute(() -> {
            player.setGameMode(gameType);
        });

        MinecraftEduMod.LOGGER.info("Game mode set to: " + mode);

        // 結果データを設定
        lastResult.addProperty("gameMode", mode);
        lastResult.addProperty("playerName", player.getName().getString());

        return true;
    }

    /**
     * ブロックタイプ文字列からBlockStateを解析する
     * 形式: "oak_stairs[half=top,facing=north]" または "stone"
     */
    private BlockState parseBlockState(String blockTypeString) {
        // [properties] 部分があるかチェック
        int bracketIndex = blockTypeString.indexOf('[');

        String blockTypeName;
        Map<String, String> properties = new HashMap<>();

        if (bracketIndex > 0) {
            // ブロック名とプロパティを分離
            blockTypeName = blockTypeString.substring(0, bracketIndex);
            String propertiesString = blockTypeString.substring(bracketIndex + 1, blockTypeString.length() - 1);

            // プロパティをパース
            if (!propertiesString.isEmpty()) {
                String[] pairs = propertiesString.split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        properties.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }
            }
        } else {
            blockTypeName = blockTypeString;
        }

        // ブロックを取得
        ResourceLocation blockId = new ResourceLocation(blockTypeName);
        Block block = BuiltInRegistries.BLOCK.get(blockId);

        if (block == null) {
            MinecraftEduMod.LOGGER.warn("Unknown block type: " + blockTypeName);
            return null;
        }

        // デフォルトのBlockStateを取得
        BlockState blockState = block.defaultBlockState();

        // プロパティを適用
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            String propertyValue = entry.getValue();

            // ブロックのプロパティを検索
            Optional<Property<?>> optionalProperty = blockState.getProperties().stream()
                .filter(p -> p.getName().equals(propertyName))
                .findFirst();

            if (optionalProperty.isPresent()) {
                blockState = setPropertyValue(blockState, optionalProperty.get(), propertyValue);
            } else {
                MinecraftEduMod.LOGGER.warn("Unknown property '" + propertyName + "' for block " + blockTypeName);
            }
        }

        return blockState;
    }

    /**
     * BlockStateに特定のプロパティ値を設定する
     */
    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> BlockState setPropertyValue(BlockState state, Property<T> property, String value) {
        Optional<T> optionalValue = property.getValue(value);
        if (optionalValue.isPresent()) {
            return state.setValue(property, optionalValue.get());
        } else {
            MinecraftEduMod.LOGGER.warn("Invalid value '" + value + "' for property '" + property.getName() + "'");
            return state;
        }
    }

    /**
     * 周囲クリア
     * X:-25～25、Y:-4～100、Z:-25～25の範囲をスーパーフラットの初期状態に戻す
     * Y=-64: 岩盤
     * Y=-63～-61: 土（2層）
     * Y=-60: 草ブロック
     * Y=-59～100: 空気
     */
    private boolean executeClearArea(JsonObject params) {
        server.execute(() -> {
            ServerLevel world = server.overworld();
            BlockState bedrock = net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState();
            BlockState dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
            BlockState grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
            BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

            int blocksCleared = 0;
            for (int x = -25; x <= 25; x++) {
                for (int y = -64; y <= 100; y++) {
                    for (int z = -25; z <= 25; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState blockToPlace;

                        if (y == -64) {
                            blockToPlace = bedrock;
                        } else if (y >= -63 && y <= -61) {
                            blockToPlace = dirt;
                        } else if (y == -60) {
                            blockToPlace = grass;
                        } else {
                            blockToPlace = air;
                        }

                        world.setBlock(pos, blockToPlace, 3);
                        blocksCleared++;
                    }
                }
            }

            MinecraftEduMod.LOGGER.info("周囲クリア完了: " + blocksCleared + "ブロック（スーパーフラット初期状態）");
        });

        lastResult.addProperty("blocksCleared", 428715);  // 51 * 165 * 51
        return true;
    }

    /**
     * 全エンティティをクリア
     * X:-25～25、Y:-4～100、Z:-25～25の範囲のエンティティを削除（プレイヤーを除く）
     */
    private boolean executeClearAllEntities(JsonObject params) {
        server.execute(() -> {
            ServerLevel world = server.overworld();

            net.minecraft.world.phys.AABB bounds = new net.minecraft.world.phys.AABB(
                -25, -4, -25,
                25, 100, 25
            );

            int entitiesRemoved = 0;
            for (net.minecraft.world.entity.Entity entity : world.getEntitiesOfClass(
                    net.minecraft.world.entity.Entity.class, bounds)) {
                // プレイヤーは除外
                if (!(entity instanceof ServerPlayer)) {
                    entity.discard();
                    entitiesRemoved++;
                }
            }

            MinecraftEduMod.LOGGER.info("エンティティクリア完了: " + entitiesRemoved + "体");
        });

        return true;
    }

    private ServerPlayer getFirstPlayer() {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return null;
        }
        return server.getPlayerList().getPlayers().get(0);
    }
}
