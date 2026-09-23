// ============================================================
// tier_andesite.js — ВЕК 1: Андезитовый
// ============================================================
// КИНЬ ЭТОТ ФАЙЛ В server_scripts + /reload = открыть век.
// УДАЛИ файл + /reload = закрыть век.
// Век накопительный: следующие века требуют этот файл тоже.
//
// Содержит:
//  А) КАСТОМНЫЕ рецепты (твои, без изменений):
//     hand_crank, frame_andesite, press_t1, saw_t1,
//     water_wheel, chute, depot
//  Б) ВАНИЛЬНЫЕ КОПИИ 1:1 (оригиналы Create, открываются в этом веке):
//     shaft, cogwheel, large_cogwheel, gearbox
//
// НЕ тронуто (остаётся ванильным всегда, не тируется):
//  - create:andesite_alloy, пластины/молот create DG (hammering)
//  - andesite_casing через corps_crafting.js (рамка + обтёсанное бревно)
// ============================================================

ServerEvents.recipes(event => {
  // ---------- А) КАСТОМ (как было) ----------
  event.shaped('create:hand_crank', [
    'A  ',
    'BBB',
    '  C'
  ], {
    A: 'create:iron_sheet',
    B: 'minecraft:oak_planks',
    C: 'create:andesite_alloy'
  }).noMirror().noShrink().id('aquasmp:andesite/hand_crank')

  event.shaped('meowaddons:frame_andesite', [
    'ABA',
    'B B',
    'ABA'
  ], {
    A: 'create:andesite_alloy',
    B: 'create:iron_sheet'
  }).id('aquasmp:andesite/frame_andesite')

  event.shaped('meowaddons:press_t1', [
    'AB',
    'C '
  ], {
    A: 'create:andesite_casing',
    B: 'create:shaft',
    C: 'minecraft:anvil'
  }).id('aquasmp:andesite/press_t1')

  event.shaped('meowaddons:saw_t1', [
    ' A ',
    'ABA',
    ' CD'
  ], {
    A: 'create:iron_sheet',
    B: 'create:andesite_alloy',
    C: 'create:andesite_casing',
    D: 'create:shaft'
  }).id('aquasmp:andesite/saw_t1')

  event.shaped('create:water_wheel', [
    'ABA',
    'BCB',
    'ABA'
  ], {
    A: 'create:iron_sheet',
    B: 'minecraft:oak_planks',
    C: 'create:andesite_casing'
  }).id('aquasmp:andesite/water_wheel')

  event.shaped('create:chute', [
    'A',
    'B',
    'A'
  ], {
    A: 'create:iron_sheet',
    B: 'minecraft:iron_ingot'
  }).id('aquasmp:andesite/chute')

  event.shaped('create:depot', [
    'A',
    'B',
    'C'
  ], {
    A: 'create:andesite_alloy',
    B: 'create:iron_sheet',
    C: 'create:andesite_casing'
  }).id('aquasmp:andesite/depot')

  // ---------- Б) ВАНИЛЬ 1:1 (копии оригиналов Create) ----------
  // Вал — оригинал: 2x andesite_alloy вертикально = 8x shaft
  event.shaped(Item.of('create:shaft', 8), [
    'A',
    'A'
  ], {
    A: 'create:andesite_alloy'
  }).id('aquasmp:andesite/shaft')

  // Шестерня — оригинал: shapeless shaft + доски
  event.shapeless('create:cogwheel', [
    'create:shaft',
    '#minecraft:planks'
  ]).id('aquasmp:andesite/cogwheel')

  // Большая шестерня — оригинал: shapeless shaft + 2x доски
  event.shapeless('create:large_cogwheel', [
    'create:shaft',
    '#minecraft:planks',
    '#minecraft:planks'
  ]).id('aquasmp:andesite/large_cogwheel')

  // Редуктор — оригинал: cogwheel вокруг andesite_casing
  event.shaped('create:gearbox', [
    ' C ',
    'CBC',
    ' C '
  ], {
    B: 'create:andesite_casing',
    C: 'create:cogwheel'
  }).id('aquasmp:andesite/gearbox')
})
