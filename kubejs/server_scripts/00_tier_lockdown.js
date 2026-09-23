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
//  - После закидывания/удаления: /reload (или рестарт сервера).
//
// ВАЖНО: priority 1000 = выполняется ПЕРВЫМ, до tier_*.js,
// поэтому tier-файлы спокойно добавляют рецепты поверх удалённых.
//
// Что НЕ блокируется (специально):
//  - create:andesite_alloy (база всего, нужен всегда)
//  - create:andesite_casing через item_application (рамка + бревно,
//    см. corps_crafting.js) — рамка сама тирована (frame_andesite в T1)
//  - молот create DG (hammering) и пластины — остаются ванильными
// ============================================================

ServerEvents.recipes(event => {
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
    'create:mechanical_roller'
  ]

  LOCKED_OUTPUTS.forEach(id => event.remove({ output: id }))
})
