package xyz.lineage.game;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.Lineage;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.net.ChronicleNetwork;
import xyz.lineage.net.SyncSoulPayload;
import xyz.lineage.registry.SoulAttachments;
import xyz.lineage.stats.FacetEngine;
import xyz.lineage.trait.Trait;

/**
 * Single place that (re)forges a player's blood: strips the prior lineage,
 * applies facets/attributes/traits and syncs the ledger. Used by the oath-book
 * and by admin commands so behaviour never drifts between the two paths.
 */
public final class LineageRites {
    private LineageRites() {
    }

    /**
     * @param giveKit when true, lineage provisions are bestowed (inventory or drop).
     * @return the lineage actually sworn, or null when the player has no ledger.
     */
    public static Lineage apply(ServerPlayer player, ResourceLocation lineageId, boolean giveKit) {
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        if (ledger == null) {
            return null;
        }
        Lineage lineage = LineageCatalog.find(lineageId);
        if (lineage == null) {
            lineage = LineageCatalog.resolve(lineageId, ledger.gambleSeed());
        }
        if (lineage == null) {
            lineage = LineageCatalog.first();
        }
        if (lineage == null) {
            return null;
        }

        // Strip the old blood before the new oath lands.
        if (ledger.lineageId() != null) {
            Lineage prior = LineageCatalog.resolve(ledger.lineageId(), ledger.gambleSeed());
            if (prior != null) {
                for (Trait trait : prior.traits()) {
                    trait.stripped(player);
                }
            }
        }
        FacetEngine.stripAll(player);

        // Fresh slate for the new oath; gambler re-rolls below.
        ledger.gambleSeed(0L);
        ledger.wardAt(0L);

        if (LineageCatalog.GAMBLER.equals(lineage.id())) {
            long seed = player.getRandom().nextLong();
            if (seed == 0L) {
                seed = 1L;
            }
            ledger.gambleSeed(seed);
            lineage = LineageCatalog.castFate(RandomSource.create(seed), LineageCatalog.gambleFor(player.getUUID()));
            LineageCatalog.swear(lineage);
        } else if (LineageCatalog.isWaywardGamble(lineage.id())) {
            long seed = player.getRandom().nextLong();
            if (seed == 0L) {
                seed = 1L;
            }
            ledger.gambleSeed(seed);
            lineage = LineageCatalog.castFate(RandomSource.create(seed), lineage.id());
            LineageCatalog.swear(lineage);
        }

        ledger.swearTo(lineage);

        if (giveKit) {
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
        }

        FacetEngine.dress(player);
        for (Trait trait : lineage.traits()) {
            trait.worn(player);
        }
        ChronicleNetwork.send(player, new SyncSoulPayload(ledger));
        return lineage;
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
}
