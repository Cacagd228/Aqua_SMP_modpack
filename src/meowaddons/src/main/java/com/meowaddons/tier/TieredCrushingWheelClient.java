package com.meowaddons.tier;

import com.meowaddons.ModBlockEntities;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredCrushingWheelClient {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
        // Берём рендер как в Create — KineticBlockEntityRenderer с shaft по AXIS
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHING_WHEEL_T1.get(), TieredCrushingWheelRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHING_WHEEL_T2.get(), TieredCrushingWheelRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHING_WHEEL_T3.get(), TieredCrushingWheelRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHING_WHEEL_T4.get(), TieredCrushingWheelRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHING_WHEEL_T5.get(), TieredCrushingWheelRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHING_WHEEL_T6.get(), TieredCrushingWheelRenderer::new);
    }

    @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
        // Берём визуал напрямую из Create — SingleAxisRotatingVisual.of(CRUSHING_WHEEL), как в AllBlockEntityTypes
        // Теперь с правильными блоками (noOcclusion) и осью — не чёрный, крутится в нужной плоскости
        e.enqueueWork(() -> {
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHING_WHEEL_T1.get()).factory((ctx, be, pt) -> new com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual<>(ctx, be, pt, dev.engine_room.flywheel.lib.model.Models.partial(com.simibubi.create.AllPartialModels.CRUSHING_WHEEL))).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHING_WHEEL_T2.get()).factory((ctx, be, pt) -> new com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual<>(ctx, be, pt, dev.engine_room.flywheel.lib.model.Models.partial(com.simibubi.create.AllPartialModels.CRUSHING_WHEEL))).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHING_WHEEL_T3.get()).factory((ctx, be, pt) -> new com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual<>(ctx, be, pt, dev.engine_room.flywheel.lib.model.Models.partial(com.simibubi.create.AllPartialModels.CRUSHING_WHEEL))).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHING_WHEEL_T4.get()).factory((ctx, be, pt) -> new com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual<>(ctx, be, pt, dev.engine_room.flywheel.lib.model.Models.partial(com.simibubi.create.AllPartialModels.CRUSHING_WHEEL))).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHING_WHEEL_T5.get()).factory((ctx, be, pt) -> new com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual<>(ctx, be, pt, dev.engine_room.flywheel.lib.model.Models.partial(com.simibubi.create.AllPartialModels.CRUSHING_WHEEL))).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHING_WHEEL_T6.get()).factory((ctx, be, pt) -> new com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual<>(ctx, be, pt, dev.engine_room.flywheel.lib.model.Models.partial(com.simibubi.create.AllPartialModels.CRUSHING_WHEEL))).apply();
        });
    }
}
