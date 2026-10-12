package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.PhantomCombat;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileAttackMixin {
    @Inject(method="spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;",
            at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),cancellable=true)
    private static void hunterseye$preventSpawn(Projectile projectile,ServerLevel level,ItemStack stack,
            Consumer<Projectile> initializer,CallbackInfoReturnable<Projectile> cir) {
        if(PhantomCombat.blockedProjectile(projectile)) { projectile.discard(); cir.setReturnValue(projectile); }
    }
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void hunterseye$disablePendingProjectile(CallbackInfo ci) {
        Projectile projectile=(Projectile)(Object)this;
        if(!projectile.level().isClientSide()&&PhantomCombat.blockedProjectile(projectile)) {
            PhantomCombat.removeActiveProjectile(projectile); ci.cancel();
        }
    }
    @Inject(method="hitTargetOrDeflectSelf",at=@At("HEAD"),cancellable=true)
    private void hunterseye$preventImpact(HitResult hit,CallbackInfoReturnable<ProjectileDeflection> cir) {
        Projectile projectile=(Projectile)(Object)this;
        if(!projectile.level().isClientSide()&&PhantomCombat.blockedProjectile(projectile)) {
            PhantomCombat.removeActiveProjectile(projectile); cir.setReturnValue(ProjectileDeflection.NONE);
        }
    }
}
