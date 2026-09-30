package at.petrak.hexcasting.api.casting.iota;

import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A reference to an {@link IotaType} -- "what kind of thing is this", without
 * carrying a value of that kind. Ported from the MoreIotas addon (MIT,
 * Talia-12).
 * <p>
 * This is the iota that lets a spell be generic over its own operands, and the
 * one the pattern-matching op consumes.
 */
public class IotaTypeIota extends Iota {
    public IotaTypeIota(@NotNull IotaType<?> datum) {
        super(HexIotaTypes.IOTA_TYPE, datum);
    }

    public IotaType<?> getIotaType() {
        return (IotaType<?>) this.payload;
    }

    public ResourceLocation getId() {
        return HexIotaTypes.REGISTRY.getKey(this.getIotaType());
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    public boolean toleratesOther(Iota that) {
        return typesMatch(this, that)
            && that instanceof IotaTypeIota titiota
            && this.getIotaType() == titiota.getIotaType();
    }

    @Override
    public @NotNull Tag serialize() {
        var id = this.getId();
        if (id == null) {
            throw new IllegalStateException("Tried to serialize an unregistered iota type");
        }
        return StringTag.valueOf(id.toString());
    }

    public static IotaType<IotaTypeIota> TYPE = new IotaType<>() {
        @Nullable
        @Override
        public IotaTypeIota deserialize(Tag tag, ServerLevel world) throws IllegalArgumentException {
            return IotaTypeIota.deserialize(tag);
        }

        @Override
        public Component display(Tag tag) {
            return IotaTypeIota.display(HexUtils.downcast(tag, StringTag.TYPE).getAsString());
        }

        @Override
        public int color() {
            return 0xff_cc99ff;
        }
    };

    public static IotaTypeIota deserialize(Tag tag) throws IllegalArgumentException {
        var id = ResourceLocation.parse(HexUtils.downcast(tag, StringTag.TYPE).getAsString());
        var type = HexIotaTypes.REGISTRY.getOptional(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown iota type " + id));
        return new IotaTypeIota(type);
    }

    public static Component display(String id) {
        return Component.literal(id).withStyle(ChatFormatting.LIGHT_PURPLE);
    }
}
