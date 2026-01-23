package com.proxy.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;

public class HytaleHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
        // 1. Log the incoming Hytale signal
        System.out.println(">>> [HYTALE] Connection attempt from: " + packet.sender());

        // 2. Create a "Hytale-style" response
        // In the real game, this would be a complex QUIC packet.
        // For our test, we send a simple String.
        String response = "Hytale_Bridge_Online";
        
        // 3. Send it back to the person who sent the packet
        ctx.writeAndFlush(new DatagramPacket(
                Unpooled.copiedBuffer(response, StandardCharsets.UTF_8),
                packet.sender()
        ));
        
        System.out.println(">>> [HYTALE] Sent 'Online' status back to Hytale player!");
    }
}