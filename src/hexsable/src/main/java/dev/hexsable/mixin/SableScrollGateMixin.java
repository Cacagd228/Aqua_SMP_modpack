package dev.hexsable.mixin;

import at.petrak.hexcasting.api.casting.PatternShapeMatch;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.casting.PatternRegistryManifest;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S;
import dev.hexsable.scroll.SableScrollGate;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Закрывает руны Hex Sable Bridge, пока не найден их свиток: нарисованный узор,
 * чья страница книги ещё закрыта (нет парного {@code hexsable:scrolls/*}
 * advancement), отклоняется, чтобы страницы нельзя было обойти узорами,
 * подсмотренными вне игры.
 *
 * <p>По образцу {@code me.nanorasmus.nanodev.hex_js.mixin.SpellPatternInterceptor}
 * из MeowHex (там же лежит gate базовых свитков). Проверяем только свой
 * неймспейс, чужие узоры пропускаем untouched.
 */
@Mixin(MsgNewSpellPatternC2S.class)
public class SableScrollGateMixin {
    @Shadow
    @Final
    private HexPattern pattern;

    @Inject(method = "handle(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"), cancellable = true)
    private void hexsable$gateNewPattern(MinecraftServer server, ServerPlayer sender, CallbackInfo ci) {
        var shapeMatch = PatternRegistryManifest.matchPattern(pattern, server.overworld(), false);
        ResourceLocation drawnOpId = null;
        if (shapeMatch instanceof PatternShapeMatch.Normal normal) {
            drawnOpId = normal.key.location();
        } else if (shapeMatch instanceof PatternShapeMatch.PerWorld perWorld && perWorld.certain) {
            drawnOpId = perWorld.key.location();
        }
        if (drawnOpId != null
                && "hexsable".equals(drawnOpId.getNamespace())
                && !SableScrollGate.canCast(sender, drawnOpId)) {
            sender.sendSystemMessage(Component.translatable("hexsable.message.scroll_locked"));
            ci.cancel();
        }
    }
}
