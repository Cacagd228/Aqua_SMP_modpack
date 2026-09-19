// Отключение спавна мобов
EntityEvents.checkSpawn(event => {
  const blockedMobs = [
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
    'scguns:scampler',
    'artifacts:mimic'
  ]

  if (blockedMobs.includes(event.entityType.toString())) {
    event.cancel()
  }
})