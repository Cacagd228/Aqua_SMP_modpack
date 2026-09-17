package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.common.items.HexBaubleItem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Линза Постижения — Curios-артефакт (слот necklace). Позволяет кликать по
 * маркеру паттерна в истории чата, чтобы скопировать его в стек текущего
 * каста посоха (100 маны + 200 за каждую иоту). Текстура-заглушка — осколок
 * аметиста.
 */
public class ItemPatternReader extends Item implements HexBaubleItem {

    public ItemPatternReader(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getHexBaubleAttrs(ItemStack stack) {
        return HashMultimap.create();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.meowhex.pattern_reader.tooltip"));
    }
}