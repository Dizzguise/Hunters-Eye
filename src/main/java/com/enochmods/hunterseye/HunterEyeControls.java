package com.enochmods.hunterseye;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/** Vanilla input packets are consumed only while a tagged Eye is held in the main hand. */
public final class HunterEyeControls {
    private static final Map<UUID,EyeGestureGuard> gestures=new HashMap<>();
    private HunterEyeControls() {}
    public static boolean held(ServerPlayer player) {
        return player.isAlive()&&!player.isSpectator()&&player.containerMenu==player.inventoryMenu
                &&EyeData.isEye(player.getMainHandItem());
    }
    public static boolean gazeGesture(ServerPlayer player) { return held(player)&&player.isShiftKeyDown(); }
    private static EyeGestureGuard guard(ServerPlayer player) {
        return gestures.computeIfAbsent(player.getUUID(),id->new EyeGestureGuard());
    }
    public static void use(ServerPlayer player) {
        if(held(player)) guard(player).suppressUse(player.level().getGameTime());
    }
    public static void forget(ServerPlayer player) { gestures.remove(player.getUUID()); }
    public static void clear() { gestures.clear(); }
    public static boolean toggle(ServerPlayer player,HunterEyeNetworking.Action action) {
        if(!held(player)) return false;
        // Drop also sends a swing. A sneaking Q must not activate Gaze as a second action.
        guard(player).suppressUse(player.level().getGameTime());
        HunterEyePower.heldAction(player,action);
        player.resetLastActionTime();
        // Vanilla predicts drop locally (swap is server-driven); re-send even when cooldown blocks activation.
        player.inventoryMenu.sendAllDataToRemote();
        return true;
    }
    public static boolean leftClick(ServerPlayer player) {
        if(!gazeGesture(player)) return false;
        if(guard(player).accept(player.level().getGameTime()))
            HunterEyePower.heldAction(player,HunterEyeNetworking.Action.TOGGLE_TRACKING);
        player.resetLastActionTime();
        return true;
    }
    public static boolean continueMining(ServerPlayer player) {
        if(!gazeGesture(player)) return false;
        guard(player).continuation(player.level().getGameTime());
        return true;
    }
    public static void swing(ServerPlayer player,InteractionHand hand) {
        if(hand==InteractionHand.MAIN_HAND) leftClick(player);
    }
}
