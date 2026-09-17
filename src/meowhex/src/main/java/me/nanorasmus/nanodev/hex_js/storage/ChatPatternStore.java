package me.nanorasmus.nanodev.hex_js.storage;

import at.petrak.hexcasting.api.casting.math.HexPattern;

import java.util.List;

/**
 * Server-side store of the last set of patterns players wrote in chat (as
 * {@code <dir,sig>} markers). Updated by the chat listener, read by the
 * "Постижение" spell. Transient — lives for the current server session.
 */
public final class ChatPatternStore {
    private static List<HexPattern> lastChatPatterns = List.of();

    private ChatPatternStore() {
    }

    public static void record(List<HexPattern> patterns) {
        lastChatPatterns = List.copyOf(patterns);
    }

    public static List<HexPattern> getLastChatPatterns() {
        return lastChatPatterns;
    }
}