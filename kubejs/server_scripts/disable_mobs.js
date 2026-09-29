// Отключение спавна мобов
const BLOCKED_MOBS = new Set([
  'estrogen:moth',
  'scguns:basic_turret',
  'scguns:cog_minion',
  'scguns:cog_knight',
  'scguns:sky_carrier',
  'scguns:hive',
  'scguns:swarm',
  'scguns:redcoat',
  'scguns:supply_scamp',
  'scguns:dissident',
  'scguns:hornlin',
  'scguns:zombified_hornlin',
  'scguns:the_merchant',
  'scguns:blunderer',
  'scguns:adjudicator',
  'scguns:subjugator',
  'scguns:praetor',
  'scguns:mother_ghast',
  'scguns:viventrum',
  'scguns:sulfurhead',
  'scguns:trauma_unit',
  'scguns:scamp_tank',
  'scguns:signal_beacon',
  'scguns:sampler',
  'artifacts:mimic'
])

function typeId(...values) {
  for (const value of values) {
    if (!value) continue
    if (typeof value === 'string') {
      if (value.includes(':')) return value
      continue
    }
    if (typeof value.location === 'function') {
      try {
        const key = value.location()
        if (key) return key.toString()
      } catch (e) {}
    }
    if (typeof value.unwrapKey === 'function') {
      const unwrapped = value.unwrapKey()
      if (unwrapped && unwrapped[0] && unwrapped[1]) {
        return unwrapped[1] + ':' + unwrapped[0]
      }
    }
    if (typeof value.getKey === 'function') {
      try {
        const key = value.getKey(location)
        if (key) return key.toString()
      } catch (e) {}
    }
    if (value.id) return String(value.id)
  }
  return ''
}

function entityTypeId(event) {
  return typeId(event.type, event.entityType, event.entity)
}

EntityEvents.checkSpawn(event => {
  if (BLOCKED_MOBS.has(entityTypeId(event))) {
    event.cancel('disabled in Aqua SMP')
  }
})

ForgeEvents.onEvent(
  'net.neoforged.neoforge.event.entity.EntityJoinLevelEvent',
  event => {
    if (BLOCKED_MOBS.has(entityTypeId({ entity: event.getEntity() }))) {
      event.setCanceled(true)
    }
  }
)

ServerEvents.highPriorityTick(event => {
  if (event.tickCount === 20) {
    console.log('[Aqua SMP] Заблокировано мобов: ' + BLOCKED_MOBS.size)
  }
})
