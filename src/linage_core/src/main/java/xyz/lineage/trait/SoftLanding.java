package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import xyz.lineage.LineageCore;

/** Unbroken falls: the cat-footed never fear the drop. */
public final class SoftLanding implements Trait {
    private final ResourceLocation sigil;

    public SoftLanding(String name) {
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
    public float sting(ServerPlayer player, DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            return 0.0F;
        }
        return amount;
    }

    @Override
    public void strike(ServerPlayer player, LivingEntity target, float damage) {
    }
}
