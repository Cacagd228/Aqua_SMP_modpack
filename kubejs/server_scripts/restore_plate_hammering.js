// ============================================================
// restore_plate_hammering.js — возврат крафта пластин молоточком
// ============================================================
// 00_tier_lockdown.js снимает ВСЕ рецепты createdieselgenerators (ПОЛНЫЙ БАН
// модов в ЧАСТИ 2), из-за чего пропадает и крафт пластин молоточком
// (type: createdieselgenerators:hammering):
//   #c:ingots/iron   -> create:iron_sheet
//   #c:ingots/copper -> create:copper_sheet
//   #c:ingots/brass  -> create:brass_sheet
//   #c:ingots/gold   -> create:golden_sheet
// Молот (createdieselgenerators:hammer) уже возвращён в 00_tier_lockdown.js
// (aquasmp:base/cdg_hammer), здесь только сами рецепты ковки пластин 1:1
// из оригинала мода:
//   data/createdieselgenerators/recipe/hammering/*.json
// Удалить этот файл + /reload = пластины снова только через Create/другие моды.

ServerEvents.recipes(event => {
  // слиток -> пластина, молоточком (рукой / create:deployer)
  // ВАЖНО: в event.custom тег пишется БЕЗ '#', иначе рецепт не парсится
  const HAMMERING_PLATES = [
    ['c:ingots/iron', 'create:iron_sheet'],
    ['c:ingots/copper', 'create:copper_sheet'],
    ['c:ingots/brass', 'create:brass_sheet'],
    ['c:ingots/gold', 'create:golden_sheet']
  ]

  for (const [ingot, sheet] of HAMMERING_PLATES) {
    event.custom({
      type: 'createdieselgenerators:hammering',
      ingredients: [{ tag: ingot }],
      results: [{ id: sheet }]
    }).id(`aquasmp:base/hammering_${sheet.split(':')[1]}`)
  }
})
