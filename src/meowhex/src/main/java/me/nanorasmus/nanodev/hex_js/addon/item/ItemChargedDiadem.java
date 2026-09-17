package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.common.items.HexBaubleItem;
import at.petrak.hexcasting.common.lib.HexAttributes;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Hextended charged amethyst diadem: a bauble doubling the wearer's mana pool
 * while worn. It deliberately does <b>not</b> grant scrying sight — the lens
 * overlay stays exclusive to the scrying lens. (The original leaves the
 * protection logic as a stub; see the overcast-damage mixin.)
 *
 * <p>Mana doubling is a {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL}
 * +1.0 modifier on {@code MANA_MAX}: the value computed from all other modifiers
 * (base, amulets, ...) is multiplied by two. Unequipping removes the modifier, at
 * which point the hexcasting clamp pulls stored mana back down to the new max.
 */
public class ItemChargedDiadem extends Item implements HexBaubleItem {
    private static final String MANA_ID = "charged_amethyst_diadem_mana_max";

    public ItemChargedDiadem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getHexBaubleAttrs(ItemStack stack) {
        HashMultimap<Attribute, AttributeModifier> out = HashMultimap.create();
        out.put(HexAttributes.MANA_MAX, new AttributeModifier(
                HexJS.modLoc(MANA_ID), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        return out;
    }
}