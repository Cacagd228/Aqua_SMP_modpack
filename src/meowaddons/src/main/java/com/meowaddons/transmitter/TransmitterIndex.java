package com.meowaddons.transmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Реестр всех передатчиков уровня: позиция -> собственный адрес.
 * SavedData хранится на каждый мир (измерение), поэтому поиск автоматически
 * ограничен тем же измерением, что и отправитель.
 */
public class TransmitterIndex extends SavedData {
    private static final String DATA_NAME = "meowaddons_transmitters";

    private final Map<BlockPos, String> addresses = new HashMap<>();

    public static TransmitterIndex get(Level level) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            throw new IllegalStateException("TransmitterIndex доступен только на сервере");
        }
        return serverLevel.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }

    private static net.minecraft.world.level.saveddata.SavedData.Factory<TransmitterIndex> factory() {
        return new net.minecraft.world.level.saveddata.SavedData.Factory<>(
                TransmitterIndex::new, TransmitterIndex::load,
                net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);
    }

    public void register(BlockPos pos, String address) {
        if (address == null || address.isBlank()) {
            addresses.remove(pos);
        } else {
            addresses.put(pos.immutable(), address);
        }
        setDirty();
    }

    public void unregister(BlockPos pos) {
        if (addresses.remove(pos) != null) {
            setDirty();
        }
    }

    /** Все позиции передатчиков, чей адрес матчится по glob-правилам Create. */
    public List<BlockPos> findMatches(Predicate<String> matcher) {
        List<BlockPos> result = new ArrayList<>();
        for (Map.Entry<BlockPos, String> e : addresses.entrySet()) {
            if (matcher.test(e.getValue())) {
                result.add(e.getKey());
            }
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, String> e : addresses.entrySet()) {
            CompoundTag entry = new CompoundTag();
            BlockPos pos = e.getKey();
            entry.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
            entry.putString("address", e.getValue());
            list.add(entry);
        }
        tag.put("transmitters", list);
        return tag;
    }

    private static TransmitterIndex load(CompoundTag tag, HolderLookup.Provider registries) {
        TransmitterIndex index = new TransmitterIndex();
        ListTag list = tag.getList("transmitters", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int[] xyz = entry.getIntArray("pos");
            if (xyz.length != 3) continue;
            index.addresses.put(new BlockPos(xyz[0], xyz[1], xyz[2]), entry.getString("address"));
        }
        return index;
    }
}
