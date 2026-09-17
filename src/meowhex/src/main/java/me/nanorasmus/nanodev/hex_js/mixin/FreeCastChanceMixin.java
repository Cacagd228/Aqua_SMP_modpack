package me.nanorasmus.nanodev.hex_js.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv;
import at.petrak.hexcasting.api.misc.ManaHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Apofix free-cast chance: with probability {@code apofix:free_cast_chance} a
 * cast goes through without spending mana — but only if the caster could
 * afford the (discounted) cost; otherwise the normal path runs and fails with
 * the usual "not enough mana" message.
 *
 * <p>Soft apofix integration (registry lookup only); inert without apofix
 * (attribute missing) or at zero chance. Hooks the single spend funnel, so
 * staff and packaged casts are covered alike.
 */
@Mixin(PlayerBasedCastEnv.class)
public abstract class FreeCastChanceMixin {
    private static final ResourceLocation FREE_CAST_CHANCE_ID =
            ResourceLocation.fromNamespaceAndPath("apofix", "free_cast_chance");

    @Inject(method = "extractMana", at = @At("HEAD"), cancellable = true)
    private void hexjs$freeCastChance(long costLeft, CallbackInfoReturnable<Long> cir) {
        ServerPlayer caster;
        try {
            caster = ((CastingEnvironment) (Object) this).getCaster();
        } catch (Throwable ignored) {
            return;
        }
        if (caster == null || caster.level().isClientSide) {
            return;
        }
        var holder = BuiltInRegistries.ATTRIBUTE.getHolder(FREE_CAST_CHANCE_ID).orElse(null);
        if (holder == null) {
            return;
        }
        double chance = caster.getAttributeValue(holder);
        if (chance <= 1e-9 || caster.level().getRandom().nextDouble() >= chance) {
            return;
        }
        double cost = ManaHelper.manaCostOfMedia(caster, costLeft);
        if (ManaHelper.getMana(caster) < cost) {
            return;
        }
        cir.setReturnValue(0L);
    }
}
