package com.proxy.network.decoder;

import com.proxy.network.packet.MinecraftPacket;
import com.proxy.network.packet.handshake.HandshakePacket;
import com.proxy.network.packet.play.ChatMessagePacket;
import com.proxy.network.packet.play.PlayerPositionPacket;
import com.proxy.network.packet.play.PlayerRotationPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;
import java.util.UUID;

public class MinecraftPacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    public enum ConnectionState {
        HANDSHAKING,
        STATUS,
        LOGIN,
        PLAY
    }

    private ConnectionState state = ConnectionState.HANDSHAKING;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (msg.readableBytes() <= 0) return;

        // 1. Read Packet ID
        int packetId = readVarInt(msg);
        
        // 2. Log basic info for debugging
        System.out.println(">>> [DEBUG] Packet ID: 0x" + Integer.toHexString(packetId) + " | State: " + state);

        // 3. Decode based on current state
        MinecraftPacket packet = decodePacket(packetId, msg, state);
        
        if (packet != null) {
            out.add(packet);
        }
    }

    private MinecraftPacket decodePacket(int packetId, ByteBuf buf, ConnectionState state) {
        switch (state) {
            case HANDSHAKING:
                return decodeHandshakePacket(packetId, buf);
                
            case STATUS:
                if (packetId == 0x00) {
                    System.out.println(">>> [STATUS] Minecraft 1.21.11 is pinging for server info...");
                }
                return null;

            case LOGIN:
                if (packetId == 0x00) { // Login Start
                    String username = readString(buf);
                    
                    // In 1.21.11, a UUID follows the username
                    if (buf.readableBytes() >= 16) {
                        long mostSig = buf.readLong();
                        long leastSig = buf.readLong();
                        UUID playerUuid = new UUID(mostSig, leastSig);
                        System.out.println(">>> [LOGIN] Player: " + username + " (UUID: " + playerUuid + ") is joining!");
                    } else {
                        System.out.println(">>> [LOGIN] Player: " + username + " is joining!");
                    }
                }
                return null;

            case PLAY:
                return decodePlayPacket(packetId, buf);

            default:
                return null;
        }
    }

    private MinecraftPacket decodeHandshakePacket(int packetId, ByteBuf buf) {
        if (packetId == 0x00) {
            int protocolVersion = readVarInt(buf);
            String serverAddress = readString(buf);
            int serverPort = buf.readUnsignedShort();
            int nextStateInt = readVarInt(buf);

            System.out.println(">>> [HANDSHAKE] Protocol: " + protocolVersion + " | Address: " + serverAddress);

            // Update state for the next packets
            if (nextStateInt == 1) state = ConnectionState.STATUS;
            if (nextStateInt == 2) state = ConnectionState.LOGIN;

            return new HandshakePacket(protocolVersion, serverAddress, serverPort, nextStateInt);
        }
        return null;
    }

    private int readVarInt(ByteBuf buf) {
        int value = 0;
        int position = 0;
        byte currentByte;
        while (true) {
            if (buf.readableBytes() < 1) return -1;
            currentByte = buf.readByte();
            value |= (currentByte & 0x7F) << position;
            if ((currentByte & 0x80) == 0) break;
            position += 7;
            if (position >= 32) throw new RuntimeException("VarInt too long");
        }
        return value;
    }

    private String readString(ByteBuf buf) {
        int length = readVarInt(buf);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    private MinecraftPacket decodePlayPacket(int packetId, ByteBuf buf) {
        // Simple handlers for Play state
        if (packetId == 0x05) {
            return new ChatMessagePacket(readString(buf), buf.readLong(), buf.readLong(), null, buf.readBoolean());
        }
        return null;
    }

    public void setState(ConnectionState newState) { this.state = newState; }
    public ConnectionState getState() { return state; }
}