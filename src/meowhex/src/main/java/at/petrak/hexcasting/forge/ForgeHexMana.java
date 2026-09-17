package at.petrak.hexcasting.forge;

import at.petrak.hexcasting.api.misc.ManaHelper;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class ForgeHexMana {
    /**
     * The synced, serialized mana pool. Registered as {@code hexcasting:mana}
     * on the {@code neoforge:attachment_types} registry.
     */
    public static final AttachmentType<Double> MANA = AttachmentType.<Double>builder(() -> 0.0)
        .serialize(Codec.doubleRange(ManaHelper.MIN_MANA, ManaHelper.STORAGE_MAX_MANA))
        .sync(ByteBufCodecs.DOUBLE)
        .build();

    private ForgeHexMana() {}
}
