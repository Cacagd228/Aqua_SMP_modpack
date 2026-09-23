// Create Deco: знаки (decals) - заменить железный лист на промышленный железный лист
ServerEvents.recipes(event => {
  // Заменяем вход во всех decal рецептах createdeco
  const decals = [
    'createdeco:decal_down','createdeco:decal_cross','createdeco:decal_flow','createdeco:decal_radioactive',
    'createdeco:decal_electrical','createdeco:decal_top_left','createdeco:decal_fire_diamond','createdeco:decal_left',
    'createdeco:decal_down_left','createdeco:decal_fluid','createdeco:decal_right','createdeco:decal_up',
    'createdeco:decal_ice','createdeco:decal_down_right','createdeco:decal_top_right','createdeco:decal_no_entry',
    'createdeco:decal_warning','createdeco:decal_creeper','createdeco:decal_skull','createdeco:decal_fire'
  ]
  decals.forEach(id => {
    event.replaceInput({id: id}, 'create:iron_sheet', 'createdeco:industrial_iron_sheet')
  })
  // На всякий - глобально для любых stonecutting где вход iron_sheet и выход decal
  // event.replaceInput({output: /createdeco:decal_.*/}, 'create:iron_sheet', 'createdeco:industrial_iron_sheet')
})
