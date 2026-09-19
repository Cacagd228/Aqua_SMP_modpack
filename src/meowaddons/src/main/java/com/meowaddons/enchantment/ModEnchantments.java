package com.meowaddons.enchantment;

import com.meowaddons.MeowAddons;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/**
 * Кастомные чары meowaddons — data-driven (1.21+).
 * Сами зачарования лежат в datapack-JSON: data/meowaddons/enchantment/*.json,
 * здесь только ResourceKey + безопасные хелперы чтения уровня.
 * Регистрировать через DeferredRegister НЕ нужно (и нельзя — конструктор
 * Enchantment в рантайме не используется, реестр наполняется из datapack).
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> TRIPLE_SHOT = key("triple_shot");
    public static final ResourceKey<Enchantment> AUTO_SHOT = key("auto_shot");

    private ModEnchantments() {}

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, path));
    }

    public static int getTripleShotLevel(ItemStack stack, Level level) {
        return levelOf(stack, level, TRIPLE_SHOT);
    }

    public static int getAutoShotLevel(ItemStack stack, Level level) {
        return levelOf(stack, level, AUTO_SHOT);
    }

    private static int levelOf(ItemStack stack, Level level, ResourceKey<Enchantment> key) {
        if (stack.isEmpty() || level == null) return 0;
        // getOrThrow здесь крашил бы игру при рассинхроне датапака — возвращаем 0
        var holder = level.holderLookup(Registries.ENCHANTMENT).get(key).orElse(null);
        if (holder == null) return 0;
        return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    }
}
