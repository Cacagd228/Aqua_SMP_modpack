package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xyz.lineage.LineageCore;

/** Living stone shrugs off alchemy, boon and bane alike. */
public final class PotionPurge implements Trait {
    private final ResourceLocation sigil;

    public PotionPurge(String name) {
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
    public void pulse(ServerPlayer player) {
        if (player.tickCount % 10 == 0) {
            player.removeAllEffects();
        }
    }
}
