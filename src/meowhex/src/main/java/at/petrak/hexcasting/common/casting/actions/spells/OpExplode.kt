package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getPositiveDoubleUnderInclusive
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.mod.HexConfig
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

class OpExplode(val fire: Boolean, val destroyBlocks: Boolean) : SpellAction {
    override val argc: Int
        get() = 2

    override fun execute(
            args: List<Iota>,
            env: CastingEnvironment
    ): SpellAction.Result {
        val pos = args.getVec3(0, argc)
        val strength = args.getPositiveDoubleUnderInclusive(1, 10.0, argc)
        env.assertVecInRange(pos)

        val clampedStrength = Mth.clamp(strength, 0.0, 10.0)
        // Правка баланса: взрыв = 100 маны × мощь + 100 маны; безопасный (без разрушения блоков) в 2 раза дешевле.
        // 1 мана = 1000 media.
        var cost = (100L * 1000L * clampedStrength + 100L * 1000L).toLong()
        if (!destroyBlocks) cost /= 2
        return SpellAction.Result(
            Spell(pos, strength, this.fire, this.destroyBlocks),
            cost.toLong(),
            listOf(ParticleSpray.burst(pos, strength, 50))
        )
    }

    private data class Spell(val pos: Vec3, val strength: Double, val fire: Boolean, val destroyBlocks: Boolean) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            // TODO: you can use this to explode things *outside* of the worldborder?
            if (!env.canEditBlockAt(BlockPos.containing(pos)))
                return

            val interaction = if (this.destroyBlocks)
                Level.ExplosionInteraction.TNT
            else
                Level.ExplosionInteraction.NONE

            val effectiveStrength = if (this.destroyBlocks) {
                strength * HexConfig.server().explosionBlockDamageMultiplier()
            } else {
                strength
            }

            env.world.explode(
                env.caster,
                pos.x,
                pos.y,
                pos.z,
                effectiveStrength.toFloat(),
                this.fire,
                interaction
            )
        }
    }
}
