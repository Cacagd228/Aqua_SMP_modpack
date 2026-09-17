package at.petrak.hexcasting.forge.interop.curios;

import at.petrak.hexcasting.api.misc.DiscoveryHandlers;
import at.petrak.hexcasting.common.items.HexBaubleItem;
import at.petrak.hexcasting.common.items.magic.ItemCreativeUnlocker;
import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.hexcasting.interop.HexInterop;
import com.google.common.collect.HashMultimap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.Optional;

public class CuriosApiInterop {
    static class Wrapper implements ICurioItem {
        private final HexBaubleItem bauble;

        Wrapper(HexBaubleItem bauble) {
            this.bauble = bauble;
        }

        @Override
        public com.google.common.collect.Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext, net.minecraft.resources.ResourceLocation id, ItemStack stack
        ) {
            var map = HashMultimap.<Holder<Attribute>, AttributeModifier>create();
            this.bauble.getHexBaubleAttrs(stack).forEach(
                (attribute, modifier) -> map.put(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), modifier));
            return map;
        }
    }

    public static void init() {
        CuriosApi.registerCurio(HexItems.SCRYING_LENS, new Wrapper(HexItems.SCRYING_LENS));
        CuriosApi.registerCurio(HexItems.CREATIVE_UNLOCKER, new Wrapper(HexItems.CREATIVE_UNLOCKER));

        DiscoveryHandlers.addDebugItemDiscoverer((player, type) -> {
            Optional<ItemStack> result = CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack -> ItemCreativeUnlocker.isDebug(stack, type)))
                .map(slot -> slot.stack());
            return result.orElse(ItemStack.EMPTY);
        });
    }

    public static final String HEXPOUCH = "hexpouch";

    public static void onInterModEnqueue(final InterModEnqueueEvent event) {
        InterModComms.sendTo(HexInterop.Forge.CURIOS_API_ID, SlotTypeMessage.REGISTER_TYPE,
            () -> SlotTypePreset.HEAD.getMessageBuilder().build());
        // hexpouch itself is registered via data packs:
        //  data/curios/curios/slots/hexpouch.json (type, size 5)
        //  data/hexcasting/curios/entities/hexpouch.json (assignment to players)
    }

}
