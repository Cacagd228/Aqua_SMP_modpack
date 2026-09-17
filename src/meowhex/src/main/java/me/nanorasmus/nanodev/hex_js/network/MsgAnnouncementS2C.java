package me.nanorasmus.nanodev.hex_js.network;

import at.petrak.hexcasting.common.msgs.IMessage;
import io.netty.buffer.ByteBuf;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.client.ClientChatHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: asks the client to open the chat input pre-filled with the
 * given text (used by the "Оглашение" spell).
 */
public record MsgAnnouncementS2C(String text) implements IMessage {

    public static final ResourceLocation ID = HexJS.modLoc("announcement");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeUtf(this.text);
    }

    public static MsgAnnouncementS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        return new MsgAnnouncementS2C(buf.readUtf());
    }

    public static void handle(MsgAnnouncementS2C msg) {
        ClientChatHandler.openChatInput(msg.text());
    }
}