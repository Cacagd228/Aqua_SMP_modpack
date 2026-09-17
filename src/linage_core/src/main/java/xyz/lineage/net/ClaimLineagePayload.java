package xyz.lineage.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import xyz.lineage.LineageCore;

public record ClaimLineagePayload(ResourceLocation lineage) implements CustomPacketPayload {
    public static final Type<ClaimLineagePayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "claim_lineage"));
    public static final StreamCodec<ByteBuf, ClaimLineagePayload> STREAM = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, ClaimLineagePayload::lineage, ClaimLineagePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
