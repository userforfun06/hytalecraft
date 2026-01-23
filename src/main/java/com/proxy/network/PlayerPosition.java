package com.proxy.network;

/**
 * Represents a player's position and rotation in the world.
 */
public class PlayerPosition {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public PlayerPosition(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
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

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    /**
     * Calculates a position in front of the player based on their yaw.
     * 
     * @param distance Distance in front of the player
     * @return New position in front of the player
     */
    public PlayerPosition getPositionInFront(double distance) {
        // Convert yaw to radians (Minecraft yaw: 0 = south, increases counterclockwise)
        double yawRad = Math.toRadians(yaw - 90); // Adjust for Minecraft's coordinate system
        
        double newX = x + Math.cos(yawRad) * distance;
        double newZ = z + Math.sin(yawRad) * distance;
        
        return new PlayerPosition(newX, y, newZ, yaw, pitch);
    }
}
