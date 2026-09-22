package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv;
import at.petrak.hexcasting.api.misc.ManaHelper;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Мана-пейринг: общий манапул двух игроков.
 * <ul>
 *   <li>Первый каст (A на B, 500 маны): обоим вешается {@code MANA_PAIRING} на 5с (100 тиков),
 *       в карте появляется pending {@code A <-> B}.</li>
 *   <li>Подтверждение (B на A в эти 5с, бесплатно): pending снимается,
 *       {@code MANA_PAIRING} убирается, обоим вешается бесконечный {@code MANA_PAIRED},
 *       пара регистрируется как активная.</li>
 *   <li>Общий пул = сумма: current = A+B, max = maxA+maxB. Траты идут из общего
 *       (сначала с кастера, остаток с партнёра). Реген обоих суммируется сам по себе.</li>
 *   <li>Upkeep 10 маны/с с общего (по 5 с каждого). Не хватило — распад.</li>
 *   <li>Разрыв: смерть, выход, руна разрыва, выход из ambit (32 блока / другое измерение),
 *       нет маны на upkeep, снятие маркера молоком.</li>
 *   <li>Только 1-на-1: уже спаренные или висящие в чужом pending — mishap (проверяется в Op).</li>
 * </ul>
 */
public final class ManaPairingHandler {
    /** 5 секунд ожидания подтверждения. */
    public static final int PENDING_TICKS = 100;
    /** Upkeep с общего пула в секунду (по 5 с каждого). */
    public static final double UPKEEP_MANA_PER_SECOND = 10.0;
    /** Радиус общего пула — ambit каста. Дальше — распад. */
    public static final double PAIR_RADIUS = PlayerBasedCastEnv.AMBIT_RADIUS;

    public record Pending(UUID initiatorId, UUID targetId, long expiryGameTime) {
    }

    /** Ключ неупорядоченной пары. */
    private static String pairKey(UUID a, UUID b) {
        return a.toString().compareTo(b.toString()) < 0 ? a + "|" + b : b + "|" + a;
    }

    private static final ConcurrentHashMap<String, Pending> PENDING = new ConcurrentHashMap<>();
    /** Двунаправленная карта активных пар: player -> partner. */
    private static final ConcurrentHashMap<UUID, UUID> PAIRED = new ConcurrentHashMap<>();

    private static int tickCounter = 0;

    private ManaPairingHandler() {
    }

    // ---------- запросы состояния ----------

    public static boolean isPaired(UUID id) {
        return PAIRED.containsKey(id);
    }

    public static UUID getPartner(UUID id) {
        return PAIRED.get(id);
    }

    public static boolean isPairedWith(UUID a, UUID b) {
        return b.equals(PAIRED.get(a));
    }

    public static Pending getPendingBetween(UUID a, UUID b) {
        return PENDING.get(pairKey(a, b));
    }

    public static boolean isInPending(UUID id) {
        for (Pending p : PENDING.values()) {
            if (p.initiatorId().equals(id) || p.targetId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBusy(UUID id) {
        return isPaired(id) || isInPending(id);
    }

    // ---------- pending ----------

    /** Первый каст: повесить ожидание на 5с обоим. */
    public static void beginRequest(ServerPlayer initiator, ServerPlayer target) {
        long expiry = initiator.level().getGameTime() + PENDING_TICKS;
        PENDING.put(pairKey(initiator.getUUID(), target.getUUID()),
                new Pending(initiator.getUUID(), target.getUUID(), expiry));
        initiator.addEffect(new MobEffectInstance(HexEffects.MANA_PAIRING, PENDING_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(HexEffects.MANA_PAIRING, PENDING_TICKS, 0, false, true, true));
        initiator.sendSystemMessage(Component.literal("§dМана-пейринг: ждём подтверждения от " + target.getGameProfile().getName() + " (5с)"));
        target.sendSystemMessage(Component.literal("§dМана-пейринг: " + initiator.getGameProfile().getName() + " предлагает общий пул! Прочти ту же руну с ним в стеке (5с)"));
    }

    /** Повторный каст инициатора — освежить таймер (бесплатно). */
    public static void refreshRequest(ServerPlayer initiator, ServerPlayer target) {
        long expiry = initiator.level().getGameTime() + PENDING_TICKS;
        PENDING.put(pairKey(initiator.getUUID(), target.getUUID()),
                new Pending(initiator.getUUID(), target.getUUID(), expiry));
        initiator.addEffect(new MobEffectInstance(HexEffects.MANA_PAIRING, PENDING_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(HexEffects.MANA_PAIRING, PENDING_TICKS, 0, false, true, true));
    }

    public static void clearPendingBetween(UUID a, UUID b) {
        PENDING.remove(pairKey(a, b));
    }

    // ---------- active ----------

    /** Подтверждение: снять pending, повесить paired-маркер, зарегистрировать пару. */
    public static void completePair(MinecraftServer server, UUID a, UUID b) {
        PENDING.remove(pairKey(a, b));
        ServerPlayer pa = server.getPlayerList().getPlayer(a);
        ServerPlayer pb = server.getPlayerList().getPlayer(b);
        if (pa != null) {
            pa.removeEffect(HexEffects.MANA_PAIRING);
            pa.addEffect(new MobEffectInstance(HexEffects.MANA_PAIRED,
                    MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        }
        if (pb != null) {
            pb.removeEffect(HexEffects.MANA_PAIRING);
            pb.addEffect(new MobEffectInstance(HexEffects.MANA_PAIRED,
                    MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
        }
        PAIRED.put(a, b);
        PAIRED.put(b, a);
        pushHudUpdate(server, a, b);
        if (pa != null) {
            pa.sendSystemMessage(Component.literal("§9Общий манапул активен! Upkeep 10м/с."));
        }
        if (pb != null) {
            pb.sendSystemMessage(Component.literal("§9Общий манапул активен! Upkeep 10м/с."));
        }
    }

    /** Разорвать пару (идемпотентно). Личные остатки маны остаются как есть. */
    public static void unpair(MinecraftServer server, UUID a, String reason) {
        UUID b = PAIRED.remove(a);
        if (b != null) {
            PAIRED.remove(b, a);
        }
        ServerPlayer pa = server != null ? server.getPlayerList().getPlayer(a) : null;
        ServerPlayer pb = (server != null && b != null) ? server.getPlayerList().getPlayer(b) : null;
        if (pa != null) {
            try {
                pa.removeEffect(HexEffects.MANA_PAIRED);
            } catch (Throwable ignored) {
            }
            pa.sendSystemMessage(Component.literal("§7Общий пул распался: " + reason));
        }
        if (pb != null) {
            try {
                pb.removeEffect(HexEffects.MANA_PAIRED);
            } catch (Throwable ignored) {
            }
            pb.sendSystemMessage(Component.literal("§7Общий пул распался: " + reason));
        }
        // Пакет сброса HUD придёт следующим тиком через clear клиента (unpair шлёт paired=false).
        if (server != null) {
            pushHudUpdate(server, a, b);
        }
    }

    public static void unpair(ServerPlayer player, String reason) {
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            unpair(server, player.getUUID(), reason);
        } else {
            UUID b = PAIRED.remove(player.getUUID());
            if (b != null) {
                PAIRED.remove(b, player.getUUID());
            }
            try {
                player.removeEffect(HexEffects.MANA_PAIRED);
            } catch (Throwable ignored) {
            }
        }
    }

    // ---------- общий пул: чтение/траты ----------

    public static double getSharedMana(ServerPlayer caster) {
        UUID partnerId = PAIRED.get(caster.getUUID());
        if (partnerId == null) {
            try {
                return ManaHelper.getMana(caster);
            } catch (Throwable t) {
                return 0;
            }
        }
        ServerPlayer partner = caster.level().getServer() != null
                ? caster.level().getServer().getPlayerList().getPlayer(partnerId) : null;
        if (partner == null) {
            try {
                return ManaHelper.getMana(caster);
            } catch (Throwable t) {
                return 0;
            }
        }
        double a = 0, b = 0;
        try {
            a = ManaHelper.getMana(caster);
        } catch (Throwable ignored) {
        }
        try {
            b = ManaHelper.getMana(partner);
        } catch (Throwable ignored) {
        }
        return a + b;
    }

    public static double getSharedMax(ServerPlayer caster) {
        UUID partnerId = PAIRED.get(caster.getUUID());
        if (partnerId == null) {
            try {
                return ManaHelper.maxMana(caster);
            } catch (Throwable t) {
                return 0;
            }
        }
        ServerPlayer partner = caster.level().getServer() != null
                ? caster.level().getServer().getPlayerList().getPlayer(partnerId) : null;
        if (partner == null) {
            try {
                return ManaHelper.maxMana(caster);
            } catch (Throwable t) {
                return 0;
            }
        }
        double a = 0, b = 0;
        try {
            a = ManaHelper.maxMana(caster);
        } catch (Throwable ignored) {
        }
        try {
            b = ManaHelper.maxMana(partner);
        } catch (Throwable ignored) {
        }
        return a + b;
    }

    /**
     * Списать manaCost маны из общего пула (сначала с кастера, остаток с партнёра).
     * @return true если хватило и списание выполнено.
     */
    public static boolean spendShared(ServerPlayer caster, double manaCost) {
        UUID partnerId = PAIRED.get(caster.getUUID());
        if (partnerId == null) {
            return false;
        }
        MinecraftServer server = caster.level().getServer();
        if (server == null) {
            return false;
        }
        ServerPlayer partner = server.getPlayerList().getPlayer(partnerId);
        if (partner == null || !partner.isAlive()) {
            return false;
        }
        double manaA = 0, manaB = 0;
        try {
            manaA = ManaHelper.getMana(caster);
        } catch (Throwable ignored) {
        }
        try {
            manaB = ManaHelper.getMana(partner);
        } catch (Throwable ignored) {
        }
        if (manaA + manaB < manaCost) {
            return false;
        }
        double takeA = Math.min(manaA, manaCost);
        double rest = manaCost - takeA;
        try {
            ManaHelper.setMana(caster, manaA - takeA);
        } catch (Throwable ignored) {
        }
        if (rest > 0) {
            try {
                ManaHelper.setMana(partner, manaB - rest);
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    // ---------- тики и события ----------

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return; // один прогон в тик
        }
        int tick = ++tickCounter;
        MinecraftServer server = level.getServer();
        if (tick % 20 == 0) {
            tickPending(server);
            tickActive(server);
        }
    }

    private static void tickPending(MinecraftServer server) {
        if (PENDING.isEmpty() || server == null) {
            return;
        }
        long now = server.overworld().getGameTime();
        for (var e : new ArrayList<>(PENDING.entrySet())) {
            Pending p = e.getValue();
            ServerPlayer initiator = server.getPlayerList().getPlayer(p.initiatorId());
            ServerPlayer target = server.getPlayerList().getPlayer(p.targetId());
            boolean expired = now >= p.expiryGameTime();
            boolean gone = initiator == null || target == null || !initiator.isAlive() || !target.isAlive();
            boolean milked = false;
            if (!gone) {
                milked = !initiator.hasEffect(HexEffects.MANA_PAIRING) || !target.hasEffect(HexEffects.MANA_PAIRING);
                // Эффект могли снять молоком досрочно — pending больше не валиден.
                // Но в тик сразу после begin эффект уже есть; защита от ложного срабатывания:
                // молоком считаем только если до expiry осталось < PENDING_TICKS - 20 (т.е. не первый тик).
                if (milked && now < p.expiryGameTime() - (PENDING_TICKS - 20)) {
                    milked = false;
                }
            }
            if (expired || gone || milked) {
                PENDING.remove(e.getKey(), p);
                if (!gone) {
                    try {
                        initiator.removeEffect(HexEffects.MANA_PAIRING);
                    } catch (Throwable ignored) {
                    }
                    try {
                        target.removeEffect(HexEffects.MANA_PAIRING);
                    } catch (Throwable ignored) {
                    }
                    if (expired) {
                        initiator.sendSystemMessage(Component.literal("§7Мана-пейринг истёк (нет подтверждения за 5с)"));
                        target.sendSystemMessage(Component.literal("§7Мана-пейринг истёк (нет подтверждения за 5с)"));
                    }
                }
            }
        }
    }

    private static void tickActive(MinecraftServer server) {
        if (PAIRED.isEmpty() || server == null) {
            return;
        }
        for (var e : new ArrayList<>(PAIRED.entrySet())) {
            UUID a = e.getKey();
            UUID b = e.getValue();
            // Обрабатываем каждую неупорядоченную пару один раз.
            if (a.toString().compareTo(b.toString()) > 0) {
                continue;
            }
            ServerPlayer pa = server.getPlayerList().getPlayer(a);
            ServerPlayer pb = server.getPlayerList().getPlayer(b);
            if (pa == null || pb == null) {
                unpair(server, a, "игрок вышел");
                continue;
            }
            if (!pa.isAlive() || !pb.isAlive()) {
                unpair(server, a, "смерть");
                continue;
            }
            if (!pa.hasEffect(HexEffects.MANA_PAIRED) || !pb.hasEffect(HexEffects.MANA_PAIRED)) {
                unpair(server, a, "маркер снят (молоко?)");
                continue;
            }
            if (!pa.level().dimension().equals(pb.level().dimension())) {
                unpair(server, a, "разные измерения");
                continue;
            }
            double distSqr = pa.distanceToSqr(pb);
            if (distSqr > PAIR_RADIUS * PAIR_RADIUS) {
                unpair(server, a, "вышли из области каста (>32)");
                continue;
            }
            double manaA = 0, manaB = 0;
            try {
                manaA = ManaHelper.getMana(pa);
            } catch (Throwable ignored) {
            }
            try {
                manaB = ManaHelper.getMana(pb);
            } catch (Throwable ignored) {
            }
            if (manaA + manaB < UPKEEP_MANA_PER_SECOND) {
                unpair(server, a, "нет маны на upkeep 10м/с");
                continue;
            }
            // Upkeep 10 с общего: по 5 с каждого (с переносом остатка на партнёра).
            double needA = UPKEEP_MANA_PER_SECOND / 2.0;
            double needB = UPKEEP_MANA_PER_SECOND / 2.0;
            double takeA = Math.min(manaA, needA);
            double takeB = Math.min(manaB, needB);
            double shortA = needA - takeA;
            double shortB = needB - takeB;
            // Покрываем недостачу за счёт партнёра (сумма проверена выше — хватит).
            takeB = Math.min(manaB, needB + shortA);
            takeA = Math.min(manaA, needA + shortB);
            // Пересчёт чтобы сумма была ровно UPKEEP (защита от округлений):
            double sum = takeA + takeB;
            if (sum < UPKEEP_MANA_PER_SECOND) {
                // Добираем копейки с того у кого больше осталось.
                if (manaA - takeA >= manaB - takeB) {
                    takeA = Math.min(manaA, UPKEEP_MANA_PER_SECOND - takeB);
                } else {
                    takeB = Math.min(manaB, UPKEEP_MANA_PER_SECOND - takeA);
                }
            }
            try {
                ManaHelper.setMana(pa, manaA - takeA);
            } catch (Throwable ignored) {
            }
            try {
                ManaHelper.setMana(pb, manaB - takeB);
            } catch (Throwable ignored) {
            }
            pushHudUpdate(server, a, b);
        }
    }

    private static void pushHudUpdate(MinecraftServer server, UUID a, UUID b) {
        try {
            ServerPlayer pa = a != null ? server.getPlayerList().getPlayer(a) : null;
            ServerPlayer pb = b != null ? server.getPlayerList().getPlayer(b) : null;
            if (pa != null && PAIRED.containsKey(a)) {
                UUID partner = PAIRED.get(a);
                ServerPlayer pp = partner != null ? server.getPlayerList().getPlayer(partner) : null;
                double shared = 0, max = 1;
                try {
                    shared = ManaHelper.getMana(pa) + (pp != null ? ManaHelper.getMana(pp) : 0);
                    max = ManaHelper.maxMana(pa) + (pp != null ? ManaHelper.maxMana(pp) : 0);
                } catch (Throwable ignored) {
                }
                at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                        pa, new me.nanorasmus.nanodev.hex_js.network.MsgManaPairS2C(true, shared, max));
            } else if (pa != null) {
                at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                        pa, new me.nanorasmus.nanodev.hex_js.network.MsgManaPairS2C(false, 0, 1));
            }
            if (pb != null && b != null && PAIRED.containsKey(b)) {
                UUID partner = PAIRED.get(b);
                ServerPlayer pp = partner != null ? server.getPlayerList().getPlayer(partner) : null;
                double shared = 0, max = 1;
                try {
                    shared = ManaHelper.getMana(pb) + (pp != null ? ManaHelper.getMana(pp) : 0);
                    max = ManaHelper.maxMana(pb) + (pp != null ? ManaHelper.maxMana(pp) : 0);
                } catch (Throwable ignored) {
                }
                at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                        pb, new me.nanorasmus.nanodev.hex_js.network.MsgManaPairS2C(true, shared, max));
            } else if (pb != null) {
                at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                        pb, new me.nanorasmus.nanodev.hex_js.network.MsgManaPairS2C(false, 0, 1));
            }
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        UUID id = player.getUUID();
        if (PAIRED.containsKey(id)) {
            unpair(server, id, "смерть");
        }
        // Чистим pending с участием умершего.
        for (var e : new ArrayList<>(PENDING.entrySet())) {
            Pending p = e.getValue();
            if (p.initiatorId().equals(id) || p.targetId().equals(id)) {
                PENDING.remove(e.getKey(), p);
                UUID other = p.initiatorId().equals(id) ? p.targetId() : p.initiatorId();
                ServerPlayer po = server.getPlayerList().getPlayer(other);
                if (po != null) {
                    try {
                        po.removeEffect(HexEffects.MANA_PAIRING);
                    } catch (Throwable ignored) {
                    }
                    po.sendSystemMessage(Component.literal("§7Мана-пейринг отменён: второй кастер умер"));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            // Сервер уже останавливается — чистим молча.
            PAIRED.remove(player.getUUID());
            return;
        }
        UUID id = player.getUUID();
        if (PAIRED.containsKey(id)) {
            unpair(server, id, "выход из игры");
        }
        for (var e : new ArrayList<>(PENDING.entrySet())) {
            Pending p = e.getValue();
            if (p.initiatorId().equals(id) || p.targetId().equals(id)) {
                PENDING.remove(e.getKey(), p);
                UUID other = p.initiatorId().equals(id) ? p.targetId() : p.initiatorId();
                ServerPlayer po = server.getPlayerList().getPlayer(other);
                if (po != null) {
                    try {
                        po.removeEffect(HexEffects.MANA_PAIRING);
                    } catch (Throwable ignored) {
                    }
                    po.sendSystemMessage(Component.literal("§7Мана-пейринг отменён: второй кастер вышел"));
                }
            }
        }
    }
}
