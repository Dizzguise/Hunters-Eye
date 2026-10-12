package com.enochmods.hunterseye;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class HunterEyeCommands {
    private HunterEyeCommands() {}
    public static boolean canUseCommands(CommandSourceStack source) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source)
                || source.getPlayer()!=null&&source.getPlayer().isCreative();
    }
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher,registries,environment)->dispatcher.register(Commands.literal("hunterseye")
            .requires(HunterEyeCommands::canUseCommands)
            .executes(c->status(c.getSource()))
            .then(Commands.literal("status").executes(c->status(c.getSource())))
            .then(Commands.literal("gaze").executes(c->{ HunterEyePower.action(c.getSource().getPlayerOrException(),HunterEyeNetworking.Action.TOGGLE_TRACKING); return 1; }))
            .then(Commands.literal("phantom")
                .then(Commands.literal("normal").executes(c->phantom(c.getSource(),ConcealmentMode.NORMAL)))
                .then(Commands.literal("full").executes(c->phantom(c.getSource(),ConcealmentMode.FULL)))
                .then(Commands.literal("off").executes(c->phantom(c.getSource(),ConcealmentMode.NONE))))
            .then(Commands.literal("give")
                .then(Commands.literal("eye").executes(c->give(c,false))
                    .then(Commands.argument("player",EntityArgument.player()).executes(c->give(c,true)))))
            .then(Commands.literal("config")
                .executes(c->showConfig(c.getSource()))
                .then(Commands.literal("show").executes(c->showConfig(c.getSource())))
                .then(Commands.literal("bindRange").then(Commands.argument("blocks",IntegerArgumentType.integer(1,128)).executes(c->
                    changeConfig(c.getSource(),config->config.setBindRange(IntegerArgumentType.getInteger(c,"blocks"))))))
                .then(Commands.literal("trackingRange").then(Commands.argument("blocks",IntegerArgumentType.integer(1,30_000_000)).executes(c->
                    changeConfig(c.getSource(),config->config.setTrackingRange(IntegerArgumentType.getInteger(c,"blocks"))))))
                .then(Commands.literal("highlightRange").then(Commands.argument("blocks",IntegerArgumentType.integer(1,512)).executes(c->
                    changeConfig(c.getSource(),config->config.setHighlightRange(IntegerArgumentType.getInteger(c,"blocks"))))))
                .then(Commands.literal("normalCooldown").then(Commands.argument("seconds",IntegerArgumentType.integer(0,3600)).executes(c->
                    changeConfig(c.getSource(),config->config.setNormalCooldown(IntegerArgumentType.getInteger(c,"seconds"))))))
                .then(Commands.literal("fullCooldown").then(Commands.argument("seconds",IntegerArgumentType.integer(0,3600)).executes(c->
                    changeConfig(c.getSource(),config->config.setFullCooldown(IntegerArgumentType.getInteger(c,"seconds"))))))
                .then(Commands.literal("normalDuration").then(Commands.argument("seconds",IntegerArgumentType.integer(1,3600)).executes(c->
                    changeConfig(c.getSource(),config->config.setNormalDuration(IntegerArgumentType.getInteger(c,"seconds"))))))
                .then(Commands.literal("fullDuration").then(Commands.argument("seconds",IntegerArgumentType.integer(1,3600)).executes(c->
                    changeConfig(c.getSource(),config->config.setFullDuration(IntegerArgumentType.getInteger(c,"seconds"))))))
                .then(Commands.literal("crossDimension").then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->
                    changeConfig(c.getSource(),config->config.setCrossDimension(BoolArgumentType.getBool(c,"enabled"))))))
                .then(Commands.literal("color").then(Commands.argument("hex",StringArgumentType.word()).executes(c->
                    changeConfig(c.getSource(),config->config.setColor(StringArgumentType.getString(c,"hex"))))))
                .then(Commands.literal("reset").executes(c->changeConfig(c.getSource(),EyeConfig::reset))))
        ));
    }
    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        HunterEyePower.status(source.getPlayerOrException()); return 1;
    }
    private static int phantom(CommandSourceStack source,ConcealmentMode mode) throws CommandSyntaxException {
        HunterEyePower.setPhantom(source.getPlayerOrException(),mode); return 1;
    }
    private static int changeConfig(CommandSourceStack source,java.util.function.Consumer<EyeConfig> mutation) {
        try { mutation.accept(HunterEyePower.config()); }
        catch(IllegalArgumentException|IllegalStateException error) {
            source.sendFailure(Component.literal(error.getMessage())); return 0;
        }
        return showConfig(source);
    }
    private static int showConfig(CommandSourceStack source) {
        source.sendSuccess(()->Component.literal("Hunter's Eye: "+HunterEyePower.config().describe()),false); return 1;
    }
    private static int give(CommandContext<CommandSourceStack> context,boolean named) throws CommandSyntaxException {
        ServerPlayer player=named?EntityArgument.getPlayer(context,"player"):context.getSource().getPlayerOrException();
        ItemStack eye=EyeData.createEye();
        if(!player.getInventory().add(eye)) {
            context.getSource().sendFailure(Component.literal("That player's inventory is full. Make space and try again.")); return 0;
        }
        player.containerMenu.broadcastChanges();
        context.getSource().sendSuccess(()->Component.literal("Gave Hunter's Eye to "+player.getName().getString()),true); return 1;
    }
}
