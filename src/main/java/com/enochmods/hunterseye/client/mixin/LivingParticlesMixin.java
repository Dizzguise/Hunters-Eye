package com.enochmods.hunterseye.client.mixin;

import com.enochmods.hunterseye.client.HuntersEyeClient;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingParticlesMixin {
    // spawnItemParticles covers food/use particles and breakItem's emission; leave item-break sounds intact.
    @Redirect(method = {"tickEffects", "spawnItemParticles", "makeDrownParticles"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"),
            require = 3, allow = 3)
    private void huntersEye$particles(Level level, ParticleOptions type, double x, double y, double z, double dx, double dy, double dz) {
        if (!level.isClientSide() || !HuntersEyeClient.fullyHidden((LivingEntity) (Object) this)) level.addParticle(type, x, y, z, dx, dy, dz);
    }
}
