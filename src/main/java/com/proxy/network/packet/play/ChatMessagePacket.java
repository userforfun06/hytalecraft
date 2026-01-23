package com.proxy.network.packet.play;

import com.proxy.network.packet.MinecraftPacket;

/**
 * Represents a Chat Message packet (0x05 in PLAY state for 1.20.4).
 * 
 * This packet is sent by the client when a player types a message in chat.
 */
public class ChatMessagePacket extends MinecraftPacket {

    private final String message;
    private final long timestamp;
    private final long salt;
    private final byte[] signature; // May be null for unsigned messages
    private final boolean signedPreview;

    public ChatMessagePacket(String message, long timestamp, long salt, byte[] signature, boolean signedPreview) {
        this.message = message;
        this.timestamp = timestamp;
        this.salt = salt;
        this.signature = signature;
        this.signedPreview = signedPreview;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getSalt() {
        return salt;
    }

    public byte[] getSignature() {
        return signature;
    }

    public boolean isSignedPreview() {
        return signedPreview;
    }

    @Override
    public String toString() {
        return String.format("ChatMessagePacket{message='%s'}", message);
    }
}
