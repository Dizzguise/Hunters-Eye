package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityInvisibilityMixin {
    @Inject(method="updateInvisibilityStatus",at=@At("RETURN"))
    private void hunterseye$syncVanillaInvisibility(CallbackInfo ci) {
        if((Object)this instanceof ServerPlayer player && HunterEyePower.isInvisible(player.getUUID())) {
            player.setInvisible(true);
        }
    }
}
