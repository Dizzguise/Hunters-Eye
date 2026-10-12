package com.enochmods.hunterseye.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.enochmods.hunterseye.ConcealmentMode;
import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppress emissions at their player source, never by world position or sound name. */
@Mixin(Entity.class)
public abstract class EntityFullEffectsMixin {
    @Unique private static boolean hunterseye$full(Entity entity) {
        return entity instanceof ServerPlayer player
                && HunterEyePower.getMode(player.getUUID()) == ConcealmentMode.FULL;
    }

    @Inject(method = "walkingStepSound", at = @At("HEAD"), cancellable = true)
    private void hunterseye$quietSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (hunterseye$full((Entity)(Object)this)) ci.cancel();
    }

    @Inject(method = {"waterSwimSound", "spawnSprintParticle"}, at = @At("HEAD"), cancellable = true)
    private void hunterseye$quietMovement(CallbackInfo ci) {
        if (hunterseye$full((Entity)(Object)this)) ci.cancel();
    }

    // Splash also emits a game event. Redirect only its visual/audio calls so water
    // movement, sculk vibrations and all gameplay effects still execute normally.
    @WrapWithCondition(method = "doWaterSplashEffect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private boolean hunterseye$hiddenSplash(Level level, ParticleOptions particle, double x, double y, double z,
            double dx, double dy, double dz) {
        return !hunterseye$full((Entity)(Object)this);
    }

    @WrapWithCondition(method = "doWaterSplashEffect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private boolean hunterseye$quietSplash(Entity source, SoundEvent sound, float volume, float pitch) {
        return !hunterseye$full(source);
    }

    @WrapWithCondition(method = "handleOnAboveBubbleColumn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;sendBubbleColumnParticles(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private static boolean hunterseye$hiddenColumnSplash(Level level, BlockPos pos, Entity source,
            boolean downward, BlockPos columnPos) {
        return !hunterseye$full(source);
    }
}
