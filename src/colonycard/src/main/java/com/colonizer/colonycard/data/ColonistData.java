package com.colonizer.colonycard.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * All persistent data for a single colonist (player).
 * Stored server-side as a data attachment on the player, synced to the
 * owning client via {@link com.colonizer.colonycard.network.SyncColonistDataPacket}.
 */
public record ColonistData(
        String colonizerName,
        long arrivalDate,
        String arrivalGoal,
        List<String> traits,
        int loyalty,
        int contribution,
        int rewardsMask,
        String archipelago
) {
    public static final int REWARD_COUNT = 6;

    public static final String DEFAULT_ARCHIPELAGO = "";

    public static final Codec<ColonistData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("colonizerName").forGetter(ColonistData::colonizerName),
            Codec.LONG.fieldOf("arrivalDate").forGetter(ColonistData::arrivalDate),
            Codec.STRING.fieldOf("arrivalGoal").forGetter(ColonistData::arrivalGoal),
            Codec.STRING.listOf().fieldOf("traits").forGetter(ColonistData::traits),
            Codec.INT.fieldOf("loyalty").forGetter(ColonistData::loyalty),
            Codec.INT.fieldOf("contribution").forGetter(ColonistData::contribution),
            Codec.INT.fieldOf("rewardsMask").forGetter(ColonistData::rewardsMask),
            // Новое поле паспорта ("Наименование Архипелага"). Optional — старые сейвы без него погрузятся.
            Codec.STRING.optionalFieldOf("archipelago", DEFAULT_ARCHIPELAGO).forGetter(ColonistData::archipelago)
    ).apply(inst, ColonistData::new));

    /** Sentinel used to detect "never initialized" (arrivalDate == 0). */
    public static final ColonistData DEFAULT = new ColonistData("", 0L, "", List.of(), 0, 0, 0, DEFAULT_ARCHIPELAGO);

    public boolean initialized() {
        return arrivalDate != 0L;
    }

    public boolean hasReward(int index) {
        return (rewardsMask & (1 << index)) != 0;
    }

    public ColonistData withReward(int index, boolean unlocked) {
        int mask = unlocked ? (rewardsMask | (1 << index)) : (rewardsMask & ~(1 << index));
        return new ColonistData(colonizerName, arrivalDate, arrivalGoal, traits, loyalty, contribution, mask, archipelago);
    }

    /** Loyalty runs -100 (hostile) .. 0 (neutral) .. +100 (devoted) -- matches the card's on-screen scale exactly. */
    public ColonistData withLoyalty(int newLoyalty) {
        return new ColonistData(colonizerName, arrivalDate, arrivalGoal, traits, Mth.clamp(newLoyalty, -100, 100), contribution, rewardsMask, archipelago);
    }

    public ColonistData withContribution(int newContribution) {
        return new ColonistData(colonizerName, arrivalDate, arrivalGoal, traits, loyalty, Math.max(0, newContribution), rewardsMask, archipelago);
    }

    public ColonistData withColonizerName(String newName) {
        return new ColonistData(newName, arrivalDate, arrivalGoal, traits, loyalty, contribution, rewardsMask, archipelago);
    }

    public ColonistData withArchipelago(String newArchipelago) {
        return new ColonistData(colonizerName, arrivalDate, arrivalGoal, traits, loyalty, contribution, rewardsMask, newArchipelago);
    }

    public ColonistData withTraits(List<String> newTraits) {
        return new ColonistData(colonizerName, arrivalDate, arrivalGoal, List.copyOf(newTraits), loyalty, contribution, rewardsMask, archipelago);
    }
}
