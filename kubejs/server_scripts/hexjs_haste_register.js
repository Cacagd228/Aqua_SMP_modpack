// server_scripts/hexjs_haste_register.js — регистрация кастомных узоров.
// Регистрируем ДВАЖДЫ: сразу при загрузке скрипта (топ-уровень, самый надёжный момент)
// и на ServerEvents.loaded. Повторная регистрация той же сигнатуры безвредна (перезапись).
// Если какая-то сигнатура совпадёт со встроенным узором — в логах появится WARN о коллизии.
function registerPatterns() {
  Hexcasting.registerCustomPattern("Haste Sigil", "awe", false, false)
  Hexcasting.registerCustomPattern("HexJS Selftest", "weswe", false, false)
  console.log("[HexJS] registered custom patterns: Haste Sigil (awe), HexJS Selftest (weswe)")
}

try {
  registerPatterns()
} catch (e) {
  console.error("[HexJS] direct register failed: " + e)
}

ServerEvents.loaded(event => {
  try {
    registerPatterns()
  } catch (e) {
    console.error("[HexJS] ServerEvents.loaded register failed: " + e)
  }
})