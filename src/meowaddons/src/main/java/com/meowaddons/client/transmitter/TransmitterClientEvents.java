package com.meowaddons.client.transmitter;

import com.meowaddons.MeowAddons;
import com.meowaddons.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MeowAddons.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TransmitterClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Форсируем статическую инициализацию PartialModel до запекания моделей Flywheel
        event.enqueueWork(TransmitterRenderer::init);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.INTERDIMENSIONAL_TRANSMITTER.get(),
                TransmitterRenderer::new);
    }
}
