package com.proxy.protocol;

import com.google.inject.Inject;
import com.proxy.network.HytaleMockServer;
import com.proxy.network.handler.HytaleHandler;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;

@Plugin(
        id = "codeninjabridge",
        name = "CodeNinja Hytale Bridge",
        version = "1.0",
        authors = {"CodeNinja"}
)
public class CodeNinjaBridge {

    private final Logger logger;

    /** UDP legacy port — kept for heartbeat/presence pings */
    private final int hytaleUdpPort = 5520;

    /** The new TCP mock server — primary session bridge */
    private HytaleMockServer mockServer;

    @Inject
    public CodeNinjaBridge(Logger logger) {
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // Proxy Initialization
    // -------------------------------------------------------------------------

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("==========================================");
        logger.info("   CodeNinja Bridge is Initializing...   ");
        logger.info("==========================================");

        // 1. Start legacy UDP heartbeat listener (port 5520)
        new Thread(() -> {
            try {
                startHytaleUdpListener();
            } catch (Exception e) {
                logger.error("CodeNinja: Failed to start UDP Listener!", e);
            }
        }, "hytale-udp").start();

        // 2. Start HytaleCraft Mock Server (TCP port 5521)
        new Thread(() -> {
            try {
                startHytaleMockServer();
            } catch (Exception e) {
                logger.error("CodeNinja: Failed to start HytaleCraft Mock Server!", e);
            }
        }, "hytale-mock").start();

        // 3. Pre-warm the block registry so it logs on startup
        int mappings = BlockRegistry.getInstance().size();
        logger.info("CodeNinja: BlockRegistry loaded with {} block mappings.", mappings);
    }

    // -------------------------------------------------------------------------
    // Proxy Shutdown
    // -------------------------------------------------------------------------

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("CodeNinja: Shutting down HytaleCraft Mock Server...");
        if (mockServer != null) {
            mockServer.stop();
        }
        logger.info("CodeNinja: Shutdown complete.");
    }

    // -------------------------------------------------------------------------
    // Internal startup helpers
    // -------------------------------------------------------------------------

    private void startHytaleMockServer() throws Exception {
        mockServer = new HytaleMockServer();
        mockServer.start();
        logger.info(">>> [CodeNinja TCP] HytaleCraft Mock Server active on port {}",
                HytaleMockServer.HYTALE_TCP_PORT);
    }

    private void startHytaleUdpListener() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioDatagramChannel.class)
             .handler(new ChannelInitializer<NioDatagramChannel>() {
                 @Override
                 protected void initChannel(NioDatagramChannel ch) {
                     ch.pipeline().addLast(new HytaleHandler());
                 }
             });

            logger.info(">>> [CodeNinja UDP] Hytale heartbeat listener active on port {}", hytaleUdpPort);
            b.bind(hytaleUdpPort).sync().channel().closeFuture().await();
        } finally {
            group.shutdownGracefully();
        }
    }
}
