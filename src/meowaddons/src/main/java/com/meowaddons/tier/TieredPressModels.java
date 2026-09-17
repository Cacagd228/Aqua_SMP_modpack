package com.meowaddons.tier;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;

/**
 * Головы прессов — как в Create Encased (CustomPressRenderer): общая ванильная
 * модель create:block/mechanical_press/head для всех тиров. Корпус различается
 * текстурами press_t*.json, молот одинаковый — Flywheel Visual/Renderer берут
 * AllPartialModels.MECHANICAL_PRESS_HEAD напрямую.
 */
public final class TieredPressModels {
    private TieredPressModels() {}

    public static PartialModel headModel(Tier tier) {
        return com.simibubi.create.AllPartialModels.MECHANICAL_PRESS_HEAD;
    }

    /** Форсирует статическую инициализацию PartialModel до запекания моделей (см. TransmitterRenderer::init). */
    public static void init() {
    }
}
