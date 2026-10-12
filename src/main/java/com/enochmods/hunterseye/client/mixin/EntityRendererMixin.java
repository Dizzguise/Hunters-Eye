package com.enochmods.hunterseye.client.mixin;

import com.enochmods.hunterseye.client.ConcealedRenderState;
import com.enochmods.hunterseye.client.HuntersEyeClient;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    // Both world extraction and the inventory paper doll use this populated-state overload.
    @Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;", at = @At("RETURN"))
    private void huntersEye$capture(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        ((ConcealedRenderState) cir.getReturnValue()).huntersEye$setConcealed(HuntersEyeClient.fullyHidden(entity));
    }
}
