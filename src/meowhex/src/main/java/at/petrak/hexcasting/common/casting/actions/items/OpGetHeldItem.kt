package at.petrak.hexcasting.common.casting.actions.items

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadEntity
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity

/**
 * Read the stack held in one of an entity's hands. Which hand is fixed when the
 * rune is drawn, not by the spell, so the two hands are two separate runes.
 *
 * This fork ships this read-only: it can look at a hand but never conjure an
 * item or take one.
 */
class OpGetHeldItem(private val hand: InteractionHand) : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val entity = args.getEntity(0, argc)
        val stack = when (entity) {
            is LivingEntity -> entity.getItemInHand(hand)
            is ItemFrame, is ItemEntity -> if (hand == InteractionHand.MAIN_HAND) {
                getContainedStack(entity)
            } else {
                throw MishapBadEntity.of(entity, "item.read.offhand")
            }

            else -> throw MishapBadEntity.of(entity, "item.read.any")
        }
        return stack.asActionResult
    }

    private fun getContainedStack(entity: Entity) = when (entity) {
        is ItemFrame -> entity.getItem()
        is ItemEntity -> entity.getItem()
        else -> throw MishapBadEntity.of(entity, "item.read.any")
    }
}
