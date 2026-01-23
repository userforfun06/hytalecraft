package com.proxy.network;

import com.proxy.network.decoder.PacketFrameDecoder;
import com.proxy.network.decoder.MinecraftPacketDecoder;
import com.proxy.network.handler.MinecraftProxyHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * Channel initializer for Minecraft TCP connections.
 * Sets up the pipeline: FrameDecoder -> PacketDecoder -> Handler
 */
public class MinecraftChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        // First, decode the variable-length packet frames
        pipeline.addLast("frameDecoder", new PacketFrameDecoder());
        
        // Then, decode the Minecraft protocol packets
        pipeline.addLast("packetDecoder", new MinecraftPacketDecoder());
        
        // Finally, handle the decoded packets
        pipeline.addLast("handler", new MinecraftProxyHandler());
    }
}
