package com.meowaddons.tier.fan;
import com.meowaddons.ModBlockEntities;
import com.simibubi.create.content.kinetics.fan.EncasedFanRenderer;
import com.simibubi.create.content.kinetics.fan.FanVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredFanClient {
 @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_FAN_T1.get(), EncasedFanRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_FAN_T2.get(), EncasedFanRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_FAN_T3.get(), EncasedFanRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_FAN_T4.get(), EncasedFanRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_FAN_T5.get(), EncasedFanRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_FAN_T6.get(), EncasedFanRenderer::new);
 }
 // Flywheel visual: vanilla propeller, no custom models.
 @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
  e.enqueueWork(() -> {
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_FAN_T1.get()).factory((ctx,be,pt)->new FanVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_FAN_T2.get()).factory((ctx,be,pt)->new FanVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_FAN_T3.get()).factory((ctx,be,pt)->new FanVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_FAN_T4.get()).factory((ctx,be,pt)->new FanVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_FAN_T5.get()).factory((ctx,be,pt)->new FanVisual(ctx,be,pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_FAN_T6.get()).factory((ctx,be,pt)->new FanVisual(ctx,be,pt)).apply();
  });
 }
}
