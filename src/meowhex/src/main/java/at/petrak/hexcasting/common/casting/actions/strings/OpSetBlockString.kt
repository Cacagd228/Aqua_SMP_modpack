package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.casting.iota.StringIota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.network.Filterable
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WritableBookContent
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.level.block.entity.SignText
import net.minecraft.world.phys.Vec3

/**
 * Write text to a sign or to the book on a lectern. A single string is split on
 * newlines to fill a sign's four lines; a list is taken one entry per line, or
 * one entry per page for a lectern. The position goes on the bottom of the
 * stack, the text on top.
 */
object OpSetBlockString : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        env.assertPosInRangeForEditing(pos)

        // Resolve to plain lines up front. Doing it here rather than at cast time
        // means a stack iota that cannot be rendered fails now, while the
        // mishap can still point at the right argument.
        val lines: List<String> = when (val text = args[argc - 1]) {
            is StringIota -> text.string.split('\n')
            is ListIota -> text.list.map { (it as? StringIota)?.string ?: it.display().string }
            else -> throw MishapInvalidIota.ofType(
                text, 0, "string_or_list"
            )
        }

        return SpellAction.Result(
            Spell(pos, lines),
            10_000L, // 10 маны
            listOf(ParticleSpray.burst(Vec3.atCenterOf(pos), 1.0))
        )
    }

    private data class Spell(val pos: BlockPos, val lines: List<String>) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val blockEntity = env.world.getBlockEntity(pos) ?: return

            when (blockEntity) {
                is SignBlockEntity -> {
                    // A sign another player is currently editing is off limits,
                    // or a spell would yank the text out from under them.
                    val editor = blockEntity.playerWhoMayEdit
                    if (editor != null && editor != env.caster?.uuid) return

                    // Keep the sign's existing colour and glow; a spell that sets
                    // text should not quietly dye the sign while it is at it.
                    val existing = blockEntity.getText(true)
                    val messages = Array(SignText.LINES) { i ->
                        Component.literal(lines.getOrElse(i) { "" })
                    }
                    val signText = SignText(
                        messages,
                        messages.copyOf(),
                        existing.color,
                        existing.hasGlowingText()
                    )
                    blockEntity.setText(signText, true)
                    // Signs carry a second face. Write it too, or the back keeps
                    // whatever it had before the spell ran.
                    blockEntity.setText(signText, false)
                }

                is LecternBlockEntity -> {
                    val book = blockEntity.book
                    // Only a writable book takes a new page list. A written book
                    // is signed and finished, and rewriting it would throw away
                    // whatever its author had already put on the page.
                    if (book.item == Items.WRITABLE_BOOK) {
                        val pages = lines
                            .take(WritableBookContent.MAX_PAGES)
                            .map { Filterable.passThrough(it) }
                        blockEntity.setBook(
                            book.copy().also {
                                it.set(
                                    DataComponents.WRITABLE_BOOK_CONTENT,
                                    WritableBookContent(pages)
                                )
                            }
                        )
                    }
                }

                else -> return
            }

            blockEntity.setChanged()
            val state = env.world.getBlockState(pos)
            env.world.sendBlockUpdated(pos, state, state, 3)
        }
    }
}
