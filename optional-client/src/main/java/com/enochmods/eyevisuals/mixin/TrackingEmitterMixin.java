package com.enochmods.eyevisuals.mixin;

import com.enochmods.eyevisuals.EyeVisualsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrackingEmitter.class)
public abstract class TrackingEmitterMixin {
    @Shadow @Final private Entity entity;

    // Existing emitters retain their target reference after Full starts, even
    // when the server removes an observer's player entity. Also retire emitters
    // for players that were unloaded; no world particles are cleared.
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void eyeVisuals$stopHiddenEmitter(CallbackInfo ci) {
        boolean absentPlayer = entity instanceof Player && entity != Minecraft.getInstance().player
                && (entity.isRemoved() || entity.level().getEntity(entity.getId()) != entity);
        if (EyeVisualsClient.hides(entity) || absentPlayer) {
            ((TrackingEmitter)(Object)this).remove();
            ci.cancel();
        }
    }
}
