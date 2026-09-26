package dev.hexsable.scroll;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Серверный гейт каста для рун, закрытых свитками.
 *
 * <p>Каждый закрытый op id отображается в один или несколько
 * {@code hexsable:scrolls/*} advancement ({@link SableScrollDefs#OP_TO_ADVANCEMENTS});
 * каст разрешён, когда игрок выполнил хотя бы один из них. Op id вне карты
 * (базовые руны и всегда открытые страницы) здесь никогда не блокируются.
 *
 * <p>По образцу {@code me.nanorasmus.nanodev.hex_js.addon.scroll.ScrollGate}.
 */
public final class SableScrollGate {
    private SableScrollGate() {
    }

    public static boolean canCast(ServerPlayer player, ResourceLocation opId) {
        List<String> advancementIds = SableScrollDefs.OP_TO_ADVANCEMENTS.get(opId.toString());
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
