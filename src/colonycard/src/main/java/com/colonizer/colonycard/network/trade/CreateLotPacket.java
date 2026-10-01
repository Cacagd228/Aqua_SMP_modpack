package com.colonizer.colonycard.network.trade;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.trade.TradeTerminalManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -&gt; server: игрок подтвердил лот (предмет, количество, цена) и место,
 * где стоит терминал. Товар не забирается — лот это объявление, сделка
 * совершается вживую по координатам.
 */
public class CreateLotPacket implements CustomPacketPayload {

    public static final Type<CreateLotPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "create_lot"));

    public static final StreamCodec<FriendlyByteBuf, CreateLotPacket> STREAM_CODEC =
            StreamCodec.ofMember(CreateLotPacket::encode, CreateLotPacket::decode);

    private static final int MAX_PRICE = 1_000_000_000;

    public final String itemId;
    public final int count;
    public final int price;
    public final String dim;
    public final BlockPos pos;

    public CreateLotPacket(String itemId, int count, int price, String dim, BlockPos pos) {
        this.itemId = itemId;
        this.count = count;
        this.price = price;
        this.dim = dim;
        this.pos = pos;
    }

    @Override
    public Type<CreateLotPacket> type() {
        return TYPE;
    }

    public static void encode(CreateLotPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.itemId, 128);
        buf.writeVarInt(msg.count);
        buf.writeVarInt(msg.price);
        buf.writeUtf(msg.dim, 128);
        buf.writeVarInt(msg.pos.getX());
        buf.writeVarInt(msg.pos.getY());
        buf.writeVarInt(msg.pos.getZ());
    }

    public static CreateLotPacket decode(FriendlyByteBuf buf) {
        String itemId = buf.readUtf(128);
        int count = buf.readVarInt();
        int price = Math.min(buf.readVarInt(), MAX_PRICE);
        String dim = buf.readUtf(128);
        int x = buf.readVarInt();
        int y = buf.readVarInt();
        int z = buf.readVarInt();
        return new CreateLotPacket(itemId, count, price, dim, new BlockPos(x, y, z));
    }

    public static void handle(CreateLotPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> TradeTerminalManager.createLot(ctx.player(), msg));
    }
}
