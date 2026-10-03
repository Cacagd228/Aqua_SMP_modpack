# 🛠️ AquaSMP — Таблица тиров Create

> Прогрессия веками. Века открываются **накоплением файлов**.
> Рецепты предметов пропадают без своего века, уже скрафченное — остаётся.

---

## 📦 Как кидать века по порядку

| Шаг | Что лежит в `kubejs/server_scripts/` | Что доступно игрокам |
|-----|--------------------------------------|----------------------|
| 0 | `00_tier_lockdown.js` (всегда) | ❌ Все тированные крафты закрыты |
| 1 | ➕ `tier_andesite.js` | ✅ Андезитовый век |
| 2 | ➕ `tier_iron.js` | ✅ Андезитовый + Железный |
| 3 | ➕ `tier_copper.js` | ✅ Андезитовый + Железный + Медный |
| 4 | ➕ `tier_brass.js` | ✅ Андезитовый + Железный + Медный + Латунный |
| 5 | ➕ `tier_steel.js` (будущий) | ✅ + Стальной |
| 6 | ➕ `tier_dark.js` | ✅ + Тёмная сталь |
| 7 | ➕ `tier_radiance.js` (будущий) | ✅ + Изысканное сияние |
| 8 | ➕ `tier_chromatic.js` | ✅ Всё + Хроматический (ФИНАЛ) |

Плюс **постоянный бан 14 модов** (см. раздел «БАН МОДОВ» ниже).

После каждого закидывания: **`/reload`** (или рестарт сервера). Откатов нет.

**Файлы:**

- `00_tier_lockdown.js` — замок, лежит всегда, удаляет рецепты тированных предметов (`priority: 1000`, выполняется первым).
- `tier_andesite.js` — век 1, возвращает свои рецепты поверх замка.
- `tier_iron.js` — век 2, требует век 1.
- `tier_copper.js` — век 3, требует века 1–2.
- `tier_brass.js` — век 4, требует веков 1–3.
- `tier_steel.js` — век 5, требует веков 1–4.
- `tier_dark.js` — век 6 (harder diesel), требует веков 1–5.
- `tier_radiance.js` — век 7, требует веков 1–6.
- `tier_chromatic.js` — век 8 (ФИНАЛ), кидается последним. Реализован:
  computercraft (16) + create_sa (19).
- `corps_crafting.js` — не трогать (корпуса через рамку + бревно).

**Легенда рецептов:**

- 🛠️ `кастом` — твой изменённый рецепт из тирного файла.
- 📋 `ваниль 1:1` — оригинал мода, скопирован в тирный файл дословно, открывается вместе с веком.
- 🔗 `sequenced only` — только через sequenced-сборку meowaddons, верстачного крафта нет.
- ⚪ `ваниль всегда` — не тируется специально (база: сплав, молот DG).
- ✏️ `черновик` — схема предложена (у Т6 не было заданных рецептов), баланс править в файле.
- 📝 `только список` — рецепты НЕ выдуманы, строка только документирует.

---

## 🪨 ВЕК 1 — Андезитовый (`tier_andesite.js`)

| # | Предмет (RU) | Item ID | Рецепт |
|---|--------------|---------|--------|
| 1 | Андезитовый сплав | `create:andesite_alloy` | ⚪ ваниль всегда (база, не блокируется) |
| 2 | Пластины (молотком, create DG) | `#c:plates/*` (`createdieselgenerators:hammering`) | ⚪ ваниль всегда |
| 3 | Источник энергии — ручка | `create:hand_crank` | 🛠️ кастом |
| 4 | Пресс | `meowaddons:press_t1` | 🛠️ кастом |
| 5 | Пила | `meowaddons:saw_t1` | 🛠️ кастом |
| 6 | Шестерни | `create:cogwheel`, `create:large_cogwheel`, `create:gearbox` | 📋 ваниль 1:1 |
| 7 | Водяное колесо | `create:water_wheel` | 🛠️ кастом |
| 8 | Жёлоб | `create:chute` | 🛠️ кастом |
| 9 | Депо | `create:depot` | 🛠️ кастом |
| 10 | Вал | `create:shaft` | 📋 ваниль 1:1 (8 шт) |
| 11 | Шлюз ❓ | `create:fluid_valve` (?) | ⚪ уточнить ID — пока остаётся ванилью |
| 12 | Воронка | `create:andesite_funnel` | 📋 ваниль 1:1 (2 шт) |
| 13 | Рамка → корпус (андезит) | `meowaddons:frame_andesite` → `create:andesite_casing` | 🛠️ кастом (рамка) + `corps_crafting.js` |
| 14 | Андезитовая столешница (скатерть) | `create:andesite_table_cloth` | 📋 ваниль 1:1 (камнерез, 2 шт) |

---

## ⛓️ ВЕК 2 — Железный (`tier_iron.js`, требует век 1)

| # | Предмет (RU) | Item ID | Рецепт |
|---|--------------|---------|--------|
| 1 | Большое водяное колесо | `create:large_water_wheel` | 🛠️ кастом |
| 2 | Жёрнов | `meowaddons:millstone_t1` | 🛠️ кастом |
| 3 | Вентилятор | `create:encased_fan` | 📋 ваниль 1:1 |
| 4 | Чаша | `create:basin` | 🛠️ кастом |
| 5 | Смешиватель | `meowaddons:mixer_t1` | 🛠️ кастом (нужен `whisk` ниже) |
| 6 | Угловая передача | `create:vertical_gearbox` | 📋 ваниль 1:1 |
| 7 | Цепной привод в корпусе | `create:encased_chain_drive` | 📋 ваниль 1:1 |
| 8 | Механический поршень | `create:mechanical_piston` | 📋 ваниль 1:1 |
| 9 | Липкий механический поршень | `create:sticky_mechanical_piston` | 📋 ваниль 1:1 |
| 10 | Удлинитель поршня | `create:piston_extension_pole` | 📋 ваниль 1:1 (8 шт) |
| 11 | Механический вращатель | `create:mechanical_bearing` | 📋 ваниль 1:1 |
| 12 | Вращатель мельницы (ветряк) | `create:windmill_bearing` | 🛠️ кастом |
| 13 | Парус | `create:white_sail` / `create:sail_frame` | 📋 ваниль 1:1 |
| 14 | Портативный складской интерфейс | `create:portable_storage_interface` | 📋 ваниль 1:1 |
| 15 | Радиальное шасси | `create:radial_chassis` | 📋 ваниль 1:1 (3 шт) |
| 16 | Андезитовый корпус | `create:andesite_casing` | через рамку века 1 |
| 17 | Венчик (для смесителя) | `create:whisk` | 📋 ваниль 1:1 |
| 18 | I2 двигатель | `create_simulated_additions:ckbgi_2_engine` | 📋 ваниль 1:1 (нужен `simulated:engine_assembly`) |

---

## 🟠 ВЕК 3 — Медный (`tier_copper.js`, требует века 1–2)

| # | Предмет (RU) | Item ID | Рецепт |
|---|--------------|---------|--------|
| 1 | Стрессометр | `create:stressometer` | 📝 ваниль, gating TODO (конверсионный рецепт) |
| 2 | Спидометр | `create:speedometer` | 🛠️ кастом |
| 3 | Комбайн | `create:mechanical_harvester` | 📋 ваниль 1:1 |
| 4 | Водолазный сет | `create:copper_diving_helmet` (+ `..._boots`) | 🛠️ кастом (шлем); ботинки — ваниль |
| 5 | Ремень | `create:belt_connector` | 🛠️ кастом |
| 6 | Схемат-пушка | `create:schematicannon` | 📝 только список (ваниль) |
| 7 | Схемат-стол | `create:schematic_table` | 📝 только список (ваниль) |
| 8 | Схематика | `create:empty_schematic` (?) | 📝 только список, уточнить ID |
| 9 | Сцепление | `create:clutch` | 📋 ваниль 1:1 |
| 10 | Реверсивная передача | `create:gearshift` | 📋 ваниль 1:1 |
| 11 | Регулируемая цепная коробка передач | `create:adjustable_chain_gearshift` | 📋 ваниль 1:1 (нужен `electron_tube`) |
| 12 | Жидкостный бак | `create:fluid_tank` | 🛠️ кастом |
| 13 | Помпа | `create:mechanical_pump` | 📋 ваниль 1:1 |
| 14 | Труба | `create:fluid_pipe` | 🔗 sequenced only (верстачного крафта НЕТ) |
| 15 | Буры 💎 | `create:mechanical_drill` | 🛠️ кастом, очень дорого. Правило «лимит 10×10» — договорённость, кодом не режется |
| 16 | Вагонеточный сборщик | `create:cart_assembler` | 📋 ваниль 1:1 |
| 17 | Контроллер штуковины | `create:contraption_controls` | 📋 ваниль 1:1 (нужен `electron_tube`) |
| 18 | Весовая катапульта | `create:weighted_ejector` | 📋 ваниль 1:1 |
| 19 | Линейное шасси | `create:linear_chassis` | 📋 ваниль 1:1 (3 шт) |
| 20 | Вторичное линейное шасси | `create:secondary_linear_chassis` (?) | 📝 только список (конверсия), уточнить |
| 21 | Механический плуг | `create:mechanical_plough` | 📋 ваниль 1:1 |
| 22 | Жидкостный вентиль | `create:fluid_valve` | 📋 ваниль 1:1 |
| 23 | Каретка линейного привода | `create:gantry_carriage` | 📋 ваниль 1:1 |
| 24 | Вал линейного привода | `create:gantry_shaft` | 📋 ваниль 1:1 (8 шт) |
| 25 | Редстоун-контакт ❓ | `create:redstone_contact` (?) | 📝 только список, уточнить ID |
| 26 | Блок-липучка | `create:super_glue` | 📋 ваниль 1:1 |
| 27 | Механический каток | `create:mechanical_roller` | 📋 ваниль 1:1 (нужен `electron_tube`) |
| 28 | Медная столешница (скатерть) | `create:copper_table_cloth` | 📋 ваниль 1:1 (камнерез, 2 шт) |
| 29 | Ящик для инструментов | `create:toolbox` | 📝 ваниль, gating TODO |
| 30 | Жидкостный люк ❓ | `create:hose_pulley` / `createdieselgenerators:...` (?) | 📝 только список, уточнить ID |
| 31 | Двигатель (aeronautica) | `create_simulated_additions:ckbg_mid_drive_engine` | 📋 ваниль 1:1 (нужен `simulated:engine_assembly`) |
| 32 | Hex casting ❓ | ID уточнить | 📝 только список |
| 33 | Электронная лампа | `create:electron_tube` | 📋 ваниль 1:1 (нужна для adjustable/roller/контроллера) |

---

## 🟡 ВЕК 4 — Латунный (`tier_brass.js`, требует век 1–3)

> ID всех предметов проверены по jar'ам. Легенда: 🛠️ кастом, 🛠️+⚙️ кастом
> с заменой компонента на латунный вариант Create Encased.

| # | Предмет (RU) | Item ID | Рецепт |
|---|--------------|---------|--------|
| 1 | Хранилище | `create:item_vault` | 🛠️ кастом |
| 2 | Горелка (пустая) | `create:empty_blaze_burner` | 🛠️ кастом (нужен `create:blaze_cake`) |
| 3 | Паровой двигатель | `create:steam_engine` | 🛠️ кастом (`engine_piston` от DG) |
| 4 | Регулятор скорости вращения | `create:rotation_speed_controller` | 🛠️ кастом |
| 5 | Механическая рука | `create:mechanical_arm` | 🛠️ кастом |
| 6 | Часовой механизм | `create:clockwork_bearing` | 🛠️ кастом |
| 7 | Пресс Т2 | `meowaddons:press_t2` | 🛠️ кастом (апгрейд) |
| 8 | Жёрнов Т2 | `meowaddons:millstone_t2` | 🛠️ кастом (апгрейд) |
| 9 | Смеситель Т2 | `meowaddons:mixer_t2` | 🛠️ кастом (апгрейд) |
| 10 | Пила Т2 | `meowaddons:saw_t2` | 🛠️ кастом (апгрейд) |
| 11 | Деплоер Т2 | `meowaddons:deployer_t2` | 🛠️ кастом (апгрейд) |
| 12 | Дробильные колёса Т2 | `meowaddons:crushing_wheel_t2` | 🛠️ кастом 5×5 (мех. крафт) |
| 13 | Квакопорт (Package Frogport) | `create:package_frogport` | 🛠️ кастом ✅ ID уточнён |
| 14 | Упаковщик | `create:packager` | 🛠️ кастом |
| 15 | Складской передатчик | `create:stock_link` | 🛠️ кастом |
| 16 | Складской тикер | `create:stock_ticker` | 🛠️ кастом |
| 17 | Механический сборщик | `create:mechanical_crafter` | 🛠️ кастом |
| 18 | Умный наблюдатель (Content Observer) | `create:content_observer` | 🛠️ кастом ✅ ID уточнён |
| 19 | Редстоун-запрашиватель | `create:redstone_requester` | 🛠️ кастом ✅ ID уточнён |
| 20 | Латунный шлюз | `create:smart_chute` | 🛠️ кастом |
| 21 | Латунная воронка | `create:brass_funnel` | 🛠️ кастом |
| 22 | Лифтовая лебёдка | `create:elevator_pulley` | 🛠️ кастом |
| 23 | Латунный цепной конвейер | `createcasing:brass_chain_conveyor` | 🛠️ кастом (Create Encased) |
| 24 | Подшипник буровой головы | `offroad:borehead_bearing` | 🛠️ кастом (offroad) |
| 25 | Режущее колесо камня | `offroad:rockcutting_wheel` | 🛠️ кастом (offroad) |
| 26 | Принтер (Enchanting Industries) | `create_enchantment_industry:printer` | 🛠️ кастом |
| 27 | Основа торта опыта | `create_enchantment_industry:experience_cake_base` | 🛠️ прессование |

### ⚠️ Уточнения по ID (бывшие «?»)

- **`create:content_observer`** — в игре называется «Smart Observer». ID `smart_observer` в Create **не существует**. В таблице было неверно.
- **`create:redstone_requester`** ≠ `create:redstone_link`. В Create 6 это **два разных блока**:
  - `create:redstone_requester` (Редстоун-запрашиватель) — рецепт задан здесь;
  - `create:redstone_link` (Редстоун-передатчик) — рецепта нет, остаётся ванилью.
- **Латунный корпус** `create:brass_casing` — через рамку `meowaddons:frame_latun` + `corps_crafting.js`, не в этом файле.
- **`createcasing:brass_adjustable_chain_gearshift`** (нужен для регулятора скорости) — это предмет из мода **Create Encased**, а не `create:adjustable_chain_gearshift` из `tier_copper.js`. Разные предметы, живут параллельно.

### 📝 Век 4 остаётся «только список» (рецепты НЕ выдуманы)

| Предмет (RU) | ID |
|--------------|----|
| Пороговый переключатель | `create:analog_lever` |
| Умная латунная труба | `create:smart_fluid_pipe` |
| Латунный передатчик инфо / табло | `create:redstone_link`, `create:display_board` |
| Последовательная коробка передач | `create:sequenced_gearshift` |
| Редстоун-повторитель/удлинитель/таймер | `create:pulse_repeater`, `create:pulse_extender`, `create:pulse_timer` |
| Латунная столешница (скатерть) | `create:brass_table_cloth` |
| Связь упаковщика | `create:packager_link` |
| V4 engine | `create_simulated_additions:ckbgv_4_engine` |
| hot aero staff, linear bearing, transmission & linkage, cobblestone | hots-aerostuff / linearbearing / createtransmission / createcobblestone |

### 🔗 Циклы прогрессии (будь внимателен)

`stock_link` ← `item_vault`, `redstone_requester` ← `stock_link`, `stock_ticker` ← `stock_link`.
Все рецепты века добавляются одним файлом, поэтому цикл безопасен — предметы разных ступеней,
нельзя получить `stock_link` из `stock_link`. Но **порядок прокачки придётся делать руками**: сначала `item_vault`,
потом `stock_link`, потом `redstone_requester`/`stock_ticker`.

---

## ⚙️ ВЕК 5 — Стальной 📝 (только список, `tier_steel.js` будущий)

> Файл НЕ создан. Только список для будущего `tier_steel.js`.

| Предмет (RU) | Предположительный ID |
|--------------|----------------------|
| Поезда / контроллер / станции / рельсы | `create:controls`, `create:track_station`, `create:controller_rail`, `railways:*` |
| Фабричный контроллер, Package accelerator/editor, Cash register | `create_factory:*`, `create:numismatics:*` |
| Монорельс, топливный бак/интерфейс | `create:track_*`, `create:portable_fluid_interface` |
| Hyper tubes, rock and stone, railways navigator, vintage, fluid logistics, aeroworks, power grid, synaxis, simulated coasters, propulsion, extra gauges | ваниль аддонов, по мере `tier_steel.js` |

Стальной фрейм уже есть: `meowaddons:frame_steel` → `create:railway_casing`.

---

## 🌑 ВЕК 6 — Тёмная сталь (`tier_dark.js`, требует век 1–5)

> Рецепты из main(2).js, ID проверены по jar'ам.

| # | Предмет (RU) | Item ID | Рецепт |
|---|--------------|---------|--------|
| 1 | Бензиновый генератор | `harderdiesel:gasoline_generator` | 🛠️ кастом |
| 2 | Дизельный генератор | `harderdiesel:diesel_generator` | 🛠️ кастом |
| 3 | Газовый генератор | `harderdiesel:gas_generator` | 🛠️ кастом |
| 4 | Нитрогенератор | `harderdiesel:nitro_generator` | 🛠️ кастом |
| 5 | Большой бензиновый | `harderdiesel:large_gasoline_generator` | 🛠️ кастом (апгрейд) |
| 6 | Большой дизельный | `harderdiesel:large_diesel_generator` | 🛠️ кастом (апгрейд) |
| 7 | Большой газовый | `harderdiesel:large_gas_generator` | 🛠️ кастом (апгрейд) |
| 8 | Большой нитрогенератор | `harderdiesel:large_nitro_generator` | 🛠️ кастом (апгрейд) |
| 9 | Огромный бензиновый | `harderdiesel:huge_gasoline_generator` | 🛠️ кастом |
| 10 | Огромный дизельный | `harderdiesel:huge_diesel_generator` | 🛠️ кастом |
| 11 | Огромный нитрогенератор | `harderdiesel:huge_nitro_generator` | 🛠️ кастом |

### ⚠️ Исправленный дубль

В исходнике `harderdiesel:gas_generator` встречался **дважды**. Второй вариант был
self-referential (`C: 'harderdiesel:gas_generator'` — предмет крафтился из самого
себя) и выброшен; осталась первая схема.

**Но есть версия**: второй дубль по структуре (ABA/CDC/EFE) совпадает с серией
`huge_*`. В моде есть блок `harderdiesel:huge_gas_generator`, и у него **нет
рецепта**. Скорее всего это был опечатанный рецепт именно его.
Если нужно — добавлю по схеме серии huge.

### 📝 Век 6 остаётся «только список»

| Предмет (RU) | ID |
|--------------|----|
| Дистилляция / перегонка | `harderdiesel:*_controller`, `harderdiesel:oil_barrel`, `harderdiesel:air_analyzer`, `createdieselgenerators:distillation_controller` |
| Резервуары | `harderdiesel:galvanized_reactor_tank`, `harderdiesel:wear_resistant_tank` |
| Фрейм | `meowaddons:frame_shadow_steel` → `create:shadow_steel_casing` (уже есть) |

⚠️ **Вне тиров:** `scguns:treated_iron / treated_brass / diamond_steel` и
`createbigcannons:nethersteel_ingot` локдауном не блокируются — это самая поздняя
прогрессия в моде, но она доступна с самого начала. Реши, стоит ли тировать.

---

## ✨ ВЕК 7 — Изысканное сияние 📝 (пусто, `tier_radiance.js` будущий)

Список пуст — заполнить позже. Фрейм уже есть: `meowaddons:frame_refined_radiance`
→ `create:refined_radiance_casing`.

---

## 🌈 ВЕК 8 (ФИНАЛ) — Хроматическое соединение (`tier_chromatic.js`)

| # | Предмет (RU) | Item ID | Рецепт |
|---|--------------|---------|--------|
| 1 | Рамка → корпус (хроматик) | `meowaddons:frame_chromatic_compound` → `createcasing:creative_casing` | ✏️ черновик: compound + незерит (рамка) + `corps_crafting.js` |
| 2 | Пресс Т6 | `meowaddons:press_t6` | ✏️ черновик: 4x compound + 2x рамка + 2x creative-корпус + precision + незерит |
| 3 | Пила Т6 | `meowaddons:saw_t6` | ✏️ черновик, та же схема |
| 4 | Жёрнов Т6 | `meowaddons:millstone_t6` | ✏️ черновик, та же схема |
| 5 | Смеситель Т6 | `meowaddons:mixer_t6` | ✏️ черновик, та же схема |
| 6 | Деплоер Т6 | `meowaddons:deployer_t6` | ✏️ черновик, та же схема |
| 7 | Дробильные колёса Т6 | `meowaddons:crushing_wheel_t6` | ✏️ черновик, та же схема |

Сам `create:chromatic_compound` — база века, не блокируется (как сплав в веке 1).

---

## 🚫 БАН МОДОВ (всегда включён, в `00_tier_lockdown.js`)

Помимо тиров, локдаун снимает **все рецепты** этих 14 модов целиком.
modid == неймспейсу рецептов.

| Мод | jar | modid / неймспейс |
|-----|-----|-------------------|
| ComputerCraft | cc-tweaked | `computercraft` |
| Create: Diesel Generators | createdieselgenerators | `createdieselgenerators` |
| Create: Transmission | createtransmission | `createtransmission` |
| Create Aeronautics: Transmission & Linkage | create_aeronautics_transmission_linkage | `aeronautics_utility_objects` |
| Power Grid | powergrid | `powergrid` |
| Steam and Rails | railways | `railways` |
| Synaxis | synaxis | `synaxis` |
| Create: Propulsion | createpropulsion | `createpropulsion` |
| Create: Connected | create_connected | `create_connected` |
| Create: Enchantment Industry | create-enchantment-industry | `create_enchantment_industry` |
| Create: Stuff & Additions | create-stuff-additions | `create_sa` |
| Create: Radars | create_radar | `create_radar` |
| Create: Radiologistics | CreateRadiologistics | `radiologistics` |
| Create: Encased | Create Encased | `createcasing` |

Каждый мод закрыт ДВАЖДЫ: `remove({mod})` + `remove({output: '@ns'})`.

**Исключения (единственное, что живёт):**

- `createdieselgenerators:hammer` — молот, пересоздан 1:1 в локдауне.
- `create:splashing/crushed_raw_gold` — оригинал Create (createpropulsion
  перезаписывал этот рецепт, добавляя 5% платины; бан снял, вернули без платины).

**Разбан мода:** удали строку из `BANNED_MODS` в `00_tier_lockdown.js` → `/reload`.

### ⚠️ Важно про кастомные рецепты

`event.remove()` в KubeJS 7 фильтрует **только рецепты из jar/datapack**.
Рецепты, добавленные самими скриптами (`event.shaped` и т.п.), он НЕ трогает.

Поэтому все кастомные рецепты забаненных модов **перенесены из `main.js` и
`main(1).js` в тирные файлы**. Если вернуть их в `main.js` — бан не сработает
и предметы станут доступны сразу.

| Куда | Что | Кол-во |
|------|-----|--------|
| `tier_brass.js` | `createdieselgenerators:engine_piston`, `:burner` | 2 |
| `tier_brass.js` | `gnkinetics:*` (12 рецептов) | 12 |
| `tier_dark.js` | `createdieselgenerators:*` (нефтяная цепь, 6) | 6 |
| `tier_dark.js` | `harderdiesel:*` (контроллеры/резервуары, 5) | 5 |
| `tier_chromatic.js` | `computercraft:*` | 16 |
| `tier_chromatic.js` | `create_sa:*` | 19 |
| — | `aeronautics_utility_objects:*` — удалены навсегда | 8 |

`main.js` теперь пуст (все 60 рецептов перенесены).
`main(1).js` остались `hexcasting:*`, `aeroworks:*`, `create:brass_sheet`.

### ⚠️ Рецепты под замком, но с недостижимыми ингредиентами

Бан модов закрывает и предметы, которые нужны как **ингредиенты** другим
рецептам. Такие рецепты лежат в тирных файлах, но не соберутся, пока
зависимые моды не разбанены:

| Рецепт | Нужен ингредиент из | Где лежит |
|--------|--------------------|-----------|
| `gnkinetics:magnet_gear`, `:large_magnet_gear` | `powergrid:magnet` | век 4 |
| `createdieselgenerators:oil_scanner` | `radiologistics:transparent_screen` | век 6 |
| `createdieselgenerators:chemical_sprayer`, `:chemical_turret` | `createcasing:brass_mechanical_pump`, `create_sa:large_filling_tank` | век 6 |
| `createdieselgenerators:burner` | `powergrid:resistive_coil` | век 4 |
| `create:steam_engine` | `createdieselgenerators:engine_piston` | век 4 — ОК, engine_piston есть в веке 4 |
| `create:rotation_speed_controller` | `createcasing:brass_adjustable_chain_gearshift` | век 4 |
| `offroad:rockcutting_wheel` | `createtransmission:transmission_chain` | век 4 |
| `create_enchantment_industry:printer` | `create_enchantment_industry:super_experience_block` | век 4 |
| `harderdiesel:nitro_generator`, `:huge_nitro_generator` | `create_enchantment_industry:super_experience_block` | век 6 |
| все `computercraft:*` | `powergrid:*`, `radiologistics:*`, `create_radar:monitor`, `createpropulsion:platinum_sheet` | век 8 |
| почти все `create_sa:*` | `powergrid:*`, `createcasing:*`, `radiologistics:*`, `create_sa:medium_*_tank` | век 8 |

Чтобы починить любую строку — разбанить нужный мод (убрать из `BANNED_MODS`)
и добавить его рецепты в нужный тирной файл.

---

## ✅ Чек-лист админа

- [ ] В папке всегда `00_tier_lockdown.js`.
- [ ] Кидаю `tier_andesite.js` → `/reload` → проверяю ручку/вал/шестерни/воронку/скатерть.
- [ ] Кидаю `tier_iron.js` → `/reload` → проверяю жёрнов/смеситель/поршни/I2.
- [ ] Кидаю `tier_copper.js` → `/reload` → проверяю бак/бур/лампы/скатерть, труба ТОЛЬКО через sequenced.
- [ ] Кидаю `tier_brass.js` → `/reload` → проверяю vault/горелку/паровой двигатель/Т2-механизмы/упаковщик/тикер/наблюдатель.
- [ ] Кидаю `tier_steel.js` (когда появится) → `/reload`.
- [ ] Кидаю `tier_dark.js` → `/reload` → проверяю 11 генераторов harder diesel.
- [ ] Кидаю `tier_chromatic.js` последним → `/reload` → проверяю рамку и Т6.
- [ ] Уже скрафченное не пропадает — пропадают только рецепты.
- [ ] ❓ с `?` — уточнить точные ID перед будущим gating'ом.

---

## 📁 Архив разобранных файлов

| Файл | Куда ушло |
|------|------------|
| `main(3).js` → `main(3).js.bak` | 25 рецептов → `tier_brass.js` |
| `main(2).js` → `main(2).js.bak` | дубль тех же 25 → `tier_brass.js`; `offroad:*` (2) → `tier_brass.js`; `harderdiesel:*` (11) → `tier_dark.js` |

Все recipe ID теперь с префиксом `aquasmp:<tier>/<item>` — по нему видно, откуда рецепт,
и можно точечно переопределять.
