package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.getString
import at.petrak.hexcasting.api.casting.iota.Iota
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity

/**
 * Rename an entity. The name goes on the bottom of the stack and the entity on
 * top, matching the way the reading runes take their subject last.
 */
object OpNameSet : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val name = args.getString(0, argc)
        val entityToRename = args.getEntity(1, argc)
        env.assertEntityInRange(entityToRename)

        return SpellAction.Result(
            Spell(name, entityToRename),
            10_000L, // 10 маны
            listOf(ParticleSpray.burst(entityToRename.position(), 0.5))
        )
    }

    private data class Spell(val name: String, val entity: Entity) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val component = Component.literal(name)
            entity.customName = component
            entity.setCustomNameVisible(true)

            // Without this a mob that is renamed and then walks away despawns
            // before it is ever saved, taking the name with it.
            if (entity is Mob) {
                entity.setPersistenceRequired()
            }

            // An item frame or a dropped item shows the stack's name rather than
            // its own, so setting one without the other leaves a renamed item
            // that still reads as dirt.
            if (entity is ItemFrame) {
                entity.item = entity.item.copy().also { it.set(DataComponents.CUSTOM_NAME, component) }
            } else if (entity is ItemEntity) {
                entity.item = entity.item.copy().also { it.set(DataComponents.CUSTOM_NAME, component) }
            }
        }
    }
}
