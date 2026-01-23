package com.proxy.network.packet.play;

import com.proxy.network.packet.MinecraftPacket;

/**
 * Represents a Player Rotation packet (0x1A in PLAY state for 1.20.4).
 * 
 * This packet is sent by the client to update the server about the player's rotation.
 */
public class PlayerRotationPacket extends MinecraftPacket {

    private final float yaw;
    private final float pitch;
    private final boolean onGround;

    public PlayerRotationPacket(float yaw, float pitch, boolean onGround) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isOnGround() {
        return onGround;
    }
}
