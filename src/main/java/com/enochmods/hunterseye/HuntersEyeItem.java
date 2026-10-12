package com.enochmods.hunterseye;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public final class HuntersEyeItem extends Item {
    public HuntersEyeItem(Properties properties) { super(properties); }
    public static void registerInteractions() {
        // Entity interaction precedes item interaction, so intercept before villagers trade.
        UseEntityCallback.EVENT.register(HuntersEyeItem::interactWithEntity);
    }
    public static InteractionResult interactWithEntity(Player player,Level level,InteractionHand hand,Entity entity,EntityHitResult hit) {
        ItemStack eye=player.getItemInHand(hand);
        if(!player.isAlive()||player.isSpectator()||!player.isShiftKeyDown()||!EyeData.isEye(eye)
                ||!(entity instanceof Player||entity instanceof Mob)) return InteractionResult.PASS;
        // The supplied entity is only an interaction hint; binding uses the server's gaze and LOS.
        if(player instanceof ServerPlayer serverPlayer) HunterEyePower.use(serverPlayer,eye);
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) HunterEyePower.use(serverPlayer, player.getItemInHand(hand));
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Sneak + use: bind a visible player or mob (32 blocks)"));
        tooltip.accept(Component.literal("Use: select this eye. Carry it to keep powers."));
        String target=EyeData.targetName(stack);
        tooltip.accept(Component.literal(target.isEmpty()?"Unbound":"Bound to: "+target));
    }
}
