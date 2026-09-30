package dev.aerofix.mixin;

import dev.aerofix.AeroFixClientConfig;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect.ClientBalloonEffectRenderer;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Клиентский рендер горячего воздуха.
 *
 * <p>Две правки:
 * <ul>
 *   <li><b>Краш.</b> Сток берёт {@code Minecraft.getInstance().getWindow()} и сразу
 *       дёргает по нему {@code getWidth()}. Окно бывает {@code null} до его создания
 *       и во время пересоздания — это гарантированный NPE в кадре рендера.</li>
 *   <li><b>Опция.</b> Рендер Envelope-дымки идёт через Veil FBO + пост-шейдер и
 *       стоит ощутимо. Раньше его можно было только выключить, пересобрав bundle;
 *       здесь это обычный конфиг без перезагрузки мира. Физика не затронута —
 *       блоки, contraptions и подъёмная сила считаются на сервере.</li>
 * </ul>
 *
 * <p>Миксин в секции {@code client}: на выделенном сервере целевой класс
 * не загружается вообще.
 */
@Mixin(ClientBalloonEffectRenderer.class)
public abstract class ClientBalloonEffectRendererMixin {
    @Inject(method = "onRenderLevelStage", at = @At("HEAD"), cancellable = true)
    private static void aerofix$guardHotAirRender(VeilRenderLevelStageEvent.Stage stage,
                                                Matrix4fc modelView, Matrix4fc projection,
                                                int partialTick, CallbackInfo ci) {
        if (!AeroFixClientConfig.hotAirRendering()) {
            ci.cancel();
            return;
        }
        if (Minecraft.getInstance().getWindow() == null) {
            ci.cancel();
        }
    }
}
