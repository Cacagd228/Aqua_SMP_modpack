package dev.hexsable.casting;

import at.petrak.hexcasting.api.casting.OperatorUtils;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation;
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota;
import at.petrak.hexcasting.api.casting.mishaps.MishapLocationInWrongDimension;
import dev.hexsable.iota.SubLevelIota;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Общие помощники: разбор аргументов, проверки дальности, поиск структур. */
public final class SableUtil {
    /** Sable считает в секундах, Hex (как и Entity) в тиках. */
    public static final double TICKS_PER_SECOND = 20.0;

    private SableUtil() {}

    /**
     * Бросает Mishap (он extends Throwable, а не RuntimeException) из Java-кода, не объявляя throws.
     * Использование: {@code throw SableUtil.sneaky(new MishapXxx(...));}
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> RuntimeException sneaky(Throwable t) throws E {
        throw (E) t;
    }

    /** Методы Hex объявляют throws Mishap (Kotlin @Throws) — оборачиваем, чтобы не тащить throws в каждое действие. */
    public static void assertVecInRange(CastingEnvironment env, Vec3 v) {
        try {
            env.assertVecInRange(v);
        } catch (Mishap m) {
            throw sneaky(m);
        }
    }

    public static void assertEntityInRange(CastingEnvironment env, Entity e) {
        try {
            env.assertEntityInRange(e);
        } catch (Mishap m) {
            throw sneaky(m);
        }
    }

    public static Vec3 toMojang(Vector3dc v) {
        return JOMLConversion.toMojang(v);
    }

    // ---------------------------------------------------------------- аргументы

    public static ServerSubLevel getSubLevel(List<? extends Iota> args, int idx, int argc) {
        Iota x = args.get(idx);
        if (x instanceof SubLevelIota s) {
            ServerSubLevel sub = s.getSubLevel();
            if (!sub.isRemoved()) {
                return sub;
            }
        }
        throw sneaky(MishapInvalidIota.ofType(x, argc == 0 ? 0 : argc - (idx + 1), "sublevel"));
    }

    public static Vec3 getVec3(List<? extends Iota> args, int idx, int argc) {
        return OperatorUtils.getVec3(args, idx, argc);
    }

    // ---------------------------------------------------------------- геометрия

    /** Мировой центр масс (инвариант Sable: pose.position == мировая позиция rotationPoint == локального центра масс). */
    public static Vec3 centerOfMass(ServerSubLevel sub) {
        return toMojang(sub.logicalPose().position());
    }

    public static double mass(ServerSubLevel sub) {
        return sub.getMassTracker().getMass();
    }

    public static RigidBodyHandle handle(ServerSubLevel sub) {
        return RigidBodyHandle.of(sub);
    }

    // ---------------------------------------------------------------- дальность

    /** Структура «в зоне досягаемости», если в неё попадает центр масс, центр или любой угол мирового AABB. */
    public static boolean isInRange(CastingEnvironment env, ServerSubLevel sub) {
        if (sub.getLevel() != env.getWorld()) {
            return false;
        }
        if (env.isVecInRange(centerOfMass(sub))) {
            return true;
        }
        BoundingBox3dc bb = sub.boundingBox();
        double[] xs = {bb.minX(), bb.maxX()};
        double[] ys = {bb.minY(), bb.maxY()};
        double[] zs = {bb.minZ(), bb.maxZ()};
        if (env.isVecInRange(new Vec3((xs[0] + xs[1]) * 0.5, (ys[0] + ys[1]) * 0.5, (zs[0] + zs[1]) * 0.5))) {
            return true;
        }
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    if (env.isVecInRange(new Vec3(x, y, z))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void assertInRange(CastingEnvironment env, ServerSubLevel sub) {
        if (sub.getLevel() != env.getWorld()) {
            throw sneaky(new MishapLocationInWrongDimension(env.getWorld().dimension().location()));
        }
        if (!isInRange(env, sub)) {
            throw sneaky(new MishapBadLocation(centerOfMass(sub), "too_far"));
        }
    }

    // ---------------------------------------------------------------- поиск

    private static List<ServerSubLevel> all(ServerLevel level) {
        List<ServerSubLevel> out = new ArrayList<>();
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return out;
        }
        for (Object o : container.getAllSubLevels()) {
            if (o instanceof ServerSubLevel s && !s.isRemoved()) {
                out.add(s);
            }
        }
        return out;
    }

    /** Структура в точке: точка может быть как в плот-координатах, так и в мировых. */
    public static ServerSubLevel findAt(ServerLevel level, Vec3 pos) {
        SubLevel plot = Sable.HELPER.getContaining(level, pos);
        if (plot instanceof ServerSubLevel s && !s.isRemoved()) {
            return s;
        }

        ServerSubLevel best = null;
        double bestDist = Double.MAX_VALUE;
        for (ServerSubLevel s : all(level)) {
            if (!s.boundingBox().contains(pos.x, pos.y, pos.z)) {
                continue;
            }
            Vec3 local = s.logicalPose().transformPositionInverse(pos);
            var lb = s.getPlot().getBoundingBox();
            // с запасом: raycast часто попадает ровно на грань блока
            boolean inside = local.x >= lb.minX() - 0.51 && local.x <= lb.maxX() + 1.51
                    && local.y >= lb.minY() - 0.51 && local.y <= lb.maxY() + 1.51
                    && local.z >= lb.minZ() - 0.51 && local.z <= lb.maxZ() + 1.51;
            if (!inside) {
                continue;
            }
            double d = centerOfMass(s).distanceToSqr(pos);
            if (d < bestDist) {
                bestDist = d;
                best = s;
            }
        }
        return best;
    }

    public static ServerSubLevel findUnder(Entity entity) {
        SubLevel s = Sable.HELPER.getTrackingOrVehicleSubLevel(entity);
        if (s instanceof ServerSubLevel ss && !ss.isRemoved()) {
            return ss;
        }
        s = Sable.HELPER.getContaining(entity);
        return s instanceof ServerSubLevel ss && !ss.isRemoved() ? ss : null;
    }

    /** Структуры, чей мировой AABB пересекает шар радиуса r; отсортированы по расстоянию до центра масс. */
    public static List<ServerSubLevel> findInZone(ServerLevel level, Vec3 center, double radius, int limit) {
        List<ServerSubLevel> out = new ArrayList<>();
        for (ServerSubLevel s : all(level)) {
            BoundingBox3dc bb = s.boundingBox();
            double dx = Math.max(Math.max(bb.minX() - center.x, 0), center.x - bb.maxX());
            double dy = Math.max(Math.max(bb.minY() - center.y, 0), center.y - bb.maxY());
            double dz = Math.max(Math.max(bb.minZ() - center.z, 0), center.z - bb.maxZ());
            if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                out.add(s);
            }
        }
        out.sort(Comparator.comparingDouble(s -> centerOfMass(s).distanceToSqr(center)));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    // ---------------------------------------------------------------- прочее

    /** Копия позы для чтения (Pose3d изменяем, поэтому не отдаём наружу оригинал). */
    public static Pose3d pose(ServerSubLevel sub) {
        return sub.logicalPose();
    }

    public static Vector3d joml(Vec3 v) {
        return new Vector3d(v.x, v.y, v.z);
    }

    public static BlockPos blockPos(Vec3 v) {
        return BlockPos.containing(v);
    }
}
