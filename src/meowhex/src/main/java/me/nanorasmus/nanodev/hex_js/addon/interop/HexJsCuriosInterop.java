package me.nanorasmus.nanodev.hex_js.addon.interop;

import at.petrak.hexcasting.common.items.HexBaubleItem;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import me.nanorasmus.nanodev.hex_js.addon.HextendedItems;
import me.nanorasmus.nanodev.hex_js.addon.HexArtifactsItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Optional Curios API interop: makes the charged amethyst diadem and the mana
 * amulets behave as curio (hexcasting attributes while worn) mirroring
 * hexcasting's own {@code CuriosApiInterop.Wrapper}. Slot fit itself is
 * data-driven: the diadem is added to {@code curios:head} and the amulets to
 * {@code curios:necklace} (see data/curios/tags/item/*.json).
 */
public final class HexJsCuriosInterop {
    private HexJsCuriosInterop() {
    }

    public static void init() {
        registerBauble((net.minecraft.world.item.Item) HextendedItems.CHARGED_AMETHYST_DIADEM.get());
        registerBauble((net.minecraft.world.item.Item) HexArtifactsItems.AMETHYST_NECKLACE.get());
        registerBauble((net.minecraft.world.item.Item) HexArtifactsItems.CHARGED_AMETHYST_NECKLACE.get());
        registerBauble((net.minecraft.world.item.Item) HexArtifactsItems.OVERLOADED_NECKLACE.get());
        registerBauble((net.minecraft.world.item.Item) HexArtifactsItems.SELF_TORTURE_RING.get());
        registerBauble((net.minecraft.world.item.Item) HexArtifactsItems.PATTERN_READER.get());
        registerBauble((net.minecraft.world.item.Item) HexArtifactsItems.SNIPER_SCOPE.get());
    }

    private static void registerBauble(net.minecraft.world.item.Item item) {
        CuriosApi.registerCurio(item, new BaubleCurio((HexBaubleItem) item));
    }

    public static final class BaubleCurio implements ICurioItem {
        private final HexBaubleItem bauble;

        BaubleCurio(HexBaubleItem bauble) {
            this.bauble = bauble;
        }

        @Override
        public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
                SlotContext slotContext, ResourceLocation id, ItemStack stack) {
            Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
            this.bauble.getHexBaubleAttrs(stack).forEach(
                    (attribute, modifier) -> map.put(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute), modifier));
            return map;
        }

        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return true;
        }
    }
}
