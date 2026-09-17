package ru.apofix;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Реестр атрибутов Apofix.
 *
 * <p>Только параметры, без игровой логики. Логика (расход маны, реген,
 * применение скидки и резистов) реализуется отдельным модом.
 *
 * <p>Рекомендуемые формулы для потребителя:
 * <ul>
 *   <li>Итоговая стоимость: {@code cost * (1 - mana_discount)}
 *   <li>Бесплатный каст: ролл {@code random < free_cast_chance} — стоимость 0.
 *   <li>Сайленс: {@code silence_status >= 0.5} (фактически 1) — каст запрещён.
 *   <li>Итоговый маг. урон: {@code damage * (1 - magic_resist) * (1 - specific_resist)},
 *       где specific — fire/water/earth/air по типу урона.
 * </ul>
 */
public final class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
        DeferredRegister.create(Registries.ATTRIBUTE, Apofix.MODID);

    /** Макс. мана. База 200. */
    public static final DeferredHolder<Attribute, Attribute> MAX_MANA = ATTRIBUTES.register("max_mana",
        () -> new RangedAttribute("apofix:max_mana", 200.0D, 0.0D, 100000.0D).setSyncable(true));

    /** Реген маны (ед./сек). База 1.0. */
    public static final DeferredHolder<Attribute, Attribute> MANA_REGEN = ATTRIBUTES.register("mana_regen",
        () -> new RangedAttribute("apofix:mana_regen", 1.0D, 0.0D, 10000.0D).setSyncable(true));

    /**
     * Скидка на стоимость маны, доля 0..1.
     * 0 = без скидки, 1 = 100% (бесплатно). Отрицательные значения = удорожание.
     */
    public static final DeferredHolder<Attribute, Attribute> MANA_DISCOUNT = ATTRIBUTES.register("mana_discount",
        () -> new PercentageAttribute("apofix:mana_discount", 0.0D, -10.0D, 1.0D).setSyncable(true));

    /** Общий маг. резист, доля 0..1. 1 = полный иммунитет. */
    public static final DeferredHolder<Attribute, Attribute> MAGIC_RESIST = ATTRIBUTES.register("magic_resist",
        () -> new PercentageAttribute("apofix:magic_resist", 0.0D, -10.0D, 1.0D).setSyncable(true));

    /** Маг. резист к огню, доля 0..1. */
    public static final DeferredHolder<Attribute, Attribute> FIRE_MAGIC_RESIST = ATTRIBUTES.register("fire_magic_resist",
        () -> new PercentageAttribute("apofix:fire_magic_resist", 0.0D, -10.0D, 1.0D).setSyncable(true));

    /** Маг. резист к воде, доля 0..1. */
    public static final DeferredHolder<Attribute, Attribute> WATER_MAGIC_RESIST = ATTRIBUTES.register("water_magic_resist",
        () -> new PercentageAttribute("apofix:water_magic_resist", 0.0D, -10.0D, 1.0D).setSyncable(true));

    /** Маг. резист к земле, доля 0..1. */
    public static final DeferredHolder<Attribute, Attribute> EARTH_MAGIC_RESIST = ATTRIBUTES.register("earth_magic_resist",
        () -> new PercentageAttribute("apofix:earth_magic_resist", 0.0D, -10.0D, 1.0D).setSyncable(true));

    /** Маг. резист к воздуху, доля 0..1. */
    public static final DeferredHolder<Attribute, Attribute> AIR_MAGIC_RESIST = ATTRIBUTES.register("air_magic_resist",
        () -> new PercentageAttribute("apofix:air_magic_resist", 0.0D, -10.0D, 1.0D).setSyncable(true));

    /**
     * Шанс бесплатного каста, доля 0..1.
     * 0 = никогда, 1 = каждый каст бесплатен (ману не тратит).
     */
    public static final DeferredHolder<Attribute, Attribute> FREE_CAST_CHANCE = ATTRIBUTES.register("free_cast_chance",
        () -> new PercentageAttribute("apofix:free_cast_chance", 0.0D, 0.0D, 1.0D).setSyncable(true));

    /**
     * Сайленс-статус, 0 или 1 (булевое как число).
     * 0 = каст разрешён, 1 = каст запрещён. Потребителю проверять {@code value >= 0.5}.
     */
    public static final DeferredHolder<Attribute, Attribute> SILENCE_STATUS = ATTRIBUTES.register("silence_status",
        () -> new RangedAttribute("apofix:silence_status", 0.0D, 0.0D, 1.0D).setSyncable(true));

    /** Все атрибуты одним списком — для навешивания на EntityType в эвенте. */
    public static final List<DeferredHolder<Attribute, Attribute>> ALL = List.of(
        MAX_MANA, MANA_REGEN, MANA_DISCOUNT,
        MAGIC_RESIST, FIRE_MAGIC_RESIST, WATER_MAGIC_RESIST, EARTH_MAGIC_RESIST, AIR_MAGIC_RESIST,
        FREE_CAST_CHANCE, SILENCE_STATUS
    );

    private ModAttributes() {}
}
