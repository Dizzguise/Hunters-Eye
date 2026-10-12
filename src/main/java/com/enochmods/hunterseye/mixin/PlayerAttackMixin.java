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
    // Packet reach/LOS checks have already accepted the entity attack before Player.attack.
    // HEAD intentionally counts shield blocks and damage immunity, but never air swings/mining.
    @Inject(method="attack",at=@At("HEAD"))
    private void hunterseye$attack(Entity target,CallbackInfo ci) { HunterEyePower.offensiveAction((Player)(Object)this); }
    @Inject(method="stabAttack",at=@At("HEAD"))
    private void hunterseye$stab(EquipmentSlot slot,Entity target,float damage,boolean a,boolean b,boolean c,
            CallbackInfoReturnable<Boolean> cir) { HunterEyePower.offensiveAction((Player)(Object)this); }
    @Inject(method="startAutoSpinAttack",at=@At("HEAD"))
    private void hunterseye$riptide(int ticks,float damage,ItemStack weapon,CallbackInfo ci) {
        HunterEyePower.offensiveAction((Player)(Object)this);
    }
}
