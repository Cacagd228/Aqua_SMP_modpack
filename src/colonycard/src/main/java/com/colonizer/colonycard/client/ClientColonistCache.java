package com.colonizer.colonycard.client;

import com.colonizer.colonycard.data.ColonistData;
import net.minecraft.client.Minecraft;

/**
 * Holds the last {@link ColonistData} the client received about itself.
 * Attachments live server-side only, so the client needs its own copy to draw the card.
 */
public final class ClientColonistCache {
    private ClientColonistCache() {
    }

    private static ColonistData data = ColonistData.DEFAULT;

    public static void apply(String playerName, ColonistData newData) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !mc.player.getGameProfile().getName().equals(playerName)) {
            // A sync packet should only ever target the owning player, but guard anyway.
            return;
        }
        data = newData;
        if (mc.screen instanceof ColonistCardScreen cardScreen) {
            cardScreen.onDataUpdated(data);
        }
    }

    public static ColonistData get() {
        return data;
    }

    public static boolean isReady() {
        return data.initialized();
    }

    public static void clear() {
        data = ColonistData.DEFAULT;
    }
}
