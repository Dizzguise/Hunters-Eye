package com.enochmods.hunterseye.mixin;
import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin({BowItem.class,CrossbowItem.class,TridentItem.class})
public abstract class RangedReleaseCombatMixin {
    @Inject(method="releaseUsing",at=@At("HEAD"),cancellable=true)
    private void hunterseye$noReleaseSideEffects(ItemStack stack,Level level,LivingEntity owner,int remaining,CallbackInfoReturnable<Boolean> cir) {
        if(HunterEyePower.blocksOutgoingDamage(owner)) cir.setReturnValue(false);
    }
}
