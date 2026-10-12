package com.enochmods.hunterseye.client.mixin;

import com.enochmods.hunterseye.client.ConcealedRenderState;
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
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void huntersEye$hide(EntityRenderState state, CameraRenderState camera, double x, double y, double z, PoseStack pose, SubmitNodeCollector collector, CallbackInfo ci) {
        // Cancel the complete submission: model, equipment, name, fire, shadow and shared glow.
        if (((ConcealedRenderState) state).huntersEye$isConcealed()) ci.cancel();
    }
}
