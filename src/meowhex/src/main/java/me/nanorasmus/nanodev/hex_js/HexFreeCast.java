package me.nanorasmus.nanodev.hex_js;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transient, never-persisted marker of players currently inside a script-invoked
 * {@code forceCast}. The cast itself uses a {@code StaffCastEnv} whose media
 * extraction the {@code FreeCastMediaMixin} short-circuits while a player is
 * listed here, mirroring the original addon's free forced casts.
 */
public final class HexFreeCast {
    private static final Set<UUID> ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private HexFreeCast() {
    }

    public static void begin(UUID playerId) {
        ACTIVE.add(playerId);
    }

    public static void end(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.contains(playerId);
    }
}