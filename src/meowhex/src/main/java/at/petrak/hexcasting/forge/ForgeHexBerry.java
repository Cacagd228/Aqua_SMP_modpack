package at.petrak.hexcasting.forge;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;

public final class ForgeHexBerry {
    /**
     * How many mana berries the player has eaten. Registered as
     * {@code meowhex:berry_count} on the {@code neoforge:attachment_types}
     * registry. Survives death (copyOnDeath); server-side only, no sync —
     * the HUD reads the MANA_MAX attribute, which syncs on its own.
     */
    public static final AttachmentType<Integer> BERRIES = AttachmentType.<Integer>builder(() -> 0)
        .serialize(Codec.intRange(0, 1_000_000))
        .copyOnDeath()
        .build();

    private ForgeHexBerry() {}
}
