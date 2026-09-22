const FRAME_TO_CASING = {
  'meowaddons:frame_andesite': 'create:andesite_casing',
  'meowaddons:frame_latun': 'create:brass_casing',
  'meowaddons:frame_steel': 'create:shadow_steel_casing',
  'meowaddons:frame_shadow_steel': 'create:shadow_steel_casing',
  'meowaddons:frame_refined_radiance': 'create:refined_radiance_casing',
  'meowaddons:frame_chromatic_compound': 'create:railway_casing'
}

const STRIPPED_LOGS_TAG = 'c:stripped_logs'

PlayerEvents.rightClickBlock(event => {
  const { player, level, hand, item, block } = event
  if (hand != 'main_hand') return

  const held = item.id
  const casingId = FRAME_TO_CASING[held]
  if (!casingId) return

  if (!block.hasTag(STRIPPED_LOGS_TAG)) return

  if (!level.isClientSide) {
    item.count--
    if (item.count <= 0) player.setItemInHand(hand, ItemStack.EMPTY)

    const casing = Item.of(casingId)
    if (!player.addItem(casing)) {
      player.drop(casing, false)
    }

    level.playSound(null, player.blockPosition(), 'minecraft:entity.item.pickup', 'players', 0.5, 1.0)
  }

  event.cancel()
})