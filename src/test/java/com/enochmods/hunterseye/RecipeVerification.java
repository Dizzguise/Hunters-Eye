package com.enochmods.hunterseye;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** Decodes the shipped data with vanilla codecs, then checks crafting and recipe-book output. */
public final class RecipeVerification {
    private static int checks;

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var lookup = VanillaRegistries.createLookup();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(lookup).forEach(pending -> pending.apply());
        var ops = lookup.createSerializationContext(JsonOps.INSTANCE);
        Recipe<?> decoded = Recipe.CODEC.parse(ops, resource("data/hunterseye/recipe/hunters_eye.json")).getOrThrow();
        check(decoded instanceof ShapedRecipe, "shipped data uses vanilla shaped recipe");
        ShapedRecipe recipe = (ShapedRecipe) decoded;
        check(recipe.getSerializer() == ShapedRecipe.SERIALIZER, "no custom network serializer needed");
        check(recipe.getWidth() == 3 && recipe.getHeight() == 3, "recipe requires a 3x3 crafting table");
        List<ItemStack> grid = List.of(new ItemStack(Items.DIAMOND_BLOCK), new ItemStack(Items.NETHERITE_SPEAR), new ItemStack(Items.DIAMOND_BLOCK),
                new ItemStack(Items.NETHERITE_SWORD), new ItemStack(Items.ENDER_EYE), new ItemStack(Items.NETHERITE_AXE),
                new ItemStack(Items.DIAMOND_BLOCK), new ItemStack(Items.BEACON), new ItemStack(Items.DIAMOND_BLOCK));
        CraftingInput input = CraftingInput.of(3, 3, grid);
        check(recipe.matches(input, null), "diamond block corners, spear top, sword/axe sides, beacon bottom and Ender Eye center craft");
        List<ItemStack> oldGrid = List.of(new ItemStack(Items.GOLD_BLOCK), new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.GOLD_BLOCK),
                new ItemStack(Items.NETHERITE_INGOT), new ItemStack(Items.ENDER_EYE), new ItemStack(Items.NETHERITE_INGOT),
                new ItemStack(Items.GOLD_BLOCK), new ItemStack(Items.DIAMOND_AXE), new ItemStack(Items.GOLD_BLOCK));
        check(!recipe.matches(CraftingInput.of(3, 3, oldGrid), null), "old gold-block recipe is rejected");
        var mirrored = new ArrayList<>(grid);
        mirrored.set(3, grid.get(5));
        mirrored.set(5, grid.get(3));
        check(recipe.matches(CraftingInput.of(3, 3, mirrored), null), "vanilla horizontal mirror also crafts");
        for (int slot = 0; slot < 9; slot++) {
            var wrong = new ArrayList<>(grid);
            wrong.set(slot, new ItemStack(Items.AMETHYST_SHARD));
            check(!recipe.matches(CraftingInput.of(3, 3, wrong), null), "wrong ingredient rejected at slot " + slot);
            var missing = new ArrayList<>(grid);
            missing.set(slot, ItemStack.EMPTY);
            check(!recipe.matches(CraftingInput.of(3, 3, missing), null), "missing ingredient rejected at slot " + slot);
            check(recipe.getIngredients().get(slot).orElseThrow().test(grid.get(slot)), "ingredient codec preserves slot " + slot);
        }
        check(!recipe.matches(CraftingInput.of(2, 2, grid.subList(0, 4)), null), "player 2x2 grid cannot craft the Eye");
        ItemStack result = recipe.assemble(input);
        check(ItemStack.matches(result, template()), "crafted output exactly matches command-created Eye components");
        check(result.getCount() == 1 && result.getMaxStackSize() == 1, "crafting creates one unstackable Eye");
        check(EyeData.isEye(result), "crafted Eye activates via vanilla custom-data identity");
        check("hunterseye:hunters_eye".equals(result.get(DataComponents.CUSTOM_MODEL_DATA).getString(0)), "crafted Eye selects original artwork");
        check(EyeData.id(result) == null, "recipe template has no shared UUID");
        ItemStack second = recipe.assemble(input);
        check(!EyeData.ensureId(result).equals(EyeData.ensureId(second)), "separate recipe outputs gain unique identities");
        check(EyeData.target(result) == null && EyeData.target(second) == null, "crafted Eyes are unbound");
        result.setCount(0);
        check(recipe.assemble(input).getCount() == 1, "each craft creates an independent output stack");
        check(!recipe.isSpecial() && !recipe.display().isEmpty(), "recipe appears in the vanilla recipe book");
        var display = (ShapedCraftingRecipeDisplay) recipe.display().getFirst();
        check(display.width() == 3 && display.height() == 3 && display.ingredients().size() == 9, "book displays full crafting grid");
        ItemStack bookResult = ((SlotDisplay.ItemStackSlotDisplay) display.result()).stack().create();
        check(ItemStack.matches(bookResult, template()), "vanilla book display preserves all Eye components");
        Advancement advancement = Advancement.CODEC.parse(ops,
                resource("data/hunterseye/advancement/recipes/equipment/hunters_eye.json")).getOrThrow();
        check(advancement.rewards().recipes().size() == 1
                && advancement.rewards().recipes().getFirst().identifier().toString().equals("hunterseye:hunters_eye"), "advancement unlocks shipped recipe");
        var pearl = (InventoryChangeTrigger.TriggerInstance) advancement.criteria().get("has_ingredient").triggerInstance();
        check(pearl.matches(null, new ItemStack(Items.ENDER_EYE), 1, 0, 0), "acquiring an Ender Eye meets unlock criterion");
        check(!pearl.matches(null, new ItemStack(Items.DIAMOND), 1, 0, 0), "unrelated ingredient does not unlock recipe");
        check(advancement.display().isEmpty(), "recipe unlock has no advancement toast or tab");
        System.out.println("Hunter's Eye recipe verification: " + checks + " checks passed.");
    }

    private static ItemStack template() {
        ItemStack stack = EyeData.createEye();
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove("hunterseye_eye_id"));
        return stack;
    }

    private static JsonElement resource(String name) throws Exception {
        try (var stream = RecipeVerification.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new AssertionError("Missing packaged resource: " + name);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
