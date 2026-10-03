// ============================================================
// tier_chromatic.js — ВЕК 8 (ФИНАЛ): Хроматическое соединение
// ============================================================
// КИНЬ ЭТОТ ФАЙЛ В server_scripts + /reload = открыть век.
// Требует: 00_tier_lockdown.js + век 1–7.
// Без этого файла рецепты века закрыты (удалены локдауном).
//
// Источник: рецепты из main.js (Recipe Builder, 30.09.2026),
// перенесены сюда из main.js, потому что event.remove() в локдауне
// НЕ трогает рецепты, добавленные скриптами. Пока файл не закинут —
// все эти предметы недоступны.
//
// Содержит:
//  А) COMPUTERCRAFT (16 рецептов) — вся линейка CC.
//  Б) CREATE: STUFF & ADDITIONS (19 рецептов) — джетпаки, экзоскелеты,
//     баки, дроны, фламет.thrower, буры.
//
// ⚠️ ВАЖНО: эти рецепты по своей природе зависят от ЗАБАНЕННЫХ модов:
//    powergrid (golden_wire, integrated_circuit, device_connector,
//    insulated_copper_wire, resistive_coil, copper_coil, magnet),
//    radiologistics (transparent_screen, antenna, main_computer,
//    memory_module, radio_transmitter, gyroscope_sensor),
//    create_radar (monitor), createcasing (brass_mechanical_drill,
//    brass_mechanical_pump, *encased_fan), createpropulsion
//    (platinum_sheet), create_connected, create_sa (medium_*_tank),
//    createdieselgenerators (kelp_handle, lighter).
//    Т.е. даже после открытия века 8 эти рецепты не соберутся,
//    пока те моды не будут разбанены (см. BANNED_MODS в
//    00_tier_lockdown.js). Это осознанно: предметы ComputerCraft
//    и Create S&A недосягаемы до явного решения.
// ============================================================

ServerEvents.recipes(event => {
  // ---------- А) COMPUTERCRAFT ----------
    event.shaped('6xcomputercraft:cable', [
    'AAA',
    'BBB',
    'AAA'
    ], {
    A: 'create:brass_sheet',
    B: 'powergrid:insulated_copper_wire'
    }).id('aquasmp:chromatic/cable_1')
    event.shaped('computercraft:computer_advanced', [
    'ABA',
    'CDC',
    'EFE'
    ], {
    A: 'create:shadow_steel',
    B: 'create:precision_mechanism',
    C: 'powergrid:integrated_circuit',
    D: 'computercraft:computer_normal',
    E: 'scguns:diamond_steel_ingot',
    F: 'scguns:treated_brass_ingot'
    }).id('aquasmp:chromatic/computer_advanced_2')
    event.shaped('computercraft:computer_normal', [
    'ABA',
    'CDC',
    'ECE'
    ], {
    A: 'powergrid:golden_wire',
    B: 'create:precision_mechanism',
    C: 'powergrid:integrated_circuit',
    D: 'radiologistics:main_computer',
    E: 'scguns:treated_brass_ingot'
    }).id('aquasmp:chromatic/computer_normal_3')
    event.shaped('computercraft:disk_drive', [
    'ABA',
    'CDC',
    'EFE'
    ], {
    A: 'powergrid:golden_wire',
    B: 'scguns:needle',
    C: 'powergrid:integrated_circuit',
    D: 'scguns:copper_disc',
    E: 'create:copper_casing',
    F: 'create:mechanical_bearing'
    }).id('aquasmp:chromatic/disk_drive_4')
    event.shaped('computercraft:monitor_advanced', [
    'ABA',
    'CDC',
    'EFE'
    ], {
    A: 'create:precision_mechanism',
    B: 'create:shadow_steel',
    C: 'scguns:diamond_steel_ingot',
    D: 'computercraft:monitor_normal',
    E: 'powergrid:integrated_circuit',
    F: 'bits_n_bobs:large_nixie_tube'
    }).id('aquasmp:chromatic/monitor_advanced_5')
    event.shaped('computercraft:monitor_normal', [
    'ABA',
    'CDC',
    'AEA'
    ], {
    A: 'scguns:treated_brass_ingot',
    B: 'create:precision_mechanism',
    C: 'powergrid:golden_wire',
    D: 'radiologistics:transparent_screen',
    E: 'create_radar:monitor'
    }).id('aquasmp:chromatic/monitor_normal_6')
    event.shaped('computercraft:pocket_computer_advanced', [
    'ABC',
    'ADE',
    ' F '
    ], {
    A: 'createpropulsion:platinum_sheet',
    B: 'create:shadow_steel',
    C: 'radiologistics:antenna',
    D: 'computercraft:computer_advanced',
    E: 'aeroworks:button_keypad_module',
    F: 'createdieselgenerators:kelp_handle'
    }).id('aquasmp:chromatic/pocket_computer_advanced_7')
    event.shaped('computercraft:pocket_computer_normal', [
    'ABC',
    'ADE',
    ' F '
    ], {
    A: 'create:brass_sheet',
    B: 'scguns:diamond_steel_ingot',
    C: 'radiologistics:antenna',
    D: 'computercraft:computer_normal',
    E: 'aeroworks:button_keypad_module',
    F: 'createdieselgenerators:kelp_handle'
    }).id('aquasmp:chromatic/pocket_computer_normal_8')
    event.shaped('computercraft:printer', [
    'ABA',
    'CDC',
    'EFE'
    ], {
    A: 'powergrid:golden_wire',
    B: 'minecraft:piston',
    C: 'powergrid:integrated_circuit',
    D: 'colorful_catalysts:fan_dyeing_black_catalyst',
    E: 'create:copper_casing',
    F: 'minecraft:paper'
    }).id('aquasmp:chromatic/printer_9')
    event.shaped('computercraft:redstone_relay', [
    'ABA',
    'BCB',
    'ABA'
    ], {
    A: 'create:copper_casing',
    B: 'minecraft:redstone',
    C: 'computercraft:wired_modem'
    }).id('aquasmp:chromatic/redstone_relay_10')
    event.shaped('computercraft:speaker', [
    'ABA',
    'CDC',
    'EBE'
    ], {
    A: 'create:copper_casing',
    B: 'powergrid:integrated_circuit',
    C: 'create:brass_ingot',
    D: 'supplementaries:speaker_block',
    E: 'powergrid:golden_wire'
    }).id('aquasmp:chromatic/speaker_11')
    event.shaped('computercraft:turtle_advanced', [
    'ABA',
    'CDC',
    'AEA'
    ], {
    A: 'scguns:diamond_steel_ingot',
    B: 'create:shadow_steel',
    C: 'powergrid:golden_wire',
    D: 'computercraft:turtle_normal',
    E: 'powergrid:integrated_circuit'
    }).id('aquasmp:chromatic/turtle_advanced_12')
    event.shaped('computercraft:turtle_normal', [
    'ABA',
    'CDC',
    'EFE'
    ], {
    A: 'scguns:treated_brass_ingot',
    B: 'create:precision_mechanism',
    C: 'powergrid:integrated_circuit',
    D: 'computercraft:computer_normal',
    E: 'radiologistics:memory_module',
    F: 'powergrid:golden_wire'
    }).id('aquasmp:chromatic/turtle_normal_13')
    event.shaped('computercraft:wired_modem', [
    'ABA',
    'CDC',
    ' A '
    ], {
    A: 'powergrid:golden_wire',
    B: 'powergrid:device_connector',
    C: 'powergrid:integrated_circuit',
    D: 'create:display_link'
    }).id('aquasmp:chromatic/wired_modem_14')
    event.shaped('computercraft:wireless_modem_advanced', [
    'ABA',
    'CDC',
    'EFE'
    ], {
    A: 'minecraft:ender_eye',
    B: 'create:shadow_steel',
    C: 'create:precision_mechanism',
    D: 'computercraft:wireless_modem_normal',
    E: 'scguns:diamond_steel_ingot',
    F: 'powergrid:golden_wire'
    }).id('aquasmp:chromatic/wireless_modem_advanced_15')
    event.shaped('computercraft:wireless_modem_normal', [
    ' A ',
    'BCB',
    ' D '
    ], {
    A: 'radiologistics:antenna',
    B: 'powergrid:integrated_circuit',
    C: 'computercraft:wired_modem',
    D: 'radiologistics:radio_transmitter'
    }).id('aquasmp:chromatic/wireless_modem_normal_16')
  // ---------- Б) CREATE: STUFF & ADDITIONS ----------
    event.recipes.create.mechanical_crafting('create_sa:andesite_exoskeleton_chestplate', [
    'ABCBA',
    'DAEAD',
    'FGDGF'
    ], {
    A: 'create:andesite_alloy',
    B: 'create:shaft',
    C: 'simulated:gyroscopic_mechanism',
    D: 'createbb:copper_zinc_catalyst',
    E: 'create_sa:andesite_jetpack_chestplate',
    F: 'create:andesite_alloy_block',
    G: 'create_sa:heat_engine'
    }).id('aquasmp:chromatic/andesite_exoskeleton_chestplate_1')
    event.recipes.create.mechanical_crafting('create_sa:andesite_jetpack_chestplate', [
    'ABCBA',
    'DBEBD',
    'DFGFD'
    ], {
    A: 'create:cogwheel',
    B: 'scguns:treated_iron_ingot',
    C: 'scguns:empty_tank',
    D: 'createcasing:brass_encased_fan',
    E: 'create:precision_mechanism',
    F: 'create_sa:heat_engine',
    G: 'create:sturdy_sheet'
    }).id('aquasmp:chromatic/andesite_jetpack_chestplate_2')
    event.shaped('create_sa:block_picker', [
    'ABA',
    'CDC',
    'EFE'
    ], {
    A: 'create:copper_sheet',
    B: 'create_sa:medium_filling_tank',
    C: 'createbb:copper_zinc_catalyst',
    D: 'create_sa:hydraulic_engine',
    E: 'powergrid:copper_coil',
    F: 'create_sa:copper_magnet'
    }).id('aquasmp:chromatic/block_picker_3')
    event.shaped('create_sa:brass_cube', [
    'ABA',
    'BCB',
    'ABA'
    ], {
    A: 'create:brass_ingot',
    B: 'scguns:treated_brass_ingot',
    C: 'createbigcannons:nethersteel_block'
    }).id('aquasmp:chromatic/brass_cube_4')
    event.shaped('create_sa:brass_drill_head', [
    'ABA',
    'CDC',
    'ACA'
    ], {
    A: 'create:brass_ingot',
    B: 'create:precision_mechanism',
    C: 'scguns:treated_brass_ingot',
    D: 'createcasing:brass_mechanical_drill'
    }).id('aquasmp:chromatic/brass_drill_head_5')
    event.shaped('create_sa:brass_drone_item', [
    'ABA',
    'BCB',
    'ABA'
    ], {
    A: 'create:propeller',
    B: 'scguns:treated_brass_ingot',
    C: 'create:precision_mechanism'
    }).id('aquasmp:chromatic/brass_drone_item_6')
    event.recipes.create.mechanical_crafting('create_sa:brass_exoskeleton_chestplate', [
    'ABCBA',
    'DAEAD',
    'FGHGF'
    ], {
    A: 'scguns:treated_brass_ingot',
    B: 'scguns:treated_brass_lamp',
    C: 'aeroworks:gyroscope',
    D: 'createbb:copper_zinc_catalyst',
    E: 'create_sa:brass_jetpack_chestplate',
    F: 'scguns:treated_brass_plates',
    G: 'create_sa:steam_engine',
    H: 'scguns:treated_brass_gun_frame'
    }).id('aquasmp:chromatic/brass_exoskeleton_chestplate_7')
    event.recipes.create.mechanical_crafting('create_sa:brass_jetpack_chestplate', [
    'ABCBA',
    'ABDBA',
    ' EBE '
    ], {
    A: 'createcasing:copper_encased_fan',
    B: 'scguns:treated_brass_ingot',
    C: 'scguns:empty_tank',
    D: 'create:precision_mechanism',
    E: 'create_sa:steam_engine'
    }).id('aquasmp:chromatic/brass_jetpack_chestplate_8')
    event.recipes.create.mechanical_crafting('create_sa:copper_exoskeleton_chestplate', [
    'ABCBA',
    'ADEDA',
    'FGHGF'
    ], {
    A: 'createbb:copper_zinc_catalyst',
    B: 'create_sa:large_filling_tank',
    C: 'radiologistics:gyroscope_sensor',
    D: 'scguns:copper_gun_frame',
    E: 'create_sa:copper_jetpack_chestplate',
    F: 'minecraft:copper_block',
    G: 'create_sa:hydraulic_engine',
    H: 'create:electron_tube'
    }).id('aquasmp:chromatic/copper_exoskeleton_chestplate_9')
    event.recipes.create.mechanical_crafting('create_sa:copper_jetpack_chestplate', [
    'ABCBA',
    'ABDBA',
    ' EFE '
    ], {
    A: 'create:encased_fan',
    B: 'createbb:copper_zinc_catalyst',
    C: 'scguns:empty_tank',
    D: 'create:precision_mechanism',
    E: 'create_sa:hydraulic_engine',
    F: 'create:copper_sheet'
    }).id('aquasmp:chromatic/copper_jetpack_chestplate_10')
    event.shaped('create_sa:copper_magnet', [
    'ABA',
    'BCB',
    'ADA'
    ], {
    A: 'minecraft:copper_ingot',
    B: 'createbb:copper_zinc_catalyst',
    C: 'minecraft:netherite_ingot',
    D: 'powergrid:magnet'
    }).id('aquasmp:chromatic/copper_magnet_11')
    event.shaped('create_sa:drone_controller', [
    ' A ',
    'ABA',
    ' C '
    ], {
    A: 'scguns:treated_brass_ingot',
    B: 'aeroworks:joystick',
    C: 'create:precision_mechanism'
    }).id('aquasmp:chromatic/drone_controller_12')
    event.shaped('create_sa:fan_component', [
    'ABA',
    'CDC',
    'ACA'
    ], {
    A: 'createbb:copper_zinc_catalyst',
    B: 'create:precision_mechanism',
    C: 'create:andesite_alloy',
    D: 'create:propeller'
    }).id('aquasmp:chromatic/fan_component_13')
    event.recipes.create.mechanical_crafting('create_sa:flamethrower', [
    'ABCDDD',
    'EEFG  '
    ], {
    A: 'create_sa:large_filling_tank',
    B: 'createdieselgenerators:burner',
    C: 'create_sa:heat_engine',
    D: 'create:sturdy_sheet',
    E: 'create:andesite_alloy',
    F: 'scguns:treated_brass_ingot',
    G: 'createdieselgenerators:lighter'
    }).id('aquasmp:chromatic/flamethrower_14')
    event.shaped('create_sa:large_filling_tank', [
    'ABA',
    'ACA',
    'DDD'
    ], {
    A: 'create:copper_sheet',
    B: 'minecraft:copper_block',
    C: 'create_sa:medium_filling_tank',
    D: 'createbb:copper_zinc_catalyst'
    }).id('aquasmp:chromatic/large_filling_tank_15')
    event.shaped('create_sa:large_fueling_tank', [
    'ABA',
    'ACA',
    'ABA'
    ], {
    A: 'create:sturdy_sheet',
    B: 'create:shadow_steel',
    C: 'create_sa:medium_fueling_tank'
    }).id('aquasmp:chromatic/large_fueling_tank_16')
    event.smithing('create_sa:netherite_exoskeleton_chestplate', 'create_dragons_plus:blaze_upgrade_smithing_template', 'create_sa:brass_exoskeleton_chestplate', 'minecraft:netherite_ingot').id('aquasmp:chromatic/netherite_exoskeleton_chestplate_17')
    event.smithing('create_sa:netherite_jetpack_chestplate', 'create_dragons_plus:blaze_upgrade_smithing_template', 'create_sa:brass_jetpack_chestplate', 'minecraft:netherite_ingot').id('aquasmp:chromatic/netherite_jetpack_chestplate_18')
    event.shaped('create_sa:portable_drill', [
    ' A ',
    'BCB',
    'DED'
    ], {
    A: 'scguns:treated_brass_ingot',
    B: 'create:cogwheel',
    C: 'create_sa:steam_engine',
    D: 'create:brass_ingot',
    E: 'create_sa:brass_drill_head'
    }).id('aquasmp:chromatic/portable_drill_19')})
