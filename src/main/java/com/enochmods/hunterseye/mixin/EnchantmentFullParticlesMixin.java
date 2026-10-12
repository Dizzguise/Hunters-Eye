package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.ConcealmentMode;
import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.SpawnParticlesEffect;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers server-originated equipment trails such as Soul Speed. */
@Mixin(SpawnParticlesEffect.class)
public abstract class EnchantmentFullParticlesMixin {
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void hunterseye$hiddenEquipmentParticles(ServerLevel level, int enchantmentLevel,
            EnchantedItemInUse item, Entity target, Vec3 pos, CallbackInfo ci) {
        if (target instanceof ServerPlayer player
                && HunterEyePower.getMode(player.getUUID()) == ConcealmentMode.FULL) ci.cancel();
    }
}
