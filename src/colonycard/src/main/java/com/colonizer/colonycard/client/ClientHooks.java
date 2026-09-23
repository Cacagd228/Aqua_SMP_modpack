package com.colonizer.colonycard.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.Minecraft;

/** Клиентские хуки, вызываемые из общего кода (предметы и т.п.). Только клиент. */
@OnlyIn(Dist.CLIENT)
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void openDecreeScreen(String recipientName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) {
            mc.setScreen(new ImperialDecreeScreen(recipientName));
        }
    }
}
