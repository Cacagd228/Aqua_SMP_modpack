package com.colonizer.colonycard.trade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Торговый терминал: общий на весь сервер список лотов + процент комиссии
 * за размещение. Хранится в SavedData оверворлда.
 */
public class TradeSavedData extends SavedData {

    /** Комиссия за размещение лота в процентах от цены, если не задана командой. */
    public static final int DEFAULT_FEE_PERCENT = 10;

    public static final Codec<TradeSavedData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            TradeLot.CODEC.listOf().fieldOf("lots").forGetter(d -> List.copyOf(d.lots)),
            Codec.INT.optionalFieldOf("feePercent", DEFAULT_FEE_PERCENT).forGetter(d -> d.feePercent)
    ).apply(inst, TradeSavedData::new));

    private static final String ID = "trade_terminal";

    private static final SavedData.Factory<TradeSavedData> FACTORY =
            new SavedData.Factory<>(TradeSavedData::new, TradeSavedData::load);

    private final List<TradeLot> lots = new ArrayList<>();
    private int feePercent = DEFAULT_FEE_PERCENT;

    public TradeSavedData() {
    }

    private TradeSavedData(List<TradeLot> lots, int feePercent) {
        this.lots.addAll(lots);
        this.feePercent = feePercent;
    }

    public static TradeSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public static TradeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag data = tag.contains("data", Tag.TAG_COMPOUND) ? tag.getCompound("data") : tag;
        return CODEC.parse(NbtOps.INSTANCE, data).result().orElseGet(TradeSavedData::new);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Tag encoded = CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        if (encoded instanceof CompoundTag compound) {
            tag.merge(compound);
        }
        return tag;
    }

    /** Все лоты, новые сверху. */
    public List<TradeLot> lots() {
        List<TradeLot> copy = new ArrayList<>(lots);
        copy.sort((a, b) -> Long.compare(b.createdAt(), a.createdAt()));
        return Collections.unmodifiableList(copy);
    }

    public int lotCount() {
        return lots.size();
    }

    public int countBy(UUID seller) {
        int n = 0;
        for (TradeLot lot : lots) {
            if (lot.sellerId().equals(seller)) {
                n++;
            }
        }
        return n;
    }

    public void add(TradeLot lot) {
        lots.add(lot);
        setDirty();
    }

    public boolean remove(UUID id) {
        boolean removed = lots.removeIf(lot -> lot.id().equals(id));
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public int removeBySeller(UUID seller) {
        int before = lots.size();
        lots.removeIf(lot -> lot.sellerId().equals(seller));
        int removed = before - lots.size();
        if (removed > 0) {
            setDirty();
        }
        return removed;
    }

    public void clear() {
        if (!lots.isEmpty()) {
            lots.clear();
            setDirty();
        }
    }

    public int feePercent() {
        return feePercent;
    }

    public void setFeePercent(int value) {
        int clamped = Math.max(0, Math.min(100, value));
        if (this.feePercent != clamped) {
            this.feePercent = clamped;
            setDirty();
        }
    }

    /** Комиссия за лот указанной цены: округление вверх, чтобы лот не был бесплатным. */
    public int feeFor(int price) {
        if (price <= 0 || feePercent <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) price * feePercent / 100.0D);
    }
}
