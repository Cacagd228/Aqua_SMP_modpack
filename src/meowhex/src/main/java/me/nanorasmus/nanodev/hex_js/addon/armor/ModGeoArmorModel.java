package me.nanorasmus.nanodev.hex_js.addon.armor;

import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Одна модель на часть сета. Geo-файлы лежат в
 * {@code assets/meowhex/geo/armor/<set>_<part>.geo.json},
 * текстура сета: {@code textures/armor/<set>_armor.png}.
 */
public class ModGeoArmorModel extends GeoModel<ModGeoArmorItem> {
    private final String set;
    private final String part;

    public ModGeoArmorModel(String set, String part) {
        this.set = set;
        this.part = part;
    }

    @Override
    public ResourceLocation getModelResource(ModGeoArmorItem animatable) {
        return HexJS.modLoc("geo/armor/" + set + "_" + part + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ModGeoArmorItem animatable) {
        return HexJS.modLoc("textures/armor/" + set + "_armor.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ModGeoArmorItem animatable) {
        return HexJS.modLoc("animations/armor/armor_static.animation.json");
    }
}
