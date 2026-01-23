package com.proxy.network.packet.play;

import com.proxy.network.packet.MinecraftPacket;

/**
 * Represents a Player Position packet (0x18 in PLAY state for 1.20.4).
 * 
 * This packet is sent by the client to update the server about the player's position.
 */
public class PlayerPositionPacket extends MinecraftPacket {

    private final double x;
    private final double y;
    private final double z;
    private final boolean onGround;

    public PlayerPositionPacket(double x, double y, double z, boolean onGround) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.onGround = onGround;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public boolean isOnGround() {
        return onGround;
    }
}
