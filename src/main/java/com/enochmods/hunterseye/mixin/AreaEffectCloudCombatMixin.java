package com.enochmods.hunterseye.mixin;
import com.enochmods.hunterseye.PhantomCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudCombatMixin {
    @Inject(method="serverTick",at=@At("HEAD"),cancellable=true)
    private void hunterseye$pauseOwnedEffects(ServerLevel level,CallbackInfo ci) {
        if(PhantomCombat.blockedOwner(((AreaEffectCloud)(Object)this).getOwner())) ci.cancel();
    }
}
