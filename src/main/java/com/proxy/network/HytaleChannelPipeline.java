package com.proxy.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HytaleCraft Protocol v1 — TCP channel pipeline for the mock server.
 *
 * Wire format (inbound):
 *   [4-byte big-endian int: payload length] [UTF-8 JSON bytes]
 *
 * Wire format (outbound):
 *   Symmetric — LengthFieldPrepender prepends the 4-byte length automatically.
 *
 * Pipeline:
 *   LengthFieldBasedFrameDecoder → StringDecoder → HytaleSessionHandler
 *                                                → StringEncoder ← LengthFieldPrepender
 *
 * Max frame size: 1 MB (should be more than enough for any HytaleCraft JSON packet)
 */
public class HytaleChannelPipeline extends ChannelInitializer<Channel> {

    private static final int MAX_FRAME_BYTES = 1024 * 1024; // 1 MB

    private final ConcurrentHashMap<Channel, HytalePlayerSession> sessions;

    public HytaleChannelPipeline(ConcurrentHashMap<Channel, HytalePlayerSession> sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline()
          // Inbound: split stream into frames by the 4-byte length prefix
          .addLast("frameDecoder",
                   new LengthFieldBasedFrameDecoder(
                           MAX_FRAME_BYTES, // max frame length
                           0,               // length field offset
                           4,               // length field length (bytes)
                           0,               // length adjustment
                           4))              // strip the 4-byte length prefix

          // Inbound: decode bytes → Java String (UTF-8)
          .addLast("stringDecoder", new StringDecoder(StandardCharsets.UTF_8))

          // Inbound + Outbound: game logic
          .addLast("sessionHandler", new HytaleSessionHandler(sessions))

          // Outbound: prepend 4-byte length before each string frame
          .addLast("lengthPrepender", new LengthFieldPrepender(4))

          // Outbound: encode Java String → bytes (UTF-8)
          .addLast("stringEncoder", new StringEncoder(StandardCharsets.UTF_8));
    }
}
