package me.nanorasmus.nanodev.hex_js.assembly;

import net.minecraft.server.ReloadableServerResources;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Re-reads the assembly recipe list after every datapack reload, so a
 * {@code /reload} is enough for a KubeJS change to reach the runes.
 *
 * <p><b>Why this is a tick hook and not a reload listener.</b> NeoForge 21.1
 * has no server-side {@code RecipesUpdatedEvent} (only a client one), and
 * {@code AddReloadListenerEvent} carries the resources but fires before the
 * recipe manager has loaded anything. A {@code PreparableReloadListener} would
 * be the natural fit, but nothing promises our listener runs after the
 * vanilla recipe listener's, so it could easily fire early and cache an empty
 * list — a failure that would look like "my recipe does not work" rather than
 * like a load error.
 *
 * <p>Instead, remember which {@link ReloadableServerResources} instance the
 * cache was built from. That object is replaced on every reload, so an identity
 * comparison is an exact "has the datapack changed" test that needs no ordering
 * guarantee and no assumptions about the recipe manager's internals. It runs on
 * the next server tick after the reload, by which point the recipes are in.
 *
 * <p>Cost is one reference comparison per tick, and a full recipe rescan only
 * when that comparison says something actually changed.
 */
public final class AssemblyRecipeReloadListener {
    /** The resources the cache was built from; null until the first build. */
    private static ReloadableServerResources loaded;
    /** The resources the in-progress reload is building; null when up to date. */
    private static ReloadableServerResources pending;

    private AssemblyRecipeReloadListener() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(AssemblyRecipeReloadListener::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(AssemblyRecipeReloadListener::onServerTick);
    }

    private static void onAddReloadListener(AddReloadListenerEvent event) {
        // Only the pending marker: reading the recipe manager here would be
        // too early, and the tick below does the actual read.
        pending = event.getServerResources();
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        ReloadableServerResources current = pending;
        if (current == null || current == loaded) {
            return;
        }
        pending = null;
        loaded = current;
        AssemblyRecipes.reload(current.getRecipeManager());
    }
}
