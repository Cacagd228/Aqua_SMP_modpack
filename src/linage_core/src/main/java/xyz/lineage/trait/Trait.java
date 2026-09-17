package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * A single inherited trait. Lineages compose behaviour out of
 * small reusable traits instead of one class per power.
 */
public interface Trait {
    ResourceLocation sigil();

    Component title();

    Component lore();

    default boolean burden() {
        return false;
    }

    default void worn(ServerPlayer player) {
    }

    default void stripped(ServerPlayer player) {
    }

    default void pulse(ServerPlayer player) {
    }

    default float sting(ServerPlayer player, DamageSource source, float amount) {
        return amount;
    }

    default void strike(ServerPlayer player, LivingEntity target, float damage) {
    }

    default void slain(ServerPlayer player, LivingEntity victim) {
    }

    default void harvest(ServerPlayer player, BlockDropsEvent event) {
    }

    default void spoil(ServerPlayer player, LivingDropsEvent event) {
    }

    default void savor(ServerPlayer player, ItemStack food) {
    }

    default void perish(ServerPlayer player, LivingDeathEvent event) {
    }

    default boolean fateful() {
        return true;
    }
}
