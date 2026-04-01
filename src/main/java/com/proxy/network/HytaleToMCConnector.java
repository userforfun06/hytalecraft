package com.proxy.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * HytaleToMCConnector — opens a raw TCP connection to the Paper backend
 * (127.0.0.1:25566) on behalf of a Hytale player and performs the Minecraft
 * offline-mode login handshake.
 *
 * MC Protocol version targeted: 1.20.x (protocol 765)
 *
 * Framing helpers:
 *   MC packets are length-prefixed with a variable-length (VarInt) before each packet.
 *   Each packet also begins with a VarInt packet ID.
 *
 * Handshake sequence (offline mode, no encryption):
 *   C→S  0x00 Handshake  (state=2 LOGIN, protocol=765, addr, port)
 *   C→S  0x00 Login Start (name, uuid)
 *   S→C  0x02 Login Success → we're IN_GAME
 *
 * After login, all Hytale→MC data forwarding happens transparently; MC→Hytale
 * forwarding routes back through the session's hytaleChannel.
 */
public class HytaleToMCConnector {

    private static final Logger log = LoggerFactory.getLogger(HytaleToMCConnector.class);

    private static final String MC_HOST    = "127.0.0.1";
    private static final int    MC_PORT    = 25566;
    // MC protocol version 765 = 1.20.4
    private static final int    MC_PROTO   = 765;

    private final HytalePlayerSession session;
    private static final NioEventLoopGroup MC_GROUP = new NioEventLoopGroup(4);

    public HytaleToMCConnector(HytalePlayerSession session) {
        this.session = session;
    }

    /**
     * Asynchronously connect to the Paper backend.
     * On success, sets session.mcChannel and transitions state to IN_GAME.
     * On failure, closes the Hytale channel.
     */
    public void connect() {
        Bootstrap b = new Bootstrap();
        b.group(MC_GROUP)
         .channel(NioSocketChannel.class)
         .option(ChannelOption.TCP_NODELAY, true)
         .handler(new ChannelInitializer<NioSocketChannel>() {
             @Override
             protected void initChannel(NioSocketChannel ch) {
                 ch.pipeline().addLast(new MCResponseHandler(session));
             }
         });

        b.connect(MC_HOST, MC_PORT).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                Channel mc = future.channel();
                session.setMcChannel(mc);
                log.info("[HytaleToMC] Connected to Paper for {}", session.getUsername());
                sendHandshakeAndLogin(mc);
            } else {
                log.warn("[HytaleToMC] Failed to connect to Paper for {}: {}",
                        session.getUsername(), future.cause().getMessage());
                session.close();
            }
        });
    }

    // -------------------------------------------------------------------------
    // MC Protocol helpers
    // -------------------------------------------------------------------------

    private void sendHandshakeAndLogin(Channel mc) {
        // --- Handshake packet (0x00, state = LOGIN) ---
        ByteBuf handshake = Unpooled.buffer();
        writeVarInt(handshake, 0x00);                        // packet id
        writeVarInt(handshake, MC_PROTO);                    // protocol version
        writeString(handshake, MC_HOST);                     // server address
        handshake.writeShort(MC_PORT);                       // port
        writeVarInt(handshake, 2);                           // next state = LOGIN
        mc.writeAndFlush(framedPacket(handshake));

        // --- Login Start packet (0x00) ---
        ByteBuf loginStart = Unpooled.buffer();
        writeVarInt(loginStart, 0x00);                       // packet id
        writeString(loginStart, session.getUsername());      // player name
        // UUID (has UUID = true, then 16 bytes)
        loginStart.writeBoolean(true);
        long msb = session.getUuid().getMostSignificantBits();
        long lsb = session.getUuid().getLeastSignificantBits();
        loginStart.writeLong(msb);
        loginStart.writeLong(lsb);
        mc.writeAndFlush(framedPacket(loginStart));

        log.info("[HytaleToMC] Sent handshake + login start for {}", session.getUsername());
    }

    /** Wrap a packet body with its VarInt length prefix. */
    private static ByteBuf framedPacket(ByteBuf body) {
        ByteBuf frame = Unpooled.buffer();
        writeVarInt(frame, body.readableBytes());
        frame.writeBytes(body);
        body.release();
        return frame;
    }

    // -------------------------------------------------------------------------
    // VarInt / String helpers (MC protocol encoding)
    // -------------------------------------------------------------------------

    public static void writeVarInt(ByteBuf buf, int value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                buf.writeByte(value);
                return;
            }
            buf.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    public static void writeString(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    // -------------------------------------------------------------------------
    // MC response handler — relays Paper→Hytale data
    // -------------------------------------------------------------------------

    /**
     * Receives raw bytes from the Paper backend.
     * For now we do simple raw relay + watch for Login Success (0x02) to
     * transition the session state.
     */
    private static class MCResponseHandler extends ChannelInboundHandlerAdapter {

        private final HytalePlayerSession session;
        private boolean loginComplete = false;

        MCResponseHandler(HytalePlayerSession session) {
            this.session = session;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof ByteBuf buf) {
                try {
                    if (!loginComplete) {
                        // Peek for Login Success (packet 0x02) or Set Compression (0x03)
                        buf.markReaderIndex();
                        int len = readVarInt(buf);
                        if (buf.readableBytes() >= len) {
                            int packetId = readVarInt(buf);
                            if (packetId == 0x02) {
                                loginComplete = true;
                                session.setState(HytalePlayerSession.PlayerState.IN_GAME);
                                log.info("[HytaleToMC] Login success for {}", session.getUsername());
                            }
                        }
                        buf.resetReaderIndex();
                    }

                    // Forward raw bytes toward Hytale client
                    // (In a full implementation this would be translated to Hytale format)
                    Channel hytaleChannel = session.getHytaleChannel();
                    if (hytaleChannel != null && hytaleChannel.isActive()) {
                        // For now: log the fact that MC data arrived.
                        // A full translator would convert MC packets → Hytale JSON here.
                        log.debug("[HytaleToMC] MC→Hytale: {} bytes for {}", buf.readableBytes(), session.getUsername());
                    }
                } finally {
                    buf.release();
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("[HytaleToMC] MC channel closed for {}", session.getUsername());
            session.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.debug("[HytaleToMC] MC channel error for {}: {}", session.getUsername(), cause.getMessage());
            ctx.close();
        }

        private static int readVarInt(ByteBuf buf) {
            int value = 0, shift = 0, b;
            do {
                if (!buf.isReadable()) return value;
                b = buf.readByte() & 0xFF;
                value |= (b & 0x7F) << shift;
                shift += 7;
            } while ((b & 0x80) != 0 && shift < 35);
            return value;
        }
    }
}
