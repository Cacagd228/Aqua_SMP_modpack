package me.nanorasmus.nanodev.hex_js.mixin;

import at.petrak.hexcasting.api.casting.PatternShapeMatch;
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment;
import me.nanorasmus.nanodev.hex_js.casting.DeceptionCastEnv;
import me.nanorasmus.nanodev.hex_js.casting.SilenceMishap;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import me.nanorasmus.nanodev.hex_js.sound.HexSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CastingEnvironment.class)
public class SilenceMagicBlockMixin {
    @Inject(method = "precheckAction", at = @At("HEAD"), cancellable = true)
    private void hexjs$blockSilence(PatternShapeMatch match, CallbackInfo ci) throws Throwable {
        CastingEnvironment self = (CastingEnvironment) (Object) this;
        ServerPlayer caster;
        ServerPlayer realCaster = null;
        try {
            caster = self.getCaster();
            if (self instanceof DeceptionCastEnv dce) {
                realCaster = dce.getRealCaster();
            } else if (caster != null && caster.getClass().getName().contains("FakePlayer")) {
                try {
                    var server = caster.getServer();
                    if (server != null) realCaster = server.getPlayerList().getPlayer(caster.getUUID());
                } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            return;
        }
        boolean silenced = false;
        ServerPlayer silencedPlayer = null;
        if (caster != null && caster.hasEffect(HexEffects.SILENCE)) { silenced = true; silencedPlayer = caster; }
        if (realCaster != null && realCaster.hasEffect(HexEffects.SILENCE)) { silenced = true; silencedPlayer = realCaster; }
        if (!silenced) return;

        // Sound + actionbar — block reason visible 0.5s (feedback to real owner)
        ServerPlayer feedback = silencedPlayer != null ? silencedPlayer : caster;
        try {
            var sound = HexSounds.SILENCE.get();
            // play at deception position if deception env, else at caster
            var pos = feedback.blockPosition();
            try { if (self instanceof DeceptionCastEnv) pos = caster.blockPosition(); } catch (Throwable ignored) {}
            self.getWorld().playSound(null, pos, sound, SoundSource.PLAYERS, 1.0f, 1.0f);
            // also to real player directly if different
            if (feedback != caster && caster != null) {
                try { feedback.level().playSound(null, feedback.blockPosition(), sound, SoundSource.PLAYERS, 1.0f, 1.0f); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        try {
            Component msg = Component.literal("Безмолвие").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
            feedback.displayClientMessage(msg, true);
        } catch (Throwable ignored) {}
        try {
            at.petrak.hexcasting.forge.network.ForgePacketHandler.sendToPlayer(
                    feedback, new me.nanorasmus.nanodev.hex_js.network.MsgSilenceDenyS2C());
        } catch (Throwable ignored) {}

        // Throw mishap — CastingEnvironment.precheckAction declares throws Mishap,
        // so throwing directly cancels the cast and triggers mishap particles/sound.
        throw new SilenceMishap();
    }
}
