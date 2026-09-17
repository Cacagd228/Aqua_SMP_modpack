package xyz.lineage.trait;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import xyz.lineage.LineageCore;

/** Soft mortal clay: steel and flame bite deeper than they should. */
public final class FrailBlood implements Trait {
    private final ResourceLocation sigil;

    public FrailBlood(String name) {
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
    public float sting(ServerPlayer player, DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)
            || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return amount * 1.15F;
        }
        return amount;
    }
}
