package me.nanorasmus.meowrelics.balance;

import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilitiesTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.AbilityTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.AbilityStatTemplate;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.misc.InitialValue;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.misc.TargetValue;
import it.hurts.sskirillss.relics.api.relics.abilities.stats.misc.ThresholdValue;
import it.hurts.sskirillss.relics.init.RelicsRegistries;
import it.hurts.sskirillss.relics.items.relics.base.data.leveling.LevelingTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootEntry;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Выгружает текущие (уже переопределённые) значения всех реликвий в JSON
 * того же формата, что и {@code balance.json}.
 *
 * <p>Смысл — дать точку отсчёта: сначала выгрузить ваниль, потом править
 * нужные числа в полученном файле и скормить его обратно через reload.
 */
public final class BalanceDumper {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Пишет снимок текущего баланса. */
    public static void dump(Path file, org.slf4j.Logger log) {
        BalanceSpec out = new BalanceSpec();
        out.levelingCostMultiplier = 1.0;
        out.statTargetMultiplier = 1.0;

        // TreeMap — чтобы в файле реликвии шли в алфавитном порядке, и diff был читаемым.
        Map<String, BalanceSpec.RelicSpec> relics = new TreeMap<>();

        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof IRelicItem relic)) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) {
                continue;
            }
            relics.put(id.toString(), describe(relic.getDefaultRelicTemplate()));
        }
        out.relics = relics;

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(out), StandardCharsets.UTF_8);
            log.info("Выгружен текущий баланс {} реликвий в {}", relics.size(), file);
        } catch (IOException e) {
            log.error("Не удалось выгрузить баланс в {}", file, e);
        }
    }

    private static BalanceSpec.RelicSpec describe(RelicTemplate template) {
        BalanceSpec.RelicSpec spec = new BalanceSpec.RelicSpec();

        LevelingTemplate leveling = template.getLeveling();
        BalanceSpec.LevelingSpec levelingSpec = new BalanceSpec.LevelingSpec();
        levelingSpec.initialCost = leveling.getInitialCost();
        levelingSpec.step = leveling.getStep();
        levelingSpec.maxRank = leveling.getMaxRank();
        spec.leveling = levelingSpec;

        AbilitiesTemplate abilities = template.getAbilities();
        for (Map.Entry<String, AbilityTemplate> abilityEntry : abilities.getAbilities().entrySet()) {
            AbilityTemplate ability = abilityEntry.getValue();
            BalanceSpec.AbilitySpec abilitySpec = new BalanceSpec.AbilitySpec();
            abilitySpec.requiredPoints = ability.getRequiredPoints();
            abilitySpec.requiredLevel = ability.getRequiredLevel();
            abilitySpec.maxLevel = ability.getInitialMaxLevel();

            for (Map.Entry<String, AbilityStatTemplate> statEntry : ability.getStats().entrySet()) {
                AbilityStatTemplate stat = statEntry.getValue();
                BalanceSpec.StatSpec statSpec = new BalanceSpec.StatSpec();

                InitialValue initial = stat.getInitialValue();
                statSpec.initialMin = initial.getMinValue();
                statSpec.initialMax = initial.getMaxValue();

                ThresholdValue threshold = stat.getThresholdValue();
                statSpec.thresholdMin = threshold.getMinValue();
                statSpec.thresholdMax = threshold.getMaxValue();

                TargetValue target = stat.getTargetValue();
                statSpec.targetValue = target.getTargetValue();
                ResourceLocation modelKey =
                        RelicsRegistries.SCALING_MODEL_REGISTRY.getKey(target.getScalingModel());
                if (modelKey != null) {
                    statSpec.scalingModel = modelKey.toString();
                }

                abilitySpec.stats.put(statEntry.getKey(), statSpec);
            }
            spec.abilities.put(abilityEntry.getKey(), abilitySpec);
        }

        LootTemplate loot = template.getLoot();
        if (loot != null && !loot.getEntries().isEmpty()) {
            BalanceSpec.LootSpec lootSpec = new BalanceSpec.LootSpec();
            List<BalanceSpec.LootEntrySpec> entries = new ArrayList<>();
            for (LootEntry entry : loot.getEntries()) {
                BalanceSpec.LootEntrySpec entrySpec = new BalanceSpec.LootEntrySpec();
                entrySpec.dimensions = entry.getDimensions();
                entrySpec.biomes = entry.getBiomes();
                entrySpec.tables = entry.getTables();
                entrySpec.weight = entry.getWeight();
                entries.add(entrySpec);
            }
            lootSpec.entries = entries;
            spec.loot = lootSpec;
        }
        return spec;
    }
}