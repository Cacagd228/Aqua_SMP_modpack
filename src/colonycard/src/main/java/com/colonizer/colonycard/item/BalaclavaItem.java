package com.colonizer.colonycard.item;

import com.colonizer.colonycard.identity.FaceCover;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Балаклава — первый предмет с {@code hides_identity=true}.
 *
 * <p>Намеренно НЕ {@code ArmorItem}: предмет нельзя надеть на ванильную голову,
 * он носится только в Curios-слоте лица ({@code curios:face}).
 *
 * <p>3D-шлем в слоте лица рисует {@code BalaclavaCurioRenderer} — по образцу
 * работающего в паке {@code HexJsCuriosInteropClient}. Скрытие личности
 * определяется только флагом {@link FaceCover}, а не ID предмета.
 */
public class BalaclavaItem extends Item implements ICurioItem {

    public BalaclavaItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    /** Новый стак балаклавы с выставленным флагом скрытия лица. */
    public static ItemStack create() {
        ItemStack stack = new ItemStack(ModItems.BALACLAVA.get());
        FaceCover.setHidesIdentity(stack, true);
        return stack;
    }
}
