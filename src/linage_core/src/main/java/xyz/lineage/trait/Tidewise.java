package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import xyz.lineage.LineageCore;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.net.ChronicleNetwork;
import xyz.lineage.net.SyncSoulPayload;
import xyz.lineage.registry.SoulAttachments;
import xyz.lineage.stats.FacetEngine;
import xyz.lineage.stats.HeroStat;

/**
 * Children of the tide: clumsy on shingle, graceful in the deep,
 * and tireless delvers beneath the waves.
 */
public final class Tidewise implements Trait {
    private final ResourceLocation sigil;
    private final ResourceLocation heft;

    public Tidewise(String name) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
        this.heft = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name + "_heft");
    }

    @Override
    public ResourceLocation sigil() {
        return sigil;
    }

    @Override
    public Component title() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath());
    }

    @Override
    public Component lore() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath() + ".desc");
    }

    @Override
    public void stripped(ServerPlayer player) {
        AttributeInstance dig = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (dig != null) {
            dig.removeModifier(heft);
        }
    }

    @Override
    public void pulse(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        attuneGrace(player);
        AttributeInstance dig = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        if (dig != null) {
            boolean wet = player.isUnderWater();
            if (wet && dig.getModifier(heft) == null) {
                dig.addTransientModifier(new AttributeModifier(heft, 4.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (!wet) {
                dig.removeModifier(heft);
            }
        }
    }

    private static void attuneGrace(ServerPlayer player) {
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        if (ledger == null || !ledger.sworn()) {
            return;
        }
        int want = player.isInWater() ? 16 : 4;
        if (ledger.facet(HeroStat.AGILITY) != want) {
            ledger.facet(HeroStat.AGILITY, want);
            FacetEngine.dress(player);
            ChronicleNetwork.send(player, new SyncSoulPayload(ledger));
        }
    }

    @Override
    public boolean fateful() {
        return false;
    }
}
