package at.petrak.hexcasting.api.casting.iota;

import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An item <i>type</i> as a value -- distinct from {@link ItemStackIota}, which
 * is a concrete stack. Ported from the MoreIotas addon (MIT, Talia-12).
 * <p>
 * A type carries no stack size and no NBT, which makes it the right thing to
 * hand to a predicate like "give me every stack of this", and the wrong thing
 * to hand to a spell that wants to consume items.
 */
public class ItemTypeIota extends Iota {
    public ItemTypeIota(@NotNull Item datum) {
        super(HexIotaTypes.ITEM_TYPE, datum);
    }

    public Item getItem() {
        return (Item) this.payload;
    }

    public ResourceLocation getId() {
        return BuiltInRegistries.ITEM.getKey(this.getItem());
    }

    @Override
    public boolean isTruthy() {
        return this.getItem() != Items_AIR;
    }

    @Override
    public boolean toleratesOther(Iota that) {
        return typesMatch(this, that)
            && that instanceof ItemTypeIota itiota
            && this.getItem() == itiota.getItem();
    }

    @Override
    public @NotNull Tag serialize() {
        return StringTag.valueOf(this.getId().toString());
    }

    private static final Item Items_AIR = net.minecraft.world.item.Items.AIR;

    public static IotaType<ItemTypeIota> TYPE = new IotaType<>() {
        @Nullable
        @Override
        public ItemTypeIota deserialize(Tag tag, ServerLevel world) throws IllegalArgumentException {
            return ItemTypeIota.deserialize(tag);
        }

        @Override
        public Component display(Tag tag) {
            return ItemTypeIota.display(HexUtils.downcast(tag, StringTag.TYPE).getAsString());
        }

        @Override
        public int color() {
            return 0xff_ffcc66;
        }
    };

    public static ItemTypeIota deserialize(Tag tag) throws IllegalArgumentException {
        var id = ResourceLocation.parse(HexUtils.downcast(tag, StringTag.TYPE).getAsString());
        var item = BuiltInRegistries.ITEM.getOptional(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown item " + id));
        return new ItemTypeIota(item);
    }

    public static Component display(String id) {
        return Component.literal(id).withStyle(ChatFormatting.YELLOW);
    }
}
