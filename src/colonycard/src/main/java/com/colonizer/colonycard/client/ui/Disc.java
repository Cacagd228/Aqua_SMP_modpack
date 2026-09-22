package com.colonizer.colonycard.client.ui;

/** Порт {@code net.mokich.panoptic.api.util.Disc}: предрасчёт полуширин круга. */
public final class Disc {
    private static final int[][] SPANS = new int[33][];

    private Disc() {
    }

    public static int[] spans(int r) {
        if (r >= 0 && r < SPANS.length) {
            int[] cached = SPANS[r];
            if (cached == null) {
                cached = compute(r);
                SPANS[r] = cached;
            }
            return cached;
        }
        return compute(r);
    }

    private static int[] compute(int r) {
        int[] out = new int[2 * r + 1];
        for (int dy = -r; dy <= r; dy++) {
            out[dy + r] = (int) Math.round(Math.sqrt((double) r * r - dy * dy));
        }
        return out;
    }
}
