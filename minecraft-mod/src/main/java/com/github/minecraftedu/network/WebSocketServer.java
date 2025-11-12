package com.github.minecraftedu.network;

import com.github.minecraftedu.MinecraftEduMod;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import net.minecraft.server.MinecraftServer;

public class WebSocketServer {

    private final int port;
    private final MinecraftServer minecraftServer;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public WebSocketServer(int port, MinecraftServer minecraftServer) {
        this.port = port;
        this.minecraftServer = minecraftServer;
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // HTTP codec
                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new ChunkedWriteHandler());

                            // WebSocket
                            pipeline.addLast(new WebSocketServerProtocolHandler("/minecraft", null, true));

                            // Custom handler
                            pipeline.addLast(new MinecraftWebSocketHandler(minecraftServer));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Bind and start
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            MinecraftEduMod.LOGGER.info("WebSocket server started on port " + port);

        } catch (Exception e) {
            MinecraftEduMod.LOGGER.error("Failed to start WebSocket server", e);
            shutdown();
            throw e;
        }
    }

    public void stop() throws InterruptedException {
        MinecraftEduMod.LOGGER.info("Stopping WebSocket server...");
        shutdown();
    }

    private void shutdown() {
        if (serverChannel != null) {
            serverChannel.close().awaitUninterruptibly();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
