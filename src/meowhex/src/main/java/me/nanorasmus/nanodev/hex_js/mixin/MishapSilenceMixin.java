package me.nanorasmus.nanodev.hex_js.mixin;

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.mishaps.Mishap;
import me.nanorasmus.nanodev.hex_js.effect.SilenceHelper;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import me.nanorasmus.nanodev.hex_js.casting.DeceptionCastEnv;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Mishap.class)
public class MishapSilenceMixin {
    // `execute` is abstract (Kotlin) -> HEAD inject fails with NPE (no instructions). Inject into concrete bridge instead.
    @Inject(method = "executeReturnStack", at = @At("HEAD"))
    private void hexjs$applySilence(CastingEnvironment env, Mishap.Context context, List<Iota> stack, CallbackInfoReturnable<List<Iota>> cir) {
        try {
            Mishap self = (Mishap) (Object) this;
            // Resolve real player: deception env uses FakePlayer, but owner must get silence
            ServerPlayer target = null;
            boolean isDeception = env instanceof DeceptionCastEnv;
            if (isDeception) {
                DeceptionCastEnv dce = (DeceptionCastEnv) env;
                target = dce.getRealCaster();
                if (target == null) target = env.getCaster();
            } else {
                ServerPlayer caster = env.getCaster();
                if (caster != null && caster.getClass().getName().contains("FakePlayer")) {
                    // FakePlayer shares UUID with real player — lookup real instance
                    try {
                        var server = caster.getServer();
                        if (server != null) {
                            var real = server.getPlayerList().getPlayer(caster.getUUID());
                            target = real != null ? real : caster;
                        } else target = caster;
                    } catch (Throwable t) { target = caster; }
                } else target = caster;
            }
            if (target != null) {
                me.nanorasmus.nanodev.hex_js.HexJS.LOGGER.info("Silence mishap {} via {} -> target {} (deception={})", self.getClass().getSimpleName(), env.getClass().getSimpleName(), target.getName().getString(), isDeception);
                SilenceHelper.applySilence(target, self);
                // Also silence fake if different instance (no-op if same)
                try {
                    ServerPlayer fake = env.getCaster();
                    if (fake != null && fake != target) SilenceHelper.applySilence(fake, self);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
