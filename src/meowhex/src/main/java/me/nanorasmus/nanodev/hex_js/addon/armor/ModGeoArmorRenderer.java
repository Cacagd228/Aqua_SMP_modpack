package me.nanorasmus.nanodev.hex_js.addon.armor;

import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * Один рендерер на все сеты — конкретная geo-модель подставляется per-item
 * через {@link ModGeoArmorItem#createGeoRenderer}.
 */
public class ModGeoArmorRenderer extends GeoArmorRenderer<ModGeoArmorItem> {
    public ModGeoArmorRenderer(ModGeoArmorModel model) {
        super(model);
    }
}
