// Крафт корпусов как в Create: рамка + обтёсанное бревно в мире -> корпус
// Реализовано через create:item_application — работает и руками, и деплоером, и видно в JEI
// Было: BlockEvents.rightClicked костыль без JEI. Стало: нативные рецепты Create.
ServerEvents.recipes(event => {
  const FRAME_TO_CASING = {
    'meowaddons:frame_andesite': 'create:andesite_casing',
    'meowaddons:frame_latun': 'create:brass_casing',
    'meowaddons:frame_steel': 'create:railway_casing',
    'meowaddons:frame_shadow_steel': 'create:shadow_steel_casing',
    'meowaddons:frame_refined_radiance': 'create:refined_radiance_casing',
    'meowaddons:frame_chromatic_compound': 'createcasing:creative_casing'
  }

  for (const [frame, casing] of Object.entries(FRAME_TO_CASING)) {
    // как у самого Create: отдельные рецепты для log и wood
    // порядок ingredients важен: [0] = блок в мире, [1] = предмет в руке
    event.custom({
      type: 'create:item_application',
      ingredients: [
        { tag: 'c:stripped_logs' },
        { item: frame }
      ],
      results: [{ id: casing }]
    })
    event.custom({
      type: 'create:item_application',
      ingredients: [
        { tag: 'c:stripped_woods' },
        { item: frame }
      ],
      results: [{ id: casing }]
    })
  }
})
