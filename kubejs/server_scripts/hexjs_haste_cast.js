// server_scripts/hexjs_haste_cast.js — обработчики кастомных узоров.
//
// 1) "Haste Sigil" (awe) — Haste II на 30с с сущностью на вершине стека.
//    Стоимость: e.tryConsumeMedia(N), где N в 1/10000 пыли (10000 = 1 аметист).
//    При нехватке аметиста Hex Casting сам наносит overcast-урон кастеру.
//    Поставь 0/убери вызов, если хочешь бесплатный узор.
// 2) "HexJS Selftest" (weswe) — ничего не требует: просто подтверждает, что кастомный
//    узор зарезолвился, событие сработало и finish() принят. Для /hexjs_selftest и ручного каста.
//
// Примечание про эффекты: в KubeJS 2101 нет глобальных MobEffects/MobEffectInstance,
// holder эффекта берётся через Registry.of('minecraft:mob_effect').get('minecraft:haste').

HexcastingEvents.registeredPatternCastEvent(e => {
  if (e.getPattern().anglesSignature() !== "awe") return
  let stack = e.getStack()
  // Если на стеке есть сущность — спешка её; иначе спешка на кастера.
  // Так свиток/узор работают без предварительной подготовки стека.
  let entity = null
  if (stack.length >= 1 && (stack[stack.length - 1] instanceof EntityIota)) {
    entity = stack.pop().getEntity()
  }
  if (!entity) {
    entity = e.getCaster()
  }
  console.log("[HexJS] Haste triggered for " + e.getCaster().getName().getString())
  try {
    let haste = Registry.of('minecraft:mob_effect').get('minecraft:haste')
    entity.potionEffects.add(haste, 600, 1, false, false)
    e.printMessage("§a[HexJS] Спешка II на " + entity.getName().getString())
    console.log("[HexJS] Applied Haste II to " + entity.getName().getString())
  } catch (err) {
    console.error("[HexJS] Haste error: " + err)
    e.scheduleMishap("Ошибка наложения эффекта: " + err)
    e.finish(); return
  }
  // 1 аметист = 10000 единиц; спишется из пыли/меди игрока, при нехватке — overcast-урон.
  e.tryConsumeMedia(10000)
  e.finish()
})

// Самопроверка: доказывает, что цепочка «резолв узора → special handler → событие → finish()» работает.
HexcastingEvents.registeredPatternCastEvent(e => {
  if (e.getPattern().anglesSignature() !== "weswe") return
  console.log("[HexJS] SELFTEST fired: custom pattern resolved, event ran")
  e.printMessage("§a[HexJS] Self-test OK — кастомный узор выполнен!")
  let stack = e.getStack()
  stack.push(new DoubleIota(42069))
  e.setStack(stack)
  e.finish()
})