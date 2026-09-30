package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import net.minecraft.world.entity.item.ItemEntity

/**
 * Read an entity's name as text. An item entity reports the name of the stack
 * it holds, since that is the name a player sees.
 */
object OpNameGet : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val entity = args.getEntity(0, argc)
        env.assertEntityInRange(entity)
        // An entity with no name at all has a null display name; report the
        // empty string rather than null, so the result is still a string.
        val name = if (entity is ItemEntity) entity.getItem().displayName.string
        else entity.displayName?.string ?: ""
        return name.asActionResult
    }
}
