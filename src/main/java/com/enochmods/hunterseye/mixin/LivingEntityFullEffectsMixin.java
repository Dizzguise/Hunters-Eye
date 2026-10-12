package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.ConcealmentMode;
import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFullEffectsMixin {
    @Shadow protected abstract void removeEffectParticles();

    @Unique private boolean hunterseye$full() {
        return (Object)this instanceof ServerPlayer player
                && HunterEyePower.getMode(player.getUUID()) == ConcealmentMode.FULL;
    }

    // Only the presentation metadata is cleared. The potion instances and their
    // timers remain untouched; vanilla rebuilds the metadata on leaving Full.
    @Inject(method = "updateInvisibilityStatus", at = @At("RETURN"))
    private void hunterseye$hiddenPotionMetadata(CallbackInfo ci) {
        if (hunterseye$full()) removeEffectParticles();
    }

    @Inject(method = "playBlockFallSound", at = @At("HEAD"), cancellable = true)
    private void hunterseye$quietLanding(CallbackInfo ci) {
        if (hunterseye$full()) ci.cancel();
    }

    // Keep fall damage, enchantments and block landing behavior; hide only the dust.
    @Redirect(method = "checkFallDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int hunterseye$hiddenLanding(ServerLevel level, ParticleOptions particle, double x, double y, double z,
            int count, double dx, double dy, double dz, double speed) {
        return hunterseye$full() ? 0 : level.sendParticles(particle, x, y, z, count, dx, dy, dz, speed);
    }
}
