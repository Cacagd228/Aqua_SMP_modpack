// ============================================================
// tier_copper.js — ВЕК 3: Медный
// ============================================================
// КИНЬ ЭТОТ ФАЙЛ В server_scripts + /reload = открыть век.
// Требует: 00_tier_lockdown.js + tier_andesite.js + tier_iron.js.
// Без этого файла рецепты века закрыты (удалены локдауном).
//
// Содержит:
//  А) КАСТОМ (как было, без изменений):
//     speedometer, copper_diving_helmet, belt_connector,
//     fluid_tank, fluid_pipe (sequenced), mechanical_drill (дорого!)
//  Б) ВАНИЛЬ 1:1 (оригиналы Create, открываются в этом веке):
//     clutch, gearshift, adjustable_chain_gearshift,
//     mechanical_pump, fluid_valve, cart_assembler,
//     weighted_ejector, linear_chassis,
//     gantry_carriage, gantry_shaft, super_glue,
//     mechanical_harvester, mechanical_plough, mechanical_roller,
//     fluid_pipe (крафт-копия, чтобы труба была доступна)
//
// ПОМЕТКИ:
//  - Буры (mechanical_drill): крафт уже очень дорогой
//    (алмаз + блоки железа) — лимит 10x10 это просто правило,
//    кодом не ограничивается.
//  - adjustable_chain_gearshift и mechanical_roller требуют
//    electron_tube (по ваниле это латунь) — рецепт добавлен как
//    в оригинале, по факту соберётся позже. Так задумано списком.
//  - Всё, что в списке века, но без точного ID (schematicannon,
//    schematic_table, schematics, linked_controller/redstone_link,
//    secondary_linear_chassis, redstone_contact, toolbox, fluid_hatch,
//    copper_casing, двигатели aeronautica/mid-drive/hex) —
//    НЕ выдумывается, остаётся ванилью. См. TIERS.md.
// ============================================================

ServerEvents.recipes(event => {
  // ---------- А) КАСТОМ (как было) ----------
  event.shaped('create:speedometer', [
    ' A ',
    'BCB'
  ], {
    A: 'minecraft:compass',
    B: 'create:andesite_casing',
    C: 'create:gearbox'
  }).id('aquasmp:copper/speedometer')

  event.shaped('create:copper_diving_helmet', [
    'AAA',
    'ABA',
    ' C '
  ], {
    A: 'create:copper_sheet',
    B: 'minecraft:iron_helmet',
    C: 'minecraft:glass_pane'
  }).id('aquasmp:copper/copper_diving_helmet')

  event.shaped('create:belt_connector', [
    'AAA',
    'BAB',
    'AAA'
  ], {
    A: 'minecraft:dried_kelp',
    B: 'minecraft:string'
  }).id('aquasmp:copper/belt_connector')

  event.shaped('create:fluid_tank', [
    ' A ',
    'BCB',
    ' A '
  ], {
    A: 'create:copper_sheet',
    B: 'minecraft:glass_pane',
    C: 'minecraft:barrel'
  }).id('aquasmp:copper/fluid_tank')

  // Как было в исходнике (sequenced для трубы). Не трогаем.
  event.recipes.meowaddons.sequenced_assembly_t1('create:copper_sheet', [
    { type: 'meowaddons:pressing_t1', ingredients: [{ item: 'create:copper_sheet' }], results: [{ id: 'create:copper_sheet' }] },
    { type: 'meowaddons:cutting_t1', ingredients: [{ item: 'create:copper_sheet' }], results: [{ id: 'create:copper_sheet' }] }
  ], 'create:fluid_pipe').set('transitional_item', 'create:copper_sheet').set('loops', 3)

  // Буры — дорого (алмаз + блоки железа). Пометка про 10x10 — правило.
  event.shaped('create:mechanical_drill', [
    ' A ',
    'BBB',
    ' C '
  ], {
    A: 'minecraft:diamond',
    B: 'minecraft:iron_block',
    C: 'create:andesite_casing'
  }).id('aquasmp:copper/mechanical_drill')

  // ---------- Б) ВАНИЛЬ 1:1 ----------
  // Труба (чтобы была доступна крафтом, не только sequenced). Оригинал: 4 шт.
  event.shaped(Item.of('create:fluid_pipe', 4), [
    'SCS'
  ], {
    C: '#c:ingots/copper',
    S: '#c:plates/copper'
  }).id('aquasmp:copper/fluid_pipe')

  // Сцепление (clutch)
  event.shapeless('create:clutch', [
    'create:andesite_casing',
    'create:shaft',
    '#c:dusts/redstone'
  ]).id('aquasmp:copper/clutch')

  // Реверсивная передача (gearshift)
  event.shapeless('create:gearshift', [
    'create:andesite_casing',
    'create:cogwheel',
    '#c:dusts/redstone'
  ]).id('aquasmp:copper/gearshift')

  // Регулируемая цепная коробка (нужен electron_tube — по факту латунь)
  event.shapeless('create:adjustable_chain_gearshift', [
    'create:encased_chain_drive',
    'create:electron_tube'
  ]).id('aquasmp:copper/adjustable_chain_gearshift')

  // Помпа
  event.shapeless('create:mechanical_pump', [
    'create:cogwheel',
    'create:fluid_pipe'
  ]).id('aquasmp:copper/mechanical_pump')

  // Жидкостный вентиль
  event.shapeless('create:fluid_valve', [
    '#c:plates/iron',
    'create:fluid_pipe'
  ]).id('aquasmp:copper/fluid_valve')

  // Вагонеточный сборщик (cart_assembler)
  event.shaped('create:cart_assembler', [
    'CRC',
    'L L'
  ], {
    C: 'create:andesite_alloy',
    R: '#c:dusts/redstone',
    L: '#minecraft:logs'
  }).id('aquasmp:copper/cart_assembler')

  // Весовая катапульта (weighted_ejector)
  event.shaped('create:weighted_ejector', [
    'A',
    'D',
    'I'
  ], {
    A: '#c:plates/gold',
    D: 'create:depot',
    I: 'create:cogwheel'
  }).id('aquasmp:copper/weighted_ejector')

  // Линейное шасси (3 шт)
  event.shaped(Item.of('create:linear_chassis', 3), [
    ' P ',
    'LLL',
    ' P '
  ], {
    P: 'create:andesite_alloy',
    L: '#minecraft:logs'
  }).id('aquasmp:copper/linear_chassis')

  // Каретка линейного привода
  event.shaped('create:gantry_carriage', [
    'B',
    'C',
    'I'
  ], {
    B: '#minecraft:wooden_slabs',
    C: 'create:andesite_casing',
    I: 'create:cogwheel'
  }).id('aquasmp:copper/gantry_carriage')

  // Вал линейного привода (8 шт)
  event.shaped(Item.of('create:gantry_shaft', 8), [
    'A',
    'R',
    'A'
  ], {
    A: 'create:andesite_alloy',
    R: '#c:dusts/redstone'
  }).id('aquasmp:copper/gantry_shaft')

  // Блок-липучка (super_glue)
  event.shaped('create:super_glue', [
    'AS',
    'NA'
  ], {
    A: '#c:slimeballs',
    N: '#c:nuggets/iron',
    S: '#c:plates/iron'
  }).id('aquasmp:copper/super_glue')

  // Комбайн (mechanical_harvester)
  event.shaped('create:mechanical_harvester', [
    'AIA',
    'AIA',
    ' C '
  ], {
    A: 'create:andesite_alloy',
    C: 'create:andesite_casing',
    I: '#c:plates/iron'
  }).id('aquasmp:copper/mechanical_harvester')

  // Механический плуг
  event.shaped('create:mechanical_plough', [
    'III',
    'AAA',
    ' C '
  ], {
    A: 'create:andesite_alloy',
    C: 'create:andesite_casing',
    I: '#c:plates/iron'
  }).id('aquasmp:copper/mechanical_plough')

  // Механический каток (нужен electron_tube + crushing_wheel)
  event.shaped('create:mechanical_roller', [
    'A',
    'C',
    'I'
  ], {
    A: 'create:electron_tube',
    C: 'create:andesite_casing',
    I: 'create:crushing_wheel'
  }).id('aquasmp:copper/mechanical_roller')
})
