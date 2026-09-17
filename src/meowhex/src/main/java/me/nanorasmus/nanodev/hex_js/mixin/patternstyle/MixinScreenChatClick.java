package me.nanorasmus.nanodev.hex_js.mixin.patternstyle;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.forge.network.ForgePacketHandler;
import me.nanorasmus.nanodev.hex_js.helpers.PatternTextUtils;
import me.nanorasmus.nanodev.hex_js.network.MsgCopyPatternC2S;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * When the player clicks a pattern marker (a ClickEvent whose value is a parseable
 * {@code <dir,sig>} marker) in chat history / books, ask the server to copy it into
 * the staff cast instead of the default clipboard action. Consumed client-side.
 */
@Mixin(Screen.class)
public abstract class MixinScreenChatClick {

    @Inject(method = "handleComponentClicked(Lnet/minecraft/network/chat/Style;)Z",
        at = @At("HEAD"), cancellable = true)
    private void hexjs$copyPatternFromChat(Style style, CallbackInfoReturnable<Boolean> cir) {
        ClickEvent event = style.getClickEvent();
        if (event == null) {
            return;
        }
        List<HexPattern> patterns = PatternTextUtils.parsePatterns(event.getValue());
        if (patterns.isEmpty()) {
            return;
        }
        ForgePacketHandler.sendToServer(new MsgCopyPatternC2S(patterns));
        cir.setReturnValue(true);
    }
}