package com.enochmods.hunterseye.client.mixin;

import com.enochmods.hunterseye.client.HuntersEyeClient;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackingEmitter.class)
public class TrackingEmitterMixin {
    @Shadow @Final private Entity entity;
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void huntersEye$emitter(CallbackInfo ci) {
        if (HuntersEyeClient.fullyHidden(entity)) { ((Particle) (Object) this).remove(); ci.cancel(); }
    }
}
