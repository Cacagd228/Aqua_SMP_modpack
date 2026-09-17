package at.petrak.hexcasting.common.items.armor;

import at.petrak.hexcasting.api.HexAPI;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;

/**
 * Armor model is obtained via a self-mixin.
 */
public class ItemRobes extends ArmorItem {
    public final ArmorItem.Type type;

    public ItemRobes(ArmorItem.Type type, Properties properties) {
        super(Holder.direct(HexAPI.instance().robesMaterial()), type, properties);
        this.type = type;
    }
}
