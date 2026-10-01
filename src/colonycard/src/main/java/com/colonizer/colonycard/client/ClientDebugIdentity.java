package com.colonizer.colonycard.client;

/**
 * Клиентский флаг отладочного режима личности (только для оп-команды).
 *
 * <p>Включён сервером командой {@code /colonycard debug_identity}:
 * над nameplate дополнительно показывается истинный ник красным,
 * а в чате игрок без отображаемого имени пишет как «...».
 */
public final class ClientDebugIdentity {
    private ClientDebugIdentity() {
    }

    private static volatile boolean enabled;

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static void clear() {
        enabled = false;
    }
}
