package me.nanorasmus.nanodev.hex_js.addon.scroll;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Server-side cast gate for scroll-locked patterns.
 *
 * <p>Every gated op id maps to one or more {@code meowhex:scrolls/*}
 * advancements ({@link ScrollDefs#OP_TO_ADVANCEMENTS}); casting is allowed
 * when the player has completed at least one of them. Ops absent from the map
 * (base runes and always-open pages) are never blocked here.
 */
public final class ScrollGate {
    private ScrollGate() {
    }

    public static boolean canCast(ServerPlayer player, ResourceLocation opId) {
        List<String> advancementIds = ScrollDefs.OP_TO_ADVANCEMENTS.get(opId.toString());
        if (advancementIds == null || advancementIds.isEmpty()) {
            return true;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return true;
        }
        var advancementLookup = server.getAdvancements();
        var playerProgress = player.getAdvancements();
        for (String advancementId : advancementIds) {
            var holder = advancementLookup.get(ResourceLocation.parse(advancementId));
            if (holder != null && playerProgress.getOrStartProgress(holder).isDone()) {
                return true;
            }
        }
        return false;
    }
}
