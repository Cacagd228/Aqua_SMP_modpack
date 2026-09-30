package at.petrak.hexcasting.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.NullIota
import at.petrak.hexcasting.api.casting.iota.StringIota
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WritableBookContent
import net.minecraft.world.item.component.WrittenBookContent
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.level.block.entity.SignText

/**
 * Read the text of a sign, or the pages of the book on a lectern. A sign comes
 * back as one newline-joined string; a lectern comes back as a list, because a
 * book's pages have no natural separator worth inventing.
 */
object OpGetBlockString : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        env.assertPosInRangeForEditing(pos)
        val blockEntity = env.world.getBlockEntity(pos) ?: return listOf(NullIota())

        return when (blockEntity) {
            is SignBlockEntity -> {
                val front = blockEntity.getText(true)
                val sb = StringBuilder(front.getMessage(0, true).string)
                for (i in 1 until SignText.LINES) {
                    sb.append('\n').append(front.getMessage(i, true).string)
                }
                sb.toString().asActionResult
            }

            is LecternBlockEntity -> {
                val book = blockEntity.book
                when {
                    book.item == Items.WRITABLE_BOOK -> {
                        val content = book.get(DataComponents.WRITABLE_BOOK_CONTENT)
                            ?: return listOf(NullIota())
                        content.pages().map { StringIota(it.raw()) }.asActionResult
                    }

                    book.item == Items.WRITTEN_BOOK -> {
                        val content = book.get(DataComponents.WRITTEN_BOOK_CONTENT)
                            ?: return listOf(NullIota())
                        // A written book's pages are components, possibly with
                        // italics and other formatting, so the rendered text is
                        // what the reader actually sees.
                        content.pages().map { StringIota(it.get(true).string) }.asActionResult
                    }

                    else -> listOf(NullIota())
                }
            }

            else -> listOf(NullIota())
        }
    }
}
