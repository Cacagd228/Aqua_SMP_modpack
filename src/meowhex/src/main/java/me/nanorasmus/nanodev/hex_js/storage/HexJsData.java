package me.nanorasmus.nanodev.hex_js.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-persisted HexJS configuration: the global {@link PatternList} plus one
 * {@link PatternList} per player (pattern black/white lists, redirects and
 * Bookkeeper's Gambit caps).
 *
 * <p>Loaded once per start ({@link #load}) and flushed on stop ({@link #save});
 * individual mutations route through {@code HexJsData} instances returned by
 * {@link #get()} and call {@link #markChanged()} so the level auto-saves them.
 * The serialised layout intentionally matches the older port's StorageManager
 * so existing worlds keep their gatekeeping config.
 */
public class HexJsData extends SavedData {
    // breaking-ренейм: было hex_js. Старые миры gatekeeping-конфиг не подхватят.
    public static final String SAVE_ID = "meowhex";

    private static HexJsData instance;

    private final PatternList global = new PatternList();
    private final Map<UUID, PatternList> players = new HashMap<>();

    public static void load(MinecraftServer server) {
        instance = server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(HexJsData::new, HexJsData::fromNbt, DataFixTypes.LEVEL),
                SAVE_ID);
    }

    public static void save(MinecraftServer server) {
        if (instance != null) {
            instance.setDirty();
            server.overworld().getDataStorage().set(SAVE_ID, instance);
        }
    }

    public static @Nullable HexJsData get() {
        return instance;
    }

    /** The shared global list. Never {@code null} for a live server. */
    public PatternList global() {
        return global;
    }

    /** The player's list, materialised on first access. */
    public PatternList player(UUID id) {
        return players.computeIfAbsent(id, ignored -> new PatternList());
    }

    /** The player's configured list, or {@code null} when nothing is stored yet. */
    public @Nullable PatternList playerOrNull(UUID id) {
        return players.get(id);
    }

    public void removePlayer(UUID id) {
        if (players.remove(id) != null) {
            markChanged();
        }
    }

    public void markChanged() {
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("saved_player_count", players.size());
        int index = 0;
        for (Map.Entry<UUID, PatternList> entry : players.entrySet()) {
            tag.putUUID("player_" + index, entry.getKey());
            writeList(tag, "player_" + index, entry.getValue());
            index++;
        }
        writeList(tag, "global", global);
        return tag;
    }

    public static HexJsData fromNbt(CompoundTag tag, HolderLookup.Provider provider) {
        HexJsData data = new HexJsData();
        int playerCount = tag.getInt("saved_player_count");
        for (int i = 0; i < playerCount; i++) {
            data.players.put(tag.getUUID("player_" + i), readList(tag, "player_" + i));
        }
        PatternList loadedGlobal = readList(tag, "global");
        // Legacy pre-1.0 key for the global Bookkeeper cap.
        if (!tag.contains("global_max_bookkeepers_length") && tag.contains("global_max_bookkeeper")) {
            loadedGlobal.setMaxBookkeepersLength(tag.getInt("global_max_bookkeeper"));
        }
        data.global.copyFrom(loadedGlobal);
        return data;
    }

    private static void writeList(CompoundTag nbt, String prefix, PatternList list) {
        nbt.putBoolean(prefix + "_is_whitelist", list.isWhitelist());
        nbt.putInt(prefix + "_pattern_count", list.signatures().size());
        int i = 0;
        for (String signature : list.signatures()) {
            nbt.putString(prefix + "_pattern_" + i, signature);
            i++;
        }
        nbt.putInt(prefix + "_redirect_count", list.redirects().size());
        i = 0;
        for (Map.Entry<String, String> redirect : list.redirects().entrySet()) {
            nbt.putString(prefix + "_redirect_" + i + "_input", redirect.getKey());
            nbt.putString(prefix + "_redirect_" + i + "_output", redirect.getValue());
            i++;
        }
        nbt.putInt(prefix + "_max_bookkeepers_length", list.maxBookkeepersLength());
    }

    private static PatternList readList(CompoundTag nbt, String prefix) {
        PatternList list = new PatternList(nbt.getBoolean(prefix + "_is_whitelist"));
        int patternCount = nbt.getInt(prefix + "_pattern_count");
        for (int i = 0; i < patternCount; i++) {
            list.addPattern(nbt.getString(prefix + "_pattern_" + i));
        }
        int redirectCount = nbt.getInt(prefix + "_redirect_count");
        for (int i = 0; i < redirectCount; i++) {
            list.addRedirect(nbt.getString(prefix + "_redirect_" + i + "_input"),
                    nbt.getString(prefix + "_redirect_" + i + "_output"));
        }
        if (nbt.contains(prefix + "_max_bookkeepers_length")) {
            list.setMaxBookkeepersLength(nbt.getInt(prefix + "_max_bookkeepers_length"));
        }
        return list;
    }
}