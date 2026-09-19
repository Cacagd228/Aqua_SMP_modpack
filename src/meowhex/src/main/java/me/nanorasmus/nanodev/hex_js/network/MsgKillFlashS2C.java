package me.nanorasmus.nanodev.hex_js.network;

import at.petrak.hexcasting.common.msgs.IMessage;
import io.netty.buffer.ByteBuf;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.client.KillFlashOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: красная вспышка виньетки после PvP-фрага
 * (свой оверлей «как worldborder», но без видимых стен рамки).
 */
public record MsgKillFlashS2C() implements IMessage {

    public static final ResourceLocation ID = HexJS.modLoc("kill_flash");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
    }

    public static MsgKillFlashS2C deserialize(ByteBuf buffer) {
        return new MsgKillFlashS2C();
    }

    public static void handle(MsgKillFlashS2C msg) {
        KillFlashOverlay.trigger();
    }
}
