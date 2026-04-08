package com.github.minecraftedu;

import com.github.minecraftedu.commands.CommandExecutor;
import com.github.minecraftedu.init.ModBlocks;
import com.github.minecraftedu.init.ModItems;
import com.github.minecraftedu.network.SimpleWebSocketServer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public MinecraftEduMod() {
        // Get the MOD event bus for registration
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register deferred registries to the MOD event bus
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);

        // Register this class to the Forge event bus for server events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("MinecraftEdu Mod initializing...");
        LOGGER.info("Registered custom blocks: vertical_oak_slab");
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent event) {
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
            webSocketServer = new SimpleWebSocketServer(14711, server, commandExecutor);
            webSocketServer.start();

            LOGGER.info("WebSocket server started on port 14711");
            LOGGER.info("Scratch clients can now connect!");
        } catch (Exception e) {
            LOGGER.error("Failed to start WebSocket server", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("MinecraftEdu server stopping...");

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
                LOGGER.info("WebSocket server stopped");
            } catch (Exception e) {
                LOGGER.error("Error stopping WebSocket server", e);
            }
        }

        // 次回 ServerStartingEvent で確実に新規生成されるよう参照をクリア
        this.webSocketServer = null;
        this.commandExecutor = null;
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
