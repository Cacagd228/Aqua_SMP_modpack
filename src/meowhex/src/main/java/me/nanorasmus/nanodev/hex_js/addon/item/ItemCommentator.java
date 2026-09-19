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
 * Комментатор — Curios-артефакт (слот necklace). Пока надет на убийцу,
 * все игроки в радиусе 200 блоков слышат дотовские анонсы PvP-убийств
 * (first blood, multi kill, killing spree и т.д.).
 * Текстура-заглушка — козий рог.
 */
public class ItemCommentator extends Item implements HexBaubleItem {

    public ItemCommentator(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getHexBaubleAttrs(ItemStack stack) {
        return HashMultimap.create();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.meowhex.commentator.tooltip"));
    }
}
