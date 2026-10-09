package dev.hexsable.scroll;

import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import dev.hexsable.casting.HexSableActions;

import java.util.List;
import java.util.Map;

/**
 * Каталог свитков Hex Sable Bridge. Сделан по образцу
 * {@code me.nanorasmus.nanodev.hex_js.addon.scroll.ScrollDefs} из MeowHex.
 *
 * <p>Схема строгая 1:1:1 — одна руна, один свиток, одна страница thehexbook
 * в категории Hex Sable. Поднятие свитка выдаёт парный
 * {@code hexsable:scrolls/*} advancement, который показывает страницу
 * и разрешает каст описанного на ней узора ({@link SableScrollGate}).
 */
public final class SableScrollDefs {
    private SableScrollDefs() {
    }

    /** Все item id свитков, стабильный порядок регистрации. */
    public static final List<String> SCROLLS = List.of(
            "scroll_get_sable",
            "scroll_zone_sable",
            "scroll_pos_sable",
            "scroll_velocity_sable",
            "scroll_angular_velocity_sable",
            "scroll_mass_sable",
            "scroll_bounds_sable",
            "scroll_to_world_sable",
            "scroll_to_local_sable",
            "scroll_dir_to_world_sable",
            "scroll_dir_to_local_sable",
            "scroll_impulse_sable",
            "scroll_impulse_at_sable",
            "scroll_spin_sable",
            "scroll_blink_sable");

    /** Scroll item id -> advancement, открывающий его страницу книги. */
    public static final Map<String, String> SCROLL_TO_ADVANCEMENT = Map.ofEntries(
            Map.entry("scroll_get_sable", "hexsable:scrolls/sable_get"),
            Map.entry("scroll_zone_sable", "hexsable:scrolls/sable_zone"),
            Map.entry("scroll_pos_sable", "hexsable:scrolls/sable_pos"),
            Map.entry("scroll_velocity_sable", "hexsable:scrolls/sable_velocity"),
            Map.entry("scroll_angular_velocity_sable", "hexsable:scrolls/sable_angular_velocity"),
            Map.entry("scroll_mass_sable", "hexsable:scrolls/sable_mass"),
            Map.entry("scroll_bounds_sable", "hexsable:scrolls/sable_bounds"),
            Map.entry("scroll_to_world_sable", "hexsable:scrolls/sable_to_world"),
            Map.entry("scroll_to_local_sable", "hexsable:scrolls/sable_to_local"),
            Map.entry("scroll_dir_to_world_sable", "hexsable:scrolls/sable_dir_to_world"),
            Map.entry("scroll_dir_to_local_sable", "hexsable:scrolls/sable_dir_to_local"),
            Map.entry("scroll_impulse_sable", "hexsable:scrolls/sable_impulse"),
            Map.entry("scroll_impulse_at_sable", "hexsable:scrolls/sable_impulse_at"),
            Map.entry("scroll_spin_sable", "hexsable:scrolls/sable_spin"),
            Map.entry("scroll_blink_sable", "hexsable:scrolls/sable_blink"));

    /** Scroll item id -> ключ названия страницы (для тултипа). */
    public static final Map<String, String> SCROLL_TO_NAME_KEY = Map.ofEntries(
            Map.entry("scroll_get_sable", "hexcasting.action.hexsable:sublevel/get"),
            Map.entry("scroll_zone_sable", "hexcasting.action.hexsable:sublevel/zone"),
            Map.entry("scroll_pos_sable", "hexcasting.action.hexsable:sublevel/pos"),
            Map.entry("scroll_velocity_sable", "hexcasting.action.hexsable:sublevel/velocity"),
            Map.entry("scroll_angular_velocity_sable", "hexcasting.action.hexsable:sublevel/angular_velocity"),
            Map.entry("scroll_mass_sable", "hexcasting.action.hexsable:sublevel/mass"),
            Map.entry("scroll_bounds_sable", "hexcasting.action.hexsable:sublevel/bounds"),
            Map.entry("scroll_to_world_sable", "hexcasting.action.hexsable:sublevel/to_world"),
            Map.entry("scroll_to_local_sable", "hexcasting.action.hexsable:sublevel/to_local"),
            Map.entry("scroll_dir_to_world_sable", "hexcasting.action.hexsable:sublevel/dir_to_world"),
            Map.entry("scroll_dir_to_local_sable", "hexcasting.action.hexsable:sublevel/dir_to_local"),
            Map.entry("scroll_impulse_sable", "hexcasting.action.hexsable:sublevel/impulse"),
            Map.entry("scroll_impulse_at_sable", "hexcasting.action.hexsable:sublevel/impulse_at"),
            Map.entry("scroll_spin_sable", "hexcasting.action.hexsable:sublevel/spin"),
            Map.entry("scroll_blink_sable", "hexcasting.action.hexsable:sublevel/blink"));

    /** Scroll item id -> ключ названия книги (для тултипа). */
    public static final Map<String, String> SCROLL_TO_BOOK_KEY = Map.ofEntries(
            Map.entry("scroll_get_sable", "item.hexcasting.book"),
            Map.entry("scroll_zone_sable", "item.hexcasting.book"),
            Map.entry("scroll_pos_sable", "item.hexcasting.book"),
            Map.entry("scroll_velocity_sable", "item.hexcasting.book"),
            Map.entry("scroll_angular_velocity_sable", "item.hexcasting.book"),
            Map.entry("scroll_mass_sable", "item.hexcasting.book"),
            Map.entry("scroll_bounds_sable", "item.hexcasting.book"),
            Map.entry("scroll_to_world_sable", "item.hexcasting.book"),
            Map.entry("scroll_to_local_sable", "item.hexcasting.book"),
            Map.entry("scroll_dir_to_world_sable", "item.hexcasting.book"),
            Map.entry("scroll_dir_to_local_sable", "item.hexcasting.book"),
            Map.entry("scroll_impulse_sable", "item.hexcasting.book"),
            Map.entry("scroll_impulse_at_sable", "item.hexcasting.book"),
            Map.entry("scroll_spin_sable", "item.hexcasting.book"),
            Map.entry("scroll_blink_sable", "item.hexcasting.book"));

    /** Scroll item id -> op id, описанный на его странице. */
    public static final Map<String, List<String>> SCROLL_TO_OPS = Map.ofEntries(
            Map.entry("scroll_get_sable", List.of("hexsable:sublevel/get")),
            Map.entry("scroll_zone_sable", List.of("hexsable:sublevel/zone")),
            Map.entry("scroll_pos_sable", List.of("hexsable:sublevel/pos")),
            Map.entry("scroll_velocity_sable", List.of("hexsable:sublevel/velocity")),
            Map.entry("scroll_angular_velocity_sable", List.of("hexsable:sublevel/angular_velocity")),
            Map.entry("scroll_mass_sable", List.of("hexsable:sublevel/mass")),
            Map.entry("scroll_bounds_sable", List.of("hexsable:sublevel/bounds")),
            Map.entry("scroll_to_world_sable", List.of("hexsable:sublevel/to_world")),
            Map.entry("scroll_to_local_sable", List.of("hexsable:sublevel/to_local")),
            Map.entry("scroll_dir_to_world_sable", List.of("hexsable:sublevel/dir_to_world")),
            Map.entry("scroll_dir_to_local_sable", List.of("hexsable:sublevel/dir_to_local")),
            Map.entry("scroll_impulse_sable", List.of("hexsable:sublevel/impulse")),
            Map.entry("scroll_impulse_at_sable", List.of("hexsable:sublevel/impulse_at")),
            Map.entry("scroll_spin_sable", List.of("hexsable:sublevel/spin")),
            Map.entry("scroll_blink_sable", List.of("hexsable:sublevel/blink")));

    /** Op id -> advancement id, любой из которых разрешает каст. */
    public static final Map<String, List<String>> OP_TO_ADVANCEMENTS = Map.ofEntries(
            Map.entry("hexsable:sublevel/get", List.of("hexsable:scrolls/sable_get")),
            Map.entry("hexsable:sublevel/zone", List.of("hexsable:scrolls/sable_zone")),
            Map.entry("hexsable:sublevel/pos", List.of("hexsable:scrolls/sable_pos")),
            Map.entry("hexsable:sublevel/velocity", List.of("hexsable:scrolls/sable_velocity")),
            Map.entry("hexsable:sublevel/angular_velocity", List.of("hexsable:scrolls/sable_angular_velocity")),
            Map.entry("hexsable:sublevel/mass", List.of("hexsable:scrolls/sable_mass")),
            Map.entry("hexsable:sublevel/bounds", List.of("hexsable:scrolls/sable_bounds")),
            Map.entry("hexsable:sublevel/to_world", List.of("hexsable:scrolls/sable_to_world")),
            Map.entry("hexsable:sublevel/to_local", List.of("hexsable:scrolls/sable_to_local")),
            Map.entry("hexsable:sublevel/dir_to_world", List.of("hexsable:scrolls/sable_dir_to_world")),
            Map.entry("hexsable:sublevel/dir_to_local", List.of("hexsable:scrolls/sable_dir_to_local")),
            Map.entry("hexsable:sublevel/impulse", List.of("hexsable:scrolls/sable_impulse")),
            Map.entry("hexsable:sublevel/impulse_at", List.of("hexsable:scrolls/sable_impulse_at")),
            Map.entry("hexsable:sublevel/spin", List.of("hexsable:scrolls/sable_spin")),
            Map.entry("hexsable:sublevel/blink", List.of("hexsable:scrolls/sable_blink")));

    /**
     * Штрихи руны, которую открывает свиток. Все руны Sable — обычные паттерны
     * с фиксированными сигнатурами (см. {@link HexSableActions}), поэтому штрихи
     * известны статически: превью в тултипе и размещение на стену работают даже
     * без запечённого в стак NBT (в отличие от per-world great spells, чьи
     * штрихи уникальны для каждого мира).
     *
     * @return узор или null, если свиток неизвестен
     */
    public static HexPattern patternForScroll(String scrollId) {
        List<String> ops = SCROLL_TO_OPS.getOrDefault(scrollId, List.of());
        if (ops.isEmpty()) {
            return null;
        }
        String op = ops.get(0);
        String prefix = "hexsable:";
        String path = op.startsWith(prefix) ? op.substring(prefix.length()) : op;
        for (HexSableActions.Entry e : HexSableActions.all()) {
            if (e.path().equals(path)) {
                return HexPattern.fromAngles(e.signature(), HexDir.EAST);
            }
        }
        return null;
    }

    /** Op id руны, которую открывает свиток, или null. */
    public static String opForScroll(String scrollId) {
        List<String> ops = SCROLL_TO_OPS.getOrDefault(scrollId, List.of());
        return ops.isEmpty() ? null : ops.get(0);
    }
}
