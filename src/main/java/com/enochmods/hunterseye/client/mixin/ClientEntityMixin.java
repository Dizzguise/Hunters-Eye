package com.enochmods.hunterseye.client.mixin;

import com.enochmods.hunterseye.ConcealmentMode;
import com.enochmods.hunterseye.client.HuntersEyeClient;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class ClientEntityMixin {
    @Inject(method = "isInvisible", at = @At("RETURN"), cancellable = true)
    private void huntersEye$normal(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide() && HuntersEyeClient.mode(entity.getUUID()) != ConcealmentMode.NONE) cir.setReturnValue(true);
    }
    @Inject(method = "spawnSprintParticle", at = @At("HEAD"), cancellable = true)
    private void huntersEye$dust(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity.level().isClientSide() && HuntersEyeClient.fullyHidden(entity)) ci.cancel();
    }

    // Suppress the two player-owned bubble/splash emissions while keeping splash sounds and game events.
    @Redirect(method = "doWaterSplashEffect", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"),
            require = 2, allow = 2)
    private void huntersEye$splash(Level level, ParticleOptions type, double x, double y, double z,
                                  double dx, double dy, double dz) {
        if (!level.isClientSide() || !HuntersEyeClient.fullyHidden((Entity) (Object) this)) {
            level.addParticle(type, x, y, z, dx, dy, dz);
        }
    }
}
