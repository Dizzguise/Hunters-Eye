package com.enochmods.hunterseye.mixin;
import com.enochmods.hunterseye.HunterEyePower;
import com.enochmods.hunterseye.HunterEyeControls;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerAttackPacketMixin {
    @Shadow public ServerPlayer player;
    @Shadow public abstract boolean hasClientLoaded();
    @Inject(method="handleAttack",at=@At(value="INVOKE",
            target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift=At.Shift.AFTER),cancellable=true)
    private void hunterseye$blockAttackDispatch(ServerboundAttackPacket packet,CallbackInfo ci) {
        if(hasClientLoaded()&&HunterEyeControls.leftClick(player)) { ci.cancel(); return; }
        if(HunterEyePower.blocksOutgoingDamage(player)) { HunterEyePower.offensiveAction(player); ci.cancel(); }
    }
}
