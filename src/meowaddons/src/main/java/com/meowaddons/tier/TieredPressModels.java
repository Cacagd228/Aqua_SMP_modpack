package com.meowaddons.tier;

import com.meowaddons.MeowAddons;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Реестр partial-моделей голов прессов, по образцу EncasedPartialModels из Create Encased:
 * один PartialModel на тир, выбор модели по Tier/BlockState в Visual и Renderer.
 * Модели: assets/meowaddons/models/block/press_head_t1..t6.json.
 */
public final class TieredPressModels {
    private TieredPressModels() {}

    public static final PartialModel HEAD_T1 = head(1);
    public static final PartialModel HEAD_T2 = head(2);
    public static final PartialModel HEAD_T3 = head(3);
    public static final PartialModel HEAD_T4 = head(4);
    public static final PartialModel HEAD_T5 = head(5);
    public static final PartialModel HEAD_T6 = head(6);

    private static PartialModel head(int tier) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(MeowAddons.MODID, "block/press_head_t" + tier));
    }

    public static PartialModel headModel(Tier tier) {
        return switch (tier) {
            case ANDESITE -> HEAD_T1;
            case BRASS -> HEAD_T2;
            case STEEL -> HEAD_T3;
            case SHADOW_STEEL -> HEAD_T4;
            case REFINED_RADIANCE -> HEAD_T5;
            case CHROMATIC -> HEAD_T6;
        };
    }

    /** Форсирует статическую инициализацию PartialModel до запекания моделей (см. TransmitterRenderer::init). */
    public static void init() {
    }
}
