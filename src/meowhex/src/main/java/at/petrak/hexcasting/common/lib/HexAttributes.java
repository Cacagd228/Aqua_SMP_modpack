package at.petrak.hexcasting.common.lib;

import at.petrak.hexcasting.api.HexAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

/**
 * These are setup in ForgeHexInit.
 */
public class HexAttributes {
    public static void register(BiConsumer<Attribute, ResourceLocation> r) {
        for (var e : ATTRIBUTES.entrySet()) {
            r.accept(e.getValue(), e.getKey());
        }
    }

    private static final Map<ResourceLocation, Attribute> ATTRIBUTES = new LinkedHashMap<>();

    public static final Attribute GRID_ZOOM = make("grid_zoom", new RangedAttribute(
        HexAPI.MOD_ID + ".attributes.grid_zoom", 1.0, 0.5, 4.0)).setSyncable(true);

    /**
     * Whether you have the lens overlay when looking at something. 0 = no, &gt; 0 = yes.
     */
    public static final Attribute SCRY_SIGHT = make("scry_sight", new RangedAttribute(
        HexAPI.MOD_ID + ".attributes.scry_sight", 0.0, 0.0, 1.0)).setSyncable(true);

    /**
     * How much mana regenerates per {@code ManaHelper.REGEN_PERIOD_TICKS} (default 1 mana / 6s).
     */
    public static final Attribute MANA_REGEN = make("mana_regen", new RangedAttribute(
        HexAPI.MOD_ID + ".attributes.mana_regen", 1.0, 0.0, 100.0)).setSyncable(true);

    /**
     * Maximum size of the mana pool (default 200).
     */
    public static final Attribute MANA_MAX = make("mana_max", new RangedAttribute(
        HexAPI.MOD_ID + ".attributes.mana_max", 200.0, 0.0, 32768.0)).setSyncable(true);

    /**
     * Fraction by which mana costs are reduced (0 = none, 1 = free).
     */
    public static final Attribute MANA_DISCOUNT = make("mana_discount", new RangedAttribute(
        HexAPI.MOD_ID + ".attributes.mana_discount", 0.0, 0.0, 1.0)).setSyncable(true);

    /**
     * &gt; 0 means the player has infinite mana (granted by wearing a creative unlocker).
     */
    public static final Attribute MANA_INFINITE = make("mana_infinite", new RangedAttribute(
        HexAPI.MOD_ID + ".attributes.mana_infinite", 0.0, 0.0, 1.0)).setSyncable(true);

    private static <T extends Attribute> T make(String id, T attr) {
        var old = ATTRIBUTES.put(modLoc(id), attr);
        if (old != null) {
            throw new IllegalArgumentException("Typo? Duplicate id " + id);
        }
        return attr;
    }
}
