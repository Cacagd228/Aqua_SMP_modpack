// server_scripts/hexjs_haste_scroll.js — команды для теста HexJS.
//
//   /haste_scroll     — выдать свиток с узором "Haste Sigil" (EAST + awe)
//   /hexjs_selftest   — принудительно кастануть самотест (weswe) БЕЗ посоха и меди;
//                       в чате должно появиться "Self-test OK" (проверка всей цепочки)
//   /hexjs_debug      — состояние руки (диагностика)
ServerEvents.commandRegistry(event => {
  const { commands: Commands } = event

  event.register(
    Commands.literal("haste_scroll")
      .requires(s => s.hasPermission(0))
      .executes(ctx => {
        let player = ctx.source.playerOrException
        let scroll = Hexcasting.createSmallScroll("Haste Sigil", HexDir.EAST, "awe")
        console.log("[HexJS] /haste_scroll give " + scroll.getId() + " to " + player.getName().getString())
        player.give(scroll)
        ctx.source.sendSuccess(Component.literal("§aВыдан свиток Спешки (EAST + awe). §7ПКМ на стене — повесить, затем кастуй с посоха."), true)
        return 1
      })
  )

  event.register(
    Commands.literal("hexjs_selftest")
      .requires(s => s.hasPermission(0))
      .executes(ctx => {
        let player = ctx.source.playerOrException
        let regs = Hexcasting.getRegisteredPatternSignatures()
        let isReg = Hexcasting.isCustomPatternRegistered("weswe")
        ctx.source.sendSuccess(Component.literal("§e[HexJS] registered=[" + regs.join(",") + "] wesweRegistered=" + isReg), true)
        Hexcasting.forceCastPlayerName(player.getName().getString(), ["weswe"])
        ctx.source.sendSuccess(Component.literal("§a[HexJS] forced 'weswe' — жди «Self-test OK»"), true)
        return 1
      })
  )

  event.register(
    Commands.literal("hexjs_debug")
      .requires(s => s.hasPermission(0))
      .executes(ctx => {
        let player = ctx.source.playerOrException
        let hand = player.getMainHandItem()
        ctx.source.sendSuccess(Component.literal("§7[HexJS Debug] Hand: " + hand.getId() + " stack=" + hand), false)
        return 1
      })
  )
})