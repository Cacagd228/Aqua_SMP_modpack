// ============================================================
// remove_vanilla_casings.js — выпил ВАНИЛЬНЫХ крафтов корпусов Create
// ============================================================
// Ваниль Create 6 крафтит корпуса через create:item_application:
//   андезитовый корпус = обтёсанное бревно + create:andesite_alloy (сплав)
//   латунный корпус    = обтёсанное бревно + латунный слиток
//   медный корпус      = обтёсанное бревно + медный слиток
//   рельсовый корпус   = латунный корпус + обсидановая пластина
// Они обходят систему рамок meowaddons, поэтому удаляются.
// Свой крафт корпусов (рамка + бревно) живёт отдельно: corps_crafting.js.
// Удалить этот файл + /reload = вернуть ванильные крафты корпусов.

ServerEvents.recipes(event => {
  const VANILLA_CASINGS = [
    // андезитовый корпус из сплава (бревно / доска)
    'create:item_application/andesite_casing_from_log',
    'create:item_application/andesite_casing_from_wood',
    // латунный корпус из латуни
    'create:item_application/brass_casing_from_log',
    'create:item_application/brass_casing_from_wood',
    // рельсовый корпус (латунный корпус + обсидановая пластина)
    // рамка meowaddons:frame_steel его заменяет
    'create:item_application/railway_casing'
    // медный корпус (copper_casing_from_log / copper_casing_from_wood) НЕ удаляем — доступен в Медном веке
  ]

  for (const id of VANILLA_CASINGS) {
    event.remove({ id: id })
  }
})
