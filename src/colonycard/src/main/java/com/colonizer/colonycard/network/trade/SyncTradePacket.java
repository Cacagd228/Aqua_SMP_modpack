package com.colonizer.colonycard.network.trade;

import com.colonizer.colonycard.ColonyCardMod;
import com.colonizer.colonycard.client.ClientTradeCache;
import com.colonizer.colonycard.trade.TradeLot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server -&gt; client: список лотов терминала + (необязательно) команда открыть
 * меню в нужном режиме.
 *
 * <p>Режим {@link Mode#CREATE} дополнительно несёт контекст: предмет в руке
 * игрока, сколько его у него и где стоит терминал — этого хватает, чтобы
 * клиент показать форму лота, а серверу — записать координаты продавца.
 */
public class SyncTradePacket implements CustomPacketPayload {

    public enum Mode {
        /** Просто обновление списка, меню не трогаем. */
        NONE,
        /** Открыть терминал на вкладке товаров. */
        BROWSE,
        /** Открыть терминал с открытой формой выставления лота. */
        CREATE
    }

    public static final Type<SyncTradePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ColonyCardMod.MODID, "sync_trade"));

    public static final StreamCodec<FriendlyByteBuf, SyncTradePacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncTradePacket::encode, SyncTradePacket::decode);

    /** Сколько лотов одного продавца можно держать одновременно. */
    public static final int MAX_LOTS_PER_PLAYER = 12;
    /** Верхняя граница, чтобы клиент не получил гигантский пакет. */
    public static final int MAX_LOTS = 1024;

    public final List<TradeLot> lots;
    public final Mode mode;
    public final String formItemId;
    public final int formMaxCount;
    public final String formDim;
    public final int formX;
    public final int formY;
    public final int formZ;
    public final int feePercent;

    public SyncTradePacket(List<TradeLot> lots, Mode mode, String formItemId, int formMaxCount,
                           String formDim, BlockPos formPos, int feePercent) {
        this.lots = List.copyOf(lots);
        this.mode = mode;
        this.formItemId = formItemId;
        this.formMaxCount = formMaxCount;
        this.formDim = formDim;
        this.formX = formPos == null ? 0 : formPos.getX();
        this.formY = formPos == null ? 0 : formPos.getY();
        this.formZ = formPos == null ? 0 : formPos.getZ();
        this.feePercent = feePercent;
    }

    /** Обновление списка без открытия меню. */
    public static SyncTradePacket update(List<TradeLot> lots, int feePercent) {
        return new SyncTradePacket(lots, Mode.NONE, "", 0, "", null, feePercent);
    }

    /** Открыть меню на вкладке товаров. */
    public static SyncTradePacket browse(List<TradeLot> lots, int feePercent) {
        return new SyncTradePacket(lots, Mode.BROWSE, "", 0, "", null, feePercent);
    }

    /** Открыть меню с формой выставления лота по предмету из руки. */
    public static SyncTradePacket create(List<TradeLot> lots, String itemId, int maxCount,
                                         String dim, BlockPos pos, int feePercent) {
        return new SyncTradePacket(lots, Mode.CREATE, itemId, maxCount, dim, pos, feePercent);
    }

    @Override
    public Type<SyncTradePacket> type() {
        return TYPE;
    }

    public BlockPos formPos() {
        return new BlockPos(formX, formY, formZ);
    }

    public static void encode(SyncTradePacket msg, FriendlyByteBuf buf) {
        List<TradeLot> lots = msg.lots.size() > MAX_LOTS ? msg.lots.subList(0, MAX_LOTS) : msg.lots;
        buf.writeVarInt(lots.size());
        for (TradeLot lot : lots) {
            buf.writeUUID(lot.id());
            buf.writeUtf(lot.itemId(), 128);
            buf.writeVarInt(lot.count());
            buf.writeVarInt(lot.price());
            buf.writeUUID(lot.sellerId());
            buf.writeUtf(lot.sellerName(), 64);
            buf.writeUtf(lot.dimension(), 128);
            buf.writeVarInt(lot.x());
            buf.writeVarInt(lot.y());
            buf.writeVarInt(lot.z());
            buf.writeVarLong(lot.createdAt());
        }
        buf.writeByte(msg.mode.ordinal());
        buf.writeUtf(msg.formItemId, 128);
        buf.writeVarInt(msg.formMaxCount);
        buf.writeUtf(msg.formDim, 128);
        buf.writeVarInt(msg.formX);
        buf.writeVarInt(msg.formY);
        buf.writeVarInt(msg.formZ);
        buf.writeVarInt(msg.feePercent);
    }

    public static SyncTradePacket decode(FriendlyByteBuf buf) {
        int count = Math.min(buf.readVarInt(), MAX_LOTS);
        List<TradeLot> lots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID();
            String itemId = buf.readUtf(128);
            int lotCount = buf.readVarInt();
            int price = buf.readVarInt();
            UUID seller = buf.readUUID();
            String sellerName = buf.readUtf(64);
            String dim = buf.readUtf(128);
            int x = buf.readVarInt();
            int y = buf.readVarInt();
            int z = buf.readVarInt();
            long createdAt = buf.readVarLong();
            lots.add(new TradeLot(id, itemId, lotCount, price, seller, sellerName, dim, x, y, z, createdAt));
        }
        Mode[] modes = Mode.values();
        int modeOrdinal = Math.min(Math.max(buf.readByte(), 0), modes.length - 1);
        Mode mode = modes[modeOrdinal];
        String formItemId = buf.readUtf(128);
        int formMaxCount = buf.readVarInt();
        String formDim = buf.readUtf(128);
        int fx = buf.readVarInt();
        int fy = buf.readVarInt();
        int fz = buf.readVarInt();
        int feePercent = buf.readVarInt();
        return new SyncTradePacket(lots, mode, formItemId, formMaxCount, formDim,
                new BlockPos(fx, fy, fz), feePercent);
    }

    public static void handle(SyncTradePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientTradeCache.apply(msg));
    }
}
