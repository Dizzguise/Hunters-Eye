package com.enochmods.eyevisuals;

/** A short-lived server assertion, tied to one connection, world and local player. */
final class OwnerVisualState {
    static final long TIMEOUT_NANOS = 2_000_000_000L;
    private boolean full;
    private long receivedAt;
    private Object connection;
    private Object world;
    private Object player;

    void receive(boolean full, Object connection, Object world, Object player, long now) {
        if (connection == null || world == null || player == null) { clear(); return; }
        this.full = full;
        this.connection = connection;
        this.world = world;
        this.player = player;
        this.receivedAt = now;
    }

    boolean isFull(Object connection, Object world, Object player, long now) {
        if (connection == null || world == null || player == null || this.connection != connection
                || this.world != world || this.player != player || now - receivedAt >= TIMEOUT_NANOS) {
            clear();
        }
        return full;
    }

    void clear() { full = false; connection = null; world = null; player = null; receivedAt = 0; }
}
