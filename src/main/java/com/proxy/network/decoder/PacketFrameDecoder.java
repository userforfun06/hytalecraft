package com.proxy.network.decoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Decodes Minecraft protocol variable-length packet frames.
 * 
 * Minecraft uses a VarInt length prefix before each packet.
 * This decoder reads the VarInt length and extracts the complete packet frame.
 */
public class PacketFrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Check if we have enough bytes to read the VarInt length
        if (in.readableBytes() < 1) {
            return; // Not enough data yet
        }

        // Read VarInt length (Minecraft protocol format)
        int readerIndex = in.readerIndex();
        int length = readVarInt(in);
        
        if (length == -1) {
            // VarInt was incomplete, reset reader index
            in.readerIndex(readerIndex);
            return;
        }

        // Check if we have the complete packet
        if (in.readableBytes() < length) {
            // Not enough data for the complete packet, reset to before VarInt
            in.readerIndex(readerIndex);
            return;
        }

        // Extract the complete packet frame
        ByteBuf frame = in.readBytes(length);
        out.add(frame);
    }

    /**
     * Reads a VarInt from the ByteBuf.
     * Returns -1 if the VarInt is incomplete.
     * 
     * @param buf The ByteBuf to read from
     * @return The VarInt value, or -1 if incomplete
     */
    private int readVarInt(ByteBuf buf) {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            if (buf.readableBytes() < 1) {
                return -1; // Incomplete VarInt
            }

            currentByte = buf.readByte();
            value |= (currentByte & 0x7F) << position;

            if ((currentByte & 0x80) == 0) {
                break; // Last byte
            }

            position += 7;

            if (position >= 32) {
                throw new RuntimeException("VarInt too long");
            }
        }

        return value;
    }
}
