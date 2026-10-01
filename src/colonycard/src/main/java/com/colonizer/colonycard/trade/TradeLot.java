package com.colonizer.colonycard.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Один лот торгового терминала: что продаётся, за сколько и где стоит продавец.
 *
 * <p>Цена хранится целым числом в базовых единицах Numismatics (шпорах) —
 * форматируется в монеты на клиенте через {@link NumismaticsMoney}.
 * Координаты — это место, где продавец выставил товар, а не где он сейчас стоит.
 */
public record TradeLot(UUID id,
                       String itemId,
                       int count,
                       int price,
                       UUID sellerId,
                       String sellerName,
                       String dimension,
                       int x, int y, int z,
                       long createdAt) {

    public static final Codec<TradeLot> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(TradeLot::id),
            Codec.STRING.fieldOf("item").forGetter(TradeLot::itemId),
            Codec.INT.fieldOf("count").forGetter(TradeLot::count),
            Codec.INT.fieldOf("price").forGetter(TradeLot::price),
            UUIDUtil.CODEC.fieldOf("seller").forGetter(TradeLot::sellerId),
            Codec.STRING.fieldOf("sellerName").forGetter(TradeLot::sellerName),
            Codec.STRING.fieldOf("dim").forGetter(TradeLot::dimension),
            Codec.INT.fieldOf("x").forGetter(TradeLot::x),
            Codec.INT.fieldOf("y").forGetter(TradeLot::y),
            Codec.INT.fieldOf("z").forGetter(TradeLot::z),
            Codec.LONG.fieldOf("createdAt").forGetter(TradeLot::createdAt)
    ).apply(inst, TradeLot::new));

    public TradeLot {
        itemId = itemId.toLowerCase(java.util.Locale.ROOT);
        count = Math.max(1, count);
        price = Math.max(0, price);
    }

    /** «x1 y2 z3» — координаты терминала, где выставлен лот. */
    public String coords() {
        return x + " " + y + " " + z;
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }

    public ResourceLocation dimensionId() {
        return ResourceLocation.parse(dimension);
    }
}
