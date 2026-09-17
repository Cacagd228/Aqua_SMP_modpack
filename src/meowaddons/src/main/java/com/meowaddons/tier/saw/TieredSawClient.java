package com.meowaddons.tier.saw;
import com.meowaddons.ModBlockEntities;
import com.simibubi.create.content.kinetics.saw.SawRenderer;
import com.simibubi.create.content.kinetics.saw.SawVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredSawClient {
 @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T1.get(), SawRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T2.get(), SawRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T3.get(), SawRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T4.get(), SawRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T5.get(), SawRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T6.get(), SawRenderer::new);
 }
 // Flywheel visual: vanilla blade partials, NO custom models.
 @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
  e.enqueueWork(() -> {
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_SAW_T1.get()).factory((ctx,be,pt)->new SawVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_SAW_T2.get()).factory((ctx,be,pt)->new SawVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_SAW_T3.get()).factory((ctx,be,pt)->new SawVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_SAW_T4.get()).factory((ctx,be,pt)->new SawVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_SAW_T5.get()).factory((ctx,be,pt)->new SawVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_SAW_T6.get()).factory((ctx,be,pt)->new SawVisual(ctx,be,pt)).apply();
  });
 }
}
