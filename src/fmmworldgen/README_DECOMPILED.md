# FMM Worldgen — декомпилированный проект (NeoForge 1.21.1, Java 21)

Декомпилятор: Vineflower 1.11.2. Оригинал сохранён в `original/FMMWorldgen-1.0.0.jar`
(и дубль в корне `FMMWorldgen-1.0.0.jar` — его можно удалить после проверки).

## Как собрать / запустить

Требуется JDK 21 (у тебя уже есть Temurin 25 и 17 — подойдёт 21+).

```bat
gradlew build
gradlew runClient
gradlew runServer --args="--nogui"
```

IDE: открой папку как Gradle-проект (IntelliJ IDEA / Eclipse). После импорта выполни `generateModMetadata`.

Свойства мода — в `gradle.properties`:
`mod_id=fmm_worldgen`, `minecraft_version=1.21.1`, `neo_version=21.1.250`.

`neoforge.mods.toml` генерируется из `src/main/templates/META-INF/neoforge.mods.toml`.

## Что делает мод

Островной генератор через 3 кастомных `DensityFunction`, подменяющих ванильные
`overworld/continents` и `overworld/offset` (см. `src/main/resources/data/...`):

- `fmm_worldgen:island_math` → `IslandMathFunction` (continents)
- `fmm_worldgen:island_offset` → `IslandOffsetFunction` (offset / глубина)
- `fmm_worldgen:island_temperature` → `IslandTemperatureFunction` (обёртка над ванильной температурой)

Всё считается детерминированно от координат (x, z), без чтения мира — поэтому одинаково
на клиенте и сервере.

## Карта исходников (`src/main/java/com/fmm/worldgen/`)

| Файл | Роль | Что менять для твиков |
|---|---|---|
| `FMMWorldgen.java` | Точка входа `@Mod`, регистрация 3 density-функций + конфига | Добавление новых функций |
| `IslandHelper.java` | **Ядро**: `sample(x,z)` → ближайший остров, слияние кластеров, `getZoneInfo()`, `isIslandChunk()` | Форма островов, спавн-остров, логика кластеров |
| `FastNoise2D.java` | `hash()` (детерминированный рандом по клетке) + `simplex2D()` | Не трогать без нужды |
| `WorldGenConfig.java` | `ModConfigSpec` + `CACHED_*` кеш (сетки, радиусы, шансы, пляж, лагуна) | Добавлять новые настройки сюда |
| `IslandMathFunction.java` | Профиль continents: пляж −0.16→−0.12, внутри острова → `targetInlandCont`, лагуна −0.28, склон → −0.75 | Высота/континентальность |
| `IslandOffsetFunction.java` | Профиль глубины: пляж −0.25→−0.23, шельф `shelfOffset` (~−0.33), склон → −1.16 | Глубина океана/шельфа |
| `IslandTemperatureFunction.java` | На суше — `islandTemperature`, в лагуне — `waterTemperature`, дальше — ваниль (cap 0.45) | Климат/биомы |
| `IslandArchetype.java` | 14 архетипов (SAVANNA … ICE_SPIKES) + привязка к климату | Новые типы островов |
| `IslandClimate.java` | HOT/WARM/TEMPERATE/COLD/SNOWY | — |
| `IslandTier.java` | SPAWN/SPALL/MEDIUM/LARGE + типичные радиусы | — |
| `IslandData.java`, `IslandZoneInfo.java`, `ZoneRegion.java` | `record`/enum данных: центр, радиусы, температуры; регион INLAND/BEACH/WARM_LAGOON/COLD_LAGOON/DEEP_OCEAN | — |
| `client/IslandClientHandler.java` | Тоггл `F3+Z/J` → `showIslandBorders` | Кейбинды |
| `client/IslandDebugOverlay.java` | Строки F3: остров, зона, дистанции | Отладка |
| `client/IslandZoneWorldRenderer.java` | 3D-стены по границам чанков + маяк в центре, цвет по климату | Рендер |

## Где крутить баланс (быстрые точки)

1. **Частота/размер островов** — `WorldGenConfig`:
   `SMALL_GRID 750 / R 75–87.5 / 40%`, `MEDIUM 1400 / 125–175 / 35%`, `LARGE 2400 / 187.5–212.5 / 30%`.
2. **Климат** — шансы `HOT 0.15 / WARM 0.25 / TEMPERATE 0.30 / COLD 0.15 / SNOWY 0.15`
   + таблицы в `IslandHelper.collectTierCandidates()` (строки ~241–347: `targetInlandCont/Offset`, `roughness`, `islandTemperature/waterTemperature`).
3. **Спавн-остров** — захардкожен в `IslandHelper.collectCandidates()` (~124–149): центр 0,0, R=80, TEMPERATE_FOREST_HILLS.
4. **Пляж/лагуна** — `BEACH_WIDTH 20`, `WARM_OCEAN 30–120` + константы вверху `IslandHelper` (`SLOPE_WIDTH 40` и т.д.).
5. **JSON ворлдгена** — `data/minecraft/worldgen/density_function/overworld*/continents.json`, `offset.json`, `noise_settings/overworld.json`.

## Известные особенности декомпиляции

- Имена локальных переменных синтетические (`targetInlandCont`, `tSnowy` и т.п. восстановлены декомпилятором — смысл верный, но оригинальные имена могли отличаться).
- `ru_ru.json` / `en_us.json` — валидный UTF-8, `§` закодирован как `C2 A7` (в консоли Win-1251 выглядит как `�` — это нормально).
- Оригинальные комментарии/ассеты `pack.mcmeta` отсутствуют в jar — их и не было.

## Следующий шаг для улучшений

- Добавить `pack.mcmeta` если захочешь описание ресурспака.
- Вынести захардкоженные числа спавна/шельфа (`-0.33`, `-1.16`, `-0.75`…) в `WorldGenConfig`.
- Разбить гигантский `collectTierCandidates()` на `IslandGenerator` + `ClimateTable`.
