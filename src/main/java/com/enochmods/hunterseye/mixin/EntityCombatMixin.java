package com.enochmods.hunterseye.mixin;
import com.enochmods.hunterseye.PhantomCombat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class EntityCombatMixin {
    @Inject(method="hurtOrSimulate",at=@At("HEAD"),cancellable=true)
    private void hunterseye$blockAttributedDamage(DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir) {
        Entity entity=(Entity)(Object)this;
        if(!entity.level().isClientSide()&&PhantomCombat.blocksDamage(entity,source)) cir.setReturnValue(false);
    }
    @Inject(method="hurt",at=@At("HEAD"),cancellable=true)
    private void hunterseye$blockAttributedDamageVoid(DamageSource source,float amount,CallbackInfo ci) {
        Entity entity=(Entity)(Object)this;
        if(!entity.level().isClientSide()&&PhantomCombat.blocksDamage(entity,source)) ci.cancel();
    }
}
