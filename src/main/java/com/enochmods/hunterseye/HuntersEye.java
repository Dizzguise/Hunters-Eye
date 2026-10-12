package com.enochmods.hunterseye;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HuntersEye implements ModInitializer {
    public static final String MOD_ID="hunterseye";
    public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
    /** Compatibility alias. Mint tagged stacks with EyeData.createEye. */
    public static final Item HUNTERS_EYE=Items.ENDER_EYE;
    @Override public void onInitialize() {
        PhantomCombat.registerInteractions();
        HuntersEyeItem.registerInteractions();
        HunterEyeCommands.register();
        ServerLifecycleEvents.SERVER_STARTING.register(HunterEyePower::start);
        ServerLifecycleEvents.BEFORE_SAVE.register((server,flush,force)->HunterEyePower.save());
        ServerLifecycleEvents.SERVER_STOPPING.register(server->HunterEyePower.save());
        ServerLifecycleEvents.SERVER_STOPPED.register(server->HunterEyePower.stop());
        ServerTickEvents.END_SERVER_TICK.register(HunterEyePower::tick);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->HunterEyePower.join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->HunterEyePower.disconnect(handler.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{ if(entity instanceof ServerPlayer player) HunterEyePower.death(player); });
        HunterEyeVisualSync.register();
    }
}
