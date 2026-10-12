package com.enochmods.hunterseye.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityInvisibilityAccessor {
    @Invoker("updateInvisibilityStatus")
    void hunterseye$updateInvisibilityStatus();
}
