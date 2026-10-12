package com.enochmods.hunterseye.mixin;

import com.enochmods.hunterseye.HunterEyeControls;
import com.enochmods.hunterseye.HunterEyeNetworking;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerEyeControlsMixin {
    @Shadow public ServerPlayer player;
    @Shadow public abstract boolean hasClientLoaded();
    @Shadow public abstract void ackBlockChangesUpTo(int sequence);

    @Inject(method="handlePlayerAction",at=@At(value="INVOKE",
            target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift=At.Shift.AFTER),cancellable=true)
    private void hunterseye$controls(ServerboundPlayerActionPacket packet,CallbackInfo ci) {
        if(!hasClientLoaded()) return;
        boolean consumed=switch(packet.getAction()) {
            case SWAP_ITEM_WITH_OFFHAND -> HunterEyeControls.toggle(player,HunterEyeNetworking.Action.TOGGLE_FULL);
            case DROP_ITEM,DROP_ALL_ITEMS -> HunterEyeControls.toggle(player,HunterEyeNetworking.Action.TOGGLE_NORMAL);
            case START_DESTROY_BLOCK -> HunterEyeControls.leftClick(player);
            case ABORT_DESTROY_BLOCK,STOP_DESTROY_BLOCK -> HunterEyeControls.continueMining(player);
            default -> false;
        };
        if(!consumed) return;
        switch(packet.getAction()) {
            case START_DESTROY_BLOCK,ABORT_DESTROY_BLOCK,STOP_DESTROY_BLOCK -> {
                ackBlockChangesUpTo(packet.getSequence());
                // Packet positions are untrusted. Correct nearby loaded predictions without loading chunks.
                if(player.level().hasChunkAt(packet.getPos())&&player.isWithinBlockInteractionRange(packet.getPos(),1.0))
                    player.connection.send(new ClientboundBlockUpdatePacket(player.level(),packet.getPos()));
            }
            default -> { }
        }
        ci.cancel();
    }

    @Inject(method="handleAnimate",at=@At(value="INVOKE",
            target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift=At.Shift.AFTER))
    private void hunterseye$airGaze(ServerboundSwingPacket packet,CallbackInfo ci) {
        if(hasClientLoaded()) HunterEyeControls.swing(player,packet.getHand());
    }
}
