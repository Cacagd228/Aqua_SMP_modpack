package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.common.items.ItemStaff;
import at.petrak.hexcasting.common.lib.HexAttributes;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Hextended "extended" staff: a normal {@link ItemStaff} that grants a bonus to the
 * Hex Casting grid-zoom attribute while held, giving a larger drawing grid.
 *
 * <p>Ported to the 1.21 attribute-component model ({@link ItemAttributeModifiers} +
 * {@link EquipmentSlotGroup}) instead of the removed per-slot override.
 */
public class ItemExtendedStaff extends ItemStaff {
    public static final AttributeModifier GRID_ZOOM = new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(HexJS.MOD_ID, "grid_zoom"),
            0.15, AttributeModifier.Operation.ADD_VALUE);

    public ItemExtendedStaff(Properties properties) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.GRID_ZOOM), GRID_ZOOM, EquipmentSlotGroup.MAINHAND)
                .add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.GRID_ZOOM), GRID_ZOOM, EquipmentSlotGroup.OFFHAND)
                .build()));
    }
}