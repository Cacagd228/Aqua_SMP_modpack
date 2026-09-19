package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.common.lib.HexDamageTypes
import me.nanorasmus.nanodev.hex_js.entity.EntitySunBeam
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

object OpSunStrike : SpellAction {
    override val argc: Int
        get() = 1

    override fun execute(
            args: List<Iota>,
            env: CastingEnvironment
    ): SpellAction.Result {
        val target = args.getVec3(0, argc)
        env.assertVecInRange(target)

        if (!env.canEditBlockAt(BlockPos.containing(target)))
            throw MishapBadLocation(target, "forbidden")

        return SpellAction.Result(
            Spell(target),
            2 * MediaConstants.SHARD_UNIT,
            listOf(ParticleSpray(target.add(0.0, 32.0, 0.0), Vec3(0.0, -1.0, 0.0), 0.5, 0.1))
        )
    }

    private data class Spell(val target: Vec3) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val world = env.world
            val caster = env.caster

            // Create damage source
            val damageSource = HexDamageTypes.source(world, HexDamageTypes.SUN_STRIKE, caster)

            // Damage entities in a small radius at the target position
            val radius = 1.5
            val box = AABB(target.x - radius, target.y, target.z - radius, target.x + radius, target.y + 10.0, target.z + radius)
            val entities = world.getEntitiesOfClass(LivingEntity::class.java, box)

            for (entity in entities) {
                entity.hurt(damageSource, 5.0f)
            }

            // Sky beam visual (custom entity, no lightning involved).
            val beam = EntitySunBeam(world, target.x, target.y, target.z)
            world.addFreshEntity(beam)
        }
    }
}