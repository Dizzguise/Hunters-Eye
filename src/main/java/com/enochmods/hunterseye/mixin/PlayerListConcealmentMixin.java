package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.HunterEyePower;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListConcealmentMixin {
    @Inject(method="placeNewPlayer",at=@At(value="INVOKE",
            target="Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void hunterseye$restoreBeforePairing(Connection connection,ServerPlayer player,
            CommonListenerCookie cookie,CallbackInfo ci) {
        // Inventory has been loaded and the connection installed here. Fabric JOIN occurs
        // later: restore saved Full before this player is first offered to other observers.
        HunterEyePower.prepareJoin(player);
    }
}
