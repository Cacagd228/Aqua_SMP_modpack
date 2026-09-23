// Estrogen: Кулдаун 2 секунды (40 тиков) на «использование» эстрогенного патча
ItemEvents.firstRightClicked(event => {
  const { player, item } = event
  if (player.level.isClientSide) return
  if (item.id !== 'estrogen:estrogen_patch') return
  player.addItemCooldown('estrogen:estrogen_patch', 40)
})

// Удаляем крафт эстрогенного патча (только сам предмет, чулки оставляем как украшение)
ServerEvents.recipes(event => {
  event.remove({ output: 'estrogen:estrogen_patch' })
  event.remove({ id: 'estrogen:estrogen_patch' })
  event.remove({ id: 'vanillamode:estrogen_patch' })
  event.remove({ id: 'minecraft:estrogen_patch' })
})