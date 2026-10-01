# MeowRelics

Аддон для мода [Relics](https://www.curseforge.com/minecraft/mc-mods/relics-mod), который
позволяет переписать его баланс под себя — без правки ванильного `relics.yaml`
и без включения экспериментального `enabledExtendedConfigs`.

## Что он умеет

Управляет всеми числовыми параметрами реликвии, которые в моде разложены по
`RelicConfigData`:

| Что | Поле в JSON |
|---|---|
| Цена прокачки реликвии | `leveling.initialCost`, `leveling.step` |
| Максимальный ранг (число ячеек исследования) | `leveling.maxRank` |
| Требования к открытию способности | `abilities.<id>.requiredLevel` |
| Стоимость в очках левела | `abilities.<id>.requiredPoints` |
| Потолок уровня способности | `abilities.<id>.maxLevel` |
| Диапазон базового значения стата | `stats.<id>.initialMin` / `initialMax` |
| Жёсткий потолок стата | `stats.<id>.thresholdMin` / `thresholdMax` |
| Максимальное значение стата | `stats.<id>.targetValue` |
| Модель масштабирования | `stats.<id>.scalingModel` |
| Условия и вес появления в луте | `loot.entries[]` |

Плюс два множителя на весь пак сразу:

- `levelingCostMultiplier` — цена прокачки всех реликвий;
- `statTargetMultiplier` — максимальные значения всех статов.

## Установка

1. Закинь `meowrelics.jar` в `mods/`.
2. Запусти сервер — рядом с конфигами появится
   `config/meowrelics/balance.json` с примером.
3. Правь файл, затем `/meowrelics reload` (перезагрузка без рестарта).

Требуется `relics` 0.12.8+ и NeoForge 21.1+.

## С чего начать настройку

Не знаешь, какие числа вообще есть? Выгрузи текущие значения всех 20 реликвий:

```
/meowrelics dump
```

Появится `config/meowrelics/current_balance.json` — все способности, статы,
модели масштабирования, условия лута. Копируешь нужный блок в `balance.json`,
правишь числа, жмёшь `/meowrelics reload`.

Так делать удобнее, чем угадывать id способностей и статов вслепую.

## Формат конфига

Всё необязательно. Что не указано — остаётся ванильным.

```jsonc
{
  "levelingCostMultiplier": 2.0,   // прокачка в 2 раза дороже
  "statTargetMultiplier": 1.25,    // статы на 25% сильнее

  "relics": {
    "relics:hunting_belt": {
      "leveling": {
        "initialCost": 100.0,       // стоимость уровня 1
        "step": 100.0,              // прибавка за каждый следующий уровень
        "maxRank": 5                // было 5
      },

      "abilities": {
        "slots": {                  // id способности
          "requiredPoints": 2,      // очков левела на уровень
          "requiredLevel": 0,       // открывается с уровня реликвии N
          "maxLevel": 5,
          "stats": {
            "amount": {             // id стата
              "initialMin": 1.0,    // диапазон при создании
              "initialMax": 2.0,
              "targetValue": 12.0,  // на максимуме
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
            "tables": ["[\\w]+:chests\\/[\\w_\\/]*(village|pillage)[\\w_\\/]*"],
            "weight": 500           // вес появления
          }
        ]
      }
    }
  }
}
```

### Заметки

- `loot.entries` — **полная замена** списка, а не дополнение. Опустил `loot` — осталось ванильное.
- В `tables` пишется регулярка по id лут-таблицы. В JSON обратные слэши удваиваются: `\\w`, `\\/`.
- Модель масштабирования берётся из `relics:scaling_models`: `additive`, `exponential`,
  `exponential_decay`, `exponential_saturation`, `logarithmic`, `multiplicative_base`, `radical`.
- `initialMin`/`initialMax` влияют только на **новые** реликвии — при создании
  качество выбирается случайно из этого диапазона и сохраняется в самом предмете.
- `targetValue`, `threshold*`, стоимость и потолки способностей действуют и на
  уже существующие реликвии: значение считается из шаблона в реальном времени.

## Команды

Все требуют права оператора (уровень 2).

- `/meowrelics reload` — перечитать конфиг.
- `/meowrelics dump` — выгрузить текущие значения всех реликвий в
  `config/meowrelics/current_balance.json`.
- `/meowrelics status` — показать путь к файлу баланса.

## Как это работает

Мод на старте сервера обходит реестр предметов, находит все `IRelicItem` и
пересобирает их `RelicTemplate` через `toBuilder()`. Оригинальные шаблоны
кэшируются при первом проходе, поэтому `/meowrelics reload` всегда считает
значения от ванильного состояния, а не от предыдущей правки — повторные
применения не накапливаются и не ломают числа (проверено: две загрузки подряд
дают одинаковый результат).

После изменения лута дополнительно вызывается `RelicLootModifier.processRelicCache()`
— без этого мод держит собственный кэш условий появления, и новые веса не применились бы.

## Сборка

```bat
gradlew.bat build
```

Готовый jar: `build/libs/meowrelics.jar`.

Компилируется против `libs/relics-1.21.1-0.12.8.jar` — исходников мода Relics
в открытом доступе нет, только бинарник.