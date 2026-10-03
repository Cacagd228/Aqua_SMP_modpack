// ============================================================
// tier_dark.js — ВЕК 6: Тёмная сталь
// ============================================================
// КИНЬ ЭТОТ ФАЙЛ В server_scripts + /reload = открыть век.
// Требует: 00_tier_lockdown.js + век 1–5 (tier_andesite, tier_iron,
//          tier_copper, tier_brass, tier_steel).
// Без этого файла рецепты века закрыты (удалены локдауном).
//
// Источник: harderdiesel-рецепты из main(2).js (Recipe Builder
// от 29.09.2026). Все ID проверены по jar'ам:
//   harderdiesel-1.4.0, createdieselgenerators-1.21.1-1.3.15,
//   createbigcannons-5.11.7, ScorchedGuns-1.5,
//   create-enchantment-industry-2.5.3, create-1.21.1-6.0.10
//
// Логика: 4 «малых» генератора (газ/бензин/дизель/нитро) — базовые.
//   4 «large_» — апгрейд через малый + латунь/тень-сталь.
//   4 «huge_»  — усиленная версия напрямую.
//
// ПОМЕТКИ:
//  - ИСПРАВЛЕННЫЙ ДУБЛЬ: в исходнике рецепт harderdiesel:gas_generator
//    встречался дважды. Второй (self-referential, C: 'harderdiesel:gas_generator')
//    ВЫБРОШЕН — предмет крафтился из самого себя.
//    ВНИМАНИЕ: по структуре (ABA/CDC/EFE) он совпадает с серией huge_*,
//    т.е. вероятно это был опечатанный рецепт harderdiesel:huge_gas_generator
//    (в моде такой блок ЕСТЬ, но рецепта у него не задано).
//    Если нужно — скажи, добавлю huge_gas_generator по этой схеме.
//  - scguns:* (treated_iron / treated_brass / diamond_steel) и
//    createbigcannons:nethersteel_ingot — это не тировые предметы,
//    локдауном не блокируются, но по факту это самая поздняя
//    прогрессия в моде. Проверить, не стоит ли их тоже тировать.
//  - create:shadow_steel — материал ТЁМНОЙ стали, тут и полагается.
//  - Дистилляция (harderdiesel:*_controller, oil_barrel, air_analyzer,
//    cracking_controller, separator_controller, tanks) и
//    createdieselgenerators:distillation_controller — остаются
//    без рецептов, только список. См. TIERS.md.
// ============================================================

ServerEvents.recipes(event => {
  // ---------- БАЗОВЫЕ ГЕНЕРАТОРЫ ----------
  event.shaped('harderdiesel:gasoline_generator', [
    'ABA',
    'CDC',
    'EFE'
  ], {
    A: 'minecraft:flint_and_steel',
    B: 'harderdiesel:low_octane_gasoline_bucket',
    C: 'createdieselgenerators:engine_piston',
    D: 'scguns:treated_iron_block',
    E: 'create:precision_mechanism',
    F: 'create:fluid_tank'
  }).id('aquasmp:dark/gasoline_generator')

  event.shaped('harderdiesel:diesel_generator', [
    'ABA',
    'CDC',
    'EFE'
  ], {
    A: 'minecraft:flint_and_steel',
    B: 'harderdiesel:low_cetane_diesel_bucket',
    C: 'createdieselgenerators:engine_piston',
    D: 'scguns:treated_brass_block',
    E: 'createbigcannons:steel_ingot',
    F: 'create:fluid_tank'
  }).id('aquasmp:dark/diesel_generator')

  event.shaped('harderdiesel:gas_generator', [
    'ABA',
    'CDC',
    'EFE'
  ], {
    A: 'minecraft:flint_and_steel',
    B: 'harderdiesel:propane_bucket',
    C: 'createdieselgenerators:engine_piston',
    D: 'scguns:diamond_steel_block',
    E: 'create:shadow_steel',
    F: 'create:fluid_tank'
  }).id('aquasmp:dark/gas_generator')

  event.shaped('harderdiesel:nitro_generator', [
    'ABA',
    'CDC',
    'EFE'
  ], {
    A: 'minecraft:flint_and_steel',
    B: 'harderdiesel:nitromethane_bucket',
    C: 'createdieselgenerators:engine_piston',
    D: 'create_enchantment_industry:super_experience_block',
    E: 'createbigcannons:nethersteel_ingot',
    F: 'create:fluid_tank'
  }).id('aquasmp:dark/nitro_generator')

  // ---------- LARGE (апгрейд через малый генератор) ----------
  event.shaped('harderdiesel:large_gasoline_generator', [
    ' A ',
    'BCB',
    ' D '
  ], {
    A: 'create:andesite_alloy',
    B: 'create:brass_sheet',
    C: 'harderdiesel:gasoline_generator',
    D: 'minecraft:polished_blackstone_slab'
  }).id('aquasmp:dark/large_gasoline_generator')

  event.shaped('harderdiesel:large_diesel_generator', [
    ' A ',
    'BCB',
    ' D '
  ], {
    A: 'create:andesite_alloy',
    B: 'scguns:treated_brass_ingot',
    C: 'harderdiesel:diesel_generator',
    D: 'minecraft:polished_blackstone_slab'
  }).id('aquasmp:dark/large_diesel_generator')

  event.shaped('harderdiesel:large_gas_generator', [
    ' A ',
    'BCB',
    ' D '
  ], {
    A: 'create:andesite_alloy',
    B: 'create:shadow_steel',
    C: 'harderdiesel:gas_generator',
    D: 'minecraft:polished_blackstone_slab'
  }).id('aquasmp:dark/large_gas_generator')

  event.shaped('harderdiesel:large_nitro_generator', [
    ' A ',
    'BCB',
    ' D '
  ], {
    A: 'create:andesite_alloy',
    B: 'createbigcannons:nethersteel_ingot',
    C: 'harderdiesel:nitro_generator',
    D: 'minecraft:polished_blackstone_slab'
  }).id('aquasmp:dark/large_nitro_generator')

  // ---------- HUGE (усиленные, напрямую) ----------
  event.shaped('harderdiesel:huge_gasoline_generator', [
    'ABA',
    'CDC',
    'EFE'
  ], {
    A: 'create:andesite_alloy',
    B: 'minecraft:flint_and_steel',
    C: 'create:brass_sheet',
    D: 'harderdiesel:gasoline_generator',
    E: 'create:fluid_pipe',
    F: 'scguns:treated_iron_block'
  }).id('aquasmp:dark/huge_gasoline_generator')

  event.shaped('harderdiesel:huge_diesel_generator', [
    'ABA',
    'CDC',
    'EFE'
  ], {
    A: 'create:andesite_alloy',
    B: 'minecraft:flint_and_steel',
    C: 'createbigcannons:steel_ingot',
    D: 'harderdiesel:diesel_generator',
    E: 'create:fluid_pipe',
    F: 'scguns:treated_brass_block'
  }).id('aquasmp:dark/huge_diesel_generator')

  event.shaped('harderdiesel:huge_nitro_generator', [
    'ABA',
    'CDC',
    'EFE'
  ], {
    A: 'create:andesite_alloy',
    B: 'minecraft:flint_and_steel',
    C: 'createbigcannons:nethersteel_ingot',
    D: 'harderdiesel:nitro_generator',
    E: 'create:fluid_pipe',
    F: 'create_enchantment_industry:super_experience_block'
  }).id('aquasmp:dark/huge_nitro_generator')

  // huge_gas_generator намеренно НЕ задан: см. пометку про дубль выше.

  // ---------- НЕФТЯНАЯ ЦЕПОЧКА (перенесено из main.js) ----------
  // ВНИМАНИЕ: oil_scanner требует radiologistics:transparent_screen,
  // а chemical_sprayer / chemical_turret — createcasing:brass_mechanical_pump
  // и create_sa:large_filling_tank. Всё это под баном модов, поэтому эти
  // рецепты фактически недоступны, пока те моды не откроются.
  event.shaped('createdieselgenerators:oil_barrel', [
    'ABA',
    'BCB',
    'ABA'
  ], {
    A: 'create:iron_sheet',
    B: 'create:fluid_tank',
    C: 'create:item_vault'
  }).id('aquasmp:dark/cdg_oil_barrel')

  event.shaped('createdieselgenerators:oil_scanner', [
    'ABA',
    'ACA',
    ' D '
  ], {
    A: 'create:andesite_alloy',
    B: 'radiologistics:transparent_screen',
    C: 'create:precision_mechanism',
    D: 'createdieselgenerators:oil_barrel'
  }).id('aquasmp:dark/cdg_oil_scanner')

  event.shaped('createdieselgenerators:distillation_controller', [
    'ABA',
    'CDC'
  ], {
    A: 'create:fluid_pipe',
    B: 'minecraft:clock',
    C: 'create:iron_sheet',
    D: 'create:precision_mechanism'
  }).id('aquasmp:dark/cdg_distillation_controller')

  event.shaped('harderdiesel:air_analyzer', [
    'ABA',
    'ACA',
    ' D '
  ], {
    A: 'create:brass_sheet',
    B: 'bits_n_bobs:nixie_board',
    C: 'create:precision_mechanism',
    D: 'scguns:air_canister'
  }).id('aquasmp:dark/hd_air_analyzer')

  event.shaped('harderdiesel:galvanized_reactor_tank', [
    'ABA',
    'BCB',
    'ABA'
  ], {
    A: 'createbb:copper_zinc_catalyst',
    B: 'create:zinc_ingot',
    C: 'createdieselgenerators:oil_barrel'
  }).id('aquasmp:dark/hd_galvanized_reactor_tank')

  event.shaped('harderdiesel:wear_resistant_tank', [
    'ABA',
    'ACA',
    'ABA'
  ], {
    A: 'create:sturdy_sheet',
    B: 'create:shadow_steel',
    C: 'harderdiesel:galvanized_reactor_tank'
  }).id('aquasmp:dark/hd_wear_resistant_tank')

  event.shaped('harderdiesel:cracking_controller', [
    'ABA',
    'CDC',
    'ACA'
  ], {
    A: 'create:brass_sheet',
    B: 'create:precision_mechanism',
    C: 'scguns:treated_brass_ingot',
    D: 'createdieselgenerators:distillation_controller'
  }).id('aquasmp:dark/hd_cracking_controller')

  event.shaped('harderdiesel:separator_controller', [
    'ABA',
    'ACA',
    'ABA'
  ], {
    A: 'create:sturdy_sheet',
    B: 'create:shadow_steel',
    C: 'harderdiesel:cracking_controller'
  }).id('aquasmp:dark/hd_separator_controller')

  event.recipes.create.mechanical_crafting('createdieselgenerators:chemical_sprayer', [
    ' A   ',
    'BCDEF',
    ' AG  '
  ], {
    A: 'createdieselgenerators:kelp_handle',
    B: 'create_sa:large_filling_tank',
    C: 'create:precision_mechanism',
    D: 'minecraft:copper_block',
    E: 'createcasing:brass_mechanical_pump',
    F: 'create:smart_fluid_pipe',
    G: 'create:copper_valve_handle'
  }).id('aquasmp:dark/cdg_chemical_sprayer')

  event.recipes.create.deploying(
    'createdieselgenerators:chemical_sprayer_lighter',
    ['createdieselgenerators:chemical_sprayer', 'createdieselgenerators:lighter']
  ).id('aquasmp:dark/cdg_chemical_sprayer_lighter')

  event.recipes.create.mechanical_crafting('createdieselgenerators:chemical_turret', [
    'ABC',
    ' D ',
    'EFE',
    'GHG'
  ], {
    A: 'create_sa:large_filling_tank',
    B: 'create:copper_sheet',
    C: 'createdieselgenerators:chemical_sprayer',
    D: 'create:cogwheel',
    E: 'create:shaft',
    F: 'minecraft:copper_block',
    G: 'create:copper_casing',
    H: 'create:precision_mechanism'
  }).id('aquasmp:dark/cdg_chemical_turret')
})