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
import me.nanorasmus.nanodev.hex_js.entity.EntitySniperShot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static at.petrak.hexcasting.api.casting.OperatorUtils.getDouble;
import static at.petrak.hexcasting.api.casting.OperatorUtils.getEntity;

/**
 * Jormungand's Laughter — разом гасит скорость всех стрел вокруг цели.
 * <p>
 * Стек: [entity, double radius] — сущность-центр (любая, можно себя,
 * должна быть в радиусе каста) и радиус в блоках. Радиус мягко ограничен
 * сверху ({@value #MAX_RADIUS}), ниже 1 — mishap.
 * Все живые {@link AbstractArrow} в кубе (кроме снайперских снарядов —
 * смеху они не подвластны) мгновенно теряют скорость и падают на землю
 * под действием гравитации. Разовый импульс, без длительности.
 * Стоимость — {@value #COST_PER_BLOCK} пыли за блок радиуса.
 */
public class OpJormungandLaughter implements SpellAction {

    public static final OpJormungandLaughter INSTANCE = new OpJormungandLaughter();

    private static final double MIN_RADIUS = 1.0;
    private static final double MAX_RADIUS = 5.0;
    private static final long COST_PER_BLOCK = 5L * 10000L; // 5 dust per block

    private OpJormungandLaughter() {
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
        double rawRadius;
        try {
            target = getEntity(args, 0, getArgc());
            rawRadius = getDouble(args, 1, getArgc());
        } catch (Throwable t) {
            sneakyThrow(t);
            return null;
        }

        try {
            env.assertEntityInRange(target);
        } catch (Throwable t) {
            sneakyThrow(t);
        }

        if (!(rawRadius >= MIN_RADIUS) || !Double.isFinite(rawRadius)) {
            sneakyThrow(MishapInvalidIota.ofType(args.get(1), 0, "double.positive"));
            return null;
        }
        int radius = (int) Math.min(MAX_RADIUS, rawRadius);

        Vec3 center = target.position().add(0, target.getBbHeight() / 2.0, 0);
        long cost = (long) radius * COST_PER_BLOCK;

        List<ParticleSpray> particles = List.of(
                ParticleSpray.burst(center, radius, 40),
                ParticleSpray.cloud(center, radius * 0.75, 25)
        );

        return new SpellAction.Result(new QuellSpell(center, radius), cost, particles, 0);
    }

    /** Rendered stage: гасит скорость стрел в кубе вокруг центра. */
    public static class QuellSpell implements RenderedSpell {
        private final Vec3 center;
        private final int radius;

        public QuellSpell(Vec3 center, int radius) {
            this.center = center;
            this.radius = radius;
        }

        @Override
        public CastingImage cast(CastingEnvironment env, CastingImage image) {
            return at.petrak.hexcasting.api.casting.RenderedSpell.DefaultImpls.cast(this, env, image);
        }

        @Override
        public void cast(CastingEnvironment env) {
            if (!(env.getWorld() instanceof ServerLevel world)) {
                return;
            }
            AABB box = new AABB(
                    center.x - radius, center.y - radius, center.z - radius,
                    center.x + radius, center.y + radius, center.z + radius);
            for (AbstractArrow arrow : world.getEntitiesOfClass(AbstractArrow.class, box,
                    e -> e.isAlive() && !(e instanceof EntitySniperShot))) {
                arrow.setDeltaMovement(Vec3.ZERO);
            }
        }
    }
}
