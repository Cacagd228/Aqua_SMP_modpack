package xyz.lineage.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import xyz.lineage.LineageCore;
import xyz.lineage.data.SoulLedger;

public record SyncSoulPayload(SoulLedger ledger) implements CustomPacketPayload {
    public static final Type<SyncSoulPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, "sync_soul"));
    public static final StreamCodec<ByteBuf, SyncSoulPayload> STREAM = StreamCodec.composite(
        SoulLedger.STREAM, SyncSoulPayload::ledger, SyncSoulPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
