package me.nanorasmus.meowrelics.balance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import it.hurts.sskirillss.relics.api.relics.RelicTemplate;
import it.hurts.sskirillss.relics.items.relics.base.data.loot.LootTemplate;
import it.hurts.sskirillss.relics.level.RelicLootModifier;
import me.nanorasmus.meowrelics.relic.RelicRandomizer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Загружает {@code config/meowrelics/balance.json} и применяет его ко всем
 * реликвиям из реестра.
 *
 * <p>Ключевой момент — перезагрузка. Чтобы не накапливать правки, перед каждым
 * применением мы берём <em>исходный</em> шаблон реликвии (тот, что мод создал
 * при регистрации) и строим новый уже поверх него. Оригиналы кэшируются в
 * {@link #vanillaTemplates} один раз и далее не меняются.
 */
public final class BalanceLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;
    private final Logger log;

    /** Оригинальные шаблоны реликвий — точка отсчёта для любой перезагрузки. */
    private final Map<IRelicItem, RelicTemplate> vanillaTemplates = new java.util.HashMap<>();

    public BalanceLoader(Path configDir, Logger log) {
        this.configPath = configDir.resolve("meowrelics").resolve("balance.json");
        this.log = log;
    }

    public Path getConfigPath() {
        return configPath;
    }

    /** Создаёт конфиг с нуля, если его ещё нет. */
    public void generateDefault() {
        try {
            if (Files.exists(configPath)) {
                return;
            }
            Files.createDirectories(configPath.getParent());
            Files.writeString(configPath, DEFAULT_TEMPLATE, StandardCharsets.UTF_8);
            log.info("Создан шаблон баланса: {}", configPath);
        } catch (IOException e) {
            log.error("Не удалось создать {}", configPath, e);
        }
    }

    /**
     * Перечитывает конфиг и применяет его.
     *
     * @return количество реликвий, чей шаблон реально изменился
     */
    public int reload() {
        BalanceSpec spec;
        if (!Files.exists(configPath)) {
            log.warn("Файл баланса не найден: {}. Пропускаю.", configPath);
            return 0;
        }
        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            spec = GSON.fromJson(json, BalanceSpec.class);
        } catch (IOException | JsonParseException e) {
            log.error("Ошибка чтения {}. Использую прошлые значения.", configPath, e);
            return 0;
        }
        if (spec == null) {
            log.warn("Пустой файл баланса, оставляю текущие значения.");
            return 0;
        }
        return apply(spec);
    }

    private int apply(BalanceSpec spec) {
        RelicBalancer balancer = new RelicBalancer(new RelicBalancer.Logger() {
            @Override
            public void warn(String msg) {
                log.warn("[баланс] {}", msg);
            }

            @Override
            public void info(String msg) {
                log.info("[баланс] {}", msg);
            }
        });

        // Список исключений — до правки шаблонов: ротация мешочков читает его
        // при каждом вскрытии, так что порядок тут неважен, но логичнее сначала.
        RelicRandomizer.setExcluded(spec.excludedFromBags);

        // Кэшируем оригиналы при первом проходе. constructDefaultRelicTemplate()
        // каждый раз создаёт новый объект из кода мода — это и есть «ваниль».
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof IRelicItem relic) {
                vanillaTemplates.computeIfAbsent(relic, r -> r.constructDefaultRelicTemplate());
            }
        }

        int changed = 0;
        for (Map.Entry<IRelicItem, RelicTemplate> entry : vanillaTemplates.entrySet()) {
            IRelicItem relic = entry.getKey();
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(relic.getItem());
            if (id == null) {
                continue;
            }
            BalanceSpec.RelicSpec relicSpec = spec.relics.get(id.toString());
            RelicTemplate rebuilt = balancer.apply(relic, entry.getValue(), relicSpec,
                    spec.levelingCostMultiplier, spec.statTargetMultiplier);
            if (rebuilt == null) {
                continue;
            }
            relic.setDefaultRelicTemplate(rebuilt);
            // Кэш лута живёт отдельно от шаблона — без этого новые веса не применятся.
            RelicLootModifier.processRelicCache(relic);
            changed++;
        }
        log.info("Баланс реликвий применён: {} шт.", changed);
        return changed;
    }

    /**
     * Убирает реликвии из сундуков: очищает лут-шаблоны у всех реликвий.
     *
     * <p>Сделано через шаблоны, а не отпиской от чужого ивента, потому что
     * {@code RelicLootModifier} читает список записей именно из шаблона. Пустой
     * список — и реликвия в лут больше не попадает. Сами мешочки кладёт наш
     * {@code BagLootModifier}.
     *
     * <p>Вызывается после {@link #apply}, иначе {@code /meowrelics reload} вернёт
     * реликвии в сундуки: баланс пересобирает шаблон из «ванильного», который
     * был закеширован до очистки.
     */
    public int stripLootTables() {
        int stripped = 0;
        for (Map.Entry<IRelicItem, RelicTemplate> entry : vanillaTemplates.entrySet()) {
            IRelicItem relic = entry.getKey();
            RelicTemplate current = relic.getDefaultRelicTemplate();
            if (current.getLoot().getEntries().isEmpty()) {
                continue;
            }
            relic.setDefaultRelicTemplate(current.toBuilder()
                    .loot(LootTemplate.builder().build())
                    .build());
            RelicLootModifier.processRelicCache(relic);
            stripped++;
        }
        log.info("Реликвии убраны из лута: {} шт. Теперь их выдают мешочки.", stripped);
        return stripped;
    }

    private static final String DEFAULT_TEMPLATE = """
            {
              "_комментарий": [
                "MeowRelics — аддон для переписывания баланса реликвий из мода Relics.",
                "Всё необязательно: перечисли только то, что меняешь. Не указанное = ваниль.",
                "",
                "Как настроить:",
                "  1. /meowrelics dump — выгрузить все текущие значения в current_balance.json.",
                "  2. Скопировать нужные блоки оттуда сюда и поправить числа.",
                "  3. /meowrelics reload — применить без перезапуска сервера.",
                "",
                "Множители levelingCostMultiplier и statTargetMultiplier действуют сразу на все реликвии.",
                "targetValue — значение стата на максимальном качестве и уровне способности.",
                "initialMin/initialMax — диапазон при создании реликвии (старые реликвии не меняются).",
                "В tables пишется регулярка по id лут-таблицы; в JSON слэши удваиваются: \\\\w, \\\\/."
              ],

              "levelingCostMultiplier": 1.0,
              "statTargetMultiplier": 1.0,

              "relics": {
                "relics:hunting_belt": {
                  "leveling": {
                    "initialCost": 100.0,
                    "step": 100.0,
                    "maxRank": 5
                  },
                  "abilities": {
                    "slots": {
                      "requiredPoints": 2,
                      "maxLevel": 5,
                      "stats": {
                        "amount": {
                          "initialMin": 1.0,
                          "initialMax": 2.0,
                          "targetValue": 12.0,
                          "scalingModel": "relics:additive"
                        }
                      }
                    }
                  },
                  "loot": {
                    "entries": [
                      {
                        "dimensions": [".*"],
                        "biomes": [".*"],
                        "tables": ["[\\\\w]+:chests\\\\/[\\\\w_\\\\/]*(village|pillage)[\\\\w_\\\\/]*"],
                        "weight": 500
                      }
                    ]
                  }
                }
              }
            }
            """;
}