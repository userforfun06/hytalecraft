package com.proxy.protocol;

/**
 * HytaleCraft Protocol v1 — Marker interface for all Hytale protocol packets.
 *
 * Protocol framing:
 *   [4-byte big-endian int: payload length][payload bytes: UTF-8 JSON]
 *
 * Every JSON payload must include a "type" field identifying the packet type.
 *
 * Packet types:
 *   LOGIN, LOGIN_ACK, PLAYER_POSITION, BLOCK_QUERY, BLOCK_RESPONSE,
 *   CHAT_MESSAGE, DISCONNECT
 */
public interface HytalePacket {

    /** Return the packet type string, e.g. "LOGIN" */
    String getType();
}
