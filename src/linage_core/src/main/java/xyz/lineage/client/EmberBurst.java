package xyz.lineage.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import xyz.lineage.vendored.panoptic.api.ui.GuiStyle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Golden oath-sparks: short-lived embers with gravity and drag,
 * saluting navigation and sworn promises.
 */
@OnlyIn(Dist.CLIENT)
final class EmberBurst {
    private static final class Mote {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float maxLife;
        float size;
    }

    private final List<Mote> motes = new ArrayList<>();
    private final RandomSource rand = RandomSource.create();
    private long lastNs;

    void burst(float x, float y, int count) {
        for (int i = 0; i < count; i++) {
            if (motes.size() > 160) {
                return;
            }
            Mote mote = new Mote();
            float angle = rand.nextFloat() * (float) (Math.PI * 2);
            float speed = 26.0F + rand.nextFloat() * 46.0F;
            mote.x = x;
            mote.y = y;
            mote.vx = Mth.cos(angle) * speed;
            mote.vy = Mth.sin(angle) * speed - 12.0F;
            mote.maxLife = 0.3F + rand.nextFloat() * 0.35F;
            mote.life = mote.maxLife;
            mote.size = rand.nextFloat() < 0.35F ? 2.0F : 1.0F;
            motes.add(mote);
        }
    }

    void frame() {
        long now = System.nanoTime();
        if (lastNs == 0L) {
            lastNs = now;
        }
        float dt = Math.min((now - lastNs) / 1.0e9F, 0.1F);
        lastNs = now;
        for (int i = motes.size() - 1; i >= 0; i--) {
            Mote mote = motes.get(i);
            mote.life -= dt;
            if (mote.life <= 0.0F) {
                motes.remove(i);
                continue;
            }
            mote.x += mote.vx * dt;
            mote.y += mote.vy * dt;
            mote.vy += 92.0F * dt;
            mote.vx *= 1.0F - 2.1F * dt;
            mote.vy *= 1.0F - 0.7F * dt;
        }
    }

    void draw(GuiGraphics gfx) {
        for (Mote mote : motes) {
            float f = Mth.clamp(mote.life / mote.maxLife, 0.0F, 1.0F);
            int alpha = (int) (255.0F * f);
            if (alpha < 6) {
                continue;
            }
            int base = GuiStyle.mix(GuiStyle.T(0xFFFF9A3C), GuiStyle.T(0xFFFFE7B0), f);
            int color = (alpha << 24) | (base & 0xFFFFFF);
            int s = Math.max(1, (int) mote.size);
            gfx.fill((int) mote.x, (int) mote.y, (int) mote.x + s, (int) mote.y + s, color);
        }
    }
}
