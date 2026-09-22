package com.colonizer.colonycard.data;

import com.colonizer.colonycard.ColonyCardMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    private ModAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ColonyCardMod.MODID);

    public static final Supplier<AttachmentType<ColonistData>> COLONIST_DATA = ATTACHMENT_TYPES.register(
            "colonist_data",
            () -> AttachmentType.builder(() -> ColonistData.DEFAULT)
                    .serialize(ColonistData.CODEC)
                    .copyOnDeath()
                    .build()
    );
}
