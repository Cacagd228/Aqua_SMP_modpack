package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getList
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadOffhandItem
import at.petrak.hexcasting.api.casting.mishaps.MishapOthersName
import at.petrak.hexcasting.common.items.magic.ItemPackagedHex
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.world.item.ItemStack

// meowhex: сосуды мысли вырезаны — создание требует только пустой предмет
// в оффхэнде и список узоров; стоимость создания платится маной кастера,
// внутреннего пула аметиста у предмета больше нет (см. PackagedItemCastEnv).
class OpMakePackagedSpell<T : ItemPackagedHex>(val itemType: T, val cost: Long) : SpellAction {
    override val argc = 1
    override fun execute(
            args: List<Iota>,
            env: CastingEnvironment
    ): SpellAction.Result {
        val patterns = args.getList(0, argc).toList()

        val (handStack, hand) = env.getHeldItemToOperateOn {
            val hexHolder = IXplatAbstractions.INSTANCE.findHexHolder(it)
            it.`is`(itemType) && hexHolder != null && !hexHolder.hasHex()
        }
            ?: throw MishapBadOffhandItem(ItemStack.EMPTY.copy(), null, itemType.description) // TODO: hack

        val hexHolder = IXplatAbstractions.INSTANCE.findHexHolder(handStack)
        if (!handStack.`is`(itemType)) {
            throw MishapBadOffhandItem(handStack, hand, itemType.description)
        } else if (hexHolder == null || hexHolder.hasHex()) {
            throw MishapBadOffhandItem.of(handStack, hand, "iota.write")
        }

        val trueName = MishapOthersName.getTrueNameFromArgs(patterns, env.caster)
        if (trueName != null)
            throw MishapOthersName(trueName)

        return SpellAction.Result(
            Spell(patterns, handStack),
            cost,
            listOf(ParticleSpray.burst(env.mishapSprayPos(), 0.5))
        )
    }

    private inner class Spell(val patterns: List<Iota>, val stack: ItemStack) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val hexHolder = IXplatAbstractions.INSTANCE.findHexHolder(stack)
            if (hexHolder != null && !hexHolder.hasHex()) {
                hexHolder.writeHex(patterns, env.pigment, 0)
            }
        }
    }
}
