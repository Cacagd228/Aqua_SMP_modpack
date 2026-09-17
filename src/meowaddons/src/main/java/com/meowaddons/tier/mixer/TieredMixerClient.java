package com.meowaddons.tier.mixer;
import com.meowaddons.ModBlockEntities;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerRenderer;
import com.simibubi.create.content.kinetics.mixer.MixerVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredMixerClient {
 @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T1.get(), MechanicalMixerRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T2.get(), MechanicalMixerRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T3.get(), MechanicalMixerRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T4.get(), MechanicalMixerRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T5.get(), MechanicalMixerRenderer::new);
  e.registerBlockEntityRenderer(ModBlockEntities.TIERED_MIXER_T6.get(), MechanicalMixerRenderer::new);
 }
 @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
  e.enqueueWork(() -> {
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T1.get()).factory((ctx, be, pt) -> new MixerVisual(ctx, be, pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T2.get()).factory((ctx, be, pt) -> new MixerVisual(ctx, be, pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T3.get()).factory((ctx, be, pt) -> new MixerVisual(ctx, be, pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T4.get()).factory((ctx, be, pt) -> new MixerVisual(ctx, be, pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T5.get()).factory((ctx, be, pt) -> new MixerVisual(ctx, be, pt)).apply();
   SimpleBlockEntityVisualizer.builder(ModBlockEntities.TIERED_MIXER_T6.get()).factory((ctx, be, pt) -> new MixerVisual(ctx, be, pt)).apply();
  });
 }
}
