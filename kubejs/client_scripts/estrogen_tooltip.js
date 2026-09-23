// Убираем упоминание падения у чулок - оставляем как украшение
// (основное уже переопределено в kubejs/assets/estrogen/lang/en_us.json + ru_ru.json,
// атрибут Fall Damage Resistance отключен через config/estrogen/server.json5 fallDamageReduction: 0)
// Оставляем пустой хук на случай если тултип всё ещё содержит строку
ItemEvents.modifyTooltips(event => {
  event.modify('estrogen:thigh_highs', tooltip => {
    let toRemove = []
    for (let i = 0; i < tooltip.length; i++) {
      try {
        const txt = tooltip.get(i).getString()
        if (txt.includes('Fall Damage') || txt.includes('fall_damage')) toRemove.push(i)
      } catch (e) {}
    }
    for (let i = toRemove.length - 1; i >= 0; i--) tooltip.remove(toRemove[i])
  })
})
