package com.colonizer.colonycard.stage;

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

/**
 * Этап «Запросы короны»: единый на сервер список задач на сбор предметов.
 * Хранится в SavedData оверворлда, прогресс общий для всех игроков.
 */
public class StageSavedData extends SavedData {
    public static final Codec<StageSavedData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            StageTask.CODEC.listOf().fieldOf("tasks").forGetter(d -> List.copyOf(d.tasks)),
            Codec.BOOL.optionalFieldOf("notified", false).forGetter(d -> d.notified)
    ).apply(inst, StageSavedData::new));

    private static final String ID = "crown_stage";

    private static final SavedData.Factory<StageSavedData> FACTORY =
            new SavedData.Factory<>(StageSavedData::new, StageSavedData::load);

    private final List<StageTask> tasks = new ArrayList<>();
    /** Уже отправляли уведомление о закрытии текущего набора задач. */
    private boolean notified;

    public StageSavedData() {
    }

    private StageSavedData(List<StageTask> tasks, boolean notified) {
        this.tasks.addAll(tasks);
        this.notified = notified;
    }

    public static StageSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, ID);
    }

    public static StageSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag data = tag.contains("data", Tag.TAG_COMPOUND) ? tag.getCompound("data") : tag;
        return CODEC.parse(NbtOps.INSTANCE, data).result().orElseGet(StageSavedData::new);
    }

    /** Сохранение через CODEC (симметрично {@link #load}). */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        Tag encoded = CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        if (encoded instanceof CompoundTag compound) {
            tag.merge(compound);
        }
        return tag;
    }

    public List<StageTask> tasks() {
        return Collections.unmodifiableList(tasks);
    }

    public boolean notified() {
        return notified;
    }

    public void setNotified(boolean value) {
        if (this.notified != value) {
            this.notified = value;
            setDirty();
        }
    }

    public void addTask(String itemId, int needed) {
        tasks.add(new StageTask(itemId, needed, 0));
        // Новый набор задач — уведомление о закрытии нужно отправить заново.
        notified = false;
        setDirty();
    }

    public boolean removeTask(int index) {
        if (index < 0 || index >= tasks.size()) {
            return false;
        }
        tasks.remove(index);
        setDirty();
        return true;
    }

    public void clear() {
        if (!tasks.isEmpty()) {
            tasks.clear();
            setDirty();
        }
    }

    /** Добавить пожертвование к задаче. Возвращает фактически зачтённое количество. */
    public int donate(int index, int amount) {
        if (index < 0 || index >= tasks.size() || amount <= 0) {
            return 0;
        }
        StageTask task = tasks.get(index);
        int accepted = Math.min(amount, task.remaining());
        if (accepted <= 0) {
            return 0;
        }
        tasks.set(index, task.withDonated(task.donated() + accepted));
        setDirty();
        return accepted;
    }

    public boolean isComplete() {
        return !tasks.isEmpty() && tasks.stream().allMatch(StageTask::isComplete);
    }
}
