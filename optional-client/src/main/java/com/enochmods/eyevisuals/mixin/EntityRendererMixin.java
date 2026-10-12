package com.enochmods.eyevisuals.mixin;

import com.enochmods.eyevisuals.OwnerRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
    private void eyeVisuals$captureOwner(Entity entity, float tick, CallbackInfoReturnable<EntityRenderState> cir) {
        ((OwnerRenderState) cir.getReturnValue()).eyeVisuals$setLocalOwner(entity == Minecraft.getInstance().player);
    }
}
