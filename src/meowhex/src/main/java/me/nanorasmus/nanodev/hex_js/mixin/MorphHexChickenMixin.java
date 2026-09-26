package me.nanorasmus.nanodev.hex_js.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.nanorasmus.nanodev.hex_js.effect.HexEffects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Морф-хекс, клиент: вместо модели морфированного игрока рисуется курица.
 * Болванка-курица нигде не спавнится — только источник модели/текстуры.
 * Ник с именем игрока вешается на курицу, чтобы было видно кого захексили.
 */
@Mixin(EntityRenderDispatcher.class)
public class MorphHexChickenMixin {
    @Unique
    private Chicken meowhex$dummy;
    @Unique
    private Level meowhex$dummyLevel;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void meowhex$renderHexedAsChicken(Entity entity, double x, double y, double z,
                                              float yaw, float partialTick, PoseStack pose,
                                              MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (!(entity instanceof Player player)) {
            return;
        }
        boolean morphed;
        try {
            morphed = player.hasEffect(HexEffects.MORPH_HEX);
        } catch (Throwable ignored) {
            return;
        }
        if (!morphed) {
            return;
        }
        Level level = entity.level();
        if (level == null) {
            return;
        }
        try {
            if (meowhex$dummy == null || meowhex$dummyLevel != level) {
                meowhex$dummy = new Chicken(EntityType.CHICKEN, level);
                meowhex$dummyLevel = level;
            }
            Chicken dummy = meowhex$dummy;
            dummy.setYRot(player.getYRot());
            dummy.setYBodyRot(player.yBodyRot);
            dummy.setYHeadRot(player.yHeadRot);
            dummy.setXRot(player.getXRot());
            dummy.tickCount = player.tickCount;
            try {
                dummy.setCustomName(player.getName());
                dummy.setCustomNameVisible(true);
            } catch (Throwable ignored) {
            }

            EntityRenderDispatcher self = (EntityRenderDispatcher) (Object) this;
            ChickenRenderer renderer = (ChickenRenderer) self.getRenderer(dummy);
            if (renderer == null) {
                return;
            }
            pose.pushPose();
            pose.translate(x, y, z);
            try {
                renderer.render(dummy, yaw, partialTick, pose, buffer, light);
            } finally {
                pose.popPose();
            }
            ci.cancel();
        } catch (Throwable ignored) {
            // Любой сбой рендера курицы — молча оставляем обычную модель игрока.
        }
    }
}
