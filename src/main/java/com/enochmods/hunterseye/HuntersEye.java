package com.enochmods.hunterseye;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HuntersEye implements ModInitializer {
    public static final String MOD_ID="hunterseye";
    public static final Logger LOGGER=LoggerFactory.getLogger(MOD_ID);
    public static final Identifier ITEM_ID=Identifier.fromNamespaceAndPath(MOD_ID,"hunters_eye");
    public static final Item HUNTERS_EYE=new HuntersEyeItem(new Item.Properties()
            .setId(ResourceKey.create(Registries.ITEM,ITEM_ID)).stacksTo(1).rarity(Rarity.EPIC));
    @Override public void onInitialize() {
        Registry.register(BuiltInRegistries.ITEM,ITEM_ID,HUNTERS_EYE);
        HuntersEyeItem.registerInteractions();
        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                Identifier.withDefaultNamespace("tools_and_utilities"))).register(output->output.accept(HUNTERS_EYE));
        HunterEyeNetworking.registerPayloads();
        ServerPlayNetworking.registerGlobalReceiver(HunterEyeNetworking.CommandPayload.TYPE,
                (payload,context)->HunterEyePower.action(context.player(),payload.action()));
        ServerLifecycleEvents.SERVER_STARTING.register(HunterEyePower::start);
        ServerLifecycleEvents.BEFORE_SAVE.register((server,flush,force)->HunterEyePower.save());
        ServerLifecycleEvents.SERVER_STOPPING.register(server->HunterEyePower.save());
        ServerLifecycleEvents.SERVER_STOPPED.register(server->HunterEyePower.stop());
        ServerTickEvents.END_SERVER_TICK.register(HunterEyePower::tick);
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->HunterEyePower.join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->HunterEyePower.disconnect(handler.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->{ if(entity instanceof ServerPlayer player) HunterEyePower.death(player); });
    }
}
