package xyz.lineage.net;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import xyz.lineage.LineageCore;
import xyz.lineage.client.SoulClientWhispers;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.game.LineageRites;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.registry.SoulAttachments;

public final class ChronicleNetwork {
    private ChronicleNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(ClaimLineagePayload.TYPE, ClaimLineagePayload.STREAM, ChronicleNetwork::swearOath);
        registrar.playToClient(SyncSoulPayload.TYPE, SyncSoulPayload.STREAM, (payload, context) -> {
            if (FMLEnvironment.dist.isClient()) {
                SoulClientWhispers.remember(payload, context);
            }
        });
        registrar.playToClient(OpenChroniclePayload.TYPE, OpenChroniclePayload.STREAM, (payload, context) -> {
            if (FMLEnvironment.dist.isClient()) {
                SoulClientWhispers.unfold(payload, context);
            }
        });
    }

    private static void swearOath(ClaimLineagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            SoulLedger ledger = player.getData(SoulAttachments.SOUL);
            Lineage requested = LineageCatalog.find(payload.lineage());
            if (requested == null) {
                requested = LineageCatalog.first();
            }
            // Ascendant blood (arch_*) may replace an already-sworn lineage;
            // anything else keeps the one-oath rule.
            boolean ascendant = requested != null && LineageCatalog.isAscendant(requested.id());
            if (ledger.sworn() && !ascendant) {
                player.sendSystemMessage(Component.translatable("gui." + LineageCore.MOD_ID + ".already_sworn"));
                // Echo the ledger so the oath-book never dangles in "Swearing...".
                send(player, new SyncSoulPayload(ledger));
                return;
            }
            if (requested == null) {
                send(player, new SyncSoulPayload(ledger));
                return;
            }
            LineageRites.apply(player, requested.id(), true);
        });
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
