package me.nanorasmus.nanodev.hex_js.network;

import at.petrak.hexcasting.common.msgs.IMessage;
import io.netty.buffer.ByteBuf;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.client.ManaPairClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Server -> client: состояние общего манапула для синего HUD. */
public record MsgManaPairS2C(boolean paired, double shared, double sharedMax) implements IMessage {

    public static final ResourceLocation ID = HexJS.modLoc("mana_pair");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeBoolean(this.paired);
        buf.writeDouble(this.shared);
        buf.writeDouble(this.sharedMax);
    }

    public static MsgManaPairS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        return new MsgManaPairS2C(buf.readBoolean(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(MsgManaPairS2C msg) {
        ManaPairClientCache.update(msg.paired(), msg.shared(), msg.sharedMax());
    }
}
