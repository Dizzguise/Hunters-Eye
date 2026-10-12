package com.enochmods.hunterseye;

import java.util.UUID;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;

/** Namespaced custom data survives item moves, saves, deaths and transfers. */
public final class EyeData {
    private static final String ID="hunterseye_eye_id", TARGET="hunterseye_target", NAME="hunterseye_target_name", KIND="hunterseye_target_kind", VARIANT="hunterseye_variant";
    public enum TargetKind { PLAYER, MOB }
    private EyeData() {}
    public static boolean isEye(ItemStack stack) {
        if(stack.isEmpty()||!stack.is(Items.ENDER_EYE)) return false;
        String variant=tag(stack).getStringOr(VARIANT,"");
        return "eye".equals(variant)||isLegacyVariant(variant);
    }
    /** Compatibility only: old item identities and bindings migrate to the regular Eye. */
    private static boolean isLegacyVariant(String variant) { return "clairvoyance".equals(variant); }
    public static ItemStack createEye() {
        ItemStack stack=new ItemStack(Items.ENDER_EYE);
        CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putString(VARIANT,"eye"));
        stack.set(DataComponents.MAX_STACK_SIZE,1);
        ensureId(stack); refreshPresentation(stack); return stack;
    }
    private static void refreshPresentation(ItemStack stack) {
        stack.set(DataComponents.CUSTOM_NAME,Component.literal("Hunter's Eye")
                .withStyle(style->style.withColor(0xB68AE8).withItalic(false)));
        stack.set(DataComponents.LORE,new ItemLore(List.of(
                Component.literal("Hunter's Gaze / Phantom Hunt"),
                Component.literal("Full: noncombat concealment"),
                Component.literal("Use: select / Sneak + use: bind visible player or mob"),
                Component.literal("Swap hands: Full / Drop: Normal / Sneak + attack: Gaze"),
                Component.literal(target(stack)==null?"Unbound":"Bound to: "+targetName(stack)))));
    }
    private static CompoundTag tag(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag(); }
    private static UUID uuid(String text) { try { return UUID.fromString(text); } catch (IllegalArgumentException e) { return null; } }
    public static UUID id(ItemStack stack) { return isEye(stack)?uuid(tag(stack).getStringOr(ID,"")):null; }
    public static UUID ensureId(ItemStack stack) { refreshModel(stack); UUID id=id(stack); if(id==null) id=renewId(stack); return id; }
    public static void refreshModel(ItemStack stack) {
        if(!isEye(stack)) return;
        boolean migrated=isLegacyVariant(tag(stack).getStringOr(VARIANT,""));
        if(migrated) CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putString(VARIANT,"eye"));
        String model="hunterseye:hunters_eye";
        var old=stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA,CustomModelData.EMPTY);
        if(model.equals(old.getString(0))&&!migrated) return;
        var strings=new java.util.ArrayList<>(old.strings());
        if(strings.isEmpty()) strings.add(model); else strings.set(0,model);
        stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(old.floats(),old.flags(),strings,old.colors()));
        refreshPresentation(stack);
    }
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
        refreshPresentation(stack);
    }
}
