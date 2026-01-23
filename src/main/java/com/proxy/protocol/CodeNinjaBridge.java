package com.proxy.network;

import com.google.inject.Inject;
import com.proxy.network.handler.HytaleHandler;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
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
        authors = {"CodeNinja"} // Name changed from BLACK_PROTIK to CodeNinja
)
public class CodeNinjaBridge {

    private final Logger logger;
    private final int hytalePort = 5520;

    @Inject
    public CodeNinjaBridge(Logger logger) {
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("==========================================");
        logger.info("   CodeNinja Bridge is Initializing...   ");
        logger.info("==========================================");

        // Start the Hytale UDP Listener in a new thread
        new Thread(() -> {
            try {
                startHytaleListener();
            } catch (Exception e) {
                logger.error("CodeNinja: Failed to start UDP Listener!", e);
            }
        }).start();
    }

    private void startHytaleListener() throws Exception {
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

            logger.info(">>> [CodeNinja UDP] Hytale Listener active on: " + hytalePort);
            b.bind(hytalePort).sync().channel().closeFuture().await();
        } finally {
            group.shutdownGracefully();
        }
    }
}