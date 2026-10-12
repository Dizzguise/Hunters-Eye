package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.EyeData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Recipe previews have no identity; each actual crafted Eye receives its own UUID. */
@Mixin(ItemStack.class)
public abstract class CraftedEyeMixin {
    @Inject(method="onCraftedBy",at=@At("TAIL"))
    private void hunterseye$identifyCraftedEye(Player player,int amount,CallbackInfo callback) {
        ItemStack stack=(ItemStack)(Object)this;
        if(!player.level().isClientSide()&&EyeData.isEye(stack)) EyeData.ensureId(stack);
    }
}
