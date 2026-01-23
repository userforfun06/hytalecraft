package com.proxy.network;

import com.proxy.network.handler.MinecraftProxyHandler;
import com.proxy.network.handler.HytaleHandler; // Import your new handler
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;

public class ProxyServer {
    private final int mcPort;
    private final int hytalePort = 5520; // Hytale's default UDP port

    public ProxyServer(int mcPort) {
        this.mcPort = mcPort;
    }

    public void start() throws Exception {
        // Boss handles incoming connections, Workers handle the data
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            // 1. MINECRAFT (TCP) SETUP
            // This is the bridge you already built that connects to Paper
            ServerBootstrap mcBootstrap = new ServerBootstrap();
            mcBootstrap.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new MinecraftProxyHandler());
                 }
             });

            // 2. HYTALE (UDP) SETUP
            // This is the new "Hytale lane" using your new HytaleHandler
            Bootstrap hytaleBootstrap = new Bootstrap();
            hytaleBootstrap.group(workerGroup)
             .channel(NioDatagramChannel.class)
             .handler(new ChannelInitializer<NioDatagramChannel>() {
                 @Override
                 public void initChannel(NioDatagramChannel ch) {
                     ch.pipeline().addLast(new HytaleHandler()); 
                 }
             });

            System.out.println("==========================================");
            System.out.println("   CodeNinja Proxy - System Starting      ");
            System.out.println("==========================================");
            System.out.println(">>> [TCP] Minecraft listening on: " + mcPort);
            System.out.println(">>> [UDP] Hytale listening on: " + hytalePort);
            System.out.println("==========================================");

            // Bind both protocols to their ports
            ChannelFuture mcFuture = mcBootstrap.bind(mcPort).sync();
            ChannelFuture hytaleFuture = hytaleBootstrap.bind(hytalePort).sync();

            // Keep the application running
            mcFuture.channel().closeFuture().sync();
        } finally {
            // Shut down gracefully if the proxy is stopped
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        // Running on standard Minecraft port 25565
        new ProxyServer(25565).start();
    }
}