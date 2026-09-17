package com.meowaddons.tier.crusher;
import com.meowaddons.ModBlockEntities;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredCrusherClient {
 @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHER_T1.get(), KineticBlockEntityRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHER_T2.get(), KineticBlockEntityRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHER_T3.get(), KineticBlockEntityRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHER_T4.get(), KineticBlockEntityRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHER_T5.get(), KineticBlockEntityRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_CRUSHER_T6.get(), KineticBlockEntityRenderer::new);
 }
 @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
  e.enqueueWork(() -> {
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHER_T1.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.CRUSHING_WHEEL)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHER_T2.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.CRUSHING_WHEEL)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHER_T3.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.CRUSHING_WHEEL)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHER_T4.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.CRUSHING_WHEEL)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHER_T5.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.CRUSHING_WHEEL)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_CRUSHER_T6.get()).factory(SingleAxisRotatingVisual.of(AllPartialModels.CRUSHING_WHEEL)).apply();
  });
 }
}
