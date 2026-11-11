package com.github.minecraftedu;

import com.github.minecraftedu.commands.CommandExecutor;
import com.github.minecraftedu.commands.PauseWatch;
import com.github.minecraftedu.commands.SummonedEntityGuard;
import com.github.minecraftedu.init.ModBlocks;
import com.github.minecraftedu.init.ModItems;
import com.github.minecraftedu.network.SimpleWebSocketServer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("minecraftedu")
public class MinecraftEduMod {

    public static final String MOD_ID = "minecraftedu";
    public static final Logger LOGGER = LogManager.getLogger();

    // volatile: ServerStarting/Stopping イベントスレッドと
    //            /entityspawn コマンドディスパッチスレッドの両方からアクセスされる
    private volatile SimpleWebSocketServer webSocketServer;
    private volatile CommandExecutor commandExecutor;
    // Scratch とつなぐ受け口を開けなかったか（開けなかったら、ワールドに入った人にチャットで知らせる）
    private volatile boolean webSocketStartFailed = false;

    // Scratch とつなぐ受け口のポート
    private static final int WEBSOCKET_PORT = 14711;

    public MinecraftEduMod() {
        // 登録用の MOD のイベントバスを取る
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ブロックとアイテムの登録先を、MOD のイベントバスにつなぐ
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        // クリエイティブの持ち物の画面に、MOD のブロック（縦スラブ・金の階段とハーフブロック）を並べる
        modEventBus.addListener(this::onBuildCreativeTab);
        // 起動の準備のイベントも MOD のイベントバスに出る（Forge のイベントバスにつないでも呼ばれない）
        modEventBus.addListener(this::onCommonSetup);

        // サーバーの起動・停止などのイベントを受け取るため、Forge のイベントバスにこのクラスをつなぐ
        MinecraftForge.EVENT_BUS.register(this);
        // Scratch から出した生き物に、作品を壊させない見張り（static のメソッドなので、クラスをつなぐ）
        MinecraftForge.EVENT_BUS.register(SummonedEntityGuard.class);
        // 1 人用のワールドが止まっている（ポーズ中）かを見て、止まっている間に読み込むチャンクを抑えるため、
        // tick が終わった時刻・数・チャンクの数を覚え、ワールドを閉じたら覚えたものを忘れる
        MinecraftForge.EVENT_BUS.register(PauseWatch.class);

        LOGGER.info("MinecraftEdu Mod initializing...");
        LOGGER.info("Registered custom blocks: " + ModBlocks.BLOCKS.getEntries().size()
            + " (vertical slabs, stairs and slabs)");
    }

    /**
     * この MOD のバージョン（mods.toml の version。Scratch へ返す接続の返事に入れる）
     */
    public static String getModVersion() {
        return ModList.get().getModContainerById(MOD_ID)
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("unknown");
    }

    /**
     * クリエイティブの持ち物の画面の「建築ブロック」のタブに、MOD のブロック（縦スラブ 39 種・金の階段とハーフブロック）を並べる
     * （並べないと、ゲームの中では手に取れず、Scratch か /give でしか出せないため）
     */
    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                event.accept(item);
            }
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("MinecraftEdu common setup");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("MinecraftEdu server starting...");

        try {
            // CommandExecutorを作成（全体で共有）
            MinecraftServer server = event.getServer();
            commandExecutor = new CommandExecutor(server);

            // WebSocketサーバー起動（CommandExecutorを渡す）
            webSocketServer = new SimpleWebSocketServer(WEBSOCKET_PORT, server, commandExecutor);
            webSocketServer.start();
            webSocketStartFailed = false;

            // 「started on port」はサーバー側で出すので、ここでは出さない
            LOGGER.info("Scratch clients can now connect!");
        } catch (Exception e) {
            // ポートがほかのアプリ（別の Minecraft など）に使われているときなど
            webSocketStartFailed = true;
            LOGGER.error("Failed to start WebSocket server", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("MinecraftEdu server stopping...");
        stopAll();
    }

    /**
     * サーバーが止まったあと（Minecraft が落ちたときは ServerStoppingEvent が来ないので、ここでも後始末する。
     * ふつうに止めたときは、もう片づいているので何もしない）
     */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        stopAll();
    }

    /**
     * CommandExecutor（命令の列とタイマーも）と WebSocket サーバを止めて、参照を外す（2 回呼んでもよい）
     */
    private void stopAll() {
        // 最初に CommandExecutor を停止状態にし、新規コマンドを全てブロック
        // ローカル変数にスナップショットしてからnullチェック → スレッドセーフ
        final CommandExecutor ce = this.commandExecutor;
        if (ce != null) {
            try {
                ce.shutdown();
            } catch (Exception e) {
                LOGGER.error("Error shutting down CommandExecutor", e);
            }
        }

        // 続いて WebSocket サーバを停止（クライアント切断・スレッド回収）
        final SimpleWebSocketServer ws = this.webSocketServer;
        if (ws != null) {
            try {
                ws.stop();
                // 「stopped」はサーバー側で出すので、ここでは出さない（起動のログと同じ）
            } catch (Exception e) {
                LOGGER.error("Error stopping WebSocket server", e);
            }
        }

        // 次回 ServerStartingEvent で確実に新規生成されるよう参照をクリア
        this.webSocketServer = null;
        this.commandExecutor = null;
        this.webSocketStartFailed = false;
    }

    /**
     * ワールドに入った人に、Scratch とつなげない状態なら知らせる
     * （受け口を開けなかったことはログにしか出ないので、子どもや先生が気づけるように）
     */
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (webSocketStartFailed) {
            event.getEntity().sendSystemMessage(Component.literal(
                "[MinecraftEdu] §cScratch と つなげません（ポート " + WEBSOCKET_PORT
                    + " ばんを ひらけませんでした。Minecraft が 2 つ ひらいていないか たしかめてね）"));
        }
    }

    /**
     * スラッシュコマンドを登録
     * /entityspawn allow - エンティティ召喚を許可
     * /entityspawn deny - エンティティ召喚を禁止
     * /entityspawn status - 現在の状態を表示
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("entityspawn")
                .requires(source -> source.hasPermission(0))  // 全員使用可能
                .then(Commands.literal("allow")
                    .executes(context -> {
                        // ローカルスナップショット → 別スレッドからの null 化に対して安全
                        final CommandExecutor ce = this.commandExecutor;
                        if (ce != null) {
                            ce.setEntitySpawningAllowed(true);
                            context.getSource().sendSuccess(
                                () -> Component.literal("エンティティ召喚を許可しました / Entity spawning enabled"),
                                true
                            );
                        }
                        return 1;
                    }))
                .then(Commands.literal("deny")
                    .executes(context -> {
                        final CommandExecutor ce = this.commandExecutor;
                        if (ce != null) {
                            ce.setEntitySpawningAllowed(false);
                            context.getSource().sendSuccess(
                                () -> Component.literal("エンティティ召喚を禁止しました / Entity spawning disabled"),
                                true
                            );
                        }
                        return 1;
                    }))
                .then(Commands.literal("status")
                    .executes(context -> {
                        final CommandExecutor ce = this.commandExecutor;
                        if (ce != null) {
                            boolean allowed = ce.isEntitySpawningAllowed();
                            String status = allowed ? "許可 / ALLOWED" : "禁止 / DENIED";
                            context.getSource().sendSuccess(
                                () -> Component.literal("エンティティ召喚: " + status),
                                false
                            );
                        }
                        return 1;
                    }))
        );
        LOGGER.info("Registered /entityspawn command");
    }
}
