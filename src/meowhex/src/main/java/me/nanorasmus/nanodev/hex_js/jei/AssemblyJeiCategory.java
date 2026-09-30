package me.nanorasmus.nanodev.hex_js.jei;

import me.nanorasmus.nanodev.hex_js.addon.scroll.ScrollItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * JEI page for sequenced-assembly recipes.
 *
 * <p>Layout: workpiece on the left, the ordered step list as a vertical column
 * of scroll glyphs in the middle, the result on the right. The step column is
 * the point of the page — an assembly recipe's identity <em>is</em> its step
 * order, and a JEI page that only showed input and output would render three
 * different recipes that happen to share an input as one indistinguishable
 * entry.
 *
 * <p>The steps are drawn as RENDER_ONLY slots rather than plain text so each one
 * is hoverable and lists its page, and so the column reads as items rather than
 * as a debug dump of string ids.
 */
public class AssemblyJeiCategory implements IRecipeCategory<AssemblyJeiRecipe> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath("meowhex", "assembly");
    public static final RecipeType<AssemblyJeiRecipe> TYPE =
            new RecipeType<>(UID, AssemblyJeiRecipe.class);
    public static final Component TITLE = Component.translatable("jei.meowhex.assembly");

    private static final int BG_W = 150;
    private static final int ROW_H = 22;
    private static final int INPUT_X = 8;
    private static final int OUTPUT_X = 118;
    private static final int STEP_X = 45;
    /** Top padding above the first step row. */
    private static final int PAD_TOP = 6;
    /**
     * Sized for the longest possible sequence (all five step ids), plus a line
     * for the aether note.
     *
     * <p>JEI's {@code getHeight()} takes no recipe, so the page cannot grow
     * per entry. Rather than clip a long sequence, the column is drawn from the
     * top and the workpiece/result slots are centred against <em>this</em>
     * recipe's own step count in {@link #setRecipe} — a one-step recipe reads
     * as a compact row with empty space under it rather than as a clipped one.
     */
    private static final int BG_H = PAD_TOP + 5 * ROW_H + 16;

    /** No frame: the page is a bare column of slots, so the background is a no-op drawable. */
    private static final IDrawable NO_BACKGROUND = new IDrawable() {
        @Override
        public int getWidth() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public void draw(GuiGraphics g, int x, int y) {
            // deliberately nothing: getWidth/getHeight supply the page size
        }
    };

    private final IDrawable icon;

    public AssemblyJeiCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.CRAFTING_TABLE));
    }

    /**
     * A no-op drawable: the page is a bare column of slots. Deprecated by JEI in
     * favour of getWidth/getHeight, but still honoured for the drawn area.
     */
    @SuppressWarnings("removal")
    @Override
    public IDrawable getBackground() {
        return NO_BACKGROUND;
    }

    @Override
    public int getWidth() {
        return BG_W;
    }

    @Override
    public int getHeight() {
        return BG_H;
    }

    @Override
    public RecipeType<AssemblyJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblyJeiRecipe recipe, IFocusGroup focuses) {
        if (recipe.inputs().isEmpty()) {
            // An ingredient matching nothing has no slot to draw; returning
            // early keeps the layout from collapsing rather than making JEI
            // error out on an empty ingredient slot.
            return;
        }

        int steps = Math.max(1, recipe.steps().size());
        int columnH = PAD_TOP + steps * ROW_H;
        // Centre the workpiece and the result against this recipe's own column
        // rather than the fixed page height.
        int sideY = Math.max(2, (columnH - 18) / 2);

        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, sideY)
                .addIngredients(VanillaTypes.ITEM_STACK, recipe.inputs());

        for (int i = 0; i < steps; i++) {
            String step = recipe.steps().get(i);
            ItemStack glyph = glyphFor(step);
            if (glyph.isEmpty()) {
                continue;
            }
            // The ordinal is drawn in the gutter by draw(); the glyph is the
            // scroll that teaches the step, and its tooltip names the rune.
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, STEP_X, PAD_TOP + i * ROW_H)
                    .addItemStack(glyph)
                    .addTooltipCallback((view, tooltip) ->
                            tooltip.add(Component.translatable("meowhex.jei.step." + step)));
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, sideY)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(AssemblyJeiRecipe recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        for (int i = 0; i < recipe.steps().size(); i++) {
            // The ordinal, in the gutter left of the glyph.
            g.drawString(font, String.valueOf(i + 1), STEP_X - 12,
                    PAD_TOP + i * ROW_H + 5, 0xFF404040, false);
        }

        int steps = Math.max(1, recipe.steps().size());
        if (recipe.mana() > 0) {
            Component need = Component.translatable("meowhex.jei.mana", recipe.mana());
            g.drawString(font, need, INPUT_X, PAD_TOP + steps * ROW_H + 2, 0xFF3F5FD0, false);
        }
    }

    /**
     * The scroll item that teaches this step, used as the glyph in the column.
     *
     * <p>Empty when the scroll is missing — the layout skips the slot rather
     * than showing an air gap, and the number still labels the position.
     */
    private static ItemStack glyphFor(String step) {
        String opId = switch (step) {
            case "merge" -> "merge_entities";
            case "absorb" -> "absorb_gifts";
            case "purify" -> "purify_essence";
            case "sacrifice" -> "draw_sacrifice";
            case "mana" -> "infuse_aether";
            default -> null;
        };
        if (opId == null) {
            return ItemStack.EMPTY;
        }
        var holder = ScrollItems.BY_ID.get("scroll_loki_" + opId);
        if (holder == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(holder.get());
    }
}
