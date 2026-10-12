package com.enochmods.hunterseye;

import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Namespaced custom data survives item moves, saves, deaths and transfers. */
public final class EyeData {
    private static final String ID="hunterseye_eye_id", TARGET="hunterseye_target", NAME="hunterseye_target_name", KIND="hunterseye_target_kind";
    public enum TargetKind { PLAYER, MOB }
    private EyeData() {}
    public static boolean isEye(ItemStack stack) { return !stack.isEmpty() && stack.getItem() == HuntersEye.HUNTERS_EYE; }
    private static CompoundTag tag(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag(); }
    private static UUID uuid(String text) { try { return UUID.fromString(text); } catch (IllegalArgumentException e) { return null; } }
    public static UUID id(ItemStack stack) { return isEye(stack)?uuid(tag(stack).getStringOr(ID,"")):null; }
    public static UUID ensureId(ItemStack stack) { UUID id=id(stack); if(id==null) id=renewId(stack); return id; }
    public static UUID renewId(ItemStack stack) {
        UUID id=UUID.randomUUID(); CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putString(ID,id.toString())); return id;
    }
    public static UUID target(ItemStack stack) { return isEye(stack)?uuid(tag(stack).getStringOr(TARGET,"")):null; }
    public static String targetName(ItemStack stack) { return isEye(stack)?tag(stack).getStringOr(NAME,""):""; }
    /** Eyes saved before mob tracking have player UUIDs and no kind field. */
    public static TargetKind targetKind(ItemStack stack) {
        return isEye(stack)&&"mob".equals(tag(stack).getStringOr(KIND,"player"))?TargetKind.MOB:TargetKind.PLAYER;
    }
    public static void bind(ItemStack stack, UUID target, String name) {
        bind(stack,target,name,TargetKind.PLAYER);
    }
    public static void bind(ItemStack stack, UUID target, String name, TargetKind kind) {
        ensureId(stack);
        CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{ t.putString(TARGET,target.toString()); t.putString(NAME,name); t.putString(KIND,kind==TargetKind.MOB?"mob":"player"); });
    }
}
