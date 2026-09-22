package dev.hexsable.casting;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.castables.Action;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import dev.hexsable.casting.actions.Ops;

import java.util.ArrayList;
import java.util.List;

/**
 * Таблица паттернов аддона. Сигнатуры подобраны так, чтобы не совпадать ни с одним паттерном Hex/HexJS из MeowHex,
 * не начинаться с "aqaa"/"dedd" (числа) и содержать 'q' (Bookkeeper's mask состоит только из w/e/a/d).
 * Если в сборке есть ещё аддоны и паттерн конфликтует — просто поменяй строку здесь.
 */
public final class HexSableActions {
    public record Entry(String path, ActionRegistryEntry entry, String signature) {}

    private static final List<Entry> ALL = new ArrayList<>();

    private static void add(String path, String angles, Action action) {
        ALL.add(new Entry(path, new ActionRegistryEntry(HexPattern.fromAngles(angles, HexDir.EAST), action), angles));
    }

    static {
        // чтение
        add("sublevel/get",              "wqweweqq",  new Ops.GetAt());
        add("sublevel/zone",             "eqqeeweeq", new Ops.GetInZone());
        add("sublevel/pos",              "adqewde",   new Ops.Pos());
        add("sublevel/velocity",         "qeaeeadq",  new Ops.Velocity(false));
        add("sublevel/angular_velocity", "wedaweqd",  new Ops.Velocity(true));
        add("sublevel/mass",             "qwqqdeaq",  new Ops.Mass());
        add("sublevel/bounds",           "wqeeqqwaq", new Ops.Bounds());
        // координаты
        add("sublevel/to_world",         "eaqwdae",   new Ops.Transform(Ops.TransformMode.POINT_TO_WORLD));
        add("sublevel/to_local",         "aqwqeaww",  new Ops.Transform(Ops.TransformMode.POINT_TO_LOCAL));
        add("sublevel/dir_to_world",     "aeedqqaq",  new Ops.Transform(Ops.TransformMode.DIR_TO_WORLD));
        add("sublevel/dir_to_local",     "weqdadea",  new Ops.Transform(Ops.TransformMode.DIR_TO_LOCAL));
        // заклинания
        add("sublevel/impulse",          "qeweqqwe",  new Ops.Impulse(false));
        add("sublevel/impulse_at",       "ewqdeeaa",  new Ops.Impulse(true));
        add("sublevel/spin",             "qweqqwaq",  new Ops.Spin());
        add("sublevel/blink",            "dqqeadwqe", new Ops.Blink());
    }

    public static List<Entry> all() {
        return ALL;
    }

    private HexSableActions() {}
}
