// ============================================================
// tier_iron.js — ВЕК 2: Железный
// ============================================================
// КИНЬ ЭТОТ ФАЙЛ В server_scripts + /reload = открыть век.
// Требует: 00_tier_lockdown.js + tier_andesite.js (накопление).
// Без этого файла рецепты века закрыты (удалены локдауном).
//
// Содержит:
//  А) КАСТОМ (как было, дубликат large_water_wheel убран):
//     large_water_wheel, millstone_t1, basin, mixer_t1, windmill_bearing
//  Б) ВАНИЛЬ 1:1 (оригиналы Create, открываются в этом веке):
//     encased_fan, vertical_gearbox, encased_chain_drive,
//     mechanical_piston, sticky_mechanical_piston, piston_extension_pole,
//     mechanical_bearing, white_sail, sail_frame,
//     portable_storage_interface, radial_chassis, whisk
//
// TODO (нужен точный ID, пока НЕ тируется, остаётся ванилью):
//  - "I2 двигатель" — предположительно createdieselgenerators:diesel_engine,
//    уточнить у автора списка.
// ============================================================

ServerEvents.recipes(event => {
  // ---------- А) КАСТОМ ----------
  event.shaped('create:large_water_wheel', [
    'ABA',
    'BCB',
    'ABA'
  ], {
    A: 'create:iron_sheet',
    B: 'minecraft:oak_planks',
    C: 'create:water_wheel'
  }).id('aquasmp:iron/large_water_wheel')

  event.shaped('meowaddons:millstone_t1', [
    'A',
    'B',
    'C'
  ], {
    A: 'create:large_cogwheel',
    B: 'create:andesite_casing',
    C: 'minecraft:smooth_stone'
  }).id('aquasmp:iron/millstone_t1')

  event.shaped('create:basin', [
    'ABA',
    'ACA'
  ], {
    A: 'create:andesite_alloy',
    B: 'minecraft:cauldron',
    C: 'create:iron_sheet'
  }).id('aquasmp:iron/basin')

  event.shaped('meowaddons:mixer_t1', [
    'A',
    'B',
    'C'
  ], {
    A: 'create:large_cogwheel',
    B: 'create:andesite_casing',
    C: 'create:whisk'
  }).id('aquasmp:iron/mixer_t1')

  event.shaped('create:windmill_bearing', [
    'ABA',
    'CDC',
    ' E '
  ], {
    A: 'create:vertical_gearbox',
    B: 'minecraft:sticky_piston',
    C: 'create:gearbox',
    D: 'create:andesite_casing',
    E: 'create:shaft'
  }).id('aquasmp:iron/windmill_bearing')

  // ---------- Б) ВАНИЛЬ 1:1 ----------
  // Вентилятор (encased_fan): shaft / casing / propeller
  event.shaped('create:encased_fan', [
    'S',
    'A',
    'P'
  ], {
    S: 'create:shaft',
    A: 'create:andesite_casing',
    P: 'create:propeller'
  }).id('aquasmp:iron/encased_fan')

  // Угловая передача (vertical_gearbox)
  event.shaped('create:vertical_gearbox', [
    'C C',
    ' B ',
    'C C'
  ], {
    B: 'create:andesite_casing',
    C: 'create:cogwheel'
  }).id('aquasmp:iron/vertical_gearbox')

  // Цепной привод в корпусе (encased_chain_drive)
  event.shapeless('create:encased_chain_drive', [
    'create:andesite_casing',
    '#c:nuggets/iron',
    '#c:nuggets/iron',
    '#c:nuggets/iron'
  ]).id('aquasmp:iron/encased_chain_drive')

  // Удлинитель поршня (даёт 8 шт) — нужен для поршня, поэтому первым
  event.shaped(Item.of('create:piston_extension_pole', 8), [
    'P',
    'A',
    'P'
  ], {
    P: '#minecraft:planks',
    A: 'create:andesite_alloy'
  }).id('aquasmp:iron/piston_extension_pole')

  // Механический поршень
  event.shaped('create:mechanical_piston', [
    'B',
    'C',
    'I'
  ], {
    B: '#minecraft:wooden_slabs',
    C: 'create:andesite_casing',
    I: 'create:piston_extension_pole'
  }).id('aquasmp:iron/mechanical_piston')

  // Липкий поршень
  event.shaped('create:sticky_mechanical_piston', [
    'S',
    'P'
  ], {
    S: '#c:slimeballs',
    P: 'create:mechanical_piston'
  }).id('aquasmp:iron/sticky_mechanical_piston')

  // Механический вращатель (mechanical_bearing)
  event.shaped('create:mechanical_bearing', [
    'B',
    'C',
    'I'
  ], {
    B: '#minecraft:wooden_slabs',
    C: 'create:andesite_casing',
    I: 'create:shaft'
  }).id('aquasmp:iron/mechanical_bearing')

  // Парус (даёт 2 шт)
  event.shaped(Item.of('create:white_sail', 2), [
    'WS',
    'SA'
  ], {
    W: '#minecraft:wool',
    S: '#c:rods/wooden',
    A: 'create:andesite_alloy'
  }).id('aquasmp:iron/white_sail')

  // Рама паруса (конверсия из паруса)
  event.shapeless('create:sail_frame', [
    'create:white_sail'
  ]).id('aquasmp:iron/sail_frame')

  // Портативный складской интерфейс
  event.shapeless('create:portable_storage_interface', [
    'create:andesite_casing',
    'create:chute'
  ]).id('aquasmp:iron/portable_storage_interface')

  // Радиальное шасси (даёт 3 шт)
  event.shaped(Item.of('create:radial_chassis', 3), [
    ' L ',
    'PLP',
    ' L '
  ], {
    L: '#minecraft:logs',
    P: 'create:andesite_alloy'
  }).id('aquasmp:iron/radial_chassis')

  // Венчик (whisk) — нужен для mixer_t1
  event.shaped('create:whisk', [
    ' C ',
    'SCS',
    'SSS'
  ], {
    C: 'create:andesite_alloy',
    S: '#c:plates/iron'
  }).id('aquasmp:iron/whisk')
})
