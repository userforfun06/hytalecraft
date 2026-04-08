package com.proxy.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * HytaleCraft Protocol v1 — per-channel session handler.
 *
 * Receives decoded JSON strings from the pipeline, dispatches on "type" field,
 * and drives the HytalePlayerSession state machine.
 *
 * Packet formats (all JSON, length-prefixed by HytaleChannelPipeline):
 *
 *   LOGIN           { "type":"LOGIN", "username":"Steve", "uuid":"<optional>" }
 *   LOGIN_ACK       { "type":"LOGIN_ACK", "success":true, "message":"..." }
 *   PLAYER_POSITION { "type":"PLAYER_POSITION", "x":0.0,"y":64.0,"z":0.0,"yaw":0.0,"pitch":0.0,"onGround":true }
 *   BLOCK_QUERY     { "type":"BLOCK_QUERY", "x":0, "y":64, "z":0 }
 *   BLOCK_RESPONSE  { "type":"BLOCK_RESPONSE", "x":0,"y":64,"z":0,"hytaleBlockId":1,"mcBlockId":0 }
 *   CHAT_MESSAGE    { "type":"CHAT_MESSAGE", "message":"hello" }
 *   DISCONNECT      { "type":"DISCONNECT", "reason":"bye" }
 */
public class HytaleSessionHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(HytaleSessionHandler.class);

    /** Shared session map owned by HytaleMockServer */
    private final ConcurrentHashMap<Channel, HytalePlayerSession> sessions;

    public HytaleSessionHandler(ConcurrentHashMap<Channel, HytalePlayerSession> sessions) {
        this.sessions = sessions;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("[HytaleSession] New connection from {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        HytalePlayerSession session = sessions.remove(ctx.channel());
        if (session != null) {
            log.info("[HytaleSession] Channel closed — cleaning up session for {}", session.getUsername());
            session.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.debug("[HytaleSession] Error from {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        ctx.close();
    }

    // -------------------------------------------------------------------------
    // Dispatch
    // -------------------------------------------------------------------------

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String json) {
        String type = extractString(json, "type");
        if (type == null) {
            log.warn("[HytaleSession] Received packet with no 'type' field: {}", json);
            return;
        }

        switch (type) {
            case "LOGIN"           -> handleLogin(ctx, json);
            case "PLAYER_POSITION" -> handlePosition(ctx, json);
            case "BLOCK_QUERY"     -> handleBlockQuery(ctx, json);
            case "CHAT_MESSAGE"    -> handleChat(ctx, json);
            case "DISCONNECT"      -> handleDisconnect(ctx, json);
            default -> log.warn("[HytaleSession] Unknown packet type '{}': {}", type, json);
        }
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    private void handleLogin(ChannelHandlerContext ctx, String json) {
        String username = extractString(json, "username");
        if (username == null || username.isBlank()) {
            sendJson(ctx, "{\"type\":\"LOGIN_ACK\",\"success\":false,\"message\":\"Username is required\"}");
            ctx.close();
            return;
        }
        // Sanitise username (MC allows 16 chars, alphanumeric + underscore)
        if (!username.matches("[a-zA-Z0-9_]{1,16}")) {
            sendJson(ctx, "{\"type\":\"LOGIN_ACK\",\"success\":false,\"message\":\"Invalid username\"}");
            ctx.close();
            return;
        }

        HytalePlayerSession session = new HytalePlayerSession(ctx.channel(), username);
        sessions.put(ctx.channel(), session);
        session.setState(HytalePlayerSession.PlayerState.AUTHENTICATED);

        log.info("[HytaleSession] LOGIN from {} (uuid={})", username, session.getUuid());

        // Acknowledge immediately
        sendJson(ctx, String.format(
                "{\"type\":\"LOGIN_ACK\",\"success\":true,\"message\":\"Welcome to HytaleCraft, %s!\",\"uuid\":\"%s\"}",
                username, session.getUuid()));

        // Connect to the Paper backend asynchronously
        new HytaleToMCConnector(session).connect();
    }

    private void handlePosition(ChannelHandlerContext ctx, String json) {
        HytalePlayerSession session = sessions.get(ctx.channel());
        if (session == null) { log.warn("[HytaleSession] PLAYER_POSITION from unauthenticated channel"); return; }

        double x     = extractDouble(json, "x");
        double y     = extractDouble(json, "y");
        double z     = extractDouble(json, "z");
        float  yaw   = (float) extractDouble(json, "yaw");
        float  pitch = (float) extractDouble(json, "pitch");
        boolean onGround = json.contains("\"onGround\":true");

        log.debug("[HytaleSession] POSITION {} → ({},{},{}) yaw={} pitch={} ground={}",
                session.getUsername(), x, y, z, yaw, pitch, onGround);

        // Forward to MC as a raw position update (0x17 Set Player Position And Rotation)
        // This is simplified — a full impl would encode a proper MC packet here.
        Channel mc = session.getMcChannel();
        if (mc != null && mc.isActive()) {
            sendMcPositionPacket(mc, x, y, z, yaw, pitch, onGround);
        }
    }

    private void handleBlockQuery(ChannelHandlerContext ctx, String json) {
        HytalePlayerSession session = sessions.get(ctx.channel());
        if (session == null) { log.warn("[HytaleSession] BLOCK_QUERY from unauthenticated channel"); return; }

        int x = (int) extractDouble(json, "x");
        int y = (int) extractDouble(json, "y");
        int z = (int) extractDouble(json, "z");

        // In a full impl we'd query the MC world state cache here.
        // For now we return a placeholder: grass_block at y≥64, stone below.
        int hytaleBlockId;
        int mcStateId;
        if (y >= 64) {
            hytaleBlockId = 1;  // hytale:grass_block
            mcStateId     = 0;  // minecraft:grass_block
        } else if (y > 0) {
            hytaleBlockId = 3;  // hytale:stone
            mcStateId     = 1;  // minecraft:stone
        } else {
            hytaleBlockId = 9;  // hytale:bedrock
            mcStateId     = 33; // minecraft:bedrock
        }

        log.debug("[HytaleSession] BLOCK_QUERY ({},{},{}) → hytale={} mc={}",
                x, y, z, hytaleBlockId, mcStateId);

        sendJson(ctx, String.format(
                "{\"type\":\"BLOCK_RESPONSE\",\"x\":%d,\"y\":%d,\"z\":%d,\"hytaleBlockId\":%d,\"mcBlockId\":%d}",
                x, y, z, hytaleBlockId, mcStateId));
    }

    private void handleChat(ChannelHandlerContext ctx, String json) {
        HytalePlayerSession session = sessions.get(ctx.channel());
        if (session == null) { log.warn("[HytaleSession] CHAT_MESSAGE from unauthenticated channel"); return; }

        String message = extractString(json, "message");
        log.info("[HytaleSession] CHAT from {}: {}", session.getUsername(), message);

        // Forward to MC backend (Chat Command packet 0x05 / Chat Message 0x06 in 1.20.x)
        Channel mc = session.getMcChannel();
        if (mc != null && mc.isActive() && message != null && !message.isBlank()) {
            sendMcChatPacket(mc, message);
        }
    }

    private void handleDisconnect(ChannelHandlerContext ctx, String json) {
        String reason = extractString(json, "reason");
        HytalePlayerSession session = sessions.get(ctx.channel());
        String name = session != null ? session.getUsername() : "unknown";
        log.info("[HytaleSession] DISCONNECT from {} — reason: {}", name, reason);
        sessions.remove(ctx.channel());
        if (session != null) session.close();
        ctx.close();
    }

    // -------------------------------------------------------------------------
    // MC packet helpers (simplified — encode minimal playable packets)
    // -------------------------------------------------------------------------

    /**
     * Sends a MC "Set Player Position And Rotation" packet (0x17 in 1.20.4 play state).
     * Framing: VarInt(len) | VarInt(0x17) | double x | double y | double z |
     *           float yaw | float pitch | byte flags | VarInt teleportId
     */
    private void sendMcPositionPacket(Channel mc, double x, double y, double z,
                                       float yaw, float pitch, boolean onGround) {
        io.netty.buffer.ByteBuf buf = mc.alloc().buffer(32);
        try {
            io.netty.buffer.ByteBuf body = mc.alloc().buffer(32);
            HytaleToMCConnector.writeVarInt(body, 0x17);
            body.writeDouble(x);
            body.writeDouble(y);
            body.writeDouble(z);
            body.writeFloat(yaw);
            body.writeFloat(pitch);
            body.writeByte(0x00); // flags (absolute)
            HytaleToMCConnector.writeVarInt(body, 1); // teleport ID

            HytaleToMCConnector.writeVarInt(buf, body.readableBytes());
            buf.writeBytes(body);
            body.release();

            mc.writeAndFlush(buf.retain());
        } finally {
            buf.release();
        }
    }

    /**
     * Sends a MC "Chat Message" packet (0x06 in 1.20.4 play state).
     * Framing: VarInt(len) | VarInt(0x06) | String(message) | long timestamp |
     *           long salt | bool has_sig(false)
     */
    private void sendMcChatPacket(Channel mc, String message) {
        io.netty.buffer.ByteBuf buf = mc.alloc().buffer();
        try {
            io.netty.buffer.ByteBuf body = mc.alloc().buffer();
            HytaleToMCConnector.writeVarInt(body, 0x06);
            HytaleToMCConnector.writeString(body, message);
            body.writeLong(System.currentTimeMillis()); // timestamp
            body.writeLong(0L);                         // salt
            body.writeBoolean(false);                   // has_signature = false
            HytaleToMCConnector.writeVarInt(body, 0);  // message count
            // Last seen messages (20 bits / 3 bytes of zeroes — simplified)
            body.writeBytes(new byte[3]);

            HytaleToMCConnector.writeVarInt(buf, body.readableBytes());
            buf.writeBytes(body);
            body.release();

            mc.writeAndFlush(buf.retain());
        } finally {
            buf.release();
        }
    }

    // -------------------------------------------------------------------------
    // JSON helpers (no external library)
    // -------------------------------------------------------------------------

    /** Extract a JSON string field value. Returns null if not found. */
    static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int q1 = json.indexOf('"', colon + 1);
        if (q1 < 0) return null;
        int q2 = json.indexOf('"', q1 + 1);
        if (q2 < 0) return null;
        return json.substring(q1 + 1, q2);
    }

    /** Extract a JSON number field as a double. Returns 0.0 if not found. */
    static double extractDouble(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return 0.0;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return 0.0;
        int i = colon + 1;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == '\t')) i++;
        int numStart = i;
        if (i < json.length() && json.charAt(i) == '-') i++;
        while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '.')) i++;
        if (i == numStart) return 0.0;
        try { return Double.parseDouble(json.substring(numStart, i).trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }

    // -------------------------------------------------------------------------
    // Outbound
    // -------------------------------------------------------------------------

    private void sendJson(ChannelHandlerContext ctx, String json) {
        ctx.writeAndFlush(json);
    }
}
