// ============================================================
// tier_brass.js — ВЕК 4: Латунный
// ============================================================
// КИНЬ ЭТОТ ФАЙЛ В server_scripts + /reload = открыть век.
// Требует: 00_tier_lockdown.js + tier_andesite.js + tier_iron.js
//          + tier_copper.js.
// Без этого файла рецепты века закрыты (удалены локдауном).
//
// Источник: main(3).js + offroad-рецепты из main(2).js (Recipe Builder
// от 29.09.2026). Все ID проверены по jar'ам:
//   create-1.21.1-6.0.10, Create Encased (createcasing),
//   meowaddons-1.0.1, create-enchantment-industry-2.5.3,
//   createtransmission-1.2.2, offroad (jarjar в create-aeronautics-bundled)
//
// Содержит:
//  А) ЛАТУННЫЕ БЛОКИ CREATE (item_vault, blaze_burner, steam_engine,
//     rotation_speed_controller, mechanical_arm, clockwork_bearing...)
//  Б) Т2 МЕХАНИЗМЫ MEOWADDONS (press/millstone/mixer/saw/deployer/
//     crushing_wheel) — upgrade с T1-версий
//  В) ЛОГИСТИКА (frogport, packager, stock_link/ticker, mechanical_crafter)
//  Г) КРАСНЫЙ КАМЕНЬ (content_observer, smart_chute, redstone_requester,
//     brass_funnel, elevator_pulley)
//  Д) ЛАЛУННЫЕ ВАРИАНТЫ CREATE ENCASED (createcasing:brass_*)
//  Е) OFFROAD (камень-бурение)
//  Ж) АДДОНЫ (Enchanting Industries)
//
// ПОМЕТКИ / ПРОБЛЕМЫ (проверено, но требует решения владельца):
//  - create:redstone_requester — это НЕ create:redstone_link из старой
//    таблицы. В Create 6 это два разных блока, рецепт задан только
//    на requester. В TIERS.md строка обновлена.
//  - create:content_observer — в langCreate он называется
//    "Smart Observer", ID именно content_observer (не smart_observer).
//  - create:rotation_speed_controller требует
//    createcasing:brass_adjustable_chain_gearshift — это НЕ тот же
//    предмет, что create:adjustable_chain_gearshift из tier_copper.js
//    (другой мод — Create Encased). Оба существуют параллельно.
//  - create:stock_link / stock_ticker / redstone_requester / item_vault
//    образуют цикл: stock_link ← item_vault, redstone_requester ← stock_link.
//    Порядок разблокировки внутри века на это не влияет (рецепты
//    добавляются одним файлом), но прогрессию придёт делать руками.
//  - create:package_frogport в старой таблице значился как «Квакопорт (?)» —
//    ID уточнён.
//  - Всё, что в TIERS.md для века 4 осталось «только список» без
//    рецепта (analog_lever, smart_fluid_pipe, display_board,
//    redstone_link, packager_link, brass_casing/рамка, brass_table_cloth,
//    pulse_*, ckbgv_4_engine, smart-оборудование hots-aerostuff,
//    linear-bearing, transmission & linkage, createcobblestone) —
//    НЕ выдумывается. См. TIERS.md.
// ============================================================

ServerEvents.recipes(event => {
  // ---------- А) ЛАТУННЫЕ БЛОКИ CREATE ----------
  event.shaped('create:item_vault', [
    'AAA',
    'ABA',
    'AAA'
  ], {
    A: 'create:iron_sheet',
    B: 'minecraft:barrel'
  }).id('aquasmp:brass/item_vault')

  event.shaped('create:empty_blaze_burner', [
    'AAA',
    'ABA',
    'AAA'
  ], {
    A: 'create:iron_sheet',
    B: 'create:blaze_cake'
  }).id('aquasmp:brass/empty_blaze_burner')

  event.shaped('create:steam_engine', [
    ' A ',
    ' B ',
    'CCC'
  ], {
    A: 'create:brass_sheet',
    B: 'createdieselgenerators:engine_piston',
    C: 'minecraft:copper_block'
  }).id('aquasmp:brass/steam_engine')

  event.shaped('create:rotation_speed_controller', [
    'ABA',
    'CDC',
    'EEE'
  ], {
    A: 'create:electron_tube',
    B: 'create:precision_mechanism',
    C: 'create:brass_casing',
    D: 'create:large_cogwheel',
    E: 'createcasing:brass_adjustable_chain_gearshift'
  }).id('aquasmp:brass/rotation_speed_controller')

  event.shaped('create:mechanical_arm', [
    'AAB',
    'A  ',
    'CDC'
  ], {
    A: 'create:brass_sheet',
    B: 'create:andesite_alloy',
    C: 'create:precision_mechanism',
    D: 'create:brass_casing'
  }).id('aquasmp:brass/mechanical_arm')

  event.shaped('create:clockwork_bearing', [
    ' A ',
    'BCB',
    ' D '
  ], {
    A: 'minecraft:andesite_slab',
    B: 'create:brass_ingot',
    C: 'create:brass_casing',
    D: 'create:electron_tube'
  }).id('aquasmp:brass/clockwork_bearing')

  // ---------- Б) Т2 МЕХАНИЗМЫ MEOWADDONS ----------
  // Апгрейд с T1-версии (мешок T1 из тира 1–2 обязателен).
  event.shaped('meowaddons:press_t2', [
    'A',
    'B',
    'C'
  ], {
    A: 'create:brass_sheet',
    B: 'create:mechanical_press',
    C: 'create:brass_block'
  }).id('aquasmp:brass/press_t2')

  event.shaped('meowaddons:millstone_t2', [
    ' A ',
    ' B ',
    'CDC'
  ], {
    A: 'create:large_cogwheel',
    B: 'meowaddons:millstone_t1',
    C: 'create:brass_casing',
    D: 'minecraft:smooth_stone'
  }).id('aquasmp:brass/millstone_t2')

  event.shaped('meowaddons:mixer_t2', [
    ' A ',
    'BCB',
    ' D '
  ], {
    A: 'create:large_cogwheel',
    B: 'create:brass_casing',
    C: 'meowaddons:mixer_t1',
    D: 'create:whisk'
  }).id('aquasmp:brass/mixer_t2')

  event.shaped('meowaddons:saw_t2', [
    ' A ',
    'ABA',
    'CDC'
  ], {
    A: 'create:brass_sheet',
    B: 'create:brass_ingot',
    C: 'create:brass_casing',
    D: 'meowaddons:saw_t1'
  }).id('aquasmp:brass/saw_t2')

  event.shaped('meowaddons:deployer_t2', [
    ' A ',
    'BCB',
    ' D '
  ], {
    A: 'create:electron_tube',
    B: 'create:brass_casing',
    C: 'create:deployer',
    D: 'create:brass_hand'
  }).id('aquasmp:brass/deployer_t2')

  // Дробильные колёса T2 — 5x5, единственный механический крафт в пачке.
  event.recipes.create.mechanical_crafting('meowaddons:crushing_wheel_t2', [
    ' AAA ',
    'AABAA',
    'ABCBA',
    'AABAA',
    ' AAA '
  ], {
    A: 'create:andesite_alloy',
    B: 'create:brass_casing',
    C: 'create:crushing_wheel'
  }).id('aquasmp:brass/crushing_wheel_t2')

  // ---------- В) ЛОГИСТИКА ----------
  event.shaped('create:package_frogport', [
    ' A ',
    ' B ',
    'CDC'
  ], {
    A: 'minecraft:slime_ball',
    B: 'minecraft:chain',
    C: 'create:precision_mechanism',
    D: 'create:brass_casing'
  }).id('aquasmp:brass/package_frogport')

  event.shaped('create:packager', [
    ' A ',
    'ABA',
    'CAC'
  ], {
    A: 'create:brass_ingot',
    B: 'create:cardboard_block',
    C: 'create:electron_tube'
  }).id('aquasmp:brass/packager')

  event.shaped('create:stock_link', [
    ' A ',
    'BCB',
    ' D '
  ], {
    A: 'create:transmitter',
    B: 'create:brass_sheet',
    C: 'create:item_vault',
    D: 'create:brass_casing'
  }).id('aquasmp:brass/stock_link')

  event.shaped('create:stock_ticker', [
    'A',
    'B',
    'C'
  ], {
    A: 'minecraft:glass',
    B: 'create:stock_link',
    C: 'create:brass_casing'
  }).id('aquasmp:brass/stock_ticker')

  event.shaped('create:mechanical_crafter', [
    'A',
    'B',
    'C'
  ], {
    A: 'create:electron_tube',
    B: 'create:brass_casing',
    C: 'minecraft:crafting_table'
  }).id('aquasmp:brass/mechanical_crafter')

  // ---------- Г) КРАСНЫЙ КАМЕНЬ ----------
  // content_observer = «Smart Observer» в переводе Create.
  event.shaped('create:content_observer', [
    ' A ',
    ' B ',
    'BCB'
  ], {
    A: 'create:electron_tube',
    B: 'create:brass_casing',
    C: 'minecraft:observer'
  }).id('aquasmp:brass/content_observer')

  event.shaped('create:redstone_requester', [
    'A',
    'B',
    'C'
  ], {
    A: 'create:electron_tube',
    B: 'create:stock_link',
    C: 'create:iron_sheet'
  }).id('aquasmp:brass/redstone_requester')

  event.shaped('create:smart_chute', [
    ' A ',
    ' B ',
    'ACA'
  ], {
    A: 'create:brass_sheet',
    B: 'create:chute',
    C: 'create:electron_tube'
  }).id('aquasmp:brass/smart_chute')

  event.shaped('create:brass_funnel', [
    'AAA',
    'ABA',
    'ACA'
  ], {
    A: 'create:brass_ingot',
    B: 'create:electron_tube',
    C: 'create:belt_connector'
  }).id('aquasmp:brass/brass_funnel')

  event.shaped('create:elevator_pulley', [
    'ABA',
    ' C ',
    'DDD'
  ], {
    A: 'create:brass_sheet',
    B: 'create:brass_casing',
    C: 'create:belt_connector',
    D: 'create:iron_sheet'
  }).id('aquasmp:brass/elevator_pulley')

  // ---------- Д) ЛАЛУННЫЕ ВАРИАНТЫ CREATE ENCASED ----------
  // Цепной конвейер из Create Encased (createcasing), латунный.
  event.shaped('createcasing:brass_chain_conveyor', [
    'ABA',
    'BCB',
    'ABA'
  ], {
    A: 'minecraft:chain',
    B: 'create:brass_casing',
    C: 'create:shaft'
  }).id('aquasmp:brass/createcasing_brass_chain_conveyor')

  // ---------- Е) OFFROAD (камень-бурение) ----------
  event.shaped('offroad:borehead_bearing', [
    'A',
    'B',
    'C'
  ], {
    A: 'minecraft:andesite_slab',
    B: 'create:gearbox',
    C: 'minecraft:iron_block'
  }).id('aquasmp:brass/borehead_bearing')

  event.shaped('offroad:rockcutting_wheel', [
    ' A ',
    'BCB',
    ' D '
  ], {
    A: 'create:crushing_wheel',
    B: 'createtransmission:transmission_chain',
    C: 'minecraft:iron_block',
    D: 'create:precision_mechanism'
  }).id('aquasmp:brass/rockcutting_wheel')

  // ---------- Ж) АДДОНЫ ----------
  event.shaped('create_enchantment_industry:printer', [
    'ABA',
    'CDC',
    'CEC'
  ], {
    A: 'create:brass_block',
    B: 'create_enchantment_industry:super_experience_block',
    C: 'create:precision_mechanism',
    D: 'create:spout',
    E: 'minecraft:iron_block'
  }).id('aquasmp:brass/cei_printer')

  event.recipes.create.pressing([
    'minecraft:diamond',
    'minecraft:glowstone_dust',
    'minecraft:lapis_lazuli'
  ], 'create_enchantment_industry:experience_cake_base').id('aquasmp:brass/cei_experience_cake_base')

  // ---------- З) Create: Diesel Generators (базовые детали) ----------
  // Двигатель piston — нужен для парового двигателя выше (век 4).
  // Горелка — нужна для create_sa:flamethrower (век 8).
  event.shaped('2xcreatedieselgenerators:engine_piston', [
    'AB ',
    'BCB',
    ' BD'
  ], {
    A: 'create:andesite_alloy_block',
    B: 'create:iron_sheet',
    C: 'create:shaft',
    D: 'createbb:copper_zinc_catalyst'
  }).id('aquasmp:brass/cdg_engine_piston')

  event.shaped('createdieselgenerators:burner', [
    'ABA',
    'CDC',
    'EFE'
  ], {
    A: 'powergrid:resistive_coil',
    B: 'scguns:treated_brass_ingot',
    C: 'createdieselgenerators:lighter',
    D: 'create:shaft',
    E: 'createbb:copper_zinc_catalyst',
    F: 'create:empty_blaze_burner'
  }).id('aquasmp:brass/cdg_burner')

  // ---------- И) GnKinetics (шестерни/моторы) ----------
  // Перенесено из main.js. ВНИМАНИЕ: magnet_gear и large_magnet_gear
  // требуют powergrid:magnet — он под баном, поэтому эти два рецепта
  // фактически недоступны, пока не откроется powergrid.
  event.shaped('gnkinetics:andesite_cogwheel', [
    'ABC'
  ], {
    A: 'create:shaft',
    B: 'create:andesite_alloy',
    C: 'create:cogwheel'
  }).id('aquasmp:brass/gnk_andesite_cogwheel')

  event.shaped('gnkinetics:brass_gear', [
    'ABC'
  ], {
    A: 'create:shaft',
    B: 'create:brass_ingot',
    C: 'create:cogwheel'
  }).id('aquasmp:brass/gnk_brass_gear')

  event.shapeless('gnkinetics:chainable_cogwheel', [
    'create:andesite_casing',
    'create:large_cogwheel',
    'create:minecart_coupling',
    'create:minecart_coupling'
  ]).id('aquasmp:brass/gnk_chainable_cogwheel')

  event.shapeless('gnkinetics:cog_crank', [
    'create:hand_crank',
    'create:hand_crank',
    'create:cogwheel'
  ]).id('aquasmp:brass/gnk_cog_crank')

  event.shaped('gnkinetics:cogstone', [
    'ABC'
  ], {
    A: 'create:shaft',
    B: 'minecraft:andesite',
    C: 'create:cogwheel'
  }).id('aquasmp:brass/gnk_cogstone')

  event.shaped('gnkinetics:industrial_gear', [
    'ABC'
  ], {
    A: 'create:shaft',
    B: 'create:industrial_iron_block',
    C: 'create:cogwheel'
  }).id('aquasmp:brass/gnk_industrial_gear')

  event.shaped('gnkinetics:large_brass_gear', [
    'ABB',
    'C  '
  ], {
    A: 'create:shaft',
    B: 'create:brass_ingot',
    C: 'create:large_cogwheel'
  }).id('aquasmp:brass/gnk_large_brass_gear')

  event.shapeless('gnkinetics:large_cog_crank', [
    'create:hand_crank',
    'create:hand_crank',
    'create:large_cogwheel'
  ]).id('aquasmp:brass/gnk_large_cog_crank')

  event.shaped('gnkinetics:large_industrial_gear', [
    'ABB',
    'C  '
  ], {
    A: 'create:shaft',
    B: 'create:industrial_iron_block',
    C: 'create:large_cogwheel'
  }).id('aquasmp:brass/gnk_large_industrial_gear')

  event.shaped('gnkinetics:large_magnet_gear', [
    ' A ',
    'ABA',
    ' A '
  ], {
    A: 'powergrid:magnet',
    B: 'gnkinetics:large_industrial_gear'
  }).id('aquasmp:brass/gnk_large_magnet_gear')

  event.shaped('gnkinetics:magnet_gear', [
    'A',
    'B',
    'A'
  ], {
    A: 'powergrid:magnet',
    B: 'gnkinetics:industrial_gear'
  }).id('aquasmp:brass/gnk_magnet_gear')

  event.shaped('gnkinetics:worm_gear', [
    'A',
    'B',
    'A'
  ], {
    A: 'create:andesite_alloy',
    B: 'create:gantry_shaft'
  }).id('aquasmp:brass/gnk_worm_gear')
})