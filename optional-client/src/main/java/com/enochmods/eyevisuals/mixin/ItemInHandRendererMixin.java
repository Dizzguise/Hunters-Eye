package com.enochmods.eyevisuals.mixin;

import com.enochmods.eyevisuals.EyeVisualsClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void eyeVisuals$hideHands(float tick, PoseStack pose, SubmitNodeCollector collector,
            LocalPlayer player, int light, CallbackInfo ci) {
        if (EyeVisualsClient.hides(player)) ci.cancel();
    }
}
