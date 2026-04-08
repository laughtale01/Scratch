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
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CommandExecutor {

    private final MinecraftServer server;

    // 録画関連のフィールド
    private Process ffmpegProcess;
    private String currentRecordingPath;
    private boolean isRecording;

    // エンティティ召喚制御フラグ
    private boolean entitySpawningAllowed = true;

    // clearArea重複実行防止フラグ
    private volatile boolean clearAreaInProgress = false;

    // サーバ停止フラグ。trueの間は全コマンドを拒否する
    // volatile 必須: ハンドラスレッドとメインスレッドの両方からアクセスされる
    private volatile boolean serverStopping = false;

    public CommandExecutor(MinecraftServer server) {
        this.server = server;
        this.isRecording = false;
        this.ffmpegProcess = null;
        this.currentRecordingPath = null;
        this.entitySpawningAllowed = true;
    }

    /**
     * コマンドを実行し、結果をJsonObjectで返す
     * @return 成功時は結果オブジェクト、失敗時はnull
     */
    public JsonObject execute(String action, JsonObject params) {
        // サーバ停止中は何も実行しない（ハンドラスレッド入口チェック）
        if (serverStopping || !server.isRunning()) {
            MinecraftEduMod.LOGGER.warn("Command rejected (server stopping): " + action);
            return null;
        }
        try {
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

                case "getTargetBlock":
                    return executeGetTargetBlock(params);

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

                case "setContainerItem":
                    return executeSetContainerItem(params);

                default:
                    MinecraftEduMod.LOGGER.warn("Unknown command: " + action);
                    return null;
            }
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Error executing command: " + action, e);
            return null;
        }
    }

    private JsonObject executeChat(JsonObject params) {
        String message = params.get("message").getAsString();

        safeExecute(() -> {
            server.getPlayerList().getPlayers().forEach(player -> {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            });
        });

        MinecraftEduMod.LOGGER.info("Chat message sent: " + message);
        return new JsonObject();
    }

    private JsonObject executeSetBlock(JsonObject params) {
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
                return null;
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
            return null;
        }

        BlockPos pos = new BlockPos(x, y, z);

        // ブロック配置
        safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;
            world.setBlock(pos, blockState, 3);
        });

        MinecraftEduMod.LOGGER.info("Block placed: " + blockType + " at " + x + "," + y + "," + z);

        // 結果データを設定
        JsonObject result = new JsonObject();
        result.addProperty("blockPlaced", true);
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        position.addProperty("z", z);
        result.add("position", position);

        return result;
    }

    private JsonObject executeGetBlock(JsonObject params) {
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
            return null;
        }

        MinecraftEduMod.LOGGER.info("Block retrieved: " + blockId + " at " + x + "," + y + "," + z);

        // 結果データを設定
        JsonObject result = new JsonObject();
        result.addProperty("blockType", blockId.toString());
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        position.addProperty("z", z);
        result.add("position", position);
        result.add("blockState", new JsonObject());

        return result;
    }

    private JsonObject executeFillBlocks(JsonObject params) {
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
            return null;
        }

        // ブロック状態を解析（プロパティを含む）
        BlockState blockState = parseBlockState(blockType);

        if (blockState == null) {
            MinecraftEduMod.LOGGER.warn("Failed to parse block state: " + blockType);
            return null;
        }

        // ブロック配置
        safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;
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
        JsonObject result = new JsonObject();
        result.addProperty("blocksFilled", volume);
        result.addProperty("blockType", blockType);
        JsonObject fromPos = new JsonObject();
        fromPos.addProperty("x", minX);
        fromPos.addProperty("y", minY);
        fromPos.addProperty("z", minZ);
        JsonObject toPos = new JsonObject();
        toPos.addProperty("x", maxX);
        toPos.addProperty("y", maxY);
        toPos.addProperty("z", maxZ);
        result.add("from", fromPos);
        result.add("to", toPos);

        return result;
    }

    private JsonObject executeGetPosition(JsonObject params) {
        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("No player found for getPosition");
            return null;
        }

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        MinecraftEduMod.LOGGER.info("Player position: " + x + "," + y + "," + z);

        // 結果データを設定
        JsonObject result = new JsonObject();
        result.addProperty("x", x);
        result.addProperty("y", y);
        result.addProperty("z", z);
        result.addProperty("yaw", yaw);
        result.addProperty("pitch", pitch);

        return result;
    }

    /**
     * プレイヤーが見ているブロック（クロスヘアの先）の座標を取得
     * メインスレッドでレイキャストを実行（ワールドデータへのアクセスが必要なため）
     */
    private JsonObject executeGetTargetBlock(JsonObject params) {
        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("No player found for getTargetBlock");
            return null;
        }

        // レイキャスト距離（デフォルト256ブロック）
        double reach = params.has("reach") ? params.get("reach").getAsDouble() : 256.0;

        // ローカルresultをラムダと共有（future.get()のhappens-before保証により安全）
        JsonObject result = new JsonObject();

        // メインスレッドでレイキャストを実行（pick()はワールドデータにアクセスするため）
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        safeExecute(() -> {
            try {
                net.minecraft.world.phys.HitResult hitResult = player.pick(reach, 0.0F, false);

                if (hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) hitResult;
                    BlockPos pos = blockHit.getBlockPos();

                    // ブロックの種類も取得
                    ServerLevel level = player.serverLevel();
                    BlockState blockState = level.getBlockState(pos);
                    String blockName = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();

                    result.addProperty("x", pos.getX());
                    result.addProperty("y", pos.getY());
                    result.addProperty("z", pos.getZ());
                    result.addProperty("blockType", blockName);
                    result.addProperty("hit", true);

                    MinecraftEduMod.LOGGER.info("Target block: " + blockName + " at " + pos.getX() + "," + pos.getY() + "," + pos.getZ());
                } else {
                    // ブロックに当たらなかった場合
                    result.addProperty("x", 0);
                    result.addProperty("y", 0);
                    result.addProperty("z", 0);
                    result.addProperty("blockType", "air");
                    result.addProperty("hit", false);

                    MinecraftEduMod.LOGGER.info("No block in sight (miss)");
                }
                future.complete(true);
            } catch (Exception e) {
                MinecraftEduMod.LOGGER.error("Error in getTargetBlock raycast", e);
                future.complete(false);
            }
        });

        try {
            boolean success = future.get(5, TimeUnit.SECONDS);
            return success ? result : null;
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("getTargetBlock timed out or interrupted", e);
            return null;
        }
    }

    private JsonObject executeGetPlayerFacing(JsonObject params) {
        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("No player found for getPlayerFacing");
            return null;
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
        JsonObject result = new JsonObject();
        result.addProperty("facing", facing);
        result.addProperty("yaw", yaw);

        return result;
    }

    private JsonObject executeGetBlockType(JsonObject params) {
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
            return null;
        }

        // "minecraft:" プレフィックスを削除してシンプルな形式にする
        String blockType = blockId.toString();
        if (blockType.startsWith("minecraft:")) {
            blockType = blockType.substring(10);
        }

        // 翻訳キーを取得 (例: block.minecraft.stone)
        String translationKey = block.getDescriptionId();

        // 翻訳コンポーネントを作成 (クライアントの言語設定に合わせて表示される)
        // 余計な装飾なし、ブロック名のみを表示
        net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component.translatable(translationKey);

        // 署名なしで送信
        sendRawChatMessage(message);

        MinecraftEduMod.LOGGER.info("Block type retrieved: " + blockType + " at " + x + "," + y + "," + z);

        // 結果データを設定
        JsonObject result = new JsonObject();
        result.addProperty("blockType", blockType);
        JsonObject position = new JsonObject();
        position.addProperty("x", x);
        position.addProperty("y", y);
        position.addProperty("z", z);
        result.add("position", position);

        return result;
    }

    private JsonObject executeSummonEntity(JsonObject params) {
        // エンティティ召喚が禁止されている場合は拒否
        if (!entitySpawningAllowed) {
            MinecraftEduMod.LOGGER.info("Entity spawning is disabled - command rejected");
            return null;
        }

        String entityType = params.get("entityType").getAsString();
        double x = params.get("x").getAsDouble();
        double y = params.get("y").getAsDouble();
        double z = params.get("z").getAsDouble();

        ResourceLocation entityId = new ResourceLocation(entityType);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);

        if (type == null) {
            MinecraftEduMod.LOGGER.warn("Unknown entity type: " + entityType);
            return null;
        }

        safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;
            net.minecraft.world.entity.Entity entity = type.create(world);

            if (entity != null) {
                entity.setPos(x, y, z);
                world.addFreshEntity(entity);
            }
        });

        MinecraftEduMod.LOGGER.info("Entity summoned: " + entityType + " at " + x + "," + y + "," + z);
        return new JsonObject();
    }

    private JsonObject executeTeleport(JsonObject params) {
        double x = params.get("x").getAsDouble();
        double y = params.get("y").getAsDouble();
        double z = params.get("z").getAsDouble();

        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("No player found for teleport");
            return null;
        }

        safeExecute(() -> {
            player.teleportTo(x, y, z);
        });

        MinecraftEduMod.LOGGER.info("Player teleported to " + x + "," + y + "," + z);
        return new JsonObject();
    }

    private JsonObject executeSetWeather(JsonObject params) {
        String weather = params.get("weather").getAsString();

        safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;

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

        // チャット表示
        sendChatMessage("天気: " + getWeatherNameJapanese(weather));

        MinecraftEduMod.LOGGER.info("Weather set to: " + weather);
        return new JsonObject();
    }

    private JsonObject executeSetTime(JsonObject params) {
        long time = params.get("time").getAsLong();

        safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;
            world.setDayTime(time);
        });

        // チャット表示
        sendChatMessage("時刻: " + getTimeNameJapanese(time));

        MinecraftEduMod.LOGGER.info("Time set to: " + time);
        return new JsonObject();
    }

    private JsonObject executeSetGameMode(JsonObject params) {
        String mode = params.get("mode").getAsString();

        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            MinecraftEduMod.LOGGER.warn("No player found for setGameMode");
            return null;
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
                return null;
        }

        safeExecute(() -> {
            player.setGameMode(gameType);
        });

        MinecraftEduMod.LOGGER.info("Game mode set to: " + mode);

        // 結果データを設定
        JsonObject result = new JsonObject();
        result.addProperty("gameMode", mode);
        result.addProperty("playerName", player.getName().getString());

        return result;
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
    private JsonObject executeSetGameRule(JsonObject params) {
        String rule = params.get("rule").getAsString();
        String value = params.get("value").getAsString();
        boolean boolValue = value.equalsIgnoreCase("true");

        safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;
            net.minecraft.world.level.GameRules gameRules = world.getGameRules();

            switch (rule) {
                case "doDaylightCycle":
                    boolean invertedDaylightValue = !boolValue;
                    gameRules.getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                        .set(invertedDaylightValue, server);
                    MinecraftEduMod.LOGGER.info("GameRule set: doDaylightCycle = " + invertedDaylightValue + " (Scratch value: " + value + ")");
                    break;
                case "doWeatherCycle":
                    boolean invertedWeatherValue = !boolValue;
                    gameRules.getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE)
                        .set(invertedWeatherValue, server);
                    MinecraftEduMod.LOGGER.info("GameRule set: doWeatherCycle = " + invertedWeatherValue + " (Scratch value: " + value + ")");
                    break;
                case "doMobSpawning":
                    gameRules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)
                        .set(boolValue, server);
                    MinecraftEduMod.LOGGER.info("GameRule set: doMobSpawning = " + boolValue);
                    break;
                default:
                    MinecraftEduMod.LOGGER.warn("Unknown game rule: " + rule);
                    return;
            }
        });

        // チャット表示（ユーザー視点での表示）
        String onOff = boolValue ? "ON" : "OFF";
        sendChatMessage(getGameRuleNameJapanese(rule) + ": " + onOff);

        // 結果データを設定
        JsonObject result = new JsonObject();
        result.addProperty("gameRule", rule);
        result.addProperty("value", value);
        result.addProperty("success", true);

        return result;
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
     * 中心座標から±200の範囲をスーパーフラットの初期状態に戻す
     * Y=-64: 岩盤
     * Y=-63～-62: 土（2層）
     * Y=-61: 草ブロック
     * Y=-60～100: 空気
     *
     * チャンク単位（16×16ブロック列）に分割し、サーバーの時間予算に応じて
     * 自動スロットリングすることでサーバーフリーズを防止する。
     */
    private JsonObject executeClearArea(JsonObject params) {
        // 重複実行チェック（実行中は失敗として返す）
        if (clearAreaInProgress) {
            MinecraftEduMod.LOGGER.warn("clearArea: 既に実行中です");
            return null;
        }

        // 中心座標を取得（デフォルト: 0, 0）
        int centerX = params.has("centerX") ? params.get("centerX").getAsInt() : 0;
        int centerZ = params.has("centerZ") ? params.get("centerZ").getAsInt() : 0;

        int minX = centerX - 200;
        int maxX = centerX + 200;
        int minZ = centerZ - 200;
        int maxZ = centerZ + 200;

        // チャンク列ごとにタスクを分割してスケジュール
        List<Runnable> tasks = new ArrayList<>();
        for (int chunkX = minX; chunkX <= maxX; chunkX += 16) {
            int cxEnd = Math.min(chunkX + 15, maxX);
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ += 16) {
                int czEnd = Math.min(chunkZ + 15, maxZ);
                final int fCxStart = chunkX, fCxEnd = cxEnd;
                final int fCzStart = chunkZ, fCzEnd = czEnd;

                tasks.add(() -> {
                    ServerLevel world = server.overworld();
                    if (world == null) return;  // ワールド未ロード時の null guard
                    BlockState bedrock = net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState();
                    BlockState dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
                    BlockState grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
                    BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
                    BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
                    for (int x = fCxStart; x <= fCxEnd; x++) {
                        for (int y = -64; y <= 100; y++) {
                            for (int z = fCzStart; z <= fCzEnd; z++) {
                                mutablePos.set(x, y, z);
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
                                // フラグ2: クライアントに通知するが隣接ブロック更新なし
                                // フラグ16: 近傍ブロックの形状更新を抑制
                                world.setBlock(mutablePos, blockToPlace, 2 | 16);
                            }
                        }
                    }
                });
            }
        }

        clearAreaInProgress = true;
        sendChatMessage("§a周囲クリア開始 (" + tasks.size() + "チャンク)");
        scheduleChunkedTasks(tasks, 0, centerX, centerZ);

        JsonObject result = new JsonObject();
        result.addProperty("blocksCleared", 26573265);
        result.addProperty("centerX", centerX);
        result.addProperty("centerZ", centerZ);
        result.addProperty("totalChunks", tasks.size());
        return result;
    }

    /**
     * タスクリストをサーバーの時間予算に応じて分割実行するスケジューラ
     */
    private void scheduleChunkedTasks(List<Runnable> tasks, int index, int centerX, int centerZ) {
        // サーバ停止中は中断
        if (serverStopping || !server.isRunning()) {
            clearAreaInProgress = false;
            MinecraftEduMod.LOGGER.info("clearArea aborted: server stopping (index=" + index + "/" + tasks.size() + ")");
            return;
        }
        if (index >= tasks.size()) {
            clearAreaInProgress = false;
            sendChatMessage("§a周囲クリア完了: 中心(" + centerX + ", " + centerZ + ")");
            MinecraftEduMod.LOGGER.info("チャンク分割タスク完了: " + tasks.size() + "チャンク処理済み");
            return;
        }
        safeExecute(() -> {
            // 25%ごとに進捗をチャットに表示
            int quarter = tasks.size() / 4;
            if (quarter > 0 && index > 0 && index % quarter == 0) {
                int percent = (index * 100) / tasks.size();
                sendChatMessage("§a周囲クリア進行中... " + percent + "%");
            }
            try {
                tasks.get(index).run();
            } catch (Exception e) {
                MinecraftEduMod.LOGGER.error("チャンク処理エラー (index=" + index + "): " + e.getMessage());
            }
            scheduleChunkedTasks(tasks, index + 1, centerX, centerZ);
        });
    }

    /**
     * 全エンティティをクリア
     * 中心座標から±200の範囲のエンティティを削除（プレイヤーを除く）
     */
    private JsonObject executeClearAllEntities(JsonObject params) {
        // 中心座標を取得（デフォルト: 0, 0）
        int centerX = params.has("centerX") ? params.get("centerX").getAsInt() : 0;
        int centerZ = params.has("centerZ") ? params.get("centerZ").getAsInt() : 0;

        safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;

            // 中心座標から±200の範囲
            net.minecraft.world.phys.AABB bounds = new net.minecraft.world.phys.AABB(
                centerX - 200, -64, centerZ - 200,
                centerX + 200, 100, centerZ + 200
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

        JsonObject result = new JsonObject();
        result.addProperty("entitiesRemoved", true);
        result.addProperty("centerX", centerX);
        result.addProperty("centerZ", centerZ);
        return result;
    }

    /**
     * プレイヤーの移動速度を設定
     * @param params multiplier: 速度倍率（小数点1位まで有効、例: 0.5, 1.0, 2.5）
     */
    private JsonObject executeSetMoveSpeed(JsonObject params) {
        if (!params.has("multiplier")) {
            MinecraftEduMod.LOGGER.warn("setMoveSpeed: multiplier parameter required");
            return null;
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
            return null;
        }

        safeExecute(() -> {
            // 全プレイヤーに適用
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);
            }
            MinecraftEduMod.LOGGER.info("SetMoveSpeed: " + finalMultiplier + "x (speed=" + newSpeed + ")");
        });

        // チャット表示
        sendChatMessage("移動速度: " + finalMultiplier + "倍");

        JsonObject result = new JsonObject();
        result.addProperty("multiplier", finalMultiplier);
        result.addProperty("speed", newSpeed);

        return result;
    }

    /**
     * 暗視エフェクトの設定
     * @param params enabled: true=暗視ON、false=暗視OFF
     */
    private JsonObject executeSetNightVision(JsonObject params) {
        if (!params.has("enabled")) {
            MinecraftEduMod.LOGGER.warn("setNightVision: enabled parameter required");
            return null;
        }

        boolean enabled = params.get("enabled").getAsBoolean();

        safeExecute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (enabled) {
                    MobEffectInstance effect = new MobEffectInstance(
                        MobEffects.NIGHT_VISION,
                        999999,
                        0,
                        true,
                        false
                    );
                    player.addEffect(effect);
                    MinecraftEduMod.LOGGER.info("Night vision enabled for player: " + player.getName().getString());
                } else {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                    MinecraftEduMod.LOGGER.info("Night vision disabled for player: " + player.getName().getString());
                }
            }
        });

        // チャット表示
        sendChatMessage("暗視: " + (enabled ? "ON" : "OFF"));

        JsonObject result = new JsonObject();
        result.addProperty("nightVision", enabled);

        return result;
    }

    /**
     * 飛行速度の設定（クリエイティブモード用）
     * @param params multiplier: 速度倍率（例: 1.0=標準、2.0=2倍）
     */
    private JsonObject executeSetFlySpeed(JsonObject params) {
        if (!params.has("multiplier")) {
            MinecraftEduMod.LOGGER.warn("setFlySpeed: multiplier parameter required");
            return null;
        }

        double rawMultiplier = params.get("multiplier").getAsDouble();
        // 小数点1位まで丸める
        double multiplier = Math.round(rawMultiplier * 10.0) / 10.0;
        // 0.1〜10.0の範囲に制限
        multiplier = Math.max(0.1, Math.min(10.0, multiplier));

        final float baseFlySpeed = 0.05f;  // デフォルトの飛行速度
        final float newFlySpeed = (float)(baseFlySpeed * multiplier);
        final double finalMultiplier = multiplier;

        safeExecute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.getAbilities().setFlyingSpeed(newFlySpeed);
                player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
            }
        });

        // チャット表示
        sendChatMessage("飛行速度: " + finalMultiplier + "倍");

        MinecraftEduMod.LOGGER.info("SetFlySpeed: " + finalMultiplier + "x (speed=" + newFlySpeed + ")");

        JsonObject result = new JsonObject();
        result.addProperty("multiplier", finalMultiplier);
        result.addProperty("flySpeed", newFlySpeed);

        return result;
    }

    private ServerPlayer getFirstPlayer() {
        // サーバ停止中やワールド未ロード時の null 安全性を強化
        if (server == null || !server.isRunning()) return null;
        try {
            var playerList = server.getPlayerList();
            if (playerList == null) return null;
            var players = playerList.getPlayers();
            if (players == null || players.isEmpty()) return null;
            return players.get(0);
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.warn("getFirstPlayer failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * サーバ停止時の後処理
     * このフラグが立った後、execute() は全てnullを返し、
     * safeExecute() でキューイングされたタスクも no-op になる。
     * 進行中のclearAreaも次タスク投入を停止する。
     */
    public void shutdown() {
        this.serverStopping = true;
        this.clearAreaInProgress = false;

        // 録画中であれば強制停止（ゾンビプロセス防止）
        if (isRecording && ffmpegProcess != null) {
            try {
                ffmpegProcess.destroyForcibly();
            } catch (Exception ignored) {}
            isRecording = false;
            ffmpegProcess = null;
            currentRecordingPath = null;
        }
        MinecraftEduMod.LOGGER.info("CommandExecutor: shutdown signaled");
    }

    /**
     * サーバ停止中かどうかを返す（MinecraftWebSocketHandler から参照される）
     */
    public boolean isServerStopping() {
        return this.serverStopping;
    }

    /**
     * server.execute() の安全ラッパー
     *
     * 1. 入口でserverStopping をチェック → trueなら何もしない
     * 2. server.isRunning() でForge公式APIも併せてチェック
     * 3. ラムダ内部でも再度 serverStopping / isRunning() をチェック
     *    （キューに積まれてから実行までの間に停止状態に遷移する可能性があるため）
     * 4. ラムダ内例外を捕捉してログ出力（メインスレッドに例外を漏らさない）
     */
    private void safeExecute(Runnable task) {
        if (serverStopping || !server.isRunning()) {
            return;
        }
        server.execute(() -> {
            // メインスレッド時点での再チェック
            if (serverStopping || !server.isRunning()) {
                return;
            }
            try {
                task.run();
            } catch (Exception e) {
                MinecraftEduMod.LOGGER.error("safeExecute task error", e);
            }
        });
    }

    // ========================================
    // チャット表示用ヘルパーメソッド
    // ========================================

    /**
     * ゲーム内チャットにメッセージを送信 (署名なし・コンポーネント指定)
     */
    private void sendRawChatMessage(net.minecraft.network.chat.Component message) {
        safeExecute(() -> {
            server.getPlayerList().getPlayers().forEach(player -> {
                player.sendSystemMessage(message);
            });
        });
    }

    /**
     * ゲーム内チャットにメッセージを送信
     */
    private void sendChatMessage(String message) {
        safeExecute(() -> {
            server.getPlayerList().getPlayers().forEach(player -> {
                player.sendSystemMessage(
                    net.minecraft.network.chat.Component.literal("[MinecraftEdu] " + message)
                );
            });
        });
    }

    private String getTimeNameJapanese(long time) {
        if (time == 0 || time == 24000) return "夜明け";
        if (time == 1000) return "朝";
        if (time == 6000) return "正午";
        if (time == 12000) return "夕方";
        if (time == 13000) return "夜";
        if (time == 18000) return "真夜中";
        return time + " tick";
    }

    private String getWeatherNameJapanese(String weather) {
        switch (weather) {
            case "clear": return "晴れ";
            case "rain": return "雨";
            case "thunder": return "雷雨";
            default: return weather;
        }
    }

    private String getGameRuleNameJapanese(String rule) {
        switch (rule) {
            case "doDaylightCycle": return "時刻固定";
            case "doWeatherCycle": return "天気固定";
            case "doMobSpawning": return "モブスポーン";
            default: return rule;
        }
    }

    // ========================================
    // 録画機能
    // ========================================

    private JsonObject executeStartRecording(JsonObject params) {
        if (isRecording) {
            MinecraftEduMod.LOGGER.warn("録画は既に開始されています");
            return null;
        }

        try {
            String userHome = System.getProperty("user.home");
            File recordingsDir = new File(userHome, "MinecraftRecordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            String filename = "recording_" + timestamp + ".mp4";

            if (params != null && params.has("filename")) {
                String customName = params.get("filename").getAsString();
                if (!customName.isEmpty()) {
                    if (!customName.endsWith(".mp4")) {
                        customName += ".mp4";
                    }
                    filename = customName;
                }
            }

            currentRecordingPath = new File(recordingsDir, filename).getAbsolutePath();

            String[] command = {
                "ffmpeg",
                "-y",
                "-f", "gdigrab",
                "-framerate", "30",
                "-i", "title=Minecraft",
                "-c:v", "libx264",
                "-preset", "ultrafast",
                "-crf", "23",
                "-pix_fmt", "yuv420p",
                currentRecordingPath
            };

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            ffmpegProcess = pb.start();

            Thread.sleep(500);
            if (!ffmpegProcess.isAlive()) {
                MinecraftEduMod.LOGGER.error("FFmpegプロセスの起動に失敗しました");
                return null;
            }

            isRecording = true;
            MinecraftEduMod.LOGGER.info("録画開始: " + currentRecordingPath);

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("isRecording", true);
            result.addProperty("filePath", currentRecordingPath);
            result.addProperty("message", "録画を開始しました");

            return result;

        } catch (IOException e) {
            MinecraftEduMod.LOGGER.error("録画開始エラー: " + e.getMessage(), e);
            return null;
        } catch (InterruptedException e) {
            MinecraftEduMod.LOGGER.error("録画開始中断: " + e.getMessage(), e);
            return null;
        }
    }

    private JsonObject executeStopRecording(JsonObject params) {
        if (!isRecording || ffmpegProcess == null) {
            MinecraftEduMod.LOGGER.warn("録画は開始されていません");
            return null;
        }

        try {
            ffmpegProcess.getOutputStream().write('q');
            ffmpegProcess.getOutputStream().flush();

            boolean exited = ffmpegProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (!exited) {
                ffmpegProcess.destroyForcibly();
                MinecraftEduMod.LOGGER.warn("FFmpegプロセスを強制終了しました");
            }

            isRecording = false;
            String recordedPath = currentRecordingPath;
            ffmpegProcess = null;
            currentRecordingPath = null;

            MinecraftEduMod.LOGGER.info("録画停止: " + recordedPath);

            JsonObject result = new JsonObject();
            File recordedFile = new File(recordedPath);
            if (recordedFile.exists()) {
                long fileSize = recordedFile.length();
                result.addProperty("success", true);
                result.addProperty("isRecording", false);
                result.addProperty("filePath", recordedPath);
                result.addProperty("fileSize", fileSize);
                result.addProperty("message", "録画を停止しました");
            } else {
                result.addProperty("success", true);
                result.addProperty("isRecording", false);
                result.addProperty("filePath", recordedPath);
                result.addProperty("warning", "ファイルが見つかりません。録画時間が短すぎた可能性があります。");
            }

            return result;

        } catch (IOException e) {
            MinecraftEduMod.LOGGER.error("録画停止エラー: " + e.getMessage(), e);
            return null;
        } catch (InterruptedException e) {
            MinecraftEduMod.LOGGER.error("録画停止中断: " + e.getMessage(), e);
            return null;
        }
    }

    private JsonObject executeGetRecordingStatus(JsonObject params) {
        JsonObject result = new JsonObject();
        result.addProperty("isRecording", isRecording);
        if (isRecording && currentRecordingPath != null) {
            result.addProperty("filePath", currentRecordingPath);
        }
        return result;
    }

    private JsonObject executeSetEntitySpawning(JsonObject params) {
        boolean enabled = params.get("enabled").getAsBoolean();
        this.entitySpawningAllowed = enabled;

        sendChatMessage("エンティティ召喚: " + (enabled ? "許可" : "禁止"));

        MinecraftEduMod.LOGGER.info("Entity spawning " + (enabled ? "enabled" : "disabled") + " via WebSocket");

        JsonObject result = new JsonObject();
        result.addProperty("entitySpawningAllowed", enabled);

        return result;
    }

    /**
     * エンティティ召喚の許可/禁止を設定（スラッシュコマンド用）
     */
    public void setEntitySpawningAllowed(boolean allowed) {
        this.entitySpawningAllowed = allowed;
        MinecraftEduMod.LOGGER.info("Entity spawning " + (allowed ? "enabled" : "disabled") + " via command");
    }

    /**
     * エンティティ召喚が許可されているか取得
     */
    public boolean isEntitySpawningAllowed() {
        return this.entitySpawningAllowed;
    }

    /**
     * コンテナブロック（ディスペンサー、チェスト、ホッパー等）にアイテムを設定する
     */
    private JsonObject executeSetContainerItem(JsonObject params) {
        int x = params.get("x").getAsInt();
        int y = params.get("y").getAsInt();
        int z = params.get("z").getAsInt();
        int slot = params.get("slot").getAsInt();
        String itemType = params.get("itemType").getAsString();
        int count = params.get("count").getAsInt();

        BlockPos pos = new BlockPos(x, y, z);

        safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof Container container) {
                if (slot < 0 || slot >= container.getContainerSize()) {
                    MinecraftEduMod.LOGGER.warn("Invalid slot number: " + slot + " (max: " + (container.getContainerSize() - 1) + ")");
                    return;
                }

                ResourceLocation itemId = new ResourceLocation(itemType);
                Item item = BuiltInRegistries.ITEM.get(itemId);

                int validCount = Math.min(64, Math.max(1, count));

                ItemStack itemStack = new ItemStack(item, validCount);

                container.setItem(slot, itemStack);
                blockEntity.setChanged();

                MinecraftEduMod.LOGGER.info("Container item set at " + pos + " slot " + slot + ": " + itemType + " x" + validCount);
            } else {
                MinecraftEduMod.LOGGER.warn("No container block at position: " + pos);
            }
        });

        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("x", x);
        result.addProperty("y", y);
        result.addProperty("z", z);
        result.addProperty("slot", slot);
        result.addProperty("itemType", itemType);
        result.addProperty("count", count);

        return result;
    }
}
