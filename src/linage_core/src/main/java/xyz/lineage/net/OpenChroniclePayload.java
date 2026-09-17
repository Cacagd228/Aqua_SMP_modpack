package xyz.lineage.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import xyz.lineage.LineageCore;

public record OpenChroniclePayload(Kind kind) implements CustomPacketPayload {
    public enum Kind {
        OATH, ASCEND, PROFILE
    }

    public static final Type<OpenChroniclePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "open_chronicle"));
    public static final StreamCodec<ByteBuf, OpenChroniclePayload> STREAM = StreamCodec.composite(
        ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal), OpenChroniclePayload::kind, OpenChroniclePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
