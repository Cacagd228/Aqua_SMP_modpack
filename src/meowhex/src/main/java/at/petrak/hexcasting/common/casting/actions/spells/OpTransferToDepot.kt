package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadBlock
import at.petrak.hexcasting.api.casting.mishaps.MishapDepotOccupied
import at.petrak.hexcasting.api.misc.MediaConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import me.nanorasmus.nanodev.hex_js.casting.DepotHelper

/**
 * "Wings of Irida" — whisk one whole item stack from one Create Depot to another.
 *
 * The stack eats two vectors: the top one is the *destination* depot (B),
 * the one below it is the *source* depot (A).
 *
 * The destination depot must be completely empty (a depot only fits one stack),
 * and both ends must actually be depots.
 */
object OpTransferToDepot : SpellAction {
    override val argc: Int
        get() = 2

    private val DEPOT_ID = ResourceLocation.fromNamespaceAndPath("create", "depot")

    override fun execute(
        args: List<Iota>,
        env: CastingEnvironment
    ): SpellAction.Result {
        // args[0] is the top of the stack: destination depot B; args[1] is source depot A.
        val destPos = BlockPos.containing(args.getVec3(0, argc))
        val srcPos = BlockPos.containing(args.getVec3(1, argc))
        env.assertPosInRange(destPos)
        env.assertPosInRange(srcPos)

        if (!isDepotAt(env.world, srcPos)) {
            throw MishapBadBlock.of(srcPos, "depot")
        }
        if (!isDepotAt(env.world, destPos)) {
            throw MishapBadBlock.of(destPos, "depot")
        }
        // Inventory IO goes through DepotHelper (getHeldItem reflection +
        // ItemHandler capability fallback): raw Container-field reflection misses
        // the depot inventory on current Create versions and used to throw a
        // misleading "expected depot" mishap on perfectly good depots.
        val srcStack = DepotHelper.getStack(env.world, srcPos) ?: throw MishapBadBlock.of(srcPos, "depot")
        val destStack = DepotHelper.getStack(env.world, destPos) ?: throw MishapBadBlock.of(destPos, "depot")
        if (!destStack.isEmpty) {
            throw MishapDepotOccupied(destPos)
        }

        // Правка баланса: крылья Ириды (ванилла) = 100 маны за блок расстояния (1 мана = 1000 media).
        val dist = Math.sqrt(srcPos.distSqr(destPos).toDouble()).coerceAtLeast(1.0)
        val cost = (dist * 100L * 1000L).toLong()
        return SpellAction.Result(
            Spell(srcPos, destPos),
            cost,
            listOf(
                ParticleSpray.burst(Vec3.atCenterOf(srcPos), 1.0),
                ParticleSpray.burst(Vec3.atCenterOf(destPos), 1.0)
            )
        )
    }

    private data class Spell(val srcPos: BlockPos, val destPos: BlockPos) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val srcStack = DepotHelper.getStack(env.world, srcPos) ?: return
            if (srcStack.isEmpty) return
            val destStack = DepotHelper.getStack(env.world, destPos) ?: return
            if (!destStack.isEmpty) return

            if (!DepotHelper.clearStack(env.world, srcPos)) return
            if (!DepotHelper.setStack(env.world, destPos, srcStack)) {
                // Destination write failed: put the stack back where it was.
                DepotHelper.setStack(env.world, srcPos, srcStack)
            }
        }
    }

    /** True if the block at [pos] is a (pre-registered-id) Create Depot. */
    private fun isDepotAt(world: net.minecraft.world.level.Level, pos: BlockPos): Boolean =
        BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).block) == DEPOT_ID
}