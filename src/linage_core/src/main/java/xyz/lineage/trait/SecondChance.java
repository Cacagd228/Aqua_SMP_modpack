package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import xyz.lineage.LineageCore;
import xyz.lineage.data.SoulLedger;
import xyz.lineage.registry.SoulAttachments;

/**
 * A soul jar cheats the reaper: on a ten-minute wheel the bearer
 * refuses death and rises whole again.
 */
public final class SecondChance implements Trait {
    private static final long WHEEL_MS = 600_000L;
    private final ResourceLocation sigil;

    public SecondChance(String name) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
    }

    @Override
    public ResourceLocation sigil() {
        return sigil;
    }

    @Override
    public Component title() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath());
    }

    @Override
    public Component lore() {
        return Component.translatable("trait." + LineageCore.MOD_ID + "." + sigil.getPath() + ".desc");
    }

    @Override
    public void perish(ServerPlayer player, LivingDeathEvent event) {
        SoulLedger ledger = player.getData(SoulAttachments.SOUL);
        long now = System.currentTimeMillis();
        if (now - ledger.wardAt() < WHEEL_MS) {
            return;
        }
        ledger.wardAt(now);
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        player.sendSystemMessage(Component.translatable("message." + LineageCore.MOD_ID + ".ward_awakens"));
    }
}
