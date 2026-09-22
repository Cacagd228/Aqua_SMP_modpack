package com.colonizer.colonycard.client;

import com.colonizer.colonycard.ColonyCardMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Listens for the keybind being pressed and opens the colonist card. Runs on the game event bus. */
@EventBusSubscriber(modid = ColonyCardMod.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientTickHandler {
    private ClientTickHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ClientModEvents.OPEN_CARD.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new ColonistCardScreen());
            }
        }
    }
}
