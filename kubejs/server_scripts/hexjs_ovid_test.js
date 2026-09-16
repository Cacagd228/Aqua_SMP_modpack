// hexjs_ovid_test.js — тестовый рецепт Ovid's Distillation
// iron_ingot (A, верх) + coal (Б, низ) -> diamond, counts должны совпадать, 20 пыли
// Паттерн edqwqa (EAST) на двух ItemEntity в радиусе

function registerOvidRecipes() {
  try {
    Hexcasting.registerOvidRecipe("minecraft:iron_ingot", "minecraft:coal", Item.of("minecraft:diamond", 1))
    console.log("[HexJS] Ovid test recipe registered: iron_ingot + coal -> diamond")
  } catch (e) {
    console.error("[HexJS] Ovid register failed: " + e)
  }
}

try {
  registerOvidRecipes()
} catch (e) {
  console.error("[HexJS] direct Ovid register failed: " + e)
}

ServerEvents.loaded(event => {
  try {
    registerOvidRecipes()
  } catch (e) {
    console.error("[HexJS] ServerEvents.loaded Ovid register failed: " + e)
  }
})
