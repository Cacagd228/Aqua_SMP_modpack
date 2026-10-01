# Changelog

## v1.1.7-release

- **Отключены структуры из генерации** через `structure_set`-оверрайды в kubejs-датапаке
  (`kubejs/data/*/worldgen/structure_set/`). Прежние оверрайды были нерабочими: имена файлов
  не совпадали с реальными наборами, поэтому настоящие наборы продолжали генерироваться.
  - `minecraft`: `strongholds` (`minecraft:stronghold`), `ocean_monuments` (`minecraft:monument`),
    `trial_chambers`.
  - `scguns`: `aboveground_medium` (`asgharian_citadel`, `asgharian_tower`, `osgood_lab`,
    `osgood_lab_outpost`), `cog_chambers`, `trench`.
  - `create_rns`: `deposits` (15 оверворлд-депозитов) + `nether_deposits` (4 незер-депозита).
    Прежние 19 файлов `configured_feature/deposit_*.json` с `minecraft:no_op` удалены —
    депозиты у create_rns являются структурами, а не configured features, поэтому `no_op` на них
    ничего не делал.
- **Возвращён `Structure Pool API` 1.2.1** (CurseForge-пин, `structure_pool_api-neoforge-1.2.1+1.21.1.jar`).
  Без него часть модов не запускается; пин был потерян при сборке v1.1.6.
- **В сборку вошёл `meowrelics` 1.0.0** (исходники в `src/meowrelics`, раньше был вне репозитория):
  генератор Relics-артефактов и сумки (`RelicRandomizer`, `ArtifactRandomizer`, `RelicBagItem`),
  балансировщик конфига Relics (`RelicBalancer`, `BalanceLoader`), loot-модификаторы.
  Пин: `mods/meowrelics.pw.toml`. Каталоги сборки/запуска закрыты `src/meowrelics/.gitignore`,
  compile-only `relics-1.21.1-0.12.8.jar` в репозиторий не идёт (см. `src/meowrelics/libs/README.txt`).
- Пересобраны из `src/` отставшие самописы:
  - `colonycard` 1.0.0 — добавлен Trade Terminal (`TradeTerminalBlock`, `TradeTerminalScreen`,
    `TradeTerminalCommands`, сеть `trade/`).
  - `lineage_core` 2.1.0 — `FrozenWaters`.
  - `meowhex` 1.4.0 — пересборка (Hourglass, LightningRod, StellarTune).
- Инструменты: `tools/refresh-index-clean.py` — пересборка `index.toml` в чистом дереве.
  packwiz не читает `.gitignore`, поэтому `packwiz refresh` в рабочем дереве с gradle-выводом
  затягивал в индекс ~9 000 служебных файлов (`src/*/build`, `run`, `.gradle`). На CI этого
  не происходило (свежий checkout = только tracked-файлы), из-за чего расхождение не было заметно.

## v1.1.6-release

- Фикс Create Aeronautics переписан в отдельный мод (`aerofix` 1.0.0, исходники в `src/aerofix`).
  Раньше в паке лежал `create-aeronautics-bundled-1.21.1-1.3.2-FIXED.jar` — полная
  пересборка оригинала с вшитыми правками, из-за чего апдейты Create Aeronautics были
  заблокированы. Теперь стоит **стоковый** bundle 1.3.2 (с Modrinth, обновляется штатно),
  а `aerofix` навешивает те же правки миксинами в рантайме, не трогая оригинал.
- Звуки комментаторов (QoP + Meepo) и `silence` вынесены из мода `meowhex` в отдельный
  ресурс-пак `AquaSMP-Sounds` (собирается скриптом `tools/build-sounds-pack.py`, раздаётся
  игрокам отдельно). Аудио намеренно не лежит в репозитории.
- Убраны моды FTB: `ftb-library`, `ftb-quests`, `ftb-teams`, `FTBQuestsOptimizer`,
  `UIQuest`. Вместе с ними выпилены их конфиги и датапак `data/ftbquests`.
- Убран `waterwheelbearing` 3.0.0.
- Убраны `rogues-and-warriors` и `Structure Pool API` — последний тянул только `rogues`,
  после его снятия зависимых не осталось.
- Убраны моды, отключённые при тестах в инстансе: `Axiom`, `Create: Bits n' Bobs`,
  `Create: Cyber Goggles`, `Create: Goggles`, `sablexaeromaps`, `Tree Physics`,
  `Xaero's Minimap`, `Xaero's World Map`, `Xaero's Maps: Multiplayer+`.
- Доделана система клеймов в `fmm_teams` 0.1.0: `TerritoryTracker` (захват островов) +
  `AdminCommands`, остров спавна занимать нельзя.
- Добавлены `Balm` 21.0.66 и `TrashSlot` 21.1.11 (+ их конфиги).
- `meowhex` пересобран: система assembly (5 рун поверх Create Sequenced Assembly),
  40+ рун из MoreIotas (строки / типы / предметы), `meowhex-assembly.toml`,
  `ItemAmethystFishingRod`, `OpInfuseAether`. Из мода убраны lesser_battery- и
  extended-посохи (модели, текстуры, рецепты) — остались только базовые посохи.
- Конфиги подтянуты из тестового инстанса: гарантированная жила Create: Rock & Stone на
  каждом острове (`fmm_worldgen-common.toml`), `explosionBlockDamageMultiplier` /
  `knockbackNonPlayerMultiplier` (`meowhex-server.toml`), `motorMinimumLoad`
  (`powergrid-server.toml`).
- Тулзы: `tools/build-sounds-pack.py`, `tools/check_patterns.py` (валидация сигнатур рун
  без запуска игры), `tools/check_sigs.py`, `tools/check_upstream.py`,
  `tools/clean-index.py`.
- `index.toml` почищен от ~9 000 записей с build-артефактами `src/*/build`,
  `src/*/run`, `.gradle` и `.mcprobe2`. `packwiz` индексирует каталог напрямую и
  `.gitignore` не читает, поэтому после локальной сборки модов индекс раздувался
  (2.5 МБ), а `packwiz mr export` втягивал артефакты в `overrides` — локальный
  `.mrpack` раздувался с 71 МБ до 1.5 ГБ.

## v1.1.5-pre-release

- `meowhex` 1.3.2 → 1.4.0 (сборка из исходников `src/meowhex`):
  - Фикс критической уязвимости Hex Cast: круги больше не продолжают выполнение после мишапа (`mishapOccurred` останавливает цепь), провал оплаты маны в круге превращается в обычный мишап круга вместо тихого пропуска заклинания.
  - Сосуды маны: новый блок `mana_vessel` — мана для кругов теперь берётся из сосудов (`OpChargeVessel`, `currentVessel`), импетус больше не хранит ману; убран `lore_fragment` и его лут/ачивки.
  - Баланс Hex Cast: правки стоимости маны, `OpTransferToDepot`, скrying-линза и HUD маны.
  - Фикс крыльев Ириды: ванильный `OpWingsOfIrida` больше не регистрируется в `HexJSInitializer` (крылья теперь только через свитки/прогрессию).
  - Увеличение дальности каста с короной: `DiademAmbit` — заряженная аметистовая диадема в слоте Curios удваивает радиус каста (32 → 64 блока).
  - Руна медсестры: `nurses_purification` (`aqwawqa`) — аналог `hexal:health`, лечит сущность за ману.
  - Новое заклинание `morph_hex` + `MorphHexEffect`/`MorphHexChickenMixin`.
- `hexsable` 1.0.1 → 1.1.0 (сборка из исходников `src/hexsable`):
  - Улучшенная интеграция с Sable Hex: свитки Sable (`SableScrollItems`, лут-модификатор, тиры сундуков), гейт рун за свитками (`SableScrollGateMixin`, `hexsable.mixins.json`), миксин креативного анлокера.
- `lineage_core` 2.0.1 → 2.1.0 (сборка из исходников `src/linage_core`):
  - Раса сосуда: `aether_mind` (игровая) и `arch_aether` (архетип вознесения: `EtherealVeil`, `EtherealFlesh`, `UnseenPresence`, `ManaExhaustion`).
  - Выдача рас админами: команды `/lineage set <игрок> <раса>` и `/lineage ascend <игрок>`.
  - Фикс арх-рас: `SeasonedPlayerWatcher` (`LivingChangeTargetEvent` — мобы теряют невидимую цель), `VeilRender` (аэфирный оттенок вуали), `ChronicleNetwork`.
- Пак: RPG-серия (`archers`, `paladins-and-priests`, `rogues-and-warriors`, `armory/arsenal-rpg-series`, `relics-rpg`, `forcemaster-rpg-class`, `spell-engine/power`, `ranged-weapon-api`, `armor-model-api`, `more-rpg-library`, `archers-expansion`), `Structure Pool API` 1.2.1 (NeoForge, CurseForge-пин), `Design-n-Decor`, `colorwheel`, reinforced backpack, estrogen-твики.

## v1.1.4-pre-release

- `colonycard` 1.0.0 пересобран: stage-система короны (StageBlock, указы-грамоты `/colonycard gramota`, донаты/таски, сеть, экраны, конфиг, Discord-вебхук).
- `lineage_core` 2.0.1 пересобран: русские описания рас/трейтов переписаны на точные механики.
- `meowhex` 1.3.2 и `meowaddons` 1.0.1 пересобраны из исходников.
- Добавлен `PatPat` 1.3.1 (Modrinth).
- KubeJS: замок прогрессии `00_tier_lockdown.js` + века `tier_andesite/iron/copper.js` + `scripts/01–08`, крафт корпусов переведён на `create:item_application` (JEI + деплоер).

## v1.1.3-beta.3

- `meowhex` 1.3.1 -> 1.3.2: фикс тегов (`craft/battery` убран из per-world great-заклинаний), чистка книги (`explode/fire` убран из basic).
- `meowaddons` 1.0.0 -> 1.0.1: фикс JEI для тировых сборок — шаги pressing/cutting/deploying показывают иконки тировых механизмов (`TieredAssemblySteps`).

## v1.1.3-beta-2

- `meowhex` 1.3.0 -> 1.3.1: оптимизация `MixinParsePatternFormatting` (рендер текста): fast-path по первому символу вместо substring + regex на каждый символ, `Matcher.region` + `lookingAt` без копий строк. Фикс просадки FPS на HUD/чате/тултипах.
- Удален `Observable` 5.4.4: его оверлей съедал ~30% времени кадра (профайлер для игроков не нужен).
- `DistantHorizons` по умолчанию выключен: `enableDistantGeneration=false`, `enableServerGeneration=false`, `enableAutoUpdater=false`, `enableCloudRendering=false` (фоновая генерация LOD грузила CPU даже в простое).

## v1.1.3-beta

- `meowhex` 1.2.1 → 1.3.0: крупная балансная правка hex casting, новая связка мана-пейринга, фикс RAMPAGE (порядок вершины holy_shit/rampage у Meepo и QoP + счётчик X2, X3...), новые звуки анонсера.
- `hexsable` 1.0.1 (новый в паке): мост Hex Casting ↔ Sable, балансные правки.
- `colonycard` 1.0.0 (новый в паке): паспорт колониста (профиль, черты, лояльность, награды).
- `FMMWorldgen` 1.0.0 → 1.1.0: спавн жил на островах (сборка под NeoForge 21.1.248, как весь пак).
- `meowaddons` 1.0.0 пересобран: фикс названий (тирные прессы/дробилки/жернова/миксеры/пилы/активаторы, рецепты, рамки).

## v1.1.2-beta.3

- Добавлен `Better Combat` 2.4.0+1.21.1 (NeoForge).
- `meowhex` 1.2.0 → 1.2.1: балансные правки стоимости маны заклинаний.

## v1.1.2-beta.2

- `meowhex` обновлён.
- meepo RAMPAGE.

## v1.1.2-beta

- `meowhex`: новые версии + все обновлённые моды.
- Небольшие балансные правки hexcasting.
- Починка клиента.
- Sun strike.

## v1.1.1-beta.4

- `meowaddons` 1.0.0 пересобран из исходников (`src/meowaddons`):
  - Вырезан спавн структур/мобов из мода (глушится kubejs-датапаком:
    `kubejs/data` no_op-оверрайды + `kubejs/server_scripts/disable_mobs.js`).
  - Новое зачарование лука «Мульти выстрел» (`meowaddons:triple_shot`): залп из 3 стрел.
  - Новое зачарование лука «Авто выстрел» (`meowaddons:auto_shot`): автоспуск тетивы на полном натяге.

## v1.1.1-beta.3

- Fix: `biolith` side=both (теперь на клиенте и сервере)
- Добавлен `fmm_worldgen` в исходники (pinned jar)
- Добавлен `fmm_teams` в исходники (pinned jar)
- Удалены kubejs скрипты HexJS (haste_cast, haste_register, haste_scroll, ovid_test)

## v1.1.1-beta.2

- Fix: `biolith` side=both (теперь на клиенте и сервере)
- Добавлены минимальные файлы FTB Quests (тема, глава квестов) для предотвращения краша
- NeoForge installer включён в server pack
- Force-add PonderJS в server pack
- Exclude UIQuest из server pack

## v1.1.1-beta.1

- `lineage_core` 2.0.1 (сборка из исходников `src/linage_core`): пофикшена раса Лудоман.
- Убран `create_parachute` (jar, конфиг, записи packwiz).
- `powergrid` 0.6.1 → 0.6.2 (PR #1 от MrPe4henika; дочинен `index.toml`).
- `meowaddons` 1.0.0 пересобран из исходников: тирные mixer/saw/millstone/crusher/deployer/fan (T1–T6).
- `create_avionics` 0.5.2 → 0.6.0.

## v1.1.0-beta.1

- `lineage_core`: починенный билд 2.0.0.
- Добавлен `fmm_teams` 0.1.0 (бета): команды в стиле Panoptic.
- Сборка в статусе беты.

## v1.0.0 — миграция на packwiz

- Переезд со старой репы `Factory_must_meowing` (ветка `master`, ~958 МБ в `.git`).
- Формат: **packwiz** — в гите только манифест, конфиги, KubeJS; моды качаются
  с Modrinth, готовые сборки — в Releases (`.mrpack`).
- 196 модов с Modrinth с запиненными версиями (проверены хеши sha512
  против рабочей сборки; MC 1.21.1, NeoForge 21.1.248).
- 15 запиненных jar'ов в `mods/`: 5 кастомных + 10 отсутствующих на Modrinth.
- `config/`: 345 файлов, вычищен рантайм (`.bak`, iris/sodium-fingerprint,
  JEI-выборки, xaero, миникарты, голосовалка преференсов).
- `kubejs/`: 22 файла без изменений (абсолютных путей нет).
- Выкинуто: `jeiexport` (дев-тулза), дубль `moonlight-3.5.2`,
  `temp.class`/`temp_eh.class`, машинный `instance.cfg`.

### Известно / TODO

- 8 сторонних пинов без автообновлений (CurseForge-only): FTB-трио,
  `framework`, `harderdiesel`, `waterwheelbearing`, `UIQuest`,
  `create-aeronautics-FIXED`. Нужен `CF_API_KEY` + `packwiz cf` либо ручной bump.
- Кастомные моды кладутся в пак вручную; следующий шаг —
  сборка их в CI из исходников.
- `fmm_teams` (0.1.0) в пак не входит, ждёт готовности.
