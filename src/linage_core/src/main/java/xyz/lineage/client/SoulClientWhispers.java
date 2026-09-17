package xyz.lineage.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.net.OpenChroniclePayload;
import xyz.lineage.net.SyncSoulPayload;
import xyz.lineage.registry.SoulAttachments;

/** Client-side whispers: apply server chronicles without touching server code. */
@OnlyIn(Dist.CLIENT)
public final class SoulClientWhispers {
    private SoulClientWhispers() {
    }

    public static void remember(SyncSoulPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            SoulLedger mine = player.getData(SoulAttachments.SOUL);
            mine.copyFrom(payload.ledger());
            if (LineageCatalog.isWaywardGamble(mine.lineageId())) {
                LineageCatalog.ensureWayward(mine.lineageId(), mine.gambleSeed());
            }
        });
    }

    public static void unfold(OpenChroniclePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var screen = switch (payload.kind()) {
                case OATH -> new OathCoverScreen(false);
                case ASCEND -> new OathCoverScreen(true);
                case PROFILE -> new ChronicleScreen(true, false);
            };
            Minecraft.getInstance().setScreen(screen);
        });
    }
}
