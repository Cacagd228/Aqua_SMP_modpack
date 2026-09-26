package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import xyz.lineage.LineageCore;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.lineage.LineageCatalog;
import xyz.lineage.registry.SoulAttachments;

/**
 * The ascended mind passes unnoticed: hostile mobs never pick it as a target
 * unless it strikes them first. Fresh acquisitions are refused at the event
 * gate ({@code SeasonedPlayerWatcher}); this pulse sweeps stalkers that
 * locked on through other means. Bosses are exempt so progression fights
 * keep working.
 */
public final class UnseenPresence implements Trait {
    /** How long after hurting a mob it may still answer back. */
    private static final int RETALIATION_TICKS = 100;
    private static final double SWEEP_RADIUS = 16.0;

    private final ResourceLocation sigil;

    public UnseenPresence(String name) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
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
    public void pulse(ServerPlayer player) {
        if (player.tickCount % 10 != 0 || !isUnseen(player)) {
            return;
        }
        for (Mob mob : player.serverLevel().getEntitiesOfClass(Mob.class,
            player.getBoundingBox().inflate(SWEEP_RADIUS),
            mob -> mob.getTarget() == player && pacifiable(mob, player))) {
            mob.setTarget(null);
        }
    }

    /** True while the player wears ascended aether blood. */
    public static boolean isUnseen(ServerPlayer player) {
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        return ledger != null && ledger.sworn() && LineageCatalog.ARCH_AETHER.equals(ledger.lineageId());
    }

    /** Bosses keep their quarrels; everyone else may be refused. */
    public static boolean pacifiable(Mob mob, ServerPlayer player) {
        if (mob.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)) {
            return false;
        }
        return !retaliating(mob, player);
    }

    /** A mob answering a fresh wound is never hushed. */
    public static boolean retaliating(Mob mob, ServerPlayer player) {
        return mob.getLastHurtByMob() == player
            && mob.tickCount - mob.getLastHurtByMobTimestamp() < RETALIATION_TICKS;
    }
}
