package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import at.petrak.hexcasting.api.casting.math.HexAngle;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getDouble;
import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Hera's Wrath — накладывает на целевого игрока эффект Безмолвия (Hex Casting silenced).
 * <p>
 * Стек: [entity, double seconds] — целевая сущность (должна быть {@link Player},
 * можно на себя) и количество секунд безмолвия. Секунды мягко обрезаются в
 * диапазон [1, 5] — если меньше 1, mishap; если больше 5, обрезается до 5.
 * Стоимость — {@code clamped * 1000000} media (1000 маны/сек): минимум 1000000
 * (1 сек = 1000 маны), максимум 5000000 (5 сек = 5000 маны до скидки).
 * Списание идёт штатно движком через StaffCastEnv.extractMana; сама йота/эффект
 * маны не стоят — платит каст целиком. Через DeceptionCastEnv (шёпот) стоимость
 * покрыта предоплатой шёпота, отдельный спелл маны не списывает.
 * Безмолвие реализовано через {@link HexEffects#SILENCE} — тот же эффект, что
 * использует {@code SilenceHelper}, поэтому цель блокируется от магических
 * взаимодействий одинаково с любого источника.
 */
public class OpHerasWrath implements SpellAction {

    public static final OpHerasWrath INSTANCE = new OpHerasWrath();

    private static final double MIN_SECONDS = 1.0;
    private static final double MAX_SECONDS = 5.0;
    // 1 mana = 1_000 media units. Cost is 1000 mana per second of silence:
    // 1_000_000 media per second (1_000_000 / 1_000 = 1000 mana).
    private static final int COST_PER_SECOND = 1_000_000;

    /**
     * Pattern for Hera's Wrath. Built programmatically as a 55-step "left-hand-rule"
     * spiral via {@link #buildWithBacktrack}. The first attempt (start=NORTH_EAST)
     * has produced the signature {@code qqqqqwqqqqqwqqqqwqqqqqeqqqqdqqqeqqqdqqqdqqqqaqqqqdqqqdq}
     * and is used as the canonical pattern. If the spiral ever changes its
     * output (e.g. due to tryAppendDir rule tweaks in a Hex Casting update),
     * the new signature is logged at static-init time so the Patchouli page
     * can be updated to match.
     */
    public static final HexPattern PATTERN = buildSpiralPattern();
    public static final String PATTERN_SIGNATURE = PATTERN.anglesSignature();

    private OpHerasWrath() {
    }

    @Override
    public int getArgc() {
        return 2;
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
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.operate(this, env, image, cont);
    }

    @Override
    public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userdata) {
        return at.petrak.hexcasting.api.casting.castables.SpellAction.DefaultImpls.executeWithUserdata(this, args, env, userdata);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    @Override
    public SpellAction.Result execute(List<? extends Iota> args, CastingEnvironment env) {
        Entity target;
        double rawSeconds;
        try {
            target = getEntity(args, 0, getArgc());
            rawSeconds = getDouble(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        // Must be a player (self-cast allowed)
        if (!(target instanceof Player playerTarget)) {
            sneakyThrow(new OvidMishap("Требуется игрок"));
            return null;
        }

        // Must be in ambit / casting range
        try {
            env.assertEntityInRange(target);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        // Lower bound: at least 1 second. We don't accept 0, negative, NaN or infinite.
        // (NaN < 1.0 is false, so an explicit finite check is required — otherwise
        // NaN would slip through with cost 0 and 0 ticks.)
        if (!(rawSeconds >= MIN_SECONDS) || !Double.isFinite(rawSeconds)) {
            sneakyThrow(MishapInvalidIota.ofType(args.get(1), 0, "double.positive"));
            return null;
        }

        // Upper bound: silently clamp to MAX_SECONDS. This is the "soft cap":
        // values above MAX are accepted and billed as MAX, not refused.
        int seconds = (int) Math.min(MAX_SECONDS, rawSeconds);
        int ticks = seconds * 20;

        long cost = (long) seconds * COST_PER_SECOND;

        HexJS.LOGGER.info("[Hera's Wrath] rawSeconds={} clampedSeconds={} cost={} ({} dust)",
                rawSeconds, seconds, cost, cost / 10_000.0);

        Vec3 eye = playerTarget.getEyePosition();
        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(eye, 1.5, 30),
                ParticleSpray.cloud(eye, 1.0, 20)
        );

        return new SpellAction.Result(new SilenceSpell(playerTarget, ticks), cost, particles, 0);
    }

    /** Rendered stage: applies the Silence marker effect to the target. */
    public static class SilenceSpell implements RenderedSpell {
        private final Player target;
        private final int ticks;

        public SilenceSpell(Player target, int ticks) {
            this.target = target;
            this.ticks = ticks;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            // Same flags as SilenceHelper.applySilence: ambient + visible + show icon.
            MobEffectInstance instance = new MobEffectInstance(HexEffects.SILENCE, ticks, 0, false, true, true);
            target.addEffect(instance);
        }
    }

    /**
     * Builds a guaranteed-valid pattern of {@code targetLength} steps using the
     * left-hand rule on the hex grid, validating each step through
     * {@link HexPattern#tryAppendDir} so the result is always accepted by
     * {@link HexPattern#fromAngles}. When a step can't be placed, we backtrack
     * to the previous step and try the next angle in the priority list. The
     * starting direction is rotated across attempts so we don't have to
     * worry about hitting a degenerate starting position.
     */
    private static HexPattern buildSpiralPattern() {
        final int targetLength = 55;
        for (int attempt = 0; attempt < 6; attempt++) {
            HexDir startDir = HexDir.values()[attempt];
            try {
                HexPattern built = buildWithBacktrack(targetLength, startDir);
                HexJS.LOGGER.info("[Hera's Wrath] pattern attempt={} start={} sig={} ({} steps)",
                        attempt, startDir, built.anglesSignature(), built.getAngles().size());
                return built;
            } catch (IllegalStateException ex) {
                HexJS.LOGGER.warn("[Hera's Wrath] attempt {} failed: {}", attempt, ex.getMessage());
            }
        }
        throw new IllegalStateException(
                "OpHerasWrath: could not build a valid spiral pattern in 6 attempts");
    }

    /**
     * Greedy left-hand-rule builder with depth-first backtracking.
     * At each step we try directions in this order relative to the
     * current cursor direction: LEFT, FORWARD, RIGHT, RIGHT_BACK,
     * LEFT_BACK. We let {@link HexPattern#tryAppendDir} validate the
     * step; on success it mutates the pattern's angles list in place
     * by appending the new angle, so we don't need to maintain a
     * separate copy. On failure we try the next candidate; if every
     * candidate fails at this depth, we pop the last angle off the
     * current frame and back up one level.
     */
    private static HexPattern buildWithBacktrack(int targetLength, HexDir startDir) {
        HexPattern current = new HexPattern(startDir, new java.util.ArrayList<>());
        // We use a list of (angle) choice indices that mirrors the
        // depth of the recursion; choiceIdx[d] is the next candidate
        // to try at depth d. We backtrack by popping the last angle
        // off `current` and re-trying from the saved index.
        java.util.ArrayList<Integer> choiceIdx = new java.util.ArrayList<>();
        choiceIdx.add(0);

        int depth = 0;
        while (depth >= 0) {
            if (current.getAngles().size() == targetLength) {
                return current;
            }
            HexDir cur = current.getAngles().isEmpty()
                    ? current.getStartDir()
                    : current.finalDir();
            HexDir[] priority = priorityAround(cur);
            int ci = choiceIdx.get(depth);
            boolean moved = false;
            while (ci < priority.length) {
                HexDir candidate = priority[ci];
                ci++;
                if (current.tryAppendDir(candidate)) {
                    // success: angle was appended in place
                    moved = true;
                    break;
                }
            }
            if (moved) {
                // Save the (now-exhausted or partially-exhausted) index
                // for this depth and push a fresh child frame.
                choiceIdx.set(depth, ci);
                if (depth + 1 >= choiceIdx.size()) {
                    choiceIdx.add(0);
                } else {
                    choiceIdx.set(depth + 1, 0);
                }
                depth++;
            } else {
                // All candidates at this depth failed: pop the last
                // angle and back up. (tryAppendDir never adds a BACK
                // angle, so the topmost angle is always one of the
                // priority candidates we tried, and removing it is
                // correct.)
                if (!current.getAngles().isEmpty()) {
                    current.getAngles().remove(current.getAngles().size() - 1);
                }
                choiceIdx.remove(depth);
                depth--;
            }
        }
        throw new IllegalStateException("backtrack exhausted for startDir=" + startDir);
    }

    private static HexDir[] priorityAround(HexDir current) {
        return new HexDir[]{
                compassRotated(current, +5), // LEFT
                current,                    // FORWARD
                compassRotated(current, +1), // RIGHT
                compassRotated(current, +2), // RIGHT_BACK
                compassRotated(current, +4)  // LEFT_BACK
                // BACK intentionally omitted: tryAppendDir always returns false
                // for it, and skipping it saves one wasted check per step.
        };
    }

    private static HexDir compassRotated(HexDir d, int steps) {
        HexDir[] all = HexDir.values();
        int idx = ((d.ordinal() + steps) % all.length + all.length) % all.length;
        return all[idx];
    }

    /** Maps a relative-angle offset (0=forward..5=back) to a HexAngle. */
    private static HexAngle angleBetween(HexDir from, HexDir to) {
        int diff = ((to.ordinal() - from.ordinal()) % 6 + 6) % 6;
        return switch (diff) {
            case 0 -> HexAngle.FORWARD;
            case 1 -> HexAngle.RIGHT;
            case 2 -> HexAngle.RIGHT_BACK;
            case 3 -> HexAngle.BACK;
            case 4 -> HexAngle.LEFT_BACK;
            case 5 -> HexAngle.LEFT;
            default -> throw new IllegalStateException("unreachable diff=" + diff);
        };
    }
}
