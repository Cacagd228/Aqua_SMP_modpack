package xyz.lineage.trait;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xyz.lineage.LineageCore;

/** Gloam mend: dim hollows knit pale flesh back together. */
public final class GloamMend implements Trait {
    private final ResourceLocation sigil;

    public GloamMend(String name) {
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
        if (player.tickCount % 200 == 0
            && player.level().getMaxLocalRawBrightness(BlockPos.containing(player.getEyePosition())) <= 7) {
            player.heal(2.0F);
        }
    }
}
