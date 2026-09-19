package com.meowaddons.tier;

import com.meowaddons.ModBlockEntities;
import com.simibubi.create.content.kinetics.deployer.DeployerRenderer;
import com.simibubi.create.content.kinetics.deployer.DeployerVisual;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.world.level.block.entity.BlockEntityType;

@EventBusSubscriber(modid="meowaddons", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public class TieredDeployerClient {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_DEPLOYER_T1.get(), DeployerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_DEPLOYER_T2.get(), DeployerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_DEPLOYER_T3.get(), DeployerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_DEPLOYER_T4.get(), DeployerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_DEPLOYER_T5.get(), DeployerRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.TIERED_DEPLOYER_T6.get(), DeployerRenderer::new);
    }
    @SubscribeEvent public static void onClientSetup(FMLClientSetupEvent e){
        e.enqueueWork(() -> {
            registerVisual(ModBlockEntities.TIERED_DEPLOYER_T1.get());
            registerVisual(ModBlockEntities.TIERED_DEPLOYER_T2.get());
            registerVisual(ModBlockEntities.TIERED_DEPLOYER_T3.get());
            registerVisual(ModBlockEntities.TIERED_DEPLOYER_T4.get());
            registerVisual(ModBlockEntities.TIERED_DEPLOYER_T5.get());
            registerVisual(ModBlockEntities.TIERED_DEPLOYER_T6.get());
        });
    }
    // Create регистрирует .visual(DeployerVisual::new) = renderNormally=true => skipVanillaRender=false,
    // BER продолжает рисовать предмет в руке и фильтр; дефолт SimpleBlockEntityVisualizer — skip=true.
    private static <T extends com.simibubi.create.content.kinetics.deployer.DeployerBlockEntity> void registerVisual(BlockEntityType<T> type){
        SimpleBlockEntityVisualizer.<T>builder(type).factory(DeployerVisual::new).neverSkipVanillaRender().apply();
    }
}
