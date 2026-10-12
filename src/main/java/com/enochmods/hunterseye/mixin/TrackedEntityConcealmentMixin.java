package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.ConcealmentMode;
import com.enochmods.hunterseye.HunterEyePower;
import com.enochmods.hunterseye.ServerConcealment;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityConcealmentMixin implements ServerConcealment.TrackedEntityAccess {
    @Shadow @Final private Entity entity;
    @Shadow @Final private Set<ServerPlayerConnection> seenBy;
    @Shadow public abstract void removePlayer(ServerPlayer observer);
    @Shadow public abstract void updatePlayer(ServerPlayer observer);

    @Inject(method="updatePlayer",at=@At("HEAD"),cancellable=true)
    private void hunterseye$gatePairing(ServerPlayer observer,CallbackInfo ci) {
        if(entity instanceof ServerPlayer target && target!=observer
                && HunterEyePower.getMode(target.getUUID())==ConcealmentMode.FULL) {
            // Native removal sends the vanilla removal packet once, clears seenBy, and stops
            // future movement/equipment/metadata broadcasts. Native range checks remain intact
            // when revealed, and addPairing sends a complete current spawn bundle once.
            removePlayer(observer);
            ci.cancel();
        }
    }

    @Override public void hunterseye$updatePlayer(ServerPlayer observer) { updatePlayer(observer); }
    @Override public boolean hunterseye$isTrackedBy(ServerPlayer observer) {
        return seenBy.contains(observer.connection);
    }
}
