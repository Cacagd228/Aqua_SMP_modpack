package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import xyz.lineage.LineageCore;

/** Grave-scented: village guardians and wolves bristle at the scent. */
public final class Beastcall implements Trait {
    private final ResourceLocation sigil;

    public Beastcall(String name) {
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
        if (player.tickCount % 40 != 0) {
            return;
        }
        for (IronGolem sentinel : player.serverLevel().getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(16.0))) {
            if (sentinel.getTarget() == null) {
                sentinel.setTarget(player);
            }
        }
        for (Wolf hound : player.serverLevel().getEntitiesOfClass(Wolf.class, player.getBoundingBox().inflate(16.0))) {
            if (hound.getTarget() == null) {
                hound.setTarget(player);
            }
        }
    }
}
