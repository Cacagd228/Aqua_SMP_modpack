package me.nanorasmus.nanodev.hex_js.addon.interop;

import at.petrak.hexcasting.api.misc.ManaHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * {@code meowhex:mana_break} — a sword enchantment (max level III) that drains a
 * fraction of the victim's {@linkplain ManaHelper maximum mana} on each melee hit.
 *
 * <p>Drain is a percent of the victim's <b>max</b> mana: 2.75% / 4% / 6% at
 * levels I / II / III, applied before the damage lands. Only players have a mana
 * pool in this mod ({@code ManaHelper} is player-bound), so the effect simply does
 * nothing against mana-less mobs. Targets with infinite mana are untouched.
 */
public final class ManaBreakHandler {
    public static final ResourceLocation MANA_BREAK_ID =
            ResourceLocation.fromNamespaceAndPath("meowhex", "mana_break");

    /** Fraction of the victim's max mana drained per hit, indexed by enchant level. */
    public static final double[] DRAIN_PER_LEVEL = {0.0, 0.0275, 0.04, 0.06};

    private ManaBreakHandler() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) {
            return;
        }
        if (!(victim instanceof Player target)) {
            return;
        }
        if (ManaHelper.hasInfiniteMana(target)) {
            return;
        }
        Entity attacker = event.getSource().getDirectEntity();
        if (!(attacker instanceof Player attackerPlayer)) {
            return;
        }
        ItemStack stack = attackerPlayer.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        var lookup = victim.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> manaBreak = lookup
                .get(ResourceKey.create(Registries.ENCHANTMENT, MANA_BREAK_ID))
                .orElse(null);
        if (manaBreak == null) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(manaBreak, stack);
        if (level <= 0 || level >= DRAIN_PER_LEVEL.length) {
            return;
        }
        double drain = ManaHelper.maxMana(target) * DRAIN_PER_LEVEL[level];
        if (drain <= 0.0) {
            return;
        }
        ManaHelper.setMana(target, ManaHelper.getMana(target) - drain);
    }
}