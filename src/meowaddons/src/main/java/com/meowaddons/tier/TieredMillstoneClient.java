package com.meowaddons.tier;

import com.meowaddons.ModBlockEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredMillstoneClient {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T1.get(), TieredMillstoneRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T2.get(), TieredMillstoneRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T3.get(), TieredMillstoneRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T4.get(), TieredMillstoneRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T5.get(), TieredMillstoneRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T6.get(), TieredMillstoneRenderer::new);
    }
    @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
        e.enqueueWork(() -> {
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T1.get()).factory(TieredMillstoneVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T2.get()).factory(TieredMillstoneVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T3.get()).factory(TieredMillstoneVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T4.get()).factory(TieredMillstoneVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T5.get()).factory(TieredMillstoneVisual::new).apply();
            SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T6.get()).factory(TieredMillstoneVisual::new).apply();
        });
    }
}
