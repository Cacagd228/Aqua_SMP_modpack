package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Мана-пейринг — общий манапул группы до 10 игроков.
 * <p>
 * Стек: [entity] — второй кастер (только ServerPlayer, не self).
 * <ul>
 *   <li>Первый каст (A на B): стоит 500 маны с A. Обоим вешается ожидание на 5с.</li>
 *   <li>Подтверждение (в эти 5с): бесплатно. Соло-новичок подтверждает кастом на ЛЮБОГО
 *       участника группы (или любой участник на новичка) — пулы сливаются в один,
 *       upkeep (n-1)*10м/с с общего.</li>
 *   <li>Повторный каст по своему (та же группа) — роспуск ВСЕЙ группы (бесплатно).
 *       Отдельная руна разрыва — {@link OpManaUnpair} (тоже роспуск всей группы).</li>
 * </ul>
 * Сигнатура qaqwawaa (EAST).
 */
public class OpManaPairing implements SpellAction {

    public static final OpManaPairing INSTANCE = new OpManaPairing();
    /** 500 маны в media (1 мана = 1000 media). */
    public static final long PAIR_COST_MEDIA = 500L * 1000L;

    private OpManaPairing() {
    }

    @Override
    public int getArgc() {
        return 1;
    }

    @Override
    public boolean hasCastingSound(CastingEnvironment env) {
        return true;
    }

    @Override
    public boolean awardsCastingStat(CastingEnvironment env) {
        return true;
    }

    @Override
    public OperationResult operate(CastingEnvironment env, CastingImage image, SpellContinuation cont) {
        return SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Entity targetRaw;
        try {
            targetRaw = getEntity(args, 0, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        ServerPlayer caster = null;
        try {
            caster = env.getCaster();
        } catch (Throwable ignored) {
        }
        if (caster == null) {
            sneakyThrow(new OvidMishap("Нет кастера"));
            return null;
        }
        if (!(targetRaw instanceof ServerPlayer target)) {
            sneakyThrow(new OvidMishap("Пейринг только между игроками"));
            return null;
        }
        try {
            env.assertEntityInRange(target);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        UUID a = caster.getUUID();
        UUID b = target.getUUID();
        if (a.equals(b)) {
            sneakyThrow(new OvidMishap("Нельзя спариться с собой"));
            return null;
        }

        Vec3 eye = target.getEyePosition();
        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(eye, 1.5, 30),
                ParticleSpray.cloud(eye, 1.0, 20));

        // Уже в одной группе — та же руна распускает ВСЮ группу (бесплатно).
        if (ManaPairingHandler.isPairedWith(a, b)) {
            return new SpellAction.Result(new UnpairSpell(a), 0, particles, 0);
        }
        boolean pairedA = ManaPairingHandler.isPaired(a);
        boolean pairedB = ManaPairingHandler.isPaired(b);
        // Оба уже в разных пулах — mishap (слияние групп запрещено).
        if (pairedA && pairedB) {
            sneakyThrow(new OvidMishap("Оба кастера уже в общих пулах"));
            return null;
        }
        // Лимит 10: группа + новичок.
        int effectiveSize = 2;
        if (pairedA) {
            effectiveSize = ManaPairingHandler.getGroupSize(a) + 1;
        } else if (pairedB) {
            effectiveSize = ManaPairingHandler.getGroupSize(b) + 1;
        }
        if (effectiveSize > ManaPairingHandler.MAX_GROUP_SIZE) {
            sneakyThrow(new OvidMishap("Группа полная (макс 10)"));
            return null;
        }

        var pending = ManaPairingHandler.getPendingBetween(a, b);
        if (pending != null) {
            if (pending.initiatorId().equals(a)) {
                // Повтор инициатора — освежить 5с, бесплатно.
                return new SpellAction.Result(new RefreshSpell(a, b), 0, particles, 0);
            } else {
                // Подтверждение от второго — бесплатно, пулы общие.
                return new SpellAction.Result(new CompleteSpell(a, b), 0, particles, 0);
            }
        }
        // Подтверждение / освежение через ЛЮБОГО участника группы.
        var completable = ManaPairingHandler.findCompletablePending(a, b);
        if (completable != null) {
            return new SpellAction.Result(new CompleteSpell(a, b), 0, particles, 0);
        }
        var refreshable = ManaPairingHandler.findRefreshablePending(a, b);
        if (refreshable != null) {
            return new SpellAction.Result(new RefreshSpell(a, b), 0, particles, 0);
        }

        if (ManaPairingHandler.isInPending(a) || ManaPairingHandler.isInPending(b)) {
            sneakyThrow(new OvidMishap("Один из кастеров уже ждёт другой пейринг"));
            return null;
        }

        // Первый каст — 500 маны с инициатора.
        return new SpellAction.Result(new BeginSpell(a, b), PAIR_COST_MEDIA, particles, 0);
    }

    /** Первый каст: повесить ожидание 5с. */
    public static class BeginSpell implements RenderedSpell {
        private final UUID initiator;
        private final UUID target;

        public BeginSpell(UUID initiator, UUID target) {
            this.initiator = initiator;
            this.target = target;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            MinecraftServer server = env.getWorld().getServer();
            if (server == null) {
                return;
            }
            ServerPlayer pi = server.getPlayerList().getPlayer(initiator);
            ServerPlayer pt = server.getPlayerList().getPlayer(target);
            if (pi == null || pt == null || !pi.isAlive() || !pt.isAlive()) {
                return;
            }
            // Перепроверка к моменту каста: своя группа / полные / чужие пулы — не начинаем.
            if (ManaPairingHandler.isPairedWith(initiator, target)) {
                return;
            }
            boolean pairedA = ManaPairingHandler.isPaired(initiator);
            boolean pairedB = ManaPairingHandler.isPaired(target);
            if (pairedA && pairedB) {
                return;
            }
            int eff = 2;
            if (pairedA) {
                eff = ManaPairingHandler.getGroupSize(initiator) + 1;
            } else if (pairedB) {
                eff = ManaPairingHandler.getGroupSize(target) + 1;
            }
            if (eff > ManaPairingHandler.MAX_GROUP_SIZE) {
                return;
            }
            if (ManaPairingHandler.getPendingBetween(initiator, target) != null
                    || ManaPairingHandler.findCompletablePending(initiator, target) != null
                    || ManaPairingHandler.findRefreshablePending(initiator, target) != null) {
                return;
            }
            if (ManaPairingHandler.isInPending(initiator) || ManaPairingHandler.isInPending(target)) {
                return;
            }
            ManaPairingHandler.beginRequest(pi, pt);
        }
    }

    /** Подтверждение: пулы становятся общими. */
    public static class CompleteSpell implements RenderedSpell {
        private final UUID a;
        private final UUID b;

        public CompleteSpell(UUID a, UUID b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            MinecraftServer server = env.getWorld().getServer();
            if (server == null) {
                return;
            }
            // Подтверждать может новичок на любого участника (и наоборот).
            // Если pending уже истёк/ушёл — ничего не делаем.
            var pending = ManaPairingHandler.getPendingBetween(a, b);
            if (pending == null) {
                pending = ManaPairingHandler.findCompletablePending(a, b);
            }
            if (pending == null) {
                return;
            }
            ServerPlayer pa = server.getPlayerList().getPlayer(a);
            ServerPlayer pb = server.getPlayerList().getPlayer(b);
            if (pa == null || pb == null || !pa.isAlive() || !pb.isAlive()) {
                return;
            }
            ManaPairingHandler.completePair(server, a, b);
        }
    }

    /** Освежить ожидание (повтор инициатора). */
    public static class RefreshSpell implements RenderedSpell {
        private final UUID initiator;
        private final UUID target;

        public RefreshSpell(UUID initiator, UUID target) {
            this.initiator = initiator;
            this.target = target;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            MinecraftServer server = env.getWorld().getServer();
            if (server == null) {
                return;
            }
            ServerPlayer pi = server.getPlayerList().getPlayer(initiator);
            ServerPlayer pt = server.getPlayerList().getPlayer(target);
            if (pi == null || pt == null || !pi.isAlive() || !pt.isAlive()) {
                return;
            }
            ManaPairingHandler.refreshRequest(pi, pt);
        }
    }

    /** Та же руна по своей паре — разрыв. */
    public static class UnpairSpell implements RenderedSpell {
        private final UUID who;

        public UnpairSpell(UUID who) {
            this.who = who;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            MinecraftServer server = env.getWorld().getServer();
            if (server == null) {
                return;
            }
            ManaPairingHandler.unpair(server, who, "разрыв руной");
            if (env.getWorld() instanceof net.minecraft.server.level.ServerLevel sl) {
                LivingEntity le = null;
                var e = sl.getEntity(who);
                if (e instanceof LivingEntity living) {
                    le = living;
                }
                if (le != null) {
                    try {
                        le.removeEffect(HexEffects.MANA_PAIRING);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }
}
