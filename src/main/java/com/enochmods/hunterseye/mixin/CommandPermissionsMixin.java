package com.enochmods.hunterseye.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Creative access changes without an operator-level change, so refresh command suggestions. */
@Mixin(ServerPlayerGameMode.class)
public abstract class CommandPermissionsMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method="changeGameModeForPlayer",at=@At("RETURN"))
    private void hunterseye$refreshCommands(GameType mode,CallbackInfoReturnable<Boolean> result) {
        if(result.getReturnValueZ()) player.level().getServer().getCommands().sendCommands(player);
    }
}
