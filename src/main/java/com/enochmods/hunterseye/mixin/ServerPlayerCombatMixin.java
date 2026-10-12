package com.enochmods.hunterseye.mixin;
import com.enochmods.hunterseye.PhantomCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerCombatMixin {
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void hunterseye$blockAttributedDamage(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir) {
        if(PhantomCombat.blocksDamage((ServerPlayer)(Object)this,source)) cir.setReturnValue(false);
    }
}
