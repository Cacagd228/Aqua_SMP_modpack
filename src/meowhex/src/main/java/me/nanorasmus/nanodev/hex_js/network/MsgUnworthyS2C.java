package me.nanorasmus.nanodev.hex_js.network;

import at.petrak.hexcasting.common.msgs.IMessage;
import io.netty.buffer.ByteBuf;
import me.nanorasmus.nanodev.hex_js.HexJS;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: кастер познал Истинное Имя и оказался недостоин.
 * В конец лога пишется приговор, затем игра закрывается.
 * Вызывается из enqueueWork, уже на клиенте.
 */
public record MsgUnworthyS2C() implements IMessage {

    public static final ResourceLocation ID = HexJS.modLoc("unworthy");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
    }

    public static MsgUnworthyS2C deserialize(ByteBuf buffer) {
        return new MsgUnworthyS2C();
    }

    public static void handle(MsgUnworthyS2C msg) {
        try {
            HexJS.LOGGER.error("ТЫ НЕ ДОСТОЕН");
        } catch (Throwable ignored) {
        }
        try {
            var mc = net.minecraft.client.Minecraft.getInstance();
            mc.stop();
        } catch (Throwable ignored) {
        }
    }
}
