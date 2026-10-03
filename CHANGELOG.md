# Changelog

## v1.1.8-release

- **Новые моды (2), оба запинены по версии:**
  - **CC: Sable 1.3.4** (`OPNBxiZD` / `j0UWQoMG`) — аддон CC: Tweaked для
    Sable-задней части Create: Simulated. Оба его обязательных зависимости
    (`CC: Tweaked` и `Sable`) уже были в сборке, ничего доустанавливать не надо.
  - **Controlling 19.0.5** (`xv94TkTM` / `FaNppCJJ`) — подсказки по клавишам в
    стиле ванильного GUI. Запрошен CurseForge-файл `6368976`; он побайтово равен
    файлу на Modrinth (одинаковый sha512), поэтому запинен через Modrinth, чтобы
    работал `packwiz update --all`. Скачан с CurseForge и сверен по хешу.
- **`aerofix` пересобран:** удалён миксин `SwivelBearingBlockEntityMixin` и
  опция конфига `fixSwivelBearing`. Фикс поворотного подшипника апстримом
  учтён, гвард больше не нужен.
- **`lineage_core` 2.1.1 пересобран:** в `FrozenWaters` холод теперь определяется
  по фрагменту пути биома (`contains`, а не точное равенство), из кода убраны
  множество `CHILLED` и одноразовое сообщение `cold_touch` (строки переведены
  из `en_us`/`ru_ru`).
- **KubeJS:** добавлены тир-скрипты `tier_brass.js`, `tier_dark.js`,
  `tier_chromatic.js` (T4/T5/T6) — их ждал `00_tier_lockdown.js`; удалён мусорный
  `startup_scripts/corps_items.js`, регистрировавший несуществующие предметы
  `corps_*` поверх блоков `meowaddons:frame_*`; вычищены дубли рецептов в
  `mcp_generated/`. Черновики Recipe Builder (`main(1).js`, `*.js.bak`) ушли в
  `.gitignore`.
- **`tools/verify_release.py`** больше не хардкодит версию релиза: берёт её из
  `AQUA_VERSION` (по умолчанию — текущая) и ищет артефакты по ней же, иначе
  проверка всегда ругалась на прошлую сборку.

### `fmm_teams`: графическая админ-панель групп

  - **`fmm_teams`: графическая админ-панель.** Открывается кнопкой «Админ» в обычном
    меню группы (`G` / `/fteams`), доступна только операторам. Отдельной клавиши и
    команды нет: право оператора приходит с сервера в составе снапшота группы, и
    кнопка не рисуется вообще, если прав нет. Неоператору сервер отдаёт пустой
    отказ — панель никогда не доверяет клиенту, каждое действие перепроверяется
    на сервере.
    - Панель — подменю: `Esc` и «Закрыть» возвращают в меню группы. Блюра за миром
      нет (виджеты рисуются вручную, без `super.render`, который зовёт
      `processBlurEffect`).
    - Слева список всех групп с поиском по названию/овнеру; вкладка «Острова»
      показывает все занятые острова сервера с владельцем.
    - Справа карточка выбранной группы: очки, бонус, потрачено, свободно, состав,
      дата создания и остров под админом.
    - Действия: **кик участника** (встроенная кнопка `⨯` в строке; если кикнули
      овнера — права автоматически переходят к следующему участнику, а группа из
      одного человека удаляется), **удалить группу** (с подтверждением в два клика),
      **переименовать**, **добавить игрока** (перетаскивает из другой группы и
      ребалансирует её острова), **повысить/понизить/передать права**.
    - Очки: `+1 / -1 / +10 / -10` и «Задать» из поля ввода (бонус можно ставить в
      ноль, но не в минус).
    - Острова: **занять остров под админом** для выбранной группы — обычным
      списанием очков либо кнопкой «без очков» (группа может уйти в минус, нужно
      для починки экономики вручную), **забрать остров** у группы кнопкой `⨯` в
      строке — как из списка группы, так и из общего списка островов.
    - Протокол поднят `2` → `4` (админ-payload'ы + флаг оператора в снапшоте
      группы), поэтому старый клиент и новый сервер честно не соединятся — всем
      игрокам нужно обновить мод.
  - Новые админ-методы в `TeamManager`: `adminKickHard`, `adminDisband`,
    `adminAddMember`, `adminSetRole`, `adminTransfer`, `adminRename`,
    `adminClaimIsland(..., free)`. Команды `/fmm admin` продолжают работать как
    раньше и ничего не потеряли.
  - Новый джарник `mods/fmm_teams-0.1.0.jar` (пин и хеши в `index.toml` /
    `fmm_teams-0-1-0.pw.toml` обновлены).

## v1.1.7-release.2

  - **Добавлен `Create Aeronautics: burner fuel` 1.0.2** (`createburnerfuel-1.0.2.jar`).
    Аддон к Create Aeronautics: горелки теперь требуют топливо. Запинен на Modrinth
    (`OizKjGJm` / `e0B0nsFU`) — на CurseForge он тоже есть, но версия там ровно та же,
    так что обходной путь через raw-ссылку на репозиторий не понадобился.
  - `armory-rpg-series` и `create-armored-constructs` **оставлены на текущих версиях**:
    - Armory 1.5.2+1.21.1 — последняя сборка под 1.21.1/NeoForge (1.5.3 вышел только
      под 1.20.1 и 26.x, в пак не подходит);
    - Create: Armored Constructs 1.0.0 — последний релиз (0.0.1 был альфой).
    Обновлять нечего; менять пришлось бы весь RPG-набор (Spell Engine 1.10.7→1.10.9,
    Archers 3.1.1→3.1.3, Paladins 3.1.1→3.1.3), а это меняет лут боссов — отдельная задача.
  - Пересобраны самописы из `src/` с новыми версиями:
    - `meowrelics` **1.1.0** — новое поле `excludedFromBags` в `balance.json` (реликвии,
      убранные из ротации мешочков, меняется без пересборки) и `abilities.<id>.disabled`
      (способность вырезается из карты, а не обнуляется). Плюс `info`-уровень в лог
      балансировщика, чтобы правки было видно в `latest.log`.
    - `lineage_core` **2.1.1** — `FrozenWaters` теперь сверяет биомы по пути
      (`frozen_ocean`, `cold_ocean`, `deep_ocean`) вместо полного id. Раньше лёд
      ловился только на четырех точных id, поэтому модные и глубокие океаны
      (`deep_frozen_ocean`, `deep_cold_ocean`, чужие неймспейсы) пропускались.
      Новый джарник: `mods/lineage_core-2.1.1.jar` (пин `lineage_core-2-1-1.pw.toml`).
  - Конфиг `fmm_worldgen-common.toml` подтянут из инстанса: плотность островов
    сбалансирована (small 0.40→0.17, medium 0.35→0.29, large 0.30→0.29).
  - Плотность островов уменьшена ещё вдвое — `spawn_chance` в
    `config/fmm_worldgen-common.toml` (small 0.17→0.085, medium 0.29→0.145,
    large 0.29→0.145). Размеры сеток и радиусы островов не тронуты, изменена
    только частота появления. Пересчитаны подписи в конфиге: ~15 мелких,
    ~7 средних и ~2 больших острова на 10000×10000 блоков (было ~30/~15/~5).
    Тот же конфиг синхронизирован в dev-окружение (`src/fmmworldgen/run/config`),
    значение можно проверить через `gradlew.bat runClient`; требуется JDK 21
    (`JAVA_HOME` в системе указывает на JDK 8, из-за чего gradle падает на старте).
    В dev-окружение также положен `Panoptic` 1.08a (NeoForge 1.21.1) — тот же джарник,
    что и в `mods/`, но в `src/fmmworldgen/run/mods`, чтобы инспектор островов был
    доступен при локальной проверке генерации.
  - Инструменты: `tools/export-mrpack-clean.py` — экспорт `.mrpack` в чистом дереве
    (`mr export` сам зовёт refresh и иначе тянет в overrides gradle-вывод, +400 МБ);
    `tools/build-prism-pack.py` — сборка zip для PrismLauncher по шагам из workflow
    (раньше это было возможно только на CI). `tools/verify_release.py` переведён
    на 1.1.7-release.2 и проверяет новые артефакты.

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

## v1.1.7-release.2

- **Добавлен `Create Aeronautics: burner fuel` 1.0.2** (`createburnerfuel-1.0.2.jar`).
  Аддон к Create Aeronautics: горелки теперь требуют топливо. Запинен на Modrinth
  (`OizKjGJm` / `e0B0nsFU`) — на CurseForge он тоже есть, но версия там ровно та же,
  так что обходной путь через raw-ссылку на репозиторий не понадобился.
- `armory-rpg-series` и `create-armored-constructs` **оставлены на текущих версиях**:
  - Armory 1.5.2+1.21.1 — последняя сборка под 1.21.1/NeoForge (1.5.3 вышел только
    под 1.20.1 и 26.x, в пак не подходит);
  - Create: Armored Constructs 1.0.0 — последний релиз (0.0.1 был альфой).
  Обновлять нечего; менять пришлось бы весь RPG-набор (Spell Engine 1.10.7→1.10.9,
  Archers 3.1.1→3.1.3, Paladins 3.1.1→3.1.3), а это меняет лут боссов — отдельная задача.
- Пересобраны самописы из `src/` с новыми версиями:
  - `meowrelics` **1.1.0** — новое поле `excludedFromBags` в `balance.json` (реликвии,
    убранные из ротации мешочков, меняется без пересборки) и `abilities.<id>.disabled`
    (способность вырезается из карты, а не обнуляется). Плюс `info`-уровень в лог
    балансировщика, чтобы правки было видно в `latest.log`.
  - `lineage_core` **2.1.1** — `FrozenWaters` теперь сверяет биомы по пути
    (`frozen_ocean`, `cold_ocean`, `deep_ocean`) вместо полного id. Раньше лёд
    ловился только на четырех точных id, поэтому модные и глубокие океаны
    (`deep_frozen_ocean`, `deep_cold_ocean`, чужие неймспейсы) пропускались.
    Новый джарник: `mods/lineage_core-2.1.1.jar` (пин `lineage_core-2-1-1.pw.toml`).
- Конфиг `fmm_worldgen-common.toml` подтянут из инстанса: плотность островов
  сбалансирована (small 0.40→0.17, medium 0.35→0.29, large 0.30→0.29).
- Плотность островов уменьшена ещё вдвое — `spawn_chance` в
  `config/fmm_worldgen-common.toml` (small 0.17→0.085, medium 0.29→0.145,
  large 0.29→0.145). Размеры сеток и радиусы островов не тронуты, изменена
  только частота появления. Пересчитаны подписи в конфиге: ~15 мелких,
  ~7 средних и ~2 больших острова на 10000×10000 блоков (было ~30/~15/~5).
  Тот же конфиг синхронизирован в dev-окружение (`src/fmmworldgen/run/config`),
  значение можно проверить через `gradlew.bat runClient`; требуется JDK 21
  (`JAVA_HOME` в системе указывает на JDK 8, из-за чего gradle падает на старте).
  В dev-окружение также положен `Panoptic` 1.08a (NeoForge 1.21.1) — тот же джарник,
  что и в `mods/`, но в `src/fmmworldgen/run/mods`, чтобы инспектор островов был
  доступен при локальной проверке генерации.
- Инструменты: `tools/export-mrpack-clean.py` — экспорт `.mrpack` в чистом дереве
  (`mr export` сам зовёт refresh и иначе тянет в overrides gradle-вывод, +400 МБ);
  `tools/build-prism-pack.py` — сборка zip для PrismLauncher по шагам из workflow
  (раньше это было возможно только на CI). `tools/verify_release.py` переведён
  на 1.1.7-release.2 и проверяет новые артефакты.

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
