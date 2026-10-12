package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.ConcealmentMode;
import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelConcealmentSoundMixin {
    @Inject(method="playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at=@At("HEAD"),cancellable=true)
    private void hunterseye$audibleHiddenEntity(Entity excluded,Entity source,Holder<SoundEvent> sound,
            SoundSource category,float volume,float pitch,long seed,CallbackInfo ci) {
        if(source instanceof ServerPlayer player && HunterEyePower.getMode(player.getUUID())==ConcealmentMode.FULL) {
            // Vanilla entity sounds need a client entity to resolve their position. Observers
            // have none in Full, so retain the same sound/range/exclusion using a position packet.
            ((ServerLevel)(Object)this).playSeededSound(excluded,source.getX(),source.getY(),source.getZ(),
                    sound,category,volume,pitch,seed);
            ci.cancel();
        }
    }
}
