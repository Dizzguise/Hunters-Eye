package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerAttackMixin {
    // Block before attack effects, knockback, weapon wear or exhaustion; mining is unaffected.
    @Inject(method="attack",at=@At("HEAD"),cancellable=true)
    private void hunterseye$attack(Entity target,CallbackInfo ci) {
        if(HunterEyePower.blocksOutgoingDamage((Player)(Object)this)) ci.cancel();
    }
    @Inject(method="stabAttack",at=@At("HEAD"),cancellable=true)
    private void hunterseye$stab(EquipmentSlot slot,Entity target,float damage,boolean a,boolean b,boolean c,
            CallbackInfoReturnable<Boolean> cir) {
        if(HunterEyePower.blocksOutgoingDamage((Player)(Object)this)) cir.setReturnValue(false);
    }
    @Inject(method="startAutoSpinAttack",at=@At("HEAD"),cancellable=true)
    private void hunterseye$riptide(int ticks,float damage,ItemStack weapon,CallbackInfo ci) {
        if(HunterEyePower.blocksOutgoingDamage((Player)(Object)this)) ci.cancel();
    }
}
