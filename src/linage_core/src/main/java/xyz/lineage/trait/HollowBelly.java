package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xyz.lineage.LineageCore;

/** Hollow belly: the gloom burns calories faster than bread restores. */
public final class HollowBelly implements Trait {
    private final ResourceLocation sigil;

    public HollowBelly(String name) {
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
    public boolean burden() {
        return true;
    }

    @Override
    public void pulse(ServerPlayer player) {
        if (player.tickCount % 100 == 0) {
            player.causeFoodExhaustion(2.0F);
        }
    }
}
