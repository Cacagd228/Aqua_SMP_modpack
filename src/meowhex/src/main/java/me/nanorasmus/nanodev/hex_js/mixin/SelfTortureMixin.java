package me.nanorasmus.nanodev.hex_js.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.eval.env.PlayerBasedCastEnv;
import at.petrak.hexcasting.api.misc.ManaHelper;
import at.petrak.hexcasting.common.lib.HexAttributes;
import at.petrak.hexcasting.common.lib.HexDamageTypes;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemSelfTortureRing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ring of Self-Torture: when a fully-paid cast goes through while the ring's
 * discount modifier is present, the spared mana strikes back as
 * {@code hexcasting:overcast} damage at {@link #MANA_PER_HP} mana per 1 HP.
 *
 * <p>Hooks the single spend funnel ({@code extractMana}), so staff and packaged
 * casts are covered alike. Only successful casts (nothing left unpaid) trigger
 * the backlash; the ring is detected by its own discount modifier id, no Curios
 * API needed.
 */
@Mixin(PlayerBasedCastEnv.class)
public abstract class SelfTortureMixin {
    /** How much spared mana buys 1 HP of overcast backlash. */
    private static final double MANA_PER_HP = 5.0;

    @Inject(method = "extractMana", at = @At("TAIL"))
    private void hexjs$selfTortureOvercast(long costLeft, CallbackInfoReturnable<Long> cir) {
        if (cir.getReturnValue() != 0L) {
            return;
        }
        ServerPlayer caster;
        try {
            caster = ((CastingEnvironment) (Object) this).getCaster();
        } catch (Throwable ignored) {
            return;
        }
        if (caster == null || caster.level().isClientSide) {
            return;
        }
        AttributeInstance discount = caster.getAttribute(
                BuiltInRegistries.ATTRIBUTE.wrapAsHolder(HexAttributes.MANA_DISCOUNT));
        if (discount == null || discount.getModifier(ItemSelfTortureRing.RING_DISCOUNT_ID) == null) {
            return;
        }
        double paid = ManaHelper.manaCostOfMedia(caster, costLeft);
        double full = ManaHelper.manaCostOfMedia(costLeft);
        double spared = full - paid;
        if (spared <= 1e-9) {
            return;
        }
        var holder = caster.level().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(HexDamageTypes.OVERCAST);
        float backlash = (float) (spared / MANA_PER_HP);
        // Печать Утгарда: внутри вложенного исполнения удар не мгновенный,
        // а суммируется в отложенный долг (бьёт через 5 с одним ударом).
        try {
            var utgard = ((CastingEnvironment) (Object) this).getExtension(
                    me.nanorasmus.nanodev.hex_js.casting.UtgardState.KEY);
            if (utgard != null && utgard.active()) {
                me.nanorasmus.nanodev.hex_js.casting.UtgardHandler.accumulate(caster, backlash);
                return;
            }
        } catch (Throwable ignored) {
        }
        caster.hurt(new DamageSource(holder), backlash);
    }
}
