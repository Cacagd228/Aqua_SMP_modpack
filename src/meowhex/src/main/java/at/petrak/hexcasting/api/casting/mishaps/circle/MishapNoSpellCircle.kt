package at.petrak.hexcasting.api.casting.mishaps.circle

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments

class MishapNoSpellCircle : Mishap() {
    override fun accentColor(ctx: CastingEnvironment, errorCtx: Context): FrozenPigment =
        dyeColor(DyeColor.LIGHT_BLUE)

    private inline fun dropAll(player: Player, stacks: MutableList<ItemStack>, filter: (ItemStack) -> Boolean = { true }) {
        for (index in stacks.indices) {
            val item = stacks[index]
            if (!item.isEmpty && filter(item)) {
                player.drop(item, true, false)
                stacks[index] = ItemStack.EMPTY
            }
        }
    }

    override fun execute(env: CastingEnvironment, errorCtx: Context, stack: MutableList<Iota>) {
        val caster = env.caster
        if (caster != null) {
            // FIXME: handle null caster case
            dropAll(caster, caster.inventory.items)
            dropAll(caster, caster.inventory.offhand)
            val bindingCurse = caster.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.BINDING_CURSE)
            dropAll(caster, caster.inventory.armor) {
                EnchantmentHelper.getItemEnchantmentLevel(bindingCurse, it) <= 0
            }
        }
    }

    override fun errorMessage(ctx: CastingEnvironment, errorCtx: Context) =
        error("no_spell_circle")
}
