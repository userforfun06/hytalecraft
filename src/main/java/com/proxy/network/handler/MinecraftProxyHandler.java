package com.proxy.network.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;

public class MinecraftProxyHandler extends ChannelInboundHandlerAdapter {

    private Channel backendChannel;
    private boolean isConnecting = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // If the bridge is already alive, just send the data!
        if (backendChannel != null && backendChannel.isActive()) {
            backendChannel.writeAndFlush(msg);
            return;
        }

        // If we are not connected yet, start the connection ONCE
        if (!isConnecting) {
            isConnecting = true;
            final Channel clientChannel = ctx.channel();

            Bootstrap b = new Bootstrap();
            b.group(clientChannel.eventLoop())
             .channel(NioSocketChannel.class)
             .handler(new ChannelInboundHandlerAdapter() {
                 @Override
                 public void channelRead(ChannelHandlerContext ctx, Object msg) {
                     // Data coming from Paper -> Client
                     clientChannel.writeAndFlush(msg);
                 }

                 @Override
                 public void channelInactive(ChannelHandlerContext ctx) {
                     clientChannel.close();
                 }
             });

            b.connect("127.0.0.1", 25566).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    backendChannel = future.channel();
                    System.out.println(">>> [SINGLE BRIDGE] Connected to Paper on 25566!");
                    // Send the very first packet that triggered this
                    backendChannel.writeAndFlush(msg);
                } else {
                    System.out.println(">>> [ERROR] Paper Server is Offline!");
                    clientChannel.close();
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (backendChannel != null) {
            backendChannel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Silently close on errors to prevent log spam
        ctx.close();
    }
}