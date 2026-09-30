package at.petrak.hexcasting.api.casting.iota;

import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An item stack as a first-class iota. Ported from the MoreIotas addon (MIT,
 * Talia-12).
 * <p>
 * This fork deliberately ships <b>read-only</b> item ops: nothing here can
 * conjure items out of nothing or read a container's inventory. The iota exists
 * so spells can ask what is in a hand and what properties a block or item
 * carries, not to mint items.
 */
public class ItemStackIota extends Iota {
    public ItemStackIota(@NotNull ItemStack datum) {
        super(HexIotaTypes.ITEM_STACK, datum.copy());
    }

    public ItemStack getStack() {
        return (ItemStack) this.payload;
    }

    @Override
    public boolean isTruthy() {
        return !this.getStack().isEmpty();
    }

    @Override
    public boolean toleratesOther(Iota that) {
        return typesMatch(this, that)
            && that instanceof ItemStackIota siota
            && ItemStack.matches(this.getStack(), siota.getStack());
    }

    @Override
    public @NotNull Tag serialize() {
        return HexUtils.serializeToNBT(this.getStack());
    }

    public static IotaType<ItemStackIota> TYPE = new IotaType<>() {
        @Nullable
        @Override
        public ItemStackIota deserialize(Tag tag, ServerLevel world) throws IllegalArgumentException {
            return ItemStackIota.deserialize(tag);
        }

        @Override
        public Component display(Tag tag) {
            return ItemStackIota.display(HexUtils.downcast(tag, CompoundTag.TYPE));
        }

        @Override
        public int color() {
            return 0xff_ddaa44;
        }
    };

    /**
     * Resolve the stack without a world. The registry access is rebuilt from
     * {@link BuiltInRegistries} because {@code display} runs client-side and on
     * the server alike, and a dataview is not always available.
     */
    public static ItemStackIota deserialize(Tag tag) throws IllegalArgumentException {
        var ctag = HexUtils.downcast(tag, CompoundTag.TYPE);
        if (ctag.isEmpty()) {
            return new ItemStackIota(ItemStack.EMPTY);
        }
        var access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var stack = ItemStack.parseOptional(access, ctag);
        if (stack == null) {
            // Unknown item id: keep the tag so a datapack that re-adds the item
            // can still resolve it, rather than silently degrading to "empty".
            throw new IllegalArgumentException("ItemStackIota: unparseable stack tag");
        }
        return new ItemStackIota(stack);
    }

    public static Component display(CompoundTag tag) {
        var access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var stack = ItemStack.parseOptional(access, tag);
        if (stack == null || stack.isEmpty()) {
            return Component.translatable("hexcasting.tooltip.unknown_item").withStyle(ChatFormatting.RED);
        }
        return Component.literal(count(stack) + "x ").append(stack.getHoverName())
            .withStyle(ChatFormatting.GOLD);
    }

    /** A stack of 1 reads better without the count prefix. */
    private static String count(ItemStack stack) {
        return stack.getCount() == 1 ? "" : Integer.toString(stack.getCount());
    }

    public static Component display(ItemStack stack) {
        return display(HexUtils.serializeToNBT(stack));
    }

    public static ResourceLocation idOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem());
    }
}
