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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Мана-пейринг: общий манапул группы до 10 игроков.
 * <ul>
 *   <li>Первый каст (A на B, 500 маны): обоим вешается {@code MANA_PAIRING} на 5с (100 тиков),
 *       в карте появляется pending {@code A <-> B} с привязкой к группе (если одна из сторон уже в группе).</li>
 *   <li>Подтверждение (в эти 5с, бесплатно): новичок кастует на ЛЮБОГО участника группы
 *       (или любой участник группы на новичка) — группы сливаются (union), всем вешается
 *       бесконечный {@code MANA_PAIRED}.</li>
 *   <li>Общий пул = сумма current и max всех участников. Траты идут из общего
 *       (сначала с кастера, остаток с остальных). Реген каждого суммируется сам по себе.</li>
 *   <li>Upkeep = (n-1) * 10 маны/с с общего пула. 2 игрока — 10, 3 — 20, ... 10 — 90.</li>
 *   <li>Разрыв = роспуск ВСЕЙ группы: смерть/выход любого, выход любого из ambit (32 блока) /
 *       другое измерение, нет маны на upkeep, снятие маркера молоком у любого,
 *       руна разрыва или повтор той же руной по своему.</li>
 *   <li>Пока игрок в группе — над головой hex-частицы (Conjure, синие) раз в секунду.</li>
 * </ul>
 */
public final class ManaPairingHandler {
    /** 5 секунд ожидания подтверждения. */
    public static final int PENDING_TICKS = 100;
    /** Максимум игроков в одном общем пуле. */
    public static final int MAX_GROUP_SIZE = 10;
    /** Плата за каждую связь: upkeep = (n-1) * UPKEEP_PER_LINK. */
    public static final double UPKEEP_PER_LINK = 10.0;
    /** Радиус общего пула — ambit каста. Дальше — роспуск. */
    public static final double PAIR_RADIUS = PlayerBasedCastEnv.AMBIT_RADIUS;
    /** Цвет виспа над головой (синий, как MANA_PAIRED). */
    private static final int WISP_COLOR = 0x3F8FFF;

    public record Pending(UUID initiatorId, UUID targetId, UUID groupId, UUID soloId, long expiryGameTime) {
    }

    /** Ключ неупорядоченной пары (для точного pending A<->B). */
    private static String pairKey(UUID a, UUID b) {
        return a.toString().compareTo(b.toString()) < 0 ? a + "|" + b : b + "|" + a;
    }

    private static final ConcurrentHashMap<String, Pending> PENDING = new ConcurrentHashMap<>();
    /** groupId -> участники. */
    private static final ConcurrentHashMap<UUID, Set<UUID>> GROUPS = new ConcurrentHashMap<>();
    /** player -> groupId. */
    private static final ConcurrentHashMap<UUID, UUID> MEMBER_TO_GROUP = new ConcurrentHashMap<>();

    private static int tickCounter = 0;

    private ManaPairingHandler() {
    }

    /** Upkeep в секунду для группы размера n. */
    public static double upkeepForSize(int n) {
        if (n <= 1) {
            return 0;
        }
        return (n - 1) * UPKEEP_PER_LINK;
    }

    // ---------- запросы состояния ----------

    public static boolean isPaired(UUID id) {
        return MEMBER_TO_GROUP.containsKey(id);
    }

    /** Совместимость: первый другой участник группы (или null). */
    public static UUID getPartner(UUID id) {
        UUID gid = MEMBER_TO_GROUP.get(id);
        if (gid == null) {
            return null;
        }
        Set<UUID> members = GROUPS.get(gid);
        if (members == null) {
            return null;
        }
        for (UUID m : members) {
            if (!m.equals(id)) {
                return m;
            }
        }
        return null;
    }

    public static boolean isPairedWith(UUID a, UUID b) {
        if (a.equals(b)) {
            return false;
        }
        UUID ga = MEMBER_TO_GROUP.get(a);
        UUID gb = MEMBER_TO_GROUP.get(b);
        return ga != null && ga.equals(gb);
    }

    public static UUID getGroupId(UUID id) {
        return MEMBER_TO_GROUP.get(id);
    }

    public static Set<UUID> getGroupMembers(UUID id) {
        UUID gid = MEMBER_TO_GROUP.get(id);
        if (gid == null) {
            return Collections.emptySet();
        }
        Set<UUID> members = GROUPS.get(gid);
        if (members == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(members));
    }

    public static int getGroupSize(UUID id) {
        UUID gid = MEMBER_TO_GROUP.get(id);
        if (gid == null) {
            return 1;
        }
        Set<UUID> members = GROUPS.get(gid);
        return members == null ? 1 : members.size();
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

    private static boolean isMemberOfGroup(UUID groupId, UUID player) {
        if (groupId == null) {
            return false;
        }
        Set<UUID> members = GROUPS.get(groupId);
        return members != null && members.contains(player);
    }

    /**
     * Pending, который каст caster->target может ЗАВЕРШИТЬ (слияние).
     * Учитывает подтверждение на любого участника группы.
     */
    public static Pending findCompletablePending(UUID caster, UUID target) {
        // Точное реверсное pending (оба соло или тот же инициатор/таргет).
        Pending exact = PENDING.get(pairKey(caster, target));
        if (exact != null && exact.initiatorId().equals(target) && exact.targetId().equals(caster)) {
            return exact;
        }
        // Групповое: соло подтверждает на любого участника группы (и наоборот).
        for (Pending p : PENDING.values()) {
            if (p.groupId() == null) {
                continue;
            }
            UUID solo = p.soloId();
            UUID gid = p.groupId();
            if (solo == null) {
                continue;
            }
            boolean casterIsSolo = caster.equals(solo);
            boolean targetIsSolo = target.equals(solo);
            boolean casterInGroup = isMemberOfGroup(gid, caster);
            boolean targetInGroup = isMemberOfGroup(gid, target);
            if (casterIsSolo && targetInGroup) {
                // Соло -> группа. Complete только если изначально приглашала группа (G->S).
                // Если соло само инициировало (S->G), это refresh, не complete.
                if (!p.initiatorId().equals(solo)) {
                    return p;
                }
            } else if (casterInGroup && targetIsSolo) {
                // Группа -> соло. Complete только если изначально звало соло (S->G).
                if (p.initiatorId().equals(solo)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Pending, который каст caster->target может ОСВЕЖИТЬ (продлить 5с).
     * Точное same-direction + групповое same-direction на любого участника.
     */
    public static Pending findRefreshablePending(UUID caster, UUID target) {
        Pending exact = PENDING.get(pairKey(caster, target));
        if (exact != null && exact.initiatorId().equals(caster) && exact.targetId().equals(target)) {
            return exact;
        }
        for (Pending p : PENDING.values()) {
            if (p.groupId() == null) {
                continue;
            }
            UUID solo = p.soloId();
            UUID gid = p.groupId();
            if (solo == null) {
                continue;
            }
            boolean casterIsSolo = caster.equals(solo);
            boolean targetIsSolo = target.equals(solo);
            boolean casterInGroup = isMemberOfGroup(gid, caster);
            boolean targetInGroup = isMemberOfGroup(gid, target);
            if (casterIsSolo && targetInGroup && p.initiatorId().equals(solo)) {
                return p; // S->G: соло освежает зов на любого участника
            } else if (casterInGroup && targetIsSolo && !p.initiatorId().equals(solo)) {
                return p; // G->S: любой участник освежает зов на соло
            }
        }
        return null;
    }

    // ---------- pending ----------

    /** Первый каст: повесить ожидание на 5с. Соло-стороне — маркер, группе — нет (у неё уже MANA_PAIRED). */
    public static void beginRequest(ServerPlayer initiator, ServerPlayer target) {
        UUID gidA = MEMBER_TO_GROUP.get(initiator.getUUID());
        UUID gidB = MEMBER_TO_GROUP.get(target.getUUID());
        UUID gid = gidA != null ? gidA : gidB;
        UUID solo = null;
        if (gid != null) {
            solo = gidA != null ? target.getUUID() : initiator.getUUID();
        }
        long expiry = initiator.level().getGameTime() + PENDING_TICKS;
        PENDING.put(pairKey(initiator.getUUID(), target.getUUID()),
                new Pending(initiator.getUUID(), target.getUUID(), gid, solo, expiry));
        applyPairingMarker(initiator);
        applyPairingMarker(target);
        if (gid != null) {
            int size = groupSizeOf(gid);
            initiator.sendSystemMessage(Component.literal("§dМана-пейринг: ждём подтверждения от "
                    + target.getGameProfile().getName() + " в группу " + (size + 1) + "/" + MAX_GROUP_SIZE + " (5с)"));
            target.sendSystemMessage(Component.literal("§dМана-пейринг: " + initiator.getGameProfile().getName()
                    + " зовёт в общий пул (" + (size + 1) + "/" + MAX_GROUP_SIZE + ")! Прочти ту же руну с любым участником в стеке (5с)"));
        } else {
            initiator.sendSystemMessage(Component.literal("§dМана-пейринг: ждём подтверждения от " + target.getGameProfile().getName() + " (5с)"));
            target.sendSystemMessage(Component.literal("§dМана-пейринг: " + initiator.getGameProfile().getName() + " предлагает общий пул! Прочти ту же руну с ним в стеке (5с)"));
        }
    }

    /** Повторный каст — освежить таймер (бесплатно). Работает и через любого участника группы. */
    public static void refreshRequest(ServerPlayer initiator, ServerPlayer target) {
        UUID a = initiator.getUUID();
        UUID b = target.getUUID();
        // Точное same-direction pending — обновляем его.
        Pending old = getPendingBetween(a, b);
        if (old != null && old.initiatorId().equals(a) && old.targetId().equals(b)) {
            long expiry = initiator.level().getGameTime() + PENDING_TICKS;
            PENDING.put(pairKey(old.initiatorId(), old.targetId()),
                    new Pending(old.initiatorId(), old.targetId(), old.groupId(), old.soloId(), expiry));
            applyPairingMarker(initiator);
            applyPairingMarker(target);
            return;
        }
        // Групповой refresh от другого участника — продлеваем ИСХОДНЫЙ pending, новый ключ не создаём.
        Pending found = findRefreshablePending(a, b);
        if (found != null) {
            long expiry = initiator.level().getGameTime() + PENDING_TICKS;
            PENDING.put(pairKey(found.initiatorId(), found.targetId()),
                    new Pending(found.initiatorId(), found.targetId(), found.groupId(), found.soloId(), expiry));
            applyPairingMarker(initiator);
            applyPairingMarker(target);
            // Освежаем маркер и у исходной пары, чтобы pending не сочли снятым молоком.
            ServerPlayer origI = initiator.level().getServer() != null
                    ? initiator.level().getServer().getPlayerList().getPlayer(found.initiatorId()) : null;
            ServerPlayer origT = initiator.level().getServer() != null
                    ? initiator.level().getServer().getPlayerList().getPlayer(found.targetId()) : null;
            if (origI != null) {
                applyPairingMarker(origI);
            }
            if (origT != null) {
                applyPairingMarker(origT);
            }
            return;
        }
        // Fallback (не должно случаться): создаём как новый.
        UUID gidA = MEMBER_TO_GROUP.get(a);
        UUID gidB = MEMBER_TO_GROUP.get(b);
        UUID gid = gidA != null ? gidA : gidB;
        UUID solo = null;
        if (gid != null && (gidA != null || gidB != null)) {
            if (gidA == null && gidB == null) {
                gid = null;
            } else {
                solo = gidA != null ? b : a;
            }
        }
        long expiry = initiator.level().getGameTime() + PENDING_TICKS;
        PENDING.put(pairKey(a, b), new Pending(a, b, gid, solo, expiry));
        applyPairingMarker(initiator);
        applyPairingMarker(target);
    }

    private static void applyPairingMarker(ServerPlayer p) {
        // Участники активной группы уже имеют MANA_PAIRED — второй маркер им не нужен.
        if (MEMBER_TO_GROUP.containsKey(p.getUUID())) {
            return;
        }
        p.addEffect(new MobEffectInstance(HexEffects.MANA_PAIRING, PENDING_TICKS, 0, false, true, true));
    }

    public static void clearPendingBetween(UUID a, UUID b) {
        PENDING.remove(pairKey(a, b));
    }

    // ---------- active ----------

    private static int groupSizeOf(UUID gid) {
        Set<UUID> m = GROUPS.get(gid);
        return m == null ? 0 : m.size();
    }

    /**
     * Подтверждение: снять pending, повесить paired-маркер всем, слить группы (union).
     * Вызывается и для пары, и для вступления в группу.
     */
    public static void completePair(MinecraftServer server, UUID a, UUID b) {
        Pending exact = PENDING.get(pairKey(a, b));
        Pending p = exact;
        if (p == null) {
            p = findCompletablePending(a, b);
        }
        if (p == null) {
            // Нет валидного pending — ничего не делаем.
            return;
        }
        // Снимаем именно тот pending, что завершаем.
        PENDING.remove(pairKey(p.initiatorId(), p.targetId()), p);

        // Собираем union: группы обеих сторон + обе стороны.
        Set<UUID> union = new HashSet<>();
        UUID gidA = MEMBER_TO_GROUP.get(a);
        UUID gidB = MEMBER_TO_GROUP.get(b);
        if (p.groupId() != null) {
            Set<UUID> gm = GROUPS.get(p.groupId());
            if (gm != null) {
                union.addAll(gm);
            }
        }
        if (gidA != null) {
            Set<UUID> gm = GROUPS.get(gidA);
            if (gm != null) {
                union.addAll(gm);
            }
        }
        if (gidB != null) {
            Set<UUID> gm = GROUPS.get(gidB);
            if (gm != null) {
                union.addAll(gm);
            }
        }
        union.add(a);
        union.add(b);
        // Соло-участник pending тоже входит (на случай confirm через другого мембера).
        if (p.soloId() != null) {
            union.add(p.soloId());
        }
        union.add(p.initiatorId());
        union.add(p.targetId());

        if (union.size() > MAX_GROUP_SIZE) {
            sendToAll(server, union, "§cМана-пейринг: группа полная (макс " + MAX_GROUP_SIZE + ")");
            return;
        }

        // Убираем старые группы целиком (их участники входят в новую).
        if (gidA != null) {
            dropGroup(gidA);
        }
        if (gidB != null && !gidB.equals(gidA)) {
            dropGroup(gidB);
        }
        if (p.groupId() != null && !p.groupId().equals(gidA) && !p.groupId().equals(gidB)) {
            dropGroup(p.groupId());
        }

        UUID newGid = UUID.randomUUID();
        Set<UUID> set = ConcurrentHashMap.newKeySet();
        set.addAll(union);
        GROUPS.put(newGid, set);
        for (UUID m : union) {
            MEMBER_TO_GROUP.put(m, newGid);
        }
        // Чистим другие pending внутри новой группы (чтобы не висели).
        for (var e : new ArrayList<>(PENDING.entrySet())) {
            Pending q = e.getValue();
            if (union.contains(q.initiatorId()) && union.contains(q.targetId())) {
                PENDING.remove(e.getKey(), q);
            }
        }
        double upkeep = upkeepForSize(union.size());
        for (UUID m : union) {
            ServerPlayer pm = server.getPlayerList().getPlayer(m);
            if (pm != null) {
                try {
                    pm.removeEffect(HexEffects.MANA_PAIRING);
                } catch (Throwable ignored) {
                }
                pm.addEffect(new MobEffectInstance(HexEffects.MANA_PAIRED,
                        MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
                pm.sendSystemMessage(Component.literal("§9Общий манапул активен (" + union.size() + "/" + MAX_GROUP_SIZE
                        + ")! Upkeep " + (int) upkeep + "м/с."));
            }
        }
        pushHudUpdate(server, union);
    }

    private static void sendToAll(MinecraftServer server, Set<UUID> members, String text) {
        for (UUID m : members) {
            ServerPlayer pm = server.getPlayerList().getPlayer(m);
            if (pm != null) {
                pm.sendSystemMessage(Component.literal(text));
            }
        }
    }

    private static void dropGroup(UUID gid) {
        Set<UUID> members = GROUPS.remove(gid);
        if (members != null) {
            for (UUID m : members) {
                MEMBER_TO_GROUP.remove(m, gid);
            }
        }
    }

    /** Роспуск ВСЕЙ группы, в которой состоит a (идемпотентно). */
    public static void unpair(MinecraftServer server, UUID a, String reason) {
        UUID gid = MEMBER_TO_GROUP.get(a);
        if (gid == null) {
            if (server != null) {
                pushHudUpdate(server, a, null);
            }
            return;
        }
        Set<UUID> members = GROUPS.remove(gid);
        if (members == null) {
            members = new HashSet<>();
            members.add(a);
        }
        for (UUID m : new HashSet<>(members)) {
            MEMBER_TO_GROUP.remove(m, gid);
        }
        for (UUID m : members) {
            ServerPlayer pm = server != null ? server.getPlayerList().getPlayer(m) : null;
            if (pm != null) {
                try {
                    pm.removeEffect(HexEffects.MANA_PAIRED);
                } catch (Throwable ignored) {
                }
                pm.sendSystemMessage(Component.literal("§7Общий пул распался: " + reason));
            }
        }
        // Убираем pending, привязанные к распущенной группе.
        for (var e : new ArrayList<>(PENDING.entrySet())) {
            Pending q = e.getValue();
            if (gid.equals(q.groupId())) {
                PENDING.remove(e.getKey(), q);
            }
        }
        if (server != null) {
            pushHudUpdate(server, members);
        }
    }

    public static void unpair(ServerPlayer player, String reason) {
        MinecraftServer server = player.level().getServer();
        if (server != null) {
            unpair(server, player.getUUID(), reason);
        } else {
            UUID gid = MEMBER_TO_GROUP.remove(player.getUUID());
            if (gid != null) {
                Set<UUID> members = GROUPS.get(gid);
                if (members != null) {
                    members.remove(player.getUUID());
                    if (members.size() <= 1) {
                        dropGroup(gid);
                    }
                }
            }
            try {
                player.removeEffect(HexEffects.MANA_PAIRED);
            } catch (Throwable ignored) {
            }
        }
    }

    // ---------- общий пул: чтение/траты ----------

    private static Set<UUID> membersOf(ServerPlayer caster) {
        UUID gid = MEMBER_TO_GROUP.get(caster.getUUID());
        if (gid == null) {
            return Collections.emptySet();
        }
        Set<UUID> members = GROUPS.get(gid);
        if (members == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(members);
    }

    public static double getSharedMana(ServerPlayer caster) {
        Set<UUID> members = membersOf(caster);
        if (members.isEmpty()) {
            try {
                return ManaHelper.getMana(caster);
            } catch (Throwable t) {
                return 0;
            }
        }
        MinecraftServer server = caster.level().getServer();
        if (server == null) {
            try {
                return ManaHelper.getMana(caster);
            } catch (Throwable t) {
                return 0;
            }
        }
        double sum = 0;
        for (UUID m : members) {
            ServerPlayer pm = server.getPlayerList().getPlayer(m);
            if (pm == null) {
                continue;
            }
            try {
                sum += ManaHelper.getMana(pm);
            } catch (Throwable ignored) {
            }
        }
        return sum;
    }

    public static double getSharedMax(ServerPlayer caster) {
        Set<UUID> members = membersOf(caster);
        if (members.isEmpty()) {
            try {
                return ManaHelper.maxMana(caster);
            } catch (Throwable t) {
                return 0;
            }
        }
        MinecraftServer server = caster.level().getServer();
        if (server == null) {
            try {
                return ManaHelper.maxMana(caster);
            } catch (Throwable t) {
                return 0;
            }
        }
        double sum = 0;
        for (UUID m : members) {
            ServerPlayer pm = server.getPlayerList().getPlayer(m);
            if (pm == null) {
                continue;
            }
            try {
                sum += ManaHelper.maxMana(pm);
            } catch (Throwable ignored) {
            }
        }
        return sum;
    }

    /**
     * Списать manaCost маны из общего пула (сначала с кастера, остаток с остальных).
     * @return true если хватило и списание выполнено.
     */
    public static boolean spendShared(ServerPlayer caster, double manaCost) {
        Set<UUID> members = membersOf(caster);
        if (members.isEmpty()) {
            return false;
        }
        MinecraftServer server = caster.level().getServer();
        if (server == null) {
            return false;
        }
        // Порядок: сначала кастер, потом остальные.
        ArrayList<ServerPlayer> order = new ArrayList<>();
        order.add(caster);
        for (UUID m : members) {
            if (m.equals(caster.getUUID())) {
                continue;
            }
            ServerPlayer pm = server.getPlayerList().getPlayer(m);
            if (pm != null && pm.isAlive()) {
                order.add(pm);
            }
        }
        double total = 0;
        double[] manas = new double[order.size()];
        for (int i = 0; i < order.size(); i++) {
            try {
                manas[i] = ManaHelper.getMana(order.get(i));
            } catch (Throwable ignored) {
                manas[i] = 0;
            }
            total += manas[i];
        }
        if (total < manaCost) {
            return false;
        }
        double rest = manaCost;
        for (int i = 0; i < order.size() && rest > 0; i++) {
            double take = Math.min(manas[i], rest);
            try {
                ManaHelper.setMana(order.get(i), manas[i] - take);
            } catch (Throwable ignored) {
            }
            manas[i] -= take;
            rest -= take;
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
        }
        // Активный пул тикаем КАЖДЫЙ тик: upkeep списывается плавно (как у обманок —
        // 1/20 секундного значения за тик), проверки роспуска тоже каждый тик.
        // Тяжёлое: частицы — раз в секунду, HUD — каждые 5 тиков (сглаживание на клиенте).
        tickActive(server, tick);
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
                if (p.groupId() != null && p.soloId() != null) {
                    // Групповой зов: маркер требуем только с соло-стороны, группа жива — по MANA_PAIRED.
                    ServerPlayer soloP = server.getPlayerList().getPlayer(p.soloId());
                    if (soloP == null || !soloP.isAlive()) {
                        gone = true;
                    } else {
                        milked = !soloP.hasEffect(HexEffects.MANA_PAIRING);
                        Set<UUID> gm = GROUPS.get(p.groupId());
                        if (gm == null || gm.isEmpty()) {
                            milked = true; // группа распущена — зов мёртв
                        }
                        if (milked && now < p.expiryGameTime() - (PENDING_TICKS - 20)) {
                            milked = false;
                        }
                    }
                } else {
                    milked = !initiator.hasEffect(HexEffects.MANA_PAIRING) || !target.hasEffect(HexEffects.MANA_PAIRING);
                    // Эффект могли снять молоком досрочно — pending больше не валиден.
                    // Но в тик сразу после begin эффект уже есть; защита от ложного срабатывания:
                    // молоком считаем только если до expiry осталось < PENDING_TICKS - 20 (т.е. не первый тик).
                    if (milked && now < p.expiryGameTime() - (PENDING_TICKS - 20)) {
                        milked = false;
                    }
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

    private static boolean isUpkeepFree(ServerPlayer p) {
        try {
            if (p.isCreative() || p.isSpectator()) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            return at.petrak.hexcasting.api.misc.ManaHelper.hasInfiniteMana(p);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void tickActive(MinecraftServer server, int tick) {
        boolean fullSec = tick % 20 == 0;
        boolean hudTick = tick % 5 == 0;
        if (GROUPS.isEmpty() || server == null) {
            return;
        }
        for (UUID gid : new ArrayList<>(GROUPS.keySet())) {
            Set<UUID> members = GROUPS.get(gid);
            if (members == null || members.isEmpty()) {
                GROUPS.remove(gid);
                continue;
            }
            Set<UUID> copy = new HashSet<>(members);
            if (copy.size() <= 1) {
                // Один остался — пул бессмысленен, распускаем молча.
                dissolveQuiet(server, gid, copy);
                continue;
            }
            ArrayList<ServerPlayer> players = new ArrayList<>();
            boolean needDissolve = false;
            String reason = "";
            for (UUID m : copy) {
                ServerPlayer pm = server.getPlayerList().getPlayer(m);
                if (pm == null) {
                    needDissolve = true;
                    reason = "игрок вышел";
                    break;
                }
                if (!pm.isAlive()) {
                    needDissolve = true;
                    reason = "смерть";
                    break;
                }
                if (!pm.hasEffect(HexEffects.MANA_PAIRED)) {
                    needDissolve = true;
                    reason = "маркер снят (молоко?)";
                    break;
                }
                players.add(pm);
            }
            if (needDissolve) {
                // Роспуск всей группы: чистим по первому участнику.
                UUID any = copy.iterator().next();
                unpair(server, any, reason);
                continue;
            }
            // Одно измерение для всех?
            var dim = players.get(0).level().dimension();
            boolean diffDim = false;
            for (ServerPlayer pm : players) {
                if (!pm.level().dimension().equals(dim)) {
                    diffDim = true;
                    break;
                }
            }
            if (diffDim) {
                unpair(server, players.get(0).getUUID(), "разные измерения");
                continue;
            }
            // Ambit: КАЖДАЯ пара в пределах 32, иначе роспуск всех.
            boolean outOfRange = false;
            for (int i = 0; i < players.size() && !outOfRange; i++) {
                for (int j = i + 1; j < players.size(); j++) {
                    if (players.get(i).distanceToSqr(players.get(j)) > PAIR_RADIUS * PAIR_RADIUS) {
                        outOfRange = true;
                        break;
                    }
                }
            }
            if (outOfRange) {
                unpair(server, players.get(0).getUUID(), "вышли из области каста (>32)");
                continue;
            }
            int n = players.size();
            double upkeepPerSecond = upkeepForSize(n);
            // Плавное списание каждый тик, как у обманок: 1/20 секундного upkeep за тик.
            // Доля каждого — upkeep/n в секунду; креатив/спектатор/бесконечная мана свою долю не платят.
            int payers = 0;
            for (ServerPlayer pm : players) {
                if (!isUpkeepFree(pm)) {
                    payers++;
                }
            }
            if (payers > 0) {
                double sharePerTick = (upkeepPerSecond / n) / 20.0;
                double needThisTick = sharePerTick * payers;
                double[] manas = new double[n];
                boolean[] free = new boolean[n];
                double totalPayerMana = 0;
                for (int i = 0; i < n; i++) {
                    free[i] = isUpkeepFree(players.get(i));
                    try {
                        manas[i] = ManaHelper.getMana(players.get(i));
                    } catch (Throwable ignored) {
                        manas[i] = 0;
                    }
                    if (!free[i]) {
                        totalPayerMana += manas[i];
                    }
                }
                if (totalPayerMana + 1e-6 < needThisTick) {
                    unpair(server, players.get(0).getUUID(), "нет маны на upkeep " + (int) upkeepPerSecond + "м/с");
                    continue;
                }
                // Равные доли с переносом недостачи на остальных (в пределах тика — копейки).
                double[] take = new double[n];
                double deficit = 0;
                for (int i = 0; i < n; i++) {
                    if (free[i]) {
                        take[i] = 0;
                    } else {
                        take[i] = Math.min(manas[i], sharePerTick);
                        deficit += sharePerTick - take[i];
                    }
                }
                int guard = 0;
                while (deficit > 0.000001 && guard++ < 10) {
                    double before = deficit;
                    for (int i = 0; i < n && deficit > 0.000001; i++) {
                        if (free[i]) {
                            continue;
                        }
                        double canGive = manas[i] - take[i];
                        if (canGive > 0) {
                            double add = Math.min(canGive, deficit);
                            take[i] += add;
                            deficit -= add;
                        }
                    }
                    if (Math.abs(before - deficit) < 1e-9) {
                        break;
                    }
                }
                for (int i = 0; i < n; i++) {
                    if (take[i] <= 0) {
                        continue;
                    }
                    try {
                        ManaHelper.setMana(players.get(i), manas[i] - take[i]);
                    } catch (Throwable ignored) {
                    }
                }
            }
            if (!fullSec) {
                if (hudTick) {
                    pushHudUpdate(server, copy);
                }
                continue;
            }
            // Висп-частицы над каждым участником (раз в секунду).
            for (ServerPlayer pm : players) {
                try {
                    if (pm.serverLevel() instanceof ServerLevel sl) {
                        sl.sendParticles(new at.petrak.hexcasting.common.particles.ConjureParticleOptions(WISP_COLOR),
                                pm.getX(), pm.getY() + pm.getBbHeight() + 0.35, pm.getZ(),
                                3, 0.25, 0.25, 0.25, 0.02);
                    }
                } catch (Throwable ignored) {
                }
            }
            pushHudUpdate(server, copy);
        }
    }

    private static void dissolveQuiet(MinecraftServer server, UUID gid, Set<UUID> members) {
        GROUPS.remove(gid);
        for (UUID m : members) {
            MEMBER_TO_GROUP.remove(m, gid);
            ServerPlayer pm = server.getPlayerList().getPlayer(m);
            if (pm != null) {
                try {
                    pm.removeEffect(HexEffects.MANA_PAIRED);
                } catch (Throwable ignored) {
                }
            }
        }
        pushHudUpdate(server, members);
    }

    private static void pushHudUpdate(MinecraftServer server, Set<UUID> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        double shared = 0, max = 1;
        try {
            for (UUID m : members) {
                ServerPlayer pm = server.getPlayerList().getPlayer(m);
                if (pm != null) {
                    shared += ManaHelper.getMana(pm);
                    max += ManaHelper.maxMana(pm);
                }
            }
        } catch (Throwable ignored) {
        }
        for (UUID m : members) {
            ServerPlayer pm = server.getPlayerList().getPlayer(m);
            if (pm == null) {
                continue;
            }
            try {
                if (MEMBER_TO_GROUP.containsKey(m)) {
                    at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                            pm, new me.nanorasmus.nanodev.hex_js.network.MsgManaPairS2C(true, shared, max));
                } else {
                    at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                            pm, new me.nanorasmus.nanodev.hex_js.network.MsgManaPairS2C(false, 0, 1));
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static void pushHudUpdate(MinecraftServer server, UUID a, UUID b) {
        Set<UUID> s = new HashSet<>();
        if (a != null) {
            s.add(a);
        }
        if (b != null) {
            s.add(b);
        }
        // Подтянем остальных сокомандников, чтобы HUD обновился у всей группы.
        UUID gid = a != null ? MEMBER_TO_GROUP.get(a) : null;
        if (gid == null && b != null) {
            gid = MEMBER_TO_GROUP.get(b);
        }
        if (gid != null) {
            Set<UUID> gm = GROUPS.get(gid);
            if (gm != null) {
                s.addAll(gm);
            }
        }
        for (UUID m : new HashSet<>(s)) {
            ServerPlayer pm = server.getPlayerList().getPlayer(m);
            if (pm == null) {
                continue;
            }
            try {
                if (MEMBER_TO_GROUP.containsKey(m)) {
                    Set<UUID> members = GROUPS.get(MEMBER_TO_GROUP.get(m));
                    double shared = 0, mx = 1;
                    if (members != null) {
                        shared = 0;
                        mx = 0;
                        for (UUID mm : members) {
                            ServerPlayer pp = server.getPlayerList().getPlayer(mm);
                            if (pp != null) {
                                shared += ManaHelper.getMana(pp);
                                mx += ManaHelper.maxMana(pp);
                            }
                        }
                        if (mx <= 0) {
                            mx = 1;
                        }
                    }
                    at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                            pm, new me.nanorasmus.nanodev.hex_js.network.MsgManaPairS2C(true, shared, mx));
                } else {
                    at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                            pm, new me.nanorasmus.nanodev.hex_js.network.MsgManaPairS2C(false, 0, 1));
                }
            } catch (Throwable ignored) {
            }
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
        if (MEMBER_TO_GROUP.containsKey(id)) {
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
            UUID gid = MEMBER_TO_GROUP.remove(player.getUUID());
            if (gid != null) {
                Set<UUID> members = GROUPS.get(gid);
                if (members != null) {
                    members.remove(player.getUUID());
                }
            }
            return;
        }
        UUID id = player.getUUID();
        if (MEMBER_TO_GROUP.containsKey(id)) {
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
