package at.petrak.hexcasting.common.casting.actions.types

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * The item type the caster is holding. An empty hand reads as null rather than
 * as `minecraft:air`, so a spell can branch on "am I holding anything".
 */
object OpTypeItemHeld : ConstMediaAction {
    override val argc = 0

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val (handStack) = env.getHeldItemToOperateOn { it.item != Items.AIR }
            ?: return listOf(NullIota())
        return handStack.item.asActionResult
    }
}
