package me.nanorasmus.nanodev.hex_js.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

/**
 * Client-only helper: opens the chat input pre-filled with the given text.
 * Kept out of the packet record so the message class stays dist-safe (the
 * dedicated server never loads this class).
 */
public final class ClientChatHandler {
    private ClientChatHandler() {
    }

    public static void openChatInput(String text) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            mc.setScreen(new ChatScreen(text));
        });
    }
}