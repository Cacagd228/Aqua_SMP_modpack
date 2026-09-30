package me.nanorasmus.nanodev.hex_js.jei;

import me.nanorasmus.nanodev.hex_js.assembly.AssemblyGate;
import me.nanorasmus.nanodev.hex_js.assembly.AssemblyRecipe;
import me.nanorasmus.nanodev.hex_js.casting.OvidRecipeRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class HexJsJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath("meowhex", "jei_plugin");
    private static IJeiRuntime runtime;
    private static final List<OvidJeiRecipe> PENDING = new ArrayList<>();
    /**
     * How many assembly recipes are currently on the page.
     *
     * <p>JEI's {@code IRecipeManager} offers {@code addRecipes} but no removal,
     * so a re-sync cannot replace the page — it can only append. Counting what
     * was added lets the duplicate-guard on the next sync skip the whole
     * re-add when nothing actually changed, which is the common case: this
     * event fires on every {@code /reload} as well as on login.
     */
    private static int assemblyCount;
    /** The most recent recipe sync, kept so a late JEI runtime can catch up. */
    private static RecipeManager lastSynced;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();
        // The assembly page is gated so a disabled pack shows no page at all,
        // rather than an empty one that looks like "no recipes written yet".
        if (AssemblyGate.enabled()) {
            registration.addRecipeCategories(new AssemblyJeiCategory(helper));
        }
        registration.addRecipeCategories(new OvidJeiCategory(helper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<OvidJeiRecipe> recipes = new ArrayList<>();
        OvidRecipeRegistry.all().forEach((key, result) -> {
            ItemStack a = new ItemStack(key.a());
            ItemStack b = new ItemStack(key.b());
            ItemStack out = result.copy();
            recipes.add(new OvidJeiRecipe(a, b, out));
        });
        registration.addRecipes(OvidJeiCategory.TYPE, recipes);
        // NOT the assembly recipes: at registration time there is no level yet
        // (see onRecipesUpdated).
    }

    /**
     * Read the assembly recipes off the client's own recipe manager.
     *
     * <p>Deliberately not {@code AssemblyRecipes.all()}: that cache is filled
     * by a server-tick hook reading the <em>server's</em> recipe manager, so on
     * a client it stays empty and the JEI page would silently show nothing. The
     * client syncs the full recipe list anyway, so the honest source for a
     * display-only page is the client's manager.
     */
    private static List<AssemblyJeiRecipe> collectAssemblyRecipes(RecipeManager manager) {
        List<AssemblyJeiRecipe> out = new ArrayList<>();
        if (manager == null) {
            return out;
        }
        for (var holder : manager.getRecipes()) {
            if (holder.value() instanceof AssemblyRecipe recipe) {
                out.add(new AssemblyJeiRecipe(
                        resolveIngredient(recipe.input()),
                        List.copyOf(recipe.steps()),
                        recipe.mana(),
                        recipe.output().copy()));
            }
        }
        return out;
    }

    /**
     * Every stack an ingredient accepts, for one display slot.
     *
     * <p>Deliberately capped: an ingredient over a broad tag would otherwise
     * add hundreds of stacks to a single JEI slot, and the page only needs to
     * show representative examples.
     */
    private static List<ItemStack> resolveIngredient(Ingredient ingredient) {
        List<ItemStack> out = new ArrayList<>();
        if (ingredient == null) {
            return out;
        }
        for (ItemStack stack : ingredient.getItems()) {
            out.add(stack);
            if (out.size() >= 12) {
                break;
            }
        }
        return out;
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.BLAST_FURNACE), OvidJeiCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), OvidJeiCategory.TYPE);
        // The runes are cast with a staff, not crafted at a table; the scroll
        // is the closest thing to a "how do I learn this" catalyst, and the
        // crafting table keeps the page reachable from the recipe book.
        if (AssemblyGate.enabled()) {
            registration.addRecipeCatalyst(new ItemStack(Items.CRAFTING_TABLE), AssemblyJeiCategory.TYPE);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        if (!PENDING.isEmpty()) {
            runtime.getRecipeManager().addRecipes(OvidJeiCategory.TYPE, List.copyOf(PENDING));
            PENDING.clear();
        }
        // Normally JEI is far up by the time recipes sync, but if it were not,
        // this picks up the sync that already happened rather than leaving the
        // page blank for the rest of the session.
        if (assemblyCount == 0 && lastSynced != null) {
            onRecipesUpdated(lastSynced);
        }
    }

    /**
     * Feed the assembly page from a recipe sync.
     *
     * <p><b>Why this fires on the recipe sync and not on login.</b> Assembly
     * recipes cannot be registered in {@link #registerRecipes}: JEI calls that
     * during mod construction, long before any level exists. The obvious
     * replacement — {@code ClientPlayerNetworkEvent.LoggingIn} — is also wrong,
     * and quietly so. The server sends {@code ClientboundLoginPacket} before
     * {@code ClientboundUpdateRecipesPacket} ({@code PlayerList.placeNewPlayer}),
     * and the client handles them in that same order: {@code handleLogin} runs
     * the login event at its very end, while {@code handleUpdateRecipes} is what
     * actually calls {@code replaceRecipes}. So at login the recipe manager is
     * still the empty one from construction, and the page comes out blank with
     * no error anywhere.
     *
     * <p>{@code RecipesUpdatedEvent} is fired by that same
     * {@code handleUpdateRecipes}, immediately after {@code replaceRecipes}, so
     * it is the first moment the manager provably holds the synced recipes. It
     * also re-fires on {@code /reload}, which {@code LoggingIn} never does.
     *
     * <p><b>The duplicate limit.</b> JEI can add recipes but not remove them, so
     * a re-sync that genuinely changed the set would show the new recipes
     * alongside the stale ones until the next restart. Re-adding an identical
     * set is skipped by {@link #assemblyCount} so the common case — a reload
     * where nothing changed — stays clean.
     *
     * @param manager the client's recipe manager, already populated
     */
    public static void onRecipesUpdated(RecipeManager manager) {
        // Checked here as well as in registerCategories: this also fires on
        // /reload, so flipping the flag and reloading must not resurrect a page
        // whose category was never registered.
        if (!AssemblyGate.enabled()) {
            return;
        }
        lastSynced = manager;
        if (runtime == null) {
            return;
        }
        List<AssemblyJeiRecipe> recipes = collectAssemblyRecipes(manager);
        if (recipes.size() == assemblyCount) {
            return;
        }
        if (recipes.isEmpty()) {
            return;
        }
        runtime.getRecipeManager().addRecipes(AssemblyJeiCategory.TYPE, recipes);
        assemblyCount = recipes.size();
    }

    public static void addRecipeToJeiRuntime(ItemStack a, ItemStack b, ItemStack result) {
        OvidJeiRecipe recipe = new OvidJeiRecipe(a.copyWithCount(1), b.copyWithCount(1), result.copy());
        // Deduplicate: same inputs already pending or already in runtime
        for (OvidJeiRecipe r : PENDING) {
            if (ItemStack.isSameItemSameComponents(r.inputA(), recipe.inputA())
                    && ItemStack.isSameItemSameComponents(r.inputB(), recipe.inputB())
                    && ItemStack.isSameItemSameComponents(r.output(), recipe.output())) {
                return;
            }
        }
        if (runtime != null) {
            // Check already displayed (simple check via manager would require iteration; skip if duplicate inputs/output already exists)
            // For now, also check that we don't add duplicate of existing registry-snapped recipes already shown
            // We rely on OvidRecipeRegistry deduplication, but extra guard: query manager
            try {
                var existing = runtime.getRecipeManager().createRecipeLookup(OvidJeiCategory.TYPE).get().toList();
                for (Object o : existing) {
                    if (o instanceof OvidJeiRecipe r) {
                        if (ItemStack.isSameItemSameComponents(r.inputA(), recipe.inputA())
                                && ItemStack.isSameItemSameComponents(r.inputB(), recipe.inputB())
                                && ItemStack.isSameItemSameComponents(r.output(), recipe.output())) {
                            return;
                        }
                    }
                }
            } catch (Throwable ignored) {}
            runtime.getRecipeManager().addRecipes(OvidJeiCategory.TYPE, List.of(recipe));
        } else {
            PENDING.add(recipe);
        }
    }
}
