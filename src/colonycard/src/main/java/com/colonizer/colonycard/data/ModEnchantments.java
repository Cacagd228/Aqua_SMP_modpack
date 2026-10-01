package com.colonizer.colonycard.data;

import com.colonizer.colonycard.ColonyCardMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Зачарования мода: {@code colonycard:stealth} — шлем скрывает личность, как балаклава.
 *
 * <p>В 1.21.1 реестр {@code minecraft:enchantment} стал датапак-реестром
 * (см. {@code RegistryDataLoader}: {@code Enchantment.DIRECT_CODEC}), поэтому
 * {@code DeferredRegister} для него молча не срабатывает — зачарование нужно
 * отдавать файлом {@code data/colonycard/enchantment/stealth.json}.
 */
public final class ModEnchantments {

    private ModEnchantments() {
    }

    /** Максимальный уровень зачарования. */
    public static final int STEALTH_MAX_LEVEL = 30;

    public static final ResourceKey<Enchantment> STEALTH = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "stealth"));

    /** Есть ли на предмете зачарование stealth (уровень не важен). */
    public static boolean hasStealth(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (Holder<Enchantment> holder : stack.getEnchantments().keySet()) {
            if (holder.is(STEALTH)) {
                return true;
            }
        }
        return false;
    }
}
