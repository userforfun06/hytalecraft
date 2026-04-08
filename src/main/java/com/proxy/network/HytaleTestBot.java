package com.proxy.network;

import io.netty.channel.ChannelHandlerContext;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class HytaleTestBot {
    // Neutral name as requested
    private static final String BOT_NAME = "CodeNinja";
    private static final UUID BOT_UUID = UUID.randomUUID();

    private static final String BRIDGE_HOST = "localhost";
    private static final int BRIDGE_PORT = 5520;

    public static void spawnFakePlayer(ChannelHandlerContext ctx, double x, double y, double z) {
        // Log the event neutrally
        System.out.println("Status: Spawning automated agent '" + BOT_NAME + "' at relative coordinates.");

        // Send a minimal UDP packet to the Hytale bridge listener so we can verify
        // the bridge is reachable from the Minecraft side.
        String payload = "FAKE_PLAYER:" + BOT_NAME + ":" + BOT_UUID + ":" + x + ":" + y + ":" + z;
        try {
            byte[] buf = payload.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName(BRIDGE_HOST);
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, BRIDGE_PORT);
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.send(packet);
            }
            System.out.println(">>> [HytaleTestBot] Sent fake-player UDP probe to " + BRIDGE_HOST + ":" + BRIDGE_PORT);
        } catch (Exception e) {
            System.err.println(">>> [HytaleTestBot] Failed to send UDP probe: " + e.getMessage());
        }
    }
}
