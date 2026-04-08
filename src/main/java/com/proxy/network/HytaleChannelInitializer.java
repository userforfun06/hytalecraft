package com.proxy.network;

import com.proxy.network.handler.HytaleHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.DatagramChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Channel initializer for Hytale UDP/QUIC connections.
 * 
 * Note: For UDP connections, you should use Netty's Bootstrap with NioDatagramChannel.
 * Example server setup:
 * 
 * Bootstrap bootstrap = new Bootstrap();
 * bootstrap.group(new NioEventLoopGroup())
 *          .channel(NioDatagramChannel.class)
 *          .handler(new HytaleChannelInitializer());
 * 
 * ChannelFuture future = bootstrap.bind(new InetSocketAddress(port)).sync();
 * 
 * Unlike TCP (SocketChannel), UDP uses DatagramChannel which is connectionless.
 * Each datagram packet is independent, so the pipeline should handle packet decoding
 * and routing based on packet headers rather than connection state.
 * 
 * For QUIC support, you may need additional libraries like:
 * - netty-incubator-codec-quic (for native QUIC support)
 * - Or handle QUIC at a higher level
 */
public class HytaleChannelInitializer extends ChannelInitializer<DatagramChannel> {

    @Override
    protected void initChannel(DatagramChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        // HytaleHandler processes raw DatagramPackets directly (UDP is message-based).
        // We add a HytaleHandler that reads the datagram payload and responds.
        pipeline.addLast("hytaleHandler", new HytaleHandler());
    }
}
