package me.nanorasmus.nanodev.hex_js.mixin;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S;
import me.nanorasmus.nanodev.hex_js.PatternGate;
import me.nanorasmus.nanodev.hex_js.addon.HexArtifactsItems;
import me.nanorasmus.nanodev.hex_js.addon.item.ItemSniperScope;
import me.nanorasmus.nanodev.hex_js.casting.OpDeadeye;
import me.nanorasmus.nanodev.hex_js.helpers.CurioHelper;
import me.nanorasmus.nanodev.hex_js.storage.HexJsData;
import me.nanorasmus.nanodev.hex_js.storage.PatternList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Front-gates every pattern a player draws on the staff. Runs at the very start of
 * {@link MsgNewSpellPatternC2S#handle} (before the pattern is queued for casting)
 * so forbidden signatures never reach the VM and redirected ones are rewritten in
 * place before the rest of the handler consumes {@code pattern}.
 *
 * <p>Handling the base literals specially ({@code aqaa}/{@code dedd}) preserves the
 * original addon's semantics: banning a literal base kills the whole family of
 * numbers, not just one drawn occurrence.
 */
@Mixin(MsgNewSpellPatternC2S.class)
public class SpellPatternInterceptor {
    @Shadow
    @Final
    @Mutable
    private HexPattern pattern;

    @Inject(method = "handle(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At("HEAD"), cancellable = true)
    private void hexjs$gateNewPattern(MinecraftServer server, ServerPlayer sender, CallbackInfo ci) {
        HexJsData data = HexJsData.get();
        if (data == null) {
            // Gatekeeping config not loaded yet — let the pattern through.
            return;
        }
        // Artifact-gated patterns: Deadeye requires the Sniper's Scope to be worn
        // and reloaded (the scope goes on cooldown after each shot).
        if (OpDeadeye.isDeadeyePattern(pattern)) {
            ItemStack scope = CurioHelper.findCurio(sender, HexArtifactsItems.SNIPER_SCOPE.get());
            if (scope.isEmpty()) {
                sender.sendSystemMessage(Component.translatable("meowhex.message.need_sniper_scope"));
                ci.cancel();
                return;
            }
            long gameTime = sender.level().getGameTime();
            long readyAt = ItemSniperScope.getDeadeyeReadyAt(scope);
            if (readyAt > gameTime) {
                long remaining = (readyAt - gameTime + 19) / 20;
                sender.sendSystemMessage(
                        Component.translatable("meowhex.message.deadeye_cooldown", remaining));
                ci.cancel();
                return;
            }
        }
        PatternList perPlayer = data.playerOrNull(sender.getUUID());
        PatternList perPlayerSafe = perPlayer != null ? perPlayer : new PatternList();
        PatternGate.Verdict verdict = PatternGate.decide(pattern, perPlayerSafe, data.global());

        switch (verdict.kind()) {
            case BLOCKED -> {
                sender.sendSystemMessage(Component.literal(verdict.reason()));
                ci.cancel();
            }
            case REDIRECT -> pattern = verdict.redirected();
            case ALLOW -> {
            }
        }
    }
}