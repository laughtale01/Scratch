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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CommandExecutor {

    private final MinecraftServer server;
    private JsonObject lastResult;

    // 録画関連のフィールド
    private Process ffmpegProcess;
    private String currentRecordingPath;
    private boolean isRecording;

    // エンティティ召喚制御フラグ
    private boolean entitySpawningAllowed = true;

    public CommandExecutor(MinecraftServer server) {
        this.server = server;
        this.lastResult = new JsonObject();
        this.isRecording = false;
        this.ffmpegProcess = null;
        this.currentRecordingPath = null;
        this.entitySpawningAllowed = true;  // デフォルトは許可
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

                case "getPlayerFacing":
                    return executeGetPlayerFacing(params);

                case "getBlockType":
                    return executeGetBlockType(params);

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

                case "setGameRule":
                    return executeSetGameRule(params);

                case "setMoveSpeed":
                    return executeSetMoveSpeed(params);

                case "setNightVision":
                    return executeSetNightVision(params);

                case "setFlySpeed":
                    return executeSetFlySpeed(params);

                case "startRecording":
                    return executeStartRecording(params);

                case "stopRecording":
                    return executeStopRecording(params);

                case "getRecordingStatus":
                    return executeGetRecordingStatus(params);

                case "setEntitySpawning":
                    return executeSetEntitySpawning(params);

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

        // 範囲チェック（最大2000000ブロックまで）
        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume > 2000000) {
            MinecraftEduMod.LOGGER.warn("Fill volume too large: " + volume + " blocks (max: 2000000)");
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

    private boolean executeGetPlayerFacing(JsonObject params) {
        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("No player found for getPlayerFacing");
            return false;
        }

        // プレーヤーの向き（yaw）から方向を判定
        // Yaw: 0=South, 90=West, 180=North, 270=East
        float yaw = player.getYRot();

        // Yawを0～360の範囲に正規化
        yaw = yaw % 360;
        if (yaw < 0) {
            yaw += 360;
        }

        String facing;
        if (yaw >= 315 || yaw < 45) {
            facing = "south";
        } else if (yaw >= 45 && yaw < 135) {
            facing = "west";
        } else if (yaw >= 135 && yaw < 225) {
            facing = "north";
        } else {
            facing = "east";
        }

        MinecraftEduMod.LOGGER.info("Player facing: " + facing + " (yaw: " + yaw + ")");

        // 結果データを設定
        lastResult.addProperty("facing", facing);
        lastResult.addProperty("yaw", yaw);

        return true;
    }

    private boolean executeGetBlockType(JsonObject params) {
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

        // "minecraft:" プレフィックスを削除してシンプルな形式にする
        String blockType = blockId.toString();
        if (blockType.startsWith("minecraft:")) {
            blockType = blockType.substring(10);
        }

        MinecraftEduMod.LOGGER.info("Block type retrieved: " + blockType + " at " + x + "," + y + "," + z);

        // 結果データを設定
        lastResult.addProperty("blockType", blockType);
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        position.addProperty("z", z);
        lastResult.add("position", position);

        return true;
    }

    private boolean executeSummonEntity(JsonObject params) {
        // エンティティ召喚が禁止されている場合は拒否
        if (!entitySpawningAllowed) {
            MinecraftEduMod.LOGGER.info("Entity spawning is disabled - command rejected");
            lastResult.addProperty("error", "Entity spawning is disabled");
            return false;
        }

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
     * ゲームルール設定
     *
     * 重要: Scratchのメニューの意図とMinecraftのゲームルールの意味が逆なので、
     * doDaylightCycleとdoWeatherCycleは値を反転させる必要があります。
     *
     * Scratchの意図:
     *   - 「時間固定ON」= 時間を止めたい
     *   - 「天気固定ON」= 天気を固定したい
     *
     * Minecraftの仕様:
     *   - doDaylightCycle = false で時間が止まる
     *   - doWeatherCycle = false で天気が固定される
     *
     * 変換:
     *   - Scratchの「ON (true)」→ Minecraftの「false」
     *   - Scratchの「OFF (false)」→ Minecraftの「true」
     */
    private boolean executeSetGameRule(JsonObject params) {
        String rule = params.get("rule").getAsString();
        String value = params.get("value").getAsString();
        boolean boolValue = value.equalsIgnoreCase("true");

        server.execute(() -> {
            ServerLevel world = server.overworld();
            net.minecraft.world.level.GameRules gameRules = world.getGameRules();

            switch (rule) {
                case "doDaylightCycle":
                    // Scratchの「時間固定ON」= Minecraftの「doDaylightCycle false」
                    // 値を反転: ON (true) → false, OFF (false) → true
                    boolean invertedDaylightValue = !boolValue;
                    gameRules.getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                        .set(invertedDaylightValue, server);
                    MinecraftEduMod.LOGGER.info("GameRule set: doDaylightCycle = " + invertedDaylightValue + " (Scratch value: " + value + ")");
                    break;
                case "doWeatherCycle":
                    // Scratchの「天気固定ON」= Minecraftの「doWeatherCycle false」
                    // 値を反転: ON (true) → false, OFF (false) → true
                    boolean invertedWeatherValue = !boolValue;
                    gameRules.getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE)
                        .set(invertedWeatherValue, server);
                    MinecraftEduMod.LOGGER.info("GameRule set: doWeatherCycle = " + invertedWeatherValue + " (Scratch value: " + value + ")");
                    break;
                case "doMobSpawning":
                    // モブスポーン: 反転不要（Scratchの意図とMinecraftの仕様が一致）
                    gameRules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)
                        .set(boolValue, server);
                    MinecraftEduMod.LOGGER.info("GameRule set: doMobSpawning = " + boolValue);
                    break;
                default:
                    MinecraftEduMod.LOGGER.warn("Unknown game rule: " + rule);
                    return;
            }
        });

        // 結果データを設定
        lastResult.addProperty("gameRule", rule);
        lastResult.addProperty("value", value);
        lastResult.addProperty("success", true);

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
     * X:-50～50、Y:-64～100、Z:-50～50の範囲をスーパーフラットの初期状態に戻す
     * Y=-64: 岩盤
     * Y=-63～-62: 土（2層）
     * Y=-61: 草ブロック
     * Y=-60～100: 空気
     */
    private boolean executeClearArea(JsonObject params) {
        // 中心座標を取得（デフォルト: 0, 0）
        int centerX = params.has("centerX") ? params.get("centerX").getAsInt() : 0;
        int centerZ = params.has("centerZ") ? params.get("centerZ").getAsInt() : 0;

        server.execute(() -> {
            ServerLevel world = server.overworld();
            BlockState bedrock = net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState();
            BlockState dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
            BlockState grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
            BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();

            // 中心座標から±50の範囲
            int minX = centerX - 50;
            int maxX = centerX + 50;
            int minZ = centerZ - 50;
            int maxZ = centerZ + 50;

            int blocksCleared = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int y = -64; y <= 100; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState blockToPlace;

                        if (y == -64) {
                            blockToPlace = bedrock;
                        } else if (y >= -63 && y <= -62) {
                            blockToPlace = dirt;
                        } else if (y == -61) {
                            blockToPlace = grass;
                        } else {
                            blockToPlace = air;
                        }

                        world.setBlock(pos, blockToPlace, 3);
                        blocksCleared++;
                    }
                }
            }

            MinecraftEduMod.LOGGER.info("周囲クリア完了: 中心(" + centerX + ", " + centerZ + ") から " + blocksCleared + "ブロック（スーパーフラット初期状態）");
        });

        lastResult.addProperty("blocksCleared", 1683165);  // 101 * 165 * 101
        lastResult.addProperty("centerX", centerX);
        lastResult.addProperty("centerZ", centerZ);
        return true;
    }

    /**
     * 全エンティティをクリア
     * X:-50～50、Y:-64～100、Z:-50～50の範囲のエンティティを削除（プレイヤーを除く）
     */
    private boolean executeClearAllEntities(JsonObject params) {
        // 中心座標を取得（デフォルト: 0, 0）
        int centerX = params.has("centerX") ? params.get("centerX").getAsInt() : 0;
        int centerZ = params.has("centerZ") ? params.get("centerZ").getAsInt() : 0;

        server.execute(() -> {
            ServerLevel world = server.overworld();

            // 中心座標から±50の範囲
            net.minecraft.world.phys.AABB bounds = new net.minecraft.world.phys.AABB(
                centerX - 50, -64, centerZ - 50,
                centerX + 50, 100, centerZ + 50
            );

            int entitiesRemoved = 0;
            for (net.minecraft.world.entity.Entity entity : world.getEntitiesOfClass(
                    net.minecraft.world.entity.Entity.class, bounds)) {
                // プレイヤーは除外
                if (!(entity instanceof ServerPlayer)) {
                    String entityType = entity.getType().toString();
                    MinecraftEduMod.LOGGER.info("エンティティ削除: " + entityType + " at " + entity.blockPosition());

                    // より確実な削除方法：remove()とkill()を両方試行
                    entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                    entity.kill();
                    entitiesRemoved++;
                }
            }

            MinecraftEduMod.LOGGER.info("エンティティクリア完了: 中心(" + centerX + ", " + centerZ + ") から " + entitiesRemoved + "体");
        });

        lastResult.addProperty("entitiesRemoved", true);
        lastResult.addProperty("centerX", centerX);
        lastResult.addProperty("centerZ", centerZ);
        return true;
    }

    /**
     * プレイヤーの移動速度を設定
     * @param params multiplier: 速度倍率（小数点1位まで有効、例: 0.5, 1.0, 2.5）
     * @return 成功時true
     */
    private boolean executeSetMoveSpeed(JsonObject params) {
        if (!params.has("multiplier")) {
            MinecraftEduMod.LOGGER.warn("setMoveSpeed: multiplier parameter required");
            return false;
        }

        // 小数点1位まで有効にするため、0.1刻みで丸める
        double rawMultiplier = params.get("multiplier").getAsDouble();
        double multiplier = Math.round(rawMultiplier * 10.0) / 10.0;

        // 安全な範囲に制限（0.1〜10.0倍）
        multiplier = Math.max(0.1, Math.min(10.0, multiplier));

        // デフォルト移動速度は0.1、倍率を適用
        final double baseSpeed = 0.1;
        final double newSpeed = baseSpeed * multiplier;
        final double finalMultiplier = multiplier;

        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("setMoveSpeed: No player found");
            return false;
        }

        server.execute(() -> {
            // 全プレイヤーに適用
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);
            }
            MinecraftEduMod.LOGGER.info("SetMoveSpeed: " + finalMultiplier + "x (speed=" + newSpeed + ")");
        });

        lastResult.addProperty("multiplier", finalMultiplier);
        lastResult.addProperty("speed", newSpeed);

        return true;
    }

    /**
     * 暗視エフェクトの設定
     * @param params enabled: true=暗視ON、false=暗視OFF
     * @return 成功時true
     */
    private boolean executeSetNightVision(JsonObject params) {
        if (!params.has("enabled")) {
            MinecraftEduMod.LOGGER.warn("setNightVision: enabled parameter required");
            return false;
        }

        boolean enabled = params.get("enabled").getAsBoolean();

        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (enabled) {
                    // 暗視エフェクトを付与（約27時間、パーティクル非表示）
                    // duration: 999999 ticks (約13.9時間)、amplifier: 0、ambient: true、visible: false
                    MobEffectInstance effect = new MobEffectInstance(
                        MobEffects.NIGHT_VISION,
                        999999,  // 持続時間（tick）
                        0,       // 効果レベル（0=レベル1）
                        true,    // アンビエント（パーティクル小さい）
                        false    // パーティクル非表示
                    );
                    player.addEffect(effect);
                    MinecraftEduMod.LOGGER.info("Night vision enabled for player: " + player.getName().getString());
                } else {
                    // 暗視エフェクトを解除
                    player.removeEffect(MobEffects.NIGHT_VISION);
                    MinecraftEduMod.LOGGER.info("Night vision disabled for player: " + player.getName().getString());
                }
            }
        });

        lastResult.addProperty("nightVision", enabled);

        return true;
    }

    /**
     * 飛行速度の設定（クリエイティブモード用）
     * @param params multiplier: 速度倍率（例: 1.0=標準、2.0=2倍）
     * @return 成功時true
     */
    private boolean executeSetFlySpeed(JsonObject params) {
        if (!params.has("multiplier")) {
            MinecraftEduMod.LOGGER.warn("setFlySpeed: multiplier parameter required");
            return false;
        }

        double rawMultiplier = params.get("multiplier").getAsDouble();
        // 小数点1位まで丸める
        double multiplier = Math.round(rawMultiplier * 10.0) / 10.0;
        // 0.1〜10.0の範囲に制限
        multiplier = Math.max(0.1, Math.min(10.0, multiplier));

        final float baseFlySpeed = 0.05f;  // デフォルトの飛行速度
        final float newFlySpeed = (float)(baseFlySpeed * multiplier);
        final double finalMultiplier = multiplier;

        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.getAbilities().setFlyingSpeed(newFlySpeed);
                // ClientboundPlayerAbilitiesPacketを送信してクライアントに同期
                // onUpdateAbilities()は飛行速度を同期しないため、直接パケットを送信
                player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
            }
        });

        MinecraftEduMod.LOGGER.info("SetFlySpeed: " + finalMultiplier + "x (speed=" + newFlySpeed + ")");

        lastResult.addProperty("multiplier", finalMultiplier);
        lastResult.addProperty("flySpeed", newFlySpeed);

        return true;
    }

    private ServerPlayer getFirstPlayer() {
        if (server.getPlayerList().getPlayers().isEmpty()) {
            return null;
        }
        return server.getPlayerList().getPlayers().get(0);
    }

    // ========================================
    // 録画機能
    // ========================================

    /**
     * 録画を開始する
     * FFmpegを使用してMinecraftウィンドウを録画
     * @param params パラメータ（オプション: filename）
     * @return 成功時true
     */
    private boolean executeStartRecording(JsonObject params) {
        // 既に録画中の場合はエラー
        if (isRecording) {
            MinecraftEduMod.LOGGER.warn("録画は既に開始されています");
            lastResult.addProperty("error", "既に録画中です");
            lastResult.addProperty("isRecording", true);
            return false;
        }

        try {
            // 録画保存先ディレクトリを作成
            String userHome = System.getProperty("user.home");
            File recordingsDir = new File(userHome, "MinecraftRecordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs();
            }

            // ファイル名を生成（タイムスタンプ付き）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            String filename = "recording_" + timestamp + ".mp4";

            // パラメータでファイル名が指定されている場合は上書き
            if (params != null && params.has("filename")) {
                String customName = params.get("filename").getAsString();
                if (!customName.isEmpty()) {
                    // 拡張子がなければ追加
                    if (!customName.endsWith(".mp4")) {
                        customName += ".mp4";
                    }
                    filename = customName;
                }
            }

            currentRecordingPath = new File(recordingsDir, filename).getAbsolutePath();

            // FFmpegコマンドを構築（Windows GDI Grab）
            // Minecraftウィンドウをキャプチャ
            String[] command = {
                "ffmpeg",
                "-y",                           // 上書き確認なし
                "-f", "gdigrab",                // Windows画面キャプチャ
                "-framerate", "30",             // 30fps
                "-i", "title=Minecraft",        // Minecraftウィンドウ
                "-c:v", "libx264",              // H.264コーデック
                "-preset", "ultrafast",         // 高速エンコード
                "-crf", "23",                   // 画質（23=バランス良い）
                "-pix_fmt", "yuv420p",          // 互換性のあるピクセルフォーマット
                currentRecordingPath
            };

            // FFmpegプロセスを起動
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            ffmpegProcess = pb.start();

            // プロセスが正常に起動したか確認（少し待つ）
            Thread.sleep(500);
            if (!ffmpegProcess.isAlive()) {
                MinecraftEduMod.LOGGER.error("FFmpegプロセスの起動に失敗しました");
                lastResult.addProperty("error", "FFmpegの起動に失敗しました。FFmpegがインストールされているか確認してください。");
                return false;
            }

            isRecording = true;
            MinecraftEduMod.LOGGER.info("録画開始: " + currentRecordingPath);

            lastResult.addProperty("success", true);
            lastResult.addProperty("isRecording", true);
            lastResult.addProperty("filePath", currentRecordingPath);
            lastResult.addProperty("message", "録画を開始しました");

            return true;

        } catch (IOException e) {
            MinecraftEduMod.LOGGER.error("録画開始エラー: " + e.getMessage(), e);
            lastResult.addProperty("error", "FFmpegの起動に失敗しました: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            MinecraftEduMod.LOGGER.error("録画開始中断: " + e.getMessage(), e);
            lastResult.addProperty("error", "録画開始が中断されました");
            return false;
        }
    }

    /**
     * 録画を停止する
     * @param params パラメータ（未使用）
     * @return 成功時true
     */
    private boolean executeStopRecording(JsonObject params) {
        // 録画中でない場合はエラー
        if (!isRecording || ffmpegProcess == null) {
            MinecraftEduMod.LOGGER.warn("録画は開始されていません");
            lastResult.addProperty("error", "録画は開始されていません");
            lastResult.addProperty("isRecording", false);
            return false;
        }

        try {
            // FFmpegに終了シグナルを送る（'q'キーを送信）
            // Windowsでは直接終了させる
            ffmpegProcess.getOutputStream().write('q');
            ffmpegProcess.getOutputStream().flush();

            // プロセスの終了を待つ（最大5秒）
            boolean exited = ffmpegProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (!exited) {
                // タイムアウトした場合は強制終了
                ffmpegProcess.destroyForcibly();
                MinecraftEduMod.LOGGER.warn("FFmpegプロセスを強制終了しました");
            }

            isRecording = false;
            String recordedPath = currentRecordingPath;
            ffmpegProcess = null;
            currentRecordingPath = null;

            MinecraftEduMod.LOGGER.info("録画停止: " + recordedPath);

            // ファイルが存在するか確認
            File recordedFile = new File(recordedPath);
            if (recordedFile.exists()) {
                long fileSize = recordedFile.length();
                lastResult.addProperty("success", true);
                lastResult.addProperty("isRecording", false);
                lastResult.addProperty("filePath", recordedPath);
                lastResult.addProperty("fileSize", fileSize);
                lastResult.addProperty("message", "録画を停止しました");
            } else {
                lastResult.addProperty("success", true);
                lastResult.addProperty("isRecording", false);
                lastResult.addProperty("filePath", recordedPath);
                lastResult.addProperty("warning", "ファイルが見つかりません。録画時間が短すぎた可能性があります。");
            }

            return true;

        } catch (IOException e) {
            MinecraftEduMod.LOGGER.error("録画停止エラー: " + e.getMessage(), e);
            lastResult.addProperty("error", "録画の停止に失敗しました: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            MinecraftEduMod.LOGGER.error("録画停止中断: " + e.getMessage(), e);
            lastResult.addProperty("error", "録画停止が中断されました");
            return false;
        }
    }

    /**
     * 録画状態を取得する
     * @param params パラメータ（未使用）
     * @return 常にtrue
     */
    private boolean executeGetRecordingStatus(JsonObject params) {
        lastResult.addProperty("isRecording", isRecording);
        if (isRecording && currentRecordingPath != null) {
            lastResult.addProperty("filePath", currentRecordingPath);
        }
        return true;
    }

    /**
     * エンティティ召喚の許可/禁止を設定（WebSocket経由）
     * @param params enabled: true=許可, false=禁止
     * @return 常にtrue
     */
    private boolean executeSetEntitySpawning(JsonObject params) {
        boolean enabled = params.get("enabled").getAsBoolean();
        this.entitySpawningAllowed = enabled;

        MinecraftEduMod.LOGGER.info("Entity spawning " + (enabled ? "enabled" : "disabled") + " via WebSocket");
        lastResult.addProperty("entitySpawningAllowed", enabled);

        return true;
    }

    /**
     * エンティティ召喚の許可/禁止を設定（スラッシュコマンド用）
     * @param allowed true=許可, false=禁止
     */
    public void setEntitySpawningAllowed(boolean allowed) {
        this.entitySpawningAllowed = allowed;
        MinecraftEduMod.LOGGER.info("Entity spawning " + (allowed ? "enabled" : "disabled") + " via command");
    }

    /**
     * エンティティ召喚が許可されているか取得
     * @return true=許可, false=禁止
     */
    public boolean isEntitySpawningAllowed() {
        return this.entitySpawningAllowed;
    }
}
