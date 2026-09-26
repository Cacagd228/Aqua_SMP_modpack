package xyz.lineage.game;

import net.minecraft.server.level.ServerPlayer;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.registry.SoulAttachments;

/**
 * Integration point for the aether ascension rite.
 * <p>
 * The condition that earns ascension lives in another mod (quest, mana
 * milestone, ...). That mod only needs this class: {@link #isReady} to test
 * the vessel and {@link #ascend} to reforge it. Both are plain static calls,
 * no packets needed. The {@code /lineage ascend} command is the same rite
 * for consoles, command blocks, functions and KubeJS.
 */
public final class AetherAscension {
    private AetherAscension() {
    }

    /** True while the player wears unascended aether blood and may rise. */
    public static boolean isReady(ServerPlayer player) {
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        return ledger != null && ledger.sworn() && LineageCatalog.AETHER_MIND.equals(ledger.lineageId());
    }

    /** True while the player already wears the ascended form. */
    public static boolean isAscended(ServerPlayer player) {
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        return ledger != null && ledger.sworn() && LineageCatalog.ARCH_AETHER.equals(ledger.lineageId());
    }

    /**
     * Reforges an unascended aether mind into its ascended form.
     *
     * @return the ascended lineage, or null when the player is not ready
     */
    public static Lineage ascend(ServerPlayer player) {
        if (!isReady(player)) {
            return null;
        }
        return LineageRites.apply(player, LineageCatalog.ARCH_AETHER, true);
    }
}
