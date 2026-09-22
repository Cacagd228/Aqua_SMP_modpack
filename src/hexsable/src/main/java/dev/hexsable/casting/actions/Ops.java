package dev.hexsable.casting.actions;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.casting.castables.SpellAction;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.DoubleIota;
import at.petrak.hexcasting.api.casting.iota.EntityIota;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import at.petrak.hexcasting.api.casting.iota.Vec3Iota;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.api.misc.MediaConstants;
import dev.hexsable.HexSableConfig;
import dev.hexsable.casting.SableConstAction;
import dev.hexsable.casting.SableRenderedSpell;
import dev.hexsable.casting.SableSpellAction;
import dev.hexsable.casting.SableUtil;
import dev.hexsable.iota.SubLevelIota;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static dev.hexsable.casting.SableUtil.TICKS_PER_SECOND;
import static dev.hexsable.casting.SableUtil.sneaky;

/**
 * Все действия аддона. Соглашения по единицам такие же, как у Entity в Hex:
 * скорости — блоки/тик, угловые — рад/тик, векторы — в мировой системе координат.
 */
public final class Ops {
    private Ops() {}

    private static final double DUST = MediaConstants.DUST_UNIT;
    private static final String GIVEN_MOTION_KEY = "hexsable:given_motion";

    private static Vec3 clampVec(Vec3 v, double max) {
        return v.lengthSqr() > max * max ? v.normalize().scale(max) : v;
    }

    // =====================================================================================
    // Чтение (константная стоимость, как у Entity-читалок)
    // =====================================================================================

    /** Structure Purification: вектор | сущность -> структура | null */
    public static final class GetAt extends SableConstAction {
        @Override public int getArgc() { return 1; }

        @Override
        public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
            Iota first = args.get(0);
            ServerSubLevel found;
            if (first instanceof EntityIota ei) {
                Entity e = ei.getEntity();
                SableUtil.assertEntityInRange(env, e);
                found = SableUtil.findUnder(e);
            } else {
                Vec3 pos = OperatorUtils.getVec3(args, 0, getArgc());
                SableUtil.assertVecInRange(env, pos);
                found = SableUtil.findAt(env.getWorld(), pos);
            }
            Iota out = found == null ? new NullIota() : new SubLevelIota(found);
            return List.of(out);
        }
    }

    /** Structure Zone Distillation: вектор, радиус -> [структуры] */
    public static final class GetInZone extends SableConstAction {
        @Override public int getArgc() { return 2; }

        @Override
        public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
            Vec3 center = OperatorUtils.getVec3(args, 0, getArgc());
            double radius = OperatorUtils.getPositiveDouble(args, 1, getArgc());
            SableUtil.assertVecInRange(env, center);
            List<Iota> list = new ArrayList<>();
            for (ServerSubLevel s : SableUtil.findInZone(env.getWorld(), center, radius, 64)) {
                list.add(new SubLevelIota(s));
            }
            return List.of(new ListIota(list));
        }
    }

    /** Structure position (центр масс, мир). */
    public static final class Pos extends SableConstAction {
        @Override public int getArgc() { return 1; }

        @Override
        public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
            ServerSubLevel sub = SableUtil.getSubLevel(args, 0, getArgc());
            SableUtil.assertInRange(env, sub);
            return List.of(new Vec3Iota(SableUtil.centerOfMass(sub)));
        }
    }

    /** Линейная (angular=false, блоков/тик) или угловая (angular=true, рад/тик) скорость в мировой системе. */
    public static final class Velocity extends SableConstAction {
        private final boolean angular;

        public Velocity(boolean angular) { this.angular = angular; }

        @Override public int getArgc() { return 1; }

        @Override
        public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
            ServerSubLevel sub = SableUtil.getSubLevel(args, 0, getArgc());
            SableUtil.assertInRange(env, sub);
            Vector3d v = new Vector3d();
            RigidBodyHandle h = SableUtil.handle(sub);
            if (h.isValid()) {
                if (angular) {
                    h.getAngularVelocity(v);
                } else {
                    h.getLinearVelocity(v);
                }
            }
            v.div(TICKS_PER_SECOND);
            return List.of(new Vec3Iota(new Vec3(v.x, v.y, v.z)));
        }
    }

    public static final class Mass extends SableConstAction {
        @Override public int getArgc() { return 1; }

        @Override
        public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
            ServerSubLevel sub = SableUtil.getSubLevel(args, 0, getArgc());
            SableUtil.assertInRange(env, sub);
            return List.of(new DoubleIota(SableUtil.mass(sub)));
        }
    }

    /** Мировой AABB: кладёт на стек min, затем max. */
    public static final class Bounds extends SableConstAction {
        @Override public int getArgc() { return 1; }

        @Override
        public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
            ServerSubLevel sub = SableUtil.getSubLevel(args, 0, getArgc());
            SableUtil.assertInRange(env, sub);
            BoundingBox3dc bb = sub.boundingBox();
            return List.of(
                    new Vec3Iota(new Vec3(bb.minX(), bb.minY(), bb.minZ())),
                    new Vec3Iota(new Vec3(bb.maxX(), bb.maxY(), bb.maxZ())));
        }
    }

    public enum TransformMode { POINT_TO_WORLD, POINT_TO_LOCAL, DIR_TO_WORLD, DIR_TO_LOCAL }

    /** Перевод точек/направлений между локальными (плот) и мировыми координатами структуры. */
    public static final class Transform extends SableConstAction {
        private final TransformMode mode;

        public Transform(TransformMode mode) { this.mode = mode; }

        @Override public int getArgc() { return 2; }

        @Override
        public List<Iota> execute(List<? extends Iota> args, CastingEnvironment env) {
            ServerSubLevel sub = SableUtil.getSubLevel(args, 0, getArgc());
            Vec3 v = OperatorUtils.getVec3(args, 1, getArgc());
            SableUtil.assertInRange(env, sub);
            Pose3d pose = sub.logicalPose();
            Vec3 out;
            switch (mode) {
                case POINT_TO_WORLD -> out = pose.transformPosition(v);
                case POINT_TO_LOCAL -> out = pose.transformPositionInverse(v);
                case DIR_TO_WORLD -> {
                    Vector3d d = SableUtil.joml(v);
                    pose.orientation().transform(d);
                    out = new Vec3(d.x, d.y, d.z);
                }
                default -> {
                    Vector3d d = SableUtil.joml(v);
                    pose.orientation().transformInverse(d);
                    out = new Vec3(d.x, d.y, d.z);
                }
            }
            return List.of(new Vec3Iota(out));
        }
    }

    // =====================================================================================
    // Заклинания
    // =====================================================================================

    private static ParticleSpray sprayAt(ServerSubLevel sub, Vec3 dir) {
        Vec3 d = dir.lengthSqr() < 1e-8 ? new Vec3(0, 1, 0) : dir.normalize();
        return new ParticleSpray(SableUtil.centerOfMass(sub), d, 0.0, 0.1, 16);
    }

    private static boolean checkAndMarkGivenMotion(CompoundTag userData, UUID id) {
        ListTag list = userData.getList(GIVEN_MOTION_KEY, Tag.TAG_STRING);
        String s = id.toString();
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(s)) {
                return true;
            }
        }
        list.add(StringTag.valueOf(s));
        userData.put(GIVEN_MOTION_KEY, list);
        return false;
    }

    /**
     * Structure Impulse: структура, dv (блоков/тик, мир). Толкает центр масс.
     * Если задана точка (atPoint) — импульс прикладывается в этой мировой точке и раскручивает структуру.
     */
    public static final class Impulse extends SableSpellAction {
        private final boolean atPoint;

        public Impulse(boolean atPoint) { this.atPoint = atPoint; }

        @Override public int getArgc() { return atPoint ? 3 : 2; }

        @Override
        public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userData) {
            int argc = getArgc();
            ServerSubLevel sub = SableUtil.getSubLevel(args, 0, argc);
            Vec3 worldPoint = atPoint ? OperatorUtils.getVec3(args, 1, argc) : null;
            Vec3 dv = clampVec(OperatorUtils.getVec3(args, argc - 1, argc), HexSableConfig.MAX_IMPULSE_SPEED.get());

            SableUtil.assertInRange(env, sub);
            if (worldPoint != null) {
                SableUtil.assertVecInRange(env, worldPoint);
            }

            MassData md = sub.getMassTracker();
            double mass = md.getMass();
            if (md.isInvalid() || mass <= 1e-6) {
                return new SpellAction.Result(new NoOp(), 0L, List.of(), 1L);
            }

            Pose3d pose = sub.logicalPose();
            // импульс в локальной системе структуры (так Sable принимает силы): J = m * dv * 20
            Vector3d localJ = SableUtil.joml(dv).mul(mass * TICKS_PER_SECOND);
            pose.orientation().transformInverse(localJ);

            Vector3d localPoint = null;
            double energy = mass * dv.lengthSqr(); // 2*KE в единицах "тик"
            if (worldPoint != null) {
                Vec3 lp = pose.transformPositionInverse(worldPoint);
                localPoint = new Vector3d(lp.x, lp.y, lp.z);
                Vector3d r = new Vector3d(localPoint).sub(md.getCenterOfMass());
                Vector3d jt = new Vector3d(localJ).div(TICKS_PER_SECOND);
                Vector3d tau = r.cross(jt, new Vector3d());
                Vector3d itau = new Vector3d(tau);
                md.getInverseInertiaTensor().transform(itau);
                energy = jt.lengthSquared() / mass + Math.max(0.0, tau.dot(itau));
            }

            boolean repeat = checkAndMarkGivenMotion(userData, sub.getUniqueId());
            double cost = DUST * HexSableConfig.IMPULSE_COST_FACTOR.get() * (energy + (repeat ? mass : 0.0));

            return new SpellAction.Result(new ImpulseSpell(sub, localJ, localPoint), (long) cost,
                    List.of(sprayAt(sub, dv)), 1L);
        }

        private static final class ImpulseSpell extends SableRenderedSpell {
            private final ServerSubLevel sub;
            private final Vector3d localJ;
            private final Vector3d localPoint;

            ImpulseSpell(ServerSubLevel sub, Vector3d localJ, Vector3d localPoint) {
                this.sub = sub;
                this.localJ = localJ;
                this.localPoint = localPoint;
            }

            @Override
            public void cast(CastingEnvironment env) {
                if (sub.isRemoved()) {
                    return;
                }
                RigidBodyHandle handle = SableUtil.handle(sub);
                if (!handle.isValid()) {
                    return;
                }
                ForceTotal total = new ForceTotal();
                if (localPoint != null) {
                    total.applyImpulseAtPoint(sub, localPoint, localJ);
                } else {
                    total.applyLinearImpulse(localJ);
                }
                handle.applyForcesAndReset(total);
            }
        }
    }

    /** Structure Torque: структура, dω (рад/тик, мир) -> раскручивает вокруг центра масс. */
    public static final class Spin extends SableSpellAction {
        @Override public int getArgc() { return 2; }

        @Override
        public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userData) {
            ServerSubLevel sub = SableUtil.getSubLevel(args, 0, getArgc());
            Vec3 dw = clampVec(OperatorUtils.getVec3(args, 1, getArgc()), HexSableConfig.MAX_SPIN.get());
            SableUtil.assertInRange(env, sub);

            MassData md = sub.getMassTracker();
            if (md.isInvalid() || md.getMass() <= 1e-6) {
                return new SpellAction.Result(new NoOp(), 0L, List.of(), 1L);
            }

            Vector3d localDw = SableUtil.joml(dw);
            sub.logicalPose().orientation().transformInverse(localDw);
            Vector3d idw = new Vector3d(localDw);
            md.getInertiaTensor().transform(idw);
            double energy = Math.max(0.0, localDw.dot(idw));
            double cost = DUST * HexSableConfig.SPIN_COST_FACTOR.get() * energy;

            // угловой импульс: L = I * (dω * 20)
            Vector3d localL = new Vector3d(idw).mul(TICKS_PER_SECOND);
            return new SpellAction.Result(new SpinSpell(sub, localL), (long) cost, List.of(sprayAt(sub, dw)), 1L);
        }

        private static final class SpinSpell extends SableRenderedSpell {
            private final ServerSubLevel sub;
            private final Vector3d localL;

            SpinSpell(ServerSubLevel sub, Vector3d localL) {
                this.sub = sub;
                this.localL = localL;
            }

            @Override
            public void cast(CastingEnvironment env) {
                if (sub.isRemoved()) {
                    return;
                }
                RigidBodyHandle handle = SableUtil.handle(sub);
                if (!handle.isValid()) {
                    return;
                }
                ForceTotal total = new ForceTotal();
                total.applyTorqueImpulse(localL);
                handle.applyForcesAndReset(total);
            }
        }
    }

    /** Structure Blink: структура, смещение (мир) -> мгновенно переносит структуру, сохраняя поворот и скорость. */
    public static final class Blink extends SableSpellAction {
        @Override public int getArgc() { return 2; }

        @Override
        public SpellAction.Result executeWithUserdata(List<? extends Iota> args, CastingEnvironment env, CompoundTag userData) {
            ServerSubLevel sub = SableUtil.getSubLevel(args, 0, getArgc());
            Vec3 delta = OperatorUtils.getVec3(args, 1, getArgc());
            SableUtil.assertInRange(env, sub);

            Vec3 from = SableUtil.centerOfMass(sub);
            Vec3 to = from.add(delta);

            if (!HexConfig.server().canTeleportInThisDimension(env.getWorld().dimension())) {
                throw sneaky(new MishapBadLocation(to, "bad_dimension"));
            }
            if (delta.length() > HexSableConfig.MAX_BLINK_DISTANCE.get()) {
                throw sneaky(new MishapBadLocation(to, "too_far"));
            }
            SableUtil.assertVecInRange(env, to);
            if (!env.isVecInWorld(to)) {
                throw sneaky(new MishapBadLocation(to, "too_close_to_out"));
            }

            double mass = Math.max(1.0, SableUtil.mass(sub));
            double cost = 0.5 * 50000.0 * delta.length() * Math.sqrt(mass) * HexSableConfig.BLINK_COST_FACTOR.get();

            return new SpellAction.Result(new BlinkSpell(sub, to), (long) cost,
                    List.of(ParticleSpray.cloud(from, 2.0, 50), ParticleSpray.burst(to, 2.0, 100)), 1L);
        }

        private static final class BlinkSpell extends SableRenderedSpell {
            private final ServerSubLevel sub;
            private final Vec3 to;

            BlinkSpell(ServerSubLevel sub, Vec3 to) {
                this.sub = sub;
                this.to = to;
            }

            @Override
            public void cast(CastingEnvironment env) {
                if (sub.isRemoved()) {
                    return;
                }
                RigidBodyHandle handle = SableUtil.handle(sub);
                if (!handle.isValid()) {
                    return;
                }
                handle.teleport(new Vector3d(to.x, to.y, to.z), new Quaterniond(sub.logicalPose().orientation()));
            }
        }
    }

    /** Пустой эффект (структура невесома / некорректна). */
    private static final class NoOp extends SableRenderedSpell {
        @Override public void cast(CastingEnvironment env) {}
    }
}
