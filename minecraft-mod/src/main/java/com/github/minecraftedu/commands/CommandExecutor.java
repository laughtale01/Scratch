package com.github.minecraftedu.commands;

import com.github.minecraftedu.MinecraftEduMod;
import com.github.minecraftedu.network.SimpleWebSocketServer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class CommandExecutor {

    // 録画ファイル名に許可する文字。拡張子は MOD 側で付けるので "." も含めない
    private static final Pattern SAFE_FILENAME = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    // Windows のデバイス名。上の文字種は通ってしまい、"NUL.mp4" のように拡張子が付いても
    // デバイス扱いになって録画が黙って消えるため、別に弾く
    private static final Pattern RESERVED_FILENAME =
        Pattern.compile("(?i)(CON|PRN|AUX|NUL|COM[0-9]|LPT[0-9])");
    // ffmpeg の出力を残すファイル。録画フォルダに置くので、動画と一緒に見つけられる
    private static final String FFMPEG_LOG_NAME = "ffmpeg.log";

    private final MinecraftServer server;

    // 録画関連のフィールド
    // volatile 必須: ハンドラスレッドとメインスレッドの両方からアクセスされる
    private volatile Process ffmpegProcess;
    private volatile String currentRecordingPath;
    private volatile boolean isRecording;

    // 録画の開始・停止・後始末を直列化するロック
    // volatile だけでは「録画中か確かめてから開始する」の間に割り込まれるのを防げない
    private final Object recordingLock = new Object();

    // エンティティ召喚制御フラグ
    // volatile 必須: WebSocket のハンドラスレッドとスラッシュコマンド（メインスレッド）の両方から書かれる
    private volatile boolean entitySpawningAllowed = true;

    // clearArea重複実行防止フラグ
    // AtomicBoolean: 2 つの接続からほぼ同時に来ても、印を付けるのは 1 つだけにするため
    private final AtomicBoolean clearAreaInProgress = new AtomicBoolean(false);

    // 失敗を知らせるチャットの間引き（同じ種類の失敗は、この時間に 1 回だけ出す）
    private static final long ERROR_CHAT_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(3);
    // 入力の誤り（ワールドの外・名前の打ちまちがいなど）のログを出す間隔（「ずっと」の中で続いてもログが増えないように）
    private static final long INPUT_ERROR_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(60);
    // チャットの命令で出す文の長さの上限（ふつうの Minecraft のチャットと同じ）
    private static final int MAX_CHAT_MESSAGE_CHARS = 256;
    // チャットに出す名前（子どもが打った文字）の長さの上限
    private static final int MAX_CHAT_NAME_CHARS = 32;

    // 失敗の種類ごとに、最後にチャットに出した時刻
    // 種類は決まった文（名前を付ける文は「: 」より前）なので、数は増えない
    // メインスレッドだけが読み書きする（チャットを出す順と間引きの記録をずらさないため）
    private final Map<String, Long> errorChatNanos = new HashMap<>();
    // 入力の誤りのログを最後に出した時刻（呼ぶ場所ごと、例外は命令と例外の種類の組ごと。自分自身で守る）
    private final Map<String, Long> inputErrorLogNanos = new HashMap<>();

    // サーバ停止フラグ。trueの間は全コマンドを拒否する
    // volatile 必須: ハンドラスレッドとメインスレッドの両方からアクセスされる
    private volatile boolean serverStopping = false;

    // 世界を変える仕事を届いた順に並べ、少しずつ進める列（大きな「埋める」で Minecraft が止まらないように）
    private final BuildQueue buildQueue;

    // 読む命令（見ているブロック・ブロックの種類）が、列で前の命令を待つ時間の上限（ミリ秒）
    // （Scratch は 5 秒で待つのをやめ、失敗を「空気」として扱うので、待ちきれなければ列を追い越して今の様子を読む。
    //   その分の時間も残す）
    private static final long READ_QUEUE_WAIT_MILLIS = 2500;
    // 列を追い越して読むときに、メインスレッドの答えを待つ時間の上限（ミリ秒）
    private static final long READ_NOW_WAIT_MILLIS = 1500;
    // 待ち終わるのと同時にメインスレッドが読み始めていたときに、読み終わるのを待つ時間の上限（ミリ秒）
    // （待つ時間を全部足しても、Scratch が待つ 5 秒より短くなるように）
    private static final long READ_FINISH_WAIT_MILLIS = 500;
    // 列にたまった仕事がこの重さ（ブロックの数）より多ければ、読む命令は列で待たずに、今の様子を読む
    // （大きな「埋める」の途中で何秒も待たせると、その間このタブのほかの命令も止まり、待ちきれないと「空気」と答えてしまうため。
    //   遅い PC でも 1.5 秒ほどで置き終わる重さにして、READ_QUEUE_WAIT_MILLIS を待ちきれずにむだにすることを少なくする）
    private static final long READ_WAIT_MAX_PENDING_WEIGHT = 15_000L;
    // 読む命令が待っている答え（ワールドを閉じるときに、待っているスレッドをすぐ放すため）
    private final Set<CompletableFuture<JsonObject>> pendingReads = ConcurrentHashMap.newKeySet();
    // 「見ているブロック」がたどる長さの上限（ブロック）
    private static final double MAX_TARGET_REACH = 256.0;
    // 列が混んでいるときの知らせ
    private static final String BUSY_CHAT = "いま たくさん つくっている とちゅうです。すこし まってね";
    // 列が混んでいること・「周りをきれいにする」の途中であることを最後に知らせた時刻（System.nanoTime。0 はまだ知らせていない）
    private final AtomicLong lastBusyNoticeNanos = new AtomicLong();
    private final AtomicLong lastClearAreaBusyNoticeNanos = new AtomicLong();
    // いま命令を受け取っている接続（execute の間だけ。列に並べる仕事に、送った接続の印として付ける）
    private final ThreadLocal<Object> commandOwner = new ThreadLocal<>();
    // 止めた・切れたときに、まだ置いていない命令をやめたことを最後に知らせた時刻（3 秒に 1 回まで）
    private final AtomicLong lastCancelNoticeNanos = new AtomicLong();
    // 「周りをきれいにする」の途中に、もう一度頼まれたときの知らせ
    private static final String CLEAR_AREA_BUSY_CHAT = "いま まわりを きれいにしている とちゅうです。おわるまで まってね";
    // チャンクの読み込みを待ちきれずにやめたときの知らせの始まり（後ろに、やめたことを足す）
    private static final String LOAD_GAVE_UP_CHAT_PREFIX = "ばしょの よみこみが まにあわなかったので、";
    // 読む命令が、チャンクの読み込みを待ちきれなかったときの知らせ
    private static final String READ_LOAD_PENDING_CHAT = "ばしょを よみこんでいる とちゅうです。すこし まってから もういちど しらべてね";
    // 読む命令が、チャンクの読み込みや列を待つあいだに、メインスレッドに読めたかを聞き直す間隔（ミリ秒。1 tick）
    private static final long READ_POLL_MILLIS = 50;

    public CommandExecutor(MinecraftServer server) {
        this.server = server;
        this.buildQueue = new BuildQueue(server, () -> this.serverStopping);
        this.isRecording = false;
        this.ffmpegProcess = null;
        this.currentRecordingPath = null;
        this.entitySpawningAllowed = true;
    }

    /**
     * コマンドを実行し、結果をJsonObjectで返す
     * @return 成功時は結果オブジェクト。失敗時は null、または
     *         success:false と errorCode を持つ結果（失敗理由を Scratch 側へ伝える場合）
     */
    public JsonObject execute(String action, JsonObject params) {
        return execute(action, params, null);
    }

    /**
     * コマンドを実行し、結果をJsonObjectで返す（列に並べる仕事に、送った接続の印を付ける）
     * @param owner 送った接続（MinecraftWebSocketHandler）。止めるボタン・接続が切れたときに、この接続の仕事だけを列から外す
     */
    public JsonObject execute(String action, JsonObject params, Object owner) {
        commandOwner.set(owner);
        try {
            return executeAs(action, params);
        } finally {
            commandOwner.remove();
        }
    }

    private JsonObject executeAs(String action, JsonObject params) {
        // サーバ停止中は何も実行しない（ハンドラスレッド入口チェック）
        if (serverStopping || !server.isRunning()) {
            logInputError("Command rejected (server stopping)", "Command rejected (server stopping): " + sanitizeForLog(action));
            return null;
        }
        // params が無い・オブジェクトでない命令は、値の形の誤りとして早めに断る
        // （「周りをきれいにする」などは中心のキーも確かめるので、二重の守り）
        if (params == null) {
            logInputError("Missing params", "Command without params: " + sanitizeForLog(action));
            sendErrorChat("うまく できませんでした（いれた あたいを たしかめてね）");
            return null;
        }
        try {
            switch (action) {
                case "chat":
                    return executeChat(params);

                case "setBlock":
                    return executeSetBlock(params);

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

                case "getBlockUnderPlayer":
                    return executeGetBlockUnderPlayer(params);

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

                case "clearInventory":
                    return executeClearInventory(params);

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

                case "stopAll":
                    return executeStopAll(params);

                default:
                    // Scratch（gui.js）の方が新しく、この MOD が知らない命令を送ってきたとき
                    logInputError("Unknown command", "Unknown command: " + sanitizeForLog(action));
                    sendErrorChat("その めいれいは この MOD では つかえません（MOD が ふるいかもしれません）");
                    return null;
            }
        } catch (Exception e) {
            // 命令の中で起きた例外（値が数でない・キーが無いなどの値の誤りか、MOD の不具合）
            // 「ずっと」の中で続いてもログが増えないよう、命令と例外の種類の組ごとに 60 秒に 1 回だけ出す
            // 例外の文には送られた値がそのまま入ることがあるので、ログには無害化して出す
            String summary = sanitizeForLog(action) + " (" + e.getClass().getSimpleName() + ": " + sanitizeForLog(e.getMessage()) + ")";
            boolean badParameter = isBadParameterException(e);
            // 間引きは命令と例外の種類の組ごと（値の誤りが続いても、本物の不具合のログが隠れないように）
            if (!shouldLogInputError("exception:" + action + ":" + e.getClass().getName())) {
                MinecraftEduMod.LOGGER.debug("Error executing command: " + summary);
            } else if (badParameter) {
                // 値が数でない・null など、送られた値の形の誤り。スタックトレースは要らない
                MinecraftEduMod.LOGGER.warn("Bad command parameters: " + summary + "（同じ失敗は 60 秒間 debug にだけ出す）");
            } else {
                MinecraftEduMod.LOGGER.error("Error executing command: " + summary, e);
            }
            // 値の誤りなら子どもに入れた値を見てもらい、それ以外は MOD の不具合として先生に知らせてもらう
            sendErrorChat(badParameter
                ? "うまく できませんでした（いれた あたいを たしかめてね）"
                : "MOD の なかで まちがいが おきました（せんせいに しらせてね）");
            return null;
        }
    }

    private JsonObject executeChat(JsonObject params) {
        // ふつうの Minecraft のチャットと同じく 256 文字までにし、表示やログを乱す見えない文字は ? にする
        // （チャットの文は Minecraft が latest.log にそのまま書くので、長い文や制御文字でログが乱れないように。
        //   色を変える § は、チャットの楽しみとして残す）
        String message = replaceUnsafeChars(cutToChars(params.get("message").getAsString(), MAX_CHAT_MESSAGE_CHARS), true);

        boolean queued = runOnMainThread(() -> sendToAllPlayers(net.minecraft.network.chat.Component.literal(message)));
        if (!queued) {
            return null;  // 列がいっぱい・サーバーが止まりかけている（ほかの命令と同じく、失敗として返す）
        }

        MinecraftEduMod.LOGGER.debug("Chat message sent (" + message.length() + " chars)");
        return new JsonObject();
    }

    private JsonObject executeSetBlock(JsonObject params) {
        String blockType = params.get("blockType").getAsString();

        // 座標取得（絶対または相対）
        int x, y, z;
        ServerPlayer player = getFirstPlayer();

        // ワールドの外の座標は断る（int にする前に確かめる。大きすぎる数は上の桁が落ちて、別の場所に置かれてしまうため）
        String[] coordKeys = params.has("x")
            ? new String[]{"x", "y", "z"}
            : new String[]{"relativeX", "relativeY", "relativeZ"};
        for (String key : coordKeys) {
            if (params.has(key) && !isWithinWorldLimit(params.get(key))) {
                logInputError("setBlock coordinates out of world limit", "setBlock coordinates out of world limit");
                sendErrorChat("ばしょがワールドの外です");
                return null;
            }
        }

        // 返事に入れる場所が、ワールドの中か（相対座標で、見込みの場所が外なら入れない）
        boolean estimateInside = true;
        if (params.has("x")) {
            // 絶対座標
            x = params.get("x").getAsInt();
            y = params.get("y").getAsInt();
            z = params.get("z").getAsInt();
        } else {
            // 相対座標
            if (player == null) {
                logInputError("No player found for relative coordinates", "No player found for relative coordinates");
                return null;
            }

            int relX = params.get("relativeX").getAsInt();
            int relY = params.get("relativeY").getAsInt();
            int relZ = params.get("relativeZ").getAsInt();

            // 足したあとの場所がワールドの中かは、列で順番が来たときのプレイヤーの位置で確かめる
            // （受け取った時点の位置で確かめると、前に並んだテレポートのあとなら置ける場所でも断ってしまうため）。
            // ここでは返事に入れる見込みの場所だけを計算する（long で足すので、あふれない）
            // プレイヤーのいるマスは切り下げで決める（0 の方への切り捨てだと、マイナスの座標で 1 マスずれるため）
            long sumX = (long) net.minecraft.util.Mth.floor(player.getX()) + relX;
            long sumY = (long) net.minecraft.util.Mth.floor(player.getY()) + relY;
            long sumZ = (long) net.minecraft.util.Mth.floor(player.getZ()) + relZ;
            estimateInside = isWithinWorldLimit(sumX) && isWithinWorldLimit(sumY) && isWithinWorldLimit(sumZ);
            x = estimateInside ? (int) sumX : 0;
            y = estimateInside ? (int) sumY : 0;
            z = estimateInside ? (int) sumZ : 0;
        }

        // ブロック状態を解析（プロパティを含む）
        BlockState blockState = parseBlockState(blockType);

        if (blockState == null) {
            // 理由は parseBlockState がチャットとログで知らせている
            MinecraftEduMod.LOGGER.debug("Failed to parse block state: " + sanitizeForLog(blockType));
            return null;
        }

        BlockPos pos = new BlockPos(x, y, z);
        // 相対座標は、列で順番が来たときのプレイヤーの位置で決める
        // （「テレポート → 相対で置く」と続けたとき、テレポートの前の位置で置かないように。上の確かめは受け取った時点の見込み）
        boolean relative = !params.has("x");
        int relX = relative ? params.get("relativeX").getAsInt() : 0;
        int relY = relative ? params.get("relativeY").getAsInt() : 0;
        int relZ = relative ? params.get("relativeZ").getAsInt() : 0;

        // ブロック配置（列が混んでいて並べられなければ、断ったことにする。知らせは enqueue が出す）
        // まだ読み込まれていないチャンクなら、読み込まれるのを待ってから置く（BlockJob。その場で置くと Minecraft が止まるため）
        boolean queued = enqueue(new BlockJob(server, world -> {
            if (!relative) {
                return pos;
            }
            ServerPlayer current = getFirstPlayer();
            if (current == null) {
                logInputError("No player found for relative coordinates", "No player found for relative coordinates");
                return null;
            }
            long sumX = (long) net.minecraft.util.Mth.floor(current.getX()) + relX;
            long sumY = (long) net.minecraft.util.Mth.floor(current.getY()) + relY;
            long sumZ = (long) net.minecraft.util.Mth.floor(current.getZ()) + relZ;
            if (!isWithinWorldLimit(sumX) || !isWithinWorldLimit(sumY) || !isWithinWorldLimit(sumZ)) {
                logInputError("setBlock relative position out of world limit", "setBlock relative position out of world limit");
                sendErrorChat("ばしょがワールドの外です");
                return null;
            }
            return new BlockPos((int) sumX, (int) sumY, (int) sumZ);
        }, relative ? null : pos, true, (world, target) -> world.setBlock(target, blockState, 3), () -> {
            logInputError("setBlock chunk not loaded", "setBlock: gave up waiting for the chunk to load");
            sendErrorChat(LOAD_GAVE_UP_CHAT_PREFIX + "ブロックを おけませんでした");
        }));
        if (!queued) {
            return null;
        }

        MinecraftEduMod.LOGGER.debug("Block placed: " + sanitizeForLog(blockType) + " at " + x + "," + y + "," + z);

        // 結果データを設定（相対座標のときの場所は、受け取った時点のプレイヤーの位置で計算した見込み）
        JsonObject result = new JsonObject();
        result.addProperty("blockPlaced", true);
        if (estimateInside) {
            JsonObject position = new JsonObject();
            position.addProperty("x", x);
            position.addProperty("y", y);
            position.addProperty("z", z);
            result.add("position", position);
        }

        return result;
    }

    // ワールドの端（Minecraft がブロックを置けるのは X・Z が -30,000,000 以上 30,000,000 未満）。これより外の座標は断る
    private static final int WORLD_COORD_LIMIT = 30_000_000;
    // テレポート・生き物を出すときの高さの上限（Minecraft の /tp・/summon と同じく -20,000,000 以上 20,000,000 未満）
    private static final int SPAWN_HEIGHT_LIMIT = 20_000_000;
    // summonEntity で出さないもの（出しただけで、まわりを壊す・燃やすもの。みんなの作品が消えてしまう）
    // ほかの生き物は、出したあと SummonedEntityGuard が作品を壊させない。
    // 弾そのものは、ここにない種類も、作ったあとで断る（持ち主がいない弾は、見張りでは止めきれないため）
    private static final Set<EntityType<?>> BLOCKED_SUMMON_TYPES = Set.of(
            EntityType.WITHER, EntityType.ENDER_DRAGON,                      // ボス（ブロックを壊しながら動く）
            EntityType.TNT, EntityType.TNT_MINECART, EntityType.END_CRYSTAL, // 出したとたんに爆発する・爆発させられる
            EntityType.LIGHTNING_BOLT,                                       // 出したとたんに火がつく
            EntityType.FIREBALL, EntityType.SMALL_FIREBALL, EntityType.WITHER_SKULL, // 当たると爆発・火がつく弾
            EntityType.DRAGON_FIREBALL,                                      // 当たるとダメージの雲が出る弾
            EntityType.FALLING_BLOCK);                                       // 落ちた所の水などを砂に置き換える
    // 1 回の fillBlocks で置けるブロックの数の上限
    private static final long MAX_FILL_VOLUME = 2_000_000L;
    // 1 回の fillBlocks の横（X・Z）の 1 辺の上限
    // （置くたびにまだ読み込んでいないチャンクを読み込み・生成するので、広すぎるとサーバーが長く止まるため。
    //  256 なら最大 17×17 = 289 チャンクで、ほぼ描画距離の中に収まる）
    private static final long MAX_FILL_SIDE = 256L;

    /**
     * 座標がワールドの端より内側か確認（大きすぎる数で計算があふれたり、ループが終わらなくなったりしないように）
     * int にする前に、送られた数のまま確かめる（getAsInt は上の桁を落とすので、4294967296 が 0 になり、
     * ワールドの外の数が原点の近くとして通ってしまうため）。NaN や無限大も外として扱う
     */
    private static boolean isWithinWorldLimit(JsonElement value) {
        if (value == null) {
            // キーが無い（params.get(…) が null）。値の誤りとして断る
            throw new MissingParameterException();
        }
        return isWithinWorldLimit(value.getAsDouble());
    }

    /**
     * 送られた値に、必要なキーが無いときの例外（値の誤りとして扱う）
     */
    private static class MissingParameterException extends IllegalArgumentException {
        MissingParameterException() {
            super("missing parameter");
        }
    }

    /**
     * 座標がワールドの端より内側か確認（-30,000,000 以上 30,000,000 未満。Minecraft がブロックを置ける範囲と同じ）
     * NaN は比べると false になるので、外として扱われる
     */
    private static boolean isWithinWorldLimit(double value) {
        return value >= -WORLD_COORD_LIMIT && value < WORLD_COORD_LIMIT;
    }

    /**
     * テレポート・生き物を出す場所がワールドの中か確認（Minecraft の /tp・/summon と同じ範囲）
     * 横（X・Z）は isWithinWorldLimit と同じ、高さ（Y）は -20,000,000 以上 20,000,000 未満。NaN は外として扱われる
     */
    private static boolean isWithinSpawnableBounds(JsonObject params) {
        double y = params.get("y").getAsDouble();
        return isWithinWorldLimit(params.get("x")) && isWithinWorldLimit(params.get("z"))
            && y >= -SPAWN_HEIGHT_LIMIT && y < SPAWN_HEIGHT_LIMIT;
    }

    private JsonObject executeFillBlocks(JsonObject params) {
        // fromとtoの座標を取得
        JsonObject from = params.getAsJsonObject("from");
        JsonObject to = params.getAsJsonObject("to");
        String blockType = params.get("blockType").getAsString();

        // ワールドの外の座標は断る（int にする前に確かめる）
        if (!isWithinWorldLimit(from.get("x")) || !isWithinWorldLimit(from.get("y")) || !isWithinWorldLimit(from.get("z"))
                || !isWithinWorldLimit(to.get("x")) || !isWithinWorldLimit(to.get("y")) || !isWithinWorldLimit(to.get("z"))) {
            logInputError("Fill coordinates out of world limit", "Fill coordinates out of world limit");
            sendErrorChat("はんいがワールドの外です");
            return null;
        }

        int fromX = from.get("x").getAsInt();
        int fromY = from.get("y").getAsInt();
        int fromZ = from.get("z").getAsInt();

        int toX = to.get("x").getAsInt();
        int toY = to.get("y").getAsInt();
        int toZ = to.get("z").getAsInt();

        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            MinecraftEduMod.LOGGER.warn("Fill skipped: overworld is not loaded");
            return null;
        }

        // 座標範囲を正規化（小さい方から大きい方へ）
        // 高さは建てられる範囲（1.20.1 のオーバーワールドは -64〜319）に切りそろえる
        // （範囲の外はもともと何も置かれないので、置ける所だけを回す。全部が外なら断る）
        int minX = Math.min(fromX, toX);
        int maxX = Math.max(fromX, toX);
        int minY = Math.max(Math.min(fromY, toY), overworld.getMinBuildHeight());
        int maxY = Math.min(Math.max(fromY, toY), overworld.getMaxBuildHeight() - 1);
        int minZ = Math.min(fromZ, toZ);
        int maxZ = Math.max(fromZ, toZ);
        if (minY > maxY) {
            logInputError("Fill range is outside the build height", "Fill range is outside the build height");
            // 前は何も置かずに成功を返していたので、断った理由をゲームの中で知らせる
            sendErrorChat("その高さには ブロックを おけません（高すぎるか ひくすぎます）");
            return null;
        }

        // 範囲チェック（最大2000000ブロックまで）
        // int のかけ算はあふれて小さな数（や負の数）になり、チェックをすり抜けるので、
        // long で計算する（横は 1 辺 256 まで、高さは建てられる範囲に切りそろえたので、3 辺の積も long に収まる）
        // 横（X・Z）は 1 辺 MAX_FILL_SIDE まで（チャンクを読み込みすぎないため）
        long sizeX = (long) maxX - minX + 1;
        long sizeY = (long) maxY - minY + 1;
        long sizeZ = (long) maxZ - minZ + 1;
        if (sizeX > MAX_FILL_SIDE || sizeZ > MAX_FILL_SIDE || sizeX * sizeY * sizeZ > MAX_FILL_VOLUME) {
            logInputError("Fill range too large", "Fill range too large: " + sizeX + "x" + sizeY + "x" + sizeZ
                + " (max side: " + MAX_FILL_SIDE + ", max: " + MAX_FILL_VOLUME + " blocks)");
            // Scratch 側は体積しか確かめないので、断った理由をゲームの中で知らせる
            // 小学生に読みやすいよう、ブロックの数は「200まん」と書く
            sendErrorChat("はんいが広すぎます（よこも おくゆきも " + MAX_FILL_SIDE + " マスまで、ぜんぶで " + (MAX_FILL_VOLUME / 10_000) + "まん ブロックまで）");
            return null;
        }
        long volume = sizeX * sizeY * sizeZ;

        // ブロック状態を解析（プロパティを含む）
        BlockState blockState = parseBlockState(blockType);

        if (blockState == null) {
            // 理由は parseBlockState がチャットとログで知らせている
            MinecraftEduMod.LOGGER.debug("Failed to parse block state: " + sanitizeForLog(blockType));
            return null;
        }

        // 列が混んでいたら断る（すぐ知らせる。小さな「埋める」は、大きな仕事の後ろでも断らず、
        // 小さな仕事だけの重さが上限に届いたときと、仕事の数がとても多いときだけ断る）
        if (!buildQueue.hasRoomFor(volume)) {
            logInputError("Build queue full", "Build queue full: fillBlocks (" + volume + " blocks)");
            notifyBusy();
            return null;
        }

        // ブロック配置（列に並べ、少しずつ置く。1 回で全部置くと、そのあいだ Minecraft が止まるため）
        FillJob fill = new FillJob(server, minX, minY, minZ, maxX, maxY, maxZ, y -> blockState, 3);
        String loggedType = sanitizeForLog(blockType);
        boolean queued = buildQueue.offer(new BuildQueue.Job() {
            @Override
            public void release() {
                // 途中でやめたので、付けたチケットを外す（メインスレッドで BuildQueue が呼ぶ。外すのはこの仕事のチケットだけなので、
                // 次の仕事が始まったあとでもよい。まだ始まっていない・終わったあとなら何もしない）
                fill.removeTickets();
            }

            @Override
            public boolean step(long deadlineNanos) {
                boolean done = fill.step(deadlineNanos);
                if (done && fill.gaveUp()) {
                    // チャンクの読み込みを待ちきれず、途中でやめた（メインスレッドの上なので、知らせはその場で出る）
                    logInputError("fillBlocks chunk not loaded", "fillBlocks: gave up waiting for a chunk to load after "
                        + fill.placed() + " blocks");
                    sendErrorChat(LOAD_GAVE_UP_CHAT_PREFIX + "うめるのを とちゅうで やめました");
                } else if (done) {
                    MinecraftEduMod.LOGGER.debug("Filled " + fill.placed() + " blocks with " + loggedType);
                }
                return done;
            }

            @Override
            public long weight() {
                return fill.weight();
            }
        }, commandOwner.get());
        if (!queued) {
            logInputError("Build queue full", "Build queue full: fillBlocks (" + volume + " blocks)");
            notifyBusy();
            return null;
        }

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
            logInputError("No player found for getPosition", "No player found for getPosition");
            return null;
        }

        // 列に並んで、メインスレッドで読む（「テレポート → 今いる場所」と続けたとき、テレポートのあとの場所を答えるように。
        // 前は受け口のスレッドでそのまま読んでいたので、列で待っているテレポートの前の場所を答えていた）。
        // ただし列がとても混んでいるときや、2.5 秒待っても順番が来ないときは、列を追い越して読む（答えの queued が false になる）。
        // プレイヤーは順番が来たときに引き直す（列で待つ間にやられて生き返ると、プレイヤーが新しく作り直されるため）
        return callInQueue("getPosition", () -> true, () -> {
            ServerPlayer current = getFirstPlayer();
            if (current == null) {
                logInputError("No player found for getPosition", "No player found for getPosition");
                return null;
            }
            double x = current.getX();
            double y = current.getY();
            double z = current.getZ();
            float yaw = current.getYRot();
            float pitch = current.getXRot();

            MinecraftEduMod.LOGGER.debug("Player position: " + x + "," + y + "," + z);

            // 結果データを設定
            JsonObject result = new JsonObject();
            result.addProperty("x", x);
            result.addProperty("y", y);
            result.addProperty("z", z);
            result.addProperty("yaw", yaw);
            result.addProperty("pitch", pitch);

            return result;
        });
    }

    /**
     * プレイヤーが見ているブロック（クロスヘアの先）の座標を取得
     * メインスレッドでレイキャストを実行（ワールドデータへのアクセスが必要なため）
     */
    private JsonObject executeGetTargetBlock(JsonObject params) {
        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            logInputError("No player found for getTargetBlock", "No player found for getTargetBlock");
            return null;
        }

        // レイキャスト距離（デフォルト256ブロック。Scratch は送らないのでいつも 256）
        double requestedReach = params.has("reach") ? params.get("reach").getAsDouble() : MAX_TARGET_REACH;
        // 無限大・NaN は断る（無限大だと見る先をいつまでもたどり、Minecraft が止まるため）
        if (!Double.isFinite(requestedReach)) {
            logInputError("getTargetBlock: reach is not finite", "getTargetBlock: reach is not finite");
            sendErrorChat("うまく できませんでした（いれた あたいを たしかめてね）");
            return null;
        }
        // 0〜256 ブロックにおさめる（遠すぎると、たどるマスが多くなり Minecraft が重くなるため）
        double reach = Math.max(0.0, Math.min(MAX_TARGET_REACH, requestedReach));

        // メインスレッドでレイキャストを実行（ワールドデータにアクセスするため。ほかの命令と同じ列に並び、
        // 前の命令で置いたブロックも見えるようにする。読み込まれていないチャンクの手前でやめる＝pickLoaded）。
        // プレイヤーは順番が来たときに引き直す
        // （列で待つ間にやられて生き返ると、プレイヤーが新しく作り直されるため）
        // 読む中で例外が起きたときは、ほかの命令と同じく execute の catch で知らせる
        // （前はここで捕まえてチャットを出さなかった。いまは「MOD の なかで まちがいが おきました」などが出る）
        return callInQueue("getTargetBlock", () -> true, () -> {
            ServerPlayer current = getFirstPlayer();
            if (current == null) {
                logInputError("No player found for getTargetBlock", "No player found for getTargetBlock");
                return null;
            }
            JsonObject result = new JsonObject();
            ServerLevel level = current.serverLevel();
            net.minecraft.world.phys.BlockHitResult blockHit = pickLoaded(current, level, reach);

            if (blockHit != null) {
                BlockPos pos = blockHit.getBlockPos();

                // ブロックの種類も取得
                BlockState blockState = level.getBlockState(pos);
                String blockName = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).toString();

                result.addProperty("x", pos.getX());
                result.addProperty("y", pos.getY());
                result.addProperty("z", pos.getZ());
                result.addProperty("blockType", blockName);
                result.addProperty("hit", true);

                MinecraftEduMod.LOGGER.debug("Target block: " + blockName + " at " + pos.getX() + "," + pos.getY() + "," + pos.getZ());
            } else {
                // ブロックに当たらなかった場合
                result.addProperty("x", 0);
                result.addProperty("y", 0);
                result.addProperty("z", 0);
                result.addProperty("blockType", "air");
                result.addProperty("hit", false);

                MinecraftEduMod.LOGGER.debug("No block in sight (miss)");
            }
            return result;
        });
    }

    /**
     * プレイヤーが見ている先のブロック（Entity.pick(reach, 0, false) と同じたどり方。当たらなければ null。メインスレッドで呼ぶ）
     * 読み込まれていないチャンクに入ったら、そこでやめて「当たらなかった」とする
     * （pick は、たどる途中のチャンクをその場で読み込み・生成し、そのあいだ Minecraft が止まるため。
     *   読み込まれている範囲より先は、プレイヤーの画面にも映っていない）
     */
    private static BlockHitResult pickLoaded(ServerPlayer player, ServerLevel level, double reach) {
        Vec3 from = player.getEyePosition(0.0F);
        Vec3 view = player.getViewVector(0.0F);
        Vec3 to = from.add(view.x * reach, view.y * reach, view.z * reach);
        ClipContext context = new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        // たどるのをやめた印（読み込まれていないチャンクに入ったとき・最後まで当たらなかったとき）
        BlockHitResult stop = BlockHitResult.miss(to, Direction.UP, BlockPos.containing(to));
        // 1 マスずつの当たり方は Minecraft の BlockGetter.clip と同じ（水などを見ない Fluid.NONE なので、液体の当たりは調べない）
        BlockHitResult hit = BlockGetter.traverseBlocks(from, to, context, (ctx, pos) -> {
            // 建てられる高さの外は、Minecraft がチャンクを読まずに空気と答えるので、読み込まれていなくても通り抜ける（pick と同じ）
            if (!level.isOutsideBuildHeight(pos) && !BlockJob.isLoaded(level, pos)) {
                return stop;
            }
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = ctx.getBlockShape(state, level, pos);
            return level.clipWithInteractionOverride(ctx.getFrom(), ctx.getTo(), pos, shape, state);
        }, ctx -> stop);
        return hit != stop && hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private JsonObject executeGetPlayerFacing(JsonObject params) {
        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            logInputError("No player found for getPlayerFacing", "No player found for getPlayerFacing");
            return null;
        }

        // 列に並んで、メインスレッドで読む（今いる場所と同じ。前の命令が終わってからの向きを答えるように。
        // 列がとても混んでいるときや、2.5 秒待っても順番が来ないときは、列を追い越して読む）。
        // プレイヤーは順番が来たときに引き直す
        return callInQueue("getPlayerFacing", () -> true, () -> {
            ServerPlayer current = getFirstPlayer();
            if (current == null) {
                logInputError("No player found for getPlayerFacing", "No player found for getPlayerFacing");
                return null;
            }

            // プレーヤーの向き（yaw）から方向を判定
            // yaw: 0=南、90=西、180=北、270=東
            float yaw = current.getYRot();

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

            MinecraftEduMod.LOGGER.debug("Player facing: " + facing + " (yaw: " + yaw + ")");

            // 結果データを設定
            JsonObject result = new JsonObject();
            result.addProperty("facing", facing);
            result.addProperty("yaw", yaw);

            return result;
        });
    }

    private JsonObject executeGetBlockType(JsonObject params) {
        // ワールドの外の座標は断る（int にする前に確かめる。大きすぎる数は上の桁が落ちて、別の場所を読んでしまうため）
        if (!isWithinWorldLimit(params.get("x")) || !isWithinWorldLimit(params.get("y")) || !isWithinWorldLimit(params.get("z"))) {
            logInputError("getBlockType coordinates out of world limit", "getBlockType coordinates out of world limit");
            sendErrorChat("ばしょがワールドの外です");
            return null;
        }

        int x = params.get("x").getAsInt();
        int y = params.get("y").getAsInt();
        int z = params.get("z").getAsInt();

        BlockPos pos = new BlockPos(x, y, z);

        // 列に並んで、メインスレッドで読む（前の命令で置いたブロックが見えるように。
        // 列に並ばずに読むと、大きな「埋める」の途中の、置く前のブロックを答えてしまうため）
        // まだ読み込まれていないチャンクなら、読み込まれるのを待ってから読む（その場で読むと Minecraft が止まるため。
        // 建てられる高さの外は、Minecraft がチャンクを読まずに答えるので待たない）
        return callInQueue("getBlockType", () -> {
            ServerLevel world = server.overworld();
            return world == null || world.isOutsideBuildHeight(pos) || BlockJob.requestLoaded(world, pos, false);
        }, () -> {
            ServerLevel world = server.overworld();
            if (world == null) {
                return null;
            }

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

            MinecraftEduMod.LOGGER.debug("Block type retrieved: " + blockType + " at " + x + "," + y + "," + z);

            // 結果データを設定
            JsonObject result = new JsonObject();
            result.addProperty("blockType", blockType);
            JsonObject position = new JsonObject();
            position.addProperty("x", x);
            position.addProperty("y", y);
            position.addProperty("z", z);
            result.add("position", position);

            return result;
        });
    }

    /**
     * プレイヤーの足元のブロックを調べる（チャットには出さない）
     *
     * 立っているときは、体を支えているブロック（ブロックのふちに立っても、フェンスや塀の上でも、テレポートの直後でも、
     * 乗っているブロックになる。ハーフブロックや階段の上ならそのブロック、水の底に立てば底のブロック。footBlockPos を参照）。
     * ジャンプ中や泳いでいるときは、足の高さのマス（空気や水）になる。
     * getBlockType と違って毎回チャットにブロック名を出さないので、
     * Scratch の「ずっと」の中で何度も呼んでもチャットが流れない（忍者屋敷ステージの見張り用）。
     *
     * メインスレッドで、列を追い越して読む（いまのプレイヤーの様子を読むだけなので、前の命令を待たない。
     * 列に並ぶと、大きな「埋める」の途中で答えが遅れ、見張りが止まるため）。
     * 足元のチャンクがまだ読み込まれていなければ（テレポートの直後など）、読み込まれるのを待ってから読む
     * （その場で読むと、Minecraft が止まるため）
     */
    private JsonObject executeGetBlockUnderPlayer(JsonObject params) {
        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            logInputError("No player found for getBlockUnderPlayer", "No player found for getBlockUnderPlayer");
            return null;
        }

        // プレイヤーと足元は、メインスレッドで読むときに引き直す（やられて生き返ると、プレイヤーが作り直されるため）。
        // ready と読み取りは同じ回の中で続けて動くので、ready で確かめたチャンクのまま読む
        return callAheadOfQueue("getBlockUnderPlayer", () -> {
            ServerPlayer current = getFirstPlayer();
            if (current == null) {
                return true;  // 読み取りの中で、いないことを知らせる
            }
            return footChunksLoaded(current);
        }, () -> {
            ServerPlayer current = getFirstPlayer();
            if (current == null) {
                logInputError("No player found for getBlockUnderPlayer", "No player found for getBlockUnderPlayer");
                return null;
            }
            BlockPos pos = footBlockPos(current);
            BlockState blockState = current.serverLevel().getBlockState(pos);
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());

            // "minecraft:" を取って、getBlockType と同じ形の名前にする
            String blockType = blockId.toString();
            if (blockType.startsWith("minecraft:")) {
                blockType = blockType.substring(10);
            }

            // 何度も呼ばれるので、ログは debug にとどめる
            MinecraftEduMod.LOGGER.debug("Block under player: " + blockType + " at " + pos.getX() + "," + pos.getY() + "," + pos.getZ());

            JsonObject result = new JsonObject();
            result.addProperty("blockType", blockType);
            JsonObject position = new JsonObject();
            position.addProperty("x", pos.getX());
            position.addProperty("y", pos.getY());
            position.addProperty("z", pos.getZ());
            result.add("position", position);

            return result;
        });
    }

    /**
     * 足元のマスを探すのに要るチャンクが、全部読み込まれているか（メインスレッドで呼ぶ）
     * 読み込まれていなければ、読み込みを頼んで false を返す（getChunkNow で確かめるだけなので、ここでは待たない）。
     * footBlockPos が当たりを調べる範囲は、当たりの箱の前後 1 マスまで（Minecraft の BlockCollisions と同じ）なので、
     * チャンクの境目に立つと、となりのチャンクも要る
     */
    private static boolean footChunksLoaded(ServerPlayer player) {
        AABB box = player.getBoundingBox();
        return BlockJob.requestLoaded(player.serverLevel(),
                (Mth.floor(box.minX - 1.0E-7) - 1) >> 4, (Mth.floor(box.maxX + 1.0E-7) + 1) >> 4,
                (Mth.floor(box.minZ - 1.0E-7) - 1) >> 4, (Mth.floor(box.maxZ + 1.0E-7) + 1) >> 4, false);
    }

    /**
     * 足元のマスを決める（メインスレッドで、footChunksLoaded が true を返したのと同じ回の中で呼ぶ）
     *
     * まず、体の当たりの箱のすぐ下に触れているブロックを探す（Minecraft が「乗っているブロック」を探すのと同じ関数）。
     * 触れているブロックが無ければ（ジャンプ中・泳いでいるとき）、体の位置から足の高さのマスを計算する。
     * Minecraft の getOnPos を使わないのは、テレポートの直後に答えがずれるため。テレポートしてすぐは、
     * テレポートの前に乗っていたブロックを覚えたままで、そのあと画面側から「まだ地面に着いていない」と知らせが来ると空になり、
     * 次に「着いた」と知らせが来る（約 1 秒後）まで、フェンスの上では上の空気、ブロックの端では体のまん中の下のマスになる。
     * 当たりを調べる関数は、読み込みの途中のチャンクに触れると、読み込み終わるまで待ってしまう（Minecraft が止まる）ので、
     * 先に footChunksLoaded で要るチャンクが全部そろっていることを確かめてから呼ぶ
     */
    private static BlockPos footBlockPos(ServerPlayer player) {
        AABB box = player.getBoundingBox();
        AABB below = new AABB(box.minX, box.minY - 1.0E-6, box.minZ, box.maxX, box.minY, box.maxZ);
        return player.level().findSupportingBlock(player, below)
                .orElseGet(() -> BlockPos.containing(player.getX(), player.getY() - 1.0E-5, player.getZ()));
    }

    private JsonObject executeSummonEntity(JsonObject params) {
        // ワールドの外の座標は断る（Minecraft の /summon と同じく、ワールドの端の外や高すぎる所に生き物を出さない）
        if (!isWithinSpawnableBounds(params)) {
            logInputError("summonEntity coordinates out of world limit", "summonEntity coordinates out of world limit");
            sendErrorChat("ばしょがワールドの外です");
            return null;
        }

        // エンティティ召喚が禁止されている場合は拒否
        if (!entitySpawningAllowed) {
            logInputError("Entity spawning is disabled", "Entity spawning is disabled - command rejected");
            sendErrorChat("いまは いきものを だせない せっていです");
            return null;
        }

        String entityType = params.get("entityType").getAsString();
        double x = params.get("x").getAsDouble();
        double y = params.get("y").getAsDouble();
        double z = params.get("z").getAsDouble();

        // ENTITY_TYPE レジストリも、見つからない名前に対して null ではなく既定（ブタ）を返す。
        // 打ち間違いでブタが出ないよう、名前として正しいかと登録されているかを先に確かめる。
        ResourceLocation entityId = ResourceLocation.tryParse(entityType);
        if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            logInputError("Unknown entity type", "Unknown entity type: " + sanitizeForLog(entityType));
            sendErrorChat("その いきものの なまえが ちがいます: " + chatSafeName(entityType));
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        // 壊す・燃やすもの、Minecraft の /summon でも出せないもの（プレイヤーなど）は断る
        if (BLOCKED_SUMMON_TYPES.contains(type) || !type.canSummon()) {
            logInputError("Blocked entity type", "Blocked entity type: " + entityId);
            sendErrorChat("その いきものは ここでは だせません: " + chatSafeName(entityType));
            return null;
        }

        boolean queued = safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;
            net.minecraft.world.entity.Entity entity = type.create(world);
            if (entity instanceof Projectile) {
                // 弾（矢・雪玉など）は、作ってみないと分からないので、ここで断る（返事はもう成功で返している）
                logInputError("Blocked entity type", "Blocked entity type: " + entityId);
                sendErrorChat("その いきものは ここでは だせません: " + chatSafeName(entityType));
                return;
            }

            if (entity != null) {
                entity.setPos(x, y, z);
                SummonedEntityGuard.mark(entity);  // 作品を壊させない印
                world.addFreshEntity(entity);
            }
        });
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは safeExecute が出す）
        }

        // 列に並べただけ（弾だったときは、列の中で断る）
        MinecraftEduMod.LOGGER.debug("Entity summon queued: " + entityType + " at " + x + "," + y + "," + z);
        return new JsonObject();
    }

    private JsonObject executeTeleport(JsonObject params) {
        // ワールドの外の座標は断る（Minecraft の /tp と同じく、ワールドの端の外や高すぎる所へは行かせない）
        if (!isWithinSpawnableBounds(params)) {
            logInputError("Teleport coordinates out of world limit", "Teleport coordinates out of world limit");
            sendErrorChat("ばしょがワールドの外です");
            return null;
        }

        double x = params.get("x").getAsDouble();
        double y = params.get("y").getAsDouble();
        double z = params.get("z").getAsDouble();

        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            logInputError("No player found for teleport", "No player found for teleport");
            return null;
        }

        // プレイヤーは順番が来たときに引き直す（列で待つ間にやられて生き返ると、プレイヤーが新しく作り直されるため）
        // 1 人用のワールドが止まっている（ポーズ中）間は、止まるたびに 1 回まで通し、それより先は動き出すまで列で待つ
        // （止まっている間にテレポートすると、画面側の返事でサーバーがプレイヤーを動かし、行き先の見える範囲のチャンクを読み込むが、
        //   止まっている間は下ろされないので、くり返すとたまってメモリを使い切るため。ふつうの地形で、1 回で 1500 チャンク・
        //   190 MB ほど増えた。1 回は通すので、Scratch のブラウザを触っている間も「テレポートしてから建てる」は動き、
        //   そのあとの「今いる場所」もテレポートした先を答える。
        //   同じ一時停止の中の 2 回目からは待つので、そのあとの「今いる場所」は、2.5 秒待ってから前の場所を答える。
        //   前に通してから 7 秒ほど動かないうちに止まり直したときと、メモリにテレポート 2 回分と予算の分の余白が無いときは、
        //   1 回目も待つ（PauseWatch.takeTeleport）。
        //   止まる直前に動いていたテレポートは、画面側の返事が止まったあとに届くので、その行き先も止まっている間に読み込まれる
        //   （1 回の一時停止でテレポート 2 回分まで。メモリの余白はその 2 回分と予算の分で見て、チャンクの数は PauseWatch.overLimit で数える））
        boolean queued = enqueue(deadlineNanos -> {
            if (PauseWatch.paused() && !PauseWatch.takeTeleport(server)) {
                return false;
            }
            ServerPlayer current = getFirstPlayer();
            if (current != null) {
                current.teleportTo(x, y, z);
            }
            return true;
        });
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは enqueue が出す）
        }

        MinecraftEduMod.LOGGER.debug("Player teleported to " + x + "," + y + "," + z);
        return new JsonObject();
    }

    private JsonObject executeSetWeather(JsonObject params) {
        String weather = params.get("weather").getAsString();
        // 知らない天気の名前は断る（前は何もせずに「天気: （名前）」と出して成功を返していた）
        if (!weather.equals("clear") && !weather.equals("rain") && !weather.equals("thunder")) {
            logInputError("Unknown weather", "Unknown weather: " + sanitizeForLog(weather));
            sendErrorChat("その てんきは ありません: " + chatSafeName(weather));
            return null;
        }

        boolean queued = safeExecute(() -> {
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
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは safeExecute が出す）
        }

        // チャット表示
        sendChatMessage("天気: " + getWeatherNameJapanese(weather));

        MinecraftEduMod.LOGGER.debug("Weather set to: " + sanitizeForLog(weather));
        return new JsonObject();
    }

    private JsonObject executeSetTime(JsonObject params) {
        long time = params.get("time").getAsLong();

        boolean queued = safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;
            world.setDayTime(time);
        });
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは safeExecute が出す）
        }

        // チャット表示
        sendChatMessage("時刻: " + getTimeNameJapanese(time));

        MinecraftEduMod.LOGGER.debug("Time set to: " + time);
        return new JsonObject();
    }

    private JsonObject executeSetGameMode(JsonObject params) {
        String mode = params.get("mode").getAsString();

        ServerPlayer player = getFirstPlayer();
        if (player == null) {
            logInputError("No player found for setGameMode", "No player found for setGameMode");
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
                logInputError("Unknown game mode", "Unknown game mode: " + sanitizeForLog(mode));
                sendErrorChat("その ゲームモードは ありません: " + chatSafeName(mode));
                return null;
        }

        // プレイヤーは順番が来たときに引き直す（テレポートと同じ理由）
        boolean queued = safeExecute(() -> {
            ServerPlayer current = getFirstPlayer();
            if (current != null) {
                current.setGameMode(gameType);
            }
        });
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは safeExecute が出す）
        }

        MinecraftEduMod.LOGGER.debug("Game mode set to: " + mode);

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
        // 知らないゲームルールの名前は断る（前は何もせずに「（名前）: ON」と出して成功を返していた）
        if (!rule.equals("doDaylightCycle") && !rule.equals("doWeatherCycle") && !rule.equals("doMobSpawning")) {
            logInputError("Unknown game rule", "Unknown game rule: " + sanitizeForLog(rule));
            sendErrorChat("その ルールは ありません: " + chatSafeName(rule));
            return null;
        }
        boolean boolValue = value.equalsIgnoreCase("true");

        boolean queued = safeExecute(() -> {
            ServerLevel world = server.overworld();
            if (world == null) return;
            net.minecraft.world.level.GameRules gameRules = world.getGameRules();

            switch (rule) {
                case "doDaylightCycle":
                    boolean invertedDaylightValue = !boolValue;
                    gameRules.getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                        .set(invertedDaylightValue, server);
                    MinecraftEduMod.LOGGER.debug("GameRule set: doDaylightCycle = " + invertedDaylightValue + " (Scratch value: " + sanitizeForLog(value) + ")");
                    break;
                case "doWeatherCycle":
                    boolean invertedWeatherValue = !boolValue;
                    gameRules.getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE)
                        .set(invertedWeatherValue, server);
                    MinecraftEduMod.LOGGER.debug("GameRule set: doWeatherCycle = " + invertedWeatherValue + " (Scratch value: " + sanitizeForLog(value) + ")");
                    break;
                case "doMobSpawning":
                    gameRules.getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)
                        .set(boolValue, server);
                    MinecraftEduMod.LOGGER.debug("GameRule set: doMobSpawning = " + boolValue);
                    break;
                default:
                    // 入口で 3 つの名前以外は断っているので、ここには来ない（念のため）
                    logInputError("Unknown game rule", "Unknown game rule: " + sanitizeForLog(rule));
                    return;
            }
        });
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは safeExecute が出す）
        }

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

        // 「[」があっても「]」で終わっていなければ、全体を名前として扱う（名前の打ちまちがいとして断られる）
        if (bracketIndex > 0 && blockTypeString.endsWith("]")) {
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
        // BLOCK レジストリは見つからない名前に対して null ではなく「空気」を返す（DefaultedRegistry）。
        // そのまま使うと、打ち間違いの名前で範囲が空気に置き換わり、作った建物が消えてしまう。
        // 名前として正しいか（tryParse）と、登録されているか（containsKey）を先に確かめる。
        ResourceLocation blockId = ResourceLocation.tryParse(blockTypeName);
        if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) {
            logInputError("Unknown block type", "Unknown block type: " + sanitizeForLog(blockTypeName));
            sendErrorChat("その ブロックの なまえが ちがいます: " + chatSafeName(blockTypeName));
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(blockId);

        // デフォルトのBlockStateを取得
        BlockState blockState = block.defaultBlockState();

        // プロパティを適用（知らないプロパティは数えて、最後に 1 行だけログに出す）
        int unknownProperties = 0;
        String firstUnknownProperty = null;
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
                if (unknownProperties == 0) {
                    firstUnknownProperty = propertyName;
                }
                unknownProperties++;
            }
        }
        if (unknownProperties > 0) {
            logInputError("Unknown property", "Unknown property '" + sanitizeForLog(firstUnknownProperty) + "' for block "
                + sanitizeForLog(blockTypeName) + " (" + unknownProperties + " unknown properties)");
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
            logInputError("Invalid property value", "Invalid value '" + sanitizeForLog(value) + "' for property '" + property.getName() + "'");
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
        if (clearAreaInProgress.get()) {
            logInputError("clearArea in progress", "clearArea: 既に実行中です");
            notifyAheadOfQueue(CLEAR_AREA_BUSY_CHAT, lastClearAreaBusyNoticeNanos);
            return null;
        }

        // ワールドの外の中心は断る（int にする前に確かめる。centerX + 200 があふれると、下のループが終わらなくなるため）
        // 中心のキーが無ければ断る（isWithinWorldLimit が MissingParameterException を投げる。
        // 決まった中心 (0, 0) で動かすと、原点のまわりの作品が消えるため）
        if (!isWithinWorldLimit(params.get("centerX")) || !isWithinWorldLimit(params.get("centerZ"))) {
            logInputError("clearArea", "clearArea: center out of world limit");
            sendErrorChat("ばしょがワールドの外です");
            return null;
        }

        // 中心座標を取得（上で、キーがあってワールドの中だと確かめてある）
        int centerX = params.get("centerX").getAsInt();
        int centerZ = params.get("centerZ").getAsInt();

        int minX = centerX - 200;
        int maxX = centerX + 200;
        int minZ = centerZ - 200;
        int maxZ = centerZ + 200;

        // チャンク列（16×16）ごとに分けて、1 つの仕事として列に並べる
        // （列の中で続きから進めるので、あとから届いた命令が途中に割りこまない。
        //   前は区画を 1 つ終えるたびに次を Minecraft の待ち列の最後に足していたので、
        //   あとから置いたブロックが割りこみ、まだの区画で空気に上書きされていた）
        BlockState bedrock = net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState();
        BlockState dirt = net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        BlockState grass = net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        IntFunction<BlockState> layer = y -> {
            if (y == -64) {
                return bedrock;
            } else if (y >= -63 && y <= -62) {
                return dirt;
            } else if (y == -61) {
                return grass;
            }
            return air;
        };
        // 列はチャンクの境でそろえる（1 つの列が 1 つのチャンクに収まり、チケットも 1 つで済むように）
        List<FillJob> columns = new ArrayList<>();
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            int cxStart = Math.max(minX, chunkX << 4);
            int cxEnd = Math.min(maxX, (chunkX << 4) + 15);
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                int czStart = Math.max(minZ, chunkZ << 4);
                int czEnd = Math.min(maxZ, (chunkZ << 4) + 15);
                // フラグ2: クライアントに通知するが隣接ブロック更新なし
                // フラグ16: 近傍ブロックの形状更新を抑制
                columns.add(new FillJob(server, cxStart, -64, czStart, cxEnd, 100, czEnd, layer, 2 | 16));
            }
        }
        ClearAreaJob job = new ClearAreaJob(columns, centerX, centerZ);

        // 列が混んでいたら断る（すぐ知らせる）
        if (!buildQueue.hasRoomFor(job.weight())) {
            logInputError("Build queue full", "Build queue full: clearArea");
            notifyBusy();
            return null;
        }
        // 印を付ける（上の確かめのあとに別の接続が先に始めていたら、ここで断る）
        if (!clearAreaInProgress.compareAndSet(false, true)) {
            logInputError("clearArea in progress", "clearArea: 既に実行中です");
            notifyAheadOfQueue(CLEAR_AREA_BUSY_CHAT, lastClearAreaBusyNoticeNanos);
            return null;
        }
        if (!buildQueue.offer(job, commandOwner.get())) {
            clearAreaInProgress.set(false);
            logInputError("Build queue full", "Build queue full: clearArea");
            notifyBusy();
            return null;
        }

        JsonObject result = new JsonObject();
        result.addProperty("blocksCleared", 26573265);
        result.addProperty("centerX", centerX);
        result.addProperty("centerZ", centerZ);
        result.addProperty("totalChunks", columns.size());
        return result;
    }

    /**
     * 「周りをきれいにする」の仕事（チャンク列ごとの FillJob を順に進め、25% ごとに進み具合をチャットに出す）
     */
    private class ClearAreaJob implements BuildQueue.Job {
        // 先にチケットを付けておく、この先の列の数（置く順番が来る前に、チャンクの読み込みを進めておくため。
        // 少ないと、列ごとに読み込みを待って遅くなる）
        private static final int PREFETCH_COLUMNS = 16;

        private final List<FillJob> columns;
        private final int centerX, centerZ;
        // 列にたまる量の重さ（並べたときと終わったときで同じ値を使うので、先に決めておく）
        // 置くブロックの数（約 2,650 万）のままだと、きれいにしている間ずっと上限を超えて、
        // そのあとに届いた「埋める」（土台など）がみな断られ、作品から黙って抜けるので、いちばん大きい「埋める」と同じ重さまでにする
        private final long weight;
        // 次に進めるチャンク列
        private int index = 0;
        // 始めたことをチャットで知らせたか
        private boolean announced = false;
        // まだ終わっていないか（終わる・やめる・取り消すのうち、最初の 1 つだけが「周りをきれいにする」の途中の印を下ろす。
        // 取り消しは受け口のスレッドで起きるので、取り消したあとに別の接続が始めた印を、こちらが下ろさないように）
        private final AtomicBoolean active = new AtomicBoolean(true);

        ClearAreaJob(List<FillJob> columns, int centerX, int centerZ) {
            this.columns = columns;
            this.centerX = centerX;
            this.centerZ = centerZ;
            long sum = 0;
            for (FillJob column : columns) {
                sum += column.weight();
            }
            this.weight = Math.min(sum, MAX_FILL_VOLUME);
        }

        @Override
        public long weight() {
            return weight;
        }

        @Override
        public void cancel() {
            // 止めた・切れたので、そこでやめる。印はすぐ下ろす（緑の旗で始め直した「周りをきれいにする」を断らないように）
            if (active.compareAndSet(true, false)) {
                clearAreaInProgress.set(false);
            }
        }

        @Override
        public void release() {
            // 先に付けたチケットを、全部の列から外す（メインスレッドで BuildQueue が呼ぶ。外すのは自分の列のチケットだけなので、
            // 次の仕事が始まったあとでもよい。もう外した列は何もしない）
            columns.forEach(FillJob::removeTickets);
        }

        @Override
        public long footprint() {
            // 片づけを待つ間も、列（FillJob）を全部持っている
            return columns.size();
        }

        @Override
        public boolean step(long deadlineNanos) {
            // 始めたことは、列で順番が来たときに知らせる（列に足せなかったときに「開始」だけ出ないように。
            // メインスレッドの上なので、チャットはその場で出る）
            if (!announced) {
                announced = true;
                sendChatMessage("§a周囲クリア開始 (" + columns.size() + "チャンク)");
            }
            while (index < columns.size()) {
                // この先の列のチケットを先に付ける（失敗しても、いまの列はそのまま進める）
                try {
                    ServerLevel world = server.overworld();
                    if (world != null) {
                        for (int ahead = index + 1; ahead <= index + PREFETCH_COLUMNS && ahead < columns.size(); ahead++) {
                            columns.get(ahead).prepare(world);
                            // 消すときに読むとなりのチャンクも、先に読み込みを頼む（時間で切れるチケットなので、回ごとに付け直す）
                            columns.get(ahead).prefetchNeighbors(world);
                        }
                    }
                } catch (Exception e) {
                    MinecraftEduMod.LOGGER.error("チャンクの先読みエラー (index=" + index + "): " + e.getMessage());
                }
                try {
                    if (!columns.get(index).step(deadlineNanos)) {
                        return false;  // 時間が来たか、チャンクの読み込みを待っている。次はこの列の続きから
                    }
                } catch (Exception e) {
                    // 1 つの列のエラーで全体を止めない（終わりの印を下ろせなくなるため）
                    MinecraftEduMod.LOGGER.error("チャンク処理エラー (index=" + index + "): " + e.getMessage());
                }
                if (columns.get(index).gaveUp()) {
                    // チャンクの読み込みを待ちきれなかった。残りもやめる（列ごとに待って知らせると、何分も続くため）
                    // （上の catch の外で呼ぶ。中で例外が起きても、次の列へ進まないように）
                    giveUp();
                    return true;
                }
                index++;
                // 25%ごとに進捗をチャットに表示
                int quarter = columns.size() / 4;
                if (quarter > 0 && index < columns.size() && index % quarter == 0) {
                    sendChatMessage("§a周囲クリア進行中... " + (index * 100) / columns.size() + "%");
                }
                if (index < columns.size() && System.nanoTime() - deadlineNanos >= 0) {
                    return false;
                }
            }
            if (!active.compareAndSet(true, false)) {
                return true;  // 進めている間に取り消された（止めた・切れた）。完了とは知らせない
            }
            clearAreaInProgress.set(false);
            sendChatMessage("§a周囲クリア完了: 中心(" + centerX + ", " + centerZ + ")");
            MinecraftEduMod.LOGGER.info("チャンク分割タスク完了: " + columns.size() + "チャンク処理済み");
            return true;
        }

        /**
         * 途中でやめる（先に付けた、この先の列のチケットを外し、終わりの印を下ろして知らせる）
         */
        private void giveUp() {
            if (!active.compareAndSet(true, false)) {
                return;  // 進めている間に取り消された。チケットは取り消しの側が外す
            }
            // 印は先に下ろす（チケットを外す途中で例外が起きても、次の「周りをきれいにする」を受け付けられるように）
            clearAreaInProgress.set(false);
            for (int rest = index + 1; rest < columns.size(); rest++) {
                columns.get(rest).removeTickets();
            }
            logInputError("clearArea chunk not loaded", "clearArea: gave up waiting for a chunk to load at column "
                + index + "/" + columns.size());
            sendErrorChat(LOAD_GAVE_UP_CHAT_PREFIX + "まわりを きれいにするのを とちゅうで やめました");
        }
    }

    /**
     * 全エンティティをクリア
     * 中心座標から±200の範囲のエンティティを削除（プレイヤーを除く）
     */
    private JsonObject executeClearAllEntities(JsonObject params) {
        // ワールドの外の中心は断る（int にする前に確かめる。大きすぎる数は上の桁が落ちて原点の近くになり、
        // centerX ± 200 があふれると、範囲がワールド全体に広がって、たくさんの生き物が消え、サーバーも長く止まるため）
        // 中心のキーが無ければ断る（isWithinWorldLimit が MissingParameterException を投げる。
        // 決まった中心 (0, 0) で動かすと、原点のまわりの作品が消えるため）
        if (!isWithinWorldLimit(params.get("centerX")) || !isWithinWorldLimit(params.get("centerZ"))) {
            logInputError("clearAllEntities", "clearAllEntities: center out of world limit");
            sendErrorChat("ばしょがワールドの外です");
            return null;
        }

        // 中心座標を取得（上で、キーがあってワールドの中だと確かめてある）
        int centerX = params.get("centerX").getAsInt();
        int centerZ = params.get("centerZ").getAsInt();

        boolean queued = safeExecute(() -> {
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
                    MinecraftEduMod.LOGGER.debug("エンティティ削除: " + entityType + " at " + entity.blockPosition());

                    // より確実な削除方法：remove()とkill()を両方試行
                    entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                    entity.kill();
                    entitiesRemoved++;
                }
            }

            MinecraftEduMod.LOGGER.debug("エンティティクリア完了: 中心(" + centerX + ", " + centerZ + ") から " + entitiesRemoved + "体");
        });
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは safeExecute が出す）
        }

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
            logInputError("setMoveSpeed: multiplier parameter required", "setMoveSpeed: multiplier parameter required");
            sendErrorChat("うまく できませんでした（いれた あたいを たしかめてね）");
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
            logInputError("setMoveSpeed: No player found", "setMoveSpeed: No player found");
            return null;
        }

        boolean queued = safeExecute(() -> {
            // 全プレイヤーに適用
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(newSpeed);
            }
            MinecraftEduMod.LOGGER.debug("SetMoveSpeed: " + finalMultiplier + "x (speed=" + newSpeed + ")");
        });
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは safeExecute が出す）
        }

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
            logInputError("setNightVision: enabled parameter required", "setNightVision: enabled parameter required");
            sendErrorChat("うまく できませんでした（いれた あたいを たしかめてね）");
            return null;
        }

        boolean enabled = params.get("enabled").getAsBoolean();

        boolean queued = safeExecute(() -> {
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
                    MinecraftEduMod.LOGGER.debug("Night vision enabled for player: " + player.getName().getString());
                } else {
                    player.removeEffect(MobEffects.NIGHT_VISION);
                    MinecraftEduMod.LOGGER.debug("Night vision disabled for player: " + player.getName().getString());
                }
            }
        });
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは safeExecute が出す）
        }

        // チャット表示
        sendChatMessage("暗視: " + (enabled ? "ON" : "OFF"));

        JsonObject result = new JsonObject();
        result.addProperty("nightVision", enabled);

        return result;
    }

    /**
     * プレイヤーの持ち物を空にする（バニラの /clear と同じく、手・防具・左手も消す）
     *
     * クリエイティブで手に入れた物（エンダーパール・コーラスフルーツなど）を、
     * アドベンチャーに変えたあとに使えないようにするため（忍者屋敷ステージの忍び込み用）。
     * サバイバルの持ち物をうっかり消して戻せなくならないように、アドベンチャーのときだけ消す。
     * 消すかどうかはメインスレッドで決めるので、直前に送った「モードをアドベンチャーにする」のあとで調べる。
     * 消したかどうかはメインスレッドで決まるので、返事は「受け付けた」だけを返し、結果はチャットで知らせる。
     * 「ずっと」の中で送られても流れないよう、アドベンチャーでないときの知らせは失敗の知らせ（同じ種類は 3 秒に 1 回）にし、
     * 「空にしました」は、本当に何かを消したときだけ出す。
     */
    private JsonObject executeClearInventory(JsonObject params) {
        if (getFirstPlayer() == null) {
            logInputError("No player found for clearInventory", "No player found for clearInventory");
            return null;
        }

        // プレイヤーは順番が来たときに引き直す（列で待つ間にやられて生き返ると、プレイヤーが新しく作り直されるため。
        // 古いプレイヤーを使うと、古い持ち物を消して、いまの持ち物が残る）
        boolean queued = safeExecute(() -> {
            ServerPlayer current = getFirstPlayer();
            if (current == null) {
                logInputError("No player found for clearInventory", "No player found for clearInventory");
                return;
            }
            if (current.gameMode.getGameModeForPlayer() != GameType.ADVENTURE) {
                logInputError("clearInventory not adventure", "clearInventory skipped: not in adventure mode");
                sendErrorChat("もちものを からっぽに できるのは、アドベンチャーの ときだけです");
                return;
            }
            // バニラの /clear（ClearInventoryCommands）と同じ消し方と、画面への知らせ方
            int removed = current.getInventory().clearOrCountMatchingItems(stack -> true, -1, current.inventoryMenu.getCraftSlots());
            MinecraftEduMod.LOGGER.debug("Inventory cleared: " + removed + " items");
            // 消した物があったときだけ、画面に知らせる（「ずっと」で呼ばれても、空のたびにレシピを調べ直して送らないように）
            if (removed > 0) {
                current.containerMenu.broadcastChanges();
                current.inventoryMenu.slotsChanged(current.getInventory());
                sendChatMessage("持ち物を空にしました");
            }
        });
        if (!queued) {
            return null;  // 列がいっぱい・サーバーが止まりかけている（知らせは safeExecute が出す）
        }

        JsonObject result = new JsonObject();
        result.addProperty("accepted", true);

        return result;
    }

    /**
     * 飛行速度の設定（クリエイティブモード用）
     * @param params multiplier: 速度倍率（例: 1.0=標準、2.0=2倍）
     */
    private JsonObject executeSetFlySpeed(JsonObject params) {
        if (!params.has("multiplier")) {
            logInputError("setFlySpeed: multiplier parameter required", "setFlySpeed: multiplier parameter required");
            sendErrorChat("うまく できませんでした（いれた あたいを たしかめてね）");
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

        boolean queued = safeExecute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.getAbilities().setFlyingSpeed(newFlySpeed);
                player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
            }
        });
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは safeExecute が出す）
        }

        // チャット表示
        sendChatMessage("飛行速度: " + finalMultiplier + "倍");

        MinecraftEduMod.LOGGER.debug("SetFlySpeed: " + finalMultiplier + "x (speed=" + newFlySpeed + ")");

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
     * サーバ停止時の後処理（2 回呼んでもよい）
     * このフラグが立った後、execute() は全てnullを返し、列（BuildQueue）にも仕事を足さない。
     * 列に残った仕事は捨て、列のタイマーも止める（途中の「埋める」が付けたチャンクのチケットは、ワールドといっしょに捨てられる）。
     */
    public void shutdown() {
        this.serverStopping = true;
        // 列に残った仕事を捨てる（「周りをきれいにする」の途中の印は、下の clearAreaInProgress で下ろす）
        this.buildQueue.clear();
        // 列の仕事を待っている読む命令を、すぐ放す（受け口のスレッドが残って、ワールドを閉じるのが遅れないように）
        for (CompletableFuture<JsonObject> read : pendingReads) {
            read.cancel(false);
        }
        this.clearAreaInProgress.set(false);

        // 録画中であれば強制停止（ゾンビプロセス防止）
        // 開始直後に割り込まれた場合は isRecording が false でもプロセスが残るため、
        // 条件を付けずに forceResetRecording() へ一本化する。
        // 停止処理が進行中ならロック解放まで最大約5秒待つが、これは意図的な挙動。
        // 途中で殺すと mp4 が壊れるため、書き終えてから後始末する（有界・デッドロックなし）
        try {
            forceResetRecording();
        } catch (Exception e) {
            // 後始末の失敗は ffmpeg の生き残り（画面録画の継続）に直結するため、無音にしない
            MinecraftEduMod.LOGGER.error("録画の後始末に失敗しました", e);
        }
        MinecraftEduMod.LOGGER.info("CommandExecutor: shutdown signaled");
    }

    /**
     * Scratch の止めるボタン（緑の旗を押したときも、始める前に止める）
     * この接続から来て、まだ置いていない命令を列から外す（途中の「埋める」もそこでやめる。ほかのタブの命令は残す。
     * 遠くに置き続けて列がたまったとき、止めたあとも何分も置き続けないように）。録画は止めない
     */
    private JsonObject executeStopAll(JsonObject params) {
        int cancelled = buildQueue.cancel(commandOwner.get());
        if (cancelled > 0) {
            MinecraftEduMod.LOGGER.debug("stopAll: cancelled " + cancelled + " queued commands");
            // 止めるボタンでも緑の旗（始める前に止める）でも合う言い方にする
            notifyCancelled("まだ おいていない めいれい " + cancelled + " こを やめました");
        }
        JsonObject result = new JsonObject();
        result.addProperty("cancelled", cancelled);
        return result;
    }

    /**
     * 接続が切れた（タブを閉じた・Scratch が切った）。その接続から来て、まだ置いていない命令と、その接続の列を外す（受け口のスレッドで呼ぶ）
     */
    public void connectionClosed(Object owner) {
        if (serverStopping) {
            return;
        }
        int cancelled = buildQueue.forget(owner);
        if (cancelled > 0) {
            MinecraftEduMod.LOGGER.debug("Connection closed: cancelled " + cancelled + " queued commands");
            notifyCancelled("Scratch との つながりが きれたので、まだ おいていない めいれい " + cancelled + " こを やめました");
        }
    }

    /**
     * まだ置いていない命令をやめたことを、列に並ばずにすぐ知らせる（3 秒に 1 回まで。止めるボタンを続けて押されても流れないように）
     */
    private void notifyCancelled(String message) {
        long now = System.nanoTime();
        long last = lastCancelNoticeNanos.get();
        if (last != 0 && now - last < ERROR_CHAT_INTERVAL_NANOS) {
            return;
        }
        if (lastCancelNoticeNanos.compareAndSet(last, now)) {
            net.minecraft.network.chat.Component text = net.minecraft.network.chat.Component.literal("[MinecraftEdu] " + message);
            executeAheadOfQueue(() -> sendToAllPlayers(text));
        }
    }

    /**
     * サーバ停止中かどうかを返す（MinecraftWebSocketHandler から参照される）
     */
    public boolean isServerStopping() {
        return this.serverStopping;
    }

    /**
     * メインスレッドで動かす
     * すでにメインスレッドの上（ほかの処理の中）なら、その場で動かす
     * （Minecraft はメインスレッドの処理の中で頼んだ処理を、すぐには動かさず待ち列の最後に回すので、
     *   チャットの順や失敗のチャットの間引きがずれないように）
     * ほかのスレッドからなら、safeExecute で待ち列に入れる
     *
     * @return 動かしたか、列に並べたら true（サーバーが止まりかけている・列がいっぱいなら false）
     */
    private boolean runOnMainThread(Runnable task) {
        if (!server.isSameThread()) {
            return safeExecute(task);
        }
        if (serverStopping || !server.isRunning()) {
            return false;
        }
        try {
            task.run();
        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("runOnMainThread task error", e);
        }
        return true;
    }

    /**
     * メインスレッドで動かす仕事を、世界を変える仕事の列（BuildQueue）の最後に並べる
     *
     * 1. 入口でserverStopping をチェック → trueなら何もしない
     * 2. server.isRunning() でForge公式APIも併せてチェック
     * 3. 列で順番が来たときも再度 serverStopping / isRunning() をチェックする（BuildQueue が見る）
     *    （並べてから動くまでの間に停止状態に遷移する可能性があるため）
     * 4. 仕事の中の例外を捕捉してログ出力（メインスレッドに例外を漏らさない）
     *
     * 命令の仕事はすべてここを通すので、届いた順に動く（大きな「埋める」のあとの命令は、それが終わるまで待つ）
     */
    private boolean safeExecute(Runnable task) {
        return enqueue(BuildQueue.of(() -> {
            try {
                task.run();
            } catch (Exception e) {
                MinecraftEduMod.LOGGER.error("safeExecute task error", e);
            }
        }));
    }

    /**
     * 仕事を、世界を変える仕事の列（BuildQueue）の最後に並べる（守りは safeExecute と同じ。
     * 仕事の中の例外は BuildQueue が捕まえてログに出し、その仕事をやめる）
     *
     * @return 列に並べたら true（サーバーが止まりかけている・列がいっぱいなら false。いっぱいのときは知らせる）
     */
    private boolean enqueue(BuildQueue.Job job) {
        if (serverStopping || !server.isRunning()) {
            return false;
        }
        boolean queued = buildQueue.offer(job, commandOwner.get());
        if (!queued && !serverStopping) {
            // 列にたまった仕事の数が上限に届いた（「ずっと」の中で送り続けたときなど）
            logInputError("Build queue full", "Build queue full: too many queued commands");
            notifyBusy();
        }
        return queued;
    }

    /**
     * 列が混んでいることを、列に並ばずにすぐ知らせる（3 秒に 1 回まで。送り続けられても、知らせがたまらないように）
     */
    private void notifyBusy() {
        notifyAheadOfQueue(BUSY_CHAT, lastBusyNoticeNanos);
    }

    /**
     * 知らせを、列に並ばずにすぐ出す（送る前に 3 秒に 1 回までにしぼる。送り続けられても、Minecraft の待ち列に知らせがたまらないように）
     *
     * @param message 知らせの文
     * @param lastNanos その知らせを最後に出した時刻
     */
    private void notifyAheadOfQueue(String message, AtomicLong lastNanos) {
        long now = System.nanoTime();
        long last = lastNanos.get();
        if (last != 0 && now - last < ERROR_CHAT_INTERVAL_NANOS) {
            return;
        }
        if (lastNanos.compareAndSet(last, now)) {
            sendErrorChatAheadOfQueue(message);
        }
    }

    /**
     * 読む命令を、メインスレッドで動かして答えを待つ
     *
     * 列にたまった仕事が少ないときは、列に並んで前の命令が終わるのを待つ（前の命令で置いたブロックも見えるように）。
     * 多いとき（大きな「埋める」の途中など）や、並んで READ_QUEUE_WAIT_MILLIS 待っても順番が来ないときは、
     * 列を追い越して今の様子を読む（置いている途中の様子になることがある）。
     * 待ちすぎると、その間このタブのほかの命令も止まり、Scratch が待つのをやめて「空気」と答えてしまうため。
     * 答えには、列に並んで読んだか（queued）を入れる（試験で、前の命令が終わったかを確かめるのに使う）
     *
     * 読む前に ready で、読む所のチャンクが読み込まれているかを見る。まだなら、ready が読み込みを頼み、読まずに待つ
     * （列の中では、その仕事のところで列が待つ。列を追い越すときは、READ_POLL_MILLIS ごとに聞き直す）。
     * 待つ時間は上と同じ上限の中なので、読み込みが間に合わなければ失敗になる（そのときはチャットで知らせる）
     *
     * @param what ログに出す命令の名前
     * @param ready メインスレッドで、読む用意ができたか（読み込みを待つなら false）
     * @param task メインスレッドで動かす読み取り（答えが無ければ null）
     * @return 答え（失敗したら null）
     */
    private JsonObject callInQueue(String what, BooleanSupplier ready, Supplier<JsonObject> task) {
        if (server.isSameThread()) {
            // メインスレッドで待つと、列が進まず止まってしまうので、その場で動かす（いまは呼ばれない）。
            // ready が false なら読まずに答えなしにする（読み込みの途中のチャンクを読むと、Minecraft が止まるため）
            if (!ready.getAsBoolean()) {
                return null;
            }
            return withQueued(task.get(), false);
        }
        // 列を追い越して読むときに待つ時間（列に並ばなかったときは、列で待つはずだった時間も、読み込みを待つのに使う）
        long nowWaitMillis = READ_QUEUE_WAIT_MILLIS + READ_NOW_WAIT_MILLIS;
        PendingRead queued = null;
        if (buildQueue.pendingWeight() <= READ_WAIT_MAX_PENDING_WEIGHT) {
            queued = new PendingRead(ready, task);
            PendingRead read = queued;
            if (enqueue(deadlineNanos -> read.run())) {
                JsonObject answer = awaitRead(what, queued.future, READ_QUEUE_WAIT_MILLIS);
                if (queued.future.isDone() || serverStopping) {
                    return withQueued(answerIfDone(what, queued.future, answer), true);
                }
                if (!queued.abandon()) {
                    // 待ち終わるのと同時に順番が来て、もう読み始めた（または読み終えた）。その答えを使う
                    // （列を追い越して読み直すと、ブロックの名前のチャットが 2 回出るため）
                    return withQueued(awaitRead(what, queued.future, READ_FINISH_WAIT_MILLIS), true);
                }
                // 順番が来なかった（または読み込みを待っていた）。並んだ仕事は、次の回に何もせず終わる（abandon で決まった）
                logInputError(what + " waited too long", what + ": read without waiting for the build queue");
                nowWaitMillis = READ_NOW_WAIT_MILLIS;
            } else if (serverStopping) {
                return null;
            }
        }
        return readAheadOfQueue(what, ready, task, nowWaitMillis, queued);
    }

    /**
     * 読む命令を、列に並ばずにメインスレッドで動かして答えを待つ（いまの様子を読むだけで、前の命令を待たなくてよいもの）
     * ready・task と待つ時間の上限は callInQueue と同じ（列で待つはずだった時間も、読み込みを待つのに使う）
     *
     * @param what ログに出す命令の名前
     * @param ready メインスレッドで、読む用意ができたか（読み込みを待つなら false）
     * @param task メインスレッドで動かす読み取り（答えが無ければ null）
     * @return 答え（失敗したら null）
     */
    private JsonObject callAheadOfQueue(String what, BooleanSupplier ready, Supplier<JsonObject> task) {
        if (server.isSameThread()) {
            // メインスレッドで待つと止まってしまうので、その場で動かす（いまは呼ばれない）。
            // ready が false なら読まずに答えなしにする（読み込みの途中のチャンクを読むと、Minecraft が止まるため）
            if (!ready.getAsBoolean()) {
                return null;
            }
            return withQueued(task.get(), false);
        }
        return readAheadOfQueue(what, ready, task, READ_QUEUE_WAIT_MILLIS + READ_NOW_WAIT_MILLIS, null);
    }

    /**
     * 列を追い越して読む（READ_POLL_MILLIS ごとにメインスレッドに聞き直し、nowWaitMillis まで待つ）
     *
     * @param queued 先に列に並べて、やめた読み取り（読み込みを待っていたかを知らせに使う。並べなかったら null）
     */
    private JsonObject readAheadOfQueue(String what, BooleanSupplier ready, Supplier<JsonObject> task,
                                        long nowWaitMillis, PendingRead queued) {
        PendingRead now = new PendingRead(ready, task);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(nowWaitMillis);
        JsonObject answer = null;
        while (true) {
            if (!executeAheadOfQueue(now::run)) {
                return null;  // サーバーが止まりかけている
            }
            long left = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            answer = awaitRead(what, now.future, Math.max(1, Math.min(READ_POLL_MILLIS, left)));
            if (now.future.isDone() || serverStopping || left <= READ_POLL_MILLIS) {
                break;
            }
        }
        // 答えが無ければ、やめられるかで決める（「終わったか」を先に見ると、見たあとに読み終えたときに答えを捨てるため）
        boolean abandoned = false;
        if (answer == null && !serverStopping) {
            if (now.abandon()) {
                abandoned = true;
            } else {
                // 読み始めた（または読み終えた）。その答えを受け取る
                answer = awaitRead(what, now.future, READ_FINISH_WAIT_MILLIS);
            }
        }
        if (abandoned && (now.waitedForLoad || (queued != null && queued.waitedForLoad))) {
            // チャンクの読み込みが間に合わなかった（Scratch は「空気」と答えるので、まだ読めていないことを知らせる。
            // 列に並べると、前の仕事が終わるまで出ないので、列を追い越して出す）
            logInputError(what + " chunk not loaded", what + ": the chunk did not load in time");
            sendErrorChatAheadOfQueue(READ_LOAD_PENDING_CHAT);
        }
        return withQueued(answer, false);
    }

    /**
     * 読み取りが終わっていれば、その答え（か、メインスレッドで起きた例外）を受け取り直す。終わっていなければ answer のまま
     * （待つのを区切った直後に読み終えると、awaitRead は null を返したまま future だけが終わっているので、
     *   読めていたのに失敗として返さないように）
     */
    private JsonObject answerIfDone(String what, CompletableFuture<JsonObject> future, JsonObject answer) {
        return future.isDone() ? awaitRead(what, future, READ_FINISH_WAIT_MILLIS) : answer;
    }

    /**
     * 読む命令 1 回分の読み取り
     * メインスレッドが読み始めるのと、待つ側が待ちきれずにやめるのは、先に来た方だけが通る
     * （やめた読み取りをあとからメインスレッドが動かして、ブロックの名前のチャットが 2 回出たり、
     *   読み終えた答えを待つ側が捨てたりしないように）
     * 読む所のチャンクが読み込まれるまでは、読み始めない（その間は、待つ側がやめられる）
     */
    private static final class PendingRead {
        // まだ読んでいない・メインスレッドが読み始めた・待つ側がやめた
        private static final int WAITING = 0;
        private static final int READING = 1;
        private static final int ABANDONED = 2;

        private final AtomicInteger state = new AtomicInteger(WAITING);
        private final CompletableFuture<JsonObject> future = new CompletableFuture<>();
        private final BooleanSupplier ready;
        private final Supplier<JsonObject> task;
        // チャンクの読み込みを待ったことがあるか（間に合わなかったときに知らせるため）
        private volatile boolean waitedForLoad;

        PendingRead(BooleanSupplier ready, Supplier<JsonObject> task) {
            this.ready = ready;
            this.task = task;
        }

        /**
         * 読み取りを動かして答えを入れる（メインスレッド。待つ側がもうやめていたら何もしない）
         *
         * @return 終わったら true（読んだ・待つ側がやめていた）。チャンクの読み込みを待つときは false
         */
        boolean run() {
            if (state.get() != WAITING) {
                return true;
            }
            try {
                if (!ready.getAsBoolean()) {
                    waitedForLoad = true;
                    return false;  // 読み込みを頼んだ。次の回にまた見る
                }
            } catch (RuntimeException e) {
                // 用意の確かめで失敗した。読み取りと同じく、待つ側に投げ直してもらう
                if (state.compareAndSet(WAITING, READING)) {
                    future.completeExceptionally(e);
                }
                return true;
            }
            if (!state.compareAndSet(WAITING, READING)) {
                return true;
            }
            try {
                future.complete(task.get());
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
            }
            return true;
        }

        /**
         * 待つのをやめる（まだ読み始めていなければ true。true のあとは、順番が来ても読まない）
         */
        boolean abandon() {
            return state.compareAndSet(WAITING, ABANDONED);
        }
    }

    /**
     * 読み取りの答えを、決まった時間まで待つ（来なければ null。メインスレッドの例外は投げ直す）
     * 待っている間は pendingReads に入れておき、ワールドを閉じるときに shutdown がすぐ放す
     */
    private JsonObject awaitRead(String what, CompletableFuture<JsonObject> future, long waitMillis) {
        pendingReads.add(future);
        try {
            // shutdown が pendingReads を放し終えたあとに入れたときは、ここで放す（待ち続けないように）
            if (serverStopping) {
                future.cancel(false);
                return null;
            }
            return future.get(waitMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | CancellationException e) {
            return null;
        } catch (ExecutionException e) {
            // メインスレッドで起きた例外は、ここで投げ直す（値の誤りなどの分け方を、ほかの命令と同じにするため）
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            MinecraftEduMod.LOGGER.error(what + " failed", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            pendingReads.remove(future);
        }
    }

    /**
     * 答えに、列に並んで読んだか（queued）を入れる
     */
    private static JsonObject withQueued(JsonObject answer, boolean queued) {
        if (answer != null) {
            answer.addProperty("queued", queued);
        }
        return answer;
    }

    /**
     * 列に並ばず、すぐメインスレッドで動かす（列が混んでいることの知らせなど、待たせると意味のないものだけ）
     * 守りは safeExecute と同じ
     *
     * @return メインスレッドに頼んだら true（サーバーが止まりかけていて頼まなかったら false）
     */
    private boolean executeAheadOfQueue(Runnable task) {
        // Minecraft が落ちたあと（isStopped）は、頼んでも下の守りで動かないので、頼まなかったことにする
        // （読む命令が、答えの来ない待ちをしないように）
        if (serverStopping || !server.isRunning() || server.isStopped()) {
            return false;
        }
        server.execute(() -> {
            // Minecraft が落ちたあとは、頼んだ処理が呼んだスレッドの上でそのまま動くので、そのときも動かさない
            // （落ちたときは止める知らせが来ないので、isStopped でも見る）
            if (serverStopping || !server.isRunning() || server.isStopped() || !server.isSameThread()) {
                return;
            }
            try {
                task.run();
            } catch (Exception e) {
                MinecraftEduMod.LOGGER.error("executeAheadOfQueue task error", e);
            }
        });
        return true;
    }

    // ========================================
    // チャット表示用ヘルパーメソッド
    // ========================================

    /**
     * ゲーム内チャットにメッセージを送信 (署名なし・コンポーネント指定)
     */
    private void sendRawChatMessage(net.minecraft.network.chat.Component message) {
        runOnMainThread(() -> sendToAllPlayers(message));
    }

    /**
     * ゲーム内チャットにメッセージを送信
     */
    private void sendChatMessage(String message) {
        runOnMainThread(() -> sendToAllPlayers(net.minecraft.network.chat.Component.literal("[MinecraftEdu] " + message)));
    }

    /**
     * 失敗をチャットで知らせる（赤い文字）
     * 「ずっと」の中で失敗が続いてもチャットが流れないよう、同じ種類の失敗は 3 秒に 1 回だけ出す
     * （ちがう種類の失敗がかわりばんこに起きても、種類ごとに 3 秒に 1 回。
     *   間にほかのチャットが出たら、すぐまた出す）
     * 種類は文の「: 」より前（後ろに付く名前は、打ちまちがえるたびに変わるため）
     * 出すかどうかは、メインスレッドで実際に出す直前に決める
     * （受け取ったスレッドで決めると、Minecraft が止まっていた間にたまった失敗が、止まりが解けたとたんに
     *   続けて出たり、ほかのチャットとの順がずれたりするため）
     */
    private void sendErrorChat(String message) {
        runOnMainThread(errorChatTask(message));
    }

    /**
     * 失敗のチャットを、列に並ばずにすぐ出す（列が混んでいることの知らせ。並ぶと、混んでいる仕事が終わるまで出ないため）
     * 間引きは sendErrorChat と同じ
     */
    private void sendErrorChatAheadOfQueue(String message) {
        Runnable task = errorChatTask(message);
        if (server.isSameThread()) {
            if (!serverStopping && server.isRunning()) {
                task.run();
            }
            return;
        }
        executeAheadOfQueue(task);
    }

    /**
     * 失敗のチャットを出す処理（メインスレッドで動かす。同じ種類が 3 秒以内に出ていたら出さない）
     */
    private Runnable errorChatTask(String message) {
        int nameStart = message.indexOf(": ");
        String kind = nameStart >= 0 ? message.substring(0, nameStart) : message;
        net.minecraft.network.chat.Component text = net.minecraft.network.chat.Component.literal("[MinecraftEdu] §c" + message);
        return () -> {
            long now = System.nanoTime();
            Long last = errorChatNanos.get(kind);
            if (last != null && now - last < ERROR_CHAT_INTERVAL_NANOS) {
                return;
            }
            // ほかのチャット（sendToAllPlayers）が表を空にするので、ここでは直接送る
            errorChatNanos.put(kind, now);
            server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(text));
        };
    }

    /**
     * 全員のチャットに出す（メインスレッドで呼ぶ）
     * 失敗のチャットの間引きは、ほかのチャットが出たら解く（間に別の文が出たら、次の失敗はすぐまた出す）
     */
    private void sendToAllPlayers(net.minecraft.network.chat.Component text) {
        errorChatNanos.clear();
        server.getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(text));
    }

    /**
     * 送られた値の形の誤り（数でない・null など）で起きる例外か
     * （Gson の getAsDouble などが、JsonNull や数でない文字で出す）
     */
    private static boolean isBadParameterException(Exception e) {
        // 送られた値にキーが無いときの NullPointerException も値の誤り
        // （Java 17 の例外の説明には、null のまま使ったのが Gson の値（JsonObject・JsonElement）だと書かれる。
        //   isWithinWorldLimit を通る座標のキーが無いときは、MissingParameterException になる）
        if (e instanceof NullPointerException) {
            return String.valueOf(e.getMessage()).contains("com.google.gson.Json");
        }
        if (e instanceof MissingParameterException || e instanceof NumberFormatException) {
            return true;
        }
        // 次の 3 つは、送られた値を Gson で読むときに起きたもの（例外の一番上の場所が Gson）だけを値の誤りとする
        // （Minecraft や MOD の本物の不具合で起きたものは、スタックトレース付きで調べられるように）
        // スタックトレースが無いとき（JVM が何度も起きる例外の記録を省いたとき）は、ほとんどが Gson なので値の誤りとする
        if (e instanceof UnsupportedOperationException || e instanceof IllegalStateException
                || e instanceof ClassCastException) {
            StackTraceElement[] trace = e.getStackTrace();
            return trace.length == 0 || trace[0].getClassName().startsWith("com.google.gson.");
        }
        return false;
    }

    /**
     * 入力の誤りのログを出してよいか（呼ぶ場所ごとに 60 秒に 1 回まで）
     */
    private boolean shouldLogInputError(String key) {
        long now = System.nanoTime();
        synchronized (inputErrorLogNanos) {
            Long last = inputErrorLogNanos.get(key);
            if (last != null && now - last < INPUT_ERROR_LOG_INTERVAL_NANOS) {
                return false;
            }
            inputErrorLogNanos.put(key, now);
            return true;
        }
    }

    /**
     * 入力の誤り（ワールドの外・名前の打ちまちがいなど）をログに出す
     * 呼ぶ場所（key）ごとに 60 秒に 1 回だけ warn で出し、そのほかは debug にする
     */
    private void logInputError(String key, String message) {
        if (shouldLogInputError(key)) {
            MinecraftEduMod.LOGGER.warn(message + "（同じ失敗は 60 秒間 debug にだけ出す）");
        } else {
            MinecraftEduMod.LOGGER.debug(message);
        }
    }

    /**
     * Minecraft のチャットと同じ数え方（Java の文字の数。絵文字は 2 つと数える）で切る
     * 絵文字の途中で切れるときは、その絵文字ごと落とす
     */
    private static String cutToChars(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        int end = maxChars;
        if (Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    /**
     * 表示やログを乱す文字を ? にする（keepColorCode が true なら、色を変える § は残す）
     */
    private static String replaceUnsafeChars(String value, boolean keepColorCode) {
        StringBuilder sb = new StringBuilder(value.length());
        value.codePoints().forEach(c -> {
            if (isUnsafeChatChar(c) && !(keepColorCode && c == '§')) {
                sb.append('?');
            } else {
                sb.appendCodePoint(c);
            }
        });
        return sb.toString();
    }

    /**
     * チャットに出してよい形にする（子どもが打った名前を、見える文字だけにして短く切る）
     * 「§」（色を変える記号）や制御文字などは ? にする
     */
    private static String chatSafeName(String value) {
        if (value == null) return "";
        // 文字（コードポイント）の数で切る（絵文字などを途中で切らないように）
        String cut = value.codePointCount(0, value.length()) > MAX_CHAT_NAME_CHARS
            ? value.substring(0, value.offsetByCodePoints(0, MAX_CHAT_NAME_CHARS)) + "…"
            : value;
        StringBuilder sb = new StringBuilder(cut.length());
        cut.codePoints().forEach(c -> {
            if (isUnsafeChatChar(c)) {
                sb.append('?');
            } else {
                sb.appendCodePoint(c);
            }
        });
        return sb.toString();
    }

    /**
     * チャットに出すと表示を乱す文字か
     * （制御文字、色を変える記号の §、文字の向きを変える記号、幅の無い文字、行・段落の区切り）
     */
    private static boolean isUnsafeChatChar(int c) {
        // ZWJ（U+200D）と ZWNJ（U+200C）は、絵文字をつなぐ（👩‍💻 など）・ペルシア語などで使うので残す
        if (c == 0x200C || c == 0x200D) {
            return false;
        }
        int type = Character.getType(c);
        return c == '§'
            || type == Character.CONTROL               // 制御文字（改行・ESC・C1 など）
            || type == Character.FORMAT                // 見えない書式の文字（向きを変える記号・幅の無い文字・タグ文字など）
            || type == Character.SURROGATE             // ペアになっていない壊れた文字
            || type == Character.LINE_SEPARATOR        // U+2028
            || type == Character.PARAGRAPH_SEPARATOR;  // U+2029
    }

    /**
     * ログに出してよい形にする（相手が送った文字に改行などがあっても、偽の行を作れないように）
     * 見える ASCII 以外は ? にして 100 文字で切る（SimpleWebSocketServer と同じ決まりを使う）
     */
    private static String sanitizeForLog(String value) {
        return SimpleWebSocketServer.sanitizeForLog(value);
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
            default: return chatSafeName(weather);  // 入口で断っているので、ふつうは来ない
        }
    }

    private String getGameRuleNameJapanese(String rule) {
        switch (rule) {
            case "doDaylightCycle": return "時刻固定";
            case "doWeatherCycle": return "天気固定";
            case "doMobSpawning": return "モブスポーン";
            default: return chatSafeName(rule);  // 入口で断っているので、ふつうは来ない
        }
    }

    // ========================================
    // 録画機能
    // ========================================

    /**
     * 録画の失敗理由を Scratch 側へ返すための結果を作る
     * 子どもに見せる文言は Scratch 側が errorCode から組み立てる。
     * ここで日本語の文言を作らないのは、ひらがな表示や多言語に対応できなくなるため。
     * @param errorCode 失敗理由を表すコード（Scratch 側の表示文言と対応）
     * @param logMessage ログに残す説明。画面には出さない
     * @return success:false と errorCode を持つ結果
     */
    private JsonObject createRecordingError(String errorCode, String logMessage) {
        // 「ずっと」の中で呼ばれても増えないよう、失敗の種類（errorCode。決まった値）ごとに間引く
        logInputError("録画エラー[" + errorCode + "]", "録画エラー[" + errorCode + "]: " + logMessage);
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("errorCode", errorCode);
        return result;
    }

    /**
     * プロセスのパイプを閉じる
     * 閉じ忘れるとファイルディスクリプタが GC まで残るため、後始末で必ず呼ぶ。
     * どのストリームが本物のパイプかは出力先の設定と OS で変わるため、リダイレクト先に関わらず 3 本とも閉じる
     * @param process 対象のプロセス
     */
    private void closeProcessStreams(Process process) {
        try { process.getOutputStream().close(); } catch (Exception ignored) {}
        try { process.getInputStream().close(); } catch (Exception ignored) {}
        try { process.getErrorStream().close(); } catch (Exception ignored) {}
    }

    /**
     * 録画の状態を強制的に初期化する
     * 停止に失敗したまま録画中フラグが残ると二度と開始できなくなるため、後始末に使う
     */
    private void forceResetRecording() {
        synchronized (recordingLock) {
            if (ffmpegProcess != null) {
                closeProcessStreams(ffmpegProcess);
                ffmpegProcess.destroyForcibly();
                ffmpegProcess = null;
            }
            isRecording = false;
            currentRecordingPath = null;
        }
    }

    /**
     * ffmpeg の出力先を決める
     * pb.start() はログファイルを開けないだけでも IOException を投げ、それが FFMPEG_MISSING と
     * 区別できないため、先に自分で開いて確かめる（開いてから pb.start() までの間に状況が変われば
     * 区別できないが、実用上は十分）。開けなければ録画自体は止めずに出力を捨てる
     * @param recordingsDir 録画フォルダ
     * @return ログファイルへの上書き出力。開けない場合は DISCARD
     */
    private ProcessBuilder.Redirect openFfmpegLog(File recordingsDir) {
        File logFile = new File(recordingsDir, FFMPEG_LOG_NAME);
        try (FileOutputStream probe = new FileOutputStream(logFile)) {
            return ProcessBuilder.Redirect.to(logFile);
        } catch (IOException e) {
            logInputError("ffmpeg log file", "ffmpeg のログファイルを開けないため出力を捨てます: " + e.getMessage());
            return ProcessBuilder.Redirect.DISCARD;
        }
    }

    /**
     * 録画を開始する
     * 「録画中か確かめてから開始する」の間に割り込まれないようロックで直列化する
     */
    private JsonObject executeStartRecording(JsonObject params) {
        synchronized (recordingLock) {
            return startRecordingLocked(params);
        }
    }

    private JsonObject startRecordingLocked(JsonObject params) {
        // ロック待ちの間に shutdown() が走ることがあるため、ここでも停止状態を再確認する。
        // execute() の入口チェックだけだと、shutdown 完了後に ffmpeg を起動してしまい
        // 以降の停止要求は serverStopping で拒否されるため誰も止められなくなる
        if (serverStopping || !server.isRunning()) {
            return createRecordingError("SERVER_STOPPING", "サーバ停止中のため録画を開始しません");
        }

        if (isRecording) {
            return createRecordingError("RECORDING_ALREADY_STARTED", "すでに録画中です");
        }

        // 画面キャプチャに Windows 専用の gdigrab を使っているため、他のOSでは動作しない
        // Locale.ROOT 指定はトルコ語ロケールで 'I' が 'ı' になり判定を誤るのを避けるため
        String osName = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
        if (!osName.contains("win")) {
            return createRecordingError("RECORDING_WINDOWS_ONLY", "Windows以外のOSです: " + osName);
        }

        try {
            String userHome = System.getProperty("user.home");
            File recordingsDir = new File(userHome, "MinecraftRecordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs();
            }
            // フォルダが作れない（同名のファイルがある等）と、ffmpeg が出力先を書けずに起動直後に
            // 終了し、ログも残らないため「ウィンドウが見つからない」と区別できなくなる。先に弾く
            if (!recordingsDir.isDirectory()) {
                return createRecordingError("RECORDING_START_FAILED",
                    "録画フォルダを作成できません: " + recordingsDir.getAbsolutePath());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            String filename = "recording_" + timestamp + ".mp4";
            boolean customNameGiven = false;

            // オブジェクト・配列・null が来たら例外で COMMAND_FAILED にせず、指定なしとして扱う
            JsonElement filenameElement = params != null ? params.get("filename") : null;
            if (filenameElement != null && filenameElement.isJsonPrimitive()) {
                String customName = filenameElement.getAsString();
                if (!customName.isEmpty()) {
                    // ポート 14711 は無認証のため、Scratch 以外から "../" 入りの名前が届きうる。
                    // 英数字と _ - だけに絞れば区切り文字・".."・代替データストリーム（":"）を弾ける。
                    // 弾いた名前は改行などを含みうるのでログには載せず、長さだけ残す。
                    // この文字種検査を最初に置くことが前提で、後ろの予約名検査（末尾ドットや
                    // 上付き数字の細工を考えずに済む）とログ出力の安全性が成り立っている
                    if (!SAFE_FILENAME.matcher(customName).matches()) {
                        return createRecordingError("RECORDING_START_FAILED",
                            "ファイル名に使えない文字が含まれています（長さ " + customName.length() + "）");
                    }
                    // ここに来る名前は上の文字種検査を通っているので、そのままログに載せてよい
                    if (RESERVED_FILENAME.matcher(customName).matches()) {
                        return createRecordingError("RECORDING_START_FAILED",
                            "Windows の予約名はファイル名に使えません: " + customName);
                    }
                    filename = customName + ".mp4";
                    customNameGiven = true;
                }
            }

            // 文字種の制限をすり抜けても録画フォルダの外には書けないよう、正規化したパスで最終確認する。
            // 正規化の失敗を下の catch に落とすと FFMPEG_MISSING（ffmpeg を入れてという案内）になり
            // 原因と食い違うため、ここで別に受ける
            String recordingsDirPath;
            String recordingFilePath;
            try {
                recordingsDirPath = recordingsDir.getCanonicalPath();
                recordingFilePath = new File(recordingsDir, filename).getCanonicalPath();
            } catch (IOException e) {
                MinecraftEduMod.LOGGER.error("録画パスの正規化エラー: " + e.getMessage(), e);
                return createRecordingError("RECORDING_START_FAILED", "録画先のパスを確定できませんでした");
            }
            if (!recordingFilePath.startsWith(recordingsDirPath + File.separator)) {
                return createRecordingError("RECORDING_START_FAILED",
                    "録画フォルダの外への保存は許可しません: " + recordingFilePath);
            }
            // ffmpeg は -y で問答無用に上書きするため、名前の指定で過去の録画を消せないようにする。
            // 既定名は秒精度なので「開始→停止→開始」が同じ秒に収まると衝突するが、消えるのは
            // 1 秒未満の録画だけであり、ここで拒否すると子どもに原因と違うエラーが出るため対象外にする
            if (customNameGiven && new File(recordingFilePath).exists()) {
                return createRecordingError("RECORDING_START_FAILED",
                    "同じ名前の録画がすでにあります: " + recordingFilePath);
            }
            currentRecordingPath = recordingFilePath;

            String[] command = {
                "ffmpeg",
                // -y を外すと既存ファイルがあるとき ffmpeg が上書き確認を標準入力で待ち続け、
                // 起動直後の生存確認を「成功」と誤判定するので外さない（消すなら -n に置き換える）
                "-y",
                // 0.5秒ごとの進捗表示とバナーを止め、下のログファイルを小さく保つ
                "-nostats",
                "-hide_banner",
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
            // ffmpeg の出力は誰も読まない。パイプのままだとバッファが埋まった時点で ffmpeg が
            // 書き込み待ちで止まり、長時間の録画が静かに途切れるため、ファイルへ流して OS に任せる。
            // ffmpeg の診断はほぼ stderr に出るので、redirectErrorStream(true) で stdout に合流させた
            // 上でファイルへ流す（この 1 行を消すと stderr がパイプのまま残り、詰まりが再発する）。
            // 捨てずに残すのは、起動直後に終了したときの理由（ウィンドウが見つからない等）を
            // 後から調べられるようにするため。追記だと起動失敗の繰り返しで際限なく増えるので
            // 起動ごとに上書きし、直前の 1 回分だけを残す。
            // 標準入力は停止時に 'q' を送るために使うので、パイプのまま残す
            pb.redirectErrorStream(true);
            ProcessBuilder.Redirect ffmpegOutput = openFfmpegLog(recordingsDir);
            pb.redirectOutput(ffmpegOutput);
            ffmpegProcess = pb.start();

            Thread.sleep(500);
            if (!ffmpegProcess.isAlive()) {
                forceResetRecording();
                // ログを開けずに捨てた回は参照先が無いので、案内を変える
                String hint = ffmpegOutput == ProcessBuilder.Redirect.DISCARD
                    ? "（今回の ffmpeg 出力は保存されていません）"
                    : "（理由は " + FFMPEG_LOG_NAME + " を参照）";
                return createRecordingError("RECORDING_START_FAILED",
                    "FFmpegプロセスが起動直後に終了しました" + hint);
            }

            isRecording = true;
            // 「ずっと」の中でくり返されても増えないよう debug（パスには PC のユーザー名も入る）
            MinecraftEduMod.LOGGER.debug("録画開始: " + currentRecordingPath);

            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("isRecording", true);
            result.addProperty("filePath", currentRecordingPath);
            result.addProperty("message", "録画を開始しました");

            return result;

        } catch (IOException e) {
            // ffmpeg が無い PC ではいつも起きるので、「ずっと」の中で呼ばれても増えないよう間引く
            // （詳しい中身は 60 秒に 1 回だけ。中身は OS の文で、送られた文字は入らない）
            if (shouldLogInputError("録画開始エラー")) {
                MinecraftEduMod.LOGGER.error("録画開始エラー: " + e.getMessage(), e);
            } else {
                MinecraftEduMod.LOGGER.debug("録画開始エラー: " + e.getMessage());
            }
            forceResetRecording();
            return createRecordingError("FFMPEG_MISSING", "FFmpegを起動できませんでした: " + e.getMessage());
        } catch (InterruptedException e) {
            MinecraftEduMod.LOGGER.error("録画開始中断: " + e.getMessage(), e);
            // 後始末をしないと ffmpeg が生き残ったまま止められなくなる
            forceResetRecording();
            // 割り込み状態を復元しないとスレッドプールの停止シグナルを握り潰す
            Thread.currentThread().interrupt();
            return createRecordingError("RECORDING_INTERRUPTED", "録画開始が割り込まれました");
        }
    }

    /**
     * 録画を停止する
     * 開始側と同じロックで直列化し、状態の読み書きが交錯しないようにする
     */
    private JsonObject executeStopRecording(JsonObject params) {
        synchronized (recordingLock) {
            return stopRecordingLocked(params);
        }
    }

    private JsonObject stopRecordingLocked(JsonObject params) {
        if (!isRecording || ffmpegProcess == null) {
            return createRecordingError("RECORDING_NOT_STARTED", "録画が開始されていません");
        }

        try {
            ffmpegProcess.getOutputStream().write('q');
            ffmpegProcess.getOutputStream().flush();

            boolean exited = ffmpegProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);

            if (!exited) {
                ffmpegProcess.destroyForcibly();
                MinecraftEduMod.LOGGER.warn("FFmpegプロセスを強制終了しました");
            }

            closeProcessStreams(ffmpegProcess);
            isRecording = false;
            String recordedPath = currentRecordingPath;
            ffmpegProcess = null;
            currentRecordingPath = null;

            MinecraftEduMod.LOGGER.debug("録画停止: " + recordedPath);

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
            forceResetRecording();
            return createRecordingError("RECORDING_STOP_FAILED", "FFmpegへの停止指示に失敗しました: " + e.getMessage());
        } catch (InterruptedException e) {
            MinecraftEduMod.LOGGER.error("録画停止中断: " + e.getMessage(), e);
            forceResetRecording();
            // 割り込み状態を復元しないとスレッドプールの停止シグナルを握り潰す
            Thread.currentThread().interrupt();
            return createRecordingError("RECORDING_STOP_FAILED", "録画停止が割り込まれました");
        }
    }

    /**
     * 録画状態を返す
     * Scratch 側はこの結果を「正」として自身の状態を上書きするため、
     * 開始・停止と同じロックで直列化し、処理途中の値（ffmpeg 起動後・フラグ更新前）を返さない
     */
    private JsonObject executeGetRecordingStatus(JsonObject params) {
        synchronized (recordingLock) {
            JsonObject result = new JsonObject();
            result.addProperty("isRecording", isRecording);
            if (isRecording && currentRecordingPath != null) {
                result.addProperty("filePath", currentRecordingPath);
            }
            return result;
        }
    }

    private JsonObject executeSetEntitySpawning(JsonObject params) {
        boolean enabled = params.get("enabled").getAsBoolean();
        this.entitySpawningAllowed = enabled;

        sendChatMessage("エンティティ召喚: " + (enabled ? "許可" : "禁止"));

        MinecraftEduMod.LOGGER.debug("Entity spawning " + (enabled ? "enabled" : "disabled") + " via WebSocket");

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
        // ワールドの外の座標は断る（int にする前に確かめる。大きすぎる数は上の桁が落ちて、別の場所の入れ物に入ってしまうため）
        if (!isWithinWorldLimit(params.get("x")) || !isWithinWorldLimit(params.get("y")) || !isWithinWorldLimit(params.get("z"))) {
            logInputError("setContainerItem coordinates out of world limit", "setContainerItem coordinates out of world limit");
            sendErrorChat("ばしょがワールドの外です");
            return null;
        }

        // スロットの番号も int にする前に確かめる（4294967296 などが 0 になり、スロット 0 の中身を上書きしないように）
        double slotValue = params.get("slot").getAsDouble();
        if (!(slotValue >= 0 && slotValue <= Integer.MAX_VALUE)) {
            logInputError("setContainerItem slot out of range", "setContainerItem slot out of range");
            sendErrorChat("その スロットの ばんごうは つかえません");
            return null;
        }

        int x = params.get("x").getAsInt();
        int y = params.get("y").getAsInt();
        int z = params.get("z").getAsInt();
        int slot = params.get("slot").getAsInt();
        String itemType = params.get("itemType").getAsString();
        int count = params.get("count").getAsInt();

        BlockPos pos = new BlockPos(x, y, z);

        // ITEM レジストリも、見つからない名前に対して null ではなく既定（空気＝空のアイテム）を返す。
        // 打ち間違いでスロットの中身が消えないよう、マイクラ側で実行する前に名前を確かめる。
        ResourceLocation itemId = ResourceLocation.tryParse(itemType);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            logInputError("Unknown item type", "Unknown item type: " + sanitizeForLog(itemType));
            sendErrorChat("その アイテムの なまえが ちがいます: " + chatSafeName(itemType));
            return null;
        }
        final Item item = BuiltInRegistries.ITEM.get(itemId);

        // まだ読み込まれていないチャンクなら、読み込まれるのを待ってから入れる（BlockJob。その場で入れると Minecraft が止まるため。
        // 中身を変えると、入れ物は比較器へ知らせるために 2 マス先まで読むので、端の近くならとなりのチャンクも待つ）
        boolean queued = enqueue(new BlockJob(server, world -> pos, pos, true, (world, target) -> {
            BlockEntity blockEntity = world.getBlockEntity(target);

            if (blockEntity instanceof Container container) {
                if (slot < 0 || slot >= container.getContainerSize()) {
                    logInputError("Invalid slot number", "Invalid slot number: " + slot + " (max: " + (container.getContainerSize() - 1) + ")");
                    sendErrorChat("その スロットの ばんごうは つかえません");
                    return;
                }

                int validCount = Math.min(64, Math.max(1, count));

                ItemStack itemStack = new ItemStack(item, validCount);

                container.setItem(slot, itemStack);
                blockEntity.setChanged();

                MinecraftEduMod.LOGGER.debug("Container item set at " + pos + " slot " + slot + ": " + itemType + " x" + validCount);
            } else {
                logInputError("No container block", "No container block at position: " + pos);
                sendErrorChat("そこに いれものが ありません");
            }
        }, () -> {
            logInputError("setContainerItem chunk not loaded", "setContainerItem: gave up waiting for the chunk to load");
            sendErrorChat(LOAD_GAVE_UP_CHAT_PREFIX + "アイテムを いれられませんでした");
        }));
        if (!queued) {
            return null;  // 列が混んでいて並べられなかった（知らせは enqueue が出す）
        }

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
