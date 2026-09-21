package me.nanorasmus.nanodev.hex_js.casting;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.RenderedSpell;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.OperationResult;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.SpellContinuation;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation;
import at.petrak.hexcasting.api.casting.mishaps.MishapImmuneEntity;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.common.casting.actions.spells.great.OpTeleport;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Loki's Gambit — свапает две сущности местами.
 * <p>
 * Стек: [entity, entity] — две сущности (порядок не важен, свап симметричен).
 * Обе должны быть в ambit/диапазоне каста, не иметь тега cannot_teleport,
 * быть в одном измерении и в измерении где телепорт разрешён.
 * Стоимость — 10 маны × дистанция в блоках × scale сущности (1 мана = 1000 media).
 */
public class OpLokisGambit implements SpellAction {

    public static final OpLokisGambit INSTANCE = new OpLokisGambit();

    private OpLokisGambit() {
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
        Entity a;
        Entity b;
        try {
            a = getEntity(args, 0, getArgc());
            b = getEntity(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        // Same entity — no-op but still mishap to avoid confusion
        if (a == b || a.getId() == b.getId()) {
            sneakyThrow(new MishapBadLocation(a.position(), "cannot_swap_same"));
        }

        // Both must be in ambit / casting range
        try {
            env.assertEntityInRange(a);
            env.assertEntityInRange(b);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        // Immune check (cannot_teleport)
        if (!a.canUsePortal(false) || a.getType().is(HexTags.Entities.CANNOT_TELEPORT)) {
            sneakyThrow(new MishapImmuneEntity(a));
        }
        if (!b.canUsePortal(false) || b.getType().is(HexTags.Entities.CANNOT_TELEPORT)) {
            sneakyThrow(new MishapImmuneEntity(b));
        }

        // Cannot teleport if any passenger is immune (mirrors teleportRespectSticky early-out as mishap)
        if (hasImmunePassenger(a) || hasImmunePassenger(b)) {
            // Prefer explicit mishap over silent no-op
            Entity culprit = hasImmunePassenger(a) ? a : b;
            sneakyThrow(new MishapImmuneEntity(culprit));
        }

        // Cross-dimension swap is forbidden
        ServerLevel world = env.getWorld();
        ServerLevel aLevel = a.level() instanceof ServerLevel sl ? sl : null;
        ServerLevel bLevel = b.level() instanceof ServerLevel sl ? sl : null;
        if (aLevel == null || bLevel == null || !aLevel.dimension().equals(bLevel.dimension()) || !aLevel.dimension().equals(world.dimension())) {
            sneakyThrow(new MishapBadLocation(a.position(), "cross_dimension_swap"));
        }

        // Dimension teleport allowed?
        if (!HexConfig.server().canTeleportInThisDimension(world.dimension())) {
            Vec3 bad = a.position().add(b.position()).scale(0.5);
            sneakyThrow(new MishapBadLocation(bad, "bad_dimension"));
        }

        // Target positions must be in world and in range (mirrors blink/teleport checks)
        Vec3 posA = a.position();
        Vec3 posB = b.position();
        try {
            env.assertVecInRange(posA);
            env.assertVecInRange(posB);
        } catch (Throwable t) {
            sneakyThrow(t);
        }
        // isVecInWorld check with y+1 offset as teleport does
        if (!env.isVecInWorld(posA.subtract(0, 1, 0)) || !env.isVecInWorld(posB.subtract(0, 1, 0))) {
            Vec3 bad = !env.isVecInWorld(posA.subtract(0, 1, 0)) ? posA : posB;
            sneakyThrow(new MishapBadLocation(bad, "too_close_to_out"));
        }
        try {
            env.assertVecInWorld(posA);
            env.assertVecInWorld(posB);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        // Particles at both entities (cloud + burst style)
        Vec3 aEye = posA.add(0, a.getEyeHeight() / 2.0, 0);
        Vec3 bEye = posB.add(0, b.getEyeHeight() / 2.0, 0);

        List<ParticleSpray> particles = List.of(
                ParticleSpray.cloud(aEye, 2.0, 30),
                ParticleSpray.cloud(bEye, 2.0, 30),
                ParticleSpray.burst(aEye, 2.0, 80),
                ParticleSpray.burst(bEye, 2.0, 80)
        );

        // Правка баланса: 10 маны × блоки × scale (средний рост обеих сущностей, мин. 0.5).
        double dist = posA.distanceTo(posB);
        double scale = (a.getBbHeight() + b.getBbHeight()) * 0.5;
        if (scale < 0.5) scale = 0.5;
        long cost = (long) (dist * scale * 10L * 1000L);
        if (cost < 10_000L) cost = 10_000L;
        return new SpellAction.Result(new Spell(a, b), cost, particles, 0);
    }

    private static boolean hasImmunePassenger(Entity e) {
        for (Entity passenger : e.getPassengers()) {
            if (passenger.getType().is(HexTags.Entities.CANNOT_TELEPORT)) {
                return true;
            }
        }
        return false;
    }

    public static class Spell implements RenderedSpell {
        private final Entity a;
        private final Entity b;

        public Spell(Entity a, Entity b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            ServerLevel world = env.getWorld();
            if (!HexConfig.server().canTeleportInThisDimension(world.dimension())) {
                return;
            }
            // Snapshot positions at cast time atomically
            Vec3 posA = a.position();
            Vec3 posB = b.position();

            // Delta for each entity
            Vec3 deltaA = posB.subtract(posA);
            Vec3 deltaB = posA.subtract(posB);

            // Use vanilla sticky-aware teleport to handle players, vehicles, tickets, portal sound etc.
            OpTeleport.INSTANCE.teleportRespectSticky(a, deltaA, world);
            OpTeleport.INSTANCE.teleportRespectSticky(b, deltaB, world);
        }
    }
}
