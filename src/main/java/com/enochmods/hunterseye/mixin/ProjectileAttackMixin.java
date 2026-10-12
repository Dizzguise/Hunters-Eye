package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.HunterEyePower;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileAttackMixin {
    @Unique private static final Set<String> hunterseye$offensive=Set.of("arrow","spectral_arrow","trident","firework_rocket",
            "snowball","egg","wind_charge","breeze_wind_charge","fireball","small_fireball","dragon_fireball",
            "wither_skull","llama_spit","shulker_bullet","splash_potion","lingering_potion");
    @Inject(method="applyOnProjectileSpawned",at=@At("HEAD"))
    private void hunterseye$shoot(ServerLevel level,ItemStack stack,CallbackInfo ci) {
        Projectile projectile=(Projectile)(Object)this;
        if(hunterseye$offensive.contains(BuiltInRegistries.ENTITY_TYPE.getKey(projectile.getType()).getPath()))
            HunterEyePower.offensiveAction(projectile.getOwner());
    }
}
