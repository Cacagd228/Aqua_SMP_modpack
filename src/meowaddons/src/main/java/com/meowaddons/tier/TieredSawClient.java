package com.meowaddons.tier;

import com.meowaddons.ModBlockEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredSawClient {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
        TieredSawBlades.touch(); // раннее создание PartialModel до первого bake
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T1.get(), TieredSawRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T2.get(), TieredSawRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T3.get(), TieredSawRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T4.get(), TieredSawRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T5.get(), TieredSawRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_SAW_T6.get(), TieredSawRenderer::new);
    }
    @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
        e.enqueueWork(() -> {
            registerVisual(ModBlockEntities.TIERED_SAW_T1.get());
            registerVisual(ModBlockEntities.TIERED_SAW_T2.get());
            registerVisual(ModBlockEntities.TIERED_SAW_T3.get());
            registerVisual(ModBlockEntities.TIERED_SAW_T4.get());
            registerVisual(ModBlockEntities.TIERED_SAW_T5.get());
            registerVisual(ModBlockEntities.TIERED_SAW_T6.get());
        });
    }
    // Create регистрирует SawVisual как visual(SawVisual::new) => renderNormally=true =>
    // skipVanillaRender=false. Дефолт SimpleBlockEntityVisualizer — skipVanillaRender=always true,
    // при этом BER (TieredSawRenderer.renderBlade/renderItems) полностью подавляется Flywheel,
    // а SawVisual инстансит только вал => лезвие невидимо.
    private static <T extends com.simibubi.create.content.kinetics.saw.SawBlockEntity> void registerVisual(net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        SimpleBlockEntityVisualizer.<T>builder(type).factory(com.simibubi.create.content.kinetics.saw.SawVisual::new).neverSkipVanillaRender().apply();
    }
}
