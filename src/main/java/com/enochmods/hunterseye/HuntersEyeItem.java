package com.enochmods.hunterseye;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Vanilla ender eyes are intercepted on the server before their ordinary behavior. */
public final class HuntersEyeItem {
    private HuntersEyeItem() {}
    public static void registerInteractions() {
        UseItemCallback.EVENT.register((player,level,hand)->use(player,hand));
        UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            InteractionResult result=use(player,hand);
            if(result!=InteractionResult.PASS&&player instanceof ServerPlayer serverPlayer) {
                // Correct vanilla client's predicted insertion into an End portal frame.
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(level,hit.getBlockPos()));
            }
            return result;
        });
        UseEntityCallback.EVENT.register(HuntersEyeItem::interactWithEntity);
    }
    private static InteractionResult use(Player player,InteractionHand hand) {
        ItemStack eye=player.getItemInHand(hand);
        if(!(player instanceof ServerPlayer serverPlayer)||!EyeData.isEye(eye)) return InteractionResult.PASS;
        HunterEyePower.use(serverPlayer,eye);
        // Client prediction follows the ordinary Ender Eye; restore the authoritative inventory.
        serverPlayer.inventoryMenu.sendAllDataToRemote();
        return InteractionResult.SUCCESS;
    }
    public static InteractionResult interactWithEntity(Player player,Level level,InteractionHand hand,Entity entity,EntityHitResult hit) {
        // Consume ordinary use on entities too so villagers/animals cannot hijack selection.
        return use(player,hand);
    }
}
