package at.petrak.hexcasting.api.casting.mishaps

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.core.BlockPos
import net.minecraft.world.item.DyeColor
import net.minecraft.world.phys.Vec3

/**
 * Thrown when the destination depot already holds an item stack, so the transfer cannot happen.
 */
class MishapDepotOccupied(val pos: BlockPos) : Mishap() {
    override fun accentColor(ctx: CastingEnvironment, errorCtx: Context): FrozenPigment =
        dyeColor(DyeColor.ORANGE)

    override fun execute(ctx: CastingEnvironment, errorCtx: Context, stack: MutableList<Iota>) {
        // Just a polite refusal — nothing destructive.
    }

    override fun particleSpray(ctx: CastingEnvironment) =
        ParticleSpray.burst(Vec3.atCenterOf(pos), 1.0)

    override fun errorMessage(ctx: CastingEnvironment, errorCtx: Context) =
        error("depot.occupied", pos.toShortString(), blockAtPos(ctx, pos))
}