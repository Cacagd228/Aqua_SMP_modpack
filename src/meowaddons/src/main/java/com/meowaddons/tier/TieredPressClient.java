package com.meowaddons.tier;
import com.meowaddons.ModBlockEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredPressClient {
 @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_PRESS_T1.get(), TieredPressRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_PRESS_T2.get(), TieredPressRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_PRESS_T3.get(), TieredPressRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_PRESS_T4.get(), TieredPressRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_PRESS_T5.get(), TieredPressRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_PRESS_T6.get(), TieredPressRenderer::new);
 }
 // Flywheel visual: tiered головка + вал. Без регистрации молот не рендерится (BER early-return при supportsVisualization).
 @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
  e.enqueueWork(() -> {
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_PRESS_T1.get()).factory(TieredPressVisual::new).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_PRESS_T2.get()).factory(TieredPressVisual::new).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_PRESS_T3.get()).factory(TieredPressVisual::new).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_PRESS_T4.get()).factory(TieredPressVisual::new).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_PRESS_T5.get()).factory(TieredPressVisual::new).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_PRESS_T6.get()).factory(TieredPressVisual::new).apply();
  });
 }
}