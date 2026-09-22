package me.nanorasmus.nanodev.hex_js.client;

/** Клиентский кэш общего пула (обновляется пакетом MsgManaPairS2C). */
public final class ManaPairClientCache {
    private static volatile boolean paired = false;
    private static volatile double shared = 0;
    private static volatile double sharedMax = 1;

    private ManaPairClientCache() {
    }

    public static void update(boolean p, double s, double m) {
        paired = p;
        shared = s;
        sharedMax = m <= 0 ? 1 : m;
    }

    public static boolean isPaired() {
        return paired;
    }

    public static double shared() {
        return shared;
    }

    public static double sharedMax() {
        return sharedMax;
    }
}
