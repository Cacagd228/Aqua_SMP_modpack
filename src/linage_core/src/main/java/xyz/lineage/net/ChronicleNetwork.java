package xyz.lineage.net;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import xyz.lineage.LineageCore;
import xyz.lineage.client.SoulClientWhispers;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.registry.SoulAttachments;
import xyz.lineage.stats.FacetEngine;
import xyz.lineage.trait.Trait;

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
            if (ledger.sworn()) {
                player.sendSystemMessage(Component.translatable("gui." + LineageCore.MOD_ID + ".already_sworn"));
                // Echo the ledger so the oath-book never dangles in "Swearing...".
                send(player, new SyncSoulPayload(ledger));
                return;
            }
            Lineage lineage = LineageCatalog.find(payload.lineage());
            if (lineage == null) {
                lineage = LineageCatalog.first();
            }
            if (lineage != null && LineageCatalog.GAMBLER.equals(lineage.id())) {
                long seed = player.getRandom().nextLong();
                if (seed == 0L) {
                    seed = 1L;
                }
                ledger.gambleSeed(seed);
                lineage = LineageCatalog.castFate(RandomSource.create(seed), LineageCatalog.gambleFor(player.getUUID()));
                LineageCatalog.swear(lineage);
            }
            FacetEngine.stripAll(player);
            ledger.swearTo(lineage);
            bestow(player, EquipmentSlot.HEAD, lineage.plateFor(player, EquipmentSlot.HEAD));
            bestow(player, EquipmentSlot.CHEST, lineage.plateFor(player, EquipmentSlot.CHEST));
            bestow(player, EquipmentSlot.LEGS, lineage.plateFor(player, EquipmentSlot.LEGS));
            bestow(player, EquipmentSlot.FEET, lineage.plateFor(player, EquipmentSlot.FEET));
            bestow(player, EquipmentSlot.OFFHAND, lineage.handOff());
            bestow(player, EquipmentSlot.MAINHAND, lineage.handMain());
            for (ItemStack spare : lineage.satchel()) {
                if (!spare.isEmpty()) {
                    ItemStack twin = spare.copy();
                    if (!player.getInventory().add(twin)) {
                        player.drop(twin, false);
                    }
                }
            }
            player.inventoryMenu.broadcastChanges();
            FacetEngine.dress(player);
            for (Trait trait : lineage.traits()) {
                trait.worn(player);
            }
            send(player, new SyncSoulPayload(ledger));
        });
    }

    private static void bestow(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ItemStack twin = stack.copy();
        if (player.getItemBySlot(slot).isEmpty()) {
            player.setItemSlot(slot, twin);
        } else if (!player.getInventory().add(twin)) {
            player.drop(twin, false);
        }
    }

    public static void send(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
