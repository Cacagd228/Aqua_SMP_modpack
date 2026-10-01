package me.nanorasmus.meowrelics.balance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Разобранный баланс-файл. Полностью повторяет структуру конфига Relics
 * ({@code RelicConfigData}), но без {@code research} и {@code synergies}:
 * их менять бессмысленно — это чисто UI-часть.
 *
 * <p>Все поля nullable: {@code null} означает «оставить ванильное значение».
 * Такой подход позволяет в JSON перечислить только то, что реально меняешь.
 */
public final class BalanceSpec {

    /**
     * Опциональный множитель цены прокачки, применяемый ко всем реликвиям.
     * {@code 1.0} — ванильно. Помогает одним движением ускорить/замедлить прогрессию.
     */
    public Double levelingCostMultiplier = null;

    /** Опциональный множитель целевого значения статов (сила эффектов на максимуме). */
    public Double statTargetMultiplier = null;

    /** Ключ — ResourceLocation реликвии, напр. {@code relics:hunting_belt}. */
    public Map<String, RelicSpec> relics = new LinkedHashMap<>();

    public static final class RelicSpec {
        /** Уровни: стоимость прокачки и максимальный ранг. */
        public LevelingSpec leveling = null;
        /** Способности. Ключ — id способности, напр. {@code slots}, {@code pack}. */
        public Map<String, AbilitySpec> abilities = new LinkedHashMap<>();
        /** Условия появления в луте. */
        public LootSpec loot = null;
    }

    public static final class LevelingSpec {
        public Double initialCost = null;
        public Double step = null;
        public Integer maxRank = null;
    }

    public static final class AbilitySpec {
        /** Очков левела нужно на один уровень способности. */
        public Integer requiredPoints = null;
        /** Уровень реликвии, на котором способность открывается. */
        public Integer requiredLevel = null;
        /** Максимальный уровень способности. */
        public Integer maxLevel = null;
        /** Статы способности. */
        public Map<String, StatSpec> stats = new LinkedHashMap<>();
    }

    public static final class StatSpec {
        /** Диапазон базового значения, рандомится при создании реликвии. */
        public Double initialMin = null;
        public Double initialMax = null;
        /** Жёсткие границы, выше которых прокачка не выводит стат. */
        public Double thresholdMin = null;
        public Double thresholdMax = null;
        /** Значение стата на максимальном качестве и уровне. */
        public Double targetValue = null;
        /** id модели масштабирования из relics:scaling_models, напр. {@code relics:additive}. */
        public String scalingModel = null;
    }

    public static final class LootSpec {
        /** Полная замена списка условий появления реликвии. */
        public List<LootEntrySpec> entries = null;
    }

    public static final class LootEntrySpec {
        public List<String> dimensions = null;
        public List<String> biomes = null;
        public List<String> tables = null;
        public Integer weight = null;
    }
}