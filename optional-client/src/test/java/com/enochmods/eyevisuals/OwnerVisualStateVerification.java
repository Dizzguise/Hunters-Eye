package com.enochmods.eyevisuals;

public final class OwnerVisualStateVerification {
    public static void main(String[] args) {
        require(com.enochmods.eyevisuals.protocol.OwnerFullPayload.TYPE.id().toString().equals("hunterseye:owner_full_v1"),
                "payload initializes with the correct private namespace");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        try {
            for (boolean full : new boolean[]{false, true}) {
                buffer.clear();
                com.enochmods.eyevisuals.protocol.OwnerFullPayload.CODEC.encode(buffer,
                        new com.enochmods.eyevisuals.protocol.OwnerFullPayload(full));
                require(buffer.readableBytes() == 1, "protocol carries only one boolean byte");
                require(com.enochmods.eyevisuals.protocol.OwnerFullPayload.CODEC.decode(buffer).full() == full,
                        "owner assertion codec round trip");
            }
        } finally { buffer.release(); }
        OwnerVisualState state = new OwnerVisualState();
        Object connection = new Object(), world = new Object(), player = new Object();
        require(!state.isFull(connection, world, player, 100), "unsupported server defaults visible");
        state.receive(true, connection, world, player, 100);
        require(state.isFull(connection, world, player, 100 + OwnerVisualState.TIMEOUT_NANOS - 1), "fresh assertion hides");
        require(!state.isFull(connection, world, player, 100 + OwnerVisualState.TIMEOUT_NANOS), "timeout restores visibility");
        state.receive(true, connection, world, player, 200);
        require(!state.isFull(new Object(), world, player, 201), "reconnect clears");
        state.receive(true, connection, world, player, 200);
        require(!state.isFull(connection, new Object(), player, 201), "world transition clears");
        state.receive(true, connection, world, player, 200);
        require(!state.isFull(connection, world, new Object(), 201), "respawn clears");
        state.receive(true, connection, world, player, 200);
        state.receive(false, connection, world, player, 201);
        require(!state.isFull(connection, world, player, 202), "server disable restores immediately");
        state.receive(true, connection, world, player, 300);
        state.receive(true, connection, world, player, 300 + OwnerVisualState.TIMEOUT_NANOS - 1);
        require(state.isFull(connection, world, player, 300 + OwnerVisualState.TIMEOUT_NANOS), "heartbeat extends freshness");
        state.clear();
        require(!state.isFull(connection, world, player, 400), "disconnect clears");
        state.receive(true, connection, null, player, 500);
        require(!state.isFull(connection, world, player, 501), "packet while world missing ignored");
        System.out.println("Owner visual freshness verification passed.");
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
