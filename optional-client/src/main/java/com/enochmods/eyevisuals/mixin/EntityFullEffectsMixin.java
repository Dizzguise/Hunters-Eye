package com.enochmods.eyevisuals.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.enochmods.eyevisuals.EyeVisualsClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers owner-predicted movement, which never comes from a server particle packet. */
@Mixin(Entity.class)
public abstract class EntityFullEffectsMixin {
    @Inject(method = "walkingStepSound", at = @At("HEAD"), cancellable = true)
    private void eyeVisuals$quietSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (EyeVisualsClient.hides((Entity)(Object)this)) ci.cancel();
    }

    @Inject(method = {"waterSwimSound", "spawnSprintParticle"}, at = @At("HEAD"), cancellable = true)
    private void eyeVisuals$quietMovement(CallbackInfo ci) {
        if (EyeVisualsClient.hides((Entity)(Object)this)) ci.cancel();
    }

    @WrapWithCondition(method = "doWaterSplashEffect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private boolean eyeVisuals$hiddenSplash(Level level, ParticleOptions particle, double x, double y, double z,
            double dx, double dy, double dz) {
        return !EyeVisualsClient.hides((Entity)(Object)this);
    }

    @WrapWithCondition(method = "doWaterSplashEffect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private boolean eyeVisuals$quietSplash(Entity source, SoundEvent sound, float volume, float pitch) {
        return !EyeVisualsClient.hides(source);
    }
}
