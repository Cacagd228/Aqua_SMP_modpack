package xyz.lineage.registry;

import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import xyz.lineage.LineageCore;
import xyz.lineage.data.SoulLedger;

public final class SoulAttachments {
    public static final DeferredRegister<AttachmentType<?>> TYPES =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LineageCore.MOD_ID);

    public static final Supplier<AttachmentType<SoulLedger>> SOUL = TYPES.register(
        "soul_ledger", () -> AttachmentType.builder(SoulLedger::new).serialize(SoulLedger.CODEC).copyOnDeath().build());

    private SoulAttachments() {
    }
}
