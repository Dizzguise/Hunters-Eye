package com.enochmods.hunterseye;

import com.enochmods.eyevisuals.protocol.OwnerFullPayload;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Sends only an optional private owner boolean; vanilla clients keep the same server behavior. */
public final class HunterEyeVisualSync {
    private static final Map<UUID, Boolean> lastFull = new HashMap<>();
    private static int ticks;
    private HunterEyeVisualSync() {}

    /** Register after HunterEyePower's tick callback so every assertion uses the validated mode. */
    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(OwnerFullPayload.TYPE, OwnerFullPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> clear());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> lastFull.remove(handler.player.getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(HunterEyeVisualSync::tick);
    }

    private static void clear() { lastFull.clear(); ticks = 0; }

    private static void tick(MinecraftServer server) {
        ticks++;
        for (ServerPlayer owner : server.getPlayerList().getPlayers()) {
            UUID id = owner.getUUID();
            if (!ServerPlayNetworking.canSend(owner, OwnerFullPayload.TYPE)) {
                lastFull.remove(id);
                continue;
            }
            boolean full = HunterEyePower.getMode(id) == ConcealmentMode.FULL;
            Boolean previous = lastFull.get(id);
            if (previous == null || previous != full || ticks % 4 == 0) {
                ServerPlayNetworking.send(owner, new OwnerFullPayload(full));
                lastFull.put(id, full);
            }
        }
    }
}
