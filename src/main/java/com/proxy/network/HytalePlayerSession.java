package com.proxy.network;

import com.proxy.protocol.BlockRegistry;
import io.netty.channel.Channel;

import java.util.UUID;

/**
 * Represents a connected Hytale player session.
 *
 * One session exists per Hytale client connection.  It holds:
 *  - The Hytale-side TCP channel (talk to the Hytale client)
 *  - The MC-side TCP channel (talk to the Paper backend, via HytaleToMCConnector)
 *  - Player identity (username + UUID derived from name for offline-mode compat)
 *  - Current lifecycle state
 *  - Reference to the shared BlockRegistry for ID translation
 */
public class HytalePlayerSession {

    // -------------------------------------------------------------------------
    // PlayerState enum
    // -------------------------------------------------------------------------

    public enum PlayerState {
        CONNECTING,
        AUTHENTICATED,
        IN_GAME,
        DISCONNECTING
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    /** TCP channel to the Hytale client */
    private final Channel hytaleChannel;

    /** TCP channel to the Paper MC backend (set after successful MC handshake) */
    private volatile Channel mcChannel;

    private final String username;
    private final UUID   uuid;

    private volatile PlayerState state;

    private final BlockRegistry registry;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public HytalePlayerSession(Channel hytaleChannel, String username) {
        this.hytaleChannel = hytaleChannel;
        this.username = username;
        this.uuid     = deriveOfflineUUID(username);
        this.state    = PlayerState.CONNECTING;
        this.registry = BlockRegistry.getInstance();
    }

    // -------------------------------------------------------------------------
    // UUID derivation (offline-mode compatible — same algorithm Bukkit uses)
    // -------------------------------------------------------------------------

    /**
     * Derive a UUID from "OfflinePlayer:<username>" — identical to how
     * offline-mode Bukkit/Paper assigns UUIDs, ensuring a Hytale player always
     * gets the same identity even without online-mode auth.
     */
    public static UUID deriveOfflineUUID(String username) {
        return UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Channel getHytaleChannel() { return hytaleChannel; }

    public Channel getMcChannel()     { return mcChannel; }
    public void    setMcChannel(Channel ch) { this.mcChannel = ch; }

    public String  getUsername()      { return username; }
    public UUID    getUuid()          { return uuid; }

    public PlayerState getState()     { return state; }
    public void        setState(PlayerState s) { this.state = s; }

    public BlockRegistry getRegistry() { return registry; }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Cleanly close both sides of the bridge. */
    public void close() {
        state = PlayerState.DISCONNECTING;
        if (mcChannel != null && mcChannel.isActive()) {
            mcChannel.close();
        }
        if (hytaleChannel != null && hytaleChannel.isActive()) {
            hytaleChannel.close();
        }
    }

    @Override
    public String toString() {
        return "HytalePlayerSession{" + username + "," + uuid + "," + state + "}";
    }
}
