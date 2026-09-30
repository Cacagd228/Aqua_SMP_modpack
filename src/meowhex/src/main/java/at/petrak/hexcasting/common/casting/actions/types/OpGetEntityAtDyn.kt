package at.petrak.hexcasting.common.casting.actions.types

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntityType
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.common.casting.actions.selectors.OpGetEntitiesBy.Companion.isReasonablySelectable
import net.minecraft.world.phys.AABB

/**
 * The nearest entity of a given type to a point. Yields null when nothing of
 * that type is there, so a spell can ask "who is here" without pre-checking.
 */
object OpGetEntityAtDyn : ConstMediaAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val type = args.getEntityType(0, argc)
        val pos = args.getVec3(1, argc)
        env.assertVecInRange(pos)

        val aabb = AABB(pos.add(-0.5, -0.5, -0.5), pos.add(0.5, 0.5, 0.5))
        val found = env.world.getEntities(null, aabb) {
            isReasonablySelectable(env, it) && it.type == type
        }.minByOrNull { it.distanceToSqr(pos) }
        return found.asActionResult
    }
}
