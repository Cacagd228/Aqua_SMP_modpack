package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.common.lib.HexAttributes;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Hextended amethyst battery staff: an {@link ItemBatteryStaff} with a larger
 * reservoir (50 dust) plus a stronger grid-zoom bonus (multiply-base) that also
 * applies from the head slot.
 */
public class ItemExtendedAmethystStaff extends ItemBatteryStaff {
    public static final int CAPACITY_IN_DUST = 50;

    public static final AttributeModifier GRID_ZOOM = new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(HexJS.MOD_ID, "grid_zoom"),
            0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    public ItemExtendedAmethystStaff(Item.Properties properties) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.GRID_ZOOM), GRID_ZOOM, EquipmentSlotGroup.HEAD)
                .add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.GRID_ZOOM), GRID_ZOOM, EquipmentSlotGroup.MAINHAND)
                .add(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.GRID_ZOOM), GRID_ZOOM, EquipmentSlotGroup.OFFHAND)
                .build()));
    }
}