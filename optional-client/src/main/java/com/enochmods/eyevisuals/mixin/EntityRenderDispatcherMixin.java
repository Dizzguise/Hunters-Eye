package com.enochmods.eyevisuals.mixin;

import com.enochmods.eyevisuals.EyeVisualsClient;
import com.enochmods.eyevisuals.OwnerRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    // Includes armor, held equipment, cape, name, fire and shadow in F5 / the inventory portrait.
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void eyeVisuals$hideOwner(EntityRenderState state, CameraRenderState camera, double x, double y,
            double z, PoseStack pose, SubmitNodeCollector collector, CallbackInfo ci) {
        if (((OwnerRenderState) state).eyeVisuals$isLocalOwner() && EyeVisualsClient.isFull()) ci.cancel();
    }
}
