StartupEvents.registry('item', event => {
  const tiers = [
    { id: 'andesite', name: 'Andesite Corps', color: 0x7A7A7A, frame: 'meowaddons:frame_andesite' },
    { id: 'latun', name: 'Latun Corps', color: 0xB5A05A, frame: 'meowaddons:frame_latun' },
    { id: 'steel', name: 'Steel Corps', color: 0x6C6C6C, frame: 'meowaddons:frame_steel' },
    { id: 'shadow_steel', name: 'Shadow Steel Corps', color: 0x2F2F2F, frame: 'meowaddons:frame_shadow_steel' },
    { id: 'refined_radiance', name: 'Refined Radiance Corps', color: 0xE8E0B0, frame: 'meowaddons:frame_refined_radiance' },
    { id: 'chromatic_compound', name: 'Chromatic Compound Corps', color: 0x8A3B8F, frame: 'meowaddons:frame_chromatic_compound' }
  ]

  tiers.forEach(t => {
    event.create(`corps_${t.id}`)
      .displayName(t.name)
      .color(0, t.color)
      .tag('meowaddons:corps')
      .tag(`meowaddons:corps/${t.id}`)
  })
})