package me.nanorasmus.nanodev.hex_js.casting;

import me.nanorasmus.nanodev.hex_js.addon.HexArtifactsItems;
import me.nanorasmus.nanodev.hex_js.helpers.CurioHelper;
import me.nanorasmus.nanodev.hex_js.network.MsgAnnounceTextS2C;
import me.nanorasmus.nanodev.hex_js.network.MsgKillFlashS2C;
import me.nanorasmus.nanodev.hex_js.sound.HexSounds;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Комментатор в стиле Доты: если убийца носит {@code meowhex:commentator}
 * в Curios-слоте necklace, PvP-убийство анонсируется звуком, боссбаром
 * и красной вспышкой виньетки у убийцы.
 *
 * <ul>
 *   <li>Только PvP: жертва и убийца — игроки, суициды не анонсируются
 *       (стрик жертвы при этом сгорает).</li>
 *   <li>Только стрик без смертей: spree/dominating/unstoppable/wicked/
 *       monster/godlike/rampage/holy_shit (+ first blood за первый фраг).</li>
 *   <li>В чат не пишется ничего; уровень рампаги и выше показывает
 *       наверх «Killer Буйствует!», остальные анонсы — просто текстом
 *       на месте боссбара (верх экрана, без полосы, на ~3 секунды).</li>
 *   <li>Аудитория — игроки в радиусе {@link #BROADCAST_RADIUS} блоков
 *       от убийцы; события уровня godlike и выше — весь сервер.</li>
 * </ul>
 */
public final class AnnouncerHandler {
    /** Радиус слышимости анонса от позиции убийцы, в блоках. */
    public static final double BROADCAST_RADIUS = 200.0;
    /** Окно мультиубийств: повторное убийство за 10 секунд продолжает серию. */
    public static final long MULTI_WINDOW_TICKS = 200L;
    /** Приоритет названия в тексте (на случай совпадений). */
    private static final List<String> DISPLAY_PRIORITY = List.of(
            "rampage", "holy_shit", "godlike",
            "monster_kill", "wicked_sick", "unstoppable", "dominating",
            "killing_spree", "first_blood");

    private static final ConcurrentHashMap<UUID, Integer> STREAK = new ConcurrentHashMap<>();
    private static boolean firstBloodDone = false;

    private AnnouncerHandler() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        firstBloodDone = false;
        STREAK.clear();
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        UUID victimId = victim.getUUID();

        // Любая смерть сбрасывает стрик жертвы.
        int victimStreak = STREAK.getOrDefault(victimId, 0);
        STREAK.remove(victimId);

        Entity rawKiller = event.getSource().getEntity();
        if (!(rawKiller instanceof ServerPlayer killer) || killer.getUUID().equals(victimId)) {
            // Не-PvP смерть или суицид: только shutdown при серии > 3.
            if (victimStreak > 3 && victim.level() instanceof ServerLevel victimLevel) {
                announceShutdown(victimLevel, victim, victimStreak);
            }
            return;
        }
        if (!(killer.level() instanceof ServerLevel level)) return;
        if (!killer.isAlive()) return;

        UUID killerId = killer.getUUID();

        // Без надетого комментатора — тишина.
        try {
            if (!CurioHelper.hasCurio(killer, HexArtifactsItems.COMMENTATOR.get())) return;
        } catch (Throwable ignored) {
            return;
        }

        int streak = STREAK.getOrDefault(killerId, 0) + 1;
        STREAK.put(killerId, streak);

        // Только стрик (+ first blood за первый фраг).
        Set<String> ids = new LinkedHashSet<>();
        if (!firstBloodDone) {
            firstBloodDone = true;
            ids.add("first_blood");
        } else if (streak == 3) {
            ids.add("killing_spree");
        } else if (streak == 4) {
            ids.add("dominating");
        } else if (streak == 5) {
            ids.add("unstoppable");
        } else if (streak == 6) {
            ids.add("wicked_sick");
        } else if (streak == 7) {
            ids.add("monster_kill");
        } else if (streak == 8) {
            ids.add("rampage");
        } else if (streak == 9) {
            ids.add("godlike");
        } else if (streak >= 10) {
            ids.add("holy_shit");
        }
        boolean global = ids.contains("rampage") || ids.contains("holy_shit") || ids.contains("godlike");

        List<SoundEvent> sounds = new ArrayList<>();
        for (String id : ids) {
            sounds.add(soundFor(id));
        }

        MinecraftServer server = level.getServer();
        List<ServerPlayer> audience = audience(server, level, killer, global);

        // В тексте — одно событие (больше одного не бывает).
        String displayId = "";
        for (String cand : DISPLAY_PRIORITY) {
            if (ids.contains(cand)) {
                displayId = cand;
                break;
            }
        }
        String killerName = killer.getGameProfile().getName();
        String victimName = victim.getGameProfile().getName();
        String shutName = victimStreak > 3 ? victimName : "";
        int shutStreak = victimStreak > 3 ? victimStreak : 0;
        // HOLLY SHIT! X2, X3... с 11-го фрага (10-й — без множителя).
        int mult = "holy_shit".equals(displayId) ? streak - 9 : 0;
        var xplat = at.petrak.hexcasting.xplat.IXplatAbstractions.INSTANCE;
        for (ServerPlayer listener : audience) {
            try {
                xplat.sendPacketToPlayer(listener, new MsgAnnounceTextS2C(
                        killerName, victimName, displayId, shutName, shutStreak, mult));
            } catch (Throwable ignored) {
            }
        }
        // Звук — один раз на позицию убийцы для всех рядом.
        // Раньше играл в цикле по слушателям через level.playSound (broadcast),
        // из-за чего каждый слышал его N раз. Глобальные (godlike+) — тоже один раз.
        for (SoundEvent sound : sounds) {
            level.playSound(null, killer.getX(), killer.getY(), killer.getZ(),
                    sound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // Красная вспышка у убийцы своим оверлеем (без стен рамки).
        try {
            at.petrak.hexcasting.xplat.IXplatAbstractions.INSTANCE.sendPacketToPlayer(killer, new MsgKillFlashS2C());
        } catch (Throwable ignored) {
        }
    }

    private static SoundEvent soundFor(String eventId) {
        return switch (eventId) {
            case "dominating" -> HexSounds.ANNOUNCER_DOMINATING.get();
            case "first_blood" -> HexSounds.ANNOUNCER_FIRST_BLOOD.get();
            case "godlike" -> HexSounds.ANNOUNCER_GODLIKE.get();
            case "holy_shit" -> HexSounds.ANNOUNCER_HOLY_SHIT.get();
            case "killing_spree" -> HexSounds.ANNOUNCER_KILLING_SPREE.get();
            case "monster_kill" -> HexSounds.ANNOUNCER_MONSTER_KILL.get();
            case "rampage" -> HexSounds.ANNOUNCER_RAMPAGE.get();
            case "unstoppable" -> HexSounds.ANNOUNCER_UNSTOPPABLE.get();
            case "wicked_sick" -> HexSounds.ANNOUNCER_WICKED_SICK.get();
            default -> throw new IllegalArgumentException("Unknown announcer event: " + eventId);
        };
    }

    /** Слушатели: весь сервер для rampage-уровня, иначе игроки рядом с убийцей. */
    private static List<ServerPlayer> audience(MinecraftServer server, ServerLevel level,
            ServerPlayer killer, boolean global) {
        if (global) {
            List<ServerPlayer> all = new ArrayList<>();
            for (ServerLevel sl : server.getAllLevels()) {
                all.addAll(sl.players());
            }
            return all;
        }
        double kx = killer.getX();
        double ky = killer.getY();
        double kz = killer.getZ();
        double r2 = BROADCAST_RADIUS * BROADCAST_RADIUS;
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer listener : level.players()) {
            if (listener.distanceToSqr(kx, ky, kz) <= r2) {
                near.add(listener);
            }
        }
        return near;
    }

    /** Конец чужой серии: смерть от мобов/падения/суицид при стрике > 3. */
    private static void announceShutdown(ServerLevel level, ServerPlayer victim, int streak) {
        String victimName = victim.getGameProfile().getName();
        var xplat = at.petrak.hexcasting.xplat.IXplatAbstractions.INSTANCE;
        var packet = new MsgAnnounceTextS2C("", "", "", victimName, streak, 0);
        double r2 = BROADCAST_RADIUS * BROADCAST_RADIUS;
        for (ServerPlayer listener : level.players()) {
            if (listener.distanceToSqr(victim.getX(), victim.getY(), victim.getZ()) > r2) continue;
            try {
                xplat.sendPacketToPlayer(listener, packet);
            } catch (Throwable ignored) {
            }
        }
    }
}
