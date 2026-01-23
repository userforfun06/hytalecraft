package com.proxy.network.util;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * Utility class for encoding Minecraft protocol data types.
 * Used for writing packets to send to clients.
 */
public class PacketEncoder {

    /**
     * Writes a VarInt to the ByteBuf.
     * 
     * @param buf The buffer to write to
     * @param value The integer value to encode
     */
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

    /**
     * Writes a VarLong to the ByteBuf.
     * 
     * @param buf The buffer to write to
     * @param value The long value to encode
     */
    public static void writeVarLong(ByteBuf buf, long value) {
        while (true) {
            if ((value & ~0x7FL) == 0) {
                buf.writeByte((int) value);
                return;
            }
            buf.writeByte((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
    }

    /**
     * Writes a Minecraft protocol string to the ByteBuf.
     * Format: VarInt length + UTF-8 bytes
     * 
     * @param buf The buffer to write to
     * @param str The string to encode
     */
    public static void writeString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    /**
     * Writes a UUID to the ByteBuf.
     * Format: Two longs (most significant, then least significant)
     * 
     * @param buf The buffer to write to
     * @param uuid The UUID to encode
     */
    public static void writeUUID(ByteBuf buf, java.util.UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }
}
