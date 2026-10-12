package com.enochmods.eyevisuals.mixin;

import com.enochmods.eyevisuals.EyeVisualsClient;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFullEffectsMixin {
    @Inject(method = "playBlockFallSound", at = @At("HEAD"), cancellable = true)
    private void eyeVisuals$quietLanding(CallbackInfo ci) {
        if (EyeVisualsClient.hides((Entity)(Object)this)) ci.cancel();
    }

    // Redirect the particle calls only: potion durations, entity events, item use
    // and drowning continue to execute, even while their body particles are hidden.
    @Redirect(method = {"tickEffects", "spawnItemParticles", "makeDrownParticles", "makePoofParticles", "handleEntityEvent"},
            at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void eyeVisuals$hiddenBodyParticles(Level level, ParticleOptions particle,
            double x, double y, double z, double dx, double dy, double dz) {
        if (!EyeVisualsClient.hides((Entity)(Object)this)) level.addParticle(particle, x, y, z, dx, dy, dz);
    }
}
