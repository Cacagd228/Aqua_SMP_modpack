// priority: 1000
// ============================================================
// 00_tier_lockdown.js — ЗАМОК ПРОГРЕССИИ (лежит ВСЕГДА)
// ============================================================
// Как это работает:
//  - Этот файл УДАЛЯЕТ рецепты всех тированных предметов.
//  - Каждый файл tier_*.js, когда он ЗАКИНУТ в папку, возвращает
//    рецепты своего века обратно (точь-в-точь или кастом).
//  - Нет файла века = век закрыт. Есть файл = век открыт.
//  - Файлы кидаются НАКОПИТЕЛЬНО:
//      только tier_andesite.js            -> только Андезитовый век
//      + tier_iron.js                     -> Андезит + Железо
//      + tier_copper.js                   -> Андезит + Железо + Медь
//      + tier_brass.js                    -> + Латунь
//      + tier_steel.js                    -> + Сталь
//      + tier_dark.js                     -> + Тёмная сталь
//      + tier_chromatic.js                -> + Хроматический (ФИНАЛ)
//  - После закидывания/удаления: /reload (или рестарт сервера).
//
// ВАЖНО: priority 1000 = выполняется ПЕРВЫМ, до tier_*.js,
// поэтому tier-файлы спокойно добавляют рецепты поверх удалённых.
//
// Что НЕ блокируется (специально):
//  - create:andesite_alloy (база всего, нужен всегда)
//  - create:andesite_casing через item_application (рамка + бревно,
//    см. corps_crafting.js) — рамка сама тирована (frame_andesite в T1)
//  - молот create DG (createdieselgenerators:hammer) — пересоздан ниже
//
// ⚠️ ТЕХНИЧЕСКИЙ НЮАНС (проверено по байткоду KubeJS 7):
//   event.remove() фильтрует ТОЛЬКО рецепты, загруженные из jar/datapack
//   (originalRecipes). Рецепты, добавленные самими скриптами (event.shaped
//   и т.п.), он НЕ трогает. Поэтому «забаненные» кастомные рецепты
//   из main.js перенесены в тирные файлы — там они и лежат до открытия
//   века. Не возвращайте их в main.js!
//   Про хек: раньше hexcasting/meowhex стояли в BANNED_MODS целиком.
//   Сейчас разбанены (магия доступна с первого века, рецепты из jar'а),
//   под замком остался только артефакт и мусорные meowhex:assembly
//   тест-рецепты — см. ЧАСТЬ 2.1. Шесть кастомных рецептов на хек
//   лежат в archive_hexcasting_recipes.js.bak и НЕ применяются: они
//   дублируют ванильные, а артефакт из них всё равно заблокирован.
// ============================================================

ServerEvents.recipes(event => {
  // ==========================================================
  // ЧАСТЬ 1 — ТИРОВЫЙ ЗАМОК (индивидуальные предметы)
  // ==========================================================
  const LOCKED_OUTPUTS = [
    // ---- T1: Андезитовый век ----
    'create:hand_crank',
    'meowaddons:frame_andesite',
    'meowaddons:press_t1',
    'meowaddons:saw_t1',
    'create:water_wheel',
    'create:chute',
    'create:depot',
    'create:shaft',
    'create:cogwheel',
    'create:large_cogwheel',
    'create:gearbox',

    // ---- T2: Железный век ----
    'create:large_water_wheel',
    'meowaddons:millstone_t1',
    'create:basin',
    'meowaddons:mixer_t1',
    'create:windmill_bearing',
    'create:encased_fan',
    'create:vertical_gearbox',
    'create:encased_chain_drive',
    'create:mechanical_piston',
    'create:sticky_mechanical_piston',
    'create:piston_extension_pole',
    'create:mechanical_bearing',
    'create:white_sail',
    'create:sail_frame',
    'create:portable_storage_interface',
    'create:radial_chassis',
    'create:whisk',

    // ---- T3: Медный век (только то, что уже задано рецептами) ----
    'create:speedometer',
    'create:copper_diving_helmet',
    'create:belt_connector',
    'create:fluid_tank',
    'create:fluid_pipe',
    // Насос на спине. Рецепт create:copper_backtank (верстачный) снимается.
    // create:netherite_backtank отдельным ID не локаем: оба его рецепта —
    // smithing_transform, и база там create:copper_backtank, так что без
    // медного баллона нетерятитовый тоже не собрать.
    'create:copper_backtank',
    'create:mechanical_drill',
    'create:mechanical_harvester',
    'create:mechanical_plough',
    'create:mechanical_pump',
    'create:clutch',
    'create:gearshift',
    'create:adjustable_chain_gearshift',
    'create:cart_assembler',
    'create:weighted_ejector',
    'create:linear_chassis',
    'create:fluid_valve',
    'create:gantry_carriage',
    'create:gantry_shaft',
    'create:super_glue',
    'create:mechanical_roller',

    // ---- T4: Латунный век (только то, что уже задано рецептами) ----
    'createcasing:brass_chain_conveyor',
    'create:item_vault',
    'create:empty_blaze_burner',
    'create:steam_engine',
    'create:rotation_speed_controller',
    'meowaddons:press_t2',
    'meowaddons:millstone_t2',
    'meowaddons:mixer_t2',
    'meowaddons:saw_t2',
    'meowaddons:deployer_t2',
    'meowaddons:crushing_wheel_t2',
    'create:mechanical_arm',
    'create:clockwork_bearing',
    'create:content_observer',
    'create:smart_chute',
    'create:brass_funnel',
    'create:elevator_pulley',
    'create:packager',
    'create:stock_link',
    'create:stock_ticker',
    'create:mechanical_crafter',
    'create:redstone_requester',
    'create:package_frogport',
    'offroad:borehead_bearing',
    'offroad:rockcutting_wheel',
    'create_enchantment_industry:printer',
    'create_enchantment_industry:experience_cake_base',

    // ---- ВЕКА 5–8: без рецептов в тирных файлах, локдаун не нужен ----

    // ---- T6: Век тёмной стали (harder diesel) ----
    'harderdiesel:gasoline_generator',
    'harderdiesel:diesel_generator',
    'harderdiesel:gas_generator',
    'harderdiesel:nitro_generator',
    'harderdiesel:large_gasoline_generator',
    'harderdiesel:large_diesel_generator',
    'harderdiesel:large_gas_generator',
    'harderdiesel:large_nitro_generator',
    'harderdiesel:huge_gasoline_generator',
    'harderdiesel:huge_diesel_generator',
    'harderdiesel:huge_nitro_generator',

    // ---- Create: Big Cannons — боеприпасы (вне тиров) ----
    // Лист гильзы — вход в ВСЮ цепочку патронов:
    //   create:cutting (латунь/медь/золото/железо) -> autocannon_cartridge_sheet
    //   -> create:sequenced_assembly (прессование, 6 шагов) -> empty_autocannon_cartridge
    //   -> create:sequenced_assembly (наполнение)     -> filled_autocannon_cartridge
    // Снимаем output, а не mod: 4 рецепта cutting из jar'а одним фильтром,
    // а рецепты, которые из листа ДЕЛАЮТ патроны (pressing/filling), не трогаем —
    // они и так становятся недостижимыми, т.к. лист не собрать.
    // Мод целиком не баним: стволы/стволки не входят в цепочку патронов.
    'createbigcannons:autocannon_cartridge_sheet'
  ]

  LOCKED_OUTPUTS.forEach(id => event.remove({ output: id }))

  // --- Бан промывания гравия (create:splashing) ---
  // Промывание гравия даёт железные/золотые кусочки и другие ресурсы.
  // Блокируем ВСЕ рецепты промывания гравия, чтобы закрыть этот путь
  // получения железных кусочков (и прочих ценных дропов).
  event.remove({ type: 'create:splashing', input: 'minecraft:gravel' })

  // ==========================================================
  // ЧАСТЬ 2 — ПОЛНЫЙ БАН МОДОВ
  // ==========================================================
  // Каждый мод закрыт ДВАЖДЫ:
  //   а) event.remove({ mod })  — все рецепты, зарегистрированные модом
  //      (рецепты лежат в data/<modid>/recipe/). modid == неймспейсу.
  //   б) event.remove({ output: '@<ns>' }) — ВСЕ рецепты из любых модов,
  //      которые ПРОИЗВОДЯТ предмет этого мода. Это ловит:
  //        - кросс-модные рецепты (например create:splashing/crushed_raw_gold
  //          из createpropulsion выдаёт createpropulsion:platinum_nugget);
  //        - рецепты, которые потом добавят другие моды.
  //
  // СПИСОК ЗАБАНЕННЫХ МОДОВ (modid == неймспейс рецептов):
  const BANNED_MODS = [
    'computercraft',                  // ComputerCraft
    'createdieselgenerators',         // Create: Diesel Generators
    'createtransmission',             // Create: Transmission
    'aeronautics_utility_objects',    // Create Aeronautics: Transmission & Linkage
    'powergrid',                      // Power Grid
    'railways',                       // Steam and Rails
    'synaxis',                        // Synaxis
    'createpropulsion',               // Create: Propulsion
    'create_connected',               // Create: Connected
    'create_enchantment_industry',    // Create: Enchantment Industry
    'create_sa',                      // Create: Stuff & Additions
    'create_radar',                   // Create: Radars
    'radiologistics',                 // Create: Radiologistics
    'createcasing',                   // Create: Encased
    // ⚠️ Hexcasting (meowhex.jar = форк hexcasting + контент meow) больше НЕ в бане.
    // Открыт полностью, оба неймспейса: hexcasting (111 рецептов) и meowhex
    // (стафы, орбы, кулоны, spellbook_cover, assembly). Магия доступна
    // с медного века без тирных файлов — рецепты лежат в jar'е.
    // Под локом осталось только одно — см. ЧАСТЬ 2.1 ниже.
  ]

  BANNED_MODS.forEach(ns => {
    event.remove({ mod: ns })        // рецепты самого мода
    event.remove({ output: '@' + ns }) // рецепты ИЗ ЛЮБЫХ модов, что дают его предметы
  })

  // ==========================================================
  // ЧАСТЬ 2.1 — ХЕК ПОД ЗАМКОМ (точечно, не бан мода)
  // ==========================================================
  // Разбан hexcasting/meowhex не должен открыть:
  //
  // 1) Артефакт (hexcasting:artifact) — вход в магию. Снимаем ВСЕ рецепты
  //    с таким output: и ванильный из jar'а
  //    (data/hexcasting/recipe/artifact.json), и кросс-модные.
  //    Артефакт по-прежнему выпадает только из мешков Relics
  //    (meowrelics:relic_bag_loot, hex_chance = 0.04).
  //
  event.remove({ output: 'hexcasting:artifact' })
  //
  // 2) Ожерелья из лута мешков Relics — оставляем только дроп:
  //      meowhex:charged_amethyst_necklace (удваивает максимум маны)
  //      meowhex:overloaded_necklace   (перегрузка, бьёт по мане)
  //    Оба предмета в пуле артефактов meowrelics
  //    (ArtifactRandomizer: mana_berry, amethyst_necklace,
  //     charged_amethyst_necklace, lightning_rod, hourglass,
  //     self_torture_ring, overloaded_necklace), т.е. это дроп, а не крафт.
  //    Базовый meowhex:amethyst_necklace (золото + аметист + нить) НЕ трогаем:
  //    он и обычный крафт, и в пуле Relics.
  //    Рецептов ровно два, оба data/meowhex/recipe/*.json — фильтр по output
  //    ловит их целиком, кросс-модных нет (проверено по всем jar'ам).
  event.remove({ output: 'meowhex:charged_amethyst_necklace' })
  event.remove({ output: 'meowhex:overloaded_necklace' })
  //
  // 3) Отладочные рецепты автора meowhex — 7 файлов
  //    data/meowhex/recipe/assembly/test_*.json (медь -> изумруд,
  //    золото -> незеритовый лом, железо -> золотое яблоко и т.п.).
  //    Это рабочие рецепты типа meowhex:assembly, чистый мусор автора.
  //    Закрываем по ТИПУ рецепта — фильтр по mod их не берёт, потому что
  //    неймспейс meowhex теперь разбанен.
  event.remove({ type: 'meowhex:assembly' })

  // ==========================================================
  // ЧАСТЬ 3 — ИСКЛЮЧЕНИЯ (единственное, что остаётся жить)
  // ==========================================================

  // --- 3.1 Молот Create: Diesel Generators ---
  // Секция выше сняла ВСЕ рецепты createdieselgenerators, включая молот.
  // Возвращаем молот 1:1 из оригинала мода:
  //   data/createdieselgenerators/recipe/crafting/hammer.json
  event.shaped('createdieselgenerators:hammer', [
    'AIA',
    'ISI',
    'SIA'
  ], {
    A: 'create:andesite_alloy',
    I: '#c:ingots/iron',
    S: '#c:rods/wooden'
  }).id('aquasmp:base/cdg_hammer')

  // --- 3.2 Восстановление create:splashing/crushed_raw_gold ---
  // createpropulsion ПЕРЕЗАПИСЫВАЕТ этот рецепт у Create (тот же id
  // data/create/recipe/splashing/crushed_raw_gold.json) и добавляет
  // 5% шанс на createpropulsion:platinum_nugget. Бан по output его снимает.
  // Возвращаем оригинал Create без платины:
  event.custom({
    type: 'create:splashing',
    ingredients: [{ item: 'create:crushed_raw_gold' }],
    results: [
      { id: 'minecraft:gold_nugget', count: 9 },
      { id: 'minecraft:quartz', chance: 0.5 }
    ]
  }).id('aquasmp:base/crushed_raw_gold')

  // --- 3.3 Восстановление create:cutting/shaft ---
  // Аналогично: create_connected добавляет свой data/create/recipe/cutting/shaft.json
  // с результатом create_connected:shear_pin. Бан по output его снимает.
  // У Create 6 такого рецепта НЕТ, восстанавливать нечего — ничего не делаем.
})
