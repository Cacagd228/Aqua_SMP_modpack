package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.addon.HexArtifactsItems;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemSniperScope;
import me.nanorasmus.nanodev.hex_js.entity.EntitySniperShot;
import me.nanorasmus.nanodev.hex_js.helpers.CurioHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Deadeye — "Снайперский выстрел". Takes a single entity from the stack, fires a
 * homing round that passes through any blocks and strips 40% of the target's max HP
 * as magic damage.
 *
 * <p>Requires the {@link HexArtifactsItems#SNIPER_SCOPE} to be worn by the caster
 * (Curios {@code head} slot). The requirement is enforced both when the pattern is
 * drawn (better UX, via the pattern interceptor) and authoritatively here at execute
 * time (covers spellbooks/circles). The bullet itself is an {@link EntitySniperShot}.
 */
public class OpDeadeye implements SpellAction {

    public static final OpDeadeye INSTANCE = new OpDeadeye();

    /** Max horizontal distance from caster to target (blocks). */
    public static final double MAX_RANGE = 200.0;
    /** ~40 dust / 400 mana per the project's 1000 media = 1 mana scale (fits amulet pools). */
    public static final long COST_MEDIA = 400_000L;
    /** Reload time after a shot (seconds / ticks) — the scope cannot fire while it lasts. */
    public static final int COOLDOWN_SECONDS = 5 * 60;
    public static final int COOLDOWN_TICKS = COOLDOWN_SECONDS * 20;
    public static final int TRACER_COLOR = 0xFF_FFAA00;

    /** Long, unique left-hand-rule spiral (see {@link #buildDeadeyePattern}). */
    public static final HexPattern PATTERN = buildDeadeyePattern();
    public static final String PATTERN_SIGNATURE = PATTERN.anglesSignature();

    static {
        HexJS.LOGGER.info("[Deadeye] pattern sig={} steps={}",
                PATTERN_SIGNATURE, PATTERN.getAngles().size());
    }

    private OpDeadeye() {
    }

    /** Whether a freshly drawn pattern is exactly the Deadeye pattern. */
    public static boolean isDeadeyePattern(HexPattern pattern) {
        return pattern != null && PATTERN.equals(pattern);
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
        Entity ent;
        try {
            ent = getEntity(args, 0, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        if (!(ent instanceof LivingEntity living)) {
            sneakyThrow(new OvidMishap("Цель должна быть живой сущностью"));
            return null;
        }
        if (!living.isAlive()) {
            sneakyThrow(new OvidMishap("Цель мертва"));
            return null;
        }

        ServerPlayer caster = env.getCaster();
        if (caster == null) {
            sneakyThrow(new OvidMishap("Требуется игрок-кастер"));
            return null;
        }

        ServerLevel world = env.getWorld();
        ItemStack scope = CurioHelper.findCurio(caster, HexArtifactsItems.SNIPER_SCOPE.get());
        if (scope.isEmpty()) {
            sneakyThrow(new OvidMishap("Для «Снайперского выстрела» нужен надетый Прицел снайпера"));
            return null;
        }
        long readyAt = ItemSniperScope.getDeadeyeReadyAt(scope);
        if (readyAt > world.getGameTime()) {
            long remaining = (readyAt - world.getGameTime() + 19) / 20;
            sneakyThrow(new OvidMishap(Component.translatable("meowhex.message.deadeye_cooldown", remaining)));
            return null;
        }

        if (living.level() != world) {
            sneakyThrow(new OvidMishap("Цель в другом измерении"));
            return null;
        }
        if (caster.distanceToSqr(living) > MAX_RANGE * MAX_RANGE) {
            sneakyThrow(new OvidMishap("Цель слишком далеко"));
            return null;
        }

        Vec3 eye = caster.getEyePosition();
        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(eye, 1.2, 25),
                ParticleSpray.cloud(eye, 0.6, 15)
        );

        return new SpellAction.Result(new DeadeyeShotSpell(caster, living), COST_MEDIA, particles, 0);
    }

    /** Rendered stage: spawns the homing round at the caster's eye. */
    public static class DeadeyeShotSpell implements RenderedSpell {
        private final ServerPlayer caster;
        private final LivingEntity target;

        public DeadeyeShotSpell(ServerPlayer caster, LivingEntity target) {
            this.caster = caster;
            this.target = target;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            // The shot actually went off (media already paid): start the scope's reload.
            ItemStack scope = CurioHelper.findCurio(caster, HexArtifactsItems.SNIPER_SCOPE.get());
            if (!scope.isEmpty()) {
                ItemSniperScope.setDeadeyeReadyAt(scope, world.getGameTime() + COOLDOWN_TICKS);
            }
            Vec3 start = caster.getEyePosition().add(caster.getLookAngle().scale(1.0));
            EntitySniperShot shot = new EntitySniperShot(
                    world, start.x, start.y, start.z, target, caster, TRACER_COLOR);
            world.addFreshEntity(shot);
            world.playSound(null, start.x, start.y, start.z,
                    SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.5f, 0.6f);
        }
    }

    // ------------------------------------------------------------------
    // Spiral pattern generation (same left-hand-rule backtracking as Hera's
    // Wrath) producing a long, collision-free Deadeye signature.
    // ------------------------------------------------------------------

    private static HexPattern buildDeadeyePattern() {
        final int targetLength = 20;
        for (int attempt = 0; attempt < 6; attempt++) {
            HexDir startDir = HexDir.values()[attempt];
            try {
                HexPattern built = buildWithBacktrack(targetLength, startDir);
                HexJS.LOGGER.info("[Deadeye] pattern attempt={} start={} sig={} ({} steps)",
                        attempt, startDir, built.anglesSignature(), built.getAngles().size());
                return built;
            } catch (IllegalStateException ex) {
                HexJS.LOGGER.warn("[Deadeye] attempt {} failed: {}", attempt, ex.getMessage());
            }
        }
        throw new IllegalStateException("OpDeadeye: could not build a valid spiral pattern in 6 attempts");
    }

    private static HexPattern buildWithBacktrack(int targetLength, HexDir startDir) {
        HexPattern current = new HexPattern(startDir, new java.util.ArrayList<>());
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
                    moved = true;
                    break;
                }
            }
            if (moved) {
                choiceIdx.set(depth, ci);
                if (depth + 1 >= choiceIdx.size()) {
                    choiceIdx.add(0);
                } else {
                    choiceIdx.set(depth + 1, 0);
                }
                depth++;
            } else {
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
        };
    }

    private static HexDir compassRotated(HexDir d, int steps) {
        HexDir[] all = HexDir.values();
        int idx = ((d.ordinal() + steps) % all.length + all.length) % all.length;
        return all[idx];
    }
}
