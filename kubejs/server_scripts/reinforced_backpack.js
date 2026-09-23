// Кастомный крафт стандартного рюкзака (Sophisticated Backpacks)
// Нитка - Укрепленная кожа - Нитка
// Нитка - Бочка - Нитка
// Укрепленная кожа - Укрепленная кожа - Укрепленная кожа
ServerEvents.recipes(event => {
  // Убираем оригинальный крафт рюкзака (по id, чтобы не задеть свой shaped-рецепт)
  event.remove({ id: 'sophisticatedbackpacks:backpack' })
  // Убираем ванильные крафты медного и железного рюкзаков по id
  // (output-удаление ломало бы наш create:sequenced_assembly на copper_backpack)
  event.remove({ id: 'sophisticatedbackpacks:copper_backpack' })
  event.remove({ id: 'sophisticatedbackpacks:iron_backpack' })
  // Вторая ванильная запись на железный рюкзак (апгрейд из медного)
  event.remove({ id: 'sophisticatedbackpacks:iron_backpack_from_copper' })
  // Отключаем крафты улучшений: наковальня, мультинструмент и продвинутый мультинструмент
  event.remove({ id: 'sophisticatedbackpacks:anvil_upgrade' })
  event.remove({ id: 'sophisticatedbackpacks:tool_swapper_upgrade' })
  event.remove({ id: 'sophisticatedbackpacks:advanced_tool_swapper_upgrade' })
  // Автокормежка, алхимия, все переполнения (stack)
  event.remove({ id: 'sophisticatedbackpacks:feeding_upgrade' })
  event.remove({ id: 'sophisticatedbackpacks:advanced_feeding_upgrade' })
  event.remove({ id: 'sophisticatedbackpacks:alchemy_upgrade' })
  event.remove({ id: 'sophisticatedbackpacks:advanced_alchemy_upgrade' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_starter_tier' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_1' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_2' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_3' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_4' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_omega_tier' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_1_from_starter' })
  event.remove({ id: 'sophisticatedbackpacks:stack_downgrade_tier_1' })
  event.remove({ id: 'sophisticatedbackpacks:stack_downgrade_tier_2' })
  event.remove({ id: 'sophisticatedbackpacks:stack_downgrade_tier_3' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_starter_tier_to_tier_1_conversion' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_starter_tier_to_tier_2_conversion' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_starter_tier_to_tier_3_conversion' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_starter_tier_to_tier_4_conversion' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_1_to_tier_2_conversion' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_1_to_tier_3_conversion' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_1_to_tier_4_conversion' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_2_to_tier_3_conversion' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_2_to_tier_4_conversion' })
  event.remove({ id: 'sophisticatedbackpacks:stack_upgrade_tier_3_to_tier_4_conversion' })

  event.shaped('sophisticatedbackpacks:backpack', [
    'SRS',
    'SBS',
    'RRR'
  ], {
    S: 'minecraft:string',
    R: 'meowaddons:pressed_reinforced_leather',
    B: 'minecraft:barrel'
  })
})