package com.enochmods.hunterseye.mixin;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ServerEntityConcealmentMixin {
    @Shadow @Final private Entity entity;
    @Shadow private List<SynchedEntityData.DataValue<?>> trackedDataValues;

    @Inject(method="sendPairingData",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/entity/Entity;updateDataBeforeSync()V",shift=At.Shift.AFTER))
    private void hunterseye$currentMetadata(ServerPlayer observer,
            Consumer<Packet<ClientGamePacketListener>> packets,CallbackInfo ci) {
        if(entity instanceof ServerPlayer) {
            // Vanilla caches metadata between periodic sendChanges calls. Reveal and a late
            // join can pair in that interval: use current flags in their initial spawn bundle,
            // while retaining dirty data for the existing observers and the owner's next sync.
            trackedDataValues=entity.getEntityData().getNonDefaultValues();
        }
    }
}
