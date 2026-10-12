package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.ConcealmentMode;
import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.PlaySoundEffect;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlaySoundEffect.class)
public abstract class EnchantmentFullSoundMixin {
    @Redirect(method = "apply", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void hunterseye$quietSoulSpeed(ServerLevel level, Entity excluded, double x, double y, double z,
            Holder<SoundEvent> sound, SoundSource category, float volume, float pitch,
            ServerLevel effectLevel, int enchantmentLevel, EnchantedItemInUse item, Entity target, Vec3 pos) {
        if (target instanceof ServerPlayer player
                && HunterEyePower.getMode(player.getUUID()) == ConcealmentMode.FULL
                && sound.value() == SoundEvents.SOUL_ESCAPE.value()) return;
        level.playSound(excluded, x, y, z, sound, category, volume, pitch);
    }
}
