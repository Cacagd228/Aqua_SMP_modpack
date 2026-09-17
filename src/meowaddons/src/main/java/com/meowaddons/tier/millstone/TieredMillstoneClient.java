package com.meowaddons.tier.millstone;
import com.meowaddons.ModBlockEntities;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.millstone.MillstoneRenderer;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredMillstoneClient {
 @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T1.get(), MillstoneRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T2.get(), MillstoneRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T3.get(), MillstoneRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T4.get(), MillstoneRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T5.get(), MillstoneRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MILLSTONE_T6.get(), MillstoneRenderer::new);
 }
 @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
  e.enqueueWork(() -> {
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T1.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.MILLSTONE_COG)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T2.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.MILLSTONE_COG)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T3.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.MILLSTONE_COG)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T4.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.MILLSTONE_COG)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T5.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.MILLSTONE_COG)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MILLSTONE_T6.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.MILLSTONE_COG)).apply();
  });
 }
}
