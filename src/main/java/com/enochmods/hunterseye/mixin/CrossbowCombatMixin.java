package com.enochmods.hunterseye.mixin;
import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CrossbowItem.class)
public abstract class CrossbowCombatMixin {
    @Inject(method="performShooting",at=@At("HEAD"),cancellable=true)
    private void hunterseye$keepChargedAmmo(Level level,LivingEntity owner,InteractionHand hand,ItemStack stack,float speed,float inaccuracy,LivingEntity target,CallbackInfo ci) {
        if(HunterEyePower.blocksOutgoingDamage(owner)) ci.cancel();
    }
}
