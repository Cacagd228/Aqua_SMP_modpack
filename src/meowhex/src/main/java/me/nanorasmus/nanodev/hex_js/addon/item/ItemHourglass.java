package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.common.items.HexBaubleItem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Песочные часы — Curios-артефакт (слот necklace). Полностью заменяет реген
 * маны: пока носитель стоит, реген выше базового на
 * {@code HourglassHandler.REGEN_MULTIPLIER - 1}, а при любом движении реген
 * обрывается полностью (в том числе базовый).
 *
 * <p>Собственных атрибутов у предмета нет: бонус условный, поэтому его
 * ставит и снимает {@link me.nanorasmus.nanodev.hex_js.casting.HourglassHandler}
 * по результату проверки «стоит ли носитель».
 */
public class ItemHourglass extends Item implements HexBaubleItem {

    public ItemHourglass(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getHexBaubleAttrs(ItemStack stack) {
        // Бонус условный (только стоя) — обычный атрибут-баббл тут не подходит.
        return HashMultimap.create();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("meowhex.tooltip.hourglass")
                .withStyle(ChatFormatting.GRAY));
    }
}
