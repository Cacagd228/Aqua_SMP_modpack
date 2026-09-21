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
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.Vec3

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
        val srcInv = depotInventory(srcPos, env) ?: throw MishapBadBlock.of(srcPos, "depot")
        val destInv = depotInventory(destPos, env) ?: throw MishapBadBlock.of(destPos, "depot")
        if (!destInv.getItem(0).isEmpty) {
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
            val srcInv = depotInventory(srcPos, env) ?: return
            val destInv = depotInventory(destPos, env) ?: return

            val stack = srcInv.getItem(0)
            if (stack.isEmpty) return
            if (!destInv.getItem(0).isEmpty) return

            destInv.setItem(0, stack)
            srcInv.setItem(0, ItemStack.EMPTY)

            // Make sure the world knows both depots changed.
            env.world.getBlockEntity(srcPos)?.setChanged()
            env.world.getBlockEntity(destPos)?.setChanged()
        }
    }

    /** True if the block at [pos] is a (pre-registered-id) Create Depot. */
    private fun isDepotAt(world: net.minecraft.world.level.Level, pos: BlockPos): Boolean =
        BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).block) == DEPOT_ID

    private fun depotInventory(pos: BlockPos, env: CastingEnvironment): Container? {
        val be = env.world.getBlockEntity(pos) ?: return null
        return reflectDepotInventory(be)
    }

    /**
     * Soft-dependency on Create: peek by reflection for a field that is a
     * `net.minecraft.world.Container` on the Depot block entity (Create's `inventory`).
     * Falls back up the class hierarchy so it keeps working across Create versions.
     */
    private fun reflectDepotInventory(be: BlockEntity): Container? {
        if (be is Container) return be
        var clazz: Class<*>? = be.javaClass
        while (clazz != null && clazz != Any::class.java) {
            for (field in clazz.declaredFields) {
                if (Container::class.java.isAssignableFrom(field.type)) {
                    try {
                        field.isAccessible = true
                        val value = field.get(be)
                        if (value is Container) return value
                    } catch (_: IllegalAccessException) {
                        // keep walking up
                    }
                }
            }
            clazz = clazz.superclass
        }
        return null
    }
}