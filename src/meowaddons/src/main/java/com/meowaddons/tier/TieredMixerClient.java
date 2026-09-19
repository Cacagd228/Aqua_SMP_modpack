package com.meowaddons.tier;

import com.meowaddons.ModBlockEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredMixerClient {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
        // Используем оригинальный рендер Create — легче поддерживать, не дублируем логику pole/head/cog
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T1.get(), com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T2.get(), com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T3.get(), com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T4.get(), com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T5.get(), com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T6.get(), com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer::new);
    }
    @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
        e.enqueueWork(() -> {
            // Flywheel — оригинальный MixerVisual (кастомный flywheel рендер Create) для каждой тиры
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T1.get()).factory(com.simibubi.create.content.kinetics.mixer.MixerVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T2.get()).factory(com.simibubi.create.content.kinetics.mixer.MixerVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T3.get()).factory(com.simibubi.create.content.kinetics.mixer.MixerVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T4.get()).factory(com.simibubi.create.content.kinetics.mixer.MixerVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T5.get()).factory(com.simibubi.create.content.kinetics.mixer.MixerVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T6.get()).factory(com.simibubi.create.content.kinetics.mixer.MixerVisual::new).apply();
        });
    }
}
