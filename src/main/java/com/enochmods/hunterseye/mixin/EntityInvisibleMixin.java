package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityInvisibleMixin {
    @Inject(method="isInvisible",at=@At("RETURN"),cancellable=true)
    private void hunterseye$invisible(CallbackInfoReturnable<Boolean> cir) {
        if((Object)this instanceof ServerPlayer player && HunterEyePower.isInvisible(player.getUUID())) cir.setReturnValue(true);
    }
}
