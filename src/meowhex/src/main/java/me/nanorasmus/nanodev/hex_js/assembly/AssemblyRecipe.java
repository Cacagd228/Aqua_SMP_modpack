package me.nanorasmus.nanodev.hex_js.assembly;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * One sequenced-assembly recipe: a workpiece ingredient, the <em>full</em>
 * ordered list of steps that turns it into the output, the aether it must have
 * accumulated by the end, and the output itself.
 *
 * <p>This mirrors Create's Sequenced Assembly rather than Ovid's Distillation:
 * a rune does not know an outcome, it only appends <em>a step id</em>, and the
 * recipe decides whether that step was legal. There is no per-rune outcome
 * table — four different runes writing four different ids into the same list is
 * the whole mechanic, and it is what lets a pack author invent new recipes
 * without writing any Java.
 *
 * @param input  the starting workpiece, matched as an {@link Ingredient}
 * @param steps  the complete sequence; must match exactly, never as a prefix
 * @param mana   required aether in <em>mana</em> units; {@code 0} = no aether
 * @param output the finished product
 */
public record AssemblyRecipe(Ingredient input, List<String> steps, long mana, ItemStack output)
        implements Recipe<AssemblyRecipe.InputHolder> {

    /**
     * A workpiece plus the aether already on it, so a recipe can be matched
     * through the plain {@code Recipe} interface instead of a hand-rolled
     * helper.
     *
     * <p>Empty, not a real inventory: these recipes are never driven by a
     * crafting grid, so {@code RecipeInput} is satisfied only nominally.
     */
    public record InputHolder(ItemStack workpiece, long mana) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? workpiece : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    @Override
    public boolean matches(InputHolder holder, Level level) {
        if (holder == null || holder.workpiece() == null || holder.workpiece().isEmpty()) {
            return false;
        }
        return input.test(holder.workpiece());
    }

    @Override
    public ItemStack assemble(InputHolder holder, HolderLookup.Provider registries) {
        ItemStack out = output.copy();
        out.setCount(output.getCount() * Math.max(1, holder.workpiece().getCount()));
        return out;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return AssemblyRecipes.TYPE;
    }

    /** True if {@code prefix} is this recipe's sequence with steps still missing. */
    public boolean isPrefixOf(List<String> prefix) {
        if (prefix.size() >= steps.size()) {
            return false;
        }
        return steps.subList(0, prefix.size()).equals(prefix);
    }

    public static final RecipeSerializer<AssemblyRecipe> SERIALIZER = new Serializer();

    /**
     * Data-pack form. The KubeJS bridge writes exactly this shape, so a script
     * and a hand-written JSON file are interchangeable.
     *
     * <p>Field names are {@code input}, {@code steps}, {@code mana},
     * {@code output}. {@code steps} also accepts a bare string, so the common
     * one-step recipe reads as {@code "steps": "merge"} instead of a one-element
     * list.
     *
     * <p>That shorthand is a datapack-only convenience. The KubeJS schema
     * declares {@code steps} as a plain list and cannot express the
     * alternative, so a script must write {@code steps: ['merge']} — which is
     * what the tests do, and what a script author should copy.
     */
    public static class Serializer implements RecipeSerializer<AssemblyRecipe> {
        /**
         * {@code steps} accepts a bare string or a list, so the common
         * single-step recipe reads as {@code "steps": "merge"} instead of
         * {@code "steps": ["merge"]}.
         *
         * <p>withAlternative tries the list codec first and falls back to
         * wrapping a single string in a one-element list.
         *
         * <p>The fallback has to be the <em>three</em>-argument overload, whose
         * last argument builds the {@code T} from the alternative's {@code U}.
         * The two-argument overload needs both branches to already produce
         * {@code List<String>}, and the obvious way to spell that — passing
         * {@code Codec.STRING.listOf()} — is a no-op: {@code listOf()} is
         * defined as {@code return list(this);}, so the fallback is the same
         * list codec as the primary and a bare string never parses. It fails
         * as "Failed to parse either. First: Not a json array; Second: Not a
         * json array", which reads like a JSON problem rather than a no-op
         * codec.
         */
        private static final Codec<List<String>> STEPS_CODEC = Codec.withAlternative(
                Codec.STRING.listOf(),
                Codec.STRING,
                List::of
        );

        private static final MapCodec<AssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC.fieldOf("input").forGetter(AssemblyRecipe::input),
                        STEPS_CODEC.fieldOf("steps").forGetter(AssemblyRecipe::steps),
                        Codec.LONG.optionalFieldOf("mana", 0L).forGetter(AssemblyRecipe::mana),
                        ItemStack.CODEC.fieldOf("output").forGetter(AssemblyRecipe::output)
                ).apply(instance, AssemblyRecipe::new));

        @Override
        public MapCodec<AssemblyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> streamCodec() {
            return StreamCodec.composite(
                    Ingredient.CONTENTS_STREAM_CODEC, AssemblyRecipe::input,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AssemblyRecipe::steps,
                    ByteBufCodecs.VAR_LONG, AssemblyRecipe::mana,
                    // Not ItemStack.CODEC: that is the JSON codec. Over the
                    // network an item stack is its own length-prefixed field.
                    ItemStack.STREAM_CODEC, AssemblyRecipe::output,
                    AssemblyRecipe::new
            );
        }
    }
}
