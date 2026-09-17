package xyz.lineage.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.lineage.client.veil.VeilRender;

/** Drapes the gloom-sworn in translucent dusk; hunger-crouch deepens the veil. */
@Mixin(LivingEntityRenderer.class)
public abstract class VeilPlayerRenderMixin {
    @Inject(
        method = "getRenderType(Lnet/minecraft/world/entity/LivingEntity;ZZZ)Lnet/minecraft/client/renderer/RenderType;",
        at = @At("HEAD"), cancellable = true
    )
    private void lineage$veilRenderType(LivingEntity entity, boolean bodyVisible, boolean translucent, boolean glowing,
            CallbackInfoReturnable<RenderType> cir) {
        if (VeilRender.drape(entity)) {
            ResourceLocation hide = veilHide(entity);
            if (hide != null) {
                cir.setReturnValue(RenderType.itemEntityTranslucentCull(hide));
            }
        }
    }

    @WrapOperation(
        method = "render",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    )
    private void lineage$veilHue(EntityModel<?> model, PoseStack pose, VertexConsumer buffer, int packedLight,
            int packedOverlay, int packedColor, Operation<Void> original,
            @Local(argsOnly = true) LivingEntity entity) {
        if (VeilRender.drape(entity)) {
            original.call(model, pose, buffer, packedLight, packedOverlay, VeilRender.hue(entity));
        } else {
            original.call(model, pose, buffer, packedLight, packedOverlay, packedColor);
        }
    }

    private static ResourceLocation veilHide(LivingEntity entity) {
        if (entity instanceof AbstractClientPlayer player) {
            return player.getSkin().texture();
        }
        return null;
    }
}
