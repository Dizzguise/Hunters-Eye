package com.enochmods.eyevisuals;

import com.enochmods.eyevisuals.protocol.OwnerFullPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public final class EyeVisualsClient implements ClientModInitializer {
    private static final OwnerVisualState STATE = new OwnerVisualState();

    @Override public void onInitializeClient() {
        // The core's main initializer owns registration when both jars are present.
        if (!FabricLoader.getInstance().isModLoaded("hunterseye")) {
            PayloadTypeRegistry.clientboundPlay().register(OwnerFullPayload.TYPE, OwnerFullPayload.CODEC);
        }
        ClientPlayNetworking.registerGlobalReceiver(OwnerFullPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            STATE.receive(payload.full(), client.getConnection(), client.level, client.player, System.nanoTime());
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> STATE.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> STATE.clear());
        ClientTickEvents.END_CLIENT_TICK.register(client -> isFull());
    }

    /** True only while this local player's Full assertion remains fresh. */
    public static boolean isFull() {
        Minecraft client = Minecraft.getInstance();
        return STATE.isFull(client.getConnection(), client.level, client.player, System.nanoTime());
    }

    public static boolean hides(Entity entity) {
        return entity != null && entity == Minecraft.getInstance().player && isFull();
    }
}
