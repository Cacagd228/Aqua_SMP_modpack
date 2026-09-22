package me.nanorasmus.nanodev.hex_js.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * Текст анонса фрага на месте боссбара (верх экрана, просто текст без полосы).
 * Появляется на ~3 секунды и гаснет. Переводы резолвятся на клиенте,
 * поэтому у каждого виден его язык.
 */
@OnlyIn(Dist.CLIENT)
public class AnnounceTextOverlay {
    private static final long DURATION_MS = 3000L;
    private static final long FADE_MS = 500L;

    private static String killer = "";
    private static String victim = "";
    private static String eventId = "";
    private static String shutName = "";
    private static int shutStreak = 0;
    private static int mult = 0;
    private static boolean meepoAnnouncer = false;
    private static long untilMs = 0L;

    public static void show(String killerName, String victimName, String event,
            String shutdownName, int shutdownStreak, int multCount, boolean meepo) {
        killer = killerName;
        victim = victimName;
        eventId = event;
        shutName = shutdownName;
        shutStreak = shutdownStreak;
        mult = multCount;
        meepoAnnouncer = meepo;
        untilMs = System.currentTimeMillis() + DURATION_MS;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        long now = System.currentTimeMillis();
        if (now >= untilMs) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        Component main;
        Component sub = null;
        int mainRgb = 0xFFC845; // золото как баннер
        String killWordStr = shutStreak > 0 ? killWord(shutStreak) : "";
        if (!eventId.isEmpty() && !"first_blood".equals(eventId)) {
            if (("rampage".equals(eventId) || "holy_shit".equals(eventId)) && mult >= 2) {
                // RAMPAGE! X2 / HOLLY SHIT! X2 — буквально, без перевода.
                main = net.minecraft.network.chat.Component.literal(
                        ("holy_shit".equals(eventId) ? "HOLLY SHIT! X" : "RAMPAGE! X") + mult);
            } else {
                main = Component.translatable("meowhex.announcer.top_" + eventId, killer);
            }
            // Красным — пик комментатора: у Meepo rampage, у QoP holy_shit.
            boolean isPeak = meepoAnnouncer ? "rampage".equals(eventId) : "holy_shit".equals(eventId);
            if (isPeak) {
                mainRgb = 0xFF3333;
            }
            if (!killer.isEmpty()) {
                sub = Component.translatable("meowhex.announcer.action_kill", killer, victim);
            }
        } else if (!eventId.isEmpty()) {
            main = Component.translatable("sound.meowhex.announcer_" + eventId);
            if (!killer.isEmpty()) {
                sub = Component.translatable("meowhex.announcer.action_kill", killer, victim);
            }
        } else if (!killer.isEmpty()) {
            main = Component.translatable("meowhex.announcer.action_kill", killer, victim);
        } else if (shutStreak > 0) {
            main = Component.translatable("meowhex.announcer.action_shutdown", shutName, shutStreak, killWordStr);
        } else {
            return;
        }
        if (shutStreak > 0 && !shutName.isEmpty() && !killer.isEmpty()) {
            // PvP: shutdown дописываем в подстроку (вне PvP он уже main).
            MutableComponent shut = Component.translatable("meowhex.announcer.action_shutdown", shutName, shutStreak, killWordStr);
            sub = sub == null ? shut : sub.copy().append(" • ").append(shut);
        }

        long remaining = untilMs - now;
        float fade = remaining < FADE_MS ? Math.max(0f, remaining / (float) FADE_MS) : 1f;
        int alpha = (int) (255 * fade);

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;
        int w = gui.guiWidth();

        int mainColor = (alpha << 24) | mainRgb;
        int mainX = (w - font.width(main)) / 2;
        gui.drawString(font, main, mainX, 8, mainColor, true);

        if (sub != null) {
            int subColor = (alpha << 24) | 0xE8E8E8;
            int subX = (w - font.width(sub)) / 2;
            gui.drawString(font, sub, subX, 21, subColor, true);
        }
    }

    /** Слово "убийство/убийства/убийств" (ru) или "kill/kills" (остальные) по языку клиента. */
    private static String killWord(int n) {
        String lang = "";
        try {
            lang = Minecraft.getInstance().getLanguageManager().getSelected();
        } catch (Throwable ignored) {
        }
        if (lang != null && lang.startsWith("ru")) {
            int m10 = n % 10;
            int m100 = n % 100;
            if (m10 == 1 && m100 != 11) return "убийство";
            if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) return "убийства";
            return "убийств";
        }
        return n == 1 ? "kill" : "kills";
    }
}
