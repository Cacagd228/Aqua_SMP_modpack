package xyz.lineage.client.veil;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.registry.SoulAttachments;

/** Gloom-veiled: those sworn to shadow wear dusk itself. */
public final class VeilRender {
    /** Dusk slate, mostly present. */
    public static final int VEIL_TINT = 0x9950505C;
    /** Pale aether glass worn by the ascended mind. */
    public static final int AETHER_TINT = 0x88A8C8E8;
    /** A held breath: crouching gloom all but vanishes. */
    public static final int VEIL_LOW_TINT = 0x0D50505C;

    private VeilRender() {
    }

    public static boolean veiled(LivingEntity entity) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        return ledger != null && (LineageCatalog.SHADOW.equals(ledger.lineageId())
            || LineageCatalog.ARCH_AETHER.equals(ledger.lineageId()));
    }

    public static boolean drape(LivingEntity entity) {
        return veiled(entity) && (!entity.isInvisible() || entity.isCrouching());
    }

    public static int hue(LivingEntity entity) {
        if (entity.isCrouching()) {
            return VEIL_LOW_TINT;
        }
        if (entity instanceof Player player) {
            SoulLedger ledger = player.getData(SoulAttachments.SOUL);
            if (ledger != null && LineageCatalog.ARCH_AETHER.equals(ledger.lineageId())) {
                return AETHER_TINT;
            }
        }
        return VEIL_TINT;
    }
}
