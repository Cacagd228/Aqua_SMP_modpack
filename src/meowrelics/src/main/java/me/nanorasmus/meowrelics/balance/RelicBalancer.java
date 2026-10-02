package me.nanorasmus.meowrelics.balance;

import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate.AbilitiesTemplateBuilder;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate.AbilityTemplateBuilder;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate.StatTemplateBuilder;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.misc.InitialValue;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.misc.TargetValue;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.misc.ThresholdValue;
import it.hurts.sskirillss.relics.api.scaling_models.ScalingModel;
import it.hurts.sskirillss.relics.init.RelicsRegistries;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Применяет {@link BalanceSpec} к шаблону реликвии.
 *
 * <p>Ключевой принцип: пересобираем шаблон через {@code toBuilder()} от
 * <em>дефолтного</em> шаблона, а не от текущего модифицированного. Иначе
 * повторная загрузка конфига (тот же {@code /meowrelics reload}) накапливала бы
 * изменения и в итоге ломала бы значения.
 */
public final class RelicBalancer {

    private final Logger log;

    public RelicBalancer(Logger log) {
        this.log = log;
    }

    /** Интерфейс логгера, чтобы не тащить slf4j в чистую логику. */
    public interface Logger {
        void warn(String msg);

        void info(String msg);
    }

    /**
     * Собирает новый шаблон реликвии на основе дефолтного.
     *
     * @return новый шаблон, либо {@code null}, если для реликвии нет оверрайдов
     */
    public RelicTemplate apply(IRelicItem relic, RelicTemplate vanilla, BalanceSpec.RelicSpec spec,
                               Double costMultiplier, Double statMultiplier) {
        if (spec == null) {
            return null;
        }
        return vanilla.toBuilder()
                .abilities(buildAbilities(relic, vanilla.getAbilities(), spec, statMultiplier))
                .leveling(buildLeveling(vanilla.getLeveling(), spec.leveling, costMultiplier))
                .loot(buildLoot(vanilla.getLoot(), spec.loot))
                .build();
    }

    private AbilitiesTemplate buildAbilities(IRelicItem relic, AbilitiesTemplate vanilla,
                                             BalanceSpec.RelicSpec spec, Double statMultiplier) {
        if (spec.abilities == null || spec.abilities.isEmpty()) {
            return vanilla;
        }
        AbilitiesTemplateBuilder builder = vanilla.toBuilder();

        // Выключенные способности выкидываем из карты, а не правим поштучно:
        // сначала собираем правки, потом пересобираем карту целиком.
        Map<String, AbilityTemplate> result = new java.util.LinkedHashMap<>(vanilla.getAbilities());
        boolean anyDisabled = false;
        for (Map.Entry<String, BalanceSpec.AbilitySpec> entry : spec.abilities.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue().disabled)) {
                if (result.remove(entry.getKey()) != null) {
                    anyDisabled = true;
                    log.info("Реликвия " + idOf(relic) + ": способность '" + entry.getKey()
                            + "' выключена балансом");
                } else {
                    log.warn("Реликвия " + idOf(relic) + ": нечего выключать — нет способности '"
                            + entry.getKey() + "'");
                }
            }
        }
        if (anyDisabled) {
            // Синергии переносим из дефолтного шаблона: builder() сам бы их потерял.
            builder.abilities(result).synergies(vanilla.getSynergies());
        }

        for (Map.Entry<String, BalanceSpec.AbilitySpec> entry : spec.abilities.entrySet()) {
            String abilityId = entry.getKey();
            BalanceSpec.AbilitySpec abilitySpec = entry.getValue();

            if (Boolean.TRUE.equals(abilitySpec.disabled)) {
                continue;
            }

            AbilityTemplate current = vanilla.getAbilities().get(abilityId);
            if (current == null) {
                log.warn("Реликвия " + idOf(relic) + ": нет способности '" + abilityId + "', пропускаю");
                continue;
            }

            AbilityTemplateBuilder abilityBuilder = current.toBuilder();
            if (abilitySpec.requiredPoints != null) {
                abilityBuilder.requiredPoints(abilitySpec.requiredPoints);
            }
            if (abilitySpec.requiredLevel != null) {
                abilityBuilder.requiredLevel(abilitySpec.requiredLevel);
            }
            if (abilitySpec.maxLevel != null) {
                abilityBuilder.initialMaxLevel(abilitySpec.maxLevel);
            }
            if (abilitySpec.stats != null) {
                for (Map.Entry<String, BalanceSpec.StatSpec> statEntry : abilitySpec.stats.entrySet()) {
                    String statId = statEntry.getKey();
                    AbilityStatTemplate currentStat = current.getStats().get(statId);
                    if (currentStat == null) {
                        log.warn("Реликвия " + idOf(relic) + ", способность '" + abilityId
                                + "': нет стата '" + statId + "', пропускаю");
                        continue;
                    }
                    AbilityStatTemplate built = buildStat(currentStat, statEntry.getValue(), statMultiplier);
                    if (built != null) {
                        abilityBuilder.stat(built);
                    }
                }
            }
            builder.ability(abilityBuilder.build());
        }
        return builder.build();
    }

    private AbilityStatTemplate buildStat(AbilityStatTemplate vanilla, BalanceSpec.StatSpec spec,
                                         Double statMultiplier) {
        if (spec == null) {
            return null;
        }
        StatTemplateBuilder builder = vanilla.toBuilder();

        InitialValue initial = vanilla.getInitialValue();
        double initialMin = spec.initialMin != null ? spec.initialMin : initial.getMinValue();
        double initialMax = spec.initialMax != null ? spec.initialMax : initial.getMaxValue();
        builder.initialValue(initialMin, initialMax);

        ThresholdValue threshold = vanilla.getThresholdValue();
        double thresholdMin = spec.thresholdMin != null ? spec.thresholdMin : threshold.getMinValue();
        double thresholdMax = spec.thresholdMax != null ? spec.thresholdMax : threshold.getMaxValue();
        builder.thresholdValue(thresholdMin, thresholdMax);

        TargetValue target = vanilla.getTargetValue();
        double targetValue = target.getTargetValue();
        if (statMultiplier != null) {
            targetValue *= statMultiplier;
        }
        if (spec.targetValue != null) {
            targetValue = spec.targetValue;
        }

        ScalingModel model = target.getScalingModel();
        if (spec.scalingModel != null) {
            ResourceLocation rl = ResourceLocation.parse(spec.scalingModel);
            model = RelicsRegistries.SCALING_MODEL_REGISTRY.get(rl);
            if (model == null) {
                log.warn("Неизвестная модель масштабирования '" + spec.scalingModel
                        + "', оставляю ванильную");
                model = target.getScalingModel();
            }
        }
        builder.targetValue(model, targetValue);
        return builder.build();
    }

    private LevelingTemplate buildLeveling(LevelingTemplate vanilla, BalanceSpec.LevelingSpec spec,
                                          Double costMultiplier) {
        if (spec == null && costMultiplier == null) {
            return vanilla;
        }
        LevelingTemplate.LevelingTemplateBuilder builder = vanilla.toBuilder();

        double initialCost = vanilla.getInitialCost();
        double step = vanilla.getStep();
        if (costMultiplier != null) {
            initialCost *= costMultiplier;
            step *= costMultiplier;
        }
        if (spec != null) {
            if (spec.initialCost != null) {
                initialCost = spec.initialCost;
            }
            if (spec.step != null) {
                step = spec.step;
            }
        }
        builder.initialCost(initialCost).step(step);

        if (spec != null && spec.maxRank != null) {
            builder.maxRank(spec.maxRank);
        }
        return builder.build();
    }

    private LootTemplate buildLoot(LootTemplate vanilla, BalanceSpec.LootSpec spec) {
        if (spec == null || spec.entries == null) {
            return vanilla;
        }
        List<LootEntry> entries = new ArrayList<>();
        for (BalanceSpec.LootEntrySpec entrySpec : spec.entries) {
            // Берём списки из дефолта, чтобы не требовать заполнять все три поля
            // ради изменения одного веса.
            List<String> dimensions = entrySpec.dimensions != null ? entrySpec.dimensions : List.of();
            List<String> biomes = entrySpec.biomes != null ? entrySpec.biomes : List.of();
            List<String> tables = entrySpec.tables != null ? entrySpec.tables : List.of();
            int weight = entrySpec.weight != null ? entrySpec.weight : 1;

            entries.add(LootEntry.builder()
                    .dimensions(dimensions)
                    .biomes(biomes)
                    .tables(tables)
                    .weight(weight)
                    .build());
        }
        return LootTemplate.builder().entries(entries).build();
    }

    private static String idOf(IRelicItem relic) {
        ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(relic.getItem());
        return key == null ? relic.getItem().toString() : key.toString();
    }
}