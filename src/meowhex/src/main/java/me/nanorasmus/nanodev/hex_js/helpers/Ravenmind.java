package me.nanorasmus.nanodev.hex_js.helpers;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

/**
 * Best-effort bridge to the 1.21 "ravenmind". In the 1.21 VM the second value of
 * a double cast lives inside the parenthesis machinery and is stashed on the
 * casting image's {@code userData} under {@link HexAPI#RAVENMIND_USERDATA}.
 *
 * <p>Custom spells are unary pattern operations, so they are almost never fed a
 * ravenmind; {@link #read} returns whatever is stashed in the slot (usually
 * {@code null}), and {@link #write} round-trips whatever the script hands to
 * {@code setRavenmind(...)} back into the image so downstream machinery can still
 * observe it. The KubeJS API keeps its 1.19 shape for compatibility.
 */
public final class Ravenmind {
    private Ravenmind() {
    }

    @Nullable
    public static Iota read(CastingImage image, ServerLevel world) {
        if (image == null || world == null) {
            return null;
        }
        CompoundTag userData = image.getUserData();
        if (userData == null) {
            return null;
        }
        Tag raw = userData.get(HexAPI.RAVENMIND_USERDATA);
        if (!(raw instanceof CompoundTag serialized)) {
            return null;
        }
        try {
            return IotaType.deserialize(serialized, world);
        } catch (RuntimeException e) {
            HexJS.LOGGER.warn("[HexJS] could not restore ravenmind iota: {}", e.toString());
            return null;
        }
    }

    /**
     * Returns a copy of {@code source} with the ravenmind slot set to {@code iota},
     * or removed when {@code iota} is {@code null}.
     */
    public static CompoundTag write(@Nullable CompoundTag source, @Nullable Iota iota) {
        CompoundTag out = source == null ? new CompoundTag() : source.copy();
        if (iota == null) {
            out.remove(HexAPI.RAVENMIND_USERDATA);
        } else {
            out.put(HexAPI.RAVENMIND_USERDATA, IotaType.serialize(iota));
        }
        return out;
    }
}