package me.nanorasmus.nanodev.hex_js.addon.armor;

import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

/**
 * Материалы сетов: оба — статы железа (тест моделек, без эффектов).
 * Слои {@code crimson_mage}/{@code scarlet_knight} требуют
 * textures/models/armor/&lt;set&gt;_layer_{1,2}.png (реально рендерит GeckoLib).
 */
public final class ModArmorMaterials {
    private ModArmorMaterials() {
    }

    private static final int ENCHANTMENT = 9;

    private static final EnumMap<ArmorItem.Type, Integer> IRON_DEFENSE = Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
        map.put(ArmorItem.Type.BOOTS, 2);
        map.put(ArmorItem.Type.LEGGINGS, 5);
        map.put(ArmorItem.Type.CHESTPLATE, 6);
        map.put(ArmorItem.Type.HELMET, 2);
        map.put(ArmorItem.Type.BODY, 6);
    });

    private static Holder<ArmorMaterial> ironLike(String layer) {
        return Holder.direct(new ArmorMaterial(
                IRON_DEFENSE,
                ENCHANTMENT,
                SoundEvents.ARMOR_EQUIP_IRON,
                () -> Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(HexJS.modLoc(layer))),
                0.0f,
                0.0f));
    }

    public static final Holder<ArmorMaterial> HOLY_VALKYRIE = ironLike("holy_valkyrie");
    public static final Holder<ArmorMaterial> SCARLET_KNIGHT = ironLike("scarlet_knight");
}
