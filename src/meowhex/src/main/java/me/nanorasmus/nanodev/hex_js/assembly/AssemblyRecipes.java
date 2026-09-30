package me.nanorasmus.nanodev.hex_js.assembly;

import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.casting.AssemblyMishap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The assembly recipe registry, and the rule that decides whether a step is
 * allowed.
 *
 * <p>Recipes are backed by the real {@link Registries#RECIPE} type, so
 * datapack JSON and KubeJS both land in the same list and survive a reload;
 * KubeJS is not a second, parallel registry the way
 * {@code OvidRecipeRegistry} is.
 *
 * <p><b>The prefix rule.</b> When a rune wants to append step {@code s} to a
 * workpiece that already has {@code existing}, the new sequence
 * {@code existing + [s]} must either
 * <ul>
 *   <li>equal some recipe's full sequence — the assembly finishes, and that
 *       recipe produces its output, or</li>
 *   <li>be a strict prefix of some recipe's sequence — the step is
 *       intermediate and the workpiece keeps going.</li>
 * </ul>
 * Anything else is rejected as a mishap and the item is left untouched. This is
 * what makes the recipe the driver of the process the way Create's is: a hammer
 * will not swing at a workpiece unless that swing means something for some
 * recipe. It also means the four activator runes cannot be used to scramble a
 * sequence into a different order, and it turns a confusing silent corruption
 * into a comprehensible error.
 */
public final class AssemblyRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, HexJS.MOD_ID);

    public static final RecipeType<AssemblyRecipe> TYPE =
            new RecipeType<AssemblyRecipe>() {
                @Override
                public String toString() {
                    return HexJS.modLoc("assembly").toString();
                }
            };

    static {
        SERIALIZERS.register("assembly", () -> AssemblyRecipe.SERIALIZER);
    }

    /**
     * Cached view of every assembly recipe, refreshed from the recipe manager on
     * every datapack reload.
     */
    private static volatile List<AssemblyRecipe> cached = List.of();

    private AssemblyRecipes() {
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    public static void init(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    /**
     * Re-read the recipe list. Called on {@code RecipesUpdatedEvent} so a
     * {@code /reload} is enough to pick up KubeJS changes; without it the first
     * cast after a reload would see the previous recipe set.
     *
     * @param manager the recipe manager handed over by the reload
     */
    public static void reload(net.minecraft.world.item.crafting.RecipeManager manager) {
        List<AssemblyRecipe> found = new ArrayList<>();
        // 1.21 hands out RecipeHolder wrappers, not the recipes themselves.
        for (var holder : manager.getRecipes()) {
            if (holder.value() instanceof AssemblyRecipe assembly) {
                found.add(assembly);
            }
        }
        // Longest first. When one step completes two recipes at once, the longer
        // sequence is the one the player was working towards; a shorter recipe
        // that merely shares a prefix must not short-circuit it.
        found.sort((a, b) -> a.steps().size() != b.steps().size()
                ? Integer.compare(b.steps().size(), a.steps().size())
                : a.steps().toString().compareTo(b.steps().toString()));
        cached = Collections.unmodifiableList(found);
    }

    public static List<AssemblyRecipe> all() {
        return cached;
    }

    /**
     * The outcome of trying to append one step to a workpiece.
     *
     * @param recipe   the finished recipe, when {@code finished}
     * @param finished whether this step completed a sequence
     */
    public record Advance(AssemblyRecipe recipe, boolean finished) {
    }

    /**
     * Decide what appending {@code step} to {@code existing} would do, or
     * explain why it cannot be done.
     *
     * <p>A pure decision: nothing is written, so the caller can raise the
     * mishap before any item is consumed.
     *
     * <p>Raises {@link AssemblyMishap.BadStep} if the new sequence leads
     * nowhere. {@code Mishap} is a checked {@code Throwable}, but it is thrown
     * through the same sneaky-throw helper the casting ops use — an assembly
     * rejection is an expected outcome of a cast, not a condition every caller
     * should have to wrap.
     */
    public static Advance advance(ItemStack workpiece,
                                  List<String> existing,
                                  String step) {
        if (!AssemblySteps.isKnown(step)) {
            sneakyThrow(new AssemblyMishap.BadStep(step, existing, List.of()));
        }

        List<String> next = AssemblyNbt.append(existing, step);
        AssemblyRecipe completed = null;
        boolean onTrack = false;

        for (AssemblyRecipe recipe : cached) {
            if (!recipe.input().test(workpiece)) {
                continue;
            }
            if (recipe.steps().equals(next)) {
                // Finishing. Keep looking: a longer recipe may also want this
                // prefix, and a worker part way through a longer sequence should
                // not be short-circuited by a shorter recipe that shares a start.
                if (completed == null || recipe.steps().size() > completed.steps().size()) {
                    completed = recipe;
                }
                onTrack = true;
            } else if (recipe.isPrefixOf(next)) {
                onTrack = true;
            }
        }

        if (!onTrack) {
            sneakyThrow(new AssemblyMishap.BadStep(step, existing, next));
        }
        return new Advance(completed, completed != null);
    }

    /** Every recipe starting with the given input; used by the aether rune. */
    public static List<AssemblyRecipe> forInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        return cached.stream().filter(r -> r.input().test(stack)).toList();
    }

    /**
     * The longest recipe in the cache.
     *
     * <p>Nothing calls this. The JEI page builds its own display records from
     * the client's own recipe manager rather than from this cache, so this is
     * only here as a debugging handle for "is the server-side cache populated
     * at all" — prefer {@link #all()} and delete this if it stays unused.
     */
    public static Optional<AssemblyRecipe> first() {
        return cached.isEmpty() ? Optional.empty() : Optional.of(cached.get(0));
    }

    static ResourceLocation id(String path) {
        return HexJS.modLoc(path);
    }
}
