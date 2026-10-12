package com.enochmods.eyevisuals.mixin;

import com.enochmods.eyevisuals.EyeVisualsClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.HoneyBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HoneyBlock.class)
public abstract class HoneyBlockParticlesMixin {
    @Inject(method = {"showSlideParticles", "showJumpParticles"}, at = @At("HEAD"), cancellable = true)
    private static void eyeVisuals$hiddenHoneyParticles(Entity source, CallbackInfo ci) {
        if (EyeVisualsClient.hides(source)) ci.cancel();
    }
}
