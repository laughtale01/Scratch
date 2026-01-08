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

    private SimpleWebSocketServer webSocketServer;
    private CommandExecutor commandExecutor;

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

        if (webSocketServer != null) {
            try {
                webSocketServer.stop();
                LOGGER.info("WebSocket server stopped");
            } catch (Exception e) {
                LOGGER.error("Error stopping WebSocket server", e);
            }
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
                        if (commandExecutor != null) {
                            commandExecutor.setEntitySpawningAllowed(true);
                            context.getSource().sendSuccess(
                                () -> Component.literal("エンティティ召喚を許可しました / Entity spawning enabled"),
                                true
                            );
                        }
                        return 1;
                    }))
                .then(Commands.literal("deny")
                    .executes(context -> {
                        if (commandExecutor != null) {
                            commandExecutor.setEntitySpawningAllowed(false);
                            context.getSource().sendSuccess(
                                () -> Component.literal("エンティティ召喚を禁止しました / Entity spawning disabled"),
                                true
                            );
                        }
                        return 1;
                    }))
                .then(Commands.literal("status")
                    .executes(context -> {
                        if (commandExecutor != null) {
                            boolean allowed = commandExecutor.isEntitySpawningAllowed();
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
