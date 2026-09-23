package com.colonizer.colonycard.stage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Одна задача этапа: какой предмет собираем, сколько надо и сколько уже положили. */
public record StageTask(String itemId, int needed, int donated) {
    public static final Codec<StageTask> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("item").forGetter(StageTask::itemId),
            Codec.INT.fieldOf("needed").forGetter(StageTask::needed),
            Codec.INT.fieldOf("donated").forGetter(StageTask::donated)
    ).apply(inst, StageTask::new));

    public StageTask {
        needed = Math.max(1, needed);
        donated = Math.max(0, donated);
    }

    public int remaining() {
        return Math.max(0, needed - donated);
    }

    public boolean isComplete() {
        return donated >= needed;
    }

    public StageTask withDonated(int newDonated) {
        return new StageTask(itemId, needed, newDonated);
    }
}
