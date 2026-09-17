package me.nanorasmus.nanodev.hex_js.jei;

import at.petrak.hexcasting.common.lib.HexItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class OvidJeiCategory implements IRecipeCategory<OvidJeiRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("meowhex", "ovids_distillation");
    public static final RecipeType<OvidJeiRecipe> TYPE = new RecipeType<>(UID, OvidJeiRecipe.class);
    public static final Component TITLE = Component.translatable("jei.meowhex.ovids_distillation");

    private final IDrawable background;
    private final IDrawable icon;

    public OvidJeiCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(120, 40);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(HexItems.ARTIFACT)); // fallback, will be blast furnace below
        // Try blast furnace as icon if available
    }

    @Override
    public RecipeType<OvidJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OvidJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 12)
                .addIngredients(VanillaTypes.ITEM_STACK, java.util.List.of(recipe.inputA()));
        builder.addSlot(RecipeIngredientRole.INPUT, 36, 12)
                .addIngredients(VanillaTypes.ITEM_STACK, java.util.List.of(recipe.inputB()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 12)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(OvidJeiRecipe recipe, IRecipeSlotsView view, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Arrow
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font, "→", 66, 16, 0xFF404040, false);
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font, "+", 26, 16, 0xFF404040, false);
    }
}
