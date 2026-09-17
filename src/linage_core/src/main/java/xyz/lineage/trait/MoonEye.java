package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import xyz.lineage.LineageCore;

/** The archmage's gaze: everything that crawls nearby is laid bare. */
public final class MoonEye implements Trait {
    private final ResourceLocation sigil;

    public MoonEye(String name) {
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
        if (player.tickCount % 20 != 0) {
            return;
        }
        AABB field = player.getBoundingBox().inflate(24.0);
        for (var soul : player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, field)) {
            if (soul != player) {
                soul.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, true));
            }
        }
    }
}
