package at.petrak.hexcasting.mixin.client;

import at.petrak.hexcasting.client.gui.ManaBarHud;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lifts the action bar above the mana bar when the mana bar is rendered.
 */
@Mixin(Gui.class)
public class MixinGui {
    @Redirect(
        method = "renderOverlayMessage",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;guiHeight()I")
    )
    private int hexcasting$liftActionbarAboveManaBar(GuiGraphics instance) {
        return instance.guiHeight() - (ManaBarHud.shouldRender() ? ManaBarHud.OVERLAY_LIFT : 0);
    }
}
