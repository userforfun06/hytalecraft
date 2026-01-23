package com.proxy.network.packet.handshake;

import com.proxy.network.packet.MinecraftPacket;

/**
 * Represents a Minecraft Handshake packet (0x00).
 * 
 * This is the first packet sent by a Minecraft client when connecting.
 * It contains the protocol version, server address, port, and next state.
 */
public class HandshakePacket extends MinecraftPacket {

    private final int protocolVersion;
    private final String serverAddress;
    private final int serverPort;
    private final int nextState;

    public HandshakePacket(int protocolVersion, String serverAddress, int serverPort, int nextState) {
        this.protocolVersion = protocolVersion;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.nextState = nextState;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getNextState() {
        return nextState;
    }

    @Override
    public String toString() {
        return String.format("HandshakePacket{protocol=%d, address='%s', port=%d, nextState=%d}",
                protocolVersion, serverAddress, serverPort, nextState);
    }
}
