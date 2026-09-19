package me.nanorasmus.nanodev.hex_js.network;

import at.petrak.hexcasting.common.msgs.IMessage;
import io.netty.buffer.ByteBuf;
import me.nanorasmus.nanodev.hex_js.HexJS;
import me.nanorasmus.nanodev.hex_js.client.AnnounceTextOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: текстовый анонс фрага на месте боссбара
 * (верх экрана, просто текст без полосы). Пустые строки / 0 — отсутствие части.
 */
public record MsgAnnounceTextS2C(String killer, String victim, String eventId,
                                 String shutName, int shutStreak, int mult) implements IMessage {

    public static final ResourceLocation ID = HexJS.modLoc("announce_text");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeUtf(this.killer);
        buf.writeUtf(this.victim);
        buf.writeUtf(this.eventId);
        buf.writeUtf(this.shutName);
        buf.writeInt(this.shutStreak);
        buf.writeInt(this.mult);
    }

    public static MsgAnnounceTextS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        return new MsgAnnounceTextS2C(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readInt());
    }

    public static void handle(MsgAnnounceTextS2C msg) {
        AnnounceTextOverlay.show(msg.killer(), msg.victim(), msg.eventId(), msg.shutName(), msg.shutStreak(), msg.mult());
    }
}
