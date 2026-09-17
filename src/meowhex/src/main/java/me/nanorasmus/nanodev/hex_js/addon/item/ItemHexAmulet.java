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
 * Mana amulet: a bauble worn in the Curios {@code necklace} slot that boosts the
 * wearer's mana pool via hexcasting attributes.
 *
 * <p>Units: {@code maxMana} is flat extra mana; {@code discount} is a fraction of
 * the cost (0.01 = 1%); {@code manaRegen} is in <b>mana per second</b> — a value
 * of {@code 1.0} gives 1.0 mana/s. The hexcasting {@code MANA_REGEN} attribute
 * itself is "mana per 6 seconds" (the tick hook divides by 120), so we multiply
 * the input by {@code 6} before writing the modifier.
 */
public class ItemHexAmulet extends Item implements HexBaubleItem {
    /** Hexcasting {@code MANA_REGEN} is "mana per 6s"; the public value is mana/s. */
    private static final double REGEN_TO_HEX_UNIT = 6.0;

    private final String idPrefix;
    private final double maxMana;
    private final double discount;
    private final double manaRegen;

    public ItemHexAmulet(Item.Properties properties, String idPrefix, double maxMana, double discount, double manaRegen) {
        super(properties);
        this.idPrefix = idPrefix;
        this.maxMana = maxMana;
        this.discount = discount;
        this.manaRegen = manaRegen;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getHexBaubleAttrs(ItemStack stack) {
        HashMultimap<Attribute, AttributeModifier> out = HashMultimap.create();
        if (maxMana > 0) {
            out.put(HexAttributes.MANA_MAX, new AttributeModifier(
                    HexJS.modLoc(idPrefix + "_mana_max"), maxMana, AttributeModifier.Operation.ADD_VALUE));
        }
        if (discount > 0) {
            out.put(HexAttributes.MANA_DISCOUNT, new AttributeModifier(
                    HexJS.modLoc(idPrefix + "_mana_discount"), discount, AttributeModifier.Operation.ADD_VALUE));
        }
        if (manaRegen > 0) {
            out.put(HexAttributes.MANA_REGEN, new AttributeModifier(
                    HexJS.modLoc(idPrefix + "_mana_regen"),
                    manaRegen * REGEN_TO_HEX_UNIT, AttributeModifier.Operation.ADD_VALUE));
        }
        return out;
    }
}
