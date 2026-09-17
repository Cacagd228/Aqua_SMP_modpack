package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.common.items.HexBaubleItem;
import at.petrak.hexcasting.common.lib.HexAttributes;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ring of Self-Torture: a bauble worn in the Curios {@code ring} slot granting
 * +50% mana discount — but the spared mana strikes back as
 * {@code hexcasting:overcast} damage, 5 mana = 1 HP (see
 * {@code SelfTortureMixin}).
 *
 * <p>The discount is a plain {@code ADD_VALUE} +0.5 on {@code MANA_DISCOUNT},
 * stacking additively with amulet discounts (clamped to 1.0 downstream).
 */
public class ItemSelfTortureRing extends Item implements HexBaubleItem {
    /** Modifier id, also used by the overcast mixin to detect the worn ring. */
    public static final ResourceLocation RING_DISCOUNT_ID =
            HexJS.modLoc("self_torture_ring_discount");

    public static final double DISCOUNT = 0.5;

    public ItemSelfTortureRing(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getHexBaubleAttrs(ItemStack stack) {
        HashMultimap<Attribute, AttributeModifier> out = HashMultimap.create();
        out.put(HexAttributes.MANA_DISCOUNT, new AttributeModifier(
                RING_DISCOUNT_ID, DISCOUNT, AttributeModifier.Operation.ADD_VALUE));
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("meowhex.tooltip.self_torture_ring")
                .withStyle(ChatFormatting.GRAY));
    }
}
