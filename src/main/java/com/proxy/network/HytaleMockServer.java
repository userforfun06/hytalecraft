package com.proxy.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * HytaleCraft Mock Server — listens on TCP port 5521 for Hytale client connections.
 *
 * Responsibilities:
 *  - Boot a Netty TCP ServerBootstrap on port 5521
 *  - Maintain the live session map (channel → HytalePlayerSession)
 *  - Clean up sessions when a channel closes (handled by HytaleSessionHandler)
 *  - Provide a clean shutdown path (called from CodeNinjaBridge shutdown hook)
 *
 * Thread safety:
 *  - sessions map is ConcurrentHashMap — safe for Netty's multi-threaded I/O
 *  - Server lifecycle methods (start/stop) must be called from a single controlling thread
 */
public class HytaleMockServer {

    private static final Logger log = LoggerFactory.getLogger(HytaleMockServer.class);

    public static final int HYTALE_TCP_PORT = 5521;

    private final ConcurrentHashMap<Channel, HytalePlayerSession> sessions = new ConcurrentHashMap<>();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Start the mock server.  Blocks briefly to bind the port, then returns.
     * The Netty event loops continue running in background daemon threads.
     *
     * @throws Exception if the port cannot be bound
     */
    public void start() throws Exception {
        bossGroup   = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap sb = new ServerBootstrap();
        sb.group(bossGroup, workerGroup)
          .channel(NioServerSocketChannel.class)
          .childHandler(new HytaleChannelPipeline(sessions));

        ChannelFuture future = sb.bind(HYTALE_TCP_PORT).sync();
        serverChannel = future.channel();

        log.info("========================================");
        log.info("  HytaleCraft Mock Server ONLINE");
        log.info("  TCP port {} ready for Hytale clients", HYTALE_TCP_PORT);
        log.info("  Protocol: HytaleCraft v1 (JSON/length-framed)");
        log.info("========================================");
    }

    /**
     * Shut down the mock server gracefully.
     * Closes all active sessions first, then tears down the Netty event loops.
     */
    public void stop() {
        log.info("[HytaleMockServer] Shutting down...");

        // Close all active sessions
        sessions.values().forEach(session -> {
            try {
                session.close();
            } catch (Exception e) {
                log.debug("[HytaleMockServer] Error closing session {}: {}", session, e.getMessage());
            }
        });
        sessions.clear();

        // Stop the server channel
        if (serverChannel != null) {
            try {
                serverChannel.close().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Shut down event loop groups
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup   != null) bossGroup.shutdownGracefully();

        log.info("[HytaleMockServer] Stopped.");
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Returns a snapshot view of active sessions (for diagnostics/commands). */
    public ConcurrentHashMap<Channel, HytalePlayerSession> getSessions() {
        return sessions;
    }

    public int getSessionCount() {
        return sessions.size();
    }
}
