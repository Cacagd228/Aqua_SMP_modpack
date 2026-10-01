package com.colonizer.colonycard.network.trade;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.trade.TradeTerminalManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Client -&gt; server: снять свой лот. Владельца проверяет сервер. */
public class RemoveLotPacket implements CustomPacketPayload {

    public static final Type<RemoveLotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "remove_lot"));

    public static final StreamCodec<FriendlyByteBuf, RemoveLotPacket> STREAM_CODEC =
            StreamCodec.ofMember(RemoveLotPacket::encode, RemoveLotPacket::decode);

    public final UUID lotId;

    public RemoveLotPacket(UUID lotId) {
        this.lotId = lotId;
    }

    @Override
    public Type<RemoveLotPacket> type() {
        return TYPE;
    }

    public static void encode(RemoveLotPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.lotId);
    }

    public static RemoveLotPacket decode(FriendlyByteBuf buf) {
        return new RemoveLotPacket(buf.readUUID());
    }

    public static void handle(RemoveLotPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> TradeTerminalManager.removeLot(ctx.player(), msg.lotId));
    }
}
