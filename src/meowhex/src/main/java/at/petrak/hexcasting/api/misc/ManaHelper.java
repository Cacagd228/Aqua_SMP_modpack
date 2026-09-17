package at.petrak.hexcasting.api.misc;

import at.petrak.hexcasting.common.lib.HexAttributes;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The player mana pool. Replaces drawing media from the inventory.
 * <p>
 * Conversion: 1 dust (10_000 media) = 10 mana.
 * Mana ranges from 0 to the player's {@code MANA_MAX} attribute (default {@link #DEFAULT_MAX_MANA});
 * there is no overcast, spells you can't afford simply don't fire. The {@code MANA_DISCOUNT}
 * attribute reduces the mana cost. The actual storage is a synced NeoForge attachment living in
 * the platform layer.
 */
public final class ManaHelper {
    public static final double MIN_MANA = 0.0;
    public static final double DEFAULT_MAX_MANA = 200.0;
    /** Hard ceiling for the synced storage / codec. */
    public static final double STORAGE_MAX_MANA = 1_000_000.0;
    /** How many media units in one mana (1 dust = 10 mana). */
    public static final double MEDIA_PER_MANA = MediaConstants.DUST_UNIT / 10.0;
    /** Ticks per mana-regen "point" (the attribute is mana per 6s). */
    public static final double REGEN_PERIOD_TICKS = 120.0;
    /** Regen is suspended this many ticks after the last mana spend. */
    public static final int REGEN_DELAY_TICKS = 40;

    private static final String TAG_PREV_MANA = "meowhex_prev_mana";
    private static final String TAG_REGEN_PAUSE_UNTIL = "meowhex_regen_pause_until";

    private ManaHelper() {}

    public static double getMana(Player player) {
        return IXplatAbstractions.INSTANCE.getMana(player);
    }

    public static double maxMana(Player player) {
        return player.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_MAX));
    }

    public static void setMana(Player player, double mana) {
        IXplatAbstractions.INSTANCE.setMana(player, Mth.clamp(mana, MIN_MANA, maxMana(player)));
    }

    /** Mana cost of the given media, after the player's discount is applied (rounded up). */
    public static double manaCostOfMedia(Player player, long media) {
        double cost = media / MEDIA_PER_MANA;
        double discount = player.getAttributeValue(
            BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_DISCOUNT));
        return Math.max(0.0, cost * (1.0 - Mth.clamp(discount, 0.0, 1.0)));
    }

    public static double manaCostOfMedia(long media) {
        return media / MEDIA_PER_MANA;
    }

    /** Wearing a creative unlocker grants infinite mana (casting is free). */
    public static boolean hasInfiniteMana(Player player) {
        return player.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_INFINITE)) > 0.0;
    }

    public static void tickManaRegen(ServerPlayer player) {
        var max = maxMana(player);
        var mana = getMana(player);
        if (mana > max) {
            // The pool shrank below the current mana (e.g. unequipping a bauble that
            // raised MANA_MAX). Stored mana must be clamped, not left floating above
            // the cap, or the player keeps mana they no longer have room for.
            setMana(player, max);
            mana = max;
        }
        // Any mana spend (cast, deception upkeep, mana-break drain, ...) pauses
        // regen for REGEN_DELAY_TICKS, tracked by comparing with the previous tick.
        var pd = player.getPersistentData();
        var prevMana = pd.getDouble(TAG_PREV_MANA);
        if (mana < prevMana) {
            pd.putLong(TAG_REGEN_PAUSE_UNTIL, player.level().getGameTime() + REGEN_DELAY_TICKS);
        }
        pd.putDouble(TAG_PREV_MANA, mana);
        if (mana >= max) {
            return;
        }
        if (player.level().getGameTime() < pd.getLong(TAG_REGEN_PAUSE_UNTIL)) {
            return;
        }
        var rate = player.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_REGEN));
        if (rate <= 0.0) {
            return;
        }
        setMana(player, mana + rate / REGEN_PERIOD_TICKS);
    }
}
