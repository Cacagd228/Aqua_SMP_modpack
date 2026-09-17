package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import xyz.lineage.LineageCore;

/** The withered court: rot clings to nearby monsters. */
public final class WiltAura implements Trait {
    private final ResourceLocation sigil;

    public WiltAura(String name) {
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
        AABB field = player.getBoundingBox().inflate(8.0);
        for (var foe : player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, field)) {
            foe.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
        }
    }
}
