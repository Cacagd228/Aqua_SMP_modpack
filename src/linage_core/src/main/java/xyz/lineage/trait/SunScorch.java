package xyz.lineage.trait;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import xyz.lineage.LineageCore;

/** The sun is cruel to pale blood: open daylight sets the bearer alight. */
public final class SunScorch implements Trait {
    private final ResourceLocation sigil;
    private final int rhythm;

    public SunScorch(String name, int rhythm) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
        this.rhythm = rhythm;
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
        if (player.tickCount % rhythm != 0) {
            return;
        }
        if (player.level().isDay() && player.level().canSeeSky(BlockPos.containing(player.getEyePosition()))) {
            player.setRemainingFireTicks(40);
        }
    }
}
