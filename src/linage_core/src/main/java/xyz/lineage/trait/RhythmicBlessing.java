package xyz.lineage.trait;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import xyz.lineage.LineageCore;

/**
 * Bestows a status draught on a fixed rhythm while a circumstance holds.
 * Covers every "gain X effect every N ticks when ..." behaviour.
 */
public final class RhythmicBlessing implements Trait {
    public enum Occasion {
        ALWAYS,
        OPEN_DAY,
        NIGHTFALL,
        SUBMERGED,
        DRY_LAND,
        BELOW_DEPTHS,
        DIM_GLOOM
    }

    private final ResourceLocation sigil;
    private final Holder<MobEffect> draught;
    private final int draftLevel;
    private final int draftLength;
    private final int rhythm;
    private final Occasion occasion;
    private final boolean burden;

    public RhythmicBlessing(String name, Holder<MobEffect> draught, int draftLevel, int draftLength,
        int rhythm, Occasion occasion, boolean burden) {
        this.sigil = ResourceLocation.fromNamespaceAndPath(LineageCore.MOD_ID, name);
        this.draught = draught;
        this.draftLevel = draftLevel;
        this.draftLength = draftLength;
        this.rhythm = rhythm;
        this.occasion = occasion;
        this.burden = burden;
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
        return burden;
    }

    @Override
    public void pulse(ServerPlayer player) {
        if (player.tickCount % rhythm != 0) {
            return;
        }
        if (ripe(player)) {
            player.addEffect(new MobEffectInstance(draught, draftLength, draftLevel, false, false, true));
        }
    }

    private boolean ripe(ServerPlayer player) {
        return switch (occasion) {
            case ALWAYS -> true;
            case OPEN_DAY -> player.level().isDay()
                && player.level().canSeeSky(BlockPos.containing(player.getEyePosition()));
            case NIGHTFALL -> !player.level().isDay();
            case SUBMERGED -> player.isUnderWater();
            case DRY_LAND -> !player.isUnderWater();
            case BELOW_DEPTHS -> player.getY() < 60.0;
            case DIM_GLOOM -> player.getLightLevelDependentMagicValue() <= 7.0F;
        };
    }
}
