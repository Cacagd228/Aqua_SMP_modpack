package me.nanorasmus.nanodev.hex_js.addon.interop;

import at.petrak.hexcasting.common.lib.HexAttributes;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Optional Apothic Attributes interop.
 *
 * <p>How the two attribute systems relate (verified against
 * ApothicAttributes 2.10.1):
 * <ul>
 *   <li>Apothic merges its own modifiers through the vanilla
 *   {@code ItemAttributeModifierEvent} and the Curios
 *   {@code CurioAttributeModifierEvent}. Hex staff modifiers (vanilla
 *   {@code ItemAttributeModifiers} component, e.g. grid zoom) and Hex bauble
 *   modifiers (Curios multimap, e.g. mana max) therefore keep working with
 *   Apothic installed — no mirroring here on purpose, it would double-apply.</li>
 *   <li>Apothic combat attributes (crit chance/damage, dodge, arrow damage)
 *   hook vanilla damage/heal events, so they already affect Hex spell damage
 *   that goes through the vanilla pipeline (e.g. Apollo's Arrow).</li>
 *   <li>Hex attributes live under {@code hexcasting:<id>}:
 *   {@code mana_max}, {@code mana_regen}, {@code mana_discount},
 *   {@code mana_infinite}, {@code grid_zoom}, {@code scry_sight}. External
  *   systems (Apothic bonus modifiers and other external attribute powers) can
 *   target them by id with no code on this side.</li>
 * </ul>
 *
 * <p>This class is the explicit bridge: safe Apothic attribute readers (sane
 * defaults when Apothic is absent) plus Hex mana bonus helpers for origins and
 * packs. It deliberately uses only vanilla registry lookups — no direct
 * references to Apothic classes — so it is safe to load with or without the
 * mod. Call sites must still gate {@link #init()} behind {@link #isLoaded()},
 * mirroring the Curios interop pattern.
 */
public final class ApothicInterop {
    public static final String APOTHIC_ID = "apothic_attributes";
    public static final String PLACEBO_ID = "placebo";

    public static final ResourceLocation CRIT_CHANCE_ID = apothicLoc("crit_chance");
    public static final ResourceLocation CRIT_DAMAGE_ID = apothicLoc("crit_damage");
    public static final ResourceLocation DODGE_CHANCE_ID = apothicLoc("dodge_chance");
    public static final ResourceLocation COOLDOWN_REDUCTION_ID = apothicLoc("cooldown_reduction");
    public static final ResourceLocation ARROW_DAMAGE_ID = apothicLoc("arrow_damage");
    public static final ResourceLocation PROJECTILE_DAMAGE_ID = apothicLoc("projectile_damage");

    /** Vanilla Apothic base crit chance (matches Apothic defaults). */
    public static final double DEFAULT_CRIT_CHANCE = 0.05;
    /** Vanilla Apothic base crit damage (matches Apothic defaults). */
    public static final double DEFAULT_CRIT_DAMAGE = 1.5;

    private static Holder<Attribute> critChance;
    private static Holder<Attribute> critDamage;
    private static Holder<Attribute> dodgeChance;
    private static Holder<Attribute> cooldownReduction;
    private static Holder<Attribute> arrowDamage;
    private static Holder<Attribute> projectileDamage;
    private static boolean resolved;

    private ApothicInterop() {
    }

    public static boolean isLoaded() {
        return net.neoforged.fml.ModList.get().isLoaded(APOTHIC_ID);
    }

    /**
     * Logs the integration state. Registry holders are resolved lazily on
     * first getter use (gameplay time, registries frozen) — never here, this
     * runs from the mod constructor.
     */
    public static void init() {
        HexJS.LOGGER.info("[MeowHex] Apothic Attributes detected: attribute integration active.");
    }

    /** Resolves Apothic holders once, on first gameplay-time use. */
    private static void ensureResolved() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!isLoaded()) {
            return;
        }
        critChance = lookup(CRIT_CHANCE_ID);
        critDamage = lookup(CRIT_DAMAGE_ID);
        dodgeChance = lookup(DODGE_CHANCE_ID);
        cooldownReduction = lookup(COOLDOWN_REDUCTION_ID);
        arrowDamage = lookup(ARROW_DAMAGE_ID);
        projectileDamage = lookup(PROJECTILE_DAMAGE_ID);
        if (critChance == null || critDamage == null || dodgeChance == null) {
            HexJS.LOGGER.warn("[MeowHex] Apothic Attributes is present but its attributes are not in the registry yet.");
        }
    }

    private static Holder<Attribute> lookup(ResourceLocation id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(id).orElse(null);
    }

    private static ResourceLocation apothicLoc(String path) {
        return ResourceLocation.fromNamespaceAndPath(APOTHIC_ID, path);
    }

    private static double valueOr(LivingEntity entity, Holder<Attribute> holder, double fallback) {
        if (holder == null || entity == null) {
            return fallback;
        }
        AttributeInstance inst = entity.getAttribute(holder);
        return inst == null ? fallback : inst.getValue();
    }

    /** Apothic crit chance, or {@link #DEFAULT_CRIT_CHANCE} when unavailable. */
    public static double getCritChance(LivingEntity entity) {
        ensureResolved();
        return valueOr(entity, critChance, DEFAULT_CRIT_CHANCE);
    }

    /** Apothic crit damage multiplier, or {@link #DEFAULT_CRIT_DAMAGE} when unavailable. */
    public static double getCritDamage(LivingEntity entity) {
        ensureResolved();
        return valueOr(entity, critDamage, DEFAULT_CRIT_DAMAGE);
    }

    /** Apothic dodge chance, or 0 when unavailable. */
    public static double getDodgeChance(LivingEntity entity) {
        ensureResolved();
        return valueOr(entity, dodgeChance, 0.0);
    }

    /** Apothic cooldown reduction, or 0 when unavailable. */
    public static double getCooldownReduction(LivingEntity entity) {
        ensureResolved();
        return valueOr(entity, cooldownReduction, 0.0);
    }

    // ------------------------------------------------------------------
    // Hex-side bonus helpers (for origins / packs / commands).
    // ------------------------------------------------------------------

    private static Holder<Attribute> hexHolder(Attribute attribute) {
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute);
    }

    private static void setBonus(ServerPlayer player, Attribute attribute, ResourceLocation id,
            double amount, AttributeModifier.Operation op) {
        if (player == null || id == null) {
            return;
        }
        AttributeInstance inst = player.getAttribute(hexHolder(attribute));
        if (inst == null) {
            return;
        }
        inst.removeModifier(id);
        if (Math.abs(amount) > 1e-9) {
            inst.addTransientModifier(new AttributeModifier(id, amount, op));
        }
    }

    private static void clearBonus(ServerPlayer player, Attribute attribute, ResourceLocation id) {
        if (player == null || id == null) {
            return;
        }
        AttributeInstance inst = player.getAttribute(hexHolder(attribute));
        if (inst != null) {
            inst.removeModifier(id);
        }
    }

    /**
     * Adds (or refreshes) a transient {@code hexcasting:mana_max} bonus, e.g.
     * from an origin's {@code startMana}. Zero removes the modifier.
     */
    public static void setManaMaxBonus(ServerPlayer player, ResourceLocation id, double amount) {
        setBonus(player, HexAttributes.MANA_MAX, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    /** Adds (or refreshes) a transient {@code hexcasting:mana_regen} bonus. Zero removes it. */
    public static void setManaRegenBonus(ServerPlayer player, ResourceLocation id, double amount) {
        setBonus(player, HexAttributes.MANA_REGEN, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    /**
     * Adds (or refreshes) a transient {@code hexcasting:mana_discount} bonus.
     * Expected range is {@code 0..1}; zero removes the modifier.
     */
    public static void setManaDiscountBonus(ServerPlayer player, ResourceLocation id, double amount) {
        setBonus(player, HexAttributes.MANA_DISCOUNT, id, amount, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void clearManaMaxBonus(ServerPlayer player, ResourceLocation id) {
        clearBonus(player, HexAttributes.MANA_MAX, id);
    }

    public static void clearManaRegenBonus(ServerPlayer player, ResourceLocation id) {
        clearBonus(player, HexAttributes.MANA_REGEN, id);
    }

    public static void clearManaDiscountBonus(ServerPlayer player, ResourceLocation id) {
        clearBonus(player, HexAttributes.MANA_DISCOUNT, id);
    }
}
