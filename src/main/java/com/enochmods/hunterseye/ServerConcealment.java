package com.enochmods.hunterseye;

import com.enochmods.hunterseye.mixin.ChunkMapAccessor;
import com.enochmods.hunterseye.mixin.LivingEntityInvisibilityAccessor;
import net.minecraft.server.level.ServerPlayer;

/** Vanilla metadata and entity pairing; no client mod or custom packets required. */
public final class ServerConcealment {
    private ServerConcealment() {}

    /** Call after changing the effective mode, and for each newly joined player. */
    public static void changed(ServerPlayer target) {
        // Recompute from vanilla effects first. The mixin adds the Eye's independent flag.
        // Removing the Eye therefore cannot remove an invisibility potion or its duration.
        ((LivingEntityInvisibilityAccessor)target).hunterseye$updateInvisibilityStatus();
        TrackedEntityAccess tracked=tracked(target);
        if(tracked==null) return; // Joining before the chunk tracker is installed is valid.
        for(ServerPlayer observer:target.level().getServer().getPlayerList().getPlayers()) {
            if(observer.level()==target.level()) tracked.hunterseye$updatePlayer(observer);
        }
    }

    /** Read-only view of native pairing, useful for dedicated-server verification. */
    public static boolean isTrackedBy(ServerPlayer target,ServerPlayer observer) {
        TrackedEntityAccess tracked=tracked(target);
        return tracked!=null && tracked.hunterseye$isTrackedBy(observer);
    }

    private static TrackedEntityAccess tracked(ServerPlayer target) {
        Object tracked=((ChunkMapAccessor)target.level().getChunkSource().chunkMap)
                .hunterseye$entityMap().get(target.getId());
        return tracked instanceof TrackedEntityAccess access?access:null;
    }

    /** Implemented directly on the vanilla tracker by a server mixin. */
    public interface TrackedEntityAccess {
        void hunterseye$updatePlayer(ServerPlayer observer);
        boolean hunterseye$isTrackedBy(ServerPlayer observer);
    }
}
