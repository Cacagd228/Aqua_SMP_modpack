package me.nanorasmus.nanodev.hex_js.addon.item;

import at.petrak.hexcasting.common.items.HexBaubleItem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Громоотвод — Curios-артефакт (слот necklace). Собственных атрибутов не даёт:
 * весь эффект живёт в {@link me.nanorasmus.nanodev.hex_js.casting.LightningRodHandler}.
 *
 * <p>Перехват срабатывает по событию наложения «Безмолвия»
 * ({@code LightningRodSilenceMixin}): все «Безмолвия» в радиусе
 * {@code LightningRodHandler.RADIUS} блоков от носящего снимаются с целей
 * и переезжают на него. За каждое перехваченное «Безмолвие» носитель получает
 * {@code LightningRodHandler.REGEN_PER_STACK_MANA_PER_6S} регена маны на
 * {@code LightningRodHandler.STACK_TICKS} секунд; стаки суммируются.
 *
 * <p>Собственное «Безмолвие» носящего не переносится и не считается бонусом.
 * «Безмолвие» других носителей Громоотвода в радиусе не перехватывается —
 * иначе два громоотвода зациклились бы друг на друге.
 */
public class ItemLightningRod extends Item implements HexBaubleItem {

    public ItemLightningRod(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getHexBaubleAttrs(ItemStack stack) {
        // Эффект не атрибутный: реген вешается и снимается обработчиком по таймеру.
        return HashMultimap.create();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
            List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("meowhex.tooltip.lightning_rod")
                .withStyle(ChatFormatting.GRAY));
    }
}
